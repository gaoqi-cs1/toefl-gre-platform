package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.TpoSuite;
import com.demo.toefl.mapper.TpoSuiteMapper;
import com.demo.toefl.service.TpoSuiteService;
import org.springframework.stereotype.Service;

@Service
public class TpoSuiteServiceImpl extends ServiceImpl<TpoSuiteMapper, TpoSuite>
        implements TpoSuiteService {
}
