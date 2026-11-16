package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GRE 套题目录
 * 对应表 gre_suite（独立于分块练习的 gre_section）
 */
@Data
@TableName("gre_suite")
public class GreSuite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    /** 入门 / 中级 / 进阶 */
    private String difficulty;

    /** 建议总时长（分钟） */
    private Integer totalMinutes;

    /** seed = 预置套题，generated = 一键生成 */
    private String source;

    private LocalDateTime createTime;

    // ---------- 返回给列表页的统计字段，不落库 ----------
    @TableField(exist = false)
    private Integer writingCount;
    @TableField(exist = false)
    private Integer verbalCount;
    @TableField(exist = false)
    private Integer quantCount;
    @TableField(exist = false)
    private Integer questionCount;
}
