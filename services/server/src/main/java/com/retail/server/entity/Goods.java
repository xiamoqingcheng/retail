package com.retail.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体，对应 sys_goods 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_goods")
public class Goods {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称。
     */
    @TableField("name")
    private String name;

    /**
     * 条形码。
     */
    @TableField("barcode")
    private String barcode;

    /**
     * 分类 ID。
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 商品价格。
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 商品库存。
     */
    @TableField("stock")
    private Integer stock;

    /**
     * 安全库存阈值。
     */
    @TableField("safe_stock")
    private Integer safeStock;

    /**
     * 货架编号。
     */
    @TableField("shelf_id")
    private String shelfId;

    /**
     * 商品图片地址。
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 状态：1 上架，0 下架。
     */
    @TableField("status")
    private Integer status;

    /**
     * 逻辑删除：0 未删除，1 已删除。
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

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
