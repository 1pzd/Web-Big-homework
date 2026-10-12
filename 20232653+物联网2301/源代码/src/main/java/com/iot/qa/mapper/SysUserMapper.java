package com.iot.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iot.qa.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}