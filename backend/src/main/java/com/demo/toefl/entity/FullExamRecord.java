package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 一次真题套题模考记录
 * 对应表 full_exam_record
 * 分数口径：新托福 1-6 分档（0.5 步进），总分 = 四项平均；legacyScore 为 0-120 兼容分。
 */
@Data
@TableName("full_exam_record")
public class FullExamRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long suiteId;

    private String suiteTitle;

    /** doing / finished */
    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 实际用时（秒），提交时按 startTime→endTime 计算 */
    private Integer costSeconds;

    private Integer readingCorrect;
    private Integer readingTotal;
    private Integer listeningCorrect;
    private Integer listeningTotal;

    /** 四项 1-6 档 */
    private BigDecimal readingBand;
    private BigDecimal listeningBand;
    private BigDecimal speakingBand;
    private BigDecimal writingBand;

    /** 四项平均 */
    private BigDecimal totalBand;

    /** 0-120 兼容分 */
    private Integer legacyScore;

    private LocalDateTime createTime;
}
