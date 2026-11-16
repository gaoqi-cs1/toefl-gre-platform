package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.TpoQuestion;
import com.demo.toefl.entity.TpoSection;
import com.demo.toefl.entity.UserAnswer;
import com.demo.toefl.entity.WrongQuestion;
import com.demo.toefl.service.TpoQuestionService;
import com.demo.toefl.service.TpoSectionService;
import com.demo.toefl.service.UserAnswerService;
import com.demo.toefl.service.WrongQuestionService;
import com.demo.toefl.util.AuthHelper;
import com.demo.toefl.util.Result;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 错题本控制器
 *
 * 说明：仓库里原本只有 WrongController.java.bak（被改名为备份，等于没启用），
 * 所以四个练习页的"加入错题本"和错题本页面的"加载/移除"全部 404。
 * 这里按前端实际调用的路径恢复：
 *   POST   /api/wrong/add            {userId, questionId, sectionId}
 *   GET    /api/wrong/list?userId=&sectionId=
 *   DELETE /api/wrong/del?id=         错题记录id
 *   DELETE /api/wrong/remove/{qId}    题目id（旧封装 api/tpo.js 与 user/WrongBook.vue 用这个）
 * "我的答案"统一从 user_answer 表读取（与作答接口同源，避免两张表数据不一致）。
 */
@RestController
@RequestMapping("/api/wrong")
public class WrongController {

    @Resource
    private WrongQuestionService wrongQuestionService;

    @Resource
    private TpoQuestionService tpoQuestionService;

    @Resource
    private TpoSectionService tpoSectionService;

    @Resource
    private UserAnswerService userAnswerService;

    @Resource
    private AuthHelper authHelper;

    /** 加入错题本 */
    @PostMapping("/add")
    public Result<String> addWrong(@RequestBody WrongQuestion body, HttpServletRequest request) {
        if (body.getQuestionId() == null) {
            return Result.fail("缺少题目id（questionId）");
        }
        Long uid = body.getUserId() != null && body.getUserId() > 0
                ? body.getUserId() : authHelper.currentUserId(request);

        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongQuestion::getUserId, uid)
                .eq(WrongQuestion::getQuestionId, body.getQuestionId());
        if (wrongQuestionService.count(wrapper) > 0) {
            return Result.fail("该题目已经在错题本");
        }

        // 篇章id没传时，从题目上补全
        Long sectionId = body.getSectionId();
        if (sectionId == null) {
            TpoQuestion question = tpoQuestionService.getById(body.getQuestionId());
            sectionId = question == null ? null : question.getSectionId();
        }

        WrongQuestion save = new WrongQuestion();
        save.setUserId(uid);
        save.setQuestionId(body.getQuestionId());
        save.setSectionId(sectionId);
        save.setType(body.getType());
        save.setCreateTime(LocalDateTime.now());
        wrongQuestionService.save(save);
        return Result.success("加入成功");
    }

    /** 错题列表（带题目详情 + 我的答案） */
    @GetMapping("/list")
    public Result<List<WrongVO>> getWrongList(@RequestParam(required = false) Long userId,
                                              @RequestParam(required = false) Long sectionId,
                                              HttpServletRequest request) {
        Long uid = userId != null ? userId : authHelper.currentUserId(request);

        LambdaQueryWrapper<WrongQuestion> wWrapper = new LambdaQueryWrapper<>();
        wWrapper.eq(WrongQuestion::getUserId, uid);
        if (sectionId != null) {
            wWrapper.eq(WrongQuestion::getSectionId, sectionId);
        }
        wWrapper.orderByDesc(WrongQuestion::getCreateTime);
        List<WrongQuestion> wrongList = wrongQuestionService.list(wWrapper);

        List<WrongVO> voList = new ArrayList<>();
        for (WrongQuestion item : wrongList) {
            WrongVO vo = new WrongVO();
            vo.setId(item.getId());
            vo.setQuestionId(item.getQuestionId());
            vo.setSectionId(item.getSectionId());

            TpoQuestion question = item.getQuestionId() == null ? null
                    : tpoQuestionService.getById(item.getQuestionId());
            // 库里存在一批"孤儿"错题记录（question_id 指向已被删掉的题目），
            // 这种记录在页面上会显示成空白卡片，直接跳过不返回
            if (question == null) {
                continue;
            }
            {
                vo.setQuestionTitle(question.getQuestionTitle());
                vo.setOptionA(question.getOptionA());
                vo.setOptionB(question.getOptionB());
                vo.setOptionC(question.getOptionC());
                vo.setOptionD(question.getOptionD());
                vo.setCorrectAnswer(question.getAnswer());
                vo.setAnalysis(question.getAnalysis());
                if (vo.getSectionId() == null) {
                    vo.setSectionId(question.getSectionId());
                }
            }

            TpoSection section = vo.getSectionId() == null ? null
                    : tpoSectionService.getById(vo.getSectionId());
            if (section != null) {
                vo.setSectionType(section.getType());
            } else {
                vo.setSectionType(item.getType());
            }

            // 我的答案：从 user_answer 取
            LambdaQueryWrapper<UserAnswer> aWrapper = new LambdaQueryWrapper<>();
            aWrapper.eq(UserAnswer::getUserId, uid)
                    .eq(UserAnswer::getQuestionId, item.getQuestionId());
            UserAnswer answer = userAnswerService.getOne(aWrapper, false);
            if (answer != null) {
                vo.setUserAnswer(answer.getUserAnswer());
                vo.setIsCorrect(answer.getIsCorrect());
            } else {
                vo.setUserAnswer(null);
                vo.setIsCorrect(0);
            }
            voList.add(vo);
        }
        return Result.success(voList);
    }

    /** 按错题记录id移除 */
    @DeleteMapping("/del")
    public Result<String> delWrong(@RequestParam Long id) {
        wrongQuestionService.removeById(id);
        return Result.success("移除成功");
    }

    /** 按题目id移除（旧接口形态） */
    @DeleteMapping("/remove/{questionId}")
    public Result<String> removeByQuestionId(@PathVariable Long questionId,
                                             HttpServletRequest request) {
        Long uid = authHelper.currentUserId(request);
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongQuestion::getUserId, uid)
                .eq(WrongQuestion::getQuestionId, questionId);
        wrongQuestionService.remove(wrapper);
        return Result.success("移除成功");
    }

    @Data
    public static class WrongVO {
        private Long id;
        private Long questionId;
        private Long sectionId;
        private String questionTitle;
        private String sectionType;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String correctAnswer;
        private String userAnswer;
        private Integer isCorrect;
        private String analysis;
    }
}
