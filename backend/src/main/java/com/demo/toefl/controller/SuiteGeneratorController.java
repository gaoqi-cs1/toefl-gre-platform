package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.FullQuestion;
import com.demo.toefl.entity.FullSection;
import com.demo.toefl.entity.FullSuite;
import com.demo.toefl.entity.TpoQuestion;
import com.demo.toefl.entity.TpoSection;
import com.demo.toefl.mapper.FullQuestionMapper;
import com.demo.toefl.mapper.FullSectionMapper;
import com.demo.toefl.mapper.FullSuiteMapper;
import com.demo.toefl.service.TpoQuestionService;
import com.demo.toefl.service.TpoSectionService;
import com.demo.toefl.util.Result;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 一键生成套题：从单模块练习题库（tpo_section / tpo_question）里随机抽篇章，
 * 复制成一套新的真题套题（full_suite / full_section / full_question），
 * 之后走原有的 /api/fullExam/start + /submit 流程，判分口径完全一致。
 *
 *   POST /api/fullSuite/generate
 *        body 可省略，参数都是可选的：
 *        { "title": "自定义名称", "reading": 1, "listening": 1, "speaking": 1, "writing": 1 }
 *
 * 设计要点：
 *   1. 只抽"有题目的篇章"，避免生成一套空题；
 *   2. 每个模块内部用 Collections.shuffle 打乱后再取前 N 篇，所以每次点都是新组合；
 *   3. 抽中的篇章会被整篇复制（正文、音频、题目、答案、解析都带过去），
 *      套题表仍是独立数据，删改题不会影响 tpo_*；
 *   4. 口语 / 写作的素材（阅读材料 / 听力要点）原本存在 tpo_section.content_text 里，
 *      而套题答题页只渲染 question.prompt，所以复制时把素材拼进 prompt，保证考生看得到。
 */
@RestController
@RequestMapping("/api/fullSuite")
public class SuiteGeneratorController {

    @Resource
    private FullSuiteMapper fullSuiteMapper;

    @Resource
    private FullSectionMapper fullSectionMapper;

    @Resource
    private FullQuestionMapper fullQuestionMapper;

    @Resource
    private TpoSectionService tpoSectionService;

    @Resource
    private TpoQuestionService tpoQuestionService;

    private static final String[] MODULE_ORDER = {"reading", "listening", "speaking", "writing"};

    private static final String[] MODULE_LABEL = {"阅读", "听力", "口语", "写作"};

    @PostMapping("/generate")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> generate(@RequestBody(required = false) Map<String, Object> body) {

        // ---------- 1. 读参数：每个模块抽几篇，默认各 1 篇 ----------
        int wantReading = readCount(body, "reading", 1);
        int wantListening = readCount(body, "listening", 1);
        int wantSpeaking = readCount(body, "speaking", 1);
        int wantWriting = readCount(body, "writing", 1);

        // ---------- 2. 按模块随机抽篇章 ----------
        Map<String, List<TpoSection>> picked = new LinkedHashMap<>();
        Map<String, Integer> shortage = new LinkedHashMap<>();
        int[] wants = {wantReading, wantListening, wantSpeaking, wantWriting};

        for (int i = 0; i < MODULE_ORDER.length; i++) {
            String type = MODULE_ORDER[i];
            List<TpoSection> candidates = new ArrayList<>();
            for (TpoSection section : tpoSectionService.list(
                    new LambdaQueryWrapper<TpoSection>().eq(TpoSection::getType, type))) {
                long count = tpoQuestionService.count(
                        new LambdaQueryWrapper<TpoQuestion>().eq(TpoQuestion::getSectionId, section.getId()));
                if (count > 0) {
                    candidates.add(section);
                }
            }
            // 随机打乱：同一批题库，每次生成的组合都不同
            Collections.shuffle(candidates, new Random());
            int want = wants[i];
            if (candidates.size() < want) {
                shortage.put(type, want - candidates.size());
            }
            picked.put(type, new ArrayList<>(candidates.subList(0, Math.min(want, candidates.size()))));
        }

        // 一篇都抽不到说明题库是空的，直接报错，别生成空套题
        int sectionTotal = 0;
        for (String type : MODULE_ORDER) {
            sectionTotal += picked.get(type).size();
        }
        if (sectionTotal == 0) {
            return Result.fail("题库里还没有可用篇章，请先在单模块练习题库录入正文和题目");
        }

        // ---------- 3. 建套题主记录 ----------
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        String title = readString(body, "title");
        if (title == null || title.trim().isEmpty()) {
            title = "随机套题 · " + stamp;
        }
        if (title.length() > 95) {
            title = title.substring(0, 95);
        }

        FullSuite suite = new FullSuite();
        suite.setTitle(title);
        suite.setCreateTime(LocalDateTime.now());
        suite.setDifficulty("随机组合");
        suite.setDescription("从题库随机组合生成 · " + stamp);
        suite.setTotalMinutes(120);
        fullSuiteMapper.insert(suite);
        Long suiteId = suite.getId();

        // ---------- 4. 复制篇章与题目 ----------
        int reading = 0;
        int listening = 0;
        int speaking = 0;
        int writing = 0;
        List<String> composition = new ArrayList<>();

        for (String type : MODULE_ORDER) {
            int moduleIndex = 0;
            for (TpoSection src : picked.get(type)) {
                FullSection fs = new FullSection();
                fs.setSuiteId(suiteId);
                fs.setModuleType(type);
                fs.setSortIndex(++moduleIndex);
                fs.setTitle(sectionTitle(src, type));
                fs.setContentText(src.getContentText());
                fs.setAudioUrl(src.getAudioUrl());
                fs.setLimitSecond(moduleLimitSecond(type));
                fs.setHint(hintOf(type));
                fullSectionMapper.insert(fs);

                List<TpoQuestion> sourceQuestions = tpoQuestionService.list(
                        new LambdaQueryWrapper<TpoQuestion>()
                                .eq(TpoQuestion::getSectionId, src.getId())
                                .orderByAsc(TpoQuestion::getId));

                int questionIndex = 0;
                for (TpoQuestion q : sourceQuestions) {
                    FullQuestion fq = new FullQuestion();
                    fq.setSectionId(fs.getId());
                    fq.setModuleType(type);
                    fq.setSortIndex(++questionIndex);
                    fq.setQuestionTitle(q.getQuestionTitle());
                    fq.setOptionA(q.getOptionA());
                    fq.setOptionB(q.getOptionB());
                    fq.setOptionC(q.getOptionC());
                    fq.setOptionD(q.getOptionD());
                    fq.setAnswer(q.getAnswer());
                    fq.setAnalysis(q.getAnalysis());
                    // 满分范例（口语/写作）一起复制，成绩页才能展示
                    fq.setSampleAnswer(q.getSampleAnswer());

                    if ("speaking".equals(type) || "writing".equals(type)) {
                        // 主观题：题面 + 素材拼成 prompt（答题页只渲染 prompt）
                        fq.setPrompt(buildPrompt(q, src));
                        fq.setTaskType(taskTypeOf(type, q.getQuestionTitle()));
                        fq.setLimitSecond("speaking".equals(type) ? 60 : 1200);
                        fq.setPrepareSecond("speaking".equals(type) ? prepareSecondsOf(q.getQuestionTitle()) : 0);
                    } else {
                        fq.setPrompt(null);
                        fq.setTaskType(null);
                        fq.setLimitSecond(0);
                        fq.setPrepareSecond(0);
                    }

                    fullQuestionMapper.insert(fq);

                    if ("reading".equals(type)) {
                        reading++;
                    } else if ("listening".equals(type)) {
                        listening++;
                    } else if ("speaking".equals(type)) {
                        speaking++;
                    } else {
                        writing++;
                    }
                }

                composition.add(MODULE_LABEL[indexOfType(type)] + "：" + fs.getTitle());
            }
        }

        // 题量太少时，描述里写清楚缺了多少篇（比如题库只有 2 篇阅读但想抽 3 篇）
        String desc = "从题库随机组合生成 · " + stamp;
        if (!shortage.isEmpty()) {
            StringBuilder sb = new StringBuilder(desc).append("（题库不足：");
            for (Map.Entry<String, Integer> entry : shortage.entrySet()) {
                sb.append(MODULE_LABEL[indexOfType(entry.getKey())])
                        .append(" 少 ")
                        .append(entry.getValue())
                        .append(" 篇，已按现有全部抽入；");
            }
            sb.append("）");
            desc = sb.toString();
        }
        suite.setDescription(desc.length() > 250 ? desc.substring(0, 250) : desc);
        fullSuiteMapper.updateById(suite);

        // ---------- 5. 返回给前端 ----------
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("suiteId", suiteId);
        data.put("title", suite.getTitle());
        data.put("description", suite.getDescription());
        data.put("readingCount", reading);
        data.put("listeningCount", listening);
        data.put("speakingCount", speaking);
        data.put("writingCount", writing);
        data.put("questionCount", reading + listening + speaking + writing);
        data.put("composition", composition);
        return Result.success(data);
    }

    // ===================================================================
    // 工具方法
    // ===================================================================

    /** 章节标题：阅读取正文第一句，其它模块给"听力篇章 #id"这类可读名称 */
    private String sectionTitle(TpoSection section, String type) {
        String text = section.getContentText();
        String label = MODULE_LABEL[indexOfType(type)];
        if (text != null && !text.trim().isEmpty()) {
            String first = text.trim().split("\n")[0].trim();
            if (first.length() > 22) {
                first = first.substring(0, 22) + "…";
            }
            return label + " · " + first;
        }
        return label + " 篇章 #" + section.getId();
    }

    private int moduleLimitSecond(String type) {
        // 阅读 / 听力按整段作答，不设单题限时；口语 / 写作的限时放在题目上
        if ("speaking".equals(type)) {
            return 0;
        }
        return 0;
    }

    private String hintOf(String type) {
        if ("reading".equals(type)) {
            return "先通读正文，再逐题作答；提交整卷后统一判分。";
        }
        if ("listening".equals(type)) {
            return "音频可反复收听，答题时看不到原文。";
        }
        if ("speaking".equals(type)) {
            return "把口述内容转写成文字后提交，AI 按新托福 1-6 档评分。";
        }
        return "写完作文再提交，AI 按新托福 1-6 档评分。";
    }

    /** 主观题 prompt = 题面 + 素材（阅读材料 / 听力要点） */
    private String buildPrompt(TpoQuestion q, TpoSection src) {
        StringBuilder sb = new StringBuilder();
        if (q.getQuestionTitle() != null) {
            sb.append(q.getQuestionTitle().trim());
        }
        String material = src.getContentText();
        if (material != null && !material.trim().isEmpty()) {
            sb.append("\n\n");
            sb.append(material.trim());
        }
        return sb.toString();
    }

    private String taskTypeOf(String type, String questionTitle) {
        String t = questionTitle == null ? "" : questionTitle;
        if ("speaking".equals(type)) {
            return t.contains("Integrated") ? "speaking_integrated" : "speaking_independent";
        }
        return t.contains("Integrated") ? "writing_integrated" : "writing_discussion";
    }

    private int prepareSecondsOf(String questionTitle) {
        return questionTitle != null && questionTitle.contains("Integrated") ? 20 : 15;
    }

    private int indexOfType(String type) {
        for (int i = 0; i < MODULE_ORDER.length; i++) {
            if (MODULE_ORDER[i].equals(type)) {
                return i;
            }
        }
        return 0;
    }

    private int readCount(Map<String, Object> body, String key, int fallback) {
        if (body == null || body.get(key) == null) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(String.valueOf(body.get(key)));
            return Math.max(0, Math.min(value, 5));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String readString(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        return String.valueOf(body.get(key));
    }
}
