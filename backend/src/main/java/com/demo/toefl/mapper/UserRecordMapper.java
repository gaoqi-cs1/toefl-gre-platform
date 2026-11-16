package com.demo.toefl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.toefl.entity.UserRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRecordMapper extends BaseMapper<UserRecord> {
}
