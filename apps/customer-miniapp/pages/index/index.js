const app = getApp();
const { debounce } = require('../../utils/utils');

Page({
  data: {
    userInfo: null
  },

  onLoad() {
    this.checkLoginStatus();
  },

  onShow() {
    this.checkLoginStatus();
  },

  checkLoginStatus() {
    const userInfo = wx.getStorageSync('userInfo');
    this.setData({
      userInfo: userInfo
    });
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

  goScan() {
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
      url: '/pages/scan/scan'
    });
  },

  goSearch() {
    wx.switchTab({
      url: '/pages/search/search'
    });
  },

  goCart() {
    wx.switchTab({
      url: '/pages/cart/cart'
    });
  }
});
