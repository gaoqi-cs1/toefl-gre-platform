package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单题作答实体，对应 tpo_answer 表
 * 之前 mapper/service 里引用过 TpoAnswer，但实体类文件丢失，导致整个项目编译失败
 * 字段与 tpo_answer 表结构一一对应
 */
@Data
@TableName("tpo_answer")
public class TpoAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 作答用户
    private Long userId;

    // 题目id
    private Long questionId;

    // 所属篇章id
    private Long sectionId;

    // 用户答案（听力阅读为A/B/C/D，口语写作为文字内容）
    private String userAnswer;

    // 是否正确 1正确 0错误
    private Integer isCorrect;

    // 口语录音地址，其他模块为空
    private String audioUrl;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
