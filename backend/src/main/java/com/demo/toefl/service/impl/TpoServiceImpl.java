package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.Tpo;
import com.demo.toefl.mapper.TpoMapper;
import com.demo.toefl.service.TpoService;
import org.springframework.stereotype.Service;

@Service
public class TpoServiceImpl extends ServiceImpl<TpoMapper, Tpo> implements TpoService {
}
