package com.retail.server.controller;

import com.retail.server.common.Result;
import com.retail.server.dto.AppletScanGoodsDTO;
import com.retail.server.dto.AppletScanRequest;
import com.retail.server.exception.BusinessException;
import com.retail.server.service.AiIntegrationService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序摄像头识别控制器。
 */
@RestController
@RequestMapping("/api/applet")
public class AppletCameraController {

    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 10;
    private static final int MAX_IMAGE_BASE64_LENGTH = 8_000_000;

    private final AiIntegrationService aiIntegrationService;

    public AppletCameraController(AiIntegrationService aiIntegrationService) {
        this.aiIntegrationService = aiIntegrationService;
    }

    /**
     * 小程序扫码识别接口。
     */
    @PostMapping("/scan")
    public Result<List<AppletScanGoodsDTO>> scan(@RequestBody AppletScanRequest request) {
        if (request == null || !StringUtils.hasText(request.imageBase64())) {
            throw new BusinessException(400, "image_base64 不能为空");
        }

        String imageBase64 = request.imageBase64().trim();
        if (imageBase64.length() > MAX_IMAGE_BASE64_LENGTH) {
            throw new BusinessException(413, "image_base64 is too large");
        }

        int requestedK = request.k() == null ? DEFAULT_TOP_K : request.k();
        int k = Math.min(Math.max(requestedK, 1), MAX_TOP_K);
        List<AppletScanGoodsDTO> data = aiIntegrationService.getTopKGoodsFromImage(imageBase64, k);
        return Result.success("识别成功", data);
    }
}
