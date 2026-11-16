package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模考记录实体
 * 对应 exam_record 表：一条记录 = 一次整套模考
 * 原先实体只映射了 score，没有映射 read_score / listen_score / speak_score /
 * write_score / total_score / exam_type，导致成绩页取不到分项分数（全是 undefined）
 */
@Data
@TableName("exam_record")
public class ExamRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long suiteId;
    private String suiteName;

    private Integer score;          // 旧字段：单科总分，保留兼容
    private String examStatus;     // doing / finished

    /** 阅读 / 听力 / 口语 / 写作 四项分数（0-30） */
    private Integer readScore;
    private Integer listenScore;
    private Integer speakScore;
    private Integer writeScore;

    /** 四科合计总分（0-120） */
    private Integer totalScore;

    /** 单科 single / 整套 suite */
    private String examType;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;

    /** 考试类型是数据库保留字段，用 @TableField 显式声明，避免驼峰推导歧义 */
    @TableField(exist = false)
    private Integer questionCount; // 非数据库字段：本次试卷题目数，仅用于返回给前端

    // ---------- 以下都是非数据库字段：只给成绩详情页做统计展示 ----------
    // 库里 exam_record 只存分数，没有"答对几题 / 共几题"这类统计，
    // 而前端结果页要显示 客观题答对 x/y、阅读 a/b、听力 c/d、口语写作已答几题，
    // 所以在实体上挂几个临时字段，由 /api/examRecord/detail 计算后填充。
    @TableField(exist = false)
    private Integer correctCount;      // 客观题答对题数
    @TableField(exist = false)
    private Integer totalCount;        // 客观题总题数
    @TableField(exist = false)
    private Integer readingCorrect;
    @TableField(exist = false)
    private Integer readingTotal;
    @TableField(exist = false)
    private Integer listeningCorrect;
    @TableField(exist = false)
    private Integer listeningTotal;
    @TableField(exist = false)
    private Integer speakingCount;     // 口语已作答题数
    @TableField(exist = false)
    private Integer writingCount;      // 写作已作答题数
}
