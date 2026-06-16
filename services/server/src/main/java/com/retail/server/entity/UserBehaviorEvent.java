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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_behavior_event")
public class UserBehaviorEvent {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("event_type")
    private String eventType;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("keyword")
    private String keyword;

    @TableField("quantity")
    private Integer quantity;

    @TableField("order_id")
    private Long orderId;

    @TableField("create_time")
    private LocalDateTime createTime;
}
