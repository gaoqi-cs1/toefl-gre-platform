package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.TpoSection;
import com.demo.toefl.mapper.TpoSectionMapper;
import com.demo.toefl.service.TpoSectionService;
import org.springframework.stereotype.Service;

@Service
public class TpoSectionServiceImpl extends ServiceImpl<TpoSectionMapper, TpoSection> implements TpoSectionService {
}
