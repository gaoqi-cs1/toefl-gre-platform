package com.demo.toefl.service;

import com.demo.toefl.util.GreScoreUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GRE Analytical Writing（AWA）自动评分 —— 本地 rubric 引擎，不依赖外网 / 不花钱
 *
 * 四个维度合计 0.5 + 1.5 + 1.5 + 1.5 + 1.0 = 6.0（0.5 一档，与 ETS 的 AWA 量纲一致）：
 *   1) 立场与论证深度   Issue：是否回应题目；Argument：是否拆假设、给替代解释
 *   2) 例证与具体性     有没有数据、案例、机制，而不是只有形容词
 *   3) 结构组织         分段、段落推进、过渡词
 *   4) 语言控制         词汇多样性（TTR）、平均句长、超长句风险
 *
 * 设计原则（也是与"样例分数"对齐的关键）：
 *   - 参照的是"结构完整、有例证、约 250 词的作文大约 5 分档"，而不是要求写到 400 词才给分；
 *   - 词表匹配先做标点归一化，避免 "however," 这种带逗号的写法漏数；
 *   - 只有一段但句子够多时给部分结构分，同时在改进建议里提示分段。
 *
 * 想换成真正的大模型评分，只需替换 scoreIssue / scoreArgument 的内部实现，
 * 入参（题面 + 作文）与出参结构保持不变，接口和三个页面都不用改。
 */
@Service
public class GreAwaScoringService {

    /** 过渡词 / 论证连接词（含口语化连接词，避免只认 however 这一种写法） */
    private static final List<String> CONNECTIVES = Arrays.asList(
            "however", "therefore", "moreover", "furthermore", "first", "second", "third",
            "finally", "in addition", "for example", "for instance", "because", "although",
            "though", "while", "thus", "as a result", "on the other hand", "in contrast",
            "consequently", "overall", "in conclusion", "nevertheless", "similarly", "consider",
            "but", "yet", "so", "if", "unless", "whether", "since", "even", "also", "still",
            "instead", "rather", "otherwise", "admittedly", "granted", "suppose", "assume");

    /** Argument 专属信号词：识别假设 / 替代解释 / 证据缺口 */
    private static final List<String> ARGUMENT_MARKERS = Arrays.asList(
            "assum", "alternat", "explanation", "evidence", "question", "doubt",
            "correlat", "coincid", "sample", "comparison", "unwarranted", "implies",
            "fails to", "does not", "missing", "overlook", "cost", "whether",
            "may", "might", "could", "likely", "unclear", "reliable", "represent",
            "generaliz", "survey", "self-report", "bias", "confound", "report");

    /** Issue 专属信号词：立场 + 让步 + 具体化 */
    private static final List<String> ISSUE_MARKERS = Arrays.asList(
            "because", "for example", "consider", "although", "however", "therefore",
            "in practice", "case", "histor", "research", "evidence", "critics",
            "should", "would", "benefit", "harm", "depends", "depends on", "extent",
            "while", "though", "unless", "if");

    /** 例子/具体化信号 */
    private static final List<String> EXAMPLE_CUES = Arrays.asList(
            "for example", "for instance", "consider", "case of", "such as", "take the",
            "imagine", "in one");

    /** 关键词覆盖时忽略的高频虚词 */
    private static final Set<String> STOPWORDS = new LinkedHashSet<>(Arrays.asList(
            "the", "and", "that", "this", "with", "have", "from", "your", "about", "which",
            "there", "their", "would", "could", "should", "because", "while", "when", "what",
            "some", "more", "most", "than", "then", "them", "they", "you", "are", "was",
            "were", "will", "not", "for", "but", "can", "has", "had", "into", "over", "also",
            "such", "very", "much", "many", "does", "did", "its", "our", "who", "how", "why",
            "all", "any", "one", "two", "both", "each", "other", "these", "those"));

    // ==================================================================
    // 对外入口
    // ==================================================================

    /** Evaluate an Issue task */
    public Map<String, Object> scoreIssue(String prompt, String rawText) {
        return score(prompt, rawText, false);
    }

    /** Evaluate an Argument task */
    public Map<String, Object> scoreArgument(String prompt, String rawText) {
        return score(prompt, rawText, true);
    }

    /** 按 taskType 自动分流（awa_issue / awa_argument） */
    public Map<String, Object> scoreByTaskType(String taskType, String prompt, String rawText) {
        boolean argument = taskType != null && taskType.toLowerCase(Locale.ROOT).contains("argument");
        return score(prompt, rawText, argument);
    }

    // ==================================================================
    // 核心算法
    // ==================================================================

    private Map<String, Object> score(String prompt, String rawText, boolean argument) {
        String text = normalize(rawText);
        int words = wordCount(text);
        int sentences = sentenceCount(text);
        int rawParagraphs = paragraphCount(text);
        // 一段到底但句子足够多时，按"隐含分段"参与打分（否则结构分会误判为 0）
        int paragraphs = (rawParagraphs <= 1 && sentences >= 8)
                ? Math.min(4, 1 + sentences / 4)
                : rawParagraphs;
        boolean singleBlock = rawParagraphs <= 1;

        int connectives = countPhrases(text, CONNECTIVES);
        int markers = countPhrases(text, argument ? ARGUMENT_MARKERS : ISSUE_MARKERS);
        int exampleCues = countPhrases(text, EXAMPLE_CUES);
        double ttr = typeTokenRatio(text);
        double avgLen = sentences == 0 ? 0 : round1(words * 1.0 / sentences);
        int longSentences = countLongSentences(text, 45);
        double coverage = keywordCoverage(prompt, text);
        int numbers = countDigits(text);
        int capitalized = countCapitalizedWords(text);

        int targetWords = argument ? 320 : 360;

        // ---------- 1) 立场与论证深度（0-1.5）----------
        double depth = 0;
        if (coverage >= 0.5) {
            depth += 0.5;
        } else if (coverage >= 0.2) {
            depth += 0.3;
        }
        if (markers >= 6) {
            depth += 1.0;
        } else if (markers >= 3) {
            depth += 0.75;
        } else if (markers >= 1) {
            depth += 0.45;
        }
        depth = Math.min(depth, 1.5);

        // ---------- 2) 例证与具体性（0-1.5）----------
        double evidence = 0;
        if (words >= targetWords) {
            evidence += 0.6;
        } else if (words >= 250) {
            evidence += 0.55;
        } else if (words >= 200) {
            evidence += 0.45;
        } else if (words >= 150) {
            evidence += 0.3;
        } else if (words >= 100) {
            evidence += 0.15;
        }
        // 具体信息：数字 / 专有名词（年份、机构、地名、案例名）
        if (numbers >= 2) {
            evidence += 0.3;
        } else if (numbers == 1) {
            evidence += 0.15;
        }
        if (capitalized >= 3) {
            evidence += 0.3;
        } else if (capitalized >= 1) {
            evidence += 0.15;
        }
        if (exampleCues >= 1) {
            evidence += 0.3;
        }
        evidence = Math.min(evidence, 1.5);

        // ---------- 3) 结构组织（0-1.5）----------
        double organization = 0;
        if (paragraphs >= 4) {
            organization += 0.75;
        } else if (paragraphs == 3) {
            organization += 0.6;
        } else if (paragraphs == 2) {
            organization += 0.4;
        } else {
            organization += 0.2;
        }
        if (connectives >= 6) {
            organization += 0.75;
        } else if (connectives >= 4) {
            organization += 0.6;
        } else if (connectives >= 2) {
            organization += 0.4;
        } else if (connectives >= 1) {
            organization += 0.25;
        }
        organization = Math.min(organization, 1.5);

        // ---------- 4) 语言控制（0-1）----------
        double language = 0;
        if (ttr >= 0.38) {
            language += 0.5;
        } else if (ttr >= 0.30) {
            language += 0.35;
        } else if (ttr >= 0.22) {
            language += 0.2;
        }
        if (avgLen >= 12 && avgLen <= 34) {
            language += 0.5;
        } else if (avgLen >= 8) {
            language += 0.3;
        } else if (avgLen > 0) {
            language += 0.15;
        }
        language = Math.min(language, 1.0);

        // ---------- 扣分 ----------
        double penalty = 0;
        if (words < 50) {
            penalty -= 2.0;
        } else if (words < 120) {
            penalty -= 1.0;
        } else if (words < targetWords * 0.5) {
            penalty -= 0.5;
        }
        if (!text.isEmpty() && Character.isLowerCase(text.charAt(0))) {
            penalty -= 0.25;
        }
        if (longSentences > 4) {
            penalty -= 0.5;
        }
        if (sentences == 0) {
            penalty -= 2.0;
        }

        // 0.5 是"至少构成一篇回应"的基础分，四维达成度往上加
        double raw = 0.5 + depth + evidence + organization + language + penalty;
        BigDecimal score = GreScoreUtil.quantize(raw);

        // ---------- 维度明细 ----------
        List<Map<String, Object>> dimensions = new ArrayList<>();
        dimensions.add(dimension(argument ? "论证缺口识别" : "立场与论证深度", depth, 1.5,
                argument
                        ? (depth >= 1.2 ? "识别了假设与替代解释，并说明假设不成立会怎样"
                        : depth >= 0.6 ? "提到部分假设，但没有逐条推演后果"
                        : "基本只在复述原论证，没有指出假设")
                        : (depth >= 1.2 ? "立场明确，理由之间有推进"
                        : depth >= 0.6 ? "有立场，但理由展开不够"
                        : "立场含糊，或只有口号式判断")));
        dimensions.add(dimension("例证与具体性", evidence, 1.5,
                evidence >= 1.2 ? "例子具体（含数据或机制），能支撑论点"
                        : evidence >= 0.6 ? "有例子，但偏概括，缺少细节"
                        : "几乎只有抽象判断，需要补一个能站住的例子"));
        dimensions.add(dimension("结构组织", organization, 1.5,
                organization >= 1.2 ? "分段清楚，段落之间有推进"
                        : organization >= 0.6 ? "有分段与过渡，但衔接词偏少"
                        : "一段到底或没有过渡，读者要自己找逻辑"));
        dimensions.add(dimension("语言控制", language, 1.0,
                language >= 0.9 ? "句长与词汇控制稳定，偶有亮点"
                        : language >= 0.5 ? "表达可懂，词汇重复较多"
                        : "句子过短或过长，语法风险高"));

        // ---------- 统计 ----------
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("words", words);
        stats.put("targetWords", targetWords);
        stats.put("sentences", sentences);
        stats.put("paragraphs", rawParagraphs);
        stats.put("avgSentenceLen", avgLen);
        stats.put("typeTokenRatio", ttr);
        stats.put("connectives", connectives);
        stats.put("argumentMarkers", markers);
        stats.put("keywordCoverage", round1(coverage));
        stats.put("exampleCues", exampleCues);
        stats.put("numbersInText", numbers);
        stats.put("capitalizedWords", capitalized);

        // ---------- 文字建议 ----------
        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        if (words >= 250) {
            strengths.add("篇幅达标（" + words + " 词），论证有展开空间");
        } else {
            improvements.add("篇幅偏短：当前 " + words + " 词，先写到 " + targetWords
                    + " 词左右，论证才有展开空间");
        }
        if (rawParagraphs >= 3) {
            strengths.add("分段清楚（" + rawParagraphs + " 段）");
        } else if (singleBlock) {
            improvements.add("现在是单段：建议分 4-5 段（立场 → 理由一 → 理由二含让步 → 结论），结构分能上一档");
        }
        if (connectives < 4) {
            improvements.add("补过渡词把段落串起来：however / therefore / for example / although / in conclusion");
        }
        if (ttr < 0.4) {
            improvements.add("同一实词反复出现，换 2-3 个同义表达可明显提分");
        }
        if (numbers < 1 && capitalized < 1) {
            improvements.add("加一处具体信息（年份、比例、地名或案例名），例证分立刻上一个档");
        }
        if (argument && markers < 3) {
            improvements.add("Argument 的关键是拆假设：逐条问“如果这个前提不成立，结论还成立吗”");
        }
        if (!argument && coverage < 0.3) {
            improvements.add("先复述题目核心词，再表明立场，避免被判偏题");
        }
        if (longSentences > 4) {
            improvements.add("拆长句：有 " + longSentences + " 个超过 45 词的长句，容易失控");
        }
        if (strengths.isEmpty()) {
            strengths.add("已完成作答，可在此基础上做结构与例证打磨");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", score);
        result.put("level", GreScoreUtil.awaComment(score.doubleValue()));
        result.put("dimensions", dimensions);
        result.put("strengths", strengths);
        result.put("improvements", improvements);
        result.put("stats", stats);
        result.put("summary", (argument ? "Argument" : "Issue") + " 自动评分 "
                + score + "/6 —— " + GreScoreUtil.awaComment(score.doubleValue()));
        result.put("engine", "gre-rubric-v2");
        result.put("note", "本地 rubric 评分：给的是结构与语言维度的自查分，不等同于 ETS 官方评分");
        return result;
    }

    // ==================================================================
    // 工具方法
    // ==================================================================

    private Map<String, Object> dimension(String name, double score, double max, String comment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("score", round1(Math.max(0, Math.min(score, max))));
        map.put("max", max);
        map.put("comment", comment);
        return map;
    }

    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\r\n", "\n").replace("\r", "\n")
                .replaceAll("[ \t\u00A0]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    private int wordCount(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String token : text.split("\\s+")) {
            if (token.matches("[A-Za-z0-9'-]+")) {
                count++;
            }
        }
        return count;
    }

    private int sentenceCount(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String part : text.split("[.!?]+")) {
            if (!part.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private int paragraphCount(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String part : text.split("\n+")) {
            if (part.trim().length() > 0) {
                count++;
            }
        }
        return count;
    }

    private double typeTokenRatio(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        Set<String> unique = new LinkedHashSet<>();
        int total = 0;
        for (String token : lower.split("\\s+")) {
            String clean = token.replaceAll("[^a-z0-9'-]", "");
            if (clean.isEmpty()) {
                continue;
            }
            total++;
            unique.add(clean);
        }
        if (total == 0) {
            return 0;
        }
        return round1(unique.size() * 1.0 / total);
    }

    /** 词/词组计数：先把标点、换行归一成单个空格，避免 "however," 这类写法漏数 */
    private int countPhrases(String text, List<String> phrases) {
        String lower = " " + text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ") + " ";
        int count = 0;
        for (String phrase : phrases) {
            int idx = 0;
            while ((idx = lower.indexOf(" " + phrase + " ", idx)) >= 0) {
                count++;
                idx += phrase.length();
            }
        }
        return count;
    }

    private int countLongSentences(String text, int limit) {
        int count = 0;
        for (String sentence : text.split("[.!?]+")) {
            int words = 0;
            for (String token : sentence.trim().split("\\s+")) {
                if (token.matches("[A-Za-z0-9'-]+")) {
                    words++;
                }
            }
            if (words > limit) {
                count++;
            }
        }
        return count;
    }

    private int countDigits(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c)) {
                count++;
            }
        }
        return count;
    }

    /** 专有名词粗估：空格后面紧跟大写字母且长度 >= 2（句首词也会被算进来，所以阈值放宽） */
    private int countCapitalizedWords(String text) {
        Matcher matcher = Pattern.compile("\\s[A-Z][a-z]{2,}").matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /** 题面关键词 → 作文覆盖比例（0-1），词干前缀匹配，容忍词形变化 */
    private double keywordCoverage(String prompt, String answer) {
        if (prompt == null || prompt.isEmpty() || answer.isEmpty()) {
            return 0;
        }
        Set<String> keywords = new LinkedHashSet<>();
        for (String token : prompt.toLowerCase(Locale.ROOT).split("\\s+")) {
            String clean = token.replaceAll("[^a-z'-]", "");
            if (clean.length() >= 4 && !STOPWORDS.contains(clean)) {
                keywords.add(clean);
            }
            if (keywords.size() >= 12) {
                break;
            }
        }
        if (keywords.isEmpty()) {
            return 0;
        }
        String lowerAnswer = " " + answer.toLowerCase(Locale.ROOT) + " ";
        int hit = 0;
        for (String keyword : keywords) {
            String stem = keyword.length() > 5 ? keyword.substring(0, keyword.length() - 2) : keyword;
            if (lowerAnswer.contains(stem)) {
                hit++;
            }
        }
        return round1(hit * 1.0 / keywords.size());
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
