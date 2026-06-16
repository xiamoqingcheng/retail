package com.retail.server.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 幂等数据库结构升级器。
 *
 * <p>历史上的库可能是用旧版 schema 建立的，{@code start.ps1} 仅在库不存在时才导入
 * {@code retail_db.sql}，已存在的库不会被迁移。这里在启动时按需补齐缺失的列与表，
 * 保证即使管理员不手动执行升级 SQL 也能正常运行。</p>
 *
 * <ul>
 *   <li>补齐 {@code sys_camera.last_scan_time} 列（缺失会导致摄像头列表查询 500、
 *       全量扫描事务回滚）。</li>
 *   <li>放宽 {@code sys_goods.shelf_id} 到 VARCHAR(255)，以容纳「逗号分隔的多货架」。</li>
 *   <li>建立定时报表所需的 {@code sys_report_schedule} 与 {@code sys_report} 表。</li>
 * </ul>
 */
@Slf4j
@Component
public class SchemaUpgradeRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaUpgradeRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        ensureCameraLastScanTimeColumn();
        widenGoodsShelfIdColumn();
        ensureReportScheduleTable();
        ensureReportTable();
    }

    /**
     * 补齐 sys_camera.last_scan_time（缺失是「服务器内部异常」弹窗与扫描不落库的根因）。
     */
    private void ensureCameraLastScanTimeColumn() {
        try {
            if (columnExists("sys_camera", "last_scan_time")) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE sys_camera ADD COLUMN last_scan_time DATETIME NULL COMMENT '最近巡检时间'");
            log.info("[schema-upgrade] sys_camera.last_scan_time 已补齐");
        } catch (DataAccessException ex) {
            log.warn("[schema-upgrade] sys_camera.last_scan_time 补齐跳过: {}", ex.getMessage());
        }
    }

    /**
     * 放宽 sys_goods.shelf_id 长度，容纳多货架逗号串（如 "SHELF-A,SHELF-B,SHELF-C"）。
     */
    private void widenGoodsShelfIdColumn() {
        try {
            Integer length = jdbcTemplate.queryForObject("""
                    SELECT CHARACTER_MAXIMUM_LENGTH
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'sys_goods'
                      AND COLUMN_NAME = 'shelf_id'
                    """, Integer.class);
            if (length == null || length >= 255) {
                return;
            }
            jdbcTemplate.execute(
                    "ALTER TABLE sys_goods MODIFY COLUMN shelf_id VARCHAR(255) NOT NULL COMMENT '货架编号(可逗号分隔多货架)'");
            log.info("[schema-upgrade] sys_goods.shelf_id 已放宽至 VARCHAR(255)");
        } catch (DataAccessException ex) {
            log.warn("[schema-upgrade] sys_goods.shelf_id 放宽跳过: {}", ex.getMessage());
        }
    }

    /**
     * 定时报表调度配置表（单行，持久化＝记忆性）。
     */
    private void ensureReportScheduleTable() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS sys_report_schedule (
                      id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                      enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用定时生成(0否/1是)',
                      interval_days INT NOT NULL DEFAULT 1 COMMENT '生成间隔-天',
                      interval_hours INT NOT NULL DEFAULT 0 COMMENT '生成间隔-时',
                      interval_minutes INT NOT NULL DEFAULT 0 COMMENT '生成间隔-分',
                      last_run_time DATETIME NULL COMMENT '上次生成时间',
                      next_run_time DATETIME NULL COMMENT '下次生成时间',
                      update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                      PRIMARY KEY (id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时报表调度配置表'
                    """);
        } catch (DataAccessException ex) {
            log.warn("[schema-upgrade] sys_report_schedule 建表跳过: {}", ex.getMessage());
        }
    }

    /**
     * 已生成报表记录表（保存报表内容快照，供回看/导出）。
     */
    private void ensureReportTable() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS sys_report (
                      id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                      title VARCHAR(255) NOT NULL COMMENT '报表标题',
                      report_type VARCHAR(16) NOT NULL DEFAULT 'MANUAL' COMMENT '生成方式(SCHEDULED/MANUAL)',
                      period_start DATETIME NOT NULL COMMENT '统计区间起',
                      period_end DATETIME NOT NULL COMMENT '统计区间止',
                      content_json LONGTEXT NOT NULL COMMENT '报表内容JSON快照',
                      create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      PRIMARY KEY (id),
                      KEY idx_report_create_time (create_time),
                      KEY idx_report_type_time (report_type, create_time)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售报表记录表'
                    """);
        } catch (DataAccessException ex) {
            log.warn("[schema-upgrade] sys_report 建表跳过: {}", ex.getMessage());
        }
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }
}
