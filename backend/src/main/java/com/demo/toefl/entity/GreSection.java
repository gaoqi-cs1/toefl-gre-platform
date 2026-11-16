package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * GRE 分块练习单元（一组同类型题）
 * 对应表 gre_section —— 与 TOEFL 的 tpo_section / full_section 完全独立
 *
 * module_type: writing / verbal / quant
 */
@Data
@TableName("gre_section")
public class GreSection {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** writing / verbal / quant */
    private String moduleType;

    /** 同模块内顺序，决定页面卡片次序 */
    private Integer sortIndex;

    /** 练习单元名称，例如 "Text Completion · 单空专项" */
    private String title;

    /** 一句话说明考查重点 */
    private String subtitle;

    /** 阅读文章正文（只有 RC 才有，其余为 null） */
    private String contentText;

    /** 给考生的提示 */
    private String hint;

    /** 入门 / 中级 / 进阶 */
    private String difficulty;

    /** 建议限时（秒），0 表示不限 */
    private Integer limitSecond;

    /** 本单元题目数，返回给页面用，不落库 */
    @TableField(exist = false)
    private Integer questionCount;
}
