"""语义检索路由 —— 向量索引构建与查询，供后端混合检索调用。"""

from typing import List

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.semantic.store import get_index

router = APIRouter(prefix="/api/ai/semantic", tags=["semantic"])


class IndexDoc(BaseModel):
    id: int
    text: str


class IndexRequest(BaseModel):
    docs: List[IndexDoc] = Field(default_factory=list)


class SearchRequest(BaseModel):
    query: str
    topk: int = 60


class ScoredGoods(BaseModel):
    id: int
    score: float


@router.post("/index")
def build_index(req: IndexRequest) -> dict:
    """用活跃商品重建语义索引（后端在启动/缓存过期时调用）。"""
    try:
        indexed = get_index().index([doc.model_dump() for doc in req.docs])
    except Exception as exc:  # 模型下载/加载失败等
        raise HTTPException(status_code=503, detail=f"语义模型不可用: {exc}") from exc
    return {"indexed": indexed}


@router.post("/search", response_model=List[ScoredGoods])
def search(req: SearchRequest) -> List[ScoredGoods]:
    """对查询做向量检索，返回 (goods_id, 余弦分数) Top-K。"""
    try:
        results = get_index().search(req.query, req.topk)
    except Exception as exc:
        raise HTTPException(status_code=503, detail=f"语义模型不可用: {exc}") from exc
    return [ScoredGoods(id=gid, score=score) for gid, score in results]


@router.get("/status")
def status() -> dict:
    """索引状态（是否就绪、条数）。"""
    return get_index().status()
