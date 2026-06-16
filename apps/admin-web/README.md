# admin-web · 管理端

零售系统 Web 管理端：商品、库存告警、摄像头管理、巡检调度、识别标注预览。

- 技术栈：Vue 3 + TypeScript + Vite + Pinia + Element Plus（基于 Geeker-Admin 模板二次开发）
- 开发端口：**8848**，`/api` 反向代理到后端 `http://localhost:8080`
- 包管理器：**npm**

## 运行

```powershell
# 仓库根目录一键起全部
..\..\start.ps1

# 或单独启动本模块（首次自动 npm install）
.\run.ps1

# 或手动
npm install
npm run dev        # 开发
npm run build:pro  # 生产构建到 dist/（另有 build:dev / build:test）
```

## 配置

| 项 | 位置 |
|----|------|
| API 地址 / 构建参数 | `.env`、`.env.development`、`.env.production`（`.env.production.example` 为模板） |
| 开发端口、代理 | `vite.config.ts` |

接口规范见 [../../docs/api.md](../../docs/api.md)。
