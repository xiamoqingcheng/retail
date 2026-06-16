# ai · AI 识别服务

FastAPI + Ultralytics YOLO。商品检测与摄像头采集，端口 **8000**。

## 模型

需将训练好的 YOLO 权重放到 `services/ai/best.pt`（模型文件不入库）。
类名格式约定 `{goods_id}_{category}`，`class_id + 1 = 数据库 goods_id`。
模型在首次推理时**懒加载**且全局单例（见 `app/recognition/detector.py`）。

## 运行

```powershell
.\run.ps1                  # 单独启动（可用环境变量 PYTHON_EXE 指定解释器）
# 或
pip install -r requirements.txt
python main.py             # http://localhost:8000
```

## 对外接口

```
POST /api/ai/recognize/center        # 以画面中心为锚点取最近 k 个不同类商品
POST /api/ai/recognize/shelf/batch   # 多摄像头批量货架识别（置信度≥0.5）
POST /api/ai/recognize/annotate      # 返回带标注框的图片
GET  /api/ai/cameras/available        # 本机可用摄像头
GET  /api/ai/cameras/frame/{index}    # 单帧
GET  /api/ai/cameras/stream/{index}   # MJPEG 流
```

摄像头编号支持 `agent:IP:端口:索引`，转发到局域网 [camera-agent](../camera-agent/README.md)。
> 说明：商品文本搜索由后端 Java 语义引擎实现，**不在本服务**。
