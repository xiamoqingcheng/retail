"""YOLO 检测器 —— 使用 best.pt 模型进行商品识别。"""

import os
import hashlib
import threading
from typing import List, Tuple, Optional

import numpy as np

MODEL_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "best.pt")

_detector: Optional["YOLODetector"] = None
_detector_lock = threading.Lock()


def get_detector() -> "YOLODetector":
    """获取单例检测器实例（延迟加载模型，线程安全）。"""
    global _detector
    if _detector is None:
        with _detector_lock:
            if _detector is None:
                _detector = YOLODetector(MODEL_PATH)
    return _detector


class YOLODetector:
    """封装 YOLO 模型推理，提供商品检测接口。"""

    def __init__(self, model_path: str):
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"YOLO 模型文件不存在: {model_path}")
        try:
            from ultralytics import YOLO
            self.model = YOLO(model_path)
        except ImportError:
            raise ImportError("请安装 ultralytics: pip install ultralytics")
        self.input_size = (640, 640)

    def detect(self, image: np.ndarray, conf_threshold: float = 0.25) -> List[Tuple[int, float, List[float]]]:
        """
        对图像执行目标检测。

        Args:
            image: BGR 格式的 numpy 图像数组
            conf_threshold: 置信度阈值

        Returns:
            List of (class_id, confidence, [x1, y1, x2, y2])
        """
        results = self.model(image, conf=conf_threshold, verbose=False)

        detections = []
        if results and len(results) > 0:
            boxes = results[0].boxes
            if boxes is not None:
                for box in boxes:
                    x1, y1, x2, y2 = box.xyxy[0].tolist()
                    conf = float(box.conf[0])
                    cls = int(box.cls[0])
                    detections.append((cls, conf, [x1, y1, x2, y2]))

        return detections

    def detect_top_k(self, image: np.ndarray, k: int = 5, conf_threshold: float = 0.25
                     ) -> List[Tuple[int, float, List[float], List[float]]]:
        """
        返回距离图像中心最近的 k 个**不同类别**商品。

        Returns:
            List of (class_id, confidence, [x1,y1,x2,y2], [cx,cy])
        """
        detections = self.detect(image, conf_threshold)

        if not detections:
            return []

        h, w = image.shape[:2]
        center_x, center_y = w / 2.0, h / 2.0

        scored = []
        for cls_id, conf, box in detections:
            x1, y1, x2, y2 = box
            cx = (x1 + x2) / 2.0
            cy = (y1 + y2) / 2.0
            dist = ((cx - center_x) ** 2 + (cy - center_y) ** 2) ** 0.5
            scored.append((cls_id, conf, box, [cx, cy], dist))

        scored.sort(key=lambda x: x[4])

        seen = set()
        result = []
        for cls_id, conf, box, center, dist in scored:
            if cls_id not in seen:
                seen.add(cls_id)
                result.append((cls_id, conf, box, center, dist))
                if len(result) >= k:
                    break

        return result


def image_seed(image_base64: str) -> int:
    """从 base64 字符串生成确定性种子（保留用于兼容）。"""
    return int(hashlib.md5(image_base64.encode("utf-8")).hexdigest()[:8], 16)
