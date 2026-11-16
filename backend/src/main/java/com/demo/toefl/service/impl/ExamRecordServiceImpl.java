package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.ExamRecord;
import com.demo.toefl.mapper.ExamRecordMapper;
import com.demo.toefl.service.ExamRecordService;
import org.springframework.stereotype.Service;

@Service
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord>
        implements ExamRecordService {
}
