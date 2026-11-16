package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.TpoQuestion;
import com.demo.toefl.mapper.TpoQuestionMapper;
import com.demo.toefl.service.TpoQuestionService;
import org.springframework.stereotype.Service;

@Service
public class TpoQuestionServiceImpl extends ServiceImpl<TpoQuestionMapper, TpoQuestion> implements TpoQuestionService {
}
