package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.Warning;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存告警数据访问层。
 */
@Mapper
public interface WarningMapper extends BaseMapper<Warning> {
}
