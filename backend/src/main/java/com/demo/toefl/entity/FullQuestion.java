package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 套题内的题目
 * 对应表 full_question
 * 客观题（reading/listening）用 optionA-D + answer；
 * 主观题（speaking/writing）用 prompt + taskType + scoreStandard，answer 为空。
 */
@Data
@TableName("full_question")
public class FullQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sectionId;

    /** reading / listening / speaking / writing */
    private String moduleType;

    /** 模块内序号，页面显示 Q1 / Q2 */
    private Integer sortIndex;

    private String questionTitle;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    /** 客观题标准答案；主观题为空 */
    private String answer;

    /** 解析，成绩页展示 */
    private String analysis;

    /** 口语 / 写作题面与要求 */
    private String prompt;

    /** speaking_independent / speaking_integrated / writing_integrated / writing_discussion / writing_email */
    private String taskType;

    /** 作答限时（秒） */
    private Integer limitSecond;

    /** 准备时间（秒） */
    private Integer prepareSecond;

    /** 本题评分标准（AI 评分会读它） */
    private String scoreStandard;

    /**
     * 满分范例答案（口语 / 写作专用，客观题留空）。
     * 只在成绩页 / 回顾页下发，考试作答期间不下发——否则等于提前给答案。
     */
    private String sampleAnswer;
}
