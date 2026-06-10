package com.retail.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 库存变更日志实体，对应 sys_inventory_log。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_inventory_log")
public class InventoryLog {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品 ID。
     */
    @TableField("goods_id")
    private Long goodsId;

    /**
     * 变动数量（正数补货，负数扣减）。
     */
    @TableField("change_amount")
    private Integer changeAmount;

    /**
     * 变更后的当前库存。
     */
    @TableField("current_stock")
    private Integer currentStock;

    /**
     * 日志类型：SALE / RESTOCK / ADJUST / AI_DETECT。
     */
    @TableField("type")
    private String type;

    /**
     * 备注。
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间。
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}