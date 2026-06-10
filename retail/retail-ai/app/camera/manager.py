"""
线程安全的摄像头管理器。

- 引用计数：多个客户端可共享同一摄像头
- 空闲超时：无客户端后自动释放硬件
- 显式释放：提供 release / release_all 方法
"""

import threading
import time
from dataclasses import dataclass, field

import cv2

from app.config import STREAM_IDLE_TIMEOUT


@dataclass
class _CameraSlot:
    """一个摄像头槽位。"""

    cap: cv2.VideoCapture
    ref_count: int = 0
    last_access: float = field(default_factory=time.monotonic)
    lock: threading.Lock = field(default_factory=threading.Lock)


class CameraManager:
    """全局单例摄像头管理器，线程安全。"""

    def __init__(self, idle_timeout: float = STREAM_IDLE_TIMEOUT):
        self._slots: dict[int, _CameraSlot] = {}
        self._lock = threading.Lock()
        self._idle_timeout = idle_timeout
        self._reaper_running = False
        self._reaper_thread: threading.Thread | None = None

    # ---------- 公开 API ----------

    def acquire(self, camera_index: int) -> cv2.VideoCapture | None:
        """获取摄像头并增加引用计数。首次打开时自动创建。"""
        with self._lock:
            slot = self._slots.get(camera_index)
            if slot is None:
                cap = cv2.VideoCapture(camera_index, cv2.CAP_DSHOW)
                if not cap.isOpened():
                    cap.release()
                    return None
                slot = _CameraSlot(cap=cap)
                self._slots[camera_index] = slot
                self._ensure_reaper()
            slot.ref_count += 1
            slot.last_access = time.monotonic()
            return slot.cap

    def release(self, camera_index: int) -> None:
        """减少引用计数，引用为 0 时标记为空闲等待回收。"""
        with self._lock:
            slot = self._slots.get(camera_index)
            if slot is None:
                return
            slot.ref_count = max(0, slot.ref_count - 1)
            slot.last_access = time.monotonic()

    def force_release(self, camera_index: int) -> None:
        """立即释放指定摄像头，不论引用计数。"""
        with self._lock:
            slot = self._slots.pop(camera_index, None)
        if slot is not None:
            try:
                slot.cap.release()
            except Exception:
                pass

    def release_all(self) -> None:
        """释放所有摄像头。"""
        with self._lock:
            slots = list(self._slots.items())
            self._slots.clear()
        for _, slot in slots:
            try:
                slot.cap.release()
            except Exception:
                pass

    def available_indexes(self, max_probe: int = 6) -> list[int]:
        """探测本机可用摄像头索引，跳过已被管理的摄像头。"""
        with self._lock:
            existing = set(self._slots.keys())
        indexes: list[int] = list(existing)
        for i in range(max_probe):
            if i in existing:
                continue
            cap = cv2.VideoCapture(i, cv2.CAP_DSHOW)
            try:
                if cap.isOpened():
                    indexes.append(i)
            finally:
                cap.release()
        return indexes

    def read_frame(self, camera_index: int) -> tuple[bool, any]:
        """从已打开的摄像头读取一帧（线程安全）。"""
        with self._lock:
            slot = self._slots.get(camera_index)
            if slot is None:
                return False, None
            slot.last_access = time.monotonic()
            return slot.cap.read()

    # ---------- 后台回收线程 ----------

    def _ensure_reaper(self) -> None:
        if self._reaper_running:
            return
        self._reaper_running = True
        self._reaper_thread = threading.Thread(
            target=self._reaper_loop, daemon=True, name="cam-reaper"
        )
        self._reaper_thread.start()

    def _reaper_loop(self) -> None:
        while True:
            time.sleep(5)
            now = time.monotonic()
            to_remove: list[int] = []
            with self._lock:
                if not self._slots:
                    self._reaper_running = False
                    return
                for idx, slot in self._slots.items():
                    if slot.ref_count <= 0 and (now - slot.last_access) > self._idle_timeout:
                        to_remove.append(idx)
                removed_slots = [self._slots.pop(idx) for idx in to_remove]
            for slot in removed_slots:
                try:
                    slot.cap.release()
                except Exception:
                    pass


# 全局单例
camera_manager = CameraManager()
