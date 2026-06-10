"""通用工具函数。"""

import base64

import cv2
import numpy as np
from fastapi import HTTPException


def decode_base64_image(image_base64: str) -> np.ndarray:
    """解码 base64 字符串为 OpenCV 图像矩阵。"""
    try:
        raw = base64.b64decode(image_base64, validate=True)
    except Exception as exc:
        raise HTTPException(status_code=400, detail="image_base64 is not valid base64") from exc

    arr = np.frombuffer(raw, dtype=np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        raise HTTPException(status_code=400, detail="image_base64 is not a valid image")
    return img
