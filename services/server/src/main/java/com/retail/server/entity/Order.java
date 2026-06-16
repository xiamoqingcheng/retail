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
import java.time.LocalDateTime;

/**
 * 订单主表实体，对应 sys_order。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_order")
public class Order {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 下单用户 ID。
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 订单总金额。
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 订单状态。
     */
    @TableField("status")
    private String status;

    /**
     * 创建时间。
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
