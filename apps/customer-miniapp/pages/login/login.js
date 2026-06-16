const app = getApp();
const { post } = require('../../utils/request');

Page({
  data: {
    loading: false
  },

  onLoad() {
  },

  // 新版微信废弃了 getUserInfo，改为直接调用 wx.login
  onLoginClick() {
    if (this.data.loading) return;
    this.setData({ loading: true });

    wx.showLoading({ title: '登录中...', mask: true });

    wx.login({
      success: (res) => {
        console.log('wx.login 结果:', res);
        if (res.code) {
          this.doLogin(res.code);
        } else {
          wx.hideLoading();
          this.setData({ loading: false });
          wx.showToast({ title: '微信登录失败', icon: 'none' });
        }
      },
      fail: (err) => {
        console.error('wx.login 失败:', err);
        wx.hideLoading();
        this.setData({ loading: false });
        wx.showToast({ title: '微信登录失败', icon: 'none' });
      }
    });
  },

  doLogin(code) {
    // 演示环境后端仅支持 test_code
    const loginCode = 'test_code';
    console.log('请求登录, 参数:', { code: loginCode, 真实code: code });

    post('/applet/auth/login', { code: loginCode }, false)
      .then(res => {
        const tokenData = res.data || {};

        if (!tokenData.token) {
          wx.showToast({ title: '登录失败：未获取到Token', icon: 'none' });
          return;
        }

        // 合并后端返回的用户信息
        const userData = {
          token: tokenData.token,
          id: tokenData.userId || 1,
          openid: tokenData.openid || '',
          nickname: tokenData.nickname || '微信用户',
          avatar: tokenData.avatarUrl || '',
          balance: tokenData.balance || 0.00
        };

        wx.setStorageSync('token', tokenData.token);
        wx.setStorageSync('userInfo', userData);
        app.globalData.userInfo = userData;

        wx.hideLoading();
        wx.showToast({ title: '登录成功', icon: 'success', duration: 1500 });

        // 登录成功后同步后端购物车
        app.syncCartFromBackend();

        setTimeout(() => {
          const pages = getCurrentPages();
          if (pages.length > 1) {
            wx.navigateBack();
          } else {
            wx.switchTab({ url: '/pages/index/index' });
          }
        }, 1500);

        setTimeout(() => {
          this.setData({ loading: false });
        }, 2000);
      })
      .catch(err => {
        console.error('登录失败:', err);
        wx.hideLoading();
        this.setData({ loading: false });

        var errorMsg = (err && (err.message || err.msg)) || '登录失败，请重试';
        wx.showToast({ title: errorMsg, icon: 'none', duration: 3000 });
      });
  },

  showUserAgreement() {
    wx.showModal({
      title: '用户协议',
      content: '欢迎使用智能零售系统！\n\n本协议阐述了使用本系统的相关条款和条件。',
      showCancel: false,
      confirmText: '我已知晓'
    });
  },

  showPrivacyPolicy() {
    wx.showModal({
      title: '隐私政策',
      content: '我们非常重视您的隐私保护。',
      showCancel: false,
      confirmText: '我已知晓'
    });
  }
});
