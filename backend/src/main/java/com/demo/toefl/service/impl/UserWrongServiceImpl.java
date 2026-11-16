package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.UserWrong;
import com.demo.toefl.mapper.UserWrongMapper;
import com.demo.toefl.service.UserWrongService;
import org.springframework.stereotype.Service;

@Service
public class UserWrongServiceImpl extends ServiceImpl<UserWrongMapper, UserWrong> implements UserWrongService {
}
