package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.TpoExamAnswer;
import com.demo.toefl.mapper.TpoExamAnswerMapper;
import com.demo.toefl.service.TpoExamAnswerService;
import org.springframework.stereotype.Service;

@Service
public class TpoExamAnswerServiceImpl extends ServiceImpl<TpoExamAnswerMapper, TpoExamAnswer>
        implements TpoExamAnswerService {
}
