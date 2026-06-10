"""文本搜索相关路由。"""

import hashlib

from fastapi import APIRouter, HTTPException

from app.config import GOODS_CATALOG
from app.models import TextSearchRequest, TextSearchResponse

router = APIRouter(prefix="/api/ai/search", tags=["search"])


def _jaccard_score(a: str, b: str) -> float:
    set_a, set_b = set(a.lower()), set(b.lower())
    if not set_a or not set_b:
        return 0.0
    return len(set_a & set_b) / len(set_a | set_b)


def _deterministic_tiebreak(query: str, goods_id: int) -> float:
    digest = hashlib.md5(f"{query}:{goods_id}".encode("utf-8")).hexdigest()[:8]
    return int(digest, 16) / 0xFFFFFFFF


@router.post("/text", response_model=TextSearchResponse)
def search_text(req: TextSearchRequest) -> TextSearchResponse:
    """文本关键词检索商品。"""
    query = req.query.strip()
    if not query:
        raise HTTPException(status_code=400, detail="query must not be empty")

    scored = []
    for goods_id, _name in GOODS_CATALOG:
        name = _name
        score = 0.0
        if query.lower() in name.lower():
            score += 0.3
        score += _jaccard_score(query, name)
        score += _deterministic_tiebreak(query, goods_id) * 1e-4
        scored.append((goods_id, score))

    scored.sort(key=lambda x: (-x[1], x[0]))
    top_k = min(req.k, len(scored))
    seen = set()
    unique_ids = []
    for gid, _ in scored:
        if gid not in seen:
            seen.add(gid)
            unique_ids.append(gid)
            if len(unique_ids) >= top_k:
                break
    return TextSearchResponse(goods_ids=unique_ids)
