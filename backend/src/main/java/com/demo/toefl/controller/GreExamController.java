package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.GreExamAnswer;
import com.demo.toefl.entity.GreExamRecord;
import com.demo.toefl.entity.GreQuestion;
import com.demo.toefl.entity.GreSuite;
import com.demo.toefl.entity.GreSuiteQuestion;
import com.demo.toefl.entity.GreSuiteSection;
import com.demo.toefl.mapper.GreExamAnswerMapper;
import com.demo.toefl.mapper.GreExamRecordMapper;
import com.demo.toefl.mapper.GreSuiteMapper;
import com.demo.toefl.mapper.GreSuiteQuestionMapper;
import com.demo.toefl.mapper.GreSuiteSectionMapper;
import com.demo.toefl.service.GreAwaScoringService;
import com.demo.toefl.service.GreWrongService;
import com.demo.toefl.util.AuthHelper;
import com.demo.toefl.util.GreScoreUtil;
import org.springframework.beans.factory.annotation.Autowired;
import com.demo.toefl.util.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GRE 套题模考接口
 *
 *   POST   /api/greExam/start          开始模考，建一条 doing 记录
 *   POST   /api/greExam/submit         三个模块全部做完后一次性提交、统一判分
 *   GET    /api/greExam/records        我的模考列表
 *   GET    /api/greExam/record/{id}    成绩详情（含正确答案、解析、AWA 反馈与范文）
 *   DELETE /api/greExam/record/{id}    删除一条记录
 *
 * 判分口径：
 *   Verbal / Quant：正确率 → 130-170 量表分（见 GreScoreUtil）
 *   AWA：GreAwaScoringService 本地 rubric → 0-6（0.5 档）
 *   总分 = Verbal + Quant（260-340），AWA 单列
 */
@RestController
@RequestMapping("/api/greExam")
public class GreExamController {

    @Autowired
    private GreExamRecordMapper recordMapper;

    @Autowired
    private GreExamAnswerMapper answerMapper;

    @Autowired
    private GreSuiteMapper suiteMapper;

    @Autowired
    private GreSuiteSectionMapper sectionMapper;

    @Autowired
    private GreSuiteQuestionMapper questionMapper;

    @Autowired
    private GreAwaScoringService awaScoringService;

    /** wrong book: suite questions use question_scope='suite' */
    @Autowired
    private GreWrongService wrongService;

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String[] MODULE_ORDER = {"writing", "verbal", "quant"};

    // ==================================================================
    // 开始模考
    // ==================================================================
    @PostMapping("/start")
    public Result<GreExamRecord> start(@RequestBody(required = false) Map<String, Object> body,
                                       @RequestParam(required = false) Long suiteId,
                                       HttpServletRequest request) {
        Long sid = suiteId;
        if (sid == null && body != null && body.get("suiteId") != null) {
            sid = Long.valueOf(body.get("suiteId").toString());
        }
        if (sid == null) {
            return Result.fail("缺少套题id（suiteId）");
        }
        GreSuite suite = suiteMapper.selectById(sid);
        if (suite == null) {
            return Result.fail("套题不存在，suiteId=" + sid);
        }

        Long uid = authHelper.currentUserId(request);
        if (body != null && body.get("userId") != null
                && body.get("userId").toString().matches("\\d+")) {
            uid = Long.valueOf(body.get("userId").toString());
        }

        GreExamRecord record = new GreExamRecord();
        record.setUserId(uid);
        record.setSuiteId(sid);
        record.setSuiteTitle(suite.getTitle());
        record.setStatus("doing");
        record.setStartTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setCostSeconds(0);
        record.setVerbalCorrect(0);
        record.setVerbalTotal(0);
        record.setQuantCorrect(0);
        record.setQuantTotal(0);
        recordMapper.insert(record);
        return Result.success(record);
    }

    // ==================================================================
    // 整卷提交 + 统一判分
    // ==================================================================
    @Transactional
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        if (body == null || body.get("recordId") == null) {
            return Result.fail("缺少模考记录id（recordId），请先调用 /api/greExam/start");
        }
        Long recordId = Long.valueOf(body.get("recordId").toString());
        GreExamRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            return Result.fail("模考记录不存在，recordId=" + recordId);
        }
        GreSuite suite = suiteMapper.selectById(record.getSuiteId());
        if (suite == null) {
            return Result.fail("记录关联的套题不存在，suiteId=" + record.getSuiteId());
        }

        // 前端提交的答案：questionId → 作答文本
        Map<Long, String> submitted = new HashMap<>();
        Object answersObj = body.get("answers");
        if (answersObj instanceof List) {
            for (Object item : (List<?>) answersObj) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<?, ?> map = (Map<?, ?>) item;
                Object qid = map.get("questionId");
                Object ans = map.get("userAnswer");
                if (qid == null || ans == null) {
                    continue;
                }
                String text = ans.toString().trim();
                if (text.isEmpty()) {
                    continue;
                }
                submitted.put(Long.valueOf(qid.toString()), text);
            }
        }

        // 套题里的全部题目（按模块顺序读出来）
        List<GreSuiteSection> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<GreSuiteSection>()
                        .eq(GreSuiteSection::getSuiteId, suite.getId())
                        .orderByAsc(GreSuiteSection::getSortIndex));
        if (sections.isEmpty()) {
            return Result.fail("套题里没有任何模块，请检查 gre_suite_section");
        }

        // 重复提交先清旧明细
        answerMapper.delete(new LambdaQueryWrapper<GreExamAnswer>()
                .eq(GreExamAnswer::getRecordId, recordId));

        int verbalTotal = 0, verbalCorrect = 0, quantTotal = 0, quantCorrect = 0;
        List<BigDecimal> awaScores = new ArrayList<>();
        List<Map<String, Object>> awaFeedback = new ArrayList<>();

        for (GreSuiteSection section : sections) {
            List<GreSuiteQuestion> questions = questionMapper.selectList(
                    new LambdaQueryWrapper<GreSuiteQuestion>()
                            .eq(GreSuiteQuestion::getSectionId, section.getId())
                            .orderByAsc(GreSuiteQuestion::getSortIndex));
            for (GreSuiteQuestion question : questions) {
                String moduleType = question.getModuleType();
                String userAnswer = submitted.get(question.getId());
                boolean answered = userAnswer != null && !userAnswer.trim().isEmpty();
                if (!answered) {
                    userAnswer = "";
                }

                if ("verbal".equals(moduleType)) {
                    verbalTotal++;
                } else if ("quant".equals(moduleType)) {
                    quantTotal++;
                }

                GreExamAnswer row = new GreExamAnswer();
                row.setRecordId(recordId);
                row.setQuestionId(question.getId());
                row.setModuleType(moduleType);
                row.setUserAnswer(userAnswer);
                row.setCreateTime(LocalDateTime.now());

                if ("writing".equals(moduleType)) {
                    BigDecimal score = null;
                    if (answered) {
                        // AWA 只在真的写了才评分；没写就留空，成绩页显示"未作答"
                        Map<String, Object> feedback = awaScoringService.scoreByTaskType(
                                question.getTaskType(), question.getPrompt(), userAnswer);
                        score = (BigDecimal) feedback.get("score");
                        awaScores.add(score);
                        row.setAiFeedback(serializeFeedback(feedback));

                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("questionId", question.getId());
                        item.put("score", score);
                        item.put("feedback", feedback);
                        awaFeedback.add(item);
                    }
                    row.setAwaScore(score);
                    row.setIsCorrect(null);
                    row.setCorrectAnswer(null);
                } else {
                    boolean correct = answered && question.getAnswer() != null
                            && question.getAnswer().trim().equalsIgnoreCase(userAnswer.trim());
                    if (correct) {
                        if ("verbal".equals(moduleType)) {
                            verbalCorrect++;
                        } else if ("quant".equals(moduleType)) {
                            quantCorrect++;
                        }
                    }
                    row.setCorrectAnswer(question.getAnswer());
                    row.setIsCorrect(correct ? 1 : 0);
                    // wrong book: record wrong / release on correct (scope=suite)
                    if (!answered) {
                        // skip unanswered: only answered-but-wrong goes to the wrong book
                    } else if (correct) {
                        wrongService.releaseSuite(record.getUserId(), question.getId());
                    } else {
                        wrongService.recordSuiteWrong(record.getUserId(), question, userAnswer);
                    }
                }
                answerMapper.insert(row);
            }
        }

        int verbalScaled = GreScoreUtil.scaled(verbalCorrect, verbalTotal);
        int quantScaled = GreScoreUtil.scaled(quantCorrect, quantTotal);
        int totalScore = GreScoreUtil.totalScore(verbalScaled, quantScaled);
        BigDecimal awaScore = GreScoreUtil.average(awaScores);

        int costSeconds = 0;
        Object costObj = body.get("costSeconds");
        if (costObj != null) {
            try {
                costSeconds = Integer.parseInt(costObj.toString());
            } catch (NumberFormatException e) {
                costSeconds = 0;
            }
        }
        if (costSeconds <= 0 && record.getStartTime() != null) {
            long seconds = Duration.between(record.getStartTime(), LocalDateTime.now()).getSeconds();
            if (seconds > 0) {
                costSeconds = (int) seconds;
            }
        }

        record.setStatus("finished");
        record.setEndTime(LocalDateTime.now());
        record.setCostSeconds(costSeconds);
        record.setVerbalCorrect(verbalCorrect);
        record.setVerbalTotal(verbalTotal);
        record.setQuantCorrect(quantCorrect);
        record.setQuantTotal(quantTotal);
        record.setVerbalScaled(verbalScaled);
        record.setQuantScaled(quantScaled);
        record.setAwaScore(awaScore);
        record.setTotalScore(totalScore);
        recordMapper.updateById(record);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordId", recordId);
        data.put("verbalScaled", verbalScaled);
        data.put("quantScaled", quantScaled);
        data.put("totalScore", totalScore);
        data.put("awaScore", awaScore);
        data.put("verbalCorrect", verbalCorrect);
        data.put("verbalTotal", verbalTotal);
        data.put("quantCorrect", quantCorrect);
        data.put("quantTotal", quantTotal);
        data.put("costSeconds", costSeconds);
        data.put("verbalComment", GreScoreUtil.scaledComment(verbalScaled));
        data.put("quantComment", GreScoreUtil.scaledComment(quantScaled));
        data.put("awaComment", awaScore == null ? null : GreScoreUtil.awaComment(awaScore.doubleValue()));
        data.put("awaFeedback", awaFeedback);
        data.put("note", "量表分为教学近似换算，非官方预测分");
        return Result.success(data);
    }

    // ==================================================================
    // 我的记录
    // ==================================================================
    @GetMapping("/records")
    public Result<List<GreExamRecord>> records(@RequestParam(required = false) Long userId,
                                               @RequestParam(required = false) String status,
                                               HttpServletRequest request) {
        Long uid = userId != null ? userId : authHelper.currentUserId(request);
        LambdaQueryWrapper<GreExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GreExamRecord::getUserId, uid);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(GreExamRecord::getStatus, status);
        }
        wrapper.orderByDesc(GreExamRecord::getCreateTime).orderByDesc(GreExamRecord::getId);
        return Result.success(recordMapper.selectList(wrapper));
    }

    // ==================================================================
    // 成绩详情：整卷每题都展示（未作答显示"我的答案：空"）
    // ==================================================================
    @GetMapping("/record/{id}")
    public Result<Map<String, Object>> recordDetail(@PathVariable Long id) {
        GreExamRecord record = recordMapper.selectById(id);
        if (record == null) {
            return Result.fail("记录不存在，recordId=" + id);
        }
        GreSuite suite = suiteMapper.selectById(record.getSuiteId());

        List<GreSuiteSection> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<GreSuiteSection>()
                        .eq(GreSuiteSection::getSuiteId, record.getSuiteId())
                        .orderByAsc(GreSuiteSection::getSortIndex));

        List<GreExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<GreExamAnswer>()
                        .eq(GreExamAnswer::getRecordId, id)
                        .orderByAsc(GreExamAnswer::getId));
        Map<Long, GreExamAnswer> answerMap = new HashMap<>();
        for (GreExamAnswer answer : answers) {
            answerMap.put(answer.getQuestionId(), answer);
        }

        List<Map<String, Object>> moduleViews = new ArrayList<>();
        for (String moduleType : MODULE_ORDER) {
            List<Map<String, Object>> answerViews = new ArrayList<>();
            for (GreSuiteSection section : sections) {
                if (!moduleType.equals(section.getModuleType())) {
                    continue;
                }
                List<GreSuiteQuestion> moduleQuestions = questionMapper.selectList(
                        new LambdaQueryWrapper<GreSuiteQuestion>()
                                .eq(GreSuiteQuestion::getSectionId, section.getId())
                                .orderByAsc(GreSuiteQuestion::getSortIndex));
                for (GreSuiteQuestion question : moduleQuestions) {
                    GreExamAnswer answer = answerMap.get(question.getId());
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("questionId", question.getId());
                    view.put("sectionId", section.getId());
                    view.put("moduleType", moduleType);
                    view.put("sectionTitle", section.getTitle());
                    view.put("sortIndex", question.getSortIndex());
                    view.put("questionTitle", question.getQuestionTitle());
                    view.put("optionA", question.getOptionA());
                    view.put("optionB", question.getOptionB());
                    view.put("optionC", question.getOptionC());
                    view.put("optionD", question.getOptionD());
                    view.put("optionE", question.getOptionE());
                    view.put("optionF", question.getOptionF());
                    view.put("analysis", question.getAnalysis());
                    view.put("prompt", question.getPrompt());
                    view.put("taskType", question.getTaskType());
                    view.put("scoreStandard", question.getScoreStandard());
                    // 写作满分范例只在成绩页下发
                    view.put("sampleAnswer", question.getSampleAnswer());

                    if (answer == null) {
                        view.put("userAnswer", "");
                        view.put("correctAnswer", question.getAnswer());
                        view.put("isCorrect", null);
                        view.put("awaScore", null);
                        view.put("aiFeedback", null);
                    } else {
                        view.put("userAnswer", answer.getUserAnswer());
                        view.put("correctAnswer", answer.getCorrectAnswer() == null
                                ? question.getAnswer() : answer.getCorrectAnswer());
                        view.put("isCorrect", answer.getIsCorrect());
                        view.put("awaScore", answer.getAwaScore());
                        view.put("aiFeedback", parseFeedback(answer.getAiFeedback()));
                    }
                    answerViews.add(view);
                }
            }
            Map<String, Object> moduleView = new LinkedHashMap<>();
            moduleView.put("moduleType", moduleType);
            moduleView.put("label", moduleLabel(moduleType));
            moduleView.put("answers", answerViews);
            moduleViews.add(moduleView);
        }

        // 阅读正文：成绩页也带上，方便回看
        List<Map<String, Object>> sectionViews = new ArrayList<>();
        for (GreSuiteSection section : sections) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("sectionId", section.getId());
            view.put("moduleType", section.getModuleType());
            view.put("title", section.getTitle());
            view.put("contentText", section.getContentText());
            sectionViews.add(view);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("record", record);
        data.put("suiteTitle", suite == null ? record.getSuiteTitle() : suite.getTitle());
        data.put("modules", moduleViews);
        data.put("sections", sectionViews);
        data.put("verbalComment", record.getVerbalScaled() == null ? null
                : GreScoreUtil.scaledComment(record.getVerbalScaled()));
        data.put("quantComment", record.getQuantScaled() == null ? null
                : GreScoreUtil.scaledComment(record.getQuantScaled()));
        data.put("awaComment", record.getAwaScore() == null ? null
                : GreScoreUtil.awaComment(record.getAwaScore().doubleValue()));
        return Result.success(data);
    }

    // ==================================================================
    // 删除记录
    // ==================================================================
    @Transactional
    @DeleteMapping("/record/{id}")
    public Result<String> deleteRecord(@PathVariable Long id) {
        GreExamRecord record = recordMapper.selectById(id);
        if (record == null) {
            return Result.fail("记录不存在，recordId=" + id);
        }
        answerMapper.delete(new LambdaQueryWrapper<GreExamAnswer>()
                .eq(GreExamAnswer::getRecordId, id));
        recordMapper.deleteById(id);
        return Result.success("已删除");
    }

    // ==================================================================
    // 内部方法
    // ==================================================================

    private String serializeFeedback(Object feedback) {
        if (feedback == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (Exception e) {
            // 序列化失败就存空，页面显示"暂无反馈"，不影响分数
            return null;
        }
    }

    private Object parseFeedback(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return json;
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
