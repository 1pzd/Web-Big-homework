package com.iot.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.qa.entity.QaSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 问答会话表 Mapper。
 */
@Mapper
public interface QaSessionMapper extends BaseMapper<QaSession> {
}