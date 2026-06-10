# 商品识别 API 接口定义

## 接口概述

当用户上传图片后，前端会将图片发送给后端进行商品识别。后端会返回识别到的所有商品信息，包括商品ID和位置信息。

---

## 1. 商品识别接口

### 请求信息

- **URL**: `https://your-api-domain.com/api/v1/goods/detect`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`

### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| image | File | 是 | 用户上传的商品图片文件 |
| user_id | String | 是 | 用户ID（从登录态获取） |

### 请求示例

```javascript
wx.chooseImage({
  success: (res) => {
    const tempFilePath = res.tempFilePaths[0];
    
    wx.uploadFile({
      url: 'https://your-api-domain.com/api/v1/goods/detect',
      filePath: tempFilePath,
      name: 'image',
      formData: {
        user_id: app.globalData.userInfo.id
      },
      success: (res) => {
        const data = JSON.parse(res.data);
        if (data.code === 200) {
          // 处理识别结果
          this.processDetectionResult(data.data);
        }
      }
    });
  }
});
```

---

## 2. 后端响应格式

### 响应数据结构

```json
{
  "code": 200,
  "message": "识别成功",
  "data": {
    "image_id": "IMG20240101000001",
    "detect_count": 4,
    "items": [
      {
        "id": "G001",
        "name": "可口可乐",
        "brand": "可口可乐公司",
        "price": 3.00,
        "location": "A区-3-15",
        "image": "/goods/cola.png",
        "centerX": 0.25,
        "centerY": 0.3,
        "width": 0.15,
        "height": 0.2,
        "confidence": 0.95
      }
    ]
  }
}
```

### 响应参数说明

| 参数名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 状态码，200表示成功 |
| message | String | 响应消息 |
| data | Object | 识别结果数据 |
| data.image_id | String | 图片唯一标识 |
| data.detect_count | Integer | 识别到的商品数量 |
| data.items | Array | 商品数组 |
| items[].id | String | 商品唯一ID |
| items[].name | String | 商品名称 |
| items[].brand | String | 商品品牌 |
| items[].price | Float | 商品价格 |
| items[].location | String | 商品位置（货架位置） |
| items[].image | String | 商品图片URL |
| items[].centerX | Float | 商品中心点X坐标（0-1，相对值） |
| items[].centerY | Float | 商品中心点Y坐标（0-1，相对值） |
| items[].width | Float | 商品宽度（0-1，相对值） |
| items[].height | Float | 商品高度（0-1，相对值） |
| items[].confidence | Float | 识别置信度（0-1） |

---

## 3. 位置坐标计算说明

后端返回的位置信息是相对于图片尺寸的**比例值**（0-1之间），前端需要根据实际图片显示尺寸来计算像素位置。

### 计算公式

```
boxLeft = (centerX - width / 2) × imageWidth
boxTop = (centerY - height / 2) × imageHeight
boxWidth = width × imageWidth
boxHeight = height × imageHeight
```

### 示例

假设图片实际显示尺寸为 750×500 像素，后端返回：
- centerX: 0.25
- centerY: 0.3
- width: 0.15
- height: 0.2

则方框位置为：
- boxLeft = (0.25 - 0.075) × 750 = 131.25px
- boxTop = (0.3 - 0.1) × 500 = 100px
- boxWidth = 0.15 × 750 = 112.5px
- boxHeight = 0.2 × 500 = 100px

---

## 4. 错误码说明

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 200 | 识别成功 | 正常处理识别结果 |
| 400 | 请求参数错误 | 检查请求参数是否正确 |
| 401 | 未授权 | 跳转到登录页面 |
| 413 | 图片过大 | 提示用户压缩图片后重试 |
| 415 | 不支持的图片格式 | 提示用户上传 JPG/PNG 格式图片 |
| 500 | 服务器内部错误 | 提示用户稍后重试 |
| 501 | 识别失败 | 提示用户图片不清晰，尝试其他角度 |

### 错误响应示例

```json
{
  "code": 413,
  "message": "图片文件过大，请上传小于5MB的图片",
  "data": null
}
```

---

## 5. 当前实现状态

### ✅ 已完成（Mock版本）

当前代码使用模拟数据实现了完整的商品识别流程：

1. **图片上传** - 支持拍照和相册选择
2. **识别模拟** - 延迟1.5秒模拟后端处理
3. **方框标注** - 在图片上显示绿色方框标注商品位置
4. **点击交互** - 点击方框显示商品详情弹窗
5. **添加购物车** - 支持单个或批量添加商品到购物车

### 🔧 待接入

当后端API准备好后，只需修改 `pages/scan/scan.js` 中的 `mockBackendDetect` 方法为真实接口调用即可。

---

## 6. 代码接入指南

### 替换Mock接口

1. 打开 `pages/scan/scan.js`
2. 找到 `mockBackendDetect` 方法
3. 将其中的模拟数据替换为真实API调用

### 参考代码

```javascript
// 真实API调用示例
uploadAndDetect(imagePath) {
  this.setData({
    uploadedImage: imagePath,
    isLoading: true
  });

  wx.getImageInfo({
    src: imagePath,
    success: (res) => {
      this.setData({
        imageWidth: res.width,
        imageHeight: res.height
      });

      // 调用真实API
      wx.uploadFile({
        url: 'https://your-api-domain.com/api/v1/goods/detect',
        filePath: imagePath,
        name: 'image',
        success: (res) => {
          const response = JSON.parse(res.data);
          if (response.code === 200) {
            this.processBackendResponse(response.data);
          } else {
            wx.showToast({
              title: response.message,
              icon: 'none'
            });
          }
        },
        fail: () => {
          wx.showToast({
            title: '网络请求失败',
            icon: 'none'
          });
        },
        complete: () => {
          this.setData({ isLoading: false });
        }
      });
    }
  });
}
```

---

## 7. 注意事项

1. **图片尺寸** - 建议上传小于2MB的图片以提高上传速度和识别速度
2. **图片质量** - 商品图片应清晰，避免过曝或过暗
3. **多商品识别** - 一次最多识别20个商品
4. **位置坐标** - 所有位置参数都是相对于图片尺寸的比例值（0-1）
5. **置信度** - 建议只显示置信度大于0.7的商品

---

## 8. 联系方式

如有API相关问题，请联系后端开发团队。
