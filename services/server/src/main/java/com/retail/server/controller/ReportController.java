package com.retail.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.retail.server.common.Result;
import com.retail.server.dto.ReportGenerateRequest;
import com.retail.server.dto.ReportScheduleRequest;
import com.retail.server.entity.Report;
import com.retail.server.entity.ReportSchedule;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.ReportMapper;
import com.retail.server.service.ReportExportService;
import com.retail.server.service.ReportSchedulerService;
import com.retail.server.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 定时报表 / 销售报表管理控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/report")
public class ReportController {

    private final ReportSchedulerService reportSchedulerService;
    private final ReportService reportService;
    private final ReportExportService reportExportService;
    private final ReportMapper reportMapper;

    public ReportController(ReportSchedulerService reportSchedulerService, ReportService reportService,
                            ReportExportService reportExportService, ReportMapper reportMapper) {
        this.reportSchedulerService = reportSchedulerService;
        this.reportService = reportService;
        this.reportExportService = reportExportService;
        this.reportMapper = reportMapper;
    }

    /** 查询定时报表配置。 */
    @GetMapping("/schedule")
    public Result<ReportSchedule> getSchedule() {
        return Result.success(reportSchedulerService.currentConfig());
    }

    /** 更新定时报表配置（启停 + 天/时/分间隔）。 */
    @PutMapping("/schedule")
    public Result<ReportSchedule> updateSchedule(@RequestBody ReportScheduleRequest request) {
        if (request == null) {
            throw new BusinessException(400, "请求体不能为空");
        }
        int enabled = request.enabled() != null && request.enabled() == 1 ? 1 : 0;
        int days = request.intervalDays() == null ? 0 : request.intervalDays();
        int hours = request.intervalHours() == null ? 0 : request.intervalHours();
        int minutes = request.intervalMinutes() == null ? 0 : request.intervalMinutes();
        return Result.success("配置已保存", reportSchedulerService.updateConfig(enabled, days, hours, minutes));
    }

    /** 手动生成指定区间报表（并落库）。 */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody ReportGenerateRequest request) {
        if (request == null) {
            throw new BusinessException(400, "请求体不能为空");
        }
        LocalDateTime start = parseDateTime(request.start(), false);
        LocalDateTime end = parseDateTime(request.end(), true);
        Report report = reportService.generateAndSave(start, end, "MANUAL");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", report.getId());
        data.put("content", reportService.parseContent(report));
        return Result.success("生成成功", data);
    }

    /** 分页查询历史报表（不含内容快照，列表更轻量）。 */
    @GetMapping("/list")
    public Result<Page<Report>> list(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long size) {
        Page<Report> result = reportMapper.selectPage(new Page<>(page, size),
                new QueryWrapper<Report>()
                        .select("id", "title", "report_type", "period_start", "period_end", "create_time")
                        .orderByDesc("create_time"));
        return Result.success(result);
    }

    /** 查询单份报表完整内容。 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(404, "报表不存在");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", report.getId());
        data.put("title", report.getTitle());
        data.put("reportType", report.getReportType());
        data.put("periodStart", report.getPeriodStart());
        data.put("periodEnd", report.getPeriodEnd());
        data.put("createTime", report.getCreateTime());
        data.put("content", reportService.parseContent(report));
        return Result.success(data);
    }

    /** 删除报表。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (reportMapper.selectById(id) == null) {
            throw new BusinessException(404, "报表不存在");
        }
        reportMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    /** 导出报表为 Excel / PDF。 */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id,
                                         @RequestParam(defaultValue = "xlsx") String format) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(404, "报表不存在");
        }
        Map<String, Object> content = reportService.parseContent(report);

        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] bytes = pdf ? reportExportService.toPdf(content) : reportExportService.toExcel(content);
        String filename = "report_" + id + (pdf ? ".pdf" : ".xlsx");
        MediaType mediaType = pdf
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, 200);
    }

    /**
     * 解析时间：支持 ISO（2026-06-10T00:00:00 / ...T00:00）、"yyyy-MM-dd HH:mm:ss"、纯日期 "yyyy-MM-dd"。
     * 纯日期时，endOfDay=true 取当日 23:59:59，否则取 00:00:00。
     */
    private LocalDateTime parseDateTime(String text, boolean endOfDay) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(400, "时间区间不能为空");
        }
        String trimmed = text.trim();
        if (trimmed.length() <= 10) {
            try {
                LocalDate date = LocalDate.parse(trimmed);
                return endOfDay ? date.atTime(LocalTime.of(23, 59, 59)) : date.atStartOfDay();
            } catch (Exception ex) {
                throw new BusinessException(400, "时间格式不正确: " + text);
            }
        }
        try {
            return LocalDateTime.parse(trimmed.replace(' ', 'T'));
        } catch (Exception ex) {
            throw new BusinessException(400, "时间格式不正确: " + text);
        }
    }
}
