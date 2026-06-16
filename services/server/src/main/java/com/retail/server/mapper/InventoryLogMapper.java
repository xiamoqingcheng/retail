package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.InventoryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存变更日志数据访问层。
 */
@Mapper
public interface InventoryLogMapper extends BaseMapper<InventoryLog> {
}