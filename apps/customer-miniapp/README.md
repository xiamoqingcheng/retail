# customer-miniapp · 顾客端小程序

微信小程序原生（WXML + WXSS + JS）。功能：登录、商品搜索、扫码识别、购物车、下单结算、首页推荐流。

## 目录

```
app.js / app.json / app.wxss   # 应用入口、全局配置、全局样式
project.config.json            # 微信开发者工具项目配置（含 appid）
utils/   request.js            # HTTP 封装与 API 基址（DEFAULT_BASE_URL）
pages/   index/login/search/scan/cart/order/user/...
assets/  icons/ goods/         # 图标与商品图
```

## 运行

1. 微信开发者工具 →「导入项目」→ 选择本目录（`apps/customer-miniapp`）。
2. 填入 AppID（或测试号），勾选「不校验合法域名」（开发阶段）。
3. 编译运行；真机调试改 `utils/request.js` 的 `DEFAULT_BASE_URL` 为本机局域网 IP 或 ngrok 地址。

## 后端对接

所有请求走后端 API（默认 `http://127.0.0.1:8080/api`），接口契约见 [../../docs/api.md](../../docs/api.md)：
登录 `/api/applet/auth/login`、文本搜索 `/api/applet/search/text`、扫码识别 `/api/applet/scan`、
批量详情 `/api/applet/goods/listByIds`、首页推荐 `/api/applet/home/recommend`。

真机调试与生产发布步骤见 [../../docs/deployment.md](../../docs/deployment.md)。
