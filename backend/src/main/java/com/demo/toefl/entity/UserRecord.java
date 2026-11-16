package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 用户做题记录实体
 * 对应 user_record 表：记录用户每一题作答情况、耗时、对错
 */
@Data
@TableName("user_record")
public class UserRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    // 当前登录用户id
    private Long userId;
    // 题目id
    private Long questionId;
    // 用户选择的答案
    private String userAnswer;
    // 是否正确 1正确 0错误
    private Integer isRight;
    // 做题耗时，单位：秒
    private Integer costTime;
    private Date createTime;
}
