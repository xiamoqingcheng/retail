-- ======================================================
-- 升级脚本：摄像头巡检字段修复 + 多货架放宽 + 定时报表表
-- 适用于「库已存在、用旧 schema 建立」的场景，可重复执行（幂等）。
-- 后端 SchemaUpgradeRunner 启动时也会自动执行同样的迁移，本脚本供手动补跑。
-- 用法：mysql -u root retail_db < database/retail_camera_report_upgrade.sql
-- ======================================================
USE `retail_db`;
SET NAMES utf8mb4;

-- 1) 补齐 sys_camera.last_scan_time（缺失会导致摄像头列表查询 500、全量扫描事务回滚）
SET @add_last_scan_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `sys_camera` ADD COLUMN `last_scan_time` DATETIME NULL COMMENT ''最近巡检时间'' AFTER `status`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_camera'
    AND COLUMN_NAME = 'last_scan_time'
);
PREPARE add_last_scan_stmt FROM @add_last_scan_sql;
EXECUTE add_last_scan_stmt;
DEALLOCATE PREPARE add_last_scan_stmt;

-- 2) 放宽 sys_goods.shelf_id 至 VARCHAR(255)，容纳多货架逗号串
SET @widen_shelf_sql = (
  SELECT IF(
    CHARACTER_MAXIMUM_LENGTH < 255,
    'ALTER TABLE `sys_goods` MODIFY COLUMN `shelf_id` VARCHAR(255) NOT NULL COMMENT ''货架编号(可逗号分隔多货架)''',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_goods'
    AND COLUMN_NAME = 'shelf_id'
);
PREPARE widen_shelf_stmt FROM @widen_shelf_sql;
EXECUTE widen_shelf_stmt;
DEALLOCATE PREPARE widen_shelf_stmt;

-- 3) 定时报表调度配置表
CREATE TABLE IF NOT EXISTS `sys_report_schedule` (
  `id`               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `enabled`          TINYINT          NOT NULL DEFAULT 0 COMMENT '是否启用定时生成(0否/1是)',
  `interval_days`    INT              NOT NULL DEFAULT 1 COMMENT '生成间隔-天',
  `interval_hours`   INT              NOT NULL DEFAULT 0 COMMENT '生成间隔-时',
  `interval_minutes` INT              NOT NULL DEFAULT 0 COMMENT '生成间隔-分',
  `last_run_time`    DATETIME         NULL COMMENT '上次生成时间',
  `next_run_time`    DATETIME         NULL COMMENT '下次生成时间',
  `update_time`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时报表调度配置表';

-- 4) 销售报表记录表
CREATE TABLE IF NOT EXISTS `sys_report` (
  `id`           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title`        VARCHAR(255)     NOT NULL COMMENT '报表标题',
  `report_type`  VARCHAR(16)      NOT NULL DEFAULT 'MANUAL' COMMENT '生成方式(SCHEDULED/MANUAL)',
  `period_start` DATETIME         NOT NULL COMMENT '统计区间起',
  `period_end`   DATETIME         NOT NULL COMMENT '统计区间止',
  `content_json` LONGTEXT         NOT NULL COMMENT '报表内容JSON快照',
  `create_time`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_create_time` (`create_time`),
  KEY `idx_report_type_time` (`report_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售报表记录表';

-- 5) 用户反馈表（旧库可能整张缺失，导致 /feedback/manage 反复弹「服务器内部异常」）
CREATE TABLE IF NOT EXISTS `sys_feedback` (
  `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `user_id`         BIGINT UNSIGNED  DEFAULT NULL COMMENT '小程序用户ID',
  `feedback_type`   VARCHAR(32)      NOT NULL DEFAULT 'OTHER' COMMENT '反馈类型',
  `content`         VARCHAR(1000)    NOT NULL COMMENT '反馈内容',
  `contact`         VARCHAR(128)     DEFAULT NULL COMMENT '联系方式',
  `api_base_url`    VARCHAR(255)     DEFAULT NULL COMMENT '小程序当前API地址',
  `system_info`     VARCHAR(255)     DEFAULT NULL COMMENT '设备系统信息',
  `diagnostic_info` TEXT             DEFAULT NULL COMMENT '诊断信息',
  `status`          VARCHAR(32)      NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING/PROCESSING/RESOLVED/CLOSED)',
  `reply`           VARCHAR(500)     DEFAULT NULL COMMENT '处理回复',
  `create_time`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `resolved_time`   DATETIME         DEFAULT NULL COMMENT '处理完成时间',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_status` (`status`),
  KEY `idx_feedback_type` (`feedback_type`),
  KEY `idx_feedback_user_id` (`user_id`),
  KEY `idx_feedback_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户反馈表';
