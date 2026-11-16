package com.demo.toefl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.toefl.entity.Tpo;
import org.apache.ibatis.annotations.Mapper;

/**
 * TPO套题Mapper
 * 继承BaseMapper，自带基础增删改查
 */
@Mapper
public interface TpoMapper extends BaseMapper<Tpo> {
}
