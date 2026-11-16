package com.demo.toefl.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.toefl.entity.WrongQuestion;
import java.util.List;

public interface WrongQuestionService extends IService<WrongQuestion> {

    /**
     * 根据用户id查询错题列表
     * 原方法 listByUserId() 内部调用了并不存在的 JwtUtil.getUserId()，
     * 是整个后端编译不过的原因之一，这里改成显式传 userId
     */
    List<WrongQuestion> listByUserId(Long userId);
}
