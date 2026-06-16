# server · 后端 API

Spring Boot 3 / Java 21 / MyBatis。系统业务核心，端口 **8080**。

## 职责

- 鉴权：管理端 JWT、小程序 JWT
- 商品 / 购物车 / 订单 / 库存告警
- 语义商品搜索（**纯 Java**，`recommendation/semantic`，商品向量已缓存）
- 个性化推荐 / 热门搜索（`recommendation`）
- 摄像头管理与定时巡检（`controller/CameraController`、`scheduler/CameraScanScheduler`）
- 调用 AI 服务（`service/impl/AiIntegrationServiceImpl` → `/api/ai/recognize/*`、`/api/ai/cameras/*`）

## 运行

```powershell
.\run.ps1                       # 单独启动（优先用仓库内置 Maven）
# 或
mvn spring-boot:run             # 开发
mvn clean package -DskipTests   # 打包到 target/*.jar
```

## 配置

`src/main/resources/application*.yml`，全部支持环境变量覆盖：
`DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD`、`REDIS_*`、`AI_SERVICE_URL`、`SPRING_PROFILES_ACTIVE`（默认 `dev`）。

接口规范见 [../../docs/api.md](../../docs/api.md)，架构见 [../../docs/architecture.md](../../docs/architecture.md)。
