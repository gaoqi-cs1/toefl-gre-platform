package com.demo.toefl.dto;

import lombok.Data;

@Data
public class QuestionDTO {
    private Long id;
    private Long sectionId;
    private Long passageId;
    private String passageTitle;
    private String passageContent;
    private String audioUrl;
    private String prompt;
    private String questionTitle;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String answer;
    private String moduleType;
    private Integer limitSecond;
    private String scoreStandard;
    /** 解析：练习页 "Show Analysis" 面板要显示，原来 DTO 漏了这个字段 */
    private String analysis;
}
