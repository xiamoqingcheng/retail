package com.retail.server.service;

import com.retail.server.entity.ReportSchedule;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.ReportScheduleMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时报表调度服务：从 DB 读取配置（持久化＝记忆性），按「天:时:分」间隔周期性生成报表。
 * 仿 {@link com.retail.server.scheduler.CameraScanScheduler} 的动态重调度方式。
 */
@Slf4j
@Service
@DependsOn("schemaUpgradeRunner")
public class ReportSchedulerService {

    /** 允许的最小间隔（秒），避免配置过小导致频繁生成。 */
    private static final long MIN_INTERVAL_SECONDS = 60;

    private final ReportScheduleMapper scheduleMapper;
    private final ReportService reportService;
    private final ThreadPoolTaskScheduler taskScheduler;

    private final Object lifecycleLock = new Object();
    private volatile ScheduledFuture<?> scheduledFuture;

    public ReportSchedulerService(ReportScheduleMapper scheduleMapper, ReportService reportService,
                                  @Qualifier("reportTaskScheduler") ThreadPoolTaskScheduler taskScheduler) {
        this.scheduleMapper = scheduleMapper;
        this.reportService = reportService;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void init() {
        synchronized (lifecycleLock) {
            reschedule(loadOrCreate());
        }
    }

    @PreDestroy
    public void destroy() {
        synchronized (lifecycleLock) {
            cancel();
        }
    }

    /**
     * 当前配置（不存在则创建一行默认 disabled 配置）。
     */
    public ReportSchedule currentConfig() {
        return loadOrCreate();
    }

    /**
     * 更新配置并重启调度。
     */
    public ReportSchedule updateConfig(int enabled, int days, int hours, int minutes) {
        if (days < 0 || hours < 0 || minutes < 0) {
            throw new BusinessException(400, "时间间隔不能为负数");
        }
        Duration interval = toDuration(days, hours, minutes);
        if (enabled == 1 && interval.getSeconds() < MIN_INTERVAL_SECONDS) {
            throw new BusinessException(400, "生成间隔至少为 1 分钟");
        }

        synchronized (lifecycleLock) {
            ReportSchedule config = loadOrCreate();
            config.setEnabled(enabled);
            config.setIntervalDays(days);
            config.setIntervalHours(hours);
            config.setIntervalMinutes(minutes);
            config.setUpdateTime(LocalDateTime.now());
            config.setNextRunTime(enabled == 1 ? LocalDateTime.now().plus(interval) : null);
            scheduleMapper.updateById(config);
            reschedule(config);
            log.info("更新定时报表配置: enabled={}, interval={}d{}h{}m", enabled, days, hours, minutes);
            return config;
        }
    }

    private ReportSchedule loadOrCreate() {
        List<ReportSchedule> list = scheduleMapper.selectList(null);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        ReportSchedule config = ReportSchedule.builder()
                .enabled(0)
                .intervalDays(1)
                .intervalHours(0)
                .intervalMinutes(0)
                .updateTime(LocalDateTime.now())
                .build();
        scheduleMapper.insert(config);
        return config;
    }

    private void reschedule(ReportSchedule config) {
        cancel();
        if (config.getEnabled() == null || config.getEnabled() != 1) {
            log.info("定时报表未启用，调度未启动");
            return;
        }
        Duration interval = toDuration(config.getIntervalDays(), config.getIntervalHours(), config.getIntervalMinutes());
        if (interval.getSeconds() < MIN_INTERVAL_SECONDS) {
            log.warn("定时报表间隔过小（<1分钟），调度未启动");
            return;
        }
        scheduledFuture = taskScheduler.scheduleAtFixedRate(this::runSafely, interval);
        log.info("定时报表调度已启动: interval={}", interval);
    }

    private void runSafely() {
        try {
            ReportSchedule config = loadOrCreate();
            Duration interval = toDuration(config.getIntervalDays(), config.getIntervalHours(), config.getIntervalMinutes());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = config.getLastRunTime() != null ? config.getLastRunTime() : now.minus(interval);
            reportService.generateAndSave(start, now, "SCHEDULED");
            config.setLastRunTime(now);
            config.setNextRunTime(now.plus(interval));
            scheduleMapper.updateById(config);
            log.info("定时报表已生成: [{} ~ {}]", start, now);
        } catch (Exception ex) {
            log.error("定时报表生成失败", ex);
        }
    }

    private void cancel() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
    }

    private Duration toDuration(Integer days, Integer hours, Integer minutes) {
        return Duration.ofDays(nz(days)).plusHours(nz(hours)).plusMinutes(nz(minutes));
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
