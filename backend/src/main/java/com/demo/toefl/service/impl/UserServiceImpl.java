package com.demo.toefl.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.User;
import com.demo.toefl.mapper.UserMapper;
import com.demo.toefl.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现类
 * ServiceImpl<Mapper,实体>：实现基础业务方法
 * @Service 交给spring管理
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
