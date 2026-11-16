package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.WrongQuestion;
import com.demo.toefl.mapper.WrongQuestionMapper;
import com.demo.toefl.service.WrongQuestionService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class WrongQuestionServiceImpl extends ServiceImpl<WrongQuestionMapper, WrongQuestion>
        implements WrongQuestionService {

    @Override
    public List<WrongQuestion> listByUserId(Long userId) {
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongQuestion::getUserId, userId);
        wrapper.orderByDesc(WrongQuestion::getCreateTime);
        return list(wrapper);
    }
}
