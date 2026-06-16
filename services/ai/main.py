"""retail-ai 服务入口。"""

import os
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI

from app.camera.manager import camera_manager
from app.camera.routes import router as camera_router
from app.recognition.routes import router as recognition_router
from app.semantic.routes import router as semantic_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield
    camera_manager.release_all()


app = FastAPI(title="retail-ai", version="1.0.0", lifespan=lifespan)

app.include_router(camera_router)
app.include_router(recognition_router)
app.include_router(semantic_router)


@app.get("/")
def index() -> dict:
    return {"service": "retail-ai", "status": "ok"}


if __name__ == "__main__":
    reload_enabled = os.getenv("UVICORN_RELOAD", "0") == "1"
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=reload_enabled)
