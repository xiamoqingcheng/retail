package com.retail.server.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.retail.server.entity.Camera;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.CameraMapper;
import com.retail.server.mapper.GoodsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * B 端摄像头定时巡检与分批调度器。
 */
@Slf4j
@Component
public class CameraScanScheduler {

    private static final int CAMERA_STATUS_NORMAL = 1;

        // 32x32 黑色 JPEG（经 Python cv2 验证可解码）。
    private static final String MOCK_IMAGE_BASE64 =
            "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAIBAQEBAQIBAQECAgICAgQDAgICAgUEBAMEBgUGBgYFBgYGBwkIBgcJBwYGCAsICQoKCgoKBggLDAsKDAkKCgr/"
                + "2wBDAQICAgICAgUDAwUKBwYHCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgr/wAARCAAgACADASIAAhEBAxEB/"
                + "8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2Jy"
                + "ggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLD"
                + "xMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3"
                + "AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6"
                + "goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD+f+ii"
                + "igAooooAKKKKACiiigD/2Q==";

    private final CameraMapper cameraMapper;
    private final GoodsMapper goodsMapper;
    private final RestTemplate restTemplate;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final com.retail.server.service.CameraCaptureService cameraCaptureService;

    private final Object lifecycleLock = new Object();
    private volatile ScheduledFuture<?> scheduledFuture;

    @Value("${ai.service-url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${camera.scheduler.interval-minutes:5}")
    private volatile Integer intervalMinutes;

    @Value("${camera.scheduler.batch-size:10}")
    private volatile Integer batchSize;

    @Value("${camera.scheduler.auto-disable-unavailable:true}")
    private Boolean autoDisableUnavailable;

    public CameraScanScheduler(
            CameraMapper cameraMapper,
            GoodsMapper goodsMapper,
            RestTemplate restTemplate,
            @Qualifier("cameraTaskScheduler") ThreadPoolTaskScheduler taskScheduler,
            com.retail.server.service.CameraCaptureService cameraCaptureService) {
        this.cameraMapper = cameraMapper;
        this.goodsMapper = goodsMapper;
        this.restTemplate = restTemplate;
        this.taskScheduler = taskScheduler;
        this.cameraCaptureService = cameraCaptureService;
    }

    @PostConstruct
    public void init() {
        synchronized (lifecycleLock) {
            restartTaskLocked();
        }
    }

    @PreDestroy
    public void destroy() {
        synchronized (lifecycleLock) {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                scheduledFuture = null;
            }
        }
    }

    /**
     * 动态修改巡检配置并重启任务。
     */
    public SchedulerConfig updateSchedulerConfig(int intervalMinutes, int batchSize) {
        validateConfig(intervalMinutes, batchSize);

        synchronized (lifecycleLock) {
            this.intervalMinutes = intervalMinutes;
            this.batchSize = batchSize;
            log.info("更新摄像头巡检调度配置: intervalMinutes={}, batchSize={}", this.intervalMinutes, this.batchSize);
            restartTaskLocked();
            return new SchedulerConfig(this.intervalMinutes, this.batchSize);
        }
    }

    /**
     * 手动立即触发一次全量扫描。
     */
    public void triggerNow() {
        log.info("收到手动触发全量巡检请求");
        executeScanBatchTask();
        log.info("手动触发全量巡检执行完成");
    }

    /**
     * 返回当前调度配置。
     */
    public SchedulerConfig currentConfig() {
        int interval = intervalMinutes == null ? 5 : intervalMinutes;
        int size = batchSize == null ? 10 : batchSize;
        return new SchedulerConfig(interval, size);
    }

    /**
     * 核心执行逻辑：分批扫描摄像头并同步货架结果。
     */
    public void executeScanBatchTask() {
        log.info("开始执行摄像头巡检任务");
        List<Camera> cameras = listNormalCameras();
        if (CollectionUtils.isEmpty(cameras)) {
            log.info("摄像头巡检跳过：无可用正常摄像头");
            return;
        }

        int currentBatchSize = batchSize == null ? 10 : batchSize;
        if (currentBatchSize < 1) {
            throw new BusinessException(400, "batchSize 必须大于 0");
        }

        List<List<Camera>> batches = splitBatches(cameras, currentBatchSize);
        log.info("摄像头巡检批次划分完成: totalCameras={}, batchSize={}, batches={}", cameras.size(), currentBatchSize, batches.size());
        for (List<Camera> batch : batches) {
            processBatch(batch);
        }

        log.info("摄像头巡检完成: cameras={}, batchSize={}, batches={}", cameras.size(), currentBatchSize, batches.size());
    }

    private List<Camera> listNormalCameras() {
        LambdaQueryWrapper<Camera> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Camera::getStatus, CAMERA_STATUS_NORMAL)
                .orderByAsc(Camera::getId);
        return cameraMapper.selectList(queryWrapper);
    }

    private List<List<Camera>> splitBatches(List<Camera> cameras, int batchSize) {
        List<List<Camera>> batches = new ArrayList<>();
        for (int i = 0; i < cameras.size(); i += batchSize) {
            int end = Math.min(i + batchSize, cameras.size());
            batches.add(new ArrayList<>(cameras.subList(i, end)));
        }
        return batches;
    }

    /**
     * 从摄像头拍照获取一帧 base64 JPEG（使用 /frame 端点，不走 MJPEG 流）。
     */
    private String captureFrame(String cameraNo) {
        return cameraCaptureService.captureFrame(cameraNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyShelfUpdates(Map<Long, List<Long>> goodsIdsByShelf, List<Long> cameraIds) {
        int total = 0;
        for (var entry : goodsIdsByShelf.entrySet()) {
            List<Long> goodsIds = entry.getValue();
            if (!goodsIds.isEmpty()) {
                total += goodsMapper.batchUpdateShelfId(goodsIds, entry.getKey().toString());
            }
        }
        if (!cameraIds.isEmpty()) {
            cameraMapper.update(null, new LambdaUpdateWrapper<Camera>()
                    .in(Camera::getId, cameraIds)
                    .set(Camera::getLastScanTime, LocalDateTime.now()));
        }
        log.info("货架绑定批量更新完成: updatedShelves={}, updatedRows={}", goodsIdsByShelf.size(), total);
    }

    private void processBatch(List<Camera> batch) {
        if (CollectionUtils.isEmpty(batch)) {
            return;
        }

        Map<String, Camera> cameraByNo = new HashMap<>();
        List<PythonBatchRequestItem> payload = new ArrayList<>();

        for (Camera camera : batch) {
            if (camera == null || !StringUtils.hasText(camera.getCameraNo())) {
                continue;
            }
            cameraByNo.put(camera.getCameraNo(), camera);
            // 从摄像头实时流中截取一帧用于识别
            String frameBase64 = captureFrame(camera.getCameraNo());
            if (frameBase64 == null) {
                handleUnavailableCamera(camera);
                continue;
            }
            payload.add(new PythonBatchRequestItem(camera.getCameraNo(), frameBase64));
        }

        if (payload.isEmpty()) {
            log.info("当前批次无有效摄像头，跳过调用 Python 批量识别接口");
            return;
        }

        log.info("开始处理摄像头批次: payloadSize={}, cameraNos={}", payload.size(), cameraByNo.keySet());

        List<PythonBatchResponseItem> results = callPythonShelfBatch(payload);
        log.info("Python 批量识别返回结果: resultSize={}", results.size());

        // 收集每个货架对应的商品ID，通过事务方法批量更新
        Map<Long, List<Long>> goodsIdsByShelf = new java.util.LinkedHashMap<>();
        for (PythonBatchResponseItem result : results) {
            if (result == null || !StringUtils.hasText(result.cameraId())) continue;
            Camera matchedCamera = cameraByNo.get(result.cameraId());
            if (matchedCamera == null || !StringUtils.hasText(matchedCamera.getShelfId())) continue;
            List<Long> goodsIds = new ArrayList<>(normalizeGoodsIds(result.detectedGoodsIds()));
            if (!goodsIds.isEmpty()) {
                try {
                    Long shelfKey = Long.parseLong(matchedCamera.getShelfId().replaceAll("[^0-9]", ""));
                    goodsIdsByShelf.computeIfAbsent(shelfKey, k -> new ArrayList<>()).addAll(goodsIds);
                } catch (NumberFormatException e) {
                    goodsIdsByShelf.computeIfAbsent(0L, k -> new ArrayList<>()).addAll(goodsIds);
                }
            }
        }

        List<Long> cameraIds = batch.stream().map(Camera::getId).filter(id -> id != null).toList();
        if (!goodsIdsByShelf.isEmpty() || !cameraIds.isEmpty()) {
            applyShelfUpdates(goodsIdsByShelf, cameraIds);
        }

        log.info("摄像头批次处理完成: payloadSize={}, shelvesUpdated={}", payload.size(), goodsIdsByShelf.size());
    }

    private void handleUnavailableCamera(Camera camera) {
        if (camera == null || !StringUtils.hasText(camera.getCameraNo())) {
            return;
        }

        if (Boolean.TRUE.equals(autoDisableUnavailable) && camera.getId() != null) {
            int updated = cameraMapper.update(null, new LambdaUpdateWrapper<Camera>()
                    .eq(Camera::getId, camera.getId())
                    .set(Camera::getStatus, 0)
                    .set(Camera::getLastScanTime, LocalDateTime.now()));
            if (updated > 0) {
                log.warn("摄像头 {} 截取帧失败，已自动标记为停用，后续巡检将跳过", camera.getCameraNo());
                return;
            }
        }

        log.warn("摄像头 {} 截取帧失败，跳过", camera.getCameraNo());
    }

    private List<PythonBatchResponseItem> callPythonShelfBatch(List<PythonBatchRequestItem> payload) {
        String url = aiServiceUrl + "/api/ai/recognize/shelf/batch";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<List<PythonBatchRequestItem>> requestEntity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<List<PythonBatchResponseItem>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<PythonBatchResponseItem> body = response.getBody();
            return body == null ? List.of() : body;
        } catch (RestClientException ex) {
            log.error("调用 Python 批量识别服务失败: url={}, payloadSize={}, message={}", url, payload.size(), ex.getMessage(), ex);
            throw new BusinessException(502, "调用 Python 批量识别服务失败: " + ex.getMessage());
        }
    }

    private Set<Long> normalizeGoodsIds(List<Long> goodsIds) {
        if (CollectionUtils.isEmpty(goodsIds)) {
            return Set.of();
        }
        return goodsIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
    }

    private void restartTaskLocked() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }

        int interval = intervalMinutes == null ? 5 : intervalMinutes;
        int currentBatchSize = batchSize == null ? 10 : batchSize;
        validateConfig(interval, currentBatchSize);

        scheduledFuture = taskScheduler.scheduleAtFixedRate(this::runSafely, Duration.ofMinutes(interval));
        log.info("摄像头巡检调度已启动: intervalMinutes={}, batchSize={}", interval, currentBatchSize);
    }

    private void runSafely() {
        try {
            log.info("定时巡检触发执行");
            executeScanBatchTask();
        } catch (Exception ex) {
            log.error("摄像头巡检执行失败", ex);
        }
    }

    private void validateConfig(int intervalMinutes, int batchSize) {
        if (intervalMinutes < 1) {
            throw new BusinessException(400, "intervalMinutes 必须大于 0");
        }
        if (batchSize < 1) {
            throw new BusinessException(400, "batchSize 必须大于 0");
        }
    }

    public record SchedulerConfig(Integer intervalMinutes, Integer batchSize) {
    }

    private record PythonBatchRequestItem(
            @JsonProperty("camera_id") String cameraId,
            @JsonProperty("image_base64") String imageBase64
    ) {
    }

    private record PythonBatchResponseItem(
            @JsonProperty("camera_id") String cameraId,
            @JsonProperty("detected_goods_ids") List<Long> detectedGoodsIds
    ) {
    }
}
