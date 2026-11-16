package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 单题作答实体，对应 user_answer 表
 * 表里还有 record_id、module_type 两列，原来没有映射，
 * 补上 module_type 是为了错题本/成绩页能区分模块
 */
@Data
@TableName("user_answer")
public class UserAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    private Long sectionId;
    private String userAnswer;
    private Integer isCorrect;
    /** reading / listening / speaking / writing */
    private String moduleType;
    private LocalDateTime createTime;
}
