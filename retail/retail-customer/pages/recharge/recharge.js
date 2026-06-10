const app = getApp();
const { get, post } = require('../../utils/request');
const { debounce } = require('../../utils/utils');

Page({
  data: {
    currentBalance: 0.00,
    amountOptions: [10, 20, 50, 100, 200, 500],
    selectedAmount: null,
    customAmount: '',
    rechargeAmount: 0
  },

  onLoad() {
    this.loadBalance();
  },

  onShow() {
    this.loadBalance();
  },

  loadBalance() {
    // 优先从后端获取余额
    get('/applet/user/info', {}, false)
      .then(res => {
        const data = res.data || {};
        const balance = data.balance || 0;
        this.setData({ currentBalance: balance });
        // 同步到本地
        const userInfo = wx.getStorageSync('userInfo') || {};
        userInfo.balance = balance;
        wx.setStorageSync('userInfo', userInfo);
        app.globalData.userInfo = userInfo;
      })
      .catch(() => {
        // 后端不可用时使用本地缓存
        const userInfo = wx.getStorageSync('userInfo');
        if (userInfo && userInfo.balance !== undefined) {
          this.setData({ currentBalance: userInfo.balance });
        }
      });
  },

  selectAmount(e) {
    const amount = e.currentTarget.dataset.amount;
    this.setData({
      selectedAmount: amount,
      customAmount: '',
      rechargeAmount: amount
    });
  },

  onCustomInput(e) {
    const value = e.detail.value;
    this.setData({
      selectedAmount: null,
      customAmount: value,
      rechargeAmount: value ? parseFloat(value) : 0
    });
  },

  confirmRecharge: debounce(function() {
    const amount = this.data.rechargeAmount;

    if (!amount || amount <= 0) {
      wx.showToast({
        title: '请选择或输入充值金额',
        icon: 'none'
      });
      return;
    }

    wx.showModal({
      title: '确认充值',
      content: '确定要充值 ¥' + amount + ' 吗？',
      success: (res) => {
        if (res.confirm) {
          this.processRecharge(amount);
        }
      }
    });
  }),

  processRecharge(amount) {
    wx.showLoading({ title: '充值中...', mask: true });

    post('/applet/user/recharge', { amount: amount }, false)
      .then(res => {
        wx.hideLoading();
        const newBalance = (res.data && res.data.balance) ? res.data.balance : 0;

        // 更新本地缓存
        const userInfo = wx.getStorageSync('userInfo') || {};
        userInfo.balance = newBalance;
        wx.setStorageSync('userInfo', userInfo);
        app.globalData.userInfo = userInfo;

        wx.showToast({
          title: '充值成功',
          icon: 'success',
          duration: 1500
        });

        this.setData({
          currentBalance: newBalance,
          selectedAmount: null,
          customAmount: '',
          rechargeAmount: 0
        });
      })
      .catch(() => {
        wx.hideLoading();
        wx.showToast({
          title: '充值失败，请重试',
          icon: 'none'
        });
      });
  }
});
