package com.demo.toefl.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.toefl.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * User的Mapper接口，继承BaseMapper自带基础CRUD方法
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
