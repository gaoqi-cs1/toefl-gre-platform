package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.GreQuestion;
import com.demo.toefl.entity.GreSection;
import com.demo.toefl.entity.GreSuite;
import com.demo.toefl.entity.GreSuiteQuestion;
import com.demo.toefl.entity.GreSuiteSection;
import com.demo.toefl.mapper.GreQuestionMapper;
import com.demo.toefl.mapper.GreSectionMapper;
import com.demo.toefl.mapper.GreSuiteMapper;
import com.demo.toefl.mapper.GreSuiteQuestionMapper;
import com.demo.toefl.mapper.GreSuiteSectionMapper;
import com.demo.toefl.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GRE 套题目录接口（只读 gre_suite / gre_suite_section / gre_suite_question）
 *
 *   GET    /api/greSuite/list        套题目录（带三个模块题量）
 *   GET    /api/greSuite/{id}        套题结构 + 题目（考试页一次性拿到）
 *   POST   /api/greSuite/generate   一键生成套题：从分块练习题库随机抽题，复制进套题表
 *   DELETE /api/greSuite/{id}       删除「一键生成」的套题（预置套题不允许删）
 *
 * 注意：生成动作是「把练习题库的题复制一份到套题表」，之后改练习库不会影响已生成的套题；
 * 套题的作答与判分只碰 gre_exam_* 表，分块练习只碰 gre_answer，两条链路互不调用。
 */
@RestController
@RequestMapping("/api/greSuite")
public class GreSuiteController {

    @Autowired
    private GreSuiteMapper suiteMapper;

    @Autowired
    private GreSuiteSectionMapper suiteSectionMapper;

    @Autowired
    private GreSuiteQuestionMapper suiteQuestionMapper;

    @Autowired
    private GreSectionMapper sectionMapper;

    @Autowired
    private GreQuestionMapper questionMapper;

    private static final String[] MODULE_ORDER = {"writing", "verbal", "quant"};

    // ==================================================================
    // 套题目录
    // ==================================================================
    @GetMapping("/list")
    public Result<List<GreSuite>> list() {
        List<GreSuite> suites = suiteMapper.selectList(
                new LambdaQueryWrapper<GreSuite>().orderByAsc(GreSuite::getId));
        for (GreSuite suite : suites) {
            fillCounts(suite);
        }
        return Result.success(suites);
    }

    // ==================================================================
    // 套题详情（考试页用）
    // ==================================================================
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        GreSuite suite = suiteMapper.selectById(id);
        if (suite == null) {
            return Result.fail("套题不存在，suiteId=" + id);
        }
        List<GreSuiteSection> sections = suiteSectionMapper.selectList(
                new LambdaQueryWrapper<GreSuiteSection>()
                        .eq(GreSuiteSection::getSuiteId, id)
                        .orderByAsc(GreSuiteSection::getSortIndex));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("suiteId", suite.getId());
        data.put("title", suite.getTitle());
        data.put("description", suite.getDescription());
        data.put("difficulty", suite.getDifficulty());
        data.put("totalMinutes", suite.getTotalMinutes());

        List<Map<String, Object>> modules = new ArrayList<>();
        for (String moduleType : MODULE_ORDER) {
            List<Map<String, Object>> sectionViews = new ArrayList<>();
            int questionCount = 0;

            for (GreSuiteSection section : sections) {
                if (!moduleType.equals(section.getModuleType())) {
                    continue;
                }
                List<GreSuiteQuestion> questions = suiteQuestionMapper.selectList(
                        new LambdaQueryWrapper<GreSuiteQuestion>()
                                .eq(GreSuiteQuestion::getSectionId, section.getId())
                                .orderByAsc(GreSuiteQuestion::getSortIndex));
                questionCount += questions.size();

                Map<String, Object> sectionView = new LinkedHashMap<>();
                sectionView.put("sectionId", section.getId());
                sectionView.put("title", section.getTitle());
                sectionView.put("hint", section.getHint());
                sectionView.put("contentText", section.getContentText());
                sectionView.put("limitSecond", section.getLimitSecond());
                sectionView.put("questionCount", questions.size());
                sectionView.put("questions", toExamQuestions(questions));
                sectionViews.add(sectionView);
            }

            Map<String, Object> moduleView = new LinkedHashMap<>();
            moduleView.put("moduleType", moduleType);
            moduleView.put("questionCount", questionCount);
            moduleView.put("sections", sectionViews);
            modules.add(moduleView);
        }

        data.put("modules", modules);
        return Result.success(data);
    }

    // ==================================================================
    // 一键生成套题
    // ==================================================================
    @Transactional
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody(required = false) Map<String, Object> body) {
        int writingCount = intValue(body, "writing", 2, 1, 4);
        int verbalCount = intValue(body, "verbal", 10, 4, 20);
        int quantCount = intValue(body, "quant", 10, 4, 20);

        // 1) 建套题
        GreSuite suite = new GreSuite();
        suite.setTitle("GRE 随机套题 · " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
        suite.setDescription("从分块题库随机抽取：Analytical Writing " + writingCount
                + " 篇 + Verbal " + verbalCount + " 题 + Quant " + quantCount + " 题");
        suite.setDifficulty("中级");
        suite.setTotalMinutes(130);
        suite.setSource("generated");
        suite.setCreateTime(LocalDateTime.now());
        suiteMapper.insert(suite);

        int seq = 1;

        // 2) 写作：整个放进一个 section
        List<GreQuestion> writingQuestions = randomQuestions("writing", writingCount);
        if (!writingQuestions.isEmpty()) {
            seq = createSection(suite.getId(), "writing", seq,
                    "Analytical Writing · Issue + Argument",
                    null, "两篇各 30 分钟：先花 3 分钟列提纲", 3600, writingQuestions);
        }

        // 3) Verbal：按来源单元分组，文章跟着题目一起复制（RC 必须有正文）
        seq = copyGrouped(suite.getId(), "verbal", seq, randomQuestions("verbal", verbalCount));

        // 4) Quant：同理，按来源单元分组（数量比较 / 问题求解各成一段）
        copyGrouped(suite.getId(), "quant", seq, randomQuestions("quant", quantCount));

        fillCounts(suite);
        return Result.success(toData(suite));
    }

    // ==================================================================
    // 删除一键生成的套题
    // ==================================================================
    @Transactional
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        GreSuite suite = suiteMapper.selectById(id);
        if (suite == null) {
            return Result.fail("套题不存在，suiteId=" + id);
        }
        if (!"generated".equals(suite.getSource())) {
            return Result.fail("预置套题不允许删除；只清理「一键生成」的套题");
        }
        List<GreSuiteSection> sections = suiteSectionMapper.selectList(
                new LambdaQueryWrapper<GreSuiteSection>().eq(GreSuiteSection::getSuiteId, id));
        for (GreSuiteSection section : sections) {
            suiteQuestionMapper.delete(new LambdaQueryWrapper<GreSuiteQuestion>()
                    .eq(GreSuiteQuestion::getSectionId, section.getId()));
            suiteSectionMapper.deleteById(section.getId());
        }
        suiteMapper.deleteById(id);
        return Result.success("已删除");
    }

    // ==================================================================
    // 内部方法
    // ==================================================================

    /** 把一批题按「来源练习单元」分组复制成套题内的若干 section */
    private int copyGrouped(Long suiteId, String moduleType, int seq, List<GreQuestion> questions) {
        if (questions.isEmpty()) {
            return seq;
        }
        // 按 sectionId 分组，保持原单元标题与正文
        Map<Long, List<GreQuestion>> groups = new LinkedHashMap<>();
        for (GreQuestion question : questions) {
            groups.computeIfAbsent(question.getSectionId(), k -> new ArrayList<>()).add(question);
        }
        for (Map.Entry<Long, List<GreQuestion>> entry : groups.entrySet()) {
            GreSection source = sectionMapper.selectById(entry.getKey());
            String title = source == null ? moduleType.toUpperCase() + " · 随机题组" : source.getTitle();
            String contentText = source == null ? null : source.getContentText();
            int limit = "verbal".equals(moduleType) ? 1080 : 1260;
            String hint = source == null ? null : source.getHint();
            seq = createSection(suiteId, moduleType, seq, title, contentText, hint, limit, entry.getValue());
        }
        return seq;
    }

    /** 建一个套题 section，并把题目复制进去；返回下一个可用 sortIndex */
    private int createSection(Long suiteId, String moduleType, int seq, String title,
                              String contentText, String hint, int limitSecond,
                              List<GreQuestion> questions) {
        GreSuiteSection section = new GreSuiteSection();
        section.setSuiteId(suiteId);
        section.setModuleType(moduleType);
        section.setSortIndex(seq);
        section.setTitle(title);
        section.setContentText(contentText);
        section.setHint(hint);
        section.setLimitSecond(limitSecond);
        suiteSectionMapper.insert(section);

        int index = 1;
        for (GreQuestion source : questions) {
            GreSuiteQuestion copy = new GreSuiteQuestion();
            copy.setSectionId(section.getId());
            copy.setModuleType(source.getModuleType());
            copy.setSortIndex(index++);
            copy.setQuestionTitle(source.getQuestionTitle());
            copy.setOptionA(source.getOptionA());
            copy.setOptionB(source.getOptionB());
            copy.setOptionC(source.getOptionC());
            copy.setOptionD(source.getOptionD());
            copy.setOptionE(source.getOptionE());
            copy.setOptionF(source.getOptionF());
            copy.setAnswer(source.getAnswer());
            copy.setAnalysis(source.getAnalysis());
            copy.setPrompt(source.getPrompt());
            copy.setTaskType(source.getTaskType());
            copy.setScoreStandard(source.getScoreStandard());
            copy.setSampleAnswer(source.getSampleAnswer());
            copy.setDifficulty(source.getDifficulty());
            suiteQuestionMapper.insert(copy);
        }
        return seq + 1;
    }

    /** 从分块题库里随机抽 n 题（不放回） */
    private List<GreQuestion> randomQuestions(String moduleType, int count) {
        List<GreQuestion> pool = questionMapper.selectList(
                new LambdaQueryWrapper<GreQuestion>().eq(GreQuestion::getModuleType, moduleType));
        Collections.shuffle(pool);
        if (pool.size() <= count) {
            return pool;
        }
        return new ArrayList<>(pool.subList(0, count));
    }

    /** 做题页题目视图：不下发答案、解析、范文 */
    private List<Map<String, Object>> toExamQuestions(List<GreSuiteQuestion> questions) {
        List<Map<String, Object>> views = new ArrayList<>();
        String[] keys = {"A", "B", "C", "D", "E", "F"};
        for (GreSuiteQuestion question : questions) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", question.getId());
            view.put("sortIndex", question.getSortIndex());
            view.put("moduleType", question.getModuleType());
            view.put("questionTitle", question.getQuestionTitle());
            view.put("prompt", question.getPrompt());
            view.put("taskType", question.getTaskType());
            view.put("scoreStandard", question.getScoreStandard());

            List<Map<String, Object>> options = new ArrayList<>();
            String[] values = {question.getOptionA(), question.getOptionB(), question.getOptionC(),
                    question.getOptionD(), question.getOptionE(), question.getOptionF()};
            for (int i = 0; i < keys.length; i++) {
                if (values[i] == null || values[i].trim().isEmpty()) {
                    continue;
                }
                Map<String, Object> option = new LinkedHashMap<>();
                option.put("key", keys[i]);
                option.put("text", values[i]);
                options.add(option);
            }
            view.put("options", options);
            views.add(view);
        }
        return views;
    }

    private void fillCounts(GreSuite suite) {
        int writing = 0, verbal = 0, quant = 0;
        List<GreSuiteSection> sections = suiteSectionMapper.selectList(
                new LambdaQueryWrapper<GreSuiteSection>().eq(GreSuiteSection::getSuiteId, suite.getId()));
        for (GreSuiteSection section : sections) {
            // selectCount 返回 Long，先拆箱再转 int
            int count = (int) (long) suiteQuestionMapper.selectCount(
                    new LambdaQueryWrapper<GreSuiteQuestion>()
                            .eq(GreSuiteQuestion::getSectionId, section.getId()));
            if ("writing".equals(section.getModuleType())) {
                writing += count;
            } else if ("verbal".equals(section.getModuleType())) {
                verbal += count;
            } else if ("quant".equals(section.getModuleType())) {
                quant += count;
            }
        }
        suite.setWritingCount(writing);
        suite.setVerbalCount(verbal);
        suite.setQuantCount(quant);
        suite.setQuestionCount(writing + verbal + quant);
    }

    private Map<String, Object> toData(GreSuite suite) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("suiteId", suite.getId());
        data.put("title", suite.getTitle());
        data.put("writingCount", suite.getWritingCount());
        data.put("verbalCount", suite.getVerbalCount());
        data.put("quantCount", suite.getQuantCount());
        data.put("questionCount", suite.getQuestionCount());
        return data;
    }

    private int intValue(Map<String, Object> body, String key, int defaultValue, int min, int max) {
        if (body == null || body.get(key) == null) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(body.get(key).toString());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
