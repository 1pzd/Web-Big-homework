package com.iot.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.qa.entity.QaCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识分类表 Mapper。
 */
@Mapper
public interface QaCategoryMapper extends BaseMapper<QaCategory> {
}