package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 套题-篇章关联实体
 * 数据库 tpo_suite_section 表里保存模块类型的字段叫 module_type、排序字段叫 sort_index，
 * 原实体写的是 type 且没有 sortIndex，映射不上会读成 null，这里改成和表结构一致
 */
@Data
@TableName("tpo_suite_section")
public class TpoSuiteSection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long suiteId;
    private Long sectionId;
    /** reading / listening / speaking / writing */
    private String moduleType;
    /** 模块内排序，从0开始 */
    private Integer sortIndex;
}
