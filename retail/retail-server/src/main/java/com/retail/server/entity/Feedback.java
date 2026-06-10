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
 * 用户反馈实体，对应 sys_feedback 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_feedback")
public class Feedback {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("feedback_type")
    private String feedbackType;

    @TableField("content")
    private String content;

    @TableField("contact")
    private String contact;

    @TableField("api_base_url")
    private String apiBaseUrl;

    @TableField("system_info")
    private String systemInfo;

    @TableField("diagnostic_info")
    private String diagnosticInfo;

    @TableField("status")
    private String status;

    @TableField("reply")
    private String reply;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("resolved_time")
    private LocalDateTime resolvedTime;
}
