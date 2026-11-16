package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.TpoWrong;
import com.demo.toefl.mapper.TpoWrongMapper;
import com.demo.toefl.service.TpoWrongService;
import org.springframework.stereotype.Service;

@Service
public class TpoWrongServiceImpl extends ServiceImpl<TpoWrongMapper, TpoWrong> implements TpoWrongService {
}
