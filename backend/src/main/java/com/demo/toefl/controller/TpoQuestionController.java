package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.dto.QuestionDTO;
import com.demo.toefl.entity.TpoQuestion;
import com.demo.toefl.service.TpoQuestionService;
import com.demo.toefl.util.Result;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/question")
public class TpoQuestionController {

    @Resource
    private TpoQuestionService tpoQuestionService;

    /**
     * 根据sectionId查询该篇章下所有题目
     * 返回DTO，对外接口干净正式
     */
    @GetMapping("/list")
    public Result<List<QuestionDTO>> listQuestion(@RequestParam Long sectionId) {
        return queryBySection(sectionId);
    }

    /**
     * 同一个接口的路径参数形态：GET /api/question/list/{sectionId}
     * （前端 api/tpo.js 里用的是这种写法，两种都留着，避免调用方踩空）
     */
    @GetMapping("/list/{sectionId}")
    public Result<List<QuestionDTO>> listByPath(@PathVariable Long sectionId) {
        return queryBySection(sectionId);
    }

    private Result<List<QuestionDTO>> queryBySection(Long sectionId) {
        try {
            LambdaQueryWrapper<TpoQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TpoQuestion::getSectionId, sectionId)
                    .orderByAsc(TpoQuestion::getId);
            List<TpoQuestion> list = tpoQuestionService.list(wrapper);
            // entity转dto
            List<QuestionDTO> dtoList = list.stream().map(item -> {
                QuestionDTO dto = new QuestionDTO();
                BeanUtils.copyProperties(item, dto);
                return dto;
            }).collect(Collectors.toList());
            return Result.success(dtoList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("查询题目信息失败：" + e.getMessage());
        }
    }
}
