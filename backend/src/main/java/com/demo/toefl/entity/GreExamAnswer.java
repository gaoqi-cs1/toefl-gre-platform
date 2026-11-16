package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GRE 套题的一道作答明细
 * 对应表 gre_exam_answer
 *
 * 整卷每题都写一条（未作答也写），成绩页才能全量展示"题目 + 正确答案 + 解析"。
 */
@Data
@TableName("gre_exam_answer")
public class GreExamAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Long questionId;

    /** writing / verbal / quant */
    private String moduleType;

    /** 客观题选项字母；写作为全文；未作答为空字符串 */
    private String userAnswer;

    private String correctAnswer;

    /** 客观题 1/0；写作题 null */
    private Integer isCorrect;

    /** 写作 AWA 0-6，客观题为 null */
    private BigDecimal awaScore;

    /** AWA 评分反馈 JSON */
    private String aiFeedback;

    private LocalDateTime createTime;
}
