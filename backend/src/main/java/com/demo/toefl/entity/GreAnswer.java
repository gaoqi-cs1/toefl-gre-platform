package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GRE 分块练习的作答记录（同一用户同一题只保留一条）
 * 对应表 gre_answer
 */
@Data
@TableName("gre_answer")
public class GreAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    private String userAnswer;

    /** 客观题 1/0；写作题 null */
    private Integer isCorrect;

    /** 写作 AWA 0-6 */
    private BigDecimal awaScore;

    private LocalDateTime createTime;
}
