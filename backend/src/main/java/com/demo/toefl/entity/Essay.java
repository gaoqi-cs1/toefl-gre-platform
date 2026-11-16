package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 作文实体
 * 注意：数据库里表名是 user_essay，不是默认推导的 essay，
 * 必须用 @TableName 显式指定，否则运行时会报表不存在
 */
@Data
@TableName("user_essay")
public class Essay {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long sectionId;
    private String content;
    private LocalDateTime createTime;
}
