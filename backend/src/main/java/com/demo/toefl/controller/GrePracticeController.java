package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.GreAnswer;
import com.demo.toefl.entity.GreQuestion;
import com.demo.toefl.entity.GreSection;
import com.demo.toefl.mapper.GreAnswerMapper;
import com.demo.toefl.mapper.GreQuestionMapper;
import com.demo.toefl.mapper.GreSectionMapper;
import com.demo.toefl.service.GreAwaScoringService;
import com.demo.toefl.service.GreWrongService;
import com.demo.toefl.util.AuthHelper;
import org.springframework.beans.factory.annotation.Autowired;
import com.demo.toefl.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GRE 分块练习接口（数据只读 gre_section / gre_question，与套题表无关）
 *
 *   GET  /api/gre/practice/catalog          分块目录：三个模块 + 每个模块的练习单元
 *   GET  /api/gre/practice/section/{id}     某个练习单元的题目（不含答案与解析）
 *   POST /api/gre/practice/answer           单题提交：客观题判对错，写作题走 AWA 评分
 */
@RestController
@RequestMapping("/api/gre/practice")
public class GrePracticeController {

    @Autowired
    private GreSectionMapper sectionMapper;

    @Autowired
    private GreQuestionMapper questionMapper;

    @Autowired
    private GreAnswerMapper answerMapper;

    @Autowired
    private GreAwaScoringService awaScoringService;

    /** wrong book: record wrong / release on correct */
    @Autowired
    private GreWrongService wrongService;

    @Autowired
    private AuthHelper authHelper;

    private static final String[] MODULE_ORDER = {"writing", "verbal", "quant"};

    // ==================================================================
    // 分块目录
    // ==================================================================
    @GetMapping("/catalog")
    public Result<Map<String, Object>> catalog() {
        List<Map<String, Object>> modules = new ArrayList<>();
        int total = 0;

        for (String moduleType : MODULE_ORDER) {
            List<GreSection> sections = sectionMapper.selectList(
                    new LambdaQueryWrapper<GreSection>()
                            .eq(GreSection::getModuleType, moduleType)
                            .orderByAsc(GreSection::getSortIndex));

            List<Map<String, Object>> sectionViews = new ArrayList<>();
            int moduleQuestions = 0;
            for (GreSection section : sections) {
                // selectCount 返回 Long，先拆箱再转 int（直接 (int) 会报 "Long cannot be converted to int"）
                int count = (int) (long) questionMapper.selectCount(
                        new LambdaQueryWrapper<GreQuestion>()
                                .eq(GreQuestion::getSectionId, section.getId()));
                moduleQuestions += count;

                Map<String, Object> view = new LinkedHashMap<>();
                view.put("sectionId", section.getId());
                view.put("title", section.getTitle());
                view.put("subtitle", section.getSubtitle());
                view.put("hint", section.getHint());
                view.put("difficulty", section.getDifficulty());
                view.put("limitSecond", section.getLimitSecond());
                view.put("questionCount", count);
                view.put("hasPassage", section.getContentText() != null
                        && !section.getContentText().trim().isEmpty());
                sectionViews.add(view);
            }
            total += moduleQuestions;

            Map<String, Object> moduleView = new LinkedHashMap<>();
            moduleView.put("moduleType", moduleType);
            moduleView.put("label", moduleLabel(moduleType));
            moduleView.put("questionCount", moduleQuestions);
            moduleView.put("sections", sectionViews);
            modules.add(moduleView);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("modules", modules);
        data.put("totalQuestions", total);
        data.put("sectionCount", sectionMapper.selectCount(null));
        return Result.success(data);
    }

    // ==================================================================
    // 一个练习单元的题目
    // ==================================================================
    @GetMapping("/section/{id}")
    public Result<Map<String, Object>> section(@PathVariable Long id) {
        GreSection section = sectionMapper.selectById(id);
        if (section == null) {
            return Result.fail("练习单元不存在，sectionId=" + id);
        }
        List<GreQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<GreQuestion>()
                        .eq(GreQuestion::getSectionId, id)
                        .orderByAsc(GreQuestion::getSortIndex));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sectionId", section.getId());
        data.put("moduleType", section.getModuleType());
        data.put("title", section.getTitle());
        data.put("subtitle", section.getSubtitle());
        data.put("contentText", section.getContentText());
        data.put("hint", section.getHint());
        data.put("difficulty", section.getDifficulty());
        data.put("limitSecond", section.getLimitSecond());

        List<Map<String, Object>> questionViews = new ArrayList<>();
        for (GreQuestion question : questions) {
            questionViews.add(toExamQuestion(question));
        }
        data.put("questions", questionViews);
        data.put("questionCount", questionViews.size());
        return Result.success(data);
    }

    // ==================================================================
    // 单题提交
    // ==================================================================
    @PostMapping("/answer")
    public Result<Map<String, Object>> answer(@RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        Object qidObj = body == null ? null : body.get("questionId");
        if (qidObj == null) {
            return Result.fail("缺少题目id（questionId）");
        }
        Long questionId = Long.valueOf(qidObj.toString());
        GreQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            return Result.fail("题目不存在，questionId=" + questionId);
        }

        String rawAnswer = body.get("userAnswer") == null ? "" : body.get("userAnswer").toString().trim();
        boolean answerEmpty = rawAnswer.isEmpty();
        if (answerEmpty) {
            return Result.fail("请先作答再提交");
        }

        Long uid = authHelper.currentUserId(request);
        if (body.get("userId") != null && body.get("userId").toString().matches("\\d+")) {
            uid = Long.valueOf(body.get("userId").toString());
        }

        boolean isWriting = question.getTaskType() != null
                && question.getTaskType().toLowerCase().startsWith("awa");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("questionId", questionId);
        data.put("objective", !isWriting);
        data.put("answered", true);

        if (isWriting) {
            Map<String, Object> feedback = awaScoringService.scoreByTaskType(
                    question.getTaskType(), question.getPrompt(), rawAnswer);
            BigDecimal score = (BigDecimal) feedback.get("score");
            data.put("awaScore", score);
            data.put("feedback", feedback);
            // 写作提交后才给满分范例，做题时看不到
            data.put("sampleAnswer", question.getSampleAnswer());
            data.put("correctAnswer", null);
            data.put("analysis", question.getScoreStandard());
            saveAnswer(uid, questionId, rawAnswer, null, score);
        } else {
            String correctAnswer = question.getAnswer();
            boolean correct = correctAnswer != null
                    && correctAnswer.trim().equalsIgnoreCase(rawAnswer.trim());
            data.put("correct", correct);
            data.put("correctAnswer", correctAnswer);
            data.put("analysis", question.getAnalysis());
            data.put("awaScore", null);
            data.put("feedback", null);
            data.put("sampleAnswer", null);
            saveAnswer(uid, questionId, rawAnswer, correct ? 1 : 0, null);
            // 错题本：答错写入/累加，答对则直接从错题本移除
            if (correct) {
                wrongService.releasePractice(uid, questionId);
            } else {
                wrongService.recordPracticeWrong(uid, question, rawAnswer);
            }
        }
        return Result.success(data);
    }

    // ==================================================================
    // 内部方法
    // ==================================================================

    /** 做题页题目视图：不给答案、不给解析、不给范例，避免提前看到结果 */
    private Map<String, Object> toExamQuestion(GreQuestion question) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", question.getId());
        view.put("sortIndex", question.getSortIndex());
        view.put("moduleType", question.getModuleType());
        view.put("questionTitle", question.getQuestionTitle());
        view.put("optionA", question.getOptionA());
        view.put("optionB", question.getOptionB());
        view.put("optionC", question.getOptionC());
        view.put("optionD", question.getOptionD());
        view.put("optionE", question.getOptionE());
        view.put("optionF", question.getOptionF());
        view.put("prompt", question.getPrompt());
        view.put("taskType", question.getTaskType());
        view.put("scoreStandard", question.getScoreStandard());
        view.put("difficulty", question.getDifficulty());

        // 选项数组：前端直接 v-for，不用判断 optionA..F 哪个是空
        List<Map<String, Object>> options = new ArrayList<>();
        String[] keys = {"A", "B", "C", "D", "E", "F"};
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
        return view;
    }

    /** 同一用户同一题只留一条：先更新，没有则插入 */
    private void saveAnswer(Long userId, Long questionId, String userAnswer, Integer isCorrect,
                            BigDecimal awaScore) {
        GreAnswer exist = answerMapper.selectOne(
                new LambdaQueryWrapper<GreAnswer>()
                        .eq(GreAnswer::getUserId, userId)
                        .eq(GreAnswer::getQuestionId, questionId)
                        .last("limit 1"));
        if (exist != null) {
            exist.setUserAnswer(userAnswer);
            exist.setIsCorrect(isCorrect);
            exist.setAwaScore(awaScore);
            answerMapper.updateById(exist);
        } else {
            GreAnswer row = new GreAnswer();
            row.setUserId(userId);
            row.setQuestionId(questionId);
            row.setUserAnswer(userAnswer);
            row.setIsCorrect(isCorrect);
            row.setAwaScore(awaScore);
            row.setCreateTime(LocalDateTime.now());
            answerMapper.insert(row);
        }
    }

    private String moduleLabel(String moduleType) {
        switch (moduleType) {
            case "writing":
                return "Analytical Writing";
            case "verbal":
                return "Verbal Reasoning";
            case "quant":
                return "Quantitative Reasoning";
            default:
                return moduleType;
        }
    }
}
