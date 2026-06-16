package com.retail.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首页轮播广告实体，对应 sys_ad 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_ad")
public class Ad {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 图片地址。
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 跳转链接。
     */
    @TableField("link_url")
    private String linkUrl;

    /**
     * 排序值，越小越靠前。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态：0 禁用，1 启用。
     */
    private Integer status;
}
