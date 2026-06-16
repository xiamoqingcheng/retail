"""文本向量化器 —— 懒加载 sentence-transformers 多语模型，单例线程安全（对齐 detector 的加载风格）。

模型来源默认走 ModelScope（魔搭，国内可达；HuggingFace 在本机被墙）：
  EMBED_SOURCE = modelscope | hf | local   (默认 modelscope)
  EMBED_MODEL  = 模型仓库名或本地目录        (默认 AI-ModelScope/bge-small-zh-v1.5，约 95MB)
首次会下载并缓存到 ~/.cache/modelscope，之后离线可用。
"""

import os
import threading
from typing import List, Optional

import numpy as np

EMBED_MODEL = os.getenv("EMBED_MODEL", "AI-ModelScope/bge-small-zh-v1.5")
EMBED_SOURCE = os.getenv("EMBED_SOURCE", "modelscope")  # modelscope | hf | local

# bge 系列检索时官方建议给 query（而非文档）加指令前缀，可显著提升召回
_BGE_QUERY_INSTRUCTION = "为这个句子生成表示以用于检索相关文章："

_embedder: Optional["Embedder"] = None
_embedder_lock = threading.Lock()


def get_embedder() -> "Embedder":
    """获取单例向量化器（延迟加载模型，线程安全）。"""
    global _embedder
    if _embedder is None:
        with _embedder_lock:
            if _embedder is None:
                _embedder = Embedder(EMBED_MODEL, EMBED_SOURCE)
    return _embedder


def _resolve_model_path(model_name: str, source: str) -> str:
    """把模型名解析为 SentenceTransformer 可加载的路径。

    modelscope：下载/复用本地缓存并返回本地目录（国内 HF 不可达时使用）。
    hf / local：原样返回，交给 SentenceTransformer（HF 仓库名或本地路径）。
    """
    if source == "modelscope":
        try:
            from modelscope import snapshot_download
        except ImportError as exc:  # pragma: no cover
            raise ImportError("EMBED_SOURCE=modelscope 需要安装 modelscope: pip install modelscope") from exc
        return snapshot_download(model_name)
    return model_name


class Embedder:
    """封装 sentence-transformers，输出 L2 归一化向量（便于用点积当余弦）。"""

    def __init__(self, model_name: str, source: str = "modelscope"):
        try:
            from sentence_transformers import SentenceTransformer
        except ImportError as exc:  # pragma: no cover
            raise ImportError("请安装 sentence-transformers: pip install sentence-transformers") from exc

        self.model_name = model_name
        model_path = _resolve_model_path(model_name, source)
        self.model = SentenceTransformer(model_path)
        self.is_bge = "bge" in model_name.lower()
        # sentence-transformers 5.x 把 get_sentence_embedding_dimension 改名为 get_embedding_dimension
        get_dim = getattr(self.model, "get_embedding_dimension", None) \
            or self.model.get_sentence_embedding_dimension
        self.dim = int(get_dim())

    def encode(self, texts: List[str], is_query: bool = False) -> np.ndarray:
        """把文本编码为归一化向量矩阵 (N, dim)，dtype=float32。"""
        if not texts:
            return np.empty((0, self.dim), dtype=np.float32)
        prepared = texts
        if is_query and self.is_bge:
            prepared = [_BGE_QUERY_INSTRUCTION + t for t in texts]
        vecs = self.model.encode(
            prepared,
            normalize_embeddings=True,  # 归一化后，余弦相似度 == 点积
            convert_to_numpy=True,
            show_progress_bar=False,
            batch_size=64,
        )
        return np.asarray(vecs, dtype=np.float32)
