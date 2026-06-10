package com.retail.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.retail.server.common.Result;
import com.retail.server.dto.CameraBindRequest;
import com.retail.server.dto.CameraSchedulerConfigRequest;
import com.retail.server.dto.CameraSchedulerConfigResponse;
import com.retail.server.dto.CameraUpdateRequest;
import com.retail.server.entity.Camera;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.CameraMapper;
import com.retail.server.scheduler.CameraScanScheduler;
import com.retail.server.service.CameraCaptureService;
import com.retail.server.service.CameraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 摄像头管理控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/camera")
public class CameraController {

    private final CameraScanScheduler cameraScanScheduler;
    private final CameraService cameraService;
    private final RestTemplate restTemplate;
    private final CameraCaptureService cameraCaptureService;

    @Value("${ai.service-url:http://localhost:8000}")
    private String aiServiceUrl;

    public CameraController(
            CameraScanScheduler cameraScanScheduler,
            CameraService cameraService,
            RestTemplate restTemplate,
            CameraCaptureService cameraCaptureService) {
        this.cameraScanScheduler = cameraScanScheduler;
        this.cameraService = cameraService;
        this.restTemplate = restTemplate;
        this.cameraCaptureService = cameraCaptureService;
    }

    /**
     * 查询摄像头列表。
     */
    @GetMapping("/list")
    public Result<List<Camera>> listCameras() {
        return Result.success(cameraService.listAll());
    }

    /**
     * 透传获取本机可用物理摄像头索引。
     */
    @GetMapping("/available_hardware")
    public Result<List<Integer>> listAvailableHardwareCameras() {
        String url = aiServiceUrl + "/api/ai/cameras/available";
        try {
            ResponseEntity<PythonAvailableCameraResponse> response = restTemplate.getForEntity(url,
                    PythonAvailableCameraResponse.class);

            PythonAvailableCameraResponse body = response.getBody();
            List<Integer> indexes = (body == null || body.availableIndexes() == null)
                    ? List.of()
                    : body.availableIndexes();
            return Result.success(indexes);
        } catch (RestClientException ex) {
            throw new BusinessException(502, "调用 Python 摄像头探测服务失败");
        }
    }

    /**
     * 绑定并新增摄像头。
     */
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> bindCamera(@RequestBody CameraBindRequest request) {
        if (request == null
                || !StringUtils.hasText(request.cameraNo())
                || !StringUtils.hasText(request.shelfId())) {
            throw new BusinessException(400, "camera_no 和 shelf_id 不能为空");
        }

        String cameraNo = request.cameraNo().trim();
        String shelfId = request.shelfId().trim();

        if (cameraService.getByCameraNo(cameraNo) != null) {
            throw new BusinessException(400, "该 camera_no 已被绑定");
        }

        Camera camera = Camera.builder()
                .cameraNo(cameraNo)
                .shelfId(shelfId)
                .status(1)
                .build();

        cameraService.create(camera);
        if (camera.getId() == null) {
            throw new BusinessException(500, "新增摄像头失败");
        }

        return Result.success("绑定成功", camera.getId());
    }

    /**
     * 编辑摄像头绑定货架号。
     */
    @PutMapping
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateCameraShelf(@RequestBody CameraUpdateRequest request) {
        if (request == null || request.id() == null || !StringUtils.hasText(request.shelfId())) {
            throw new BusinessException(400, "id 和 shelf_id 不能为空");
        }

        Camera camera = cameraService.getById(request.id());
        if (camera == null) {
            throw new BusinessException(404, "摄像头不存在");
        }

        camera.setShelfId(request.shelfId().trim());
        int affected = cameraService.update(camera);
        if (affected != 1) {
            throw new BusinessException(500, "更新摄像头绑定失败");
        }
        return Result.success("更新成功", null);
    }

    /**
     * 更新摄像头启停状态。
     */
    @PutMapping("/{id}/status")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateCameraStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        if (id == null || id < 1) {
            throw new BusinessException(400, "摄像头 ID 非法");
        }
        Integer status = body == null ? null : body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(400, "状态值无效");
        }

        Camera camera = cameraService.getById(id);
        if (camera == null) {
            throw new BusinessException(404, "摄像头不存在");
        }

        camera.setStatus(status);
        int affected = cameraService.update(camera);
        if (affected != 1) {
            throw new BusinessException(500, "更新摄像头状态失败");
        }
        return Result.success("状态更新成功", null);
    }

    /**
     * 删除摄像头。
     */
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteCamera(@PathVariable Long id) {
        if (id == null || id < 1) {
            throw new BusinessException(400, "摄像头 ID 非法");
        }

        Camera camera = cameraService.getById(id);
        if (camera == null) {
            throw new BusinessException(404, "摄像头不存在");
        }

        int affected = cameraService.delete(id);
        if (affected != 1) {
            throw new BusinessException(500, "删除摄像头失败");
        }
        return Result.success("删除成功", null);
    }

    /**
     * 预览摄像头实时画面（单帧透传）。
     */
    @GetMapping("/preview/{cameraNo}")
    public Result<String> previewCamera(@PathVariable String cameraNo) {
        if (!StringUtils.hasText(cameraNo)) {
            throw new BusinessException(400, "cameraNo 不能为空");
        }

        Integer cameraIndex = parseCameraIndex(cameraNo);
        if (cameraIndex == null || cameraIndex < 0) {
            throw new BusinessException(400, "cameraNo 非法");
        }

        String url = aiServiceUrl + "/api/ai/cameras/frame/" + cameraIndex;
        try {
            ResponseEntity<PythonCameraFrameResponse> response = restTemplate.getForEntity(url,
                    PythonCameraFrameResponse.class);
            PythonCameraFrameResponse body = response.getBody();
            if (body == null || !StringUtils.hasText(body.imageBase64())) {
                throw new BusinessException(500, "摄像头预览数据为空");
            }
            return Result.success(body.imageBase64());
        } catch (RestClientException ex) {
            throw new BusinessException(502, "调用 Python 摄像头预览服务失败");
        }
    }

    /**
     * MJPEG 视频流代理 —— 将 Python AI 服务的 MJPEG 流透传给前端（使用 RestTemplate.Streaming）。
     */
    @GetMapping("/stream/{cameraNo}")
    public void streamCamera(@PathVariable String cameraNo, HttpServletResponse response) {
        if (!StringUtils.hasText(cameraNo)) {
            response.setStatus(400);
            return;
        }
        Integer cameraIndex = parseCameraIndex(cameraNo);
        if (cameraIndex == null || cameraIndex < 0) {
            response.setStatus(400);
            return;
        }

        String url = aiServiceUrl + "/api/ai/cameras/stream/" + cameraIndex;
        try {
            restTemplate.execute(url, HttpMethod.GET, null, streamResponse -> {
                if (streamResponse.getStatusCode().value() != 200) {
                    response.setStatus(streamResponse.getStatusCode().value());
                    return null;
                }

                response.setContentType("multipart/x-mixed-replace; boundary=frame");
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Connection", "keep-alive");

                try (InputStream in = streamResponse.getBody();
                     OutputStream out = response.getOutputStream()) {
                    byte[] buf = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buf)) != -1) {
                        out.write(buf, 0, bytesRead);
                        out.flush();
                    }
                }
                return null;
            });
        } catch (Exception ex) {
            log.warn("摄像头视频流代理异常: {}", ex.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(502);
            }
        }
    }

    /**
     * 动态修改定时任务配置并重启任务。
     */
    @PutMapping("/scheduler/config")
    public Result<CameraSchedulerConfigResponse> updateSchedulerConfig(
            @RequestBody CameraSchedulerConfigRequest request) {
        if (request == null || request.intervalMinutes() == null || request.batchSize() == null) {
            throw new BusinessException(400, "intervalMinutes 和 batchSize 不能为空");
        }

        log.info("收到更新调度配置请求: intervalMinutes={}, batchSize={}", request.intervalMinutes(), request.batchSize());

        CameraScanScheduler.SchedulerConfig config = cameraScanScheduler.updateSchedulerConfig(
                request.intervalMinutes(),
                request.batchSize());

        CameraSchedulerConfigResponse data = new CameraSchedulerConfigResponse(
                config.intervalMinutes(),
                config.batchSize());
        return Result.success("调度配置更新成功", data);
    }

    /**
     * 查询当前定时任务配置。
     */
    @GetMapping("/scheduler/config")
    public Result<CameraSchedulerConfigResponse> getSchedulerConfig() {
        CameraScanScheduler.SchedulerConfig config = cameraScanScheduler.currentConfig();
        CameraSchedulerConfigResponse data = new CameraSchedulerConfigResponse(
                config.intervalMinutes(),
                config.batchSize());
        return Result.success(data);
    }

    /**
     * 手动触发一次全量扫描。
     */
    @PostMapping("/scheduler/trigger")
    public Result<Void> triggerScheduler() {
        log.info("收到手动触发全量巡检接口请求");
        cameraScanScheduler.triggerNow();
        return Result.success("触发成功", null);
    }

    /**
     * 主动释放指定摄像头视频流资源（透传至 Python）。
     */
    @PostMapping("/stream/{cameraNo}/stop")
    public Result<Void> stopStream(@PathVariable String cameraNo) {
        Integer cameraIndex = parseCameraIndex(cameraNo);
        if (cameraIndex == null || cameraIndex < 0) {
            throw new BusinessException(400, "cameraNo 非法");
        }
        String url = aiServiceUrl + "/api/ai/cameras/stream/" + cameraIndex + "/stop";
        try {
            restTemplate.postForEntity(url, null, String.class);
            return Result.success("已释放", null);
        } catch (RestClientException ex) {
            log.warn("释放摄像头资源异常: {}", ex.getMessage());
            throw new BusinessException(502, "释放摄像头资源失败: " + ex.getMessage());
        }
    }

    private record PythonAvailableCameraResponse(
            @JsonProperty("available_indexes") List<Integer> availableIndexes) {
    }

    private record PythonCameraFrameResponse(
            @JsonProperty("camera_index") Integer cameraIndex,
            @JsonProperty("image_base64") String imageBase64) {
    }

    /**
     * 获取摄像头当前帧的标注结果（YOLO 检测 + 边界框）。
     */
    @GetMapping("/snapshot/{cameraNo}")
    public Result<Map<String, Object>> annotatedSnapshot(@PathVariable String cameraNo) {
        Integer cameraIndex = parseCameraIndex(cameraNo);
        if (cameraIndex == null || cameraIndex < 0) {
            throw new BusinessException(400, "cameraNo 非法");
        }

        // 1. 从流中截取一帧
        String frameBase64 = captureCameraFrame(cameraIndex);
        if (frameBase64 == null) {
            throw new BusinessException(502, "摄像头截帧失败");
        }

        // 2. 调用 Python 标注接口
        String url = aiServiceUrl + "/api/ai/recognize/annotate";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of("image_base64", frameBase64);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(url, entity, Map.class);
            return Result.success(result);
        } catch (RestClientException ex) {
            throw new BusinessException(502, "AI 标注服务异常: " + ex.getMessage());
        }
    }

    private String captureCameraFrame(Integer cameraIndex) {
        return cameraCaptureService.captureFrameByIndex(cameraIndex);
    }

    private Integer parseCameraIndex(String cameraNo) {
        return com.retail.server.service.impl.CameraCaptureServiceImpl.parseCameraIndex(cameraNo);
    }
}
