package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * GRE 分块练习题库中的一道题
 * 对应表 gre_question
 *
 * 选项最多 6 个：Sentence Equivalence 是六选一，Quant 用 A-E。
 * 写作题（task_type = awa_issue / awa_argument）没有选项，题面在 prompt。
 */
@Data
@TableName("gre_question")
public class GreQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属练习单元 gre_section.id */
    private Long sectionId;

    /** writing / verbal / quant */
    private String moduleType;

    /** 单元内序号，页面显示 Q1/Q2 */
    private Integer sortIndex;

    /** 题干 */
    private String questionTitle;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String optionE;
    private String optionF;

    /** 客观题答案字母；写作题留空 */
    private String answer;

    /** 解析（做题页提交后才下发，避免一打开就看到答案） */
    private String analysis;

    /** 写作题面与写作要求 */
    private String prompt;

    /** text_completion / sentence_equivalence / reading / quant_compare / quant_problem / awa_issue / awa_argument */
    private String taskType;

    /** 写作评分说明 */
    private String scoreStandard;

    /** 写作满分范例 */
    private String sampleAnswer;

    private String difficulty;
}
