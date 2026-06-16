const request = require('../../utils/request');
const app = getApp();

Page({
  data: {
    apiBaseUrl: '',
    defaultApiBaseUrl: request.DEFAULT_BASE_URL || request.BASE_URL,
    connectionStatus: 'idle',
    connectionText: '未检测服务状态',
    hasToken: false,
    userLabel: '未登录',
    cacheSummary: '购物车 0 件，搜索历史 0 条',
    storageSummary: '本地存储 0KB / 0KB',
    showDiagnostics: false,
    systemText: '',
    versionText: 'v1.0.0',
    envText: 'develop'
  },

  onLoad() {
    this.loadDeviceInfo();
  },

  onShow() {
    this.refreshSettings();
  },

  loadDeviceInfo() {
    let systemText = '未知设备';
    let versionText = 'v1.0.0';
    let envText = 'develop';

    try {
      const info = wx.getSystemInfoSync();
      systemText = [info.brand, info.model, info.platform].filter(Boolean).join(' · ');
    } catch (e) {}

    try {
      const accountInfo = wx.getAccountInfoSync && wx.getAccountInfoSync();
      if (accountInfo && accountInfo.miniProgram) {
        versionText = accountInfo.miniProgram.version || versionText;
        envText = accountInfo.miniProgram.envVersion || envText;
      }
    } catch (e) {}

    this.setData({
      systemText,
      versionText,
      envText
    });
  },

  refreshSettings() {
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo') || {};
    const cartList = wx.getStorageSync('cartList') || [];
    const historySearch = wx.getStorageSync('historySearch') || [];
    const showDiagnostics = !!wx.getStorageSync('showDiagnostics');
    let storageSummary = '本地存储 0KB / 0KB';

    try {
      const storageInfo = wx.getStorageInfoSync();
      storageSummary = '本地存储 ' + storageInfo.currentSize + 'KB / ' + storageInfo.limitSize + 'KB';
    } catch (e) {}

    this.setData({
      apiBaseUrl: request.getBaseUrl(),
      hasToken: !!token,
      userLabel: token ? (userInfo.nickname || '已登录用户') : '未登录',
      cacheSummary: '购物车 ' + cartList.length + ' 件，搜索历史 ' + historySearch.length + ' 条',
      storageSummary,
      showDiagnostics
    });
  },

  onApiInput(e) {
    this.setData({
      apiBaseUrl: e.detail.value,
      connectionStatus: 'idle',
      connectionText: '地址已修改，保存后生效'
    });
  },

  saveApiBaseUrl() {
    if (!request.isValidBaseUrl(this.data.apiBaseUrl)) {
      wx.showToast({
        title: '请输入有效服务地址',
        icon: 'none'
      });
      return;
    }

    const normalized = request.setBaseUrl(this.data.apiBaseUrl);
    this.setData({
      apiBaseUrl: normalized,
      connectionStatus: 'idle',
      connectionText: '已保存，建议检测连接'
    });
    wx.showToast({
      title: '已保存',
      icon: 'success'
    });
  },

  resetApiBaseUrl() {
    wx.showModal({
      title: '恢复默认地址',
      content: '将接口地址恢复为 ' + this.data.defaultApiBaseUrl,
      success: (res) => {
        if (!res.confirm) return;
        const baseUrl = request.resetBaseUrl();
        this.setData({
          apiBaseUrl: baseUrl,
          connectionStatus: 'idle',
          connectionText: '已恢复默认地址'
        });
      }
    });
  },

  testConnection() {
    if (!request.isValidBaseUrl(this.data.apiBaseUrl)) {
      wx.showToast({
        title: '请先填写有效地址',
        icon: 'none'
      });
      return;
    }

    const baseUrl = request.normalizeBaseUrl(this.data.apiBaseUrl);
    this.setData({
      connectionStatus: 'checking',
      connectionText: '正在检测服务...'
    });
    wx.showLoading({
      title: '检测中...'
    });

    wx.request({
      url: baseUrl + '/applet/home/ads',
      method: 'GET',
      timeout: 8000,
      success: (res) => {
        const body = res.data || {};
        const ok = res.statusCode >= 200 && res.statusCode < 300 && (body.code === 200 || body.data);
        this.setData({
          apiBaseUrl: baseUrl,
          connectionStatus: ok ? 'success' : 'warning',
          connectionText: ok ? '服务连接正常' : '服务有响应，但返回格式异常'
        });
        if (ok) {
          request.setBaseUrl(baseUrl);
        }
      },
      fail: (err) => {
        this.setData({
          connectionStatus: 'error',
          connectionText: err && err.errMsg ? err.errMsg : '服务连接失败'
        });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  },

  copyApiBaseUrl() {
    wx.setClipboardData({
      data: this.data.apiBaseUrl
    });
  },

  clearCartCache() {
    wx.showModal({
      title: '清理购物车缓存',
      content: '将清空本地购物车和待结算数据，登录状态与接口配置会保留。',
      success: (res) => {
        if (!res.confirm) return;
        if (wx.getStorageSync('token') && app.clearCart) {
          app.clearCart();
        } else {
          wx.removeStorageSync('cartList');
          if (app.globalData) app.globalData.cartList = [];
          wx.removeTabBarBadge({ index: 2 });
        }
        wx.removeStorageSync('checkoutData');
        this.refreshSettings();
        wx.showToast({
          title: '已清理',
          icon: 'success'
        });
      }
    });
  },

  clearBrowseCache() {
    wx.showModal({
      title: '清理浏览缓存',
      content: '将清空搜索历史和临时结算数据，登录状态与接口配置会保留。',
      success: (res) => {
        if (!res.confirm) return;
        wx.removeStorageSync('historySearch');
        wx.removeStorageSync('checkoutData');
        this.refreshSettings();
        wx.showToast({
          title: '已清理',
          icon: 'success'
        });
      }
    });
  },

  onDiagnosticsChange(e) {
    const checked = !!e.detail.value;
    wx.setStorageSync('showDiagnostics', checked);
    this.setData({
      showDiagnostics: checked
    });
  },

  copyDiagnostics() {
    const text = [
      '智能零售小程序诊断信息',
      'API: ' + this.data.apiBaseUrl,
      '连接状态: ' + this.data.connectionText,
      '登录状态: ' + this.data.userLabel,
      '设备: ' + this.data.systemText,
      '版本: ' + this.data.versionText,
      '环境: ' + this.data.envText,
      this.data.storageSummary
    ].join('\n');

    wx.setClipboardData({
      data: text
    });
  },

  goLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    });
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后仍会保留购物车缓存和接口配置。',
      success: (res) => {
        if (!res.confirm) return;
        wx.removeStorageSync('token');
        wx.removeStorageSync('userInfo');
        if (app.globalData) app.globalData.userInfo = null;
        this.refreshSettings();
        wx.showToast({
          title: '已退出',
          icon: 'success'
        });
      }
    });
  }
});
