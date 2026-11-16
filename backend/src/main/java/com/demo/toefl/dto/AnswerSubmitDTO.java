package com.demo.toefl.dto;

import lombok.Data;

/**
 * 单题作答提交参数
 * 阅读/听力：userAnswer = A/B/C/D
 * 口语/写作：userAnswer = 文字内容（口语可能额外带录音文件）
 */
@Data
public class AnswerSubmitDTO {
    /** 前端仍会传 userId，但后端以 token 解析出的用户为准，这里保留兼容 */
    private Long userId;
    private Long questionId;
    private Long sectionId;
    private String userAnswer;
    /** 做题耗时（秒），可为空 */
    private Integer costTime;
}
