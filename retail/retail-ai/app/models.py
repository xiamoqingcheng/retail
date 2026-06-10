"""Pydantic 请求 / 响应模型。"""

from typing import List

from pydantic import BaseModel, Field


class CenterRecognizeRequest(BaseModel):
    image_base64: str
    k: int = Field(..., ge=1, le=100)


class CenterRecognizeResponse(BaseModel):
    goods_id: str
    box_center: List[float]
    box: List[float]  # [x1, y1, x2, y2] bounding box
    distance: float


class ShelfBatchItem(BaseModel):
    camera_id: str
    image_base64: str


class ShelfBatchResult(BaseModel):
    camera_id: str
    detected_goods_ids: List[int]


class TextSearchRequest(BaseModel):
    query: str
    k: int = Field(..., ge=1, le=100)


class TextSearchResponse(BaseModel):
    goods_ids: List[int]


class AnnotateRequest(BaseModel):
    image_base64: str


class AnnotateResponse(BaseModel):
    image_base64: str
    detected_count: int
    detected_items: List[dict]
