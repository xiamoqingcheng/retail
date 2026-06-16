# 零售智能识别系统 - 后端API接口文档

## 📌 基础信息

| 项目 | 说明 |
|------|------|
| **服务地址** | `http://localhost:8080` |
| **API前缀** | `/api` |
| **认证方式** | JWT Bearer Token |
| **数据格式** | JSON |
| **字符编码** | UTF-8 |

---

## 📋 统一响应格式

所有接口均返回以下JSON结构：

```json
{
  "code": 200,
  "msg": "success",
  "data": { }
}
```

### 响应码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（登录失效或Token无效） |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 502 | 网关错误（调用下游服务失败） |

### 认证说明

除登录接口外，其他接口需要在请求头中携带Token：

```http
Authorization: Bearer <your_jwt_token>
```

---

## 🔐 1. 认证模块

### 1.1 管理端登录

**接口地址**：`POST /api/auth/login`

**请求参数**：

```json
{
  "username": "admin",
  "password": "123456"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer"
  }
}
```

---

## 🛒 2. 商品模块

### 2.1 分页查询商品列表

**接口地址**：`GET /api/goods/page`

**认证要求**：需要登录

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Long | 否 | 1 | 页码 |
| size | Long | 否 | 10 | 每页数量 |
| name | String | 否 | - | 商品名称（模糊搜索） |

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "name": "可口可乐",
        "price": 3.50,
        "stock": 100,
        "imageUrl": "/uploads/goods/001.jpg",
        "shelfId": "A-01",
        "barcode": "6920202888888"
      }
    ],
    "total": 50,
    "size": 10,
    "current": 1,
    "pages": 5
  }
}
```

### 2.2 新增商品

**接口地址**：`POST /api/goods`

**认证要求**：需要登录

**请求参数**：

```json
{
  "name": "可口可乐",
  "price": 3.50,
  "stock": 100,
  "imageUrl": "/uploads/goods/001.jpg",
  "shelfId": "A-01",
  "barcode": "6920202888888"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 商品名称 |
| price | BigDecimal | 是 | 商品价格 |
| stock | Integer | 是 | 库存数量 |
| imageUrl | String | 否 | 图片URL |
| shelfId | String | 否 | 货架编号 |
| barcode | String | 否 | 条形码 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "新增成功",
  "data": 1
}
```

### 2.3 修改商品

**接口地址**：`PUT /api/goods`

**认证要求**：需要登录

**请求参数**：

```json
{
  "id": 1,
  "name": "可口可乐（新版）",
  "price": 4.00,
  "stock": 150
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 商品ID |
| name | String | 否 | 商品名称 |
| price | BigDecimal | 否 | 商品价格 |
| stock | Integer | 否 | 库存数量 |

### 2.4 删除商品

**接口地址**：`DELETE /api/goods/{id}`

**认证要求**：需要登录

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 商品ID |

**响应示例**：

```json
{
  "code": 200,
  "msg": "删除成功",
  "data": null
}
```

---

## 🛍️ 3. 购物车模块

### 3.1 加入购物车

**接口地址**：`POST /api/cart/add`

**认证要求**：需要登录

**请求参数**：

```json
{
  "goodsId": 1,
  "quantity": 2
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| goodsId | Long | 是 | 商品ID |
| quantity | Integer | 是 | 数量 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 3.2 查询购物车列表

**接口地址**：`GET /api/cart/list`

**认证要求**：需要登录

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "goodsId": 1,
      "goodsName": "可口可乐",
      "goodsPrice": 3.50,
      "quantity": 2,
      "subtotal": 7.00,
      "goodsImage": "/uploads/goods/001.jpg"
    }
  ]
}
```

### 3.3 清空购物车

**接口地址**：`DELETE /api/cart/clear`

**认证要求**：需要登录

**响应示例**：

```json
{
  "code": 200,
  "msg": "清空成功",
  "data": null
}
```

---

## 💳 4. 订单模块

### 4.1 订单结算

**接口地址**：`POST /api/order/checkout`

**认证要求**：需要登录

**功能说明**：将当前用户购物车中的所有商品生成订单，并清空购物车。

**响应示例**：

```json
{
  "code": 200,
  "msg": "结算成功",
  "data": 20240001
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| data | Long | 生成的订单ID |

---

## ⚠️ 5. 告警模块

### 5.1 分页查询告警列表

**接口地址**：`GET /api/warning/page`

**认证要求**：需要登录

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Long | 否 | 1 | 页码 |
| size | Long | 否 | 10 | 每页数量 |
| status | Integer | 否 | - | 状态：0-未处理，1-已处理 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "goodsId": 1,
        "goodsName": "可口可乐",
        "warningType": "LOW_STOCK",
        "warningMessage": "库存不足：当前库存 5，低于阈值 10",
        "status": 0,
        "createTime": "2024-01-15 10:30:00",
        "resolveTime": null
      }
    ],
    "total": 10,
    "size": 10,
    "current": 1
  }
}
```

### 5.2 标记告警为已处理

**接口地址**：`PUT /api/warning/{id}/resolve`

**认证要求**：需要登录

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 告警ID |

**响应示例**：

```json
{
  "code": 200,
  "msg": "处理成功",
  "data": null
}
```

---

## 📷 6. 摄像头管理模块（管理端）

### 6.1 查询摄像头列表

**接口地址**：`GET /api/admin/camera/list`

**认证要求**：需要登录

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "cameraNo": "0",
      "shelfId": "A-01",
      "status": 1,
      "createTime": "2024-01-15 10:00:00"
    }
  ]
}
```

### 6.2 获取可用物理摄像头

**接口地址**：`GET /api/admin/camera/available_hardware`

**认证要求**：需要登录

**功能说明**：获取本机可用的摄像头硬件索引列表。

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [0, 1, 2]
}
```

### 6.3 绑定新增摄像头

**接口地址**：`POST /api/admin/camera`

**认证要求**：需要登录

**请求参数**：

```json
{
  "cameraNo": "0",
  "shelfId": "A-01"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| cameraNo | String | 是 | 摄像头编号/索引 |
| shelfId | String | 是 | 绑定的货架编号 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "绑定成功",
  "data": 1
}
```

### 6.4 编辑摄像头货架号

**接口地址**：`PUT /api/admin/camera`

**认证要求**：需要登录

**请求参数**：

```json
{
  "id": 1,
  "shelfId": "A-02"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | 是 | 摄像头ID |
| shelfId | String | 是 | 新的货架编号 |

### 6.5 删除摄像头

**接口地址**：`DELETE /api/admin/camera/{id}`

**认证要求**：需要登录

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 摄像头ID |

**响应示例**：

```json
{
  "code": 200,
  "msg": "删除成功",
  "data": null
}
```

### 6.6 预览摄像头画面

**接口地址**：`GET /api/admin/camera/preview/{cameraNo}`

**认证要求**：需要登录

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| cameraNo | String | 摄像头编号 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": "iVBORw0KGgoAAAANS..."
}
```

| 字段 | 说明 |
|------|------|
| data | Base64编码的图片数据 |

### 6.7 修改定时任务配置

**接口地址**：`PUT /api/admin/camera/scheduler/config`

**认证要求**：需要登录

**请求参数**：

```json
{
  "intervalMinutes": 30,
  "batchSize": 5
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| intervalMinutes | Integer | 是 | 扫描间隔（分钟） |
| batchSize | Integer | 是 | 每批处理摄像头数量 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "更新成功",
  "data": {
    "intervalMinutes": 30,
    "batchSize": 5
  }
}
```

### 6.8 查询定时任务配置

**接口地址**：`GET /api/admin/camera/scheduler/config`

**认证要求**：需要登录

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "intervalMinutes": 60,
    "batchSize": 3
  }
}
```

### 6.9 手动触发全量扫描

**接口地址**：`POST /api/admin/camera/scheduler/trigger`

**认证要求**：需要登录

**功能说明**：手动触发一次全量的摄像头扫描任务。

**响应示例**：

```json
{
  "code": 200,
  "msg": "触发成功",
  "data": null
}
```

---

## 📊 7. 管理端告警模块

### 7.1 查询所有未处理告警

**接口地址**：`GET /api/admin/warning/list`

**认证要求**：需要登录

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "goodsId": 1,
      "goodsName": "可口可乐",
      "goodsImage": "/uploads/goods/001.jpg",
      "currentStock": 5,
      "threshold": 10,
      "warningMessage": "库存不足：当前库存 5，低于阈值 10",
      "createTime": "2024-01-15 10:30:00"
    }
  ]
}
```

---

## 📤 8. 文件上传模块

### 8.1 通用文件上传

**接口地址**：`POST /api/upload`

**认证要求**：需要登录

**请求格式**：`multipart/form-data`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 上传的文件 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "上传成功",
  "data": "http://localhost:8080/uploads/abc123.jpg"
}
```

---

## 📱 9. 小程序接口模块

### 9.1 小程序登录

**接口地址**：`POST /api/applet/auth/login`

**认证要求**：不需要

**请求参数**：

```json
{
  "code": "微信授权code"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | String | 是 | 微信授权码（测试环境可使用任意字符串） |

**响应示例**：

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer"
  }
}
```

### 9.2 文本搜索商品

**接口地址**：`GET /api/applet/search/text`

**认证要求**：不需要

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| keyword | String | 是 | - | 搜索关键词 |
| k | Integer | 否 | 10 | 返回结果数量 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [1, 5, 8, 12]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| data | List<Long> | 商品ID列表 |

### 9.3 图像搜索商品

**接口地址**：`POST /api/applet/search/image`

**认证要求**：不需要

**请求参数**：

```json
{
  "imageBase64": "iVBORw0KGgoAAAANSUVORK5CYII=",
  "k": 3
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| imageBase64 | String | 是 | - | 图片Base64编码 |
| k | Integer | 否 | 3 | 返回结果数量 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [1, 5, 8]
}
```

### 9.4 批量查询商品详情

**接口地址**：`POST /api/applet/goods/listByIds`

**认证要求**：需要登录

**请求参数**：

```json
{
  "goodsIds": [1, 5, 8]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| goodsIds | List<Long> | 是 | 商品ID列表 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "可口可乐",
      "price": 3.50,
      "imageUrl": "/uploads/goods/001.jpg",
      "shelfId": "A-01"
    }
  ]
}
```

### 9.5 查询首页轮播图

**接口地址**：`GET /api/applet/home/ads`

**认证要求**：不需要

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "imageUrl": "/uploads/ads/banner1.jpg",
      "linkUrl": "/pages/goods/detail?id=1"
    }
  ]
}
```

### 9.6 推荐流接口

**接口地址**：`GET /api/applet/home/recommend`

**认证要求**：需要登录

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| offset | Integer | 否 | 0 | 偏移量 |
| k | Integer | 否 | 10 | 返回数量 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "success",
  "data": [3, 7, 12, 18, 25]
}
```

### 9.7 小程序扫码识别

**接口地址**：`POST /api/applet/scan`

**认证要求**：不需要

**请求参数**：

```json
{
  "imageBase64": "iVBORw0KGgoAAAANSUVORK5CYII=",
  "k": 3
}
```

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| imageBase64 | String | 是 | - | 摄像头拍摄图片的Base64编码 |
| k | Integer | 否 | 3 | 返回识别结果数量 |

**响应示例**：

```json
{
  "code": 200,
  "msg": "识别成功",
  "data": [
    {
      "goodsId": 1,
      "goodsName": "可口可乐",
      "goodsPrice": 3.50,
      "confidence": 0.95
    }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| goodsId | Long | 识别到的商品ID |
| goodsName | String | 商品名称 |
| goodsPrice | BigDecimal | 商品价格 |
| confidence | Double | 识别置信度（0-1） |

---

## 🗄️ 10. 数据模型

### 10.1 商品实体 (Goods)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| name | String | 商品名称 |
| price | BigDecimal | 价格 |
| stock | Integer | 库存数量 |
| imageUrl | String | 图片URL |
| shelfId | String | 货架编号 |
| barcode | String | 条形码 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

### 10.2 订单实体 (Order)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 订单ID |
| userId | Long | 用户ID |
| totalAmount | BigDecimal | 订单总金额 |
| status | Integer | 订单状态 |
| createTime | LocalDateTime | 下单时间 |

### 10.3 告警实体 (Warning)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 告警ID |
| goodsId | Long | 商品ID |
| warningType | String | 告警类型 |
| warningMessage | String | 告警信息 |
| status | Integer | 状态：0-未处理，1-已处理 |
| createTime | LocalDateTime | 创建时间 |
| resolveTime | LocalDateTime | 处理时间 |

### 10.4 摄像头实体 (Camera)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| cameraNo | String | 摄像头编号 |
| shelfId | String | 绑定货架编号 |
| status | Integer | 状态 |
| createTime | LocalDateTime | 创建时间 |

---

## 🔧 错误码对照表

| 错误码 | HTTP状态码 | 说明 |
|--------|------------|------|
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未授权 |
| 404 | 404 | 资源不存在 |
| 500 | 500 | 服务器内部错误 |
| 502 | 502 | 下游服务调用失败 |

---

## 📝 注意事项

1. **图片Base64编码**：小程序接口中的图片需要去除`data:image/jpeg;base64,`前缀
2. **JWT Token**：登录成功后返回的Token有效期为120分钟
3. **分页参数**：所有分页接口的page和size参数必须大于0
4. **文件上传**：上传文件大小限制由服务器配置决定
5. **文本搜索**：`/api/applet/search/text` 由后端 Java 语义引擎实现，不经 AI 服务；扫码识别 `/api/applet/scan` 需 AI 服务可达，否则返回 502。

---

## 🧪 测试与排错

免鉴权接口仅：`/api/auth/login`、`/api/applet/auth/login`、`/api/applet/home/ads`；其余需 `Authorization: Bearer <token>`。

推荐联调顺序：

- 管理端：登录 → 新增商品 → 加购物车 → 结算（触发库存/告警）→ 查/处理告警 → 摄像头与巡检接口。
- 小程序：小程序登录 → 首页轮播 → 推荐流 → 文本搜索 → 扫码识别 → 批量查询详情。

快速验证：

```bash
curl -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

| 现象 | 排查 |
|------|------|
| 401 | 检查 `Authorization: Bearer <token>` 是否带上、是否已登录 |
| 404（PUT/DELETE 商品） | `goodsId` 已删或过期；重新新增刷新，勿对 ID 做 +1/-1 |
| 502（扫码/识别） | AI 服务未运行或 `AI_SERVICE_URL` 不可达，确认 `http://127.0.0.1:8000` |
| 摄像头预览失败 | 本机无可用摄像头或 `cameraNo` 为空，先查 `available_hardware` |

> 历史 Postman 用例文档（`api.md`）已并入本节，原件删除。

