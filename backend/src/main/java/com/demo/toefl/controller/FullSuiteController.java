package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.FullQuestion;
import com.demo.toefl.entity.FullSection;
import com.demo.toefl.entity.FullSuite;
import com.demo.toefl.mapper.FullQuestionMapper;
import com.demo.toefl.mapper.FullSectionMapper;
import com.demo.toefl.mapper.FullSuiteMapper;
import com.demo.toefl.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 真题套题目录接口（数据完全独立于 tpo_section / tpo_question）
 *
 *   GET /api/fullSuite/list       套题目录（带四个模块题量统计）
 *   GET /api/fullSuite/{id}       套题结构 + 题目（考试页一次性拿到全部内容）
 */
@RestController
@RequestMapping("/api/fullSuite")
public class FullSuiteController {

    @Resource
    private FullSuiteMapper fullSuiteMapper;

    @Resource
    private FullSectionMapper fullSectionMapper;

    @Resource
    private FullQuestionMapper fullQuestionMapper;

    private static final String[] MODULE_ORDER = {"reading", "listening", "speaking", "writing"};

    /** 套题目录 */
    @GetMapping("/list")
    public Result<List<FullSuite>> list() {
        List<FullSuite> suites = fullSuiteMapper.selectList(
                new LambdaQueryWrapper<FullSuite>().orderByAsc(FullSuite::getId));
        for (FullSuite suite : suites) {
            fillCounts(suite);
        }
        return Result.success(suites);
    }

    /** 套题详情：考试页需要 Module → Section → Question 三层结构 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        FullSuite suite = fullSuiteMapper.selectById(id);
        if (suite == null) {
            return Result.fail("套题不存在，suiteId=" + id);
        }

        List<FullSection> sections = fullSectionMapper.selectList(
                new LambdaQueryWrapper<FullSection>()
                        .eq(FullSection::getSuiteId, id)
                        .orderByAsc(FullSection::getSortIndex));

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

            for (FullSection section : sections) {
                if (!moduleType.equals(section.getModuleType())) {
                    continue;
                }
                List<FullQuestion> questions = fullQuestionMapper.selectList(
                        new LambdaQueryWrapper<FullQuestion>()
                                .eq(FullQuestion::getSectionId, section.getId())
                                .orderByAsc(FullQuestion::getSortIndex));
                questionCount += questions.size();

                Map<String, Object> sectionView = new LinkedHashMap<>();
                sectionView.put("sectionId", section.getId());
                sectionView.put("title", section.getTitle());
                sectionView.put("hint", section.getHint());
                sectionView.put("audioUrl", section.getAudioUrl());
                sectionView.put("limitSecond", section.getLimitSecond());
                // 阅读需要正文；听力不下发原文（原文只在成绩页出现）
                if ("reading".equals(moduleType)) {
                    sectionView.put("contentText", section.getContentText());
                }
                sectionView.put("questionCount", questions.size());
                // 注意：考试页不下发 answer / analysis，否则前端一打开就能看到答案
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

    /**
     * 考试页的题目视图：只给题面、选项、限时，不给标准答案与解析。
     * 判分统一在后端 /api/fullExam/submit 做，前端拿不到 answer。
     */
    private List<Map<String, Object>> toExamQuestions(List<FullQuestion> questions) {
        List<Map<String, Object>> views = new ArrayList<>();
        for (FullQuestion question : questions) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", question.getId());
            view.put("moduleType", question.getModuleType());
            view.put("sortIndex", question.getSortIndex());
            view.put("questionTitle", question.getQuestionTitle());
            view.put("optionA", question.getOptionA());
            view.put("optionB", question.getOptionB());
            view.put("optionC", question.getOptionC());
            view.put("optionD", question.getOptionD());
            view.put("prompt", question.getPrompt());
            view.put("taskType", question.getTaskType());
            view.put("limitSecond", question.getLimitSecond());
            view.put("prepareSecond", question.getPrepareSecond());
            view.put("scoreStandard", question.getScoreStandard());
            views.add(view);
        }
        return views;
    }

    /** 补四个模块的题量统计（列表页展示用） */
    private void fillCounts(FullSuite suite) {        int reading = 0;
        int listening = 0;
        int speaking = 0;
        int writing = 0;

        List<FullSection> sections = fullSectionMapper.selectList(
                new LambdaQueryWrapper<FullSection>().eq(FullSection::getSuiteId, suite.getId()));
        for (FullSection section : sections) {
            long count = fullQuestionMapper.selectCount(
                    new LambdaQueryWrapper<FullQuestion>().eq(FullQuestion::getSectionId, section.getId()));
            if ("reading".equals(section.getModuleType())) {
                reading += (int) count;
            } else if ("listening".equals(section.getModuleType())) {
                listening += (int) count;
            } else if ("speaking".equals(section.getModuleType())) {
                speaking += (int) count;
            } else if ("writing".equals(section.getModuleType())) {
                writing += (int) count;
            }
        }
        suite.setReadingCount(reading);
        suite.setListeningCount(listening);
        suite.setSpeakingCount(speaking);
        suite.setWritingCount(writing);
        suite.setQuestionCount(reading + listening + speaking + writing);
    }
}
