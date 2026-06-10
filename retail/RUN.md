# 智能零售系统 — 运行指南

---

## 前置条件

在启动项目前，请确保以下依赖已安装：

| 依赖 | 最低版本 | 说明 |
|------|----------|------|
| Java | 21 | Spring Boot 3.x |
| Maven | 3.9 | 后端构建 |
| Python | 3.10 | AI 识别服务 |
| Node.js | 20+ | 管理端前端 |
| pnpm | (任意) | 管理端包管理器 (npm 亦可) |
| MySQL | 8.0+ | 数据库 |
| Redis | (任意) | 缓存 |
| 微信开发者工具 | (任意) | 小程序调试 |

安装 Python 依赖：
```powershell
pip install -r retail-ai/requirements.txt
```

---

## 1. 初始化数据库（仅首次需执行）

> 请确保 MySQL 已启动，以下命令在项目根目录执行。

```powershell
# 步骤一：建库建表
mysql -u root < retail_db.sql

# 步骤二：导入商品数据（必须在步骤一之后）
mysql -u root < retail_goods.sql
```

如果未将 `mysql` 加入 PATH，请使用完整路径：
```powershell
# 例如免安装版 MySQL
C:\mysql\bin\mysql.exe -u root < retail_db.sql
C:\mysql\bin\mysql.exe -u root < retail_goods.sql
```

执行结果：
- `retail_db.sql` → 创建 `retail_db` 库、10 张表、3 个管理员账户、摄像头数据
- `retail_goods.sql` → 清空并重建 17 个分类、插入 200 件 YOLO 对齐商品

---

## 2. 启动服务

按以下顺序启动，每个服务在单独的终端窗口中运行。

### 2.1 MySQL

```powershell
# Windows 服务方式（已安装为服务）
net start MySQL

# 或免安装版
mysqld --console
```

### 2.2 Redis

```powershell
redis-server.exe
```

### 2.3 AI 识别服务（端口 8000）

```powershell
cd retail-ai
python main.py
# 首次启动加载 best.pt 约 10-30 秒
# 输出 "Application startup complete" 即就绪
```

### 2.4 后端 API（端口 8080）

```powershell
cd retail-server
mvn spring-boot:run
# 首次编译下载依赖约 1-3 分钟
```

### 2.5 管理端前端（端口 8848）

```powershell
cd retail-admin-pro
pnpm install   # 仅首次，npm install 亦可
pnpm dev       # npm run dev 亦可
```

### 2.6 微信小程序

```
1. 打开微信开发者工具
2. 导入项目 → 选择 retail-customer 目录
3. AppID: wxa3c891b495325b9b (或使用测试号)
4. 详情 → 本地设置 → 勾选"不校验合法域名"
5. 模拟器直接运行
6. 真机调试: 修改 retail-customer/utils/request.js 中的 DEFAULT_BASE_URL
   改为本机局域网 IP（如 http://192.168.x.x:8080/api）
```

---

## 3. 健康检查

```powershell
# 端口监听
netstat -ano | findstr "3306 "    # MySQL
netstat -ano | findstr "6379 "    # Redis
netstat -ano | findstr "8000 "    # AI 识别
netstat -ano | findstr "8080 "    # 后端 API
netstat -ano | findstr "8848 "    # 管理端前端

# 后端登录测试
curl http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"123456\"}"

# AI 服务探活
curl http://localhost:8000/
```

---

## 4. ngrok 公网转发（真机调试用）

```powershell
ngrok http 8080
```

输出示例：`Forwarding  https://abc123.ngrok-free.app -> http://localhost:8080`

然后修改小程序 API 地址：
```
retail-customer/utils/request.js
DEFAULT_BASE_URL = 'https://abc123.ngrok-free.app/api'
```

---

## 5. 项目结构

```
retail/
├── retail_db.sql            # 建库建表 + 管理员 + 摄像头
├── retail_goods.sql         # 200 件 YOLO 对齐商品数据
├── RUN.md                   # 本文件
├── api.md                   # Postman API 测试文档 (29 个接口)
├── retail-server/           # Spring Boot 后端 (8080)
│   └── src/main/resources/application.yml
├── retail-ai/               # Python AI 识别 (8000)
│   ├── main.py
│   ├── best.pt              # YOLO 商品识别模型
│   └── requirements.txt
├── retail-admin-pro/        # Vue3 管理端 (8848)
├── retail-customer/         # 微信小程序
└── log/                     # 日志文件 (运行时自动创建)
```

---

## 6. 关键配置文件

| 配置项 | 文件位置 | 说明 |
|--------|----------|------|
| 数据库连接 | `retail-server/.../application.yml` | MySQL root@localhost:3306 |
| 后端端口 | `同上` | `server.port: 8080` |
| AI 服务地址 | `同上` | `ai.service-url: http://localhost:8000` |
| Redis | `同上` | `spring.data.redis.host: 127.0.0.1` |
| 日志文件 | `同上` | `./log/retail-server.log` |
| 管理端代理 | `retail-admin-pro/.env.development` | `/api` → `http://localhost:8080` |
| 管理端端口 | `retail-admin-pro/.env` | `VITE_PORT: 8848` |
| 小程序 API | `retail-customer/utils/request.js` | `DEFAULT_BASE_URL` |
| 小程序 AppID | `retail-customer/project.config.json` | `appid` 字段 |

---

## 7. 默认管理员账户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 管理员 |
| zhaosheng | 123456 | 管理员 |
| chenzhifeng | 123456 | 管理员 |

> 密码通过 bcrypt 加密存储。
