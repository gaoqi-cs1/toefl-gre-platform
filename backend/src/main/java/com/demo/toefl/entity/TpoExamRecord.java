package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tpo_exam_record")
public class TpoExamRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long suiteId;
    private Long userId;
    private Integer correctCount;
    private Integer totalCount;
    private Integer readingCorrect;
    private Integer readingTotal;
    private Integer listeningCorrect;
    private Integer listeningTotal;
    private Integer speakingCount;
    private Integer writingCount;
    private Integer costSeconds;
    private LocalDateTime createTime;
}
