package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tpo_suite")
public class TpoSuite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private Integer readingMinutes;
    private Integer listeningMinutes;
    private Integer speakingMinutes;
    private Integer writingMinutes;
    private Long createBy;
    private LocalDateTime createTime;
}
