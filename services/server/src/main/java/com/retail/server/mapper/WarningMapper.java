package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.Warning;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 库存告警数据访问层。
 */
@Mapper
public interface WarningMapper extends BaseMapper<Warning> {

    /**
     * 报表区间内的告警（含商品名，半开区间 [start, end)）。
     */
    @Select("""
            SELECT w.id, w.goods_id AS goodsId, g.name AS goodsName,
                   w.warning_type AS warningType, w.warning_msg AS warningMsg,
                   w.status, w.create_time AS createTime
            FROM sys_warning w
            LEFT JOIN sys_goods g ON g.id = w.goods_id
            WHERE w.create_time >= #{start} AND w.create_time < #{end}
            ORDER BY w.create_time DESC
            """)
    List<Map<String, Object>> selectInRange(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
