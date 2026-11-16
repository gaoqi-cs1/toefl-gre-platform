package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.dto.ExamSubmitDTO;
import com.demo.toefl.entity.ExamRecord;
import com.demo.toefl.entity.SuiteSection;
import com.demo.toefl.entity.TpoExamAnswer;
import com.demo.toefl.entity.TpoQuestion;
import com.demo.toefl.entity.TpoSuite;
import com.demo.toefl.entity.UserAnswer;
import com.demo.toefl.service.ExamRecordService;
import com.demo.toefl.service.SuiteSectionService;
import com.demo.toefl.service.TpoExamAnswerService;
import com.demo.toefl.service.TpoQuestionService;
import com.demo.toefl.service.TpoSuiteService;
import com.demo.toefl.service.UserAnswerService;
import com.demo.toefl.util.AuthHelper;
import com.demo.toefl.util.Result;
import com.demo.toefl.util.ToeflScoreUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模考记录控制器
 *
 * 前端套题流程：
 *   SuiteList「开始整卷模考」→ /tpo/suite/exam?suiteId=x
 *   SuiteExam 进入页面时创建记录 → POST /api/examRecord/start {suiteId}
 *   SuiteExam 点「提交」→ POST /api/examRecord/submit（ExamSubmitDTO）
 *   跳转 /tpo/suite/result?recordId=x → GET /api/examRecord/{id}
 *   模考记录页 → GET /api/examRecord/list
 *
 * 原实现只有 list 和 {id} 两个查询接口，start/submit 都缺失，
 * 所以"开始模考"和"提交试卷"这两步必定失败。
 */
@RestController
@RequestMapping("/api/examRecord")
public class ExamRecordController {

    @Resource
    private ExamRecordService examRecordService;
    @Resource
    private TpoSuiteService tpoSuiteService;
    @Resource
    private SuiteSectionService suiteSectionService;
    @Resource
    private TpoQuestionService tpoQuestionService;
    @Resource
    private TpoExamAnswerService tpoExamAnswerService;
    @Resource
    private UserAnswerService userAnswerService;
    @Resource
    private AuthHelper authHelper;

    /** 模考记录列表，默认只查当前登录用户 */
    @GetMapping("/list")
    public Result<List<ExamRecord>> list(@RequestParam(required = false) Long userId,
                                        @RequestParam(required = false) String status,
                                        HttpServletRequest request) {
        Long uid = userId != null ? userId : authHelper.currentUserId(request);
        LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamRecord::getUserId, uid);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ExamRecord::getExamStatus, status);
        }
        wrapper.orderByDesc(ExamRecord::getCreateTime);
        return Result.success(examRecordService.list(wrapper));
    }

    /** 按id查单条（成绩页用） */
    @GetMapping("/{id}")
    public Result<ExamRecord> getOne(@PathVariable Long id) {
        ExamRecord record = examRecordService.getById(id);
        if (record == null) {
            return Result.fail("记录不存在");
        }
        return Result.success(record);
    }

    /** 成绩详情：记录 + 每题作答（/suite/result 页面用） */
    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam Long recordId) {
        ExamRecord record = examRecordService.getById(recordId);
        if (record == null) {
            return Result.fail("记录不存在");
        }
        List<TpoExamAnswer> answers = tpoExamAnswerService.list(
                new LambdaQueryWrapper<TpoExamAnswer>().eq(TpoExamAnswer::getRecordId, recordId));

        // 把题目信息补到作答上，成绩详情页要显示题干
        List<Map<String, Object>> answerViews = new ArrayList<>();
        for (TpoExamAnswer answer : answers) {
            Map<String, Object> view = new HashMap<>();
            view.put("questionId", answer.getQuestionId());
            view.put("moduleType", answer.getModuleType());
            view.put("userAnswer", answer.getUserAnswer());
            view.put("correctAnswer", answer.getCorrectAnswer());
            view.put("isCorrect", answer.getIsCorrect());
            TpoQuestion question = tpoQuestionService.getById(answer.getQuestionId());
            if (question != null) {
                view.put("questionTitle", question.getQuestionTitle());
                view.put("sectionId", question.getSectionId());
                view.put("analysis", question.getAnalysis());
                // 成绩详情页要按选项高亮"正确答案/我的答案"，这里把选项一并返回
                view.put("optionA", question.getOptionA());
                view.put("optionB", question.getOptionB());
                view.put("optionC", question.getOptionC());
                view.put("optionD", question.getOptionD());
                view.put("prompt", question.getPrompt());
            }
            answerViews.add(view);
        }

        Map<String, Object> data = new HashMap<>();
        // 结果页要显示"客观题答对 x/y、阅读 a/b、听力 c/d、口语/写作各答几题"，
        // 库里 exam_record 只存分数，这里按作答明细现算一遍，塞进 record 的临时字段
        int readingCorrect = 0, readingTotal = 0;
        int listeningCorrect = 0, listeningTotal = 0;
        int speakingCount = 0, writingCount = 0;
        for (Map<String, Object> view : answerViews) {
            Object moduleType = view.get("moduleType");
            Object isCorrect = view.get("isCorrect");
            boolean correct = isCorrect != null && ((Number) isCorrect).intValue() == 1;
            if ("reading".equals(moduleType)) {
                readingTotal++;
                if (correct) {
                    readingCorrect++;
                }
            } else if ("listening".equals(moduleType)) {
                listeningTotal++;
                if (correct) {
                    listeningCorrect++;
                }
            } else if ("speaking".equals(moduleType)) {
                speakingCount++;
            } else if ("writing".equals(moduleType)) {
                writingCount++;
            }
        }
        record.setCorrectCount(readingCorrect + listeningCorrect);
        record.setTotalCount(readingTotal + listeningTotal);
        record.setReadingCorrect(readingCorrect);
        record.setReadingTotal(readingTotal);
        record.setListeningCorrect(listeningCorrect);
        record.setListeningTotal(listeningTotal);
        record.setSpeakingCount(speakingCount);
        record.setWritingCount(writingCount);

        data.put("record", record);
        data.put("answers", answerViews);
        return Result.success(data);
    }

    /**
     * 删除一条模考记录及其作答明细
     * DELETE /api/examRecord/del?id=
     */
    @Transactional
    @DeleteMapping("/del")
    public Result<String> delete(@RequestParam Long id) {
        ExamRecord record = examRecordService.getById(id);
        if (record == null) {
            return Result.fail("记录不存在");
        }
        tpoExamAnswerService.remove(new LambdaQueryWrapper<TpoExamAnswer>()
                .eq(TpoExamAnswer::getRecordId, id));
        examRecordService.removeById(id);
        return Result.success("删除成功");
    }

    /**
     * 清空指定用户的全部模考记录与作答明细
     * DELETE /api/examRecord/clear?userId=
     * 前端调用前已弹二次确认
     */
    @Transactional
    @DeleteMapping("/clear")
    public Result<Map<String, Object>> clear(@RequestParam(required = false) Long userId,
                                             HttpServletRequest request) {
        Long uid = userId != null ? userId : authHelper.currentUserId(request);
        List<ExamRecord> records = examRecordService.list(
                new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getUserId, uid));
        int recordDeleted = 0;
        int answerDeleted = 0;
        for (ExamRecord record : records) {
            answerDeleted += (int) tpoExamAnswerService.count(
                    new LambdaQueryWrapper<TpoExamAnswer>().eq(TpoExamAnswer::getRecordId, record.getId()));
            tpoExamAnswerService.remove(new LambdaQueryWrapper<TpoExamAnswer>()
                    .eq(TpoExamAnswer::getRecordId, record.getId()));
            examRecordService.removeById(record.getId());
            recordDeleted++;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("recordDeleted", recordDeleted);
        data.put("answerDeleted", answerDeleted);
        return Result.success(data);
    }

    /**
     * 开始模考：创建一条 doing 状态的记录
     * POST /api/examRecord/start  参数 {suiteId} 或 ?suiteId=
     */
    @PostMapping("/start")
    public Result<ExamRecord> start(@RequestBody(required = false) Map<String, Object> body,
                                    @RequestParam(required = false) Long suiteId,
                                    HttpServletRequest request) {
        Long sid = suiteId;
        if (sid == null && body != null && body.get("suiteId") != null) {
            sid = Long.valueOf(body.get("suiteId").toString());
        }
        if (sid == null) {
            return Result.fail("缺少套题id（suiteId）");
        }

        TpoSuite suite = tpoSuiteService.getById(sid);
        if (suite == null) {
            return Result.fail("套题不存在，suiteId=" + sid);
        }

        Long uid = authHelper.currentUserId(request);
        if (body != null && body.get("userId") != null) {
            uid = Long.valueOf(body.get("userId").toString());
        }

        ExamRecord record = new ExamRecord();
        record.setUserId(uid);
        record.setSuiteId(sid);
        record.setSuiteName(suite.getTitle());
        record.setExamStatus("doing");
        record.setExamType("suite");
        record.setScore(0);
        record.setReadScore(0);
        record.setListenScore(0);
        record.setSpeakScore(0);
        record.setWriteScore(0);
        record.setTotalScore(0);
        record.setStartTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        examRecordService.save(record);
        return Result.success(record);
    }

    /**
     * 整卷提交并自动判分
     * POST /api/examRecord/submit
     */
    @Transactional
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody ExamSubmitDTO dto,
                                              HttpServletRequest request) {
        if (dto.getRecordId() == null) {
            return Result.fail("缺少模考记录id（recordId），请先调用 /api/examRecord/start");
        }
        ExamRecord record = examRecordService.getById(dto.getRecordId());
        if (record == null) {
            return Result.fail("模考记录不存在，recordId=" + dto.getRecordId());
        }
        Long uid = record.getUserId() != null ? record.getUserId() : authHelper.currentUserId(request);
        Long suiteId = record.getSuiteId();

        // 本次套题里阅读 / 听力各有多少题（分母）
        int readingTotal = 0;
        int listeningTotal = 0;
        List<SuiteSection> relList = suiteSectionService.list(
                new LambdaQueryWrapper<SuiteSection>().eq(SuiteSection::getSuiteId, suiteId));
        for (SuiteSection rel : relList) {
            long count = tpoQuestionService.count(
                    new LambdaQueryWrapper<TpoQuestion>().eq(TpoQuestion::getSectionId, rel.getSectionId()));
            if ("reading".equals(rel.getType())) {
                readingTotal += (int) count;
            } else if ("listening".equals(rel.getType())) {
                listeningTotal += (int) count;
            }
        }

        // 清掉上一次同记录的作答，避免重复提交产生重复行
        tpoExamAnswerService.remove(new LambdaQueryWrapper<TpoExamAnswer>()
                .eq(TpoExamAnswer::getRecordId, record.getId()));

        int readingCorrect = 0;
        int listeningCorrect = 0;
        int speakingCount = 0;
        int writingCount = 0;

        // ---------- 阅读 ----------
        if (dto.getReadingAnswerList() != null) {
            for (ExamSubmitDTO.QuestionAnswerDTO item : dto.getReadingAnswerList()) {
                TpoQuestion question = item.getQuestionId() == null ? null
                        : tpoQuestionService.getById(item.getQuestionId());
                if (question == null) {
                    continue;
                }
                boolean correct = isChoiceCorrect(question, item.getUserAns());
                if (correct) {
                    readingCorrect++;
                }
                saveExamAnswer(record.getId(), question, "reading",
                        item.getUserAns(), question.getAnswer(), correct ? 1 : 0);
                saveUserAnswer(uid, question, item.getUserAns(), correct ? 1 : 0);
            }
        }

        // ---------- 听力 ----------
        if (dto.getListeningAnswerList() != null) {
            for (ExamSubmitDTO.QuestionAnswerDTO item : dto.getListeningAnswerList()) {
                TpoQuestion question = item.getQuestionId() == null ? null
                        : tpoQuestionService.getById(item.getQuestionId());
                if (question == null) {
                    continue;
                }
                boolean correct = isChoiceCorrect(question, item.getUserAns());
                if (correct) {
                    listeningCorrect++;
                }
                saveExamAnswer(record.getId(), question, "listening",
                        item.getUserAns(), question.getAnswer(), correct ? 1 : 0);
                saveUserAnswer(uid, question, item.getUserAns(), correct ? 1 : 0);
            }
        }

        // ---------- 口语（主观，不自动判分） ----------
        if (dto.getSpeakingAnswerList() != null) {
            for (ExamSubmitDTO.SpeakWriteDTO item : dto.getSpeakingAnswerList()) {
                TpoQuestion question = item.getQuestionId() == null ? null
                        : tpoQuestionService.getById(item.getQuestionId());
                if (question == null || isBlank(item.getContent())) {
                    continue;
                }
                speakingCount++;
                saveExamAnswer(record.getId(), question, "speaking", item.getContent(), null, 0);
                saveUserAnswer(uid, question, item.getContent(), 0);
            }
        }

        // ---------- 写作（主观，不自动判分） ----------
        if (dto.getWritingAnswerList() != null) {
            for (ExamSubmitDTO.SpeakWriteDTO item : dto.getWritingAnswerList()) {
                TpoQuestion question = item.getQuestionId() == null ? null
                        : tpoQuestionService.getById(item.getQuestionId());
                if (question == null || isBlank(item.getContent())) {
                    continue;
                }
                writingCount++;
                saveExamAnswer(record.getId(), question, "writing", item.getContent(), null, 0);
                saveUserAnswer(uid, question, item.getContent(), 0);
            }
        }

        // ---------- 换算分数 ----------
        int readScore = ToeflScoreUtil.calcReadScore(readingCorrect);
        int listenScore = ToeflScoreUtil.calcListenScore(listeningCorrect);
        int speakScore = 0;   // 待 AI/老师评分
        int writeScore = 0;   // 待 AI/老师评分
        int totalScore = readScore + listenScore;

        record.setExamStatus("finished");
        record.setEndTime(LocalDateTime.now());
        record.setReadScore(readScore);
        record.setListenScore(listenScore);
        record.setSpeakScore(speakScore);
        record.setWriteScore(writeScore);
        record.setTotalScore(totalScore);
        record.setScore(totalScore);
        examRecordService.updateById(record);

        Map<String, Object> data = new HashMap<>();
        data.put("recordId", record.getId());
        data.put("readScore", readScore);
        data.put("listenScore", listenScore);
        data.put("speakScore", speakScore);
        data.put("writeScore", writeScore);
        data.put("totalScore", totalScore);
        data.put("readingCorrect", readingCorrect);
        data.put("readingTotal", readingTotal);
        data.put("listeningCorrect", listeningCorrect);
        data.put("listeningTotal", listeningTotal);
        data.put("speakingCount", speakingCount);
        data.put("writingCount", writingCount);
        return Result.success(data);
    }

    /** 客观题判分：忽略大小写、忽略首尾空格 */
    private boolean isChoiceCorrect(TpoQuestion question, String userAns) {
        if (isBlank(userAns) || question.getAnswer() == null) {
            return false;
        }
        return question.getAnswer().trim().equalsIgnoreCase(userAns.trim());
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    /** 写入 tpo_exam_answer（整卷作答明细） */
    private void saveExamAnswer(Long recordId, TpoQuestion question, String moduleType,
                                String userAnswer, String correctAnswer, Integer isCorrect) {
        TpoExamAnswer answer = new TpoExamAnswer();
        answer.setRecordId(recordId);
        answer.setQuestionId(question.getId());
        answer.setModuleType(moduleType);
        answer.setUserAnswer(userAnswer);
        answer.setCorrectAnswer(correctAnswer);
        answer.setIsCorrect(isCorrect);
        tpoExamAnswerService.save(answer);
    }

    /** 同步写入 user_answer（错题本"我的答案"依赖它） */
    private void saveUserAnswer(Long userId, TpoQuestion question, String userAnswer, Integer isCorrect) {
        LambdaQueryWrapper<UserAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAnswer::getUserId, userId)
                .eq(UserAnswer::getQuestionId, question.getId());
        UserAnswer exist = userAnswerService.getOne(wrapper, false);
        if (exist != null) {
            exist.setUserAnswer(userAnswer);
            exist.setIsCorrect(isCorrect);
            userAnswerService.updateById(exist);
        } else {
            UserAnswer entity = new UserAnswer();
            entity.setUserId(userId);
            entity.setQuestionId(question.getId());
            entity.setSectionId(question.getSectionId());
            entity.setUserAnswer(userAnswer);
            entity.setIsCorrect(isCorrect);
            entity.setModuleType(question.getModuleType());
            entity.setCreateTime(LocalDateTime.now());
            userAnswerService.save(entity);
        }
    }
}
