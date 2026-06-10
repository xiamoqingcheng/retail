package com.retail.server.service;

public interface CameraCaptureService {
    String captureFrame(String cameraNo);
    String captureFrameByIndex(Integer cameraIndex);
}