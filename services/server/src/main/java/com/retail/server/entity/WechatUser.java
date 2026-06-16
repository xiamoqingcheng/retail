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
 * 小程序用户实体，对应 sys_user_wechat 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_wechat")
public class WechatUser {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 微信 openid。
     */
    private String openid;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 头像 URL。
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 钱包余额。
     */
    @TableField("balance")
    private BigDecimal balance;

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
