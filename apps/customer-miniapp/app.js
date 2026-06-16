// 安全加载网络请求模块，防止模块加载失败导致白屏
var post, get, del, requestModule;
try {
  requestModule = require('./utils/request');
  post = requestModule.post;
  get = requestModule.get;
  del = requestModule.del;
} catch (e) {
  console.error('加载 request 模块失败:', e);
  var noop = function () { return Promise.reject(new Error('网络模块未加载')); };
  post = noop; get = noop; del = noop;
}

var mergeCart = require('./utils/cartSync').mergeCart;
var DEFAULT_GOODS_IMAGE = '/assets/goods/default.png';

App({
  globalData: {
    userInfo: null,
    cartList: [],
    // apiBaseUrl 由 utils/request.js 统一读取和持久化
    apiBaseUrl: ''
  },

  onLaunch() {
    if (requestModule && requestModule.getBaseUrl) {
      this.globalData.apiBaseUrl = requestModule.getBaseUrl();
    }

    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');

    if (token && userInfo) {
      this.globalData.userInfo = userInfo;
    }

    this.loadCartFromStorage();

    if (token) {
      this.globalData.userInfo = userInfo;
      this.syncCartFromBackend();
    }
  },

  // ========== 本地缓存读取 ==========

  loadCartFromStorage() {
    const cartList = wx.getStorageSync('cartList') || [];
    this.globalData.cartList = cartList;
  },

  // ========== 从后端同步购物车 ==========

  syncCartFromBackend() {
    var that = this;
    get('/cart/list', {}, false)
      .then(function (res) {
        var backendItems = res.data || [];
        var localCart = wx.getStorageSync('cartList') || [];

        if (backendItems.length > 0) {
          // 如果本地购物车为空，说明用户已清空，清除后端残留数据
          if (localCart.length === 0) {
            del('/cart/clear', {}, false).catch(function () {});
            that.refreshCartBadge();
            return;
          }
          var merged = mergeCart(backendItems, localCart, function (path) {
            return that.getImageUrl(path);
          });
          that.globalData.cartList = merged;
          wx.setStorageSync('cartList', merged);
        } else {
          // 后端为空，将本地购物车推送到后端
          if (localCart.length > 0) {
            localCart.forEach(function (item) {
              post('/cart/add', { goodsId: item.id, quantity: item.count }, false).catch(function () {});
            });
          }
        }
        that.refreshCartBadge();
      })
      .catch(function () {});
  },

  // ========== 购物车操作（本地 + 后端同步） ==========

  addToCart(item, quantity = 1) {
    const cartList = this.globalData.cartList;
    const existIndex = cartList.findIndex(goods => goods.id === item.id);

    if (existIndex > -1) {
      cartList[existIndex].count += quantity;
    } else {
      cartList.push({ ...item, count: quantity, selected: true });
    }

    this.globalData.cartList = cartList;
    wx.setStorageSync('cartList', cartList);
    this.refreshCartBadge();

    // 同步到后端
    const newCount = existIndex > -1 ? cartList[existIndex].count : quantity;
    post('/cart/add', { goodsId: item.id, quantity: newCount }, false).catch(() => {});
  },

  removeFromCart(itemId) {
    let cartList = this.globalData.cartList;
    cartList = cartList.filter(item => item.id !== itemId);
    this.globalData.cartList = cartList;
    wx.setStorageSync('cartList', cartList);
    this.refreshCartBadge();

    // 后端没有单条删除接口，用清空+重新添加的方式同步
    this.fullSyncCartToBackend();
  },

  updateCartCount(itemId, count) {
    const cartList = this.globalData.cartList;
    const index = cartList.findIndex(item => item.id === itemId);
    if (index > -1) {
      if (count <= 0) {
        this.removeFromCart(itemId);
        return;
      }
      cartList[index].count = count;
      this.globalData.cartList = cartList;
      wx.setStorageSync('cartList', cartList);
      this.refreshCartBadge();

      // 同步数量到后端
      post('/cart/add', { goodsId: itemId, quantity: count }, false).catch(() => {});
    }
  },

  clearCart() {
    this.globalData.cartList = [];
    wx.removeStorageSync('cartList');
    wx.removeTabBarBadge({ index: 2 });

    // 同步清空到后端
    del('/cart/clear', {}, false).catch(() => {});
  },

  // ========== 全量同步（用于删除操作后） ==========

  fullSyncCartToBackend() {
    del('/cart/clear', {}, false)
      .then(() => {
        const cartList = this.globalData.cartList;
        if (cartList.length === 0) return;
        const promises = cartList.map(item =>
          post('/cart/add', { goodsId: item.id, quantity: item.count }, false)
        );
        return Promise.all(promises);
      })
      .catch(() => {});
  },

  // ========== 工具方法 ==========

  // 将后端返回的相对图片路径转为完整 URL（兼容 ngrok / 局域网 IP 等场景）
  getImageUrl: function (relativePath) {
    if (!relativePath || typeof relativePath !== 'string') return DEFAULT_GOODS_IMAGE;
    var imagePath = relativePath.trim();
    if (!imagePath) return DEFAULT_GOODS_IMAGE;
    if (
      imagePath.indexOf('/assets/') === 0 ||
      imagePath.indexOf('wxfile://') === 0 ||
      imagePath.indexOf('http://tmp/') === 0 ||
      imagePath.indexOf('data:image/') === 0
    ) {
      return imagePath;
    }

    // 获取服务器根地址（去掉 /api 后缀）
    var apiBase = this.globalData.apiBaseUrl;
    if (!apiBase) {
      try {
        apiBase = requestModule.getBaseUrl ? requestModule.getBaseUrl() : requestModule.BASE_URL;
      } catch (e) {}
    }
    if (!apiBase) apiBase = requestModule && requestModule.DEFAULT_BASE_URL ? requestModule.DEFAULT_BASE_URL : 'http://127.0.0.1:8080/api';
    var base = apiBase.replace(/\/api\/?$/i, '').replace(/\/+$/, '');

    // 处理完整的 HTTP URL：替换 localhost 为真实 IP
    if (/^https?:\/\//i.test(imagePath)) {
      return imagePath.replace(/^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?/i, base);
    }

    return base + '/' + imagePath.replace(/^\/+/, '');
  },

  isImageDownloadResponse: function (res) {
    var headers = (res && res.header) || {};
    var contentType = '';
    Object.keys(headers).some(function (key) {
      if (key.toLowerCase() === 'content-type') {
        contentType = String(headers[key] || '');
        return true;
      }
      return false;
    });
    return !contentType || /^image\//i.test(contentType);
  },

  resolveDisplayImage: function (imagePath) {
    var that = this;
    var url = that.getImageUrl(imagePath);
    if (
      !url ||
      url.indexOf('/assets/') === 0 ||
      url.indexOf('wxfile://') === 0 ||
      url.indexOf('http://tmp/') === 0 ||
      url.indexOf('data:image/') === 0
    ) {
      return Promise.resolve(url || DEFAULT_GOODS_IMAGE);
    }

    return new Promise(function (resolve) {
      wx.downloadFile({
        url: url,
        header: {
          'ngrok-skip-browser-warning': 'true'
        },
        success: function (res) {
          if (res.statusCode === 200 && res.tempFilePath && that.isImageDownloadResponse(res)) {
            resolve(res.tempFilePath);
          } else {
            resolve(DEFAULT_GOODS_IMAGE);
          }
        },
        fail: function () {
          resolve(DEFAULT_GOODS_IMAGE);
        }
      });
    });
  },

  refreshCartBadge() {
    const totalCount = this.getTotalCount();
    if (totalCount > 0) {
      wx.setTabBarBadge({
        index: 2,
        text: String(totalCount > 99 ? '99+' : totalCount)
      });
    } else {
      wx.removeTabBarBadge({ index: 2 });
    }
  },

  getCartList() {
    this.loadCartFromStorage();
    return this.globalData.cartList;
  },

  getTotalCount() {
    const cartList = this.globalData.cartList;
    return cartList.reduce((total, item) => total + item.count, 0);
  },

  // ========== 图片缓存（真机修复：HTTP→本地临时文件） ==========
  // 真机上 iOS ATS / Android cleartext 会拦截 <image> 的 HTTP 请求，
  // 但不会拦截 wx.request/wx.downloadFile（走微信网络层）。
  // 所以先用 downloadFile 下载到本地临时文件，再用本地路径显示。

  cacheImages: function (list, imageField) {
    var that = this;
    var defaultImg = DEFAULT_GOODS_IMAGE;
    var tasks = [];

    (list || []).forEach(function (item) {
      var url = item[imageField || 'image'];
      // 已经是本地资源或空路径，跳过
      if (!url) {
        item[imageField || 'image'] = DEFAULT_GOODS_IMAGE;
        return;
      }
      // 需要下载的远程 HTTP URL
      var downloadUrl = that.getImageUrl(url);
      var task = new Promise(function (resolve, reject) {
        wx.downloadFile({
          url: downloadUrl,
          header: {
            'ngrok-skip-browser-warning': 'true'
          },
          success: function (res) {
            if (res.statusCode === 200 && res.tempFilePath && that.isImageDownloadResponse(res)) {
              item[imageField || 'image'] = res.tempFilePath;
            } else {
              item[imageField || 'image'] = defaultImg;
            }
            resolve();
          },
          fail: function () {
            item[imageField || 'image'] = defaultImg;
            resolve(); // 失败不中断，用默认图
          }
        });
      });
      tasks.push(task);
    });

    return Promise.all(tasks);
  }
})
