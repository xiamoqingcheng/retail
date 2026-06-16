package com.retail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.retail.server.entity.UserBehaviorEvent;
import com.retail.server.recommendation.model.GoodsBehaviorAggregate;
import com.retail.server.recommendation.model.GoodsSearchDocument;
import com.retail.server.recommendation.model.KeywordAggregate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserBehaviorEventMapper extends BaseMapper<UserBehaviorEvent> {

    /**
     * 报表区间内按事件类型统计行为数量（半开区间 [start, end)）。
     */
    @Select("""
            SELECT event_type AS type, COUNT(*) AS cnt, COALESCE(SUM(quantity), 0) AS qty
            FROM sys_user_behavior_event
            WHERE create_time >= #{start} AND create_time < #{end}
            GROUP BY event_type
            """)
    List<Map<String, Object>> countByTypeInRange(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    @Select("""
            SELECT goods_id AS goodsId,
                   SUM(CASE WHEN event_type = 'VIEW' THEN quantity ELSE 0 END) AS viewCount,
                   SUM(CASE WHEN event_type = 'PURCHASE' THEN quantity ELSE 0 END) AS purchaseQty,
                   SUM(CASE WHEN event_type = 'CANCEL' THEN quantity ELSE 0 END) AS cancelQty,
                   MAX(create_time) AS latestEventTime
            FROM sys_user_behavior_event
            WHERE user_id = #{userId}
              AND goods_id IS NOT NULL
              AND create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY goods_id
            """)
    List<GoodsBehaviorAggregate> selectUserGoodsAggregates(
            @Param("userId") Long userId,
            @Param("days") int days);

    @Select("""
            SELECT keyword AS keyword, SUM(quantity) AS countValue
            FROM sys_user_behavior_event
            WHERE user_id = #{userId}
              AND event_type = 'SEARCH'
              AND keyword IS NOT NULL
              AND keyword <> ''
              AND create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY keyword
            ORDER BY countValue DESC
            LIMIT #{limit}
            """)
    List<KeywordAggregate> selectUserKeywordAggregates(
            @Param("userId") Long userId,
            @Param("days") int days,
            @Param("limit") int limit);

    @Select("""
            SELECT oi.goods_id AS goodsId,
                   0 AS viewCount,
                   SUM(CASE WHEN o.status IN ('PAID', 'COMPLETED') THEN oi.quantity ELSE 0 END) AS purchaseQty,
                   SUM(CASE WHEN o.status = 'CANCELLED' THEN oi.quantity ELSE 0 END) AS cancelQty,
                   MAX(o.create_time) AS latestEventTime
            FROM sys_order_item oi
            JOIN sys_order o ON o.id = oi.order_id
            WHERE o.user_id = #{userId}
              AND o.status IN ('PAID', 'COMPLETED', 'CANCELLED')
              AND o.create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY oi.goods_id
            """)
    List<GoodsBehaviorAggregate> selectUserOrderGoodsAggregates(
            @Param("userId") Long userId,
            @Param("days") int days);

    @Select("""
            SELECT g.id, g.name, g.barcode, g.category_id AS categoryId, c.name AS categoryName,
                   g.price, g.stock, g.shelf_id AS shelfId, g.image_url AS imageUrl
            FROM sys_order_item oi
            JOIN sys_order o ON o.id = oi.order_id
            JOIN sys_goods g ON g.id = oi.goods_id
            LEFT JOIN sys_goods_category c ON c.id = g.category_id
            WHERE o.status IN ('PAID', 'COMPLETED')
              AND g.status = 1
              AND o.create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
            GROUP BY g.id, g.name, g.barcode, g.category_id, c.name, g.price, g.stock, g.shelf_id, g.image_url
            ORDER BY SUM(oi.quantity) DESC, MAX(o.create_time) DESC
            LIMIT #{limit}
            """)
    List<GoodsSearchDocument> selectRecentPurchasedGoodsDocuments(
            @Param("days") int days,
            @Param("limit") int limit);
}
