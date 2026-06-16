package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 订单明细数据访问层。
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    /**
     * 批量写入订单明细。
     */
    @Insert("""
            <script>
            INSERT INTO sys_order_item (order_id, goods_id, goods_name, price, quantity)
            VALUES
            <foreach collection='items' item='item' separator=','>
                (#{item.orderId}, #{item.goodsId}, #{item.goodsName}, #{item.price}, #{item.quantity})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("items") List<OrderItem> items);

    /**
     * 近7日热销商品 TOP 8（SQL 聚合，避免 N+1 查询）。
     */
    @Select("""
            SELECT oi.goods_id AS goodsId, g.name AS name, SUM(oi.quantity) AS quantity
            FROM sys_order_item oi
            JOIN sys_order o ON o.id = oi.order_id
            JOIN sys_goods g ON g.id = oi.goods_id
            WHERE o.create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            GROUP BY oi.goods_id, g.name
            ORDER BY quantity DESC
            LIMIT 8
            """)
    List<Map<String, Object>> selectHotGoodsTop8();

    /**
     * 近7日按商品分类汇总销量（SQL 聚合）。
     */
    @Select("""
            SELECT g.category_id AS categoryId, COALESCE(c.name, '未分类') AS name,
                   SUM(oi.quantity) AS quantity
            FROM sys_order_item oi
            JOIN sys_order o ON o.id = oi.order_id
            JOIN sys_goods g ON g.id = oi.goods_id
            LEFT JOIN sys_goods_category c ON c.id = g.category_id
            WHERE o.create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
            GROUP BY g.category_id, c.name
            ORDER BY quantity DESC
            """)
    List<Map<String, Object>> selectCategorySalesTop7();
}