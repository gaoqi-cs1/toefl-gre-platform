package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.UserAnswer;
import com.demo.toefl.mapper.UserAnswerMapper;
import com.demo.toefl.service.UserAnswerService;
import org.springframework.stereotype.Service;

@Service
public class UserAnswerServiceImpl extends ServiceImpl<UserAnswerMapper, UserAnswer> implements UserAnswerService {
}
