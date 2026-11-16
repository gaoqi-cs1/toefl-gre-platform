package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * GRE 套题内的模块（一段 Verbal / 一段 Quant / 一组 Writing）
 * 对应表 gre_suite_section
 */
@Data
@TableName("gre_suite_section")
public class GreSuiteSection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long suiteId;

    /** writing / verbal / quant */
    private String moduleType;

    private Integer sortIndex;

    private String title;

    /** 阅读文章正文（RC 才有） */
    private String contentText;

    private String hint;

    /** 本模块限时（秒） */
    private Integer limitSecond;

    @TableField(exist = false)
    private Integer questionCount;
}
