package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * tpo_section：一个「练习单元」（一篇阅读文章 / 一段听力音频 / 一组口语题 / 一组写作题）
 *
 * 本轮新增两列（见项目根 db-tpo-section-ext.sql）：
 *   title               —— 篇章标题（中英结合），列表页直接展示，不再用正文前 40 字凑标题
 *   content_translation —— 正文/听力原文的中文译文，提交答案后前端按钮可切换显示，允许为空
 *
 * 说明：下面的 getter/setter 是显式写死的（部分环境里 Lombok 注解处理器与
 * 新版 JDK 不兼容，显式方法能保证 javac 直接编译通过，Lombok 遇到已存在的方法会跳过）。
 */
@TableName("tpo_section")
public class TpoSection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tpoId;
    /** reading / listening / speaking / writing */
    private String type;
    /** 篇章标题（中英结合，列表页展示；为空时后端回退为正文首句） */
    private String title;
    private String contentText;
    /** 正文/听力原文的中文译文；为空则前端提示「本篇暂无译文」 */
    private String contentTranslation;
    private String audioUrl;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTpoId() { return tpoId; }
    public void setTpoId(Long tpoId) { this.tpoId = tpoId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }

    public String getContentTranslation() { return contentTranslation; }
    public void setContentTranslation(String contentTranslation) { this.contentTranslation = contentTranslation; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
