package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 真题套题目录（独立数据，和单模块练习用的 tpo_suite 不是一套东西）
 * 对应表 full_suite
 */
@Data
@TableName("full_suite")
public class FullSuite {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套题名称，例如 TPO 01 全真模考 */
    private String title;

    /** 一句话介绍 */
    private String description;

    /** 难度：入门 / 中级 / 进阶 */
    private String difficulty;

    /** 建议总时长（分钟） */
    private Integer totalMinutes;

    private LocalDateTime createTime;

    // ---------- 以下为返回给列表页的统计字段，不落库 ----------
    @TableField(exist = false)
    private Integer readingCount;
    @TableField(exist = false)
    private Integer listeningCount;
    @TableField(exist = false)
    private Integer speakingCount;
    @TableField(exist = false)
    private Integer writingCount;
    @TableField(exist = false)
    private Integer questionCount;
}
