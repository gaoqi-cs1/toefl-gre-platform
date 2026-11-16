package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.dto.SubmitAnswerDTO;
import com.demo.toefl.entity.TpoQuestion;
import com.demo.toefl.entity.UserRecord;
import com.demo.toefl.service.TpoQuestionService;
import com.demo.toefl.service.UserRecordService;
import com.demo.toefl.util.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 做题记录控制器：提交答案、自动判分、保存记录
 */
@RestController
@RequestMapping("/api/record")
public class RecordController {
    @Resource
    private UserRecordService recordService;
    @Resource
    private TpoQuestionService questionService;

    /**
     * 提交答案接口
     * POST /api/record/submit
     * 自动比对正确答案，判断对错，存入user_record表
     */
    @PostMapping("/submit")
    public Result<String> submitAnswer(@RequestBody SubmitAnswerDTO dto, HttpServletRequest request){
        // 从request取出当前登录用户名，你可以在这里扩展：根据用户名查userId
        String username = (String) request.getAttribute("username");

        // 根据题目id拿到原题信息（包含标准答案）
        TpoQuestion question = questionService.getById(dto.getQuestionId());
        if(question == null){
            return Result.fail("题目不存在");
        }

        // 判断答案是否正确
        boolean ok = question.getAnswer().equals(dto.getUserAnswer());
        Integer isRight = ok ? 1 : 0;

        UserRecord record = new UserRecord();
        // 这里简化处理：暂时固定userId=1；【后续优化：需要userService根据username查询真实userId】
        record.setUserId(1L);
        record.setQuestionId(dto.getQuestionId());
        record.setUserAnswer(dto.getUserAnswer());
        record.setIsRight(isRight);
        record.setCostTime(dto.getCostTime());
        recordService.save(record);

        if(ok){
            return Result.success("回答正确");
        }else{
            return Result.fail("回答错误，正确答案：" + question.getAnswer());
        }
    }
}
