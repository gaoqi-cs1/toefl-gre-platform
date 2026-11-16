package com.demo.toefl.dto;

import lombok.Data;

/**
 * 提交答案DTO
 * 前端做题提交答案时传递的参数
 */
@Data
public class SubmitAnswerDTO {
    // 题目id
    private Long questionId;
    // 用户答案
    private String userAnswer;
    // 该题耗时（秒）
    private Integer costTime;
}
