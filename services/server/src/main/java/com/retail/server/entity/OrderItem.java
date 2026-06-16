package com.retail.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单明细实体，对应 sys_order_item。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_order_item")
public class OrderItem {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单 ID。
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 商品 ID。
     */
    @TableField("goods_id")
    private Long goodsId;

    /**
     * 商品名称快照。
     */
    @TableField("goods_name")
    private String goodsName;

    /**
     * 商品单价。
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 购买数量。
     */
    @TableField("quantity")
    private Integer quantity;
}