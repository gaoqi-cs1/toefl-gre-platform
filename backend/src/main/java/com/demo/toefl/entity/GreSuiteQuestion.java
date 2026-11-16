package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * GRE 套题内的一道题（与分块练习的 gre_question 结构相同但独立存储）
 * 对应表 gre_suite_question
 */
@Data
@TableName("gre_suite_question")
public class GreSuiteQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属 gre_suite_section.id */
    private Long sectionId;

    private String moduleType;

    private Integer sortIndex;

    private String questionTitle;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String optionE;
    private String optionF;

    private String answer;

    private String analysis;

    private String prompt;

    private String taskType;

    private String scoreStandard;

    private String sampleAnswer;

    private String difficulty;
}
