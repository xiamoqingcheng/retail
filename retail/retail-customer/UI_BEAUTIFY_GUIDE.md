# 智能零售小程序 · 界面美化方案（京东白色简约风）

> 设计理念：**白色为主、红色点缀**。参考京东 App 的克制用色哲学——大面积留白营造高端感，仅在价格、徽标、主按钮等关键触点使用品牌红，让用户视线自然聚焦于商品与操作。

---

## 一、设计风格定义

### 1.1 京东风格核心特征

| 特征 | 说明 |
| ---- | ---- |
| 白色主基调 | 页面背景纯白或极浅灰 `#F5F5F5`，卡片纯白无彩色渐变 |
| 红色极度克制 | 仅用于：价格文字、主操作按钮、选中态勾选、角标徽章 |
| 扁平无阴影 | 卡片用 1px 细线分割或微弱阴影，不用浓重投影 |
| 直角 / 小圆角 | 卡片 `8~12rpx`，按钮 `8rpx` 直角或小圆角，拒绝大胶囊 |
| 黑灰文字层级 | 标题纯黑 `#333`，正文深灰 `#666`，辅助浅灰 `#999` |
| 分割线代替阴影 | 使用 `1rpx solid #F0F0F0` 分隔区域 |
| 图标线性简洁 | 单色线性图标，不使用彩色填充 |

### 1.2 色板对比

```
当前 (暖橙红渐变)                京东简约白 (目标)
─────────────────              ─────────────────
品牌色  #FF5040 → #FF7849       品牌色  #E4393C (京东红, 仅点缀)
页面背景 #F5F7FA                 页面背景 #F5F5F5
卡片    白 + 浓阴影              卡片    纯白 + 1px边线
头部    大面积渐变色块            头部    纯白背景
按钮    渐变胶囊 + 发光阴影       按钮    纯红直角 / 红色描边
价格    #F53F3F                  价格    #E4393C
文字    4级蓝灰色阶              文字    #333 / #666 / #999
```

---

## 二、设计令牌体系（需更新 app.wxss）

```css
page {
  /* ── 品牌 ── */
  --brand: #E4393C;                              /* 京东红，仅点缀 */
  --brand-bg: #FFF1F0;                           /* 极浅红底 */
  --brand-gradient: none;                        /* 不使用渐变 */

  /* ── 功能色 ── */
  --success: #52C41A;
  --warning: #FAAD14;
  --danger: #E4393C;

  /* ── 背景 ── */
  --bg-page: #F5F5F5;
  --bg-card: #FFFFFF;

  /* ── 文字 ── */
  --text-title: #333333;
  --text-body: #666666;
  --text-secondary: #999999;
  --text-placeholder: #CCCCCC;

  /* ── 边框 ── */
  --border: #F0F0F0;
  --border-dark: #E8E8E8;

  /* ── 圆角（京东偏小直角） ── */
  --radius-sm: 8rpx;
  --radius-md: 12rpx;
  --radius-lg: 12rpx;                           /* 不再使用大圆角 */
  --radius-pill: 8rpx;                           /* 按钮也用小圆角 */

  /* ── 阴影（极轻或不用） ── */
  --shadow-sm: 0 1rpx 4rpx rgba(0,0,0,0.03);
  --shadow-md: 0 2rpx 8rpx rgba(0,0,0,0.04);
  --shadow-lg: 0 2rpx 8rpx rgba(0,0,0,0.04);

  font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: var(--text-body);
  background-color: var(--bg-page);
}
```

---

## 三、各页面改造细节

### 3.1 全局组件 `app.wxss`

| 组件 | 当前样式 | 京东白色风格 |
| ---- | -------- | ------------ |
| `.btn-primary` | 渐变背景 + 胶囊 + 发光阴影 | 纯色 `#E4393C` + 小圆角 `8rpx` + 无阴影 |
| `.btn-primary:active` | `scale(0.97)` | `opacity: 0.85` |
| `.card` | `radius 28rpx` + `shadow-md` | `radius 12rpx` + `border 1rpx solid #F0F0F0` |
| `.price` | `#F53F3F` + 800 粗 | `#E4393C` + 700 粗 |
| `.tag` | 渐变品牌底 | `#FFF1F0` 底 + `#E4393C` 字 |

**按钮代码**：

```css
.btn-primary {
  background-color: #E4393C;
  color: #ffffff;
  border-radius: 8rpx;
  padding: 22rpx 48rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  box-shadow: none;
}
.btn-primary::after { border: none; }
.btn-primary:active { opacity: 0.85; }

/* 次要按钮：红色描边 */
.btn-outline {
  background-color: #ffffff;
  color: #E4393C;
  border: 2rpx solid #E4393C;
  border-radius: 8rpx;
  padding: 22rpx 48rpx;
  font-size: 30rpx;
}
```

**卡片代码**：

```css
.card {
  background-color: #ffffff;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  border: 1rpx solid #F0F0F0;
  box-shadow: none;
}
```

### 3.2 导航栏 `app.json`

```json
{
  "window": {
    "navigationBarBackgroundColor": "#FFFFFF",
    "navigationBarTitleText": "智能零售",
    "navigationBarTextStyle": "black",
    "backgroundColor": "#F5F5F5"
  },
  "tabBar": {
    "color": "#999999",
    "selectedColor": "#E4393C",
    "backgroundColor": "#FFFFFF",
    "borderStyle": "white"
  }
}
```

### 3.3 首页 `index`

| 区域 | 当前 | 京东风格 |
| ---- | ---- | -------- |
| 顶部 Hero | 大面积橙红渐变 + 装饰圆 | **纯白背景**，仅 Logo + 标题，干净简约 |
| 用户卡片 | 浮动白卡 + 重阴影 | 白色区块 + 底部细线分隔，无阴影 |
| 功能网格 | 彩色渐变图标背景 | **白底 + 线性图标**，图标下方文字，无背景色 |
| 登录引导 | 渐变按钮 | 纯红按钮 `#E4393C` + 小圆角 |

**首页结构参考（京东风）**：

```
┌─────────────────────────────┐
│  Logo   智能零售    [消息]   │  ← 纯白顶栏
├─────────────────────────────┤
│ ┌─────────────────────────┐ │
│ │    Banner 轮播广告       │ │  ← 圆角 12rpx
│ └─────────────────────────┘ │
│                             │
│  🔍识别    📦查询    🛒购物车  │  ← 纯白底、线性图标、无背景色块
│                             │
│ ─ ─ ─ ─ 1px 分割线 ─ ─ ─ ─ │
│                             │
│  为你推荐              更多 > │  ← 区域标题
│ ┌──────┐ ┌──────┐          │
│ │ 商品  │ │ 商品  │          │  ← 双列商品，白底卡片
│ │ ¥19.9│ │ ¥29.9│          │
│ └──────┘ └──────┘          │
└─────────────────────────────┘
```

### 3.4 搜索页 `search`

| 元素 | 京东风格 |
| ---- | -------- |
| 搜索框 | 浅灰底 `#F5F5F5` + 小圆角 `20rpx`，无彩色边框 |
| 搜索框聚焦 | 边框变 `#E8E8E8`，**不用红色** |
| 筛选标签 | 白底 + 细边框，选中态：白底 + **红色文字 + 红色下划线** |
| 排序按钮 | 纯文字，选中态红色 |
| 商品卡片 | 纯白底 + `1rpx` 底线，无阴影 |
| 加购按钮 | 小尺寸红色填充按钮或红色 `+` 圆形图标 |
| 热搜标签 | `#F5F5F5` 灰底，不用品牌色底 |

**搜索框代码**：

```css
.search-box {
  background-color: #F5F5F5;
  border-radius: 20rpx;
  padding: 14rpx 24rpx;
  border: 2rpx solid transparent;
}
.search-box:focus-within {
  border-color: #E8E8E8;
  background-color: #FFFFFF;
  /* 不用红色聚焦环 */
}
```

### 3.5 购物车 `cart`

| 元素 | 京东风格 |
| ---- | -------- |
| 页头 | 纯白背景 + 底线分隔 |
| 商品项 | 纯白底 + `1rpx` 底线分隔，不用卡片阴影 |
| 数量控制器 | 方形按钮 `+` `-` + 细边框，不用圆形 |
| 删除按钮 | 灰色文字"删除"，不用红色背景 |
| 底部结算栏 | 纯白底 + 顶部细线，结算按钮红色直角 |
| Checkbox | 选中态红色勾选圆 |

**数量控制器代码**：

```css
.quantity-control {
  display: flex;
  align-items: center;
  border: 1rpx solid #E8E8E8;
  border-radius: 4rpx;
}
.qty-btn {
  width: 52rpx;
  height: 52rpx;
  text-align: center;
  line-height: 52rpx;
  font-size: 28rpx;
  color: #333;
  background: #F8F8F8;
}
.qty-value {
  width: 64rpx;
  text-align: center;
  font-size: 26rpx;
  border-left: 1rpx solid #E8E8E8;
  border-right: 1rpx solid #E8E8E8;
}
```

### 3.6 个人中心 `user`

| 元素 | 京东风格 |
| ---- | -------- |
| 头部背景 | **纯白背景**，不用渐变色块 |
| 用户头像 | 灰色圆形占位，无彩色光环 |
| 用户名 | `#333` 粗体 |
| 会员标签 | 小号灰色文字，不用品牌色底标签 |
| 余额卡片 | **白底 + 细线边框**，金额红色，标签灰色 |
| 订单入口 | 横排图标 + 文字，纯线性图标 |
| 菜单列表 | 白底 + `1rpx` 分割线，右侧灰色箭头 `>` |
| 退出按钮 | 灰色文字按钮，不用红色边框 |

### 3.7 登录页 `login`

| 元素 | 京东风格 |
| ---- | -------- |
| 头部 | **纯白背景**，Logo 居中，标题黑色 |
| 欢迎语 | `#333` 标题 + `#999` 描述 |
| 微信按钮 | 微信绿 `#07C160`，**小圆角 `8rpx`** |
| 协议文字 | 灰色，链接用 `#E4393C` |
| 功能介绍 | 白底小卡片 + 细线边框 |

### 3.8 订单页 `order`

| 元素 | 京东风格 |
| ---- | -------- |
| Tab 栏 | 白底，选中态：**红色文字 + 底部红色短线** |
| Tab 徽标 | 小红点或红色数字，不用渐变 |
| 订单卡片 | 白底 + `1rpx` 底边，无阴影 |
| 操作按钮 | "去支付"红色填充，"取消"灰色描边 |
| 支付方式 | 选中项：左侧红色竖条 + 浅红底 |

### 3.9 商品详情 `goods`

| 元素 | 京东风格 |
| ---- | -------- |
| 商品图 | 纯白底满宽展示，无圆角 |
| 价格行 | `#E4393C` 大号价格 + `¥` 小号前缀 |
| 商品名 | `#333` 两行截断 |
| 位置信息 | 灰底标签样式，不用品牌色底 |
| 底部栏 | 纯白 + 顶线，"加入购物车" 红色直角按钮 |

### 3.10 扫描 / 充值 / 支付

| 页面 | 京东风格要点 |
| ---- | ------------ |
| 扫描 | 头部纯白 + 标题居中，上传区域灰色虚线框 |
| 充值 | 余额展示：白底卡片 + 红色大号金额，金额选项白底细线 |
| 支付 | 纯白卡片、红色确认按钮、选中方式红色竖条 |

---

## 四、红色使用规则（关键约束）

> **原则：红色只出现在需要引导用户注意或行动的地方。**

### 4.1 允许使用红色的场景

| 场景 | 用法 | 色值 |
| ---- | ---- | ---- |
| 价格文字 | `color: #E4393C` | `#E4393C` |
| 主操作按钮 | `background: #E4393C` | `#E4393C` |
| 角标/徽章 | 小红点或红底白字数字 | `#E4393C` |
| Tab 选中态 | 文字红色 + 底部短线红色 | `#E4393C` |
| Checkbox 选中 | 红色填充勾选 | `#E4393C` |
| 次要按钮描边 | 红色边框 + 红色文字 | `#E4393C` |
| 链接文字 | 协议、"查看全部" | `#E4393C` |

### 4.2 禁止使用红色的场景

| 场景 | 正确做法 |
| ---- | -------- |
| 页面头部/导航栏背景 | 纯白 `#FFFFFF` |
| 卡片背景 | 纯白 `#FFFFFF` |
| 大面积渐变色块 | 去除，改为纯白 |
| 搜索框聚焦边框 | 灰色 `#E8E8E8` |
| 热搜标签底色 | 灰色 `#F5F5F5` |
| 筛选标签未选中态 | 灰色底 `#F5F5F5` |
| 功能图标背景色块 | 去除彩色背景，图标直接放白底 |
| 分割线 | `#F0F0F0` 灰线 |

---

## 五、进一步增强方向

### 5.1 骨架屏（Skeleton Screen）

适用页面：首页、搜索列表、订单列表。

```css
.sk-block {
  background: linear-gradient(90deg, #F5F5F5 25%, #EEEEEE 50%, #F5F5F5 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4rpx;
}
@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
```

### 5.2 首页内容丰富化

1. **Banner 轮播**（接口 `/api/applet/home/ads`）— 圆角 `12rpx`，白色指示点
2. **快捷入口**— 线性图标 + 文字，无背景色块
3. **推荐商品双列流**（接口 `/api/applet/home/recommend`）— 白底卡片 + 细线分隔

### 5.3 商品标签系统

```css
/* 标签统一小圆角、小字号 */
.tag-hot    { background: #FFF1F0; color: #E4393C; border: 1rpx solid #FFCCC7; }
.tag-new    { background: #F6FFED; color: #52C41A; border: 1rpx solid #D9F7BE; }
.tag-sale   { background: #FFF7E6; color: #FA8C16; border: 1rpx solid #FFE7BA; }
```

### 5.4 下拉刷新 + 无限加载

```javascript
onPullDownRefresh() {
  this.loadData().then(() => wx.stopPullDownRefresh());
},
onReachBottom() {
  if (this.data.hasMore) this.loadMore();
}
```

### 5.5 Tab Bar 图标升级

| Tab | 图标风格 | 普通色 | 选中色 |
| ---- | -------- | ------ | ------ |
| 首页 | 线性房屋 | `#999999` | `#E4393C` |
| 查询 | 线性搜索 | `#999999` | `#E4393C` |
| 购物车 | 线性购物车 | `#999999` | `#E4393C` |
| 我的 | 线性人物 | `#999999` | `#E4393C` |

推荐图标源：[RemixIcon](https://remixicon.com/) 导出 81×81px PNG，线宽 1.5px。

### 5.6 深色模式

```css
@media (prefers-color-scheme: dark) {
  page {
    --bg-page: #141414;
    --bg-card: #1F1F1F;
    --text-title: #F0F0F0;
    --text-body: #B3B3B3;
    --text-secondary: #808080;
    --border: #2A2A2A;
    --shadow-sm: none;
    --shadow-md: none;
  }
}
```

### 5.7 体验细节

| 改进项 | 说明 |
| ------ | ---- |
| 安全区域 | 底部固定栏 + `env(safe-area-inset-bottom)` |
| 触控热区 | 可点击元素最小 `88rpx × 88rpx` |
| 空状态 | 灰色线性插画 + 引导按钮 |
| 按钮防抖 | `loading` 态 + 禁用重复点击 |
| Toast 反馈 | 统一样式，成功绿 / 错误红 / 警告橙 |

### 5.8 性能优化

| 方向 | 措施 |
| ---- | ---- |
| 图片 | WebP + CDN + `lazy-load` |
| 长列表 | `recycle-view` 虚拟滚动 |
| 分包 | `scan`、`recharge`、`payment` 子包 |
| 缓存 | 分类、搜索历史本地缓存 |

---

## 六、优先级排期

| 优先级 | 事项 | 工时 |
| ------ | ---- | ---- |
| P0 | 全局色板切换为京东白色风格（CSS 变量 + 各页面） | 3h |
| P0 | 去除所有渐变色块，头部改纯白 | 2h |
| P0 | 按钮改小圆角纯色、卡片改细线边框 | 1h |
| P1 | 首页增加 Banner 轮播 + 推荐瀑布流 | 4h |
| P1 | 骨架屏组件 | 2h |
| P1 | Tab Bar 图标替换 | 1h |
| P2 | 商品标签（热销/新品/折扣） | 2h |
| P2 | 下拉刷新 + 加载更多 | 2h |
| P2 | 深色模式 | 2h |
| P3 | 空状态插画 | 依赖设计 |
| P3 | 分包优化 | 1h |

---

## 七、设计规范速查

| 项目 | 规范 |
| ---- | ---- |
| 间距 | 8 的倍数：`8 / 16 / 24 / 32 / 48 rpx` |
| 字号 | `22 → 24 → 26 → 28 → 30 → 34 → 40rpx` |
| 字重 | 正文 `400` / 加粗 `600` / 标题 `700` |
| 圆角 | 卡片 `12rpx` / 按钮 `8rpx` / 标签 `4rpx` / 搜索框 `20rpx` |
| 颜色 | 全部使用 CSS 变量，禁止硬编码 |
| 红色 | 仅 `#E4393C`，仅用于价格、主按钮、选中态、角标 |
| 分隔 | 用 `1rpx solid #F0F0F0` 代替阴影 |
| 头部 | 统一纯白，不使用彩色背景 |
