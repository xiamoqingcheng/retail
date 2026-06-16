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
 * 库存告警实体，对应 sys_warning 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_warning")
public class Warning {

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
     * 告警类型：LOW_STOCK / OUT_OF_STOCK / EXPIRING。
     */
    @TableField("warning_type")
    private String warningType;

    /**
     * 告警内容。
     */
    @TableField("warning_msg")
    private String warningMsg;

    /**
     * 处理状态：0 未处理，1 已处理。
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间。
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 处理时间。
     */
    @TableField("resolve_time")
    private LocalDateTime resolveTime;
}
