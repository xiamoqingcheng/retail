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
 * 摄像头实体，对应 sys_camera 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_camera")
public class Camera {

    /**
     * 主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 摄像头编号。
     */
    @TableField("camera_no")
    private String cameraNo;

    /**
     * 绑定货架编号。
     */
    @TableField("shelf_id")
    private String shelfId;

    /**
     * 状态：1 正常，0 停用。
     */
    @TableField("status")
    private Integer status;

    /**
     * 最近巡检时间。
     */
    @TableField("last_scan_time")
    private LocalDateTime lastScanTime;

    /**
     * 创建时间。
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}