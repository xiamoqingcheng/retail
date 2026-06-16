USE `retail_db`;
SET NAMES utf8mb4;

SET @add_deleted_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `sys_goods` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除(0未删除/1已删除)'' AFTER `status`',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_goods'
    AND COLUMN_NAME = 'deleted'
);
PREPARE add_deleted_stmt FROM @add_deleted_sql;
EXECUTE add_deleted_stmt;
DEALLOCATE PREPARE add_deleted_stmt;

CREATE TABLE IF NOT EXISTS `sys_user_behavior_event` (
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
