"""摄像头相关路由。"""

import base64
import time

import cv2
from fastapi import APIRouter, HTTPException
from starlette.requests import Request
from starlette.responses import StreamingResponse

from app.camera.manager import camera_manager
from app.config import STREAM_JPEG_QUALITY, STREAM_FPS_LIMIT

router = APIRouter(prefix="/api/ai/cameras", tags=["camera"])


@router.get("/available")
def get_available_cameras() -> dict:
    """探测本机可用物理摄像头索引。"""
    return {"available_indexes": camera_manager.available_indexes()}


@router.get("/frame/{camera_index}")
def get_camera_frame(camera_index: int) -> dict:
    """拍摄单帧并返回 base64 编码的 JPEG。"""
    if camera_index < 0:
        raise HTTPException(status_code=400, detail="camera_index must be >= 0")

    cap = camera_manager.acquire(camera_index)
    if cap is None:
        raise HTTPException(status_code=404, detail="camera is not available")
    try:
        # 使用 read_frame（带 slot.lock），避免与 MJPEG 流并发冲突
        ok, frame = camera_manager.read_frame(camera_index)
        if not ok or frame is None:
            raise HTTPException(status_code=500, detail="failed to read camera frame")

        encoded_ok, buffer = cv2.imencode(".jpg", frame)
        if not encoded_ok:
            raise HTTPException(status_code=500, detail="failed to encode camera frame")

        return {
            "camera_index": camera_index,
            "image_base64": base64.b64encode(buffer.tobytes()).decode("utf-8"),
        }
    finally:
        camera_manager.release(camera_index)


def _mjpeg_generator(camera_index: int):
    """生成 MJPEG 帧的同步生成器，退出时自动释放引用。"""
    interval = 1.0 / STREAM_FPS_LIMIT
    # 丢弃可能残留的旧缓冲帧：重开预览时若复用了上一次的摄像头句柄，
    # 第一帧可能是关闭前的旧帧，先抓取并丢弃数帧，保证首帧为实时画面。
    for _ in range(3):
        camera_manager.read_frame(camera_index)
    try:
        while True:
            ok, frame = camera_manager.read_frame(camera_index)
            if not ok or frame is None:
                break
            encoded_ok, buffer = cv2.imencode(
                ".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, STREAM_JPEG_QUALITY]
            )
            if not encoded_ok:
                break
            frame_bytes = buffer.tobytes()
            yield (
                b"--frame\r\n"
                b"Content-Type: image/jpeg\r\n"
                b"Content-Length: " + str(len(frame_bytes)).encode() + b"\r\n\r\n"
                + frame_bytes + b"\r\n"
            )
            time.sleep(interval)
    finally:
        camera_manager.release(camera_index)


@router.get("/stream/{camera_index}")
def stream_camera(camera_index: int, request: Request):
    """MJPEG 视频流端点。"""
    if camera_index < 0 or camera_index > 10:
        raise HTTPException(status_code=400, detail="invalid camera index")

    cap = camera_manager.acquire(camera_index)
    if cap is None:
        raise HTTPException(status_code=404, detail="camera is not available")

    # acquire 已 +1 引用；_mjpeg_generator 内部 finally 会 -1。
    return StreamingResponse(
        _mjpeg_generator(camera_index),
        media_type="multipart/x-mixed-replace; boundary=frame",
    )


@router.post("/stream/{camera_index}/stop")
def stop_camera_stream(camera_index: int):
    """主动释放指定摄像头资源。"""
    camera_manager.force_release(camera_index)
    return {"detail": "released"}
