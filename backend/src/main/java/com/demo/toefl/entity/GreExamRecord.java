package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 一次 GRE 套题模考记录
 * 对应表 gre_exam_record
 *
 * 计分口径（GRE 官方量纲的近似换算）：
 *   Verbal / Quant 各 130-170（1 分档），由正确率映射；
 *   AWA 0-6（0.5 档），由 GreAwaScoringService 给出；
 *   总分 = Verbal + Quant（260-340），AWA 单独列，不计入总分。
 */
@Data
@TableName("gre_exam_record")
public class GreExamRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long suiteId;

    private String suiteTitle;

    /** doing / finished */
    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer costSeconds;

    private Integer verbalCorrect;
    private Integer verbalTotal;
    private Integer quantCorrect;
    private Integer quantTotal;

    /** Verbal 130-170 */
    private Integer verbalScaled;

    /** Quant 130-170 */
    private Integer quantScaled;

    /** AWA 0-6 */
    private BigDecimal awaScore;

    /** verbal + quant，260-340 */
    private Integer totalScore;

    private LocalDateTime createTime;
}
