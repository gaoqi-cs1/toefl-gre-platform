package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.dto.FullSubmitDTO;
import com.demo.toefl.entity.FullExamAnswer;
import com.demo.toefl.entity.FullExamRecord;
import com.demo.toefl.entity.FullQuestion;
import com.demo.toefl.entity.FullSection;
import com.demo.toefl.entity.FullSuite;
import com.demo.toefl.mapper.FullExamAnswerMapper;
import com.demo.toefl.mapper.FullExamRecordMapper;
import com.demo.toefl.mapper.FullQuestionMapper;
import com.demo.toefl.mapper.FullSectionMapper;
import com.demo.toefl.mapper.FullSuiteMapper;
import com.demo.toefl.service.AiScoringService;
import com.demo.toefl.util.AuthHelper;
import com.demo.toefl.util.FullScoreUtil;
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

import javax.annotation.Resource;
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
 * 真题套题模考接口
 *
 *   POST /api/fullExam/start             开始模考，建一条 doing 记录
 *   POST /api/fullExam/submit            四个模块全部做完后一次性提交、统一判分
 *   GET  /api/fullExam/records?userId=   我的套题成绩列表
 *   GET  /api/fullExam/record/{id}       成绩详情（含 AI 反馈、正确答案、解析）
 *   DELETE /api/fullExam/record/{id}     删除一条记录
 *
 * 判分口径：
 *   阅读 / 听力：正确率 → 1-6 档（FullScoreUtil.bandFromCorrect）
 *   口语 / 写作：AiScoringService 本地 rubric 引擎 → 1-6 档 + 维度反馈
 *   总分 = 四项平均；同时给 0-120 兼容分
 */
@RestController
@RequestMapping("/api/fullExam")
public class FullExamController {

    @Resource
    private FullExamRecordMapper recordMapper;

    @Resource
    private FullExamAnswerMapper answerMapper;

    @Resource
    private FullSuiteMapper suiteMapper;

    @Resource
    private FullSectionMapper sectionMapper;

    @Resource
    private FullQuestionMapper questionMapper;

    @Resource
    private AiScoringService aiScoringService;

    @Resource
    private AuthHelper authHelper;

    @Resource
    private ObjectMapper objectMapper;

    private static final String[] MODULE_ORDER = {"reading", "listening", "speaking", "writing"};

    // ==================================================================
    // 开始模考
    // ==================================================================
    @PostMapping("/start")
    public Result<FullExamRecord> start(@RequestBody(required = false) Map<String, Object> body,
                                        @RequestParam(required = false) Long suiteId,
                                        HttpServletRequest request) {
        Long sid = suiteId;
        if (sid == null && body != null && body.get("suiteId") != null) {
            sid = Long.valueOf(body.get("suiteId").toString());
        }
        if (sid == null) {
            return Result.fail("缺少套题id（suiteId）");
        }
        FullSuite suite = suiteMapper.selectById(sid);
        if (suite == null) {
            return Result.fail("套题不存在，suiteId=" + sid);
        }

        Long uid = authHelper.currentUserId(request);
        if (body != null && body.get("userId") != null) {
            uid = Long.valueOf(body.get("userId").toString());
        }

        FullExamRecord record = new FullExamRecord();
        record.setUserId(uid);
        record.setSuiteId(sid);
        record.setSuiteTitle(suite.getTitle());
        record.setStatus("doing");
        record.setStartTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setCostSeconds(0);
        record.setReadingCorrect(0);
        record.setReadingTotal(0);
        record.setListeningCorrect(0);
        record.setListeningTotal(0);
        record.setLegacyScore(0);
        recordMapper.insert(record);
        return Result.success(record);
    }

    // ==================================================================
    // 整卷提交 + 统一判分
    // ==================================================================
    @Transactional
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody FullSubmitDTO dto, HttpServletRequest request) {
        if (dto.getRecordId() == null) {
            return Result.fail("缺少模考记录id（recordId），请先调用 /api/fullExam/start");
        }
        FullExamRecord record = recordMapper.selectById(dto.getRecordId());
        if (record == null) {
            return Result.fail("模考记录不存在，recordId=" + dto.getRecordId());
        }
        FullSuite suite = suiteMapper.selectById(record.getSuiteId());
        if (suite == null) {
            return Result.fail("记录关联的套题不存在，suiteId=" + record.getSuiteId());
        }

        // 前端提交的答案 → questionId : userAnswer
        Map<Long, String> submitted = new HashMap<>();
        // 口语录音 → questionId : audioUrl
        // 即使转写文本为空，录音也照存：成绩页要能回放考生自己的发音
        Map<Long, String> audioUrls = new HashMap<>();
        if (dto.getAnswers() != null) {
            for (FullSubmitDTO.Item item : dto.getAnswers()) {
                if (item.getQuestionId() == null) {
                    continue;
                }
                if (item.getAudioUrl() != null && !item.getAudioUrl().trim().isEmpty()) {
                    audioUrls.put(item.getQuestionId(), item.getAudioUrl().trim());
                }
                if (item.getUserAnswer() == null || item.getUserAnswer().trim().isEmpty()) {
                    continue;
                }
                submitted.put(item.getQuestionId(), item.getUserAnswer());
            }
        }

        // 套题里的所有模块与题目
        List<FullSection> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<FullSection>().eq(FullSection::getSuiteId, suite.getId()));
        List<FullQuestion> questions = new ArrayList<>();
        for (FullSection section : sections) {
            questions.addAll(questionMapper.selectList(
                    new LambdaQueryWrapper<FullQuestion>()
                            .eq(FullQuestion::getSectionId, section.getId())
                            .orderByAsc(FullQuestion::getSortIndex)));
        }
        if (questions.isEmpty()) {
            return Result.fail("套题里没有任何题目，请检查 full_question 表");
        }

        // 重复提交时先清掉旧明细
        answerMapper.delete(new LambdaQueryWrapper<FullExamAnswer>()
                .eq(FullExamAnswer::getRecordId, record.getId()));

        int readingTotal = 0, listeningTotal = 0;
        int readingCorrect = 0, listeningCorrect = 0;
        List<BigDecimal> speakingBands = new ArrayList<>();
        List<BigDecimal> writingBands = new ArrayList<>();
        List<Map<String, Object>> speakingFeedback = new ArrayList<>();
        List<Map<String, Object>> writingFeedback = new ArrayList<>();

        for (FullQuestion question : questions) {
            String moduleType = question.getModuleType();
            if ("reading".equals(moduleType)) {
                readingTotal++;
            } else if ("listening".equals(moduleType)) {
                listeningTotal++;
            }

            String userAnswer = submitted.get(question.getId());
            // 未作答不再跳过：整卷每道题都写一条明细，
            // 成绩页才能"题目 + 正确答案 + 解析"全量展示，未作答的显示「我的答案：空」。
            boolean answered = userAnswer != null && !userAnswer.trim().isEmpty();
            if (!answered) {
                userAnswer = "";
            }

            if ("reading".equals(moduleType) || "listening".equals(moduleType)) {
                boolean correct = answered && isChoiceCorrect(question, userAnswer);
                if ("reading".equals(moduleType) && correct) {
                    readingCorrect++;
                }
                if ("listening".equals(moduleType) && correct) {
                    listeningCorrect++;
                }
                FullExamAnswer row = new FullExamAnswer();
                row.setRecordId(record.getId());
                row.setQuestionId(question.getId());
                row.setModuleType(moduleType);
                row.setUserAnswer(userAnswer);
                row.setCorrectAnswer(question.getAnswer());
                row.setIsCorrect(correct ? 1 : 0);
                row.setCreateTime(LocalDateTime.now());
                answerMapper.insert(row);
            } else if ("speaking".equals(moduleType) || "writing".equals(moduleType)) {
                BigDecimal band = null;
                String feedbackJson = null;
                if (answered) {
                    // 主观题交给本地评分引擎；未作答就不评分，页面直接显示未作答 + 满分范例
                    Map<String, Object> scored = "speaking".equals(moduleType)
                            ? aiScoringService.scoreSpeaking(question, userAnswer)
                            : aiScoringService.scoreWriting(question, userAnswer);
                    band = (BigDecimal) scored.get("band");
                    feedbackJson = aiScoringService.toJson(scored);
                    if ("speaking".equals(moduleType)) {
                        speakingBands.add(band);
                    } else {
                        writingBands.add(band);
                    }

                    Map<String, Object> feedbackItem = new LinkedHashMap<>();
                    feedbackItem.put("questionId", question.getId());
                    feedbackItem.put("band", band);
                    feedbackItem.put("feedback", scored);
                    if ("speaking".equals(moduleType)) {
                        speakingFeedback.add(feedbackItem);
                    } else {
                        writingFeedback.add(feedbackItem);
                    }
                }

                FullExamAnswer row = new FullExamAnswer();
                row.setRecordId(record.getId());
                row.setQuestionId(question.getId());
                row.setModuleType(moduleType);
                row.setUserAnswer(userAnswer);
                // 口语录音：成绩页可回放；写作/客观题这里为 null
                if ("speaking".equals(moduleType)) {
                    row.setAudioUrl(audioUrls.get(question.getId()));
                }
                row.setScoreBand(band);
                row.setAiFeedback(feedbackJson);
                row.setCreateTime(LocalDateTime.now());
                answerMapper.insert(row);
            }
        }

        // 分项分档
        BigDecimal readingBand = FullScoreUtil.bandFromCorrect(readingCorrect, readingTotal);
        BigDecimal listeningBand = FullScoreUtil.bandFromCorrect(listeningCorrect, listeningTotal);
        BigDecimal speakingBand = FullScoreUtil.average(speakingBands);
        BigDecimal writingBand = FullScoreUtil.average(writingBands);
        BigDecimal totalBand = FullScoreUtil.average(
                asList(readingBand, listeningBand, speakingBand, writingBand));

        // 用时：优先用前端计时，其次用 startTime→endTime
        int costSeconds = dto.getCostSeconds() == null ? 0 : dto.getCostSeconds();
        if (costSeconds <= 0 && record.getStartTime() != null) {
            long seconds = Duration.between(record.getStartTime(), LocalDateTime.now()).getSeconds();
            if (seconds > 0) {
                costSeconds = (int) seconds;
            }
        }

        record.setStatus("finished");
        record.setEndTime(LocalDateTime.now());
        record.setCostSeconds(costSeconds);
        record.setReadingCorrect(readingCorrect);
        record.setReadingTotal(readingTotal);
        record.setListeningCorrect(listeningCorrect);
        record.setListeningTotal(listeningTotal);
        record.setReadingBand(readingBand);
        record.setListeningBand(listeningBand);
        record.setSpeakingBand(speakingBand);
        record.setWritingBand(writingBand);
        record.setTotalBand(totalBand);
        record.setLegacyScore(FullScoreUtil.legacyFromTotal(totalBand));
        recordMapper.updateById(record);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordId", record.getId());
        data.put("totalBand", totalBand);
        data.put("legacyScore", record.getLegacyScore());
        data.put("readingBand", readingBand);
        data.put("listeningBand", listeningBand);
        data.put("speakingBand", speakingBand);
        data.put("writingBand", writingBand);
        data.put("readingCorrect", readingCorrect);
        data.put("readingTotal", readingTotal);
        data.put("listeningCorrect", listeningCorrect);
        data.put("listeningTotal", listeningTotal);
        data.put("speakingFeedback", speakingFeedback);
        data.put("writingFeedback", writingFeedback);
        data.put("costSeconds", costSeconds);
        data.put("scoreComment", FullScoreUtil.comment(totalBand.doubleValue()));
        data.put("cefr", FullScoreUtil.cefr(totalBand.doubleValue()));
        return Result.success(data);
    }

    // ==================================================================
    // 我的套题成绩列表
    // ==================================================================
    @GetMapping("/records")
    public Result<List<FullExamRecord>> records(@RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) String status,
                                                HttpServletRequest request) {
        Long uid = userId != null ? userId : authHelper.currentUserId(request);
        LambdaQueryWrapper<FullExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FullExamRecord::getUserId, uid);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(FullExamRecord::getStatus, status);
        }
        wrapper.orderByDesc(FullExamRecord::getCreateTime).orderByDesc(FullExamRecord::getId);
        return Result.success(recordMapper.selectList(wrapper));
    }

    // ==================================================================
    // 成绩详情
    // ==================================================================
    @GetMapping("/record/{id}")
    public Result<Map<String, Object>> recordDetail(@PathVariable Long id) {
        FullExamRecord record = recordMapper.selectById(id);
        if (record == null) {
            return Result.fail("记录不存在，recordId=" + id);
        }
        FullSuite suite = suiteMapper.selectById(record.getSuiteId());

        List<FullSection> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<FullSection>()
                        .eq(FullSection::getSuiteId, record.getSuiteId())
                        .orderByAsc(FullSection::getSortIndex));
        List<FullExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<FullExamAnswer>()
                        .eq(FullExamAnswer::getRecordId, id)
                        .orderByAsc(FullExamAnswer::getId));

        // 明细按 questionId 建索引，方便逐题补齐
        Map<Long, FullExamAnswer> answerMap = new HashMap<>();
        for (FullExamAnswer answer : answers) {
            answerMap.put(answer.getQuestionId(), answer);
        }

        // 按「套题里的每一道题」生成回顾视图：
        // 不再只遍历已作答明细——否则没作答的题整题都不会出现。
        // 老记录（升级前提交、缺明细）同样能完整展示。
        List<Map<String, Object>> moduleViews = new ArrayList<>();
        for (String moduleType : MODULE_ORDER) {
            List<Map<String, Object>> answerViews = new ArrayList<>();
            for (FullSection section : sections) {
                if (!moduleType.equals(section.getModuleType())) {
                    continue;
                }
                List<FullQuestion> moduleQuestions = questionMapper.selectList(
                        new LambdaQueryWrapper<FullQuestion>()
                                .eq(FullQuestion::getSectionId, section.getId())
                                .eq(FullQuestion::getModuleType, moduleType)
                                .orderByAsc(FullQuestion::getSortIndex));
                for (FullQuestion question : moduleQuestions) {
                    FullExamAnswer answer = answerMap.get(question.getId());
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("questionId", question.getId());
                    view.put("sectionId", section.getId());
                    view.put("moduleType", moduleType);
                    view.put("sortIndex", question.getSortIndex());
                    view.put("questionTitle", question.getQuestionTitle());
                    view.put("optionA", question.getOptionA());
                    view.put("optionB", question.getOptionB());
                    view.put("optionC", question.getOptionC());
                    view.put("optionD", question.getOptionD());
                    view.put("analysis", question.getAnalysis());
                    view.put("prompt", question.getPrompt());
                    view.put("taskType", question.getTaskType());
                    // 口语 / 写作的满分范例，只在成绩页下发
                    view.put("sampleAnswer", question.getSampleAnswer());

                    if (answer == null) {
                        // 明细缺失（未作答或升级前的老记录）：答案留空，正确答案照常给
                        view.put("userAnswer", "");
                        view.put("correctAnswer", question.getAnswer());
                        view.put("isCorrect", null);
                        view.put("band", null);
                        view.put("aiFeedback", null);
                        view.put("audioUrl", null);
                    } else {
                        view.put("userAnswer", answer.getUserAnswer());
                        view.put("correctAnswer", answer.getCorrectAnswer() == null
                                ? question.getAnswer() : answer.getCorrectAnswer());
                        view.put("isCorrect", answer.getIsCorrect());
                        view.put("band", answer.getScoreBand());
                        view.put("aiFeedback", parseFeedback(answer.getAiFeedback()));
                        // 口语录音地址（/upload/audio/xxx.webm），成绩页 audio 标签直接播放
                        view.put("audioUrl", answer.getAudioUrl());
                    }
                    answerViews.add(view);
                }
            }
            Map<String, Object> moduleView = new LinkedHashMap<>();
            moduleView.put("moduleType", moduleType);
            moduleView.put("answers", answerViews);
            moduleViews.add(moduleView);
        }

        // 阅读正文与听力原文放在成绩页（考试中不下发，防止提前看到听力原文）
        List<Map<String, Object>> sectionViews = new ArrayList<>();
        for (FullSection section : sections) {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("sectionId", section.getId());
            view.put("moduleType", section.getModuleType());
            view.put("title", section.getTitle());
            view.put("contentText", section.getContentText());
            view.put("audioUrl", section.getAudioUrl());
            sectionViews.add(view);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("record", record);
        data.put("suiteTitle", suite == null ? record.getSuiteTitle() : suite.getTitle());
        data.put("modules", moduleViews);
        data.put("sections", sectionViews);
        data.put("totalComment", record.getTotalBand() == null ? null
                : FullScoreUtil.comment(record.getTotalBand().doubleValue()));
        data.put("cefr", record.getTotalBand() == null ? null
                : FullScoreUtil.cefr(record.getTotalBand().doubleValue()));
        return Result.success(data);
    }

    // ==================================================================
    // 删除一条记录
    // ==================================================================
    @Transactional
    @DeleteMapping("/record/{id}")
    public Result<String> deleteRecord(@PathVariable Long id) {
        FullExamRecord record = recordMapper.selectById(id);
        if (record == null) {
            return Result.fail("记录不存在，recordId=" + id);
        }
        answerMapper.delete(new LambdaQueryWrapper<FullExamAnswer>()
                .eq(FullExamAnswer::getRecordId, id));
        recordMapper.deleteById(id);
        return Result.success("已删除");
    }

    // ==================================================================
    // 内部方法
    // ==================================================================

    private boolean isChoiceCorrect(FullQuestion question, String userAnswer) {
        if (userAnswer == null || question.getAnswer() == null) {
            return false;
        }
        return question.getAnswer().trim().equalsIgnoreCase(userAnswer.trim());
    }

    private List<BigDecimal> asList(BigDecimal... items) {
        List<BigDecimal> list = new ArrayList<>();
        for (BigDecimal item : items) {
            list.add(item);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private Object parseFeedback(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            // 解析失败就原样返回字符串，页面自己兜底
            return json;
        }
    }
}
