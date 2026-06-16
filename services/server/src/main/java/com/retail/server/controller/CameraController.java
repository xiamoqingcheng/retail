package com.retail.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.retail.server.camera.RemoteCameraAddress;
import com.retail.server.common.Result;
import com.retail.server.dto.CameraBindRequest;
import com.retail.server.dto.CameraSchedulerConfigRequest;
import com.retail.server.dto.CameraSchedulerConfigResponse;
import com.retail.server.dto.CameraUpdateRequest;
import com.retail.server.entity.Camera;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.CameraMapper;
import com.retail.server.scheduler.CameraScanScheduler;
import com.retail.server.service.CameraAgentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final CameraAgentService cameraAgentService;

    @Value("${ai.service-url:http://localhost:8000}")
    private String aiServiceUrl;

    public CameraController(
            CameraScanScheduler cameraScanScheduler,
            CameraService cameraService,
            RestTemplate restTemplate,
            CameraCaptureService cameraCaptureService,
            CameraAgentService cameraAgentService) {
        this.cameraScanScheduler = cameraScanScheduler;
        this.cameraService = cameraService;
        this.restTemplate = restTemplate;
        this.cameraCaptureService = cameraCaptureService;
        this.cameraAgentService = cameraAgentService;
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
     * Discover camera agents in the same LAN.
     */
    @GetMapping("/agents/discover")
    public Result<List<Map<String, Object>>> discoverCameraAgents(
            @RequestParam(required = false) Integer timeoutMillis) {
        return Result.success(cameraAgentService.discoverAgents(timeoutMillis));
    }

    @GetMapping("/agent/available")
    public Result<Map<String, Object>> listAgentCameras(
            @RequestParam String host,
            @RequestParam(required = false) Integer port) {
        try {
            return Result.success(cameraAgentService.listAvailable(host, port));
        } catch (RestClientException ex) {
            throw new BusinessException(502, "Remote camera agent unavailable: " + ex.getMessage());
        }
    }

    /**
     * Bind and create a camera.
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

        // 切换为「运行」状态前，先检测摄像头是否有画面响应；无响应则拒绝切换，保持离线。
        if (status == 1) {
            String frame = cameraCaptureService.captureFrame(camera.getCameraNo());
            if (!StringUtils.hasText(frame)) {
                throw new BusinessException(409, "摄像头无画面响应，无法切换为运行状态，请检查摄像头连接或 AI 识别服务是否已启动");
            }
        }

        camera.setStatus(status);
        int affected = cameraService.update(camera);
        if (affected != 1) {
            throw new BusinessException(500, "更新摄像头状态失败");
        }
        if (status == 0) {
            releaseCameraStream(camera.getCameraNo());
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
        releaseCameraStream(camera.getCameraNo());
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
        assertBoundCameraEnabled(cameraNo);

        Optional<RemoteCameraAddress> remoteAddress = cameraAgentService.parse(cameraNo);
        if (remoteAddress.isPresent()) {
            String imageBase64 = cameraAgentService.captureFrame(remoteAddress.get());
            if (!StringUtils.hasText(imageBase64)) {
                throw new BusinessException(502, "Remote camera preview failed");
            }
            return Result.success(imageBase64);
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
        if (isDisabledBoundCamera(cameraNo)) {
            response.setStatus(409);
            return;
        }

        Optional<RemoteCameraAddress> remoteAddress = cameraAgentService.parse(cameraNo);
        if (remoteAddress.isPresent()) {
            proxyMjpegStream(remoteAddress.get().streamUrl(), response);
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

    @GetMapping("/stream")
    public void streamCameraByQuery(@RequestParam String cameraNo, HttpServletResponse response) {
        streamCamera(cameraNo, response);
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
    public Result<CameraScanScheduler.ScanResult> triggerScheduler() {
        log.info("收到手动触发全量巡检接口请求");
        CameraScanScheduler.ScanResult result = cameraScanScheduler.triggerNow();
        return Result.success("触发成功", result);
    }

    /**
     * 主动释放指定摄像头视频流资源（透传至 Python）。
     */
    @PostMapping("/stream/{cameraNo}/stop")
    public Result<Void> stopStream(@PathVariable String cameraNo) {
        if (!StringUtils.hasText(cameraNo)) {
            throw new BusinessException(400, "cameraNo invalid");
        }
        if (!releaseCameraStream(cameraNo)) {
            throw new BusinessException(502, "release camera stream failed");
        }
        return Result.success("Released", null);
    }

    @PostMapping("/stream/stop")
    public Result<Void> stopStreamByQuery(@RequestParam String cameraNo) {
        return stopStream(cameraNo);
    }

    private void proxyMjpegStream(String url, HttpServletResponse response) {
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
            log.warn("Camera stream proxy failed: {}", ex.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(502);
            }
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
        if (!StringUtils.hasText(cameraNo)) {
            throw new BusinessException(400, "cameraNo invalid");
        }
        assertBoundCameraEnabled(cameraNo);
        if (cameraAgentService.parse(cameraNo).isEmpty()) {
            Integer cameraIndex = parseCameraIndex(cameraNo);
            if (cameraIndex == null || cameraIndex < 0) {
                throw new BusinessException(400, "cameraNo 非法");
            }
        }

        // 1. 从流中截取一帧
        String frameBase64 = captureCameraFrame(cameraNo);
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

    private String captureCameraFrame(String cameraNo) {
        return cameraCaptureService.captureFrame(cameraNo);
    }

    private void assertBoundCameraEnabled(String cameraNo) {
        if (isDisabledBoundCamera(cameraNo)) {
            throw new BusinessException(409, "camera is offline");
        }
    }

    private boolean isDisabledBoundCamera(String cameraNo) {
        if (!StringUtils.hasText(cameraNo)) {
            return false;
        }
        Camera camera = cameraService.getByCameraNo(cameraNo.trim());
        return camera != null && !Integer.valueOf(1).equals(camera.getStatus());
    }

    private boolean releaseCameraStream(String cameraNo) {
        if (!StringUtils.hasText(cameraNo)) {
            return false;
        }
        boolean released = cameraCaptureService.releaseStream(cameraNo);
        if (!released) {
            log.warn("Release camera stream failed or skipped: cameraNo={}", cameraNo);
        }
        return released;
    }

    private Integer parseCameraIndex(String cameraNo) {
        return com.retail.server.service.impl.CameraCaptureServiceImpl.parseCameraIndex(cameraNo);
    }
}
