package com.demo.toefl.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * GRE 分数换算与评语（独立于 TOEFL 的 FullScoreUtil）
 *
 * 说明：GRE 官方分数不是"正确率线性映射"，这里用的是教学场景下的近似换算——
 * 正确率 → 130-170 区间线性映射（1 分档），用于自测看趋势，不能当作官方预测分。
 */
public final class GreScoreUtil {

    private GreScoreUtil() {
    }

    /** 正确率 → 130-170 的量表分（1 分档）；一题没做时给 130 */
    public static int scaled(int correct, int total) {
        if (total <= 0) {
            return 130;
        }
        double ratio = Math.max(0, Math.min(1.0, correct * 1.0 / total));
        int score = 130 + (int) Math.round(ratio * 40);
        return Math.max(130, Math.min(170, score));
    }

    /** 总分 = Verbal + Quant（GRE 总分 260-340） */
    public static int totalScore(int verbalScaled, int quantScaled) {
        return verbalScaled + quantScaled;
    }

    /** AWA 0-6，0.5 一档；超出范围自动裁剪 */
    public static BigDecimal quantize(double raw) {
        double clamped = Math.max(0, Math.min(6.0, raw));
        double half = Math.round(clamped * 2) / 2.0;
        return BigDecimal.valueOf(half).setScale(1, RoundingMode.HALF_UP);
    }

    /** 多篇作文取平均，结果仍是 0.5 一档；没有成绩返回 null */
    public static BigDecimal average(List<BigDecimal> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        double sum = 0;
        for (BigDecimal item : list) {
            sum += item.doubleValue();
        }
        return quantize(sum / list.size());
    }

    /** AWA 档位说明（尽量贴近 ETS 的 6/5/4/3/2/1 描述） */
    public static String awaComment(double score) {
        if (score >= 5.5) {
            return "Outstanding：论证深入、例证具体、语言准确且句式有变化（6 分档）";
        }
        if (score >= 4.5) {
            return "Strong：立场清楚，论证有支撑，个别段落展开不足（5 分档）";
        }
        if (score >= 3.5) {
            return "Adequate：能回应题目，但论证偏浅、例子空泛（4 分档）";
        }
        if (score >= 2.5) {
            return "Limited：有立场但逻辑跳跃，组织或语言有明显问题（3 分档）";
        }
        if (score >= 1.5) {
            return "Weak：论点不清，句子之间缺少关系（2 分档）";
        }
        return "Deficient：几乎无法读出论证（1 分档）";
    }

    /** 量表分对应的粗略水平判断（仅用于页面提示） */
    public static String scaledComment(int scaled) {
        if (scaled >= 165) {
            return "接近满分区间，可以冲 330+";
        }
        if (scaled >= 158) {
            return "高于平均（约相当于 80 百分位）";
        }
        if (scaled >= 152) {
            return "中等偏上，接近多数理工科项目的要求线";
        }
        if (scaled >= 145) {
            return "中等，需要继续补短板题型";
        }
        return "基础区间，建议先回到分块练习补齐题型";
    }
}
