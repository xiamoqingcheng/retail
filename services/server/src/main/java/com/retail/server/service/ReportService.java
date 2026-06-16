package com.retail.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.server.entity.Report;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.ReportMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 报表生成 + 落库 + 内容解析的应用服务。手动生成与定时生成共用。
 */
@Slf4j
@Service
public class ReportService {

    private final ReportGenerationService generationService;
    private final ReportMapper reportMapper;
    private final ObjectMapper objectMapper;

    public ReportService(ReportGenerationService generationService, ReportMapper reportMapper,
                         ObjectMapper objectMapper) {
        this.generationService = generationService;
        this.reportMapper = reportMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成 [start, end) 报表并保存快照。
     *
     * @param type SCHEDULED / MANUAL
     */
    public Report generateAndSave(LocalDateTime start, LocalDateTime end, String type) {
        Map<String, Object> content = generationService.generate(start, end);
        String json;
        try {
            json = objectMapper.writeValueAsString(content);
        } catch (Exception ex) {
            throw new BusinessException(500, "报表内容序列化失败: " + ex.getMessage());
        }
        Report report = Report.builder()
                .title(String.valueOf(content.get("title")))
                .reportType(type)
                .periodStart(start)
                .periodEnd(end)
                .contentJson(json)
                .createTime(LocalDateTime.now())
                .build();
        reportMapper.insert(report);
        return report;
    }

    /**
     * 解析报表快照 JSON 为 Map。
     */
    public Map<String, Object> parseContent(Report report) {
        if (report == null || report.getContentJson() == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(report.getContentJson(), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new BusinessException(500, "报表内容解析失败: " + ex.getMessage());
        }
    }
}
