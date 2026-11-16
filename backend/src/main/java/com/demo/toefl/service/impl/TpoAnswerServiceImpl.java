package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.TpoAnswer;
import com.demo.toefl.mapper.TpoAnswerMapper;
import com.demo.toefl.service.TpoAnswerService;
import org.springframework.stereotype.Service;

@Service
public class TpoAnswerServiceImpl extends ServiceImpl<TpoAnswerMapper, TpoAnswer> implements TpoAnswerService {
}
