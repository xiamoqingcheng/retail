package com.retail.server.service.impl;

import com.retail.server.service.CameraCaptureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class CameraCaptureServiceImpl implements CameraCaptureService {

    private final RestTemplate restTemplate;

    @Value("${ai.service-url:http://localhost:8000}")
    private String aiServiceUrl;

    public CameraCaptureServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String captureFrame(String cameraNo) {
        Integer index = parseCameraIndex(cameraNo);
        if (index == null) {
            return null;
        }
        return captureFrameByIndex(index);
    }

    @Override
    public String captureFrameByIndex(Integer cameraIndex) {
        if (cameraIndex == null || cameraIndex < 0) {
            return null;
        }
        try {
            String url = aiServiceUrl + "/api/ai/cameras/frame/" + cameraIndex;
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && resp.get("image_base64") != null) {
                return resp.get("image_base64").toString();
            }
        } catch (Exception e) {
            log.warn("截取摄像头 {} 帧失败: {}", cameraIndex, e.getMessage());
        }
        return null;
    }

    public static Integer parseCameraIndex(String cameraNo) {
        if (cameraNo == null) {
            return null;
        }
        String trimmed = cameraNo.trim();
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            String digits = trimmed.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }
}