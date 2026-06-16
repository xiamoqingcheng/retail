# 系统架构

## 总览

```
                    ┌─────────────────┐     ┌──────────────────┐
                    │  管理端 Web 8848 │     │  顾客端小程序      │
                    │  (apps/admin-web)│     │(customer-miniapp)│
                    └────────┬─────────┘     └────────┬─────────┘
                             │  /api                  │ /api/applet
                             └───────────┬────────────┘
                                         ▼
                          ┌──────────────────────────┐      ┌─────────────┐
                          │   后端 API  8080           │◀────▶│ MySQL 3306  │
                          │   (services/server)        │      └─────────────┘
                          │  - 商品/购物车/订单/告警     │      ┌─────────────┐
                          │  - 语义搜索(纯 Java)        │◀────▶│ Redis 6379  │
                          │  - 摄像头管理/巡检调度       │      └─────────────┘
                          └───────┬──────────────┬─────┘
                            /api/ai/recognize/*   │ /api/ai/cameras/*
                                  ▼               ▼
                          ┌──────────────────────────┐
                          │   AI 识别服务 8000          │
                          │   (services/ai, YOLO best.pt)│
                          │  - recognize/center|shelf|annotate
                          │  - cameras/available|frame|stream
                          └───────────┬──────────────┘
                                      │ agent:IP:port:idx（局域网）
                                      ▼
                          ┌──────────────────────────┐
                          │ 摄像头边缘端 8765/8766(UDP) │
                          │ (services/camera-agent, C++)│
                          └──────────────────────────┘
```

## 模块职责

- **apps/admin-web**：管理端，商品/告警/摄像头管理、巡检调度配置、识别标注预览。Vite dev 端口 8848，`/api` 反代到 8080。
- **apps/customer-miniapp**：顾客端，登录、搜索、扫码识别、购物车、下单、推荐流。请求基址 `utils/request.js` 的 `DEFAULT_BASE_URL`。
- **services/server**：业务核心。鉴权（管理端 JWT + 小程序 JWT）、商品/购物车/订单/库存告警、**语义商品搜索（纯 Java，见 `recommendation/semantic`）**、个性化/热搜推荐、摄像头编排与定时巡检（`CameraScanScheduler`）。
- **services/ai**：YOLO 推理与摄像头采集。对外 `/api/ai/recognize/{center,shelf/batch,annotate}` 与 `/api/ai/cameras/{available,frame,stream}`；可把摄像头编号 `agent:IP:端口:索引` 转发到局域网边缘端。
- **services/camera-agent**：客机上的轻量 C++ exe，向主机后端/AI 暴露本机摄像头（单帧 JPEG / MJPEG 流），仅依赖 Windows 系统 DLL。

## 关键数据流

1. **管理端 CRUD**：admin-web → `/api/**` → server → MySQL/Redis。
2. **小程序文本搜索**：miniapp → `/api/applet/search/text` → server `SemanticGoodsSearchService`（**全程 Java，不经 AI 服务**；商品向量已缓存，见 `GoodsSemanticVectorizer`）。
3. **小程序扫码识别**：miniapp → `/api/applet/scan` → server `AiIntegrationService` → AI `/api/ai/recognize/center` → 返回商品。
4. **货架定时巡检**：`CameraScanScheduler`（间隔/批量可配）→ 取各摄像头帧 → AI `/api/ai/recognize/shelf/batch` → 更新货架在架商品。
5. **远端摄像头**：摄像头编号填 `agent:IP:端口:索引` 时，AI 服务转发到对应 `camera-agent`。

## 端口与依赖

| 端口 | 服务 | 备注 |
|------|------|------|
| 8848 | admin-web (Vite) | 开发服务器 |
| 8080 | server | Spring Boot |
| 8000 | ai | FastAPI/uvicorn |
| 8765 / 8766 | camera-agent | HTTP / UDP 发现 |
| 3306 | MySQL | 库名 `retail_db` |
| 6379 | Redis | 缓存 |

环境覆盖（server）：`DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD`、`REDIS_*`、`AI_SERVICE_URL`、`SPRING_PROFILES_ACTIVE`。
