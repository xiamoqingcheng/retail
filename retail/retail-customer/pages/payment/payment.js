const app = getApp();
const { get, post } = require('../../utils/request');
const { debounce } = require('../../utils/utils');

Page({
  data: {
    orderId: '',
    order: null,
    currentBalance: 0,
    selectedMethod: 'balance',
    canPay: false
  },

  onLoad(options) {
    if (options.orderId) {
      this.setData({
        orderId: options.orderId
      });
      this.loadOrder(options.orderId);
    }
    this.loadBalance();
  },

  loadOrder(orderId) {
    wx.showLoading({
      title: '加载中...',
      mask: true
    });

    get('/applet/order/list', {}, false)
      .then(res => {
        wx.hideLoading();
        const orders = res.data || [];
        const order = orders.find(o => o.id === parseInt(orderId) || o.orderId == orderId);

        if (order) {
          this.setData({
            order: {
              id: order.id,
              orderId: order.orderId || order.id,
              totalPrice: order.totalAmount ? order.totalAmount.toString() : '0.00',
              goods: (order.goods || []).map(item => ({
                name: item.goodsName,
                price: item.goodsPrice,
                count: item.quantity
              }))
            }
          });
          this.checkCanPay();
        } else {
          wx.showToast({
            title: '订单不存在',
            icon: 'none'
          });
        }
      })
      .catch(() => {
        wx.hideLoading();
      });
  },

  loadBalance() {
    // 优先从后端获取余额
    get('/applet/user/info', {}, false)
      .then(res => {
        const data = res.data || {};
        const balance = data.balance || 0;
        this.setData({ currentBalance: balance });
        this.checkCanPay();
      })
      .catch(() => {
        const userInfo = wx.getStorageSync('userInfo');
        if (userInfo && userInfo.balance !== undefined) {
          this.setData({ currentBalance: userInfo.balance });
        }
        this.checkCanPay();
      });
  },

  selectMethod(e) {
    const method = e.currentTarget.dataset.method;
    this.setData({
      selectedMethod: method
    });
    this.checkCanPay();
  },

  checkCanPay() {
    const { order, currentBalance, selectedMethod } = this.data;
    let canPay = false;

    if (order) {
      const orderPrice = parseFloat(order.totalPrice);
      if (selectedMethod === 'balance') {
        canPay = currentBalance >= orderPrice;
      } else if (selectedMethod === 'wechat') {
        canPay = true;
      }
    }

    this.setData({
      canPay: canPay
    });
  },

  confirmPayment: debounce(function () {
    const { order, selectedMethod, currentBalance } = this.data;

    if (!order) {
      wx.showToast({
        title: '订单不存在',
        icon: 'none'
      });
      return;
    }

    if (selectedMethod === 'balance' && currentBalance < parseFloat(order.totalPrice)) {
      wx.showToast({
        title: '余额不足',
        icon: 'none'
      });
      return;
    }

    wx.showModal({
      title: '确认支付',
      content: '确定要支付 ¥' + order.totalPrice + ' 吗？',
      success: (res) => {
        if (res.confirm) {
          this.processPayment();
        }
      }
    });
  }),

  processPayment() {
    const { selectedMethod } = this.data;

    wx.showLoading({ title: '支付中...', mask: true });

    post('/applet/order/pay', {
      orderId: this.data.orderId,
      paymentMethod: selectedMethod
    }, false)
      .then(() => {
        wx.hideLoading();

        // 从后端同步最新余额
        if (selectedMethod === 'balance') {
          get('/applet/user/info', {}, false)
            .then(res => {
              const data = res.data || {};
              const userInfo = wx.getStorageSync('userInfo') || {};
              userInfo.balance = data.balance || 0;
              wx.setStorageSync('userInfo', userInfo);
              app.globalData.userInfo = userInfo;
            })
            .catch(() => {});
        }

        wx.showToast({
          title: '支付成功',
          icon: 'success',
          duration: 1500
        });

        setTimeout(() => {
          wx.switchTab({
            url: '/pages/user/user'
          });
        }, 1500);
      })
      .catch((err) => {
        wx.hideLoading();
        wx.showToast({
          title: (err && (err.message || err.msg)) || '支付失败，请重试',
          icon: 'none',
          duration: 2000
        });
      });
  },

  goBack() {
    wx.navigateBack();
  }
});
