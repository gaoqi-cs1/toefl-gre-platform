package com.demo.toefl.service;

import com.demo.toefl.entity.FullQuestion;
import com.demo.toefl.util.FullScoreUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 口语 / 写作自动评分（本地评分引擎，不依赖外网与大模型 API）
 *
 * 评分口径：新托福 1-6 分档（0.5 步进），按四个维度累加：
 *   内容覆盖 / 结构组织 / 语言质量 / 流畅度（口语）或句法（写作）
 * 每个维度先算 0-N 的小分，最后 1 + 各维度小分 = 分档，再量化到 0.5。
 *
 * 想换成真正的大模型评分时，只要替换本类里 scoreWriting / scoreSpeaking 的实现，
 * 入参（题目 + 文本）和返回结构（band + dimensions + strengths + improvements）保持不变，
 * 前端和接口都不用改。
 */
@Service
public class AiScoringService {

    /** 常见连接词 / 篇章信号词，用来判断结构组织 */
    private static final List<String> CONNECTIVES = Arrays.asList(
            "however", "therefore", "moreover", "furthermore", "first", "second", "third",
            "finally", "in addition", "for example", "for instance", "because", "although",
            "while", "thus", "as a result", "on the other hand", "in contrast", "besides",
            "overall", "to sum up", "in conclusion", "also", "but", "so");

    /** 口语评分时忽略的高频虚词（算关键词覆盖度时排除） */
    private static final Set<String> STOPWORDS = new LinkedHashSet<>(Arrays.asList(
            "the", "and", "that", "this", "with", "have", "from", "your", "about", "which",
            "there", "their", "would", "could", "should", "because", "while", "when", "what",
            "some", "more", "most", "than", "then", "them", "they", "you", "are", "was",
            "were", "will", "not", "for", "but", "can", "has", "had", "into", "over", "also",
            "such", "very", "much", "many", "use", "one", "two", "why", "how", "who", "all"));

    // ==================================================================
    // 写作评分
    // ==================================================================

    public Map<String, Object> scoreWriting(FullQuestion question, String rawText) {
        String text = normalize(rawText);
        int words = wordCount(text);
        int sentences = sentenceCount(text);
        double avgLen = sentences == 0 ? 0 : round1(words * 1.0 / sentences);
        double ttr = typeTokenRatio(text);
        int connectives = countConnectives(text);
        int paragraphs = paragraphCount(text);
        int longSentences = countLongSentences(text, 45);

        // 目标词数：按题型给区间（综合写作 150-225，学术讨论 120 起，邮件 100 起）
        int minWords = targetMinWords(question.getTaskType());

        double content = 0;
        double ratio = minWords == 0 ? 1 : words * 1.0 / minWords;
        if (ratio >= 1.0) {
            content = 2.0;
        } else if (ratio >= 0.8) {
            content = 1.5;
        } else if (ratio >= 0.6) {
            content = 1.0;
        } else if (ratio >= 0.4) {
            content = 0.5;
        }

        double structure = 0;
        if (paragraphs >= 2) {
            structure += 0.5;
        } else if (paragraphs == 1) {
            structure += 0.2;
        }
        if (connectives >= 3) {
            structure += 0.5;
        }
        if (connectives >= 6) {
            structure += 0.5;
        }
        structure = Math.min(structure, 1.5);

        double lexis;
        if (ttr >= 0.50) {
            lexis = 1.5;
        } else if (ttr >= 0.42) {
            lexis = 1.0;
        } else if (ttr >= 0.34) {
            lexis = 0.5;
        } else {
            lexis = 0;
        }

        double syntax;
        if (avgLen >= 12 && avgLen <= 30 && longSentences <= 1) {
            syntax = 1.0;
        } else if (avgLen >= 8 && avgLen <= 35) {
            syntax = 0.5;
        } else {
            syntax = 0.2;
        }

        // 扣分项：字数极少、通篇小写开头（说明没有句子边界意识）、超长句过多
        double penalty = 0;
        if (words < 40) {
            penalty -= 1.0;
        }
        if (!text.isEmpty() && Character.isLowerCase(text.charAt(0))) {
            penalty -= 0.25;
        }
        if (longSentences > 2) {
            penalty -= 0.5;
        }

        double rawBand = 1 + content + structure + lexis + syntax + penalty;
        BigDecimal band = FullScoreUtil.quantize(rawBand);

        List<Map<String, Object>> dimensions = new ArrayList<>();
        dimensions.add(dimension("内容覆盖", content, 2.0, contentComment(content)));
        dimensions.add(dimension("结构组织", structure, 1.5, structureComment(structure, connectives, paragraphs)));
        dimensions.add(dimension("语言质量", lexis, 1.5, lexisComment(ttr)));
        dimensions.add(dimension("句法控制", syntax, 1.0, syntaxComment(avgLen, longSentences)));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("words", words);
        stats.put("targetWords", minWords);
        stats.put("sentences", sentences);
        stats.put("avgSentenceLen", avgLen);
        stats.put("typeTokenRatio", ttr);
        stats.put("connectives", connectives);
        stats.put("paragraphs", paragraphs);

        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        buildAdvice(band.doubleValue(), words, minWords, connectives, ttr, avgLen, longSentences, strengths, improvements);

        return buildResult(band, dimensions, strengths, improvements, stats, "写作");
    }

    // ==================================================================
    // 口语评分
    // ==================================================================

    public Map<String, Object> scoreSpeaking(FullQuestion question, String rawText) {
        String text = normalize(rawText);
        int words = wordCount(text);
        int sentences = sentenceCount(text);
        double avgLen = sentences == 0 ? 0 : round1(words * 1.0 / sentences);
        double ttr = typeTokenRatio(text);
        int connectives = countConnectives(text);

        // 口语目标语速：独立题 45 秒约 90-120 词；综合题 60 秒约 120-160 词
        int targetWords = "speaking_integrated".equals(question.getTaskType()) ? 130 : 100;

        // 1) 内容与任务完成度：题目关键词覆盖 + 是否有足够信息量
        double coverage = keywordCoverage(question.getPrompt(), text);
        double content = Math.min(1.5, coverage * 1.8);
        if (words >= targetWords) {
            content += 0.5;
        }
        content = Math.min(content, 2.0);

        // 2) 组织：连接词 + 句子数量（说明有没有形成"观点-理由-例子"的链条）
        double organization = 0;
        if (sentences >= 3) {
            organization += 0.5;
        }
        if (connectives >= 2) {
            organization += 0.5;
        }
        if (connectives >= 4) {
            organization += 0.5;
        }
        organization = Math.min(organization, 1.5);

        // 3) 语言质量：词汇多样性 + 句长
        double language = 0;
        if (ttr >= 0.45) {
            language += 0.75;
        } else if (ttr >= 0.35) {
            language += 0.5;
        }
        if (avgLen >= 10) {
            language += 0.75;
        } else if (avgLen >= 7) {
            language += 0.5;
        }
        language = Math.min(language, 1.5);

        // 4) 流畅度（用文本量近似）：能不能在限时内说够信息
        double fluency;
        if (words >= targetWords * 0.9) {
            fluency = 1.0;
        } else if (words >= targetWords * 0.7) {
            fluency = 0.5;
        } else {
            fluency = 0.2;
        }

        double penalty = words < 25 ? -1.0 : 0;
        BigDecimal band = FullScoreUtil.quantize(1 + content + organization + language + fluency + penalty);

        List<Map<String, Object>> dimensions = new ArrayList<>();
        dimensions.add(dimension("任务完成", content, 2.0,
                content >= 1.6 ? "要点覆盖完整，信息量足够" : content >= 1.0 ? "回应了题目，但细节偏少" : "偏题或内容过少，需要先说清立场再展开"));
        dimensions.add(dimension("结构组织", organization, 1.5,
                organization >= 1.2 ? "有清楚的展开链条" : organization >= 0.6 ? "有基本结构，连接词偏少" : "缺少组织，建议用 first / because / for example"));
        dimensions.add(dimension("语言质量", language, 1.5,
                language >= 1.2 ? "词汇与句式有变化" : language >= 0.6 ? "表达可懂，词汇重复较多" : "句式和词汇都比较单一"));
        dimensions.add(dimension("流畅度", fluency, 1.0,
                fluency >= 0.9 ? "内容量达到限时要求" : fluency >= 0.5 ? "说得偏少，可能没说完" : "内容太少，限时内没展开"));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("words", words);
        stats.put("targetWords", targetWords);
        stats.put("sentences", sentences);
        stats.put("avgSentenceLen", avgLen);
        stats.put("typeTokenRatio", ttr);
        stats.put("connectives", connectives);
        stats.put("keywordCoverage", round1(coverage));

        List<String> strengths = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        if (words >= targetWords) {
            strengths.add("限时内的信息量达标（" + words + " 词）");
        } else {
            improvements.add("内容量不足：目标约 " + targetWords + " 词，当前 " + words + " 词，先说结论再给一个具体例子");
        }
        if (connectives < 2) {
            improvements.add("补连接词把句子串起来：first / because / for example / so");
        }
        if (ttr < 0.4) {
            improvements.add("同一实词反复出现，换 2-3 个同义表达可明显提档");
        }
        if (coverage < 0.4) {
            improvements.add("题面关键词出现太少，先在开头复述题目核心词再展开");
        }
        if (strengths.isEmpty()) {
            strengths.add("已完成作答，可在此基础上做结构打磨");
        }

        return buildResult(band, dimensions, strengths, improvements, stats, "口语");
    }

    // ==================================================================
    // 内部工具方法
    // ==================================================================

    private Map<String, Object> buildResult(BigDecimal band, List<Map<String, Object>> dimensions,
                                            List<String> strengths, List<String> improvements,
                                            Map<String, Object> stats, String moduleLabel) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("band", band);
        result.put("dimensions", dimensions);
        result.put("strengths", strengths);
        result.put("improvements", improvements);
        result.put("stats", stats);
        result.put("summary", moduleLabel + "自动评分 " + band + "/6（" + FullScoreUtil.comment(band.doubleValue()) + "）");
        result.put("engine", "local-rubric-v1");
        return result;
    }

    private Map<String, Object> dimension(String name, double score, double max, String comment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("score", round1(Math.max(0, Math.min(score, max))));
        map.put("max", max);
        map.put("comment", comment);
        return map;
    }

    private void buildAdvice(double band, int words, int minWords, int connectives, double ttr,
                             double avgLen, int longSentences, List<String> strengths, List<String> improvements) {
        if (words >= minWords) {
            strengths.add("字数达标（" + words + " 词，目标 " + minWords + " 词起）");
        } else {
            improvements.add("字数不足：当前 " + words + " 词，建议至少写到 " + minWords + " 词，先保结构再求细节");
        }
        if (connectives >= 4) {
            strengths.add("篇章信号词使用到位（" + connectives + " 处）");
        } else {
            improvements.add("补足过渡词：however / therefore / for example / in conclusion，段落衔接更清楚");
        }
        if (ttr >= 0.45) {
            strengths.add("词汇重复度低（type-token ratio " + ttr + "）");
        } else {
            improvements.add("同义替换偏少：把高频实词换 2-3 种说法，词汇分能上一个档");
        }
        if (longSentences > 2) {
            improvements.add("拆长句：有 " + longSentences + " 个超过 45 词的长句，容易失控，建议一句一个观点");
        }
        if (avgLen > 0 && avgLen < 10) {
            improvements.add("句子偏短且碎，试着用 although / because 把两个短句合成一句");
        }
        if (band >= 5.0 && improvements.isEmpty()) {
            strengths.add("整体已达熟练档，可打磨论证的具体性（数字、例子、对比）");
        }
        if (band < 3.0) {
            improvements.add("先练模板骨架：立场句 → 两个理由 → 每个理由配一个例子 → 一句话总结");
        }
    }

    private String contentComment(double content) {
        if (content >= 1.8) {
            return "字数达标，信息量足够支撑观点";
        }
        if (content >= 1.2) {
            return "内容基本完整，细节还可以更具体";
        }
        if (content > 0) {
            return "内容偏少，论证没有展开";
        }
        return "几乎没有有效内容";
    }

    private String structureComment(double structure, int connectives, int paragraphs) {
        if (structure >= 1.2) {
            return "分段清楚，过渡自然（信号词 " + connectives + " 处）";
        }
        if (structure >= 0.7) {
            return "有分段与过渡，但衔接词偏少（" + connectives + " 处）";
        }
        return "结构不清（段落 " + paragraphs + " 段，信号词 " + connectives + " 处），建议先写清立场再分点";
    }

    private String lexisComment(double ttr) {
        if (ttr >= 0.5) {
            return "词汇多样（TTR " + ttr + "），同义替换做得好";
        }
        if (ttr >= 0.4) {
            return "词汇尚可（TTR " + ttr + "），仍有重复";
        }
        return "词汇重复明显（TTR " + ttr + "），需要换词";
    }

    private String syntaxComment(double avgLen, int longSentences) {
        if (longSentences > 2) {
            return "长句过多（" + longSentences + " 个），容易语法失控，建议拆句";
        }
        if (avgLen >= 12 && avgLen <= 30) {
            return "句长控制合理（平均 " + avgLen + " 词）";
        }
        if (avgLen > 0) {
            return "句长偏" + (avgLen < 12 ? "短" : "长") + "（平均 " + avgLen + " 词）";
        }
        return "没有识别到完整句子";
    }

    /** 归一化：统一空白符、去首尾空白 */
    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\r", "\n").replaceAll("[ \t]+", " ").trim();
    }

    /** 词数：按空白切分，只统计含字母或数字的片段 */
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

    /** 句子数：按 . ! ? 切分 */
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

    /** Type-Token Ratio：不同词形 / 总词数，衡量词汇重复度 */
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

    /** 连接词出现次数 */
    private int countConnectives(String text) {
        String lower = " " + text.toLowerCase(Locale.ROOT) + " ";
        int count = 0;
        for (String word : CONNECTIVES) {
            int idx = 0;
            while ((idx = lower.indexOf(" " + word + " ", idx)) >= 0) {
                count++;
                idx += word.length();
            }
        }
        return count;
    }

    /** 段落数：按空行或换行切分 */
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

    /** 超过 limit 词的长句个数 */
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

    /** 题目关键词 → 回答里的覆盖比例（0-1） */
    private double keywordCoverage(String prompt, String answer) {
        if (prompt == null || prompt.isEmpty() || answer.isEmpty()) {
            return 0;
        }
        Set<String> keywords = new LinkedHashSet<>();
        for (String token : prompt.toLowerCase(Locale.ROOT).split("\\s+")) {
            String clean = token.replaceAll("[^a-z'-]", "");
            if (clean.length() >= 5 && !STOPWORDS.contains(clean)) {
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
            // 允许简单词形变化：去找前缀命中
            if (lowerAnswer.contains(" " + keyword) || lowerAnswer.contains(keyword + "s")
                    || lowerAnswer.contains(keyword + "ed") || lowerAnswer.contains(keyword + "ing")) {
                hit++;
            }
        }
        return round1(hit * 1.0 / keywords.size());
    }

    /** 目标最低词数 */
    private int targetMinWords(String taskType) {
        if (taskType == null) {
            return 150;
        }
        switch (taskType) {
            case "writing_integrated":
                return 150;
            case "writing_discussion":
                return 120;
            case "writing_email":
                return 100;
            default:
                return 150;
        }
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /** 供接口层把评分结果转成 JSON 字符串 */
    public String toJson(Map<String, Object> result) {
        if (result == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof BigDecimal) {
                sb.append(value);
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof List) {
                sb.append(listToJson((List<?>) value));
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                sb.append(mapToJson(map));
            } else {
                sb.append('"').append(escape(String.valueOf(value))).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof BigDecimal) {
                sb.append(value);
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof List) {
                sb.append(listToJson((List<?>) value));
            } else {
                sb.append('"').append(escape(String.valueOf(value))).append('"');
            }
        }
        return sb.append('}').toString();
    }

    private String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            Object item = list.get(i);
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) item;
                sb.append(mapToJson(map));
            } else {
                sb.append('"').append(escape(String.valueOf(item))).append('"');
            }
        }
        return sb.append(']').toString();
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    /** 供前端展示的题目统计（保留方法便于以后接入大模型时做上下文拼装） */
    public Map<String, Object> debugStats(String text) {
        Map<String, Object> map = new HashMap<>();
        map.put("words", wordCount(normalize(text)));
        map.put("sentences", sentenceCount(normalize(text)));
        return map;
    }
}
