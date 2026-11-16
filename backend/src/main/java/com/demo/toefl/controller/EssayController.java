package com.demo.toefl.controller;


import com.demo.toefl.service.EssayService;
import com.demo.toefl.util.JwtUtil;
import com.demo.toefl.vo.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/essay")
public class EssayController {

    @Resource
    private EssayService essayService;

    /**
     * 提交作文
     * 参数：sectionId 题目id、content作文内容、userId
     */
    @PostMapping("/submit")
    public Result<String> submitEssay(@RequestBody Map<String,Object> req){
        Long sectionId = Long.valueOf(req.get("sectionId").toString());
        Long userId = Long.valueOf(req.get("userId").toString());
        String content = req.get("content").toString();
        essayService.saveEssay(userId,sectionId,content);
        return Result.success("作文提交成功");
    }
}
