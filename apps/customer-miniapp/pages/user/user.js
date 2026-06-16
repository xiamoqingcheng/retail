const app = getApp();
const { get } = require('../../utils/request');
const { debounce } = require('../../utils/utils');

Page({
  data: {
    userInfo: null
  },

  onShow() {
    this.checkLoginStatus();
    this.syncBalanceFromBackend();
    app.refreshCartBadge();
  },

  checkLoginStatus() {
    const userInfo = wx.getStorageSync('userInfo');
    this.setData({
      userInfo: userInfo
    });
  },

  syncBalanceFromBackend() {
    const token = wx.getStorageSync('token');
    if (!token) return;

    get('/applet/user/info', {}, false)
      .then(res => {
        const data = res.data || {};
        const balance = data.balance || 0;
        const userInfo = wx.getStorageSync('userInfo') || {};
        userInfo.balance = balance;
        wx.setStorageSync('userInfo', userInfo);
        app.globalData.userInfo = userInfo;
        this.setData({ userInfo: userInfo });
      })
      .catch(() => {});
  },

  goLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    });
  },

  logout: debounce(function() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          app.globalData.userInfo = null;
          this.setData({
            userInfo: null
          });
          wx.showToast({
            title: '已退出登录',
            icon: 'success'
          });
        }
      }
    });
  }),

  goOrder() {
    if (!this.data.userInfo) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateTo({
          url: '/pages/login/login'
        });
      }, 1500);
      return;
    }
    wx.navigateTo({
      url: '/pages/order/order'
    });
  },

  goOrderByStatus(e) {
    const status = e.currentTarget.dataset.status;
    if (!this.data.userInfo) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      });
      return;
    }
    wx.navigateTo({
      url: '/pages/order/order?status=' + status
    });
  },

  goRecharge() {
    if (!this.data.userInfo) {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      });
      return;
    }
    wx.navigateTo({
      url: '/pages/recharge/recharge'
    });
  },

  goSettings() {
    wx.navigateTo({
      url: '/pages/settings/settings'
    });
  },

  goHelp() {
    wx.navigateTo({
      url: '/pages/help/help'
    });
  },

  goAbout() {
    wx.navigateTo({
      url: '/pages/about/about'
    });
  }
});
