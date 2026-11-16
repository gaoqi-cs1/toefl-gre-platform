package com.demo.toefl.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 新托福（2026 版）分档换算工具
 *
 * 口径约定：
 *  1. 阅读 / 听力 / 口语 / 写作 各模块单独给 1-6 分档，步进 0.5；
 *  2. 总分 = 四项平均，1-6 分档，保留一位小数；
 *  3. 同时给出 0-120 的兼容分（band / 6 * 120），旧页面用这个字段；
 *  4. 1-6 分档与 CEFR 的对应关系只是展示用（B2 / C1 之类），不参与计算。
 */
public class FullScoreUtil {

    private FullScoreUtil() {
    }

    /**
     * 客观题：按正确率换 1-6 档（0.5 步进）
     * 正确率区间 → 档位是经验映射，题库换难度时可整体调这里的阈值。
     */
    public static BigDecimal bandFromCorrect(int correct, int total) {
        if (total <= 0) {
            return BigDecimal.valueOf(1.0).setScale(1, RoundingMode.HALF_UP);
        }
        double ratio = correct * 1.0 / total;
        double band;
        if (ratio >= 0.95) {
            band = 6.0;
        } else if (ratio >= 0.85) {
            band = 5.5;
        } else if (ratio >= 0.75) {
            band = 5.0;
        } else if (ratio >= 0.65) {
            band = 4.5;
        } else if (ratio >= 0.55) {
            band = 4.0;
        } else if (ratio >= 0.45) {
            band = 3.5;
        } else if (ratio >= 0.35) {
            band = 3.0;
        } else if (ratio >= 0.25) {
            band = 2.5;
        } else if (ratio >= 0.15) {
            band = 2.0;
        } else if (ratio > 0) {
            band = 1.5;
        } else {
            band = 1.0;
        }
        return BigDecimal.valueOf(band).setScale(1, RoundingMode.HALF_UP);
    }

    /** 把任意分数量化到 1-6、0.5 步进 */
    public static BigDecimal quantize(double raw) {
        double v = Math.round(raw * 2.0) / 2.0;
        if (v < 1.0) {
            v = 1.0;
        }
        if (v > 6.0) {
            v = 6.0;
        }
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP);
    }

    /** 多个分档求平均（跳过 null），结果量化到 0.5 */
    public static BigDecimal average(List<BigDecimal> bands) {
        double sum = 0;
        int count = 0;
        for (BigDecimal band : bands) {
            if (band != null) {
                sum += band.doubleValue();
                count++;
            }
        }
        if (count == 0) {
            return BigDecimal.valueOf(1.0).setScale(1, RoundingMode.HALF_UP);
        }
        return quantize(sum / count);
    }

    /** 1-6 分档 → 0-120 兼容分（6 档 = 120） */
    public static int legacyFromTotal(BigDecimal totalBand) {
        if (totalBand == null) {
            return 0;
        }
        int v = (int) Math.round(totalBand.doubleValue() / 6.0 * 120);
        if (v > 120) {
            v = 120;
        }
        if (v < 0) {
            v = 0;
        }
        return v;
    }

    /** 分档 → CEFR 展示标签 */
    public static String cefr(double band) {
        if (band >= 5.5) {
            return "C2";
        }
        if (band >= 5.0) {
            return "C1+";
        }
        if (band >= 4.0) {
            return "C1";
        }
        if (band >= 3.0) {
            return "B2";
        }
        if (band >= 2.0) {
            return "B1";
        }
        return "A2 及以下";
    }

    /** 分档 → 中文评语 */
    public static String comment(double band) {
        if (band >= 5.5) {
            return "接近母语学术水平";
        }
        if (band >= 5.0) {
            return "熟练，能完成学术任务";
        }
        if (band >= 4.0) {
            return "良好，表达清楚但细节略欠";
        }
        if (band >= 3.0) {
            return "基本能表达，组织与语言需加强";
        }
        return "基础薄弱，建议先练结构化表达";
    }
}
