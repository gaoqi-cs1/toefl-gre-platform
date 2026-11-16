package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套题逐题作答明细
 * 对应表 full_exam_answer
 * 客观题：isCorrect 为 1/0；主观题：band 为 AI 给的分档，aiFeedback 存反馈 JSON。
 */
@Data
@TableName("full_exam_answer")
public class FullExamAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Long questionId;

    private String moduleType;

    private String userAnswer;

    /** 口语录音地址（/upload/audio/xxx.webm）；客观题与写作题为空 */
    private String audioUrl;

    private String correctAnswer;

    private Integer isCorrect;

    /** 主观题 AI 分档；属性名不能叫 band —— band 是 OGNL 的位运算关键字，会和 MyBatis 动态 SQL 冲突 */
    @TableField("band")
    private BigDecimal scoreBand;

    /** AI 反馈（JSON 字符串） */
    private String aiFeedback;

    private LocalDateTime createTime;
}
