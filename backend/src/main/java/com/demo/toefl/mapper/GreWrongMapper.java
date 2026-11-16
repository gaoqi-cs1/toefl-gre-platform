package com.demo.toefl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.toefl.entity.GreWrong;
import org.apache.ibatis.annotations.Mapper;

/** GRE 错题本 mapper */
@Mapper
public interface GreWrongMapper extends BaseMapper<GreWrong> {
}
