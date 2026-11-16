package com.demo.toefl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.toefl.entity.UserRecord;
import com.demo.toefl.mapper.UserRecordMapper;
import com.demo.toefl.service.UserRecordService;
import org.springframework.stereotype.Service;

@Service
public class UserRecordServiceImpl extends ServiceImpl<UserRecordMapper, UserRecord> implements UserRecordService {
}
