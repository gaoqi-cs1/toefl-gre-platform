package com.demo.toefl.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.Essay;
import com.demo.toefl.mapper.EssayMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class EssayService extends ServiceImpl<EssayMapper, Essay> {

    @Resource
    private EssayMapper essayMapper;

    public void saveEssay(Long userId, Long sectionId, String content){
        Essay essay = new Essay();
        essay.setUserId(userId);
        essay.setSectionId(sectionId);
        essay.setContent(content);
        essay.setCreateTime(LocalDateTime.now());
        essayMapper.insert(essay);
    }
}
