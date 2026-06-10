# 零售系统 Postman 测试用例

## 1. 你当前两个报错的原因

1. 你把登录 JSON 发到了 `POST http://127.0.0.1:8080/`。
- 这是根路径，不是登录接口。
- 正确登录地址是：`POST http://127.0.0.1:8080/api/auth/login`。

2. 你把请求发到了 `POST http://localhost:5173/` 返回 404。
- `5173` 是前端开发服务器地址，不是后端 API 根地址。
- 如果要经前端代理调用，应该是 `POST http://localhost:5173/api/auth/login`（且前端 dev server 必须正在运行）。
- Postman 推荐直接测后端：`http://127.0.0.1:8080`。

## 2. Postman 环境变量建议

创建环境变量：

- `baseUrl` = `http://127.0.0.1:8080`
- `token` = 空
- `appletToken` = 空
- `goodsId` = 空
- `warningId` = 空
- `orderId` = 空
- `cameraId` = 空
- `cameraNo` = 空
- `hwCameraNo` = 空
- `recommendIds` = 空
- `sampleImageBase64` = `/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAIBAQEBAQIBAQECAgICAgQDAgICAgUEBAMEBgUGBgYFBgYGBwkIBgcJBwYGCAsICQoKCgoKBggLDAsKDAkKCgr/2wBDAQICAgICAgUDAwUKBwYHCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgr/wAARCAAgACADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD+f+iiigAooooAKKKKACiiigD/2Q==`

也建议创建同名 Collection Variables（尤其在 VS Code Postman 插件中更稳）：

- `token` = 空
- `appletToken` = 空
- `goodsId` = 空
- `warningId` = 空
- `orderId` = 空
- `cameraId` = 空
- `cameraNo` = 空
- `hwCameraNo` = 空
- `recommendIds` = 空
- `sampleImageBase64` = 同环境变量

说明：

- `goodsId` 必须来自“新增商品”接口返回值。
- 不要手工对 `goodsId` 做 `+1/-1`，ID 可能不连续（删除、并发插入后会跳号）。
- 如果你删除了测试商品，再用旧 `goodsId` 去更新/删除，会返回 404，这是正常表现。
- `token` 建议用于管理端接口，`appletToken` 用于小程序接口。
- 当前免鉴权接口只有：`/api/auth/login`、`/api/applet/auth/login`、`/api/applet/home/ads`。

## 3. 通用 Tests 脚本模板

可放在 Collection 级 Tests 中：

```javascript
pm.test('HTTP status is 200', function () {
  pm.expect(pm.response.code).to.equal(200);
});

pm.test('Response code field is 200', function () {
  const body = pm.response.json();
  pm.expect(body).to.have.property('code');
  pm.expect(body.code).to.equal(200);
});
```

## 4. 接口测试清单

### 4.1 登录接口

- 方法：`POST`
- URL：`{{baseUrl}}/api/auth/login`
- Headers：`Content-Type: application/json`
- Body（raw/json）：

```json
{
  "username": "admin",
  "password": "123456"
}
```

Tests：

```javascript
pm.test('登录成功', function () {
  pm.expect(pm.response.code).to.equal(200);
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.have.property('token');
  pm.environment.set('token', body.data.token);
});
```

### 4.2 文件上传

- 方法：`POST`
- URL：`{{baseUrl}}/api/upload`
- Headers：
  - `Authorization: Bearer {{token}}`
- Body：`form-data`
  - `file` (File)

Tests：

```javascript
pm.test('上传成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.include('/uploads/');
});
```

### 4.3 商品分页

- 方法：`GET`
- URL：`{{baseUrl}}/api/goods/page?page=1&size=10&name=`
- Headers：`Authorization: Bearer {{token}}`

Tests：

```javascript
pm.test('分页查询成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.have.property('records');
});
```

### 4.4 新增商品

- 方法：`POST`
- URL：`{{baseUrl}}/api/goods`
- Headers：
  - `Content-Type: application/json`
  - `Authorization: Bearer {{token}}`
- Body：

```json
{
  "name": "Postman测试商品",
  "price": 29.9,
  "stock": 12,
  "shelfId": "P-01",
  "imageUrl": "http://127.0.0.1:8080/uploads/demo.jpg"
}
```

Tests：

```javascript
pm.test('新增商品成功并保存 goodsId', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.a('number');
  pm.collectionVariables.set('goodsId', body.data);
  pm.environment.set('goodsId', body.data);
});
```

### 4.5 修改商品

- 方法：`PUT`
- URL：`{{baseUrl}}/api/goods`
- Headers：
  - `Content-Type: application/json`
  - `Authorization: Bearer {{token}}`
- Body：

```json
{
  "id": {{goodsId}},
  "name": "Postman测试商品-更新",
  "price": 35.5,
  "stock": 11,
  "shelfId": "P-02",
  "imageUrl": "http://127.0.0.1:8080/uploads/demo.jpg"
}
```

Pre-request Script（建议加在本请求）：

```javascript
const goodsId =
  pm.environment.get('goodsId') ||
  pm.collectionVariables.get('goodsId') ||
  pm.globals.get('goodsId');
if (!goodsId) {
  throw new Error('goodsId 为空：请先执行 4.4 新增商品接口，或手动设置变量 goodsId');
}
pm.variables.set('goodsId', goodsId);
```

Tests：

```javascript
pm.test('修改商品成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
});
```

### 4.6 删除商品

- 方法：`DELETE`
- URL：`{{baseUrl}}/api/goods/{{goodsId}}`
- Headers：`Authorization: Bearer {{token}}`

说明：删除成功后，建议手工清空环境变量 `goodsId`，避免后续误用旧 ID。

Tests：

```javascript
pm.test('删除商品成功并清空 goodsId', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.environment.unset('goodsId');
  pm.collectionVariables.unset('goodsId');
});
```

### 4.7 购物车：加入/修改

- 方法：`POST`
- URL：`{{baseUrl}}/api/cart/add`
- Headers：
  - `Content-Type: application/json`
  - `Authorization: Bearer {{token}}`
- Body：

```json
{
  "goodsId": {{goodsId}},
  "quantity": 2
}
```

Pre-request Script（建议加在本请求）：

```javascript
const goodsId =
  pm.environment.get('goodsId') ||
  pm.collectionVariables.get('goodsId') ||
  pm.globals.get('goodsId');
if (!goodsId) {
  throw new Error('goodsId 为空：请先执行 4.4 新增商品接口，或手动设置变量 goodsId');
}
pm.variables.set('goodsId', goodsId);
```

### 4.8 购物车：列表

- 方法：`GET`
- URL：`{{baseUrl}}/api/cart/list`
- Headers：`Authorization: Bearer {{token}}`

Tests：

```javascript
pm.test('购物车列表查询成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.an('array');
});
```

### 4.9 购物车：清空

- 方法：`DELETE`
- URL：`{{baseUrl}}/api/cart/clear`
- Headers：`Authorization: Bearer {{token}}`

Tests：

```javascript
pm.test('购物车清空成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
});
```

### 4.10 订单结算

- 方法：`POST`
- URL：`{{baseUrl}}/api/order/checkout`
- Headers：`Authorization: Bearer {{token}}`

Tests：

```javascript
pm.test('结算成功并保存 orderId', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.a('number');
  pm.environment.set('orderId', body.data);
});
```

### 4.11 库存告警分页

- 方法：`GET`
- URL：`{{baseUrl}}/api/warning/page?page=1&size=10&status=0`
- Headers：`Authorization: Bearer {{token}}`

Tests（提取首条告警 ID）：

```javascript
pm.test('告警查询成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.have.property('records');
  if (body.data.records.length > 0) {
    pm.environment.set('warningId', body.data.records[0].id);
  }
});
```

### 4.12 告警处理

- 方法：`PUT`
- URL：`{{baseUrl}}/api/warning/{{warningId}}/resolve`
- Headers：`Authorization: Bearer {{token}}`

Tests：

```javascript
pm.test('告警处理成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
});
```

### 4.13 管理端未处理告警列表

- 方法：`GET`
- URL：`{{baseUrl}}/api/admin/warning/list`
- Headers：`Authorization: Bearer {{token}}`

Tests：

```javascript
pm.test('管理端未处理告警查询成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.an('array');
});
```

### 4.14 摄像头列表

- 方法：`GET`
- URL：`{{baseUrl}}/api/admin/camera/list`
- Headers：`Authorization: Bearer {{token}}`

Tests：

```javascript
pm.test('摄像头列表查询成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.an('array');
  if (body.data.length > 0) {
    pm.environment.set('cameraId', body.data[0].id);
    pm.environment.set('cameraNo', body.data[0].cameraNo);
  }
});
```

### 4.15 获取可用物理摄像头

- 方法：`GET`
- URL：`{{baseUrl}}/api/admin/camera/available_hardware`
- Headers：`Authorization: Bearer {{token}}`

Tests：

```javascript
pm.test('可用摄像头索引查询成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.an('array');
  if (body.data.length > 0) {
    pm.environment.set('hwCameraNo', String(body.data[0]));
  }
});
```

### 4.16 新增绑定摄像头

- 方法：`POST`
- URL：`{{baseUrl}}/api/admin/camera`
- Headers：
  - `Content-Type: application/json`
  - `Authorization: Bearer {{token}}`

Pre-request Script：

```javascript
const cameraNo = `C${Date.now()}`;
pm.variables.set('cameraNo', cameraNo);
```

Body：

```json
{
  "camera_no": "{{cameraNo}}",
  "shelf_id": "SHELF-POSTMAN"
}
```

Tests：

```javascript
pm.test('新增绑定摄像头成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.a('number');
  pm.environment.set('cameraId', body.data);
  pm.environment.set('cameraNo', pm.variables.get('cameraNo'));
});
```

### 4.17 编辑摄像头货架绑定

- 方法：`PUT`
- URL：`{{baseUrl}}/api/admin/camera`
- Headers：
  - `Content-Type: application/json`
  - `Authorization: Bearer {{token}}`

Body：

```json
{
  "id": {{cameraId}},
  "shelf_id": "SHELF-POSTMAN-UPDATED"
}
```

Tests：

```javascript
pm.test('编辑摄像头绑定成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
});
```

### 4.18 删除摄像头

- 方法：`DELETE`
- URL：`{{baseUrl}}/api/admin/camera/{{cameraId}}`
- Headers：`Authorization: Bearer {{token}}`

Tests：

```javascript
pm.test('删除摄像头成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.environment.unset('cameraId');
});
```

### 4.19 摄像头预览（单帧）

- 方法：`GET`
- URL：`{{baseUrl}}/api/admin/camera/preview/{{hwCameraNo}}`
- Headers：`Authorization: Bearer {{token}}`

说明：若设备无可用摄像头，`hwCameraNo` 为空时此接口可跳过。

Tests：

```javascript
pm.test('摄像头预览返回成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.a('string');
  pm.expect(body.data.length).to.be.greaterThan(50);
});
```

### 4.20 查询巡检调度配置

- 方法：`GET`
- URL：`{{baseUrl}}/api/admin/camera/scheduler/config`
- Headers：`Authorization: Bearer {{token}}`

Tests：

```javascript
pm.test('查询调度配置成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.have.property('intervalMinutes');
  pm.expect(body.data).to.have.property('batchSize');
});
```

### 4.21 更新巡检调度配置

- 方法：`PUT`
- URL：`{{baseUrl}}/api/admin/camera/scheduler/config`
- Headers：
  - `Content-Type: application/json`
  - `Authorization: Bearer {{token}}`
- Body：

```json
{
  "intervalMinutes": 5,
  "batchSize": 10
}
```

Tests：

```javascript
pm.test('更新调度配置成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.have.property('intervalMinutes');
  pm.expect(body.data).to.have.property('batchSize');
});
```

### 4.22 手动触发全量巡检

- 方法：`POST`
- URL：`{{baseUrl}}/api/admin/camera/scheduler/trigger`
- Headers：`Authorization: Bearer {{token}}`

Tests：

```javascript
pm.test('手动触发巡检成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
});
```

### 4.23 小程序登录

- 方法：`POST`
- URL：`{{baseUrl}}/api/applet/auth/login`
- Headers：`Content-Type: application/json`
- Body：

```json
{
  "code": "test_code"
}
```

Tests：

```javascript
pm.test('小程序登录成功并保存 appletToken', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.have.property('token');
  pm.environment.set('appletToken', body.data.token);
});
```

### 4.24 小程序首页轮播

- 方法：`GET`
- URL：`{{baseUrl}}/api/applet/home/ads`
- Headers：无

Tests：

```javascript
pm.test('小程序轮播接口成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.an('array');
});
```

### 4.25 小程序推荐流

- 方法：`GET`
- URL：`{{baseUrl}}/api/applet/home/recommend?offset=0&k=10`
- Headers：`Authorization: Bearer {{appletToken}}`

Tests：

```javascript
pm.test('小程序推荐流返回 200 或 204', function () {
  const body = pm.response.json();
  pm.expect([200, 204]).to.include(body.code);
  if (body.code === 200 && Array.isArray(body.data) && body.data.length > 0) {
    pm.environment.set('recommendIds', body.data.join(','));
  }
});
```

### 4.26 小程序扫码识别（返回商品详情）

- 方法：`POST`
- URL：`{{baseUrl}}/api/applet/scan`
- Headers：
  - `Content-Type: application/json`
  - `Authorization: Bearer {{appletToken}}`
- Body：

```json
{
  "image_base64": "{{sampleImageBase64}}",
  "k": 3
}
```

Tests：

```javascript
pm.test('小程序扫码识别成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.an('array');
});
```

### 4.27 小程序文本搜索（返回 ID 列表）

- 方法：`GET`
- URL：`{{baseUrl}}/api/applet/search/text?keyword=milk&k=5`
- Headers：`Authorization: Bearer {{appletToken}}`

Tests：

```javascript
pm.test('小程序文本搜索成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.an('array');
  if (body.data.length > 0) {
    pm.environment.set('recommendIds', body.data.join(','));
  }
});
```

### 4.28 小程序图像搜索（返回 ID 列表）

- 方法：`POST`
- URL：`{{baseUrl}}/api/applet/search/image`
- Headers：
  - `Content-Type: application/json`
  - `Authorization: Bearer {{appletToken}}`
- Body：

```json
{
  "image_base64": "{{sampleImageBase64}}",
  "k": 3
}
```

Tests：

```javascript
pm.test('小程序图像搜索成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.an('array');
});
```

### 4.29 小程序按 ID 批量查询商品详情

- 方法：`POST`
- URL：`{{baseUrl}}/api/applet/goods/listByIds`
- Headers：
  - `Content-Type: application/json`
  - `Authorization: Bearer {{appletToken}}`

Pre-request Script：

```javascript
const recommendIds = pm.environment.get('recommendIds');
if (recommendIds) {
  pm.variables.set('recommendIdsJson', `[${recommendIds}]`);
} else {
  pm.variables.set('recommendIdsJson', '[1,2,3]');
}
```

Body（raw/json）：

```json
{{recommendIdsJson}}
```

Tests：

```javascript
pm.test('小程序商品批量查询成功', function () {
  const body = pm.response.json();
  pm.expect(body.code).to.equal(200);
  pm.expect(body.data).to.be.an('array');
});
```

## 5. 推荐测试顺序

### 5.1 管理端链路

1. 4.1 登录（保存 token）
2. 4.4 新增商品（保存 goodsId）
3. 4.7 加入购物车
4. 4.10 订单结算（触发库存与告警）
5. 4.11 查询告警
6. 4.12 标记告警已处理
7. 4.14~4.22 摄像头管理和巡检相关接口
8. 4.6 删除测试商品

### 5.2 小程序链路

1. 4.23 小程序登录（保存 appletToken）
2. 4.24 首页轮播
3. 4.25 推荐流（保存 recommendIds）
4. 4.27 文本搜索（可刷新 recommendIds）
5. 4.28 图像搜索
6. 4.29 按 ID 批量查询商品详情
7. 4.26 扫码识别（详情版接口）

## 6. 快速排错

1. 如果返回 `401`：检查 `Authorization` 是否为 `Bearer {{token}}`。
2. 如果返回 `404`：检查 URL 路径是否含 `/api/...`。
3. 如果返回 `500`：优先看后端控制台日志（Run: RetailServerApplication）。
4. 如果调 `localhost:5173`：确认这是前端地址，不是后端根 API 地址。
5. 登录接口必须使用 `Body -> raw -> JSON`，并确保 Header 是 `Content-Type: application/json`。
6. 登录接口请使用 `{{baseUrl}}/api/auth/login`，不要发到 `{{baseUrl}}/`。
7. 可先用 cURL 对照验证：

```bash
curl -X POST "http://127.0.0.1:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```
8. 如果 `PUT /api/goods` 或 `DELETE /api/goods/{{goodsId}}` 返回 404：
   - 通常是 `goodsId` 过期（商品已删）或未设置。
   - 重新执行“4.4 新增商品”刷新 `goodsId`，不要对 ID 做 `+1/-1`。
9. 如果提示 `goodsId 为空`：
  - 确认已先执行 4.4，且响应为 200。
  - 在 Postman 的 Environment 或 Collection Variables 里检查 `goodsId` 是否有值。
  - 可临时手工设置一个存在的商品 ID（例如先调用 4.3 分页接口查看已有 ID）。
10. 如果小程序接口返回 `401`：
  - 检查请求头是否使用 `Authorization: Bearer {{appletToken}}`。
  - 确认已先执行 4.23 保存了 `appletToken`。
11. 如果文本/图像搜索返回 `502`：
  - 说明 Java 中转调用 Python 失败。
  - 检查 `retail-ai` 是否运行在 `http://127.0.0.1:8000`。
12. 如果 `4.19 摄像头预览` 失败：
  - 常见原因是本机没有可用摄像头或 `hwCameraNo` 未设置。
  - 先执行 4.15，确认返回数组非空。
13. 如果 `4.25 推荐流` 返回 `code=204`：
  - 表示当前推荐流无更多数据（可能商品库为空或 offset 已超出）。
  - 先执行 4.4 新增若干商品后重试。
