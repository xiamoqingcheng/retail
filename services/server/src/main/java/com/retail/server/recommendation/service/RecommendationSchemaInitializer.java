package com.retail.server.recommendation.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RecommendationSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @Value("${recommendation.schema.auto-init:true}")
    private boolean autoInit;

    public RecommendationSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        if (!autoInit) {
            return;
        }
        ensureBehaviorEventTable();
        ensureGoodsDeletedColumn();
    }

    private void ensureBehaviorEventTable() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS sys_user_behavior_event (
                      id BIGINT NOT NULL AUTO_INCREMENT,
                      user_id BIGINT NOT NULL,
                      event_type VARCHAR(32) NOT NULL,
                      goods_id BIGINT NULL,
                      keyword VARCHAR(128) NULL,
                      quantity INT NOT NULL DEFAULT 1,
                      order_id BIGINT NULL,
                      create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (id),
                      KEY idx_ube_user_time (user_id, create_time),
                      KEY idx_ube_user_goods_time (user_id, goods_id, create_time),
                      KEY idx_ube_event_time (event_type, create_time),
                      KEY idx_ube_keyword_time (keyword, create_time)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
        } catch (DataAccessException ex) {
            log.warn("Recommendation behavior table auto-init skipped: {}", ex.getMessage());
        }
    }

    private void ensureGoodsDeletedColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = 'sys_goods'
                      AND column_name = 'deleted'
                    """, Integer.class);
            if (count == null || count > 0) {
                return;
            }
            jdbcTemplate.execute("ALTER TABLE sys_goods ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0");
        } catch (DataAccessException ex) {
            log.warn("Goods deleted column auto-init skipped: {}", ex.getMessage());
        }
    }
}
