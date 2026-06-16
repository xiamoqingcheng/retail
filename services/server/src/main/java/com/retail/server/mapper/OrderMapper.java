package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("""
        SELECT
          COALESCE(SUM(CASE WHEN create_time>=#{todayStart} AND create_time<=#{todayEnd}
            THEN total_amount END),0) AS todaySales,
          COUNT(CASE WHEN create_time>=#{todayStart} AND create_time<=#{todayEnd}
            THEN 1 END) AS todayCount,
          COUNT(DISTINCT CASE WHEN create_time>=#{todayStart} AND create_time<=#{todayEnd}
            THEN user_id END) AS todayVisitors,
          COALESCE(SUM(CASE WHEN create_time>=#{yestStart} AND create_time<=#{yestEnd}
            THEN total_amount END),0) AS yestSales,
          COUNT(CASE WHEN create_time>=#{yestStart} AND create_time<=#{yestEnd}
            THEN 1 END) AS yestCount,
          COUNT(DISTINCT CASE WHEN create_time>=#{yestStart} AND create_time<=#{yestEnd}
            THEN user_id END) AS yestVisitors
        FROM sys_order
        WHERE create_time>=#{yestStart} AND create_time<=#{todayEnd}
        """)
    Map<String, Object> selectDailySummary(@Param("todayStart") LocalDateTime todayStart,
                                           @Param("todayEnd") LocalDateTime todayEnd,
                                           @Param("yestStart") LocalDateTime yestStart,
                                           @Param("yestEnd") LocalDateTime yestEnd);

    @Select("""
        SELECT HOUR(create_time) AS h, COUNT(DISTINCT user_id) AS visitors
        FROM sys_order WHERE create_time>=#{start} AND create_time<=#{end}
        GROUP BY HOUR(create_time)
        """)
    List<Map<String, Object>> selectHourlyVisitors(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
}
