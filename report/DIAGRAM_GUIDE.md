# 零售物品智能识别系统 — 图表绘制指导文档

> **适用范围**：《系统概要设计报告》中涉及的全部 UML 图与 ER 图
> **推荐工具**：[draw.io (diagrams.net)](https://app.diagrams.net/) / PlantUML / Visio
> **源文件目录**：`report/puml/`，渲染输出到 `report/images/`

---

## 一、需要绘制的图表清单

| 编号 | 图名 | 类型 | 报告章节 | 源文件 |
|------|------|------|---------|--------|
| 1 | 逻辑视图（简化类图） | UML 类图 | 2.2 系统体系结构设计 §(1) | `fig-logic-view.puml` |
| 2 | 开发运行视图（组件交互图） | UML 组件图 | 2.2 系统体系结构设计 §(2) | `fig-component-view.puml` |
| 3 | 部署视图（部署图） | UML 部署图 | 2.2 系统体系结构设计 §(3) | `fig-deploy-view.puml` |
| 4 | 概念数据库设计（ER 图） | E-R 图 | 4.1 概念数据库设计 | `fig-er-diagram.puml` |

---

## 二、图 1 — 逻辑视图（简化类图）

### 2.1 目的

展示 retail-server 内部分层架构（Controller → Service → Mapper → Entity）和与 retail-ai 的跨系统依赖。

### 2.2 包含元素

#### 包（Package）

| 包名 | 层级 | 包含的类 |
|------|------|---------|
| controller（控制层） | 最顶层 | AuthController, GoodsController, CartController, OrderController, WarningController, CameraController, FileController |
| controller.applet（小程序控制层） | 同上 | AppletAuthController, AppletHomeController, AppletGoodsController, AppletCameraController |
| service（业务层） | 第二层 | GoodsService, CartService, OrderService, WarningService, UserService, AiIntegrationService |
| mapper（持久层） | 第三层 | GoodsMapper, OrderMapper, OrderItemMapper, CameraMapper, UserMapper, WechatUserMapper, WarningMapper, AdMapper, InventoryLogMapper, GoodsCategoryMapper |
| entity（实体层） | 最底层 | Goods, GoodsCategory, Order, OrderItem, Warning, Camera, User, WechatUser, Ad, InventoryLog |
| 基础设施 | 横切 | GlobalExceptionHandler, Result\<T\>, BusinessException, CameraScanScheduler, WebConfig, JwtAuthInterceptor |
| retail-ai（外部系统） | 独立 | CameraManager, recognition（识别模块）, search（搜索模块） |

#### 依赖关系

```
controller → service（方法调用）
service → mapper（数据访问）
mapper → entity（ORM 映射）
service → retail-ai（HTTP/JSON via RestTemplate）
```

### 2.3 绘制建议

- **工具**：draw.io 使用 UML Package Diagram 模板，或 PlantUML class diagram
- **布局**：自上而下（Top-Down），每层一个 Package 矩形
- **配色**：每层使用不同浅色背景区分

---

## 三、图 2 — 开发运行视图（组件交互图）

### 3.1 目的

展示四个子系统之间的通信关系、使用的协议与端口、外部依赖（MySQL/Redis/摄像头）。

### 3.2 包含元素

#### 参与者（Actor）

| 名称 | 说明 |
|------|------|
| 商超管理员 | 通过浏览器访问管理端 |
| 小程序用户 | 通过微信客户端使用小程序 |

#### 组件（Component）

| 组件名 | 技术栈 | 端口 | 部署位置 |
|--------|--------|------|---------|
| retail-admin-pro | Vue3 + Vite + Element Plus + TypeScript | 5173 | 前端开发机 |
| retail-customer | 微信小程序 (WXML + WXSS + JS) | — | 微信平台 |
| retail-server | Spring Boot 3.3 + Java 21 + MyBatis-Plus | 8080 | 应用服务器 |
| retail-ai | FastAPI + Uvicorn + OpenCV | 8000 | 应用服务器 |

#### 外部依赖

| 名称 | 类型 | 端口 |
|------|------|------|
| MySQL 8.x (retail_db) | 数据库 | 3306 |
| Redis 8.x | 缓存 | 6379 |
| USB 摄像头 | 硬件设备 | DirectShow |

#### 连接关系

| 起点 | 终点 | 协议 | 说明 |
|------|------|------|------|
| 管理员 | retail-admin-pro | 浏览器 | 用户交互 |
| 小程序用户 | retail-customer | 微信客户端 | 用户交互 |
| retail-admin-pro | retail-server | HTTP/JSON | Vite 代理 `/api/*` |
| retail-customer | retail-server | HTTPS/JSON | `wx.request /api/applet/*` |
| retail-server | MySQL | JDBC | MyBatis-Plus ORM |
| retail-server | Redis | Lettuce | 推荐流缓存 + 购物车 |
| retail-server | retail-ai | HTTP/JSON | RestTemplate `/api/ai/*` |
| retail-ai | USB 摄像头 | DirectShow | OpenCV `cv2.VideoCapture` |

#### 注释说明

- retail-server 上标注：定时巡检 `CameraScanScheduler` 每 5 分钟调用 `/api/ai/recognize/shelf/batch`
- retail-ai 上标注：AI 能力端点（摄像头探测/预览/流、中心点识别、批量货架识别、文本检索）

### 3.3 绘制建议

- **工具**：draw.io 使用 UML Component Diagram，或 PlantUML component diagram
- **布局**：左右布局（用户在左，后端服务在中，数据库/硬件在右）
- **连线**：实线箭头标注协议和端口

---

## 四、图 3 — 部署视图（部署图）

### 4.1 目的

展示系统在物理/虚拟节点上的部署拓扑与制品分布。

### 4.2 包含元素

#### 节点（Node）及制品（Artifact）

| 节点 | 运行环境 | 制品 | 端口 |
|------|---------|------|------|
| 前端开发机 | Node.js 24 | retail-admin-pro（pnpm + Vite 构建） | 5173 |
| 微信云 | 微信客户端 | retail-customer（小程序包） | — |
| 应用服务器 | Java 21 + Python 3.10+ | retail-server.jar（Maven 构建） | 8080 |
| 应用服务器 | （同上） | retail-ai/main.py（pip + uvicorn） | 8000 |
| 数据库服务器 | — | MySQL 8.x（retail_db） | 3306 |
| 缓存服务器 | — | Redis 8.x | 6379 |
| 物理设备 | DirectShow | USB 摄像头 | — |

#### 通信路径

| 起点制品 | 终点制品 | 协议/端口 |
|---------|---------|----------|
| retail-admin-pro | retail-server.jar | HTTP :8080 (Vite proxy) |
| retail-customer | retail-server.jar | HTTPS :8080 |
| retail-server.jar | MySQL | JDBC :3306 |
| retail-server.jar | Redis | Lettuce :6379 |
| retail-server.jar | retail-ai | HTTP :8000 |
| retail-ai | USB 摄像头 | DirectShow |

### 4.3 绘制建议

- **工具**：draw.io 使用 UML Deployment Diagram，或 PlantUML deployment diagram
- **布局**：3D 盒子代表节点，内部嵌套制品
- **生产环境注意**：retail-server 与 retail-ai 建议分离部署到不同节点

---

## 五、图 4 — 概念数据库设计（ER 图）⭐ 重点

### 5.1 目的

展示系统全部 10 张数据表的完整属性、主键/外键/唯一键约束及表间关系。

### 5.2 推荐工具

- **首选**：draw.io（使用 Entity Relationship / Crow's Foot 模板）
- **备选**：PlantUML entity diagram, MySQL Workbench 逆向工程

### 5.3 全部实体及完整属性

> 以下严格对照 `retail/retail_db.sql`，每个字段均列出。

#### 5.3.1 sys_user（系统用户表）

| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
|--------|---------|------|--------|------|
| **id** | BIGINT UNSIGNED | **PK**, AUTO_INCREMENT | — | 主键 ID |
| username | VARCHAR(64) | NOT NULL, **UNIQUE** | — | 用户名 |
| password | VARCHAR(255) | NOT NULL | — | 密码（明文/摘要） |
| role | ENUM('admin','customer') | NOT NULL | 'customer' | 角色 |
| status | TINYINT | NOT NULL | 1 | 状态（1 启用/0 禁用） |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：`uk_sys_user_username (username)`

---

#### 5.3.2 sys_goods_category（商品分类表）

| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
|--------|---------|------|--------|------|
| **id** | BIGINT UNSIGNED | **PK**, AUTO_INCREMENT | — | 分类 ID |
| name | VARCHAR(64) | NOT NULL, **UNIQUE** | — | 分类名称 |
| sort_order | INT | NOT NULL | 0 | 排序值 |
| status | TINYINT | NOT NULL | 1 | 状态（1 启用/0 禁用） |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`uk_category_name (name)`

---

#### 5.3.3 sys_goods（商品基础表）

| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
|--------|---------|------|--------|------|
| **id** | BIGINT UNSIGNED | **PK**, AUTO_INCREMENT | — | 主键 ID |
| name | VARCHAR(128) | NOT NULL | — | 商品名称 |
| barcode | VARCHAR(64) | NULLABLE | NULL | 条形码 |
| category_id | BIGINT UNSIGNED | NULLABLE, **FK → sys_goods_category.id** | NULL | 分类 ID |
| price | DECIMAL(10,2) | NOT NULL | 0.00 | 商品价格 |
| stock | INT | NOT NULL | 0 | 库存数量 |
| safe_stock | INT | NOT NULL | 10 | 安全库存阈值 |
| shelf_id | VARCHAR(64) | NOT NULL | — | 货架编号 |
| image_url | VARCHAR(255) | NULLABLE | NULL | 商品图片 URL |
| status | TINYINT | NOT NULL | 1 | 状态（1 上架/0 下架） |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：`idx_goods_shelf_id (shelf_id)`, `idx_goods_category_id (category_id)`, `idx_goods_barcode (barcode)`, `idx_goods_status (status)`

---

#### 5.3.4 sys_order（订单主表）

| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
|--------|---------|------|--------|------|
| **id** | BIGINT UNSIGNED | **PK**, AUTO_INCREMENT | — | 订单 ID |
| user_id | BIGINT UNSIGNED | NOT NULL, **FK → sys_user_wechat.id** | — | 用户 ID |
| total_amount | DECIMAL(10,2) | NOT NULL | 0.00 | 订单总金额 |
| status | VARCHAR(32) | NOT NULL | 'PENDING' | 订单状态（PENDING/PAID/COMPLETED/CANCELLED） |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：`idx_order_user_id (user_id)`, `idx_order_status (status)`, `idx_order_create_time (create_time)`

---

#### 5.3.5 sys_order_item（订单明细表）

| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
|--------|---------|------|--------|------|
| **id** | BIGINT UNSIGNED | **PK**, AUTO_INCREMENT | — | 明细 ID |
| order_id | BIGINT UNSIGNED | NOT NULL, **FK → sys_order.id** | — | 订单 ID |
| goods_id | BIGINT UNSIGNED | NOT NULL, **FK → sys_goods.id** | — | 商品 ID |
| goods_name | VARCHAR(128) | NOT NULL | '' | 商品名称快照 |
| price | DECIMAL(10,2) | NOT NULL | 0.00 | 商品单价快照 |
| quantity | INT | NOT NULL | 1 | 购买数量 |

**索引**：`idx_order_item_order_id (order_id)`, `idx_order_item_goods_id (goods_id)`

---

#### 5.3.6 sys_warning（库存告警表）

| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
|--------|---------|------|--------|------|
| **id** | BIGINT UNSIGNED | **PK**, AUTO_INCREMENT | — | 告警 ID |
| goods_id | BIGINT UNSIGNED | NOT NULL, **FK → sys_goods.id** | — | 商品 ID |
| warning_type | VARCHAR(32) | NOT NULL | 'LOW_STOCK' | 告警类型（LOW_STOCK/OUT_OF_STOCK/EXPIRING） |
| warning_msg | VARCHAR(255) | NOT NULL | — | 告警内容 |
| status | TINYINT | NOT NULL | 0 | 处理状态（0 未处理/1 已处理） |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| resolve_time | DATETIME | NULLABLE | NULL | 处理时间 |

**索引**：`idx_warning_goods_id (goods_id)`, `idx_warning_status (status)`, `idx_warning_create_time (create_time)`

---

#### 5.3.7 sys_inventory_log（库存变更日志表）

| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
|--------|---------|------|--------|------|
| **id** | BIGINT UNSIGNED | **PK**, AUTO_INCREMENT | — | 日志 ID |
| goods_id | BIGINT UNSIGNED | NOT NULL, **FK → sys_goods.id** | — | 商品 ID |
| change_amount | INT | NOT NULL | 0 | 变动数量（正:补货 / 负:扣减） |
| current_stock | INT | NOT NULL | 0 | 变动后当前库存 |
| type | VARCHAR(32) | NOT NULL | — | 日志类型（SALE/RESTOCK/ADJUST/AI_DETECT） |
| remark | VARCHAR(255) | NULLABLE | NULL | 备注 |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_inv_log_goods_id (goods_id)`, `idx_inv_log_type (type)`, `idx_inv_log_create_time (create_time)`

---

#### 5.3.8 sys_user_wechat（小程序用户表）

| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
|--------|---------|------|--------|------|
| **id** | BIGINT UNSIGNED | **PK**, AUTO_INCREMENT | — | 主键 ID |
| openid | VARCHAR(128) | NOT NULL, **UNIQUE** | — | 微信用户唯一标识 |
| nickname | VARCHAR(64) | NULLABLE | NULL | 昵称 |
| avatar_url | VARCHAR(255) | NULLABLE | NULL | 头像 URL |
| balance | DECIMAL(10,2) | NOT NULL | 0.00 | 钱包余额 |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：`uk_wechat_openid (openid)`

---

#### 5.3.9 sys_ad（首页轮播广告表）

| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
|--------|---------|------|--------|------|
| **id** | BIGINT UNSIGNED | **PK**, AUTO_INCREMENT | — | 主键 ID |
| image_url | VARCHAR(255) | NOT NULL | — | 轮播图图片 URL |
| link_url | VARCHAR(255) | NULLABLE | NULL | 点击跳转链接 |
| sort_order | INT | NOT NULL | 0 | 排序值（越小越靠前） |
| status | TINYINT | NOT NULL | 1 | 状态（0 禁用/1 启用） |

**索引**：`idx_ad_status_sort (status, sort_order)` 复合索引

---

#### 5.3.10 sys_camera（摄像头管理表）

| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
|--------|---------|------|--------|------|
| **id** | BIGINT UNSIGNED | **PK**, AUTO_INCREMENT | — | 摄像头 ID |
| camera_no | VARCHAR(32) | NOT NULL, **UNIQUE** | — | 摄像头编号 |
| shelf_id | VARCHAR(64) | NOT NULL | — | 绑定货架编号 |
| status | TINYINT | NOT NULL | 1 | 状态（1 正常/0 停用） |
| create_time | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |

**索引**：`uk_camera_no (camera_no)`, `idx_camera_status (status)`

---

### 5.4 实体间关系（Crow's Foot 记法）

| 关系 | 基数 | 外键路径 | 说明 |
|------|------|---------|------|
| sys_goods_category → sys_goods | 1 : N | `sys_goods.category_id` → `sys_goods_category.id` | 一个分类下有多个商品 |
| sys_user_wechat → sys_order | 1 : N | `sys_order.user_id` → `sys_user_wechat.id` | 一个小程序用户有多笔订单 |
| sys_order → sys_order_item | 1 : N | `sys_order_item.order_id` → `sys_order.id` | 一笔订单含多个明细 |
| sys_goods → sys_order_item | 1 : N | `sys_order_item.goods_id` → `sys_goods.id` | 一个商品出现在多个订单明细中 |
| sys_goods → sys_warning | 1 : N | `sys_warning.goods_id` → `sys_goods.id` | 一个商品可产生多条告警 |
| sys_goods → sys_inventory_log | 1 : N | `sys_inventory_log.goods_id` → `sys_goods.id` | 一个商品有多条库存变更记录 |
| sys_camera ↔ sys_goods | N : M (逻辑) | 通过 `shelf_id` 字段逻辑关联 | 摄像头绑定货架，货架上有多个商品 |

> **注意**：`sys_user`（管理端用户）与 `sys_user_wechat`（小程序用户）是两套独立的用户体系，不存在直接外键关系。`sys_ad`（广告表）为独立表，不与其他表关联。

### 5.5 ER 图绘制要点（陈氏 Chen's 记法）

当前 ER 图采用 **陈氏（Chen's）记法**，通过 Graphviz DOT 语言绘制：

| 图形 | 含义 | 样式 |
|------|------|------|
| **矩形（Box）** | 实体（Entity） | 蓝色填充 |
| **椭圆（Ellipse）** | 属性（Attribute） | 白色填充；PK 字段文字**下划线** |
| **虚线椭圆** | 外键属性（FK） | 橙色虚线边框 |
| **菱形（Diamond）** | 联系（Relationship） | 橙色填充 |
| **连线上数字** | 基数（Cardinality） | `1` / `N` / `M` |
| **虚线连线** | 逻辑关联（非物理外键） | 如 `shelf_id` 关联 |

**绘制规则**：

1. 每个实体（表）对应一个矩形，属性以椭圆形式辐射分布在实体周围
2. 主键属性用下划线标记（如 `_id_`），唯一键标记 `(UK)`
3. 外键属性用虚线椭圆，标记 `(FK)`
4. 实体间通过菱形联系节点连接，菱形内标注联系名称（如"属于""下单"）
5. 连线上标注基数：`1` 端表示"一"，`N` 端表示"多"
6. 独立表（`sys_ad`、`sys_user`）无外键联系，放在图的边缘
7. `sys_camera ↔ sys_goods` 通过 `shelf_id` 逻辑关联，用虚线 N:M 表示

---

## 六、使用 draw.io 绘图操作步骤

### 6.1 安装

- **VS Code 插件**：搜索安装 `hediet.vscode-drawio`，可直接在 IDE 中编辑 `.drawio` 文件
- **在线版**：访问 <https://app.diagrams.net/>
- **桌面版**：<https://github.com/jgraph/drawio-desktop/releases>

### 6.2 新建 ER 图

1. 新建文件 → 选择模板 `Software / Entity Relationship`
2. 从左侧面板拖入 `Entity` 元素
3. 双击编辑属性，按上述 §5.3 填入全部字段
4. 使用 `Relationship` 连线，设置 Crow's Foot 端标记
5. 导出为 PNG（建议 DPI ≥ 200）放入 `report/images/fig-er-diagram.png`

### 6.3 新建 UML 图

1. 新建文件 → 选择模板 `Software / UML` 对应子类型
2. 类图：拖入 `Class` 元素，按 §二 填入包和类
3. 组件图：拖入 `Component` / `Node` 元素，按 §三 填入
4. 部署图：拖入 `Node` / `Artifact` 元素，按 §四 填入

---

## 七、使用 PlantUML 渲染命令

所有 `.puml` 源文件位于 `report/puml/`，渲染命令：

```bash
# ER 图（DOT 格式，需要 Graphviz）
java "-DGRAPHVIZ_DOT=C:\Program Files\Graphviz\bin\dot.exe" -jar report/puml/plantuml.jar -tpng -o ../images report/puml/fig-er-diagram.puml

# 其他 UML 图（PlantUML 原生语法，无需 Graphviz）
java -jar report/puml/plantuml.jar -tpng -o ../images report/puml/fig-logic-view.puml
java -jar report/puml/plantuml.jar -tpng -o ../images report/puml/fig-component-view.puml
java -jar report/puml/plantuml.jar -tpng -o ../images report/puml/fig-deploy-view.puml
```

> **依赖**：Java 运行环境 + Graphviz（`winget install Graphviz.Graphviz`）
> PlantUML JAR 路径：`D:\Project\report\puml\plantuml.jar`
> Graphviz DOT 路径：`C:\Program Files\Graphviz\bin\dot.exe`

---

## 八、文件清单

| 文件路径 | 说明 |
|---------|------|
| `report/puml/fig-er-diagram.puml` | ER 图 DOT 源文件（陈氏记法，含全部 67 个属性） |
| `report/puml/fig-logic-view.puml` | 逻辑视图 PlantUML 源文件 |
| `report/puml/fig-component-view.puml` | 组件交互图 PlantUML 源文件 |
| `report/puml/fig-deploy-view.puml` | 部署视图 PlantUML 源文件 |
| `report/images/*.png` | 渲染后的 PNG 图片 |
| `report/DIAGRAM_GUIDE.md` | 本指导文档 |
