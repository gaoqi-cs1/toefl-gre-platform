package com.demo.toefl.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 用户实体，和user数据库表一一对应
 * @TableName 指定映射的表名
 * @Data lombok注解，自动生成getter、setter、toString
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO) // 自增主键
    private Long id;
    private String username;
    private String password;
    private Date createTime;
}
