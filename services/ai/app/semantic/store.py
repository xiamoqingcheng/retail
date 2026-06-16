"""商品语义索引 —— 内存向量库，余弦 Top-K 检索，单例线程安全。

后端把活跃商品（id + 文本）推到 /index 建库；查询时 /search 返回 (goods_id, score) 列表。
200 件规模内存矩阵点积足够快，无需外部向量数据库。
"""

import threading
from typing import List, Optional, Tuple

import numpy as np

from app.semantic.embedder import get_embedder

_index: Optional["SemanticIndex"] = None
_index_lock = threading.Lock()


def get_index() -> "SemanticIndex":
    """获取单例语义索引。"""
    global _index
    if _index is None:
        with _index_lock:
            if _index is None:
                _index = SemanticIndex()
    return _index


class SemanticIndex:
    """商品向量内存索引：ids 与归一化向量矩阵一一对应。"""

    def __init__(self) -> None:
        self._ids: List[int] = []
        self._matrix: Optional[np.ndarray] = None  # (N, dim) 已归一化
        self._lock = threading.Lock()

    def index(self, docs: List[dict]) -> int:
        """用 [{"id": int, "text": str}] 重建索引，返回入库条数。"""
        ids: List[int] = []
        texts: List[str] = []
        for doc in docs:
            gid = doc.get("id")
            text = (doc.get("text") or "").strip()
            if gid is None or not text:
                continue
            ids.append(int(gid))
            texts.append(text)

        if not ids:
            with self._lock:
                self._ids = []
                self._matrix = None
            return 0

        matrix = get_embedder().encode(texts, is_query=False)
        with self._lock:
            self._ids = ids
            self._matrix = matrix
        return len(ids)

    def search(self, query: str, topk: int = 60) -> List[Tuple[int, float]]:
        """返回与 query 最相近的 (goods_id, cosine_score) 列表，按分数降序。"""
        with self._lock:
            ids = self._ids
            matrix = self._matrix
        if matrix is None or not ids or not query or not query.strip():
            return []

        query_vec = get_embedder().encode([query], is_query=True)[0]  # (dim,)
        sims = matrix @ query_vec  # 均已归一化 → 点积即余弦
        k = max(1, min(int(topk), len(ids)))
        # 取 top-k（先 partition 再排序，避免全排序）
        top_idx = np.argpartition(-sims, k - 1)[:k]
        top_idx = top_idx[np.argsort(-sims[top_idx])]
        return [(int(ids[i]), float(sims[i])) for i in top_idx]

    def status(self) -> dict:
        with self._lock:
            return {"ready": self._matrix is not None, "size": len(self._ids)}
