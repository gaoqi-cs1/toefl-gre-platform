package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.dto.AnswerSubmitDTO;
import com.demo.toefl.entity.TpoQuestion;
import com.demo.toefl.entity.UserAnswer;
import com.demo.toefl.service.TpoQuestionService;
import com.demo.toefl.service.UserAnswerService;
import com.demo.toefl.util.AuthHelper;
import com.demo.toefl.util.Result;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 单题作答控制器
 *
 * 前端四个练习页（ReadingExam / ListeningExam / SpeakingExam / WritingExam）都会调
 * POST /api/answer/submit，其中口语页用 multipart 上传录音，其余用 json。
 * 之前后端只有 GET /api/answer/list，没有 submit，页面点"提交答案"必定 404。
 * 这里补齐：客观题自动判分、主观题记录作答、作答结果写入 user_answer 表。
 */
@RestController
@RequestMapping("/api/answer")
public class AnswerController {

    @Resource
    private UserAnswerService userAnswerService;

    @Resource
    private TpoQuestionService tpoQuestionService;

    @Resource
    private AuthHelper authHelper;

    /** 口语录音保存目录，与 FileController 保持一致 */
    private static final String AUDIO_DIR = "upload/audio/";

    /**
     * 已做过的答案列表，前端用来恢复每道题上次的选择
     * GET /api/answer/list?userId=1
     */
    @GetMapping("/list")
    public Result<List<UserAnswer>> list(@RequestParam(required = false) Long userId,
                                         HttpServletRequest request) {
        Long uid = userId != null ? userId : authHelper.currentUserId(request);
        LambdaQueryWrapper<UserAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAnswer::getUserId, uid);
        List<UserAnswer> list = userAnswerService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 提交答案（JSON）：阅读、听力、写作页走这里
     * POST /api/answer/submit
     */
    @PostMapping(value = "/submit", consumes = "application/json")
    public Result<AnswerResultVO> submitJson(@RequestBody AnswerSubmitDTO dto,
                                            HttpServletRequest request) {
        return doSubmit(dto, null, request);
    }

    /**
     * 提交答案（multipart）：口语页带录音文件走这里
     * POST /api/answer/submit  Content-Type: multipart/form-data
     */
    @PostMapping(value = "/submit", consumes = "multipart/form-data")
    public Result<AnswerResultVO> submitForm(@RequestParam(required = false) Long userId,
                                            @RequestParam Long questionId,
                                            @RequestParam(required = false) Long sectionId,
                                            @RequestParam(required = false) String userAnswer,
                                            @RequestParam(required = false) MultipartFile audioFile,
                                            HttpServletRequest request) {
        AnswerSubmitDTO dto = new AnswerSubmitDTO();
        dto.setUserId(userId);
        dto.setQuestionId(questionId);
        dto.setSectionId(sectionId);
        dto.setUserAnswer(userAnswer);
        return doSubmit(dto, audioFile, request);
    }

    /**
     * 真正的判分 + 落库逻辑
     */
    private Result<AnswerResultVO> doSubmit(AnswerSubmitDTO dto, MultipartFile audioFile,
                                            HttpServletRequest request) {
        if (dto.getQuestionId() == null) {
            return Result.fail("缺少题目id（questionId）");
        }
        TpoQuestion question = tpoQuestionService.getById(dto.getQuestionId());
        if (question == null) {
            return Result.fail("题目不存在，questionId=" + dto.getQuestionId());
        }

        Long uid = authHelper.currentUserId(request);
        if (dto.getUserId() != null && dto.getUserId() > 0L) {
            // 前端传了就以前端为准，保持和旧页面行为一致
            uid = dto.getUserId();
        }

        String moduleType = question.getModuleType();
        boolean objective = "reading".equals(moduleType) || "listening".equals(moduleType);

        String userAnswer = dto.getUserAnswer() == null ? "" : dto.getUserAnswer().trim();
        String correctAnswer = question.getAnswer();
        boolean answered = !userAnswer.isEmpty();

        // 客观题：与标准答案比对（忽略大小写和首尾空格）
        boolean correct = false;
        Integer isCorrect;
        if (objective) {
            correct = answered && correctAnswer != null
                    && correctAnswer.trim().equalsIgnoreCase(userAnswer);
            isCorrect = correct ? 1 : 0;
        } else {
            // 主观题(口语/写作)不自动判分，只要作答了就算"已作答"
            correct = answered;
            isCorrect = 0;
        }

        // 口语录音落盘
        String audioUrl = null;
        if (audioFile != null && !audioFile.isEmpty()) {
            try {
                // 相对路径先解析成绝对路径：MultipartFile.transferTo(相对路径)
                // 会被写到 Tomcat 临时目录，结果"保存成功"但文件其实找不到
                File folder = new File(AUDIO_DIR);
                if (!folder.isAbsolute()) {
                    folder = new File(System.getProperty("user.dir"), AUDIO_DIR);
                }
                folder = folder.getAbsoluteFile();
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                String fileName = UUID.randomUUID() + ".webm";
                audioFile.transferTo(new File(folder, fileName));
                audioUrl = "/upload/audio/" + fileName;
            } catch (Exception e) {
                // 录音保存失败不影响文字作答入库，只记录日志
                e.printStackTrace();
            }
        }

        Long sectionId = dto.getSectionId() != null ? dto.getSectionId() : question.getSectionId();
        saveOrUpdateAnswer(uid, dto.getQuestionId(), sectionId, userAnswer, isCorrect, moduleType);

        AnswerResultVO vo = new AnswerResultVO();
        vo.setQuestionId(dto.getQuestionId());
        vo.setUserAnswer(userAnswer);
        vo.setCorrectAnswer(objective ? correctAnswer : "本题为主观题，提交后由老师/AI 评分");
        vo.setAnswered(answered);
        vo.setCorrect(correct);
        vo.setIsCorrect(isCorrect);
        vo.setAnalysis(question.getAnalysis());
        vo.setAudioUrl(audioUrl);
        vo.setModuleType(moduleType);
        vo.setObjective(objective);
        return Result.success(vo);
    }

    /**
     * 同一用户同一题目只保留一条作答（先更新，没有则插入），
     * 避免每点一次提交就在 user_answer 里多一条脏数据
     */
    private void saveOrUpdateAnswer(Long userId, Long questionId, Long sectionId,
                                    String userAnswer, Integer isCorrect, String moduleType) {
        LambdaQueryWrapper<UserAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAnswer::getUserId, userId)
                .eq(UserAnswer::getQuestionId, questionId);
        UserAnswer exist = userAnswerService.getOne(wrapper, false);
        if (exist != null) {
            exist.setUserAnswer(userAnswer);
            exist.setIsCorrect(isCorrect);
            if (sectionId != null) {
                exist.setSectionId(sectionId);
            }
            userAnswerService.updateById(exist);
        } else {
            UserAnswer entity = new UserAnswer();
            entity.setUserId(userId);
            entity.setQuestionId(questionId);
            entity.setSectionId(sectionId);
            entity.setUserAnswer(userAnswer);
            entity.setIsCorrect(isCorrect);
            entity.setModuleType(moduleType);
            entity.setCreateTime(LocalDateTime.now());
            userAnswerService.save(entity);
        }
    }

    /**
     * 作答结果返回体，字段名与四个练习页读取的字段完全对应
     */
    @Data
    public static class AnswerResultVO {
        private Long questionId;
        private String userAnswer;
        private String correctAnswer;
        private Boolean answered;
        private Boolean correct;
        private Integer isCorrect;
        private String analysis;
        private String audioUrl;
        private String moduleType;
        private Boolean objective;
    }
}
