package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 套题内的模块（一篇阅读 / 一段听力 / 一组口语 / 一组写作）
 * 对应表 full_section
 */
@Data
@TableName("full_section")
public class FullSection {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属套题 id */
    private Long suiteId;

    /** reading / listening / speaking / writing */
    private String moduleType;

    /** 同模块内的顺序 */
    private Integer sortIndex;

    private String title;

    /** 阅读正文；听力放原文（考试页不下发，成绩页才给） */
    private String contentText;

    private String audioUrl;

    /** 本模块限时（秒），0 表示不限 */
    private Integer limitSecond;

    /** 给考生的提示语 */
    private String hint;

    /** 本模块题目数（返回给页面用，不落库） */
    @TableField(exist = false)
    private Integer questionCount;
}
