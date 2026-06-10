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

        int k = request.k() == null ? 3 : request.k();
        List<AppletScanGoodsDTO> data = aiIntegrationService.getTopKGoodsFromImage(request.imageBase64(), k);
        return Result.success("识别成功", data);
    }
}