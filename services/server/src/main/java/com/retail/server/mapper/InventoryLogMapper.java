package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.InventoryLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 库存变更日志数据访问层。
 */
@Mapper
public interface InventoryLogMapper extends BaseMapper<InventoryLog> {

    /**
     * 报表区间内的异常库存调整（ADJUST/AI_DETECT，半开区间 [start, end)）。
     */
    @Select("""
            SELECT l.id, l.goods_id AS goodsId, g.name AS goodsName, l.change_amount AS changeAmount,
                   l.current_stock AS currentStock, l.type, l.remark, l.create_time AS createTime
            FROM sys_inventory_log l
            LEFT JOIN sys_goods g ON g.id = l.goods_id
            WHERE l.create_time >= #{start} AND l.create_time < #{end}
              AND l.type IN ('ADJUST', 'AI_DETECT')
            ORDER BY l.create_time DESC
            """)
    List<Map<String, Object>> selectAdjustInRange(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /**
     * 报表区间内的补货记录汇总（RESTOCK），按商品累计补货量。
     */
    @Select("""
            SELECT l.goods_id AS goodsId, g.name AS goodsName, SUM(l.change_amount) AS restockQty
            FROM sys_inventory_log l
            LEFT JOIN sys_goods g ON g.id = l.goods_id
            WHERE l.create_time >= #{start} AND l.create_time < #{end}
              AND l.type = 'RESTOCK'
            GROUP BY l.goods_id, g.name
            ORDER BY restockQty DESC
            """)
    List<Map<String, Object>> selectRestockInRange(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
}