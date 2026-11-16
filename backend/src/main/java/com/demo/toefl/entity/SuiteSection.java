package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("suite_section")
public class SuiteSection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long suiteId;
    private Long sectionId;
    private String type;
}
