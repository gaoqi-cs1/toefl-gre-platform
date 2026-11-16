package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 错题本实体
 * 对应 user_wrong 表，用户手动添加错题
 */
@Data
@TableName("user_wrong")
public class UserWrong {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long questionId;
    // 加入错题本时间
    private Date addTime;
}
