package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * TPO套题实体
 * 对应数据库 tpo 表：存储TPO编号、标题
 */
@Data
@TableName("tpo")
public class Tpo {
    // 自增主键
    @TableId(type = IdType.AUTO)
    private Long id;
    // TPO编号：TPO01、TPO02
    private String tpoNo;
    // 套题备注标题
    private String title;
    // 创建时间
    private Date createTime;
}
