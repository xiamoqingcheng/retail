const app = getApp();
const { post } = require('../../utils/request');

// 收起阈值：拖拽超过面板高度 35% 时认定为收回
const COLLAPSE_RATIO = 0.35;
// touchmove setData 节流：约 60fps
const TOUCH_THROTTLE = 16;

Page({
  data: {
    // 主模式：choose 选择 / camera 即拍识别 / album 图片识别
    mode: 'choose',

    // 屏幕信息
    screenWidth: 375,
    screenHeight: 667,
    panelHeight: 444,    // 屏幕 2/3
    panelMaxY: 444,      // 完全收起时的 translateY

    // 即拍识别状态
    captureStage: 'preview',  // preview / result
    capturedImage: '',
    imageWidth: 0,
    imageHeight: 0,
    displayWidth: 0,
    displayHeight: 0,
    isDetecting: false,       // 异步识别中（面板已上拉，仅 loading 占位）

    // 识别结果
    detectedItems: [],
    selectedCount: 0,
    selectedTotalText: '0.00',

    // 上拉面板（translateY: 0=展开, panelMaxY=收起）
    panelY: 0,
    panelTransition: true,

    // 图片识别（保留旧逻辑）
    uploadedImage: null,
    albumDetectedItems: [],
    albumSelectedCount: 0,
    albumSelectedTotalText: '0.00',
    cartTotalCount: 0,
    isLoading: false
  },

  onLoad() {
    const sys = wx.getSystemInfoSync();
    const screenW = sys.windowWidth;
    const screenH = sys.windowHeight;
    const panelH = Math.round(screenH * 2 / 3);
    this.setData({
      screenWidth: screenW,
      screenHeight: screenH,
      panelHeight: panelH,
      panelMaxY: panelH,
      cartTotalCount: this._getCartTotalCount()
    });
    this._cameraContext = null;
    this._touchStartY = 0;
    this._panelStartY = 0;
    this._lastMove = 0;
  },

  onUnload() {
    this._cameraContext = null;
  },

  onShow() {
    this._refreshCartTotal();
  },

  // ============ 模式选择 ============

  selectCameraMode() {
    wx.authorize({
      scope: 'scope.camera',
      success: () => this._enterCamera(),
      fail: () => {
        wx.showModal({
          title: '需要相机权限',
          content: '请在设置中开启相机权限以使用即拍识别',
          showCancel: true,
          confirmText: '去设置',
          success: (res) => {
            if (res.confirm) wx.openSetting();
          }
        });
      }
    });
  },

  _enterCamera() {
    this.setData({
      mode: 'camera',
      captureStage: 'preview',
      capturedImage: '',
      detectedItems: [],
      isDetecting: false,
      selectedCount: 0,
      selectedTotalText: '0.00',
      cartTotalCount: this._getCartTotalCount(),
      panelY: 0,
      panelTransition: true
    }, () => {
      this._cameraContext = wx.createCameraContext();
    });
  },

  selectAlbumMode() {
    this.setData({
      mode: 'album',
      cartTotalCount: this._getCartTotalCount()
    });
    this._chooseAlbumImage();
  },

  backToChoose() {
    this.setData({
      mode: 'choose',
      captureStage: 'preview',
      capturedImage: '',
      uploadedImage: null,
      detectedItems: [],
      albumDetectedItems: [],
      albumSelectedCount: 0,
      albumSelectedTotalText: '0.00',
      cartTotalCount: this._getCartTotalCount(),
      panelY: 0
    });
  },

  // ============ 即拍识别 ============

  onCameraError(e) {
    console.error('camera error:', e.detail);
    wx.showToast({ title: '相机初始化失败', icon: 'none' });
  },

  onCapture() {
    if (this.data.captureStage !== 'preview') return;
    if (!this._cameraContext) this._cameraContext = wx.createCameraContext();

    this._cameraContext.takePhoto({
      quality: 'high',
      success: (res) => {
        const path = res.tempImagePath;
        // 拍照成功 → 立即切换到 result：捕获帧 + 面板上拉 + loading 占位
        this.setData({
          capturedImage: path,
          captureStage: 'result',
          isDetecting: true,
          detectedItems: [],
          selectedCount: 0,
          selectedTotalText: '0.00',
          cartTotalCount: this._getCartTotalCount(),
          panelY: 0,
          panelTransition: true
        });
        this._detect(path);
      },
      fail: (err) => {
        console.error('拍照失败:', err);
        wx.showToast({ title: '拍照失败，请重试', icon: 'none' });
      }
    });
  },

  _detect(path) {
    wx.getImageInfo({
      src: path,
      success: (info) => {
        const screenW = this.data.screenWidth;
        const ratio = screenW / info.width;
        this.setData({
          imageWidth: info.width,
          imageHeight: info.height,
          displayWidth: screenW,
          displayHeight: Math.round(info.height * ratio)
        });
      }
    });

    this._doDetectRequest(path, 0);
  },

  _doDetectRequest(path, retryCount) {
    wx.getFileSystemManager().readFile({
      filePath: path,
      encoding: 'base64',
      success: (res) => {
        post('/applet/scan', { imageBase64: res.data, k: 8 }, false)
          .then((response) => {
            this._handleResult(response.data || []);
          })
          .catch((err) => {
            console.error('识别失败:', err);
            if (retryCount < 1) {
              this._doDetectRequest(path, retryCount + 1);
            } else {
              this._handleResult([]);
              wx.showToast({ title: '识别失败，请稍后重试', icon: 'none' });
            }
          });
      },
      fail: (err) => {
        console.error('图片读取失败:', err);
        this._handleResult([]);
        wx.showToast({ title: '图片读取失败', icon: 'none' });
      }
    });
  },

  _handleResult(items) {
    // 按置信度降序（兼容 confidence / distance）
    const sorted = (items || []).slice().sort((a, b) => {
      const ca = a.confidence != null ? a.confidence : (a.distance != null ? a.distance : 0);
      const cb = b.confidence != null ? b.confidence : (b.distance != null ? b.distance : 0);
      return cb - ca;
    });

    const displayW = this.data.displayWidth || this.data.screenWidth;
    const displayH = this.data.displayHeight || displayW;
    const origW = this.data.imageWidth || 1920;
    const origH = this.data.imageHeight || 1080;
    const scaleX = displayW / origW;
    const scaleY = displayH / origH;

    // 用购物车现有数量初始化每项 count
    const cartMap = {};
    (app.globalData.cartList || []).forEach((c) => { cartMap[c.id] = c.count; });

    const detectedItems = sorted.map((item, i) => {
      const box = item.box || [];
      let boxLeft, boxTop, boxWidth, boxHeight;
      if (box.length === 4) {
        boxLeft = Math.round(box[0] * scaleX);
        boxTop = Math.round(box[1] * scaleY);
        boxWidth = Math.round((box[2] - box[0]) * scaleX);
        boxHeight = Math.round((box[3] - box[1]) * scaleY);
      } else if (item.boxCenter && item.boxCenter.length === 2) {
        boxWidth = 80; boxHeight = 80;
        boxLeft = Math.round(item.boxCenter[0] * scaleX) - 40;
        boxTop = Math.round(item.boxCenter[1] * scaleY) - 40;
      } else {
        boxWidth = 90; boxHeight = 90;
        boxLeft = 24 + (i % 3) * 100;
        boxTop = 24 + Math.floor(i / 3) * 100;
      }

      const rawConf = item.confidence != null
        ? item.confidence
        : (item.distance != null ? item.distance : 0);
      const conf = Math.max(0, Math.min(100, Math.round(rawConf * 100)));
      const price = Number(item.price != null ? item.price : (item.goodsPrice || 0));

      return {
        id: item.goodsId,
        name: item.goodsName || '未知商品',
        price: price,
        formattedPrice: price.toFixed(2),
        image: item.imageUrl ? app.getImageUrl(item.imageUrl) : '/assets/goods/default.png',
        shelfId: item.shelfId || '',
        confidence: conf,
        confidenceText: conf + '%',
        boxIndex: i + 1,
        boxLeft, boxTop, boxWidth, boxHeight,
        count: cartMap[item.goodsId] || 0
      };
    });

    // 预下载远程图，避免真机 HTTP 拦截
    const tasks = detectedItems.map((it) => {
      return app.resolveDisplayImage(it.image).then(image => {
        it.image = image;
      });
    });

    Promise.all(tasks).then(() => {
      const totals = this._calcSelected(detectedItems);
      this.setData({
        detectedItems: detectedItems,
        isDetecting: false,
        selectedCount: totals.count,
        selectedTotalText: totals.total,
        cartTotalCount: this._getCartTotalCount()
      });
    });
  },

  // ============ 上拉面板手势 ============

  onPanelTouchStart(e) {
    if (!e.touches || !e.touches[0]) return;
    this._touchStartY = e.touches[0].clientY;
    this._panelStartY = this.data.panelY;
    this._lastMove = 0;
    this.setData({ panelTransition: false });
  },

  onPanelTouchMove(e) {
    if (!e.touches || !e.touches[0]) return;
    const deltaY = e.touches[0].clientY - this._touchStartY;
    let newY = this._panelStartY + deltaY;
    if (newY < 0) newY = 0;
    if (newY > this.data.panelMaxY) newY = this.data.panelMaxY;
    const now = Date.now();
    if (now - this._lastMove < TOUCH_THROTTLE) return;
    this._lastMove = now;
    this.setData({ panelY: newY });
  },

  onPanelTouchEnd() {
    const threshold = this.data.panelMaxY * COLLAPSE_RATIO;
    if (this.data.panelY > threshold) {
      // 收回 → 重新激活摄像头
      this.setData({ panelTransition: true, panelY: this.data.panelMaxY });
      setTimeout(() => this._reactivateCamera(), 280);
    } else {
      // 回弹到展开
      this.setData({ panelTransition: true, panelY: 0 });
    }
  },

  _reactivateCamera() {
    this.setData({
      captureStage: 'preview',
      capturedImage: '',
      detectedItems: [],
      isDetecting: false,
      selectedCount: 0,
      selectedTotalText: '0.00',
      cartTotalCount: this._getCartTotalCount(),
      panelY: 0,
      panelTransition: true
    }, () => {
      this._cameraContext = wx.createCameraContext();
    });
  },

  // ============ 增减购 ============

  _calcSelected(items) {
    let count = 0, total = 0;
    (items || []).forEach((it) => {
      if (it.count > 0) {
        count += it.count;
        total += it.count * (Number(it.price) || 0);
      }
    });
    return { count: count, total: total.toFixed(2) };
  },

  _getCartTotalCount() {
    if (app && typeof app.getTotalCount === 'function') {
      return app.getTotalCount();
    }
    return (app.globalData.cartList || []).reduce((total, item) => total + (Number(item.count) || 0), 0);
  },

  _refreshCartTotal() {
    this.setData({ cartTotalCount: this._getCartTotalCount() });
  },

  _checkoutDetectedItems(items) {
    const selectedItems = (items || [])
      .filter((item) => (Number(item.count) || 0) > 0)
      .map((item) => ({
        id: item.id,
        name: item.name,
        price: item.price,
        image: item.image,
        count: item.count,
        selected: true
      }));

    if (selectedItems.length === 0) {
      wx.showToast({ title: '请先选择商品', icon: 'none' });
      return;
    }

    const totalAmount = selectedItems.reduce((sum, item) => {
      return sum + (Number(item.price) || 0) * (Number(item.count) || 0);
    }, 0);
    wx.setStorageSync('checkoutData', { items: selectedItems, totalAmount: totalAmount });
    wx.navigateTo({ url: '/pages/order/order?type=checkout' });
  },

  onIncrease(e) {
    const id = e.currentTarget.dataset.id;
    const items = this.data.detectedItems;
    const idx = items.findIndex((i) => i.id === id);
    if (idx < 0) return;
    const item = items[idx];
    const newCount = (item.count || 0) + 1;
    items[idx] = Object.assign({}, item, { count: newCount });

    if (newCount === 1) {
      app.addToCart({
        id: item.id,
        name: item.name,
        price: item.price,
        image: item.image
      }, 1);
    } else {
      app.updateCartCount(id, newCount);
    }

    const totals = this._calcSelected(items);
    this.setData({
      detectedItems: items,
      selectedCount: totals.count,
      selectedTotalText: totals.total,
      cartTotalCount: this._getCartTotalCount()
    });
  },

  onDecrease(e) {
    const id = e.currentTarget.dataset.id;
    const items = this.data.detectedItems;
    const idx = items.findIndex((i) => i.id === id);
    if (idx < 0) return;
    const item = items[idx];
    const newCount = Math.max(0, (item.count || 0) - 1);
    items[idx] = Object.assign({}, item, { count: newCount });

    if (newCount === 0) {
      app.removeFromCart(id);
    } else {
      app.updateCartCount(id, newCount);
    }

    const totals = this._calcSelected(items);
    this.setData({
      detectedItems: items,
      selectedCount: totals.count,
      selectedTotalText: totals.total,
      cartTotalCount: this._getCartTotalCount()
    });
  },

  // ============ 立即结算 ============

  goSettlement() {
    if (this.data.selectedCount === 0) {
      wx.showToast({ title: '请先选择商品', icon: 'none' });
      return;
    }
    this._checkoutDetectedItems(this.data.detectedItems);
  },

  goCart() {
    wx.switchTab({ url: '/pages/cart/cart' });
  },

  goAlbumSettlement() {
    if (this.data.albumSelectedCount === 0) {
      wx.showToast({ title: '请先选择商品', icon: 'none' });
      return;
    }
    this._checkoutDetectedItems(this.data.albumDetectedItems);
  },

  // ============ 退出/返回 ============

  exitCamera() {
    if (this.data.captureStage === 'result') {
      // 在结果态，按 × 等价于下拉面板
      this.setData({ panelTransition: true, panelY: this.data.panelMaxY });
      setTimeout(() => this._reactivateCamera(), 280);
      return;
    }
    this.backToChoose();
  },

  // ============ 图片识别（保留旧逻辑） ============

  _chooseAlbumImage() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album'],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0];
        this._uploadAndDetectAlbum(tempFilePath);
      },
      fail: () => {
        this.setData({ mode: 'choose' });
      }
    });
  },

  _uploadAndDetectAlbum(imagePath) {
    this.setData({
      uploadedImage: imagePath,
      isLoading: true,
      albumDetectedItems: [],
      albumSelectedCount: 0,
      albumSelectedTotalText: '0.00',
      cartTotalCount: this._getCartTotalCount(),
      displayWidth: 0,
      displayHeight: 0
    });

    wx.getImageInfo({
      src: imagePath,
      success: (res) => {
        wx.getSystemInfo({
          success: (sys) => {
            const w = sys.windowWidth - 40;
            const ratio = w / res.width;
            this.setData({
              imageWidth: res.width,
              imageHeight: res.height,
              displayWidth: w,
              displayHeight: Math.round(res.height * ratio)
            });
          }
        });
      }
    });

    wx.showLoading({ title: '识别中...', mask: true });
    this._doAlbumDetectRequest(imagePath, 0);
  },

  _doAlbumDetectRequest(imagePath, retryCount) {
    wx.getFileSystemManager().readFile({
      filePath: imagePath,
      encoding: 'base64',
      success: (res) => {
        post('/applet/scan', { imageBase64: res.data, k: 5 }, false)
          .then((response) => {
            wx.hideLoading();
            this._processAlbumResult(response.data || [], imagePath);
          })
          .catch((err) => {
            console.error('识别失败:', err);
            if (retryCount < 1) {
              this._doAlbumDetectRequest(imagePath, retryCount + 1);
            } else {
              wx.hideLoading();
              this.setData({ isLoading: false });
              wx.showToast({ title: '识别失败，请稍后重试', icon: 'none' });
            }
          });
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('图片读取失败:', err);
        this.setData({ isLoading: false });
        wx.showToast({ title: '图片读取失败', icon: 'none' });
      }
    });
  },

  _processAlbumResult(items, imagePath) {
    if (!items || items.length === 0) {
      this.setData({
        albumDetectedItems: [],
        albumSelectedCount: 0,
        albumSelectedTotalText: '0.00',
        cartTotalCount: this._getCartTotalCount(),
        isLoading: false
      });
      wx.showToast({ title: '未识别到商品', icon: 'none' });
      return;
    }

    const displayW = this.data.displayWidth || this.data.imageWidth || 375;
    const displayH = this.data.displayHeight || this.data.imageHeight || 375;
    const origW = this.data.imageWidth || 1920;
    const origH = this.data.imageHeight || 1080;
    const scaleX = displayW / origW;
    const scaleY = displayH / origH;

    const cartMap = {};
    (app.globalData.cartList || []).forEach((c) => { cartMap[c.id] = c.count; });

    const sorted = items.slice().sort((a, b) => {
      const ca = a.confidence != null ? a.confidence : (a.distance != null ? a.distance : 0);
      const cb = b.confidence != null ? b.confidence : (b.distance != null ? b.distance : 0);
      return cb - ca;
    });

    const list = sorted.map((item, i) => {
      const box = item.box || [];
      let boxLeft, boxTop, boxWidth, boxHeight;
      if (box.length === 4) {
        boxLeft = Math.round(box[0] * scaleX);
        boxTop = Math.round(box[1] * scaleY);
        boxWidth = Math.round((box[2] - box[0]) * scaleX);
        boxHeight = Math.round((box[3] - box[1]) * scaleY);
      } else if (item.boxCenter && item.boxCenter.length === 2) {
        boxWidth = 60; boxHeight = 60;
        boxLeft = Math.round(item.boxCenter[0] * scaleX) - 30;
        boxTop = Math.round(item.boxCenter[1] * scaleY) - 30;
      } else {
        boxWidth = 80; boxHeight = 80;
        boxLeft = 50 + (i % 3) * 90;
        boxTop = 50 + Math.floor(i / 3) * 90;
      }
      const rawConf = item.confidence != null ? item.confidence
        : (item.distance != null ? item.distance : 0);
      const conf = Math.max(0, Math.min(100, Math.round(rawConf * 100)));
      const price = Number(item.price != null ? item.price : (item.goodsPrice || 0));

      return {
        id: item.goodsId,
        name: item.goodsName || '未知商品',
        price: price,
        formattedPrice: price.toFixed(2),
        image: item.imageUrl ? app.getImageUrl(item.imageUrl) : '/assets/goods/default.png',
        shelfId: item.shelfId || '',
        confidence: conf,
        confidenceText: conf + '%',
        boxIndex: i + 1,
        boxLeft, boxTop, boxWidth, boxHeight,
        count: cartMap[item.goodsId] || 0
      };
    });

    const tasks = list.map((it) => {
      return app.resolveDisplayImage(it.image).then(image => {
        it.image = image;
      });
    });

    Promise.all(tasks).then(() => {
      const totals = this._calcSelected(list);
      this.setData({
        albumDetectedItems: list,
        albumSelectedCount: totals.count,
        albumSelectedTotalText: totals.total,
        cartTotalCount: this._getCartTotalCount(),
        isLoading: false
      });
      wx.showToast({ title: '识别到 ' + list.length + ' 个商品', icon: 'success' });
    });
  },

  // album 模式下的增减购（与即拍模式共享逻辑）
  onAlbumIncrease(e) {
    const id = e.currentTarget.dataset.id;
    const items = this.data.albumDetectedItems;
    const idx = items.findIndex((i) => i.id === id);
    if (idx < 0) return;
    const item = items[idx];
    const newCount = (item.count || 0) + 1;
    items[idx] = Object.assign({}, item, { count: newCount });
    if (newCount === 1) {
      app.addToCart({ id: item.id, name: item.name, price: item.price, image: item.image }, 1);
    } else {
      app.updateCartCount(id, newCount);
    }
    const totals = this._calcSelected(items);
    this.setData({
      albumDetectedItems: items,
      albumSelectedCount: totals.count,
      albumSelectedTotalText: totals.total,
      cartTotalCount: this._getCartTotalCount()
    });
  },

  onAlbumDecrease(e) {
    const id = e.currentTarget.dataset.id;
    const items = this.data.albumDetectedItems;
    const idx = items.findIndex((i) => i.id === id);
    if (idx < 0) return;
    const item = items[idx];
    const newCount = Math.max(0, (item.count || 0) - 1);
    items[idx] = Object.assign({}, item, { count: newCount });
    if (newCount === 0) app.removeFromCart(id);
    else app.updateCartCount(id, newCount);
    const totals = this._calcSelected(items);
    this.setData({
      albumDetectedItems: items,
      albumSelectedCount: totals.count,
      albumSelectedTotalText: totals.total,
      cartTotalCount: this._getCartTotalCount()
    });
  },

  reupload() {
    this.setData({
      uploadedImage: null,
      albumDetectedItems: [],
      albumSelectedCount: 0,
      albumSelectedTotalText: '0.00',
      cartTotalCount: this._getCartTotalCount(),
      isLoading: false
    });
    this._chooseAlbumImage();
  },

  previewImage() {
    const url = this.data.uploadedImage;
    if (url) wx.previewImage({ urls: [url], current: url });
  }
});
