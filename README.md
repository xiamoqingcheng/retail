# 零售物品智能识别系统（多端融合）

基于 YOLO 商品识别的多端零售系统：微信小程序（顾客端）、Web 管理端、后端 API、AI 识别服务、局域网摄像头边缘端。

## 组成与端口

| 模块 | 目录 | 技术栈 | 端口 |
|------|------|--------|------|
| 管理端 Web | `apps/admin-web` | Vue3 + Vite + TS（Geeker-Admin） | 8848 |
| 顾客端小程序 | `apps/customer-miniapp` | 微信小程序原生 | —（开发者工具） |
| 后端 API | `services/server` | Spring Boot 3 / Java 21 / MyBatis | 8080 |
| AI 识别服务 | `services/ai` | FastAPI / Ultralytics YOLO | 8000 |
| 摄像头边缘端 | `services/camera-agent` | C++ / Media Foundation | 8765(HTTP) / 8766(UDP) |
| 数据库 | `database/` | MySQL | 3306 |
| 缓存 | — | Redis | 6379 |

详见 [docs/architecture.md](docs/architecture.md)。

## 目录结构

```
retail/
├─ apps/                 # 面向用户的客户端
│  ├─ admin-web/         #   管理端（含独立启动脚本 run.ps1）
│  └─ customer-miniapp/  #   微信小程序
├─ services/             # 后端服务
│  ├─ server/            #   后端 API（run.ps1）
│  ├─ ai/                #   AI 识别（run.ps1）
│  └─ camera-agent/      #   摄像头边缘端（build.ps1）
├─ database/             # 建库 / 商品 / 升级 SQL
├─ docs/                 # 规范文档（架构 / 接口 / 部署 / 库表）
├─ tests/performance/    # JMeter + 冒烟脚本
├─ report/              # 毕业设计 LaTeX（自包含）
├─ start.ps1 / start.bat # 总启动（本地联调，含停止管理）
└─ stop.ps1              # 兜底停止
```

## 快速开始（本地联调）

前置依赖：Java 21、Maven 3.9、Python 3.10、Node 20+、MySQL 8+、Redis、微信开发者工具。
首次需安装 AI 依赖：`pip install -r services/ai/requirements.txt`，并将 YOLO 模型 `best.pt` 放入 `services/ai/`。

**一键启动全部服务**（拉起 MySQL → Redis → 建库导数据 → AI → 后端 → 管理端，并持续监管）：

```powershell
.\start.ps1          # 或双击 start.bat
# 按 Ctrl+C 或关闭窗口即停止全部子进程；也可另开窗口运行 .\stop.ps1
```

**单独启动某个服务**（各服务目录下自带脚本）：

```powershell
.\services\server\run.ps1     # 后端 API  8080
.\services\ai\run.ps1         # AI 识别   8000
.\apps\admin-web\run.ps1      # 管理端    8848
```

小程序：用微信开发者工具打开 `apps/customer-miniapp` 目录。

完整步骤（含数据库初始化、真机调试、生产部署）见 [docs/deployment.md](docs/deployment.md)。

## 默认管理员账户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin / zhaosheng / chenzhifeng | 123456 | 管理员（密码 bcrypt 存储） |

## 文档索引

- [docs/architecture.md](docs/architecture.md) — 系统架构、模块职责、数据流
- [docs/api.md](docs/api.md) — 后端 API 接口规范 + 测试排错
- [docs/deployment.md](docs/deployment.md) — 本地运行、真机调试、生产部署
- [docs/database.md](docs/database.md) — 数据库库表说明

> 安全提示：请勿将密钥/Token 提交进仓库。历史上 `配置文档.md` 曾包含一个 ngrok authtoken，已移除，建议在 ngrok 控制台吊销重置。
