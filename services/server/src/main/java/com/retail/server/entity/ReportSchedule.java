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
 * 定时报表调度配置（单行），对应 sys_report_schedule 表。
 * 服务端持久化，保证「重启系统/刷新页面后仍是上次设置」（记忆性）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_report_schedule")
public class ReportSchedule {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 是否启用定时生成：0 否，1 是。 */
    @TableField("enabled")
    private Integer enabled;

    /** 生成间隔 - 天。 */
    @TableField("interval_days")
    private Integer intervalDays;

    /** 生成间隔 - 时。 */
    @TableField("interval_hours")
    private Integer intervalHours;

    /** 生成间隔 - 分。 */
    @TableField("interval_minutes")
    private Integer intervalMinutes;

    /** 上次生成时间。 */
    @TableField("last_run_time")
    private LocalDateTime lastRunTime;

    /** 下次生成时间。 */
    @TableField("next_run_time")
    private LocalDateTime nextRunTime;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
