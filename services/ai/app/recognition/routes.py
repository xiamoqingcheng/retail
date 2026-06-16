"""识别相关路由 —— 使用 best.pt YOLO 模型进行真实商品检测。"""

import base64
import os
from io import BytesIO
from typing import List

import cv2
import numpy as np
from fastapi import APIRouter, Body, HTTPException
from PIL import Image, ImageDraw, ImageFont

from app.models import (
    AnnotateRequest,
    AnnotateResponse,
    CenterRecognizeRequest,
    CenterRecognizeResponse,
    ShelfBatchItem,
    ShelfBatchResult,
)
from app.recognition.detector import get_detector
from app.config import GOODS_NAME_MAP
from app.utils import decode_base64_image

router = APIRouter(prefix="/api/ai/recognize", tags=["recognition"])


def _shelf_conf_threshold() -> float:
    try:
        return max(0.05, min(float(os.getenv("SHELF_CONF_THRESHOLD", "0.35")), 0.95))
    except ValueError:
        return 0.35


def _goods_id_from_class(class_id: int) -> int:
    """YOLO class_id 映射到数据库 goods_id。
    best.pt 模型的类名格式为 '{goods_id}_{category}'，goods_id 从 1 开始。
    所以 class_id + 1 = goods_id。
    """
    return class_id + 1


def _goods_id_str(class_id: int) -> str:
    """返回数据库 goods_id 的字符串形式。"""
    return str(_goods_id_from_class(class_id))


@router.post("/center", response_model=List[CenterRecognizeResponse])
def recognize_center(req: CenterRecognizeRequest) -> List[CenterRecognizeResponse]:
    """以画面中心为锚点，使用 YOLO 检测最近 k 个商品。"""
    image = decode_base64_image(req.image_base64)
    detector = get_detector()
    detections = detector.detect_top_k(image, k=req.k)

    results = []
    for cls_id, conf, box, center, dist in detections:
        results.append(CenterRecognizeResponse(
            goods_id=_goods_id_str(cls_id),
            box_center=[float(center[0]), float(center[1])],
            box=[float(box[0]), float(box[1]), float(box[2]), float(box[3])],
            distance=float(dist),
        ))
    return results


@router.post("/shelf/batch", response_model=List[ShelfBatchResult])
def recognize_shelf_batch(items: List[ShelfBatchItem]) -> List[ShelfBatchResult]:
    """对多台摄像头图像批量检测货架商品（默认与标注预览使用相同阈值）。"""
    detector = get_detector()
    results: List[ShelfBatchResult] = []
    conf_threshold = _shelf_conf_threshold()

    for item in items:
        image = decode_base64_image(item.image_base64)
        detections = detector.detect(image, conf_threshold=conf_threshold)

        # 每个 class_id 只保留最高置信度
        best = {}
        for cls_id, conf, box in detections:
            if conf >= conf_threshold:
                gid = _goods_id_from_class(cls_id)
                if gid not in best or conf > best[gid][0]:
                    best[gid] = (conf, gid)

        results.append(ShelfBatchResult(
            camera_id=item.camera_id,
            detected_goods_ids=list(best.keys()),
        ))

    return results


@router.post("/annotate")
def annotate_image(req: AnnotateRequest = Body(...)):
    """对上传的图片进行 YOLO 检测，返回带标注框+商品名的 base64 图片。"""
    image = decode_base64_image(req.image_base64)
    if image is None or image.size == 0:
        raise HTTPException(status_code=400, detail="invalid image data")

    h, w = image.shape[:2]
    detector = get_detector()
    detections = detector.detect(image, conf_threshold=0.35)

    # OpenCV BGR → PIL RGB
    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    pil_img = Image.fromarray(image_rgb)
    draw = ImageDraw.Draw(pil_img)

    # 尝试加载中文字体
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", max(14, min(24, w // 40)))
    except Exception:
        try:
            font = ImageFont.load_default()
        except Exception:
            font = None

    detected = []
    for cls_id, conf, (x1, y1, x2, y2) in detections:
        goods_id = _goods_id_from_class(cls_id)
        name = GOODS_NAME_MAP.get(goods_id, f"ID:{goods_id}")
        label = f"{name} {conf:.0%}"

        # 画框
        color = (228, 57, 60)  # 京东红
        line_w = max(2, w // 300)
        draw.rectangle([x1, y1, x2, y2], outline=color, width=line_w)
        # 画标签
        if font:
            try:
                tb = draw.textbbox((x1, y1 - 2), label, font=font)
                draw.rectangle([tb[0] - 2, tb[1] - 28, tb[2] + 2, tb[1]], fill=color)
                draw.text((x1 + 2, y1 - 26), label, fill=(255, 255, 255), font=font)
            except Exception:
                draw.text((x1 + 2, y1 + 2), label, fill=color, font=font)

        detected.append({"goods_id": goods_id, "name": name, "confidence": round(conf, 3),
                         "box": [round(x1), round(y1), round(x2), round(y2)]})

    # 转 base64
    try:
        buf = BytesIO()
        pil_img.save(buf, format="JPEG", quality=85)
        annotated_b64 = base64.b64encode(buf.getvalue()).decode()
    except Exception:
        raise HTTPException(status_code=500, detail="failed to encode annotated image")

    return AnnotateResponse(
        image_base64=annotated_b64,
        detected_count=len(detected),
        detected_items=detected,
    )
