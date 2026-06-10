# 零售物品智能识别系统 - 微信小程序

## 项目简介

这是一个基于微信小程序的零售物品智能识别系统客户端，提供用户扫码认证、物品查询、视频识别、购物车管理和订单结算等功能。

## 功能特性

### 1. 用户扫码与身份认证
- 微信扫码登录/注册
- 用户身份验证
- 用户信息管理
- 记住密码功能

### 2. 物品查询与推荐
- 商品搜索功能
- 热门搜索词推荐
- 搜索历史记录
- 商品分类浏览
- 个性化推荐

### 3. 购物车管理与结算
- 添加/删除商品
- 修改商品数量
- 商品选择/取消选择
- 订单确认
- 多种支付方式

### 4. 视频识别与物品定位
- 实时视频流识别
- 物品自动定位
- 商品信息展示
- 批量添加购物车

## 项目结构

```
├── app.js                 # 应用入口
├── app.json               # 应用配置
├── app.wxss               # 全局样式
├── project.config.json    # 项目配置
├── sitemap.json           # 站点地图
├── utils/
│   ├── request.js         # HTTP请求封装
│   └── utils.js           # 工具函数
├── pages/
│   ├── index/             # 首页
│   ├── login/             # 登录页
│   ├── search/            # 查询页
│   ├── cart/              # 购物车页
│   ├── scan/              # 视频识别页
│   ├── user/              # 用户中心
│   └── order/             # 订单页
└── assets/
    ├── icons/             # 图标资源
    └── goods/              # 商品图片资源
```

## 页面说明

### 首页 (pages/index)
- 展示用户信息
- 功能入口导航
- 推荐商品展示
- 快捷操作列表

### 登录页 (pages/login)
- 用户名密码登录
- 微信一键登录
- 记住密码
- 用户协议同意

### 查询页 (pages/search)
- 搜索商品
- 热门搜索
- 搜索历史
- 商品分类
- 推荐商品

### 购物车页 (pages/cart)
- 商品列表展示
- 数量修改
- 商品选择
- 价格计算
- 去结算

### 视频识别页 (pages/scan)
- 相机权限获取
- 实时视频流
- 物品识别
- 识别结果展示
- 加入购物车

### 用户中心 (pages/user)
- 用户信息展示
- 账户余额/积分
- 订单管理入口
- 功能菜单

### 订单页 (pages/order)
- 订单列表
- 订单状态筛选
- 订单详情
- 订单支付

## 使用说明

### 开发环境
- 微信开发者工具
- 基础库版本 >= 2.19.0

### 安装步骤
1. 克隆项目代码
2. 使用微信开发者工具导入项目
3. 填写 app.json 中的 appid
4. 编译运行

### 配置说明

#### 1. 修改 AppID
在 `project.config.json` 中修改 `appid` 为您的小程序 AppID

#### 2. 配置服务器地址
在 `app.js` 的 `globalData` 中修改 `baseUrl` 为您的后端服务器地址

#### 3. 添加图标资源
在 `assets/icons/` 和 `assets/goods/` 目录下添加相应的图标和商品图片资源

### API 接口说明

#### 用户相关
- `POST /user/login` - 用户登录
- `POST /user/logout` - 用户登出
- `GET /user/info` - 获取用户信息

#### 商品相关
- `GET /goods/list` - 获取商品列表
- `GET /goods/detail/:id` - 获取商品详情
- `GET /goods/search` - 搜索商品
- `GET /goods/recommend` - 获取推荐商品
- `GET /goods/categories` - 获取商品分类

#### 订单相关
- `POST /order/create` - 创建订单
- `GET /order/list` - 获取订单列表
- `GET /order/detail/:id` - 获取订单详情
- `POST /order/pay` - 支付订单

#### 视频识别相关
- `POST /identify/goods` - 识别商品
- `GET /identify/location/:barcode` - 获取商品位置

## 功能接口预留

以下接口在当前版本中预留，待数据库实现后进一步完善：

```javascript
// 物品查询接口
GET /api/goods/query?keyword=xxx&category=xxx

// 物品推荐接口
GET /api/goods/recommend?user_id=xxx

// 视频识别接口
POST /api/identify/video
```

## 注意事项

1. **相机权限**: 视频识别功能需要用户授权相机权限
2. **用户登录**: 部分功能需要用户登录后才能使用
3. **网络请求**: 需要后端服务器支持，当前版本使用模拟数据
4. **图标资源**: 请根据实际需求添加图标和商品图片资源

## 开发计划

- [x] 用户扫码与身份认证
- [x] 物品查询与推荐
- [x] 购物车管理与结算
- [x] 视频识别与物品定位
- [ ] 真实 API 接口对接
- [ ] 数据库集成
- [ ] 支付功能完善
- [ ] 订单物流追踪

## 技术栈

- 微信小程序框架
- WXML + WXSS + JavaScript
- 原生 API

## 联系方式

如有问题或建议，请联系开发团队。

## 许可证

MIT License
