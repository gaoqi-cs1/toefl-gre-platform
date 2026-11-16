package com.demo.toefl.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.toefl.entity.User;

/**
 * 用户业务接口
 * IService是MyBatisPlus封装的业务层父接口，自带save、getOne、count等方法
 */
public interface UserService extends IService<User> {
}
