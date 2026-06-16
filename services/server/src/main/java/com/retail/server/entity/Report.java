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
 * 销售报表记录，对应 sys_report 表。保存报表内容 JSON 快照供回看与导出。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_report")
public class Report {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 报表标题。 */
    @TableField("title")
    private String title;

    /** 生成方式：SCHEDULED / MANUAL。 */
    @TableField("report_type")
    private String reportType;

    /** 统计区间起。 */
    @TableField("period_start")
    private LocalDateTime periodStart;

    /** 统计区间止。 */
    @TableField("period_end")
    private LocalDateTime periodEnd;

    /** 报表内容 JSON 快照。 */
    @TableField("content_json")
    private String contentJson;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;
}
