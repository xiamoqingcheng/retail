# 部署与运行指南

## 一、前置依赖

| 依赖 | 最低版本 | 说明 |
|------|----------|------|
| Java | 21 | Spring Boot 3.x |
| Maven | 3.9 | 后端构建 |
| Python | 3.10 | AI 识别服务 |
| Node.js | 20+ | 管理端前端 |
| MySQL | 8.0+ | 数据库 |
| Redis | 任意 | 缓存 |
| 微信开发者工具 | 任意 | 小程序调试 |

安装 AI 依赖，并放置 YOLO 模型：

```powershell
pip install -r services/ai/requirements.txt
# 将训练好的 best.pt 放到 services/ai/best.pt（模型文件不入库）
```

## 二、初始化数据库（仅首次）

> 确保 MySQL 已启动；在仓库根目录执行。

```powershell
mysql -u root < database/retail_db.sql       # 建库建表 + 管理员 + 摄像头
mysql -u root < database/retail_goods.sql     # 17 个分类 + 200 件 YOLO 对齐商品
```

未将 `mysql` 加入 PATH 时用完整路径，例如 `D:\mysql-9.6.0-winx64\bin\mysql.exe -u root < database/retail_db.sql`。

> 总启动脚本 `start.ps1` 会在 `retail_db` 不存在时自动导入以上两个脚本。

## 三、本地运行

### 推荐：一键启动

```powershell
.\start.ps1            # 或双击 start.bat
```

按序拉起 MySQL → Redis → 建库导数据 → AI(8000) → 后端(8080) → 管理端(8848)，并持续监管。
**Ctrl+C 或关闭窗口会统一停止全部由它拉起的子进程**；窗口丢失时可另跑 `.\stop.ps1`（默认不停 MySQL，加 `-IncludeMysql` 一并停）。

### 单独启动（分窗口调试）

```powershell
.\services\ai\run.ps1         # AI 识别 8000（首次加载 best.pt 约 10-30s）
.\services\server\run.ps1     # 后端 API 8080（首次编译 1-3 分钟）
.\apps\admin-web\run.ps1      # 管理端 8848（首次自动 npm install）
```

各脚本会优先使用仓库根目录内置的运行时（`apache-maven-3.9.14` / `node-v24.14.1-win-x64`），否则回退到 PATH；AI 可用环境变量 `PYTHON_EXE` 指定解释器。

### 微信小程序

1. 微信开发者工具 →「导入项目」→ 选择 `apps/customer-miniapp` 目录。
2. 填入小程序 AppID（或测试号）。
3. 「详情 →本地设置」勾选「不校验合法域名」（开发阶段）。
4. 模拟器直接运行；真机调试改 `apps/customer-miniapp/utils/request.js` 的 `DEFAULT_BASE_URL` 为本机局域网 IP（如 `http://192.168.x.x:8080/api`）。

## 四、健康检查

```powershell
netstat -ano | findstr "3306 6379 8000 8080 8848"
curl http://localhost:8000/                                # AI 探活
curl http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"123456\"}"
```

## 五、真机调试：内网穿透（ngrok）

微信真机需 HTTPS。可用 ngrok 暴露后端 8080：

```powershell
ngrok config add-authtoken <你的_authtoken>    # 在 ngrok 控制台获取，勿写入仓库
ngrok http 8080
```

将 `Forwarding` 出的 `https://xxxx.ngrok-free.app` 填入小程序 `utils/request.js` 的 `DEFAULT_BASE_URL`（加 `/api`）。免费版每次重启更换地址；请求头需带 `ngrok-skip-browser-warning`。

> ngrok 可执行文件与 authtoken 都不应提交进仓库。

## 六、生产部署（后端）

服务器建议 2C4G+，MySQL 5.7+。

```bash
sudo apt install openjdk-21-jdk          # 安装 JDK
mysql -u root -p < database/retail_db.sql && mysql -u root -p < database/retail_goods.sql
# 打包并上传 jar
mvn -f services/server/pom.xml clean package -DskipTests
scp services/server/target/*.jar user@server:/opt/retail-system/app.jar
```

通过环境变量注入配置并以 `prod` 启动：

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=localhost DB_PORT=3306 DB_NAME=retail_db DB_USERNAME=root DB_PASSWORD=***
export AI_SERVICE_URL=http://127.0.0.1:8000
nohup java -jar /opt/retail-system/app.jar > app.log 2>&1 &
```

可选 systemd 单元（`/etc/systemd/system/retail.service`）：

```ini
[Unit]
Description=Retail Smart System
After=network.target mysql.service
[Service]
Type=simple
ExecStart=/usr/bin/java -jar /opt/retail-system/app.jar
EnvironmentFile=/opt/retail-system/retail.env
Restart=always
[Install]
WantedBy=multi-user.target
```

Nginx 反代 + HTTPS：

```nginx
server {
    listen 443 ssl;
    server_name api.yourdomain.com;
    ssl_certificate     /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 七、生产部署（小程序）

1. 改 `utils/request.js` 的 `DEFAULT_BASE_URL` 为 `https://api.yourdomain.com/api`。
2. 微信公众平台 →「开发管理 →开发设置 →服务器域名」配置 request / uploadFile / downloadFile 合法域名。
3. 开发者工具「上传」→ 公众平台「版本管理」提交审核 → 发布。

## 八、关键配置位置

| 配置 | 位置 |
|------|------|
| 数据库 / 端口 / AI 地址 / Redis / 日志 | `services/server/src/main/resources/application*.yml`（均支持环境变量覆盖） |
| 管理端 API 代理 / 端口 | `apps/admin-web/.env*`、`vite.config.ts` |
| 小程序 API 基址 | `apps/customer-miniapp/utils/request.js`（`DEFAULT_BASE_URL`） |
| 小程序 AppID | `apps/customer-miniapp/project.config.json` |

## 九、常见问题

- **真机域名不合法**：开发期勾「不校验合法域名」；生产期配服务器域名。
- **文本/图像搜索异常**：文本搜索为纯 Java 语义搜索（不依赖 AI 服务）；扫码识别需 AI 服务在 `AI_SERVICE_URL` 可达，否则后端返回 502。
- **图片不显示**：检查图片 URL 与 downloadFile 合法域名。
