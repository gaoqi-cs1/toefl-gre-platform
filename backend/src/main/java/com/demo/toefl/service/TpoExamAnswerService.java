package com.demo.toefl.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.toefl.entity.TpoExamAnswer;

/**
 * 整卷模考作答明细业务接口
 * 之前只有 TpoExamAnswerMapper，没有对应的 service，
 * 整卷提交时没有可用的业务层，这里补上
 */
public interface TpoExamAnswerService extends IService<TpoExamAnswer> {
}
