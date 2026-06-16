-- ======================================================
-- 零售物品智能识别系统 - 数据库初始化脚本 (v2)
-- ======================================================
CREATE DATABASE IF NOT EXISTS `retail_db`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `retail_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------
-- 1. 系统用户表（管理端）
-- ---------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`    VARCHAR(64)      NOT NULL COMMENT '用户名',
  `password`    VARCHAR(255)     NOT NULL COMMENT '密码（明文/摘要）',
  `role`        ENUM('admin','customer') NOT NULL DEFAULT 'customer' COMMENT '角色',
  `status`      TINYINT          NOT NULL DEFAULT 1 COMMENT '状态(1启用/0禁用)',
  `create_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';

-- ---------------------------
-- 2. 商品分类表
-- ---------------------------
DROP TABLE IF EXISTS `sys_goods_category`;
CREATE TABLE `sys_goods_category` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`        VARCHAR(64)      NOT NULL COMMENT '分类名称',
  `sort_order`  INT              NOT NULL DEFAULT 0 COMMENT '排序值',
  `status`      TINYINT          NOT NULL DEFAULT 1 COMMENT '状态(1启用/0禁用)',
  `create_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';

-- ---------------------------
-- 3. 商品基础表
-- ---------------------------
DROP TABLE IF EXISTS `sys_goods`;
CREATE TABLE `sys_goods` (
  `id`           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`         VARCHAR(128)     NOT NULL COMMENT '商品名称',
  `barcode`      VARCHAR(64)      DEFAULT NULL COMMENT '条形码',
  `category_id`  BIGINT UNSIGNED  DEFAULT NULL COMMENT '分类ID',
  `price`        DECIMAL(10,2)    NOT NULL DEFAULT 0.00 COMMENT '商品价格',
  `stock`        INT              NOT NULL DEFAULT 0 COMMENT '库存数量',
  `safe_stock`   INT              NOT NULL DEFAULT 10 COMMENT '安全库存阈值',
  `shelf_id`     VARCHAR(64)      NOT NULL COMMENT '货架编号',
  `image_url`    VARCHAR(255)     DEFAULT NULL COMMENT '商品图片URL',
  `status`       TINYINT          NOT NULL DEFAULT 1 COMMENT '状态(1上架/0下架)',
  `deleted`      TINYINT          NOT NULL DEFAULT 0 COMMENT '逻辑删除(0未删除/1已删除)',
  `create_time`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_goods_shelf_id` (`shelf_id`),
  KEY `idx_goods_category_id` (`category_id`),
  KEY `idx_goods_barcode` (`barcode`),
  KEY `idx_goods_status` (`status`),
  KEY `idx_goods_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品基础表';

-- ---------------------------
-- 4. 订单主表
-- ---------------------------
DROP TABLE IF EXISTS `sys_order`;
CREATE TABLE `sys_order` (
  `id`           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `user_id`      BIGINT UNSIGNED  NOT NULL COMMENT '用户ID',
  `total_amount` DECIMAL(10,2)    NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  `status`       VARCHAR(32)      NOT NULL DEFAULT 'PENDING' COMMENT '订单状态(PENDING/PAID/COMPLETED/CANCELLED)',
  `create_time`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_user_id` (`user_id`),
  KEY `idx_order_status` (`status`),
  KEY `idx_order_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单主表';

-- ---------------------------
-- 5. 订单明细表
-- ---------------------------
DROP TABLE IF EXISTS `sys_order_item`;
CREATE TABLE `sys_order_item` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id`    BIGINT UNSIGNED  NOT NULL COMMENT '订单ID',
  `goods_id`    BIGINT UNSIGNED  NOT NULL COMMENT '商品ID',
  `goods_name`  VARCHAR(128)     NOT NULL DEFAULT '' COMMENT '商品名称快照',
  `price`       DECIMAL(10,2)    NOT NULL DEFAULT 0.00 COMMENT '商品单价快照',
  `quantity`    INT              NOT NULL DEFAULT 1 COMMENT '购买数量',
  PRIMARY KEY (`id`),
  KEY `idx_order_item_order_id` (`order_id`),
  KEY `idx_order_item_goods_id` (`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细表';

-- ---------------------------
-- 6. 库存告警表
-- ---------------------------
DROP TABLE IF EXISTS `sys_warning`;
CREATE TABLE `sys_warning` (
  `id`             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '告警ID',
  `goods_id`       BIGINT UNSIGNED  NOT NULL COMMENT '商品ID',
  `warning_type`   VARCHAR(32)      NOT NULL DEFAULT 'LOW_STOCK' COMMENT '告警类型(LOW_STOCK/OUT_OF_STOCK/EXPIRING)',
  `warning_msg`    VARCHAR(255)     NOT NULL COMMENT '告警内容',
  `status`         TINYINT          NOT NULL DEFAULT 0 COMMENT '处理状态(0未处理/1已处理)',
  `create_time`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `resolve_time`   DATETIME         DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_warning_goods_id` (`goods_id`),
  KEY `idx_warning_status` (`status`),
  KEY `idx_warning_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存告警表';

-- ---------------------------
-- 7. 库存变更日志表
-- ---------------------------
DROP TABLE IF EXISTS `sys_inventory_log`;
CREATE TABLE `sys_inventory_log` (
  `id`            BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `goods_id`      BIGINT UNSIGNED  NOT NULL COMMENT '商品ID',
  `change_amount` INT              NOT NULL DEFAULT 0 COMMENT '变动数量(正补货/负扣减)',
  `current_stock` INT              NOT NULL DEFAULT 0 COMMENT '变动后当前库存',
  `type`          VARCHAR(32)      NOT NULL COMMENT '日志类型(SALE/RESTOCK/ADJUST/AI_DETECT)',
  `remark`        VARCHAR(255)     DEFAULT NULL COMMENT '备注',
  `create_time`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_inv_log_goods_id` (`goods_id`),
  KEY `idx_inv_log_type` (`type`),
  KEY `idx_inv_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存变更日志表';

-- ---------------------------
-- 8. 小程序用户表
-- ---------------------------
DROP TABLE IF EXISTS `sys_user_wechat`;
CREATE TABLE `sys_user_wechat` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `openid`      VARCHAR(128)     NOT NULL COMMENT '微信用户唯一标识',
  `nickname`    VARCHAR(64)      DEFAULT NULL COMMENT '昵称',
  `avatar_url`  VARCHAR(255)     DEFAULT NULL COMMENT '头像URL',
  `balance`     DECIMAL(10,2)    NOT NULL DEFAULT 0.00 COMMENT '钱包余额',
  `create_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wechat_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序用户表';

-- ---------------------------
-- 9. 首页轮播广告表
-- ---------------------------
DROP TABLE IF EXISTS `sys_ad`;
CREATE TABLE `sys_ad` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `image_url`   VARCHAR(255)     NOT NULL COMMENT '轮播图图片URL',
  `link_url`    VARCHAR(255)     DEFAULT NULL COMMENT '点击跳转链接',
  `sort_order`  INT              NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `status`      TINYINT          NOT NULL DEFAULT 1 COMMENT '状态(0禁用/1启用)',
  PRIMARY KEY (`id`),
  KEY `idx_ad_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页轮播广告表';

-- ---------------------------
-- 10. 用户行为事件表（推荐/搜索）
-- ---------------------------
DROP TABLE IF EXISTS `sys_user_behavior_event`;
CREATE TABLE `sys_user_behavior_event` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '事件ID',
  `user_id`     BIGINT UNSIGNED  NOT NULL COMMENT '小程序用户ID',
  `event_type`  VARCHAR(32)      NOT NULL COMMENT '事件类型(SEARCH/VIEW/PURCHASE/CANCEL)',
  `goods_id`    BIGINT UNSIGNED  DEFAULT NULL COMMENT '商品ID',
  `keyword`     VARCHAR(128)     DEFAULT NULL COMMENT '搜索关键词',
  `quantity`    INT              NOT NULL DEFAULT 1 COMMENT '事件数量/权重',
  `order_id`    BIGINT UNSIGNED  DEFAULT NULL COMMENT '订单ID',
  `create_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_behavior_user_time` (`user_id`, `create_time`),
  KEY `idx_behavior_type_time` (`event_type`, `create_time`),
  KEY `idx_behavior_goods_time` (`goods_id`, `create_time`),
  KEY `idx_behavior_keyword_time` (`keyword`, `create_time`),
  KEY `idx_behavior_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户行为事件表';

-- ---------------------------
-- 11. 用户反馈表
-- ---------------------------
DROP TABLE IF EXISTS `sys_feedback`;
CREATE TABLE `sys_feedback` (
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

-- ---------------------------
-- 12. 摄像头管理表
-- ---------------------------
DROP TABLE IF EXISTS `sys_camera`;
CREATE TABLE `sys_camera` (
  `id`             BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '摄像头ID',
  `camera_no`      VARCHAR(32)      NOT NULL COMMENT '摄像头编号',
  `shelf_id`       VARCHAR(64)      NOT NULL COMMENT '绑定货架编号',
  `status`         TINYINT          NOT NULL DEFAULT 1 COMMENT '状态(1正常/0停用)',
  `last_scan_time` DATETIME         NULL COMMENT '最近巡检时间',
  `create_time`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_camera_no` (`camera_no`),
  KEY `idx_camera_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='摄像头管理表';

-- ======================================================
-- 初始数据
-- ======================================================

-- 管理员
INSERT INTO `sys_user` (`username`, `password`, `role`, `status`)
VALUES ('admin', '$2b$12$LHomOdwgjPlF39ZayDqWgeDNRylDzCmAH41B0hnaKEB5vcZJqZgGK', 'admin', 1)
ON DUPLICATE KEY UPDATE `password` = '$2b$12$LHomOdwgjPlF39ZayDqWgeDNRylDzCmAH41B0hnaKEB5vcZJqZgGK', `role` = 'admin';

INSERT INTO `sys_user` (`username`, `password`, `role`, `status`)
VALUES ('zhaosheng', '$2b$12$KmNCmPVilbQlo7nXctmMLuPCvSK2fz6HhtLyaSpjNKo.fNYlvVK5O', 'admin', 1)
ON DUPLICATE KEY UPDATE `password` = '$2b$12$KmNCmPVilbQlo7nXctmMLuPCvSK2fz6HhtLyaSpjNKo.fNYlvVK5O';

INSERT INTO `sys_user` (`username`, `password`, `role`, `status`)
VALUES ('chenzhifeng', '$2b$12$2iZ3MpS3zzwN1EmHQKncdenyHrQbCUolO2HyEPPYzCyzo/RwCBm6i', 'admin', 1)
ON DUPLICATE KEY UPDATE `password` = '$2b$12$2iZ3MpS3zzwN1EmHQKncdenyHrQbCUolO2HyEPPYzCyzo/RwCBm6i';

-- 分类与商品数据请执行 retail_goods.sql

-- 摄像头
INSERT INTO `sys_camera` (`camera_no`, `shelf_id`, `status`) VALUES
('C01', 'SHELF-A', 1),
('C02', 'SHELF-B', 1),
('C03', 'SHELF-C', 1)
ON DUPLICATE KEY UPDATE `shelf_id` = VALUES(`shelf_id`), `status` = VALUES(`status`);

-- 轮播广告
INSERT INTO `sys_ad` (`image_url`, `link_url`, `sort_order`, `status`) VALUES
('/uploads/ad-1.jpg', '/pages/goods/detail?id=1', 1, 1),
('/uploads/ad-2.jpg', '/pages/activity/spring-sale', 2, 1)
ON DUPLICATE KEY UPDATE `image_url` = VALUES(`image_url`), `sort_order` = VALUES(`sort_order`);

-- 告警数据由系统运行时自动生成（下单时检测 safe_stock 阈值）

SET FOREIGN_KEY_CHECKS = 1;
