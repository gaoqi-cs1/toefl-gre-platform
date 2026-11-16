package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.SuiteSection;
import com.demo.toefl.mapper.SuiteSectionMapper;
import com.demo.toefl.service.SuiteSectionService;
import org.springframework.stereotype.Service;

@Service
public class SuiteSectionServiceImpl extends ServiceImpl<SuiteSectionMapper, SuiteSection>
        implements SuiteSectionService {
}
