const app = getApp();
const { get, post } = require('../../utils/request');
const { debounce } = require('../../utils/utils');

Page({
  data: {
    statusTabs: [
      { label: '全部', value: 'all', count: 0 },
      { label: '待付款', value: 'pending', count: 0 },
      { label: '已完成', value: 'completed', count: 0 }
    ],
    currentStatus: 'all',
    allOrders: [],
    orderList: [],
    isCheckout: false,
    checkoutItems: [],
    totalPrice: '0.00',
    isPaying: false,
    selectedMethod: 'balance',
    currentBalance: 0
  },

  onLoad(options) {
    if (options.type === 'checkout') {
      this.initCheckout();
      this.loadBalance();
    } else {
      this.loadOrders();
    }
  },

  initCheckout() {
    const checkoutData = wx.getStorageSync('checkoutData');
    if (checkoutData) {
      this.setData({
        isCheckout: true,
        checkoutItems: checkoutData.items,
        totalPrice: checkoutData.totalAmount || checkoutData.totalPrice || '0.00'
      });
    } else {
      wx.showToast({
        title: '购物车为空',
        icon: 'none'
      });
      setTimeout(() => {
        wx.switchTab({
          url: '/pages/cart/cart'
        });
      }, 1500);
    }
  },

  loadOrders() {
    wx.showLoading({
      title: '加载中...',
      mask: true
    });

    get('/applet/order/list', {}, false)
      .then(res => {
        wx.hideLoading();
        const orders = res.data || [];
        const formattedOrders = orders.map(order => ({
          id: order.id,
          orderId: order.orderId || order.id,
          status: this.mapStatus(order.status),
          statusText: this.getStatusText(order.status),
          statusClass: this.getStatusClass(order.status),
          goods: (order.goods || []).map(item => ({
            id: item.goodsId,
            name: item.goodsName,
            price: item.goodsPrice,
            count: item.quantity,
            image: app.getImageUrl(item.goodsImage)
          })),
          totalPrice: order.totalAmount ? order.totalAmount.toString() : '0.00',
          createTime: order.createTime || ''
        }));

        // 真机图片预下载
        var imgTasks = [];
        formattedOrders.forEach(function (order) {
          (order.goods || []).forEach(function (g) {
            imgTasks.push(app.resolveDisplayImage(g.image).then(function (image) {
              g.image = image;
            }));
          });
        });
        Promise.all(imgTasks).then(() => {
          this.setData({ allOrders: formattedOrders, orderList: formattedOrders });
          this.updateStatusCounts();
        });
      })
      .catch(() => {
        wx.hideLoading();
        this.setData({
          allOrders: [],
          orderList: []
        });
      });
  },

  mapStatus(status) {
    const statusMap = {
      'PENDING': 'pending',
      'PAID': 'paid',
      'COMPLETED': 'completed',
      'CANCELLED': 'cancelled'
    };
    return statusMap[status] || 'pending';
  },

  getStatusText(status) {
    const textMap = {
      'PENDING': '待付款',
      'PAID': '待取货',
      'COMPLETED': '已完成',
      'CANCELLED': '已取消'
    };
    return textMap[status] || '未知状态';
  },

  getStatusClass(status) {
    const classMap = {
      'PENDING': 'status-pending',
      'PAID': 'status-paid',
      'COMPLETED': 'status-completed',
      'CANCELLED': 'status-cancelled'
    };
    return classMap[status] || 'status-pending';
  },

  updateStatusCounts() {
    const orders = this.data.orderList;
    const counts = {
      all: orders.length,
      pending: orders.filter(o => o.status === 'pending').length,
      completed: orders.filter(o => o.status === 'completed').length
    };

    const tabs = this.data.statusTabs.map(tab => ({
      ...tab,
      count: counts[tab.value] || 0
    }));

    this.setData({
      statusTabs: tabs
    });
  },

  changeStatus(e) {
    const status = e.currentTarget.dataset.status;
    const allOrders = this.data.allOrders;

    let filteredOrders = allOrders;
    if (status !== 'all') {
      filteredOrders = allOrders.filter(order => order.status === status);
    }

    this.setData({
      currentStatus: status,
      orderList: filteredOrders
    });
  },

  cancelOrder: debounce(function (e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '提示',
      content: '确定要取消该订单吗？',
      success: (res) => {
        if (res.confirm) {
          wx.showLoading({ title: '取消中...', mask: true });
          post('/applet/order/cancel', { orderId: id }, false)
            .then(() => {
              wx.hideLoading();
              const allOrders = this.data.allOrders.filter(order => order.id !== id);
              let orderList = allOrders;
              if (this.data.currentStatus !== 'all') {
                orderList = allOrders.filter(order => order.status === this.data.currentStatus);
              }
              this.setData({ allOrders, orderList });
              this.updateStatusCounts();
              wx.showToast({ title: '订单已取消', icon: 'success' });
            })
            .catch(() => {
              wx.hideLoading();
              wx.showToast({ title: '取消失败', icon: 'none' });
            });
        }
      }
    });
  }),

  payOrder: debounce(function (e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/payment/payment?orderId=' + id
    });
  }),

  selectMethod(e) {
    this.setData({ selectedMethod: e.currentTarget.dataset.method });
  },

  loadBalance() {
    get('/applet/user/info', {}, false)
      .then(res => {
        const data = res.data || {};
        this.setData({ currentBalance: data.balance || 0 });
      })
      .catch(() => {});
  },

  // 确认支付：结算 + 立即付款
  confirmPayment: debounce(function () {
    if (this.data.isPaying) return;

    const totalPrice = parseFloat(this.data.totalPrice);
    const method = this.data.selectedMethod;

    if (method === 'balance' && this.data.currentBalance < totalPrice) {
      wx.showToast({ title: '余额不足，请选择其他方式', icon: 'none' });
      return;
    }

    this.setData({ isPaying: true });
    wx.showLoading({ title: '支付中...', mask: true });

    const that = this;
    var checkoutPayload = { items: (this.data.checkoutItems || []).map(function (item) { return { id: item.id, count: item.count }; }) };
    // 1. 结算
    post('/applet/order/checkout', checkoutPayload, false)
      .then(res => {
        const orderId = res.data;
        // 2. 立即付款
        return post('/applet/order/pay', { orderId, paymentMethod: method }, false)
          .then(() => orderId);
      })
      .then(orderId => {
        wx.hideLoading();
        that.setData({ isPaying: false });

        app.clearCart();
        wx.removeStorageSync('checkoutData');

        // 同步最新余额
        if (method === 'balance') {
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

        wx.showToast({ title: '支付成功', icon: 'success', duration: 1500 });
        setTimeout(() => { wx.switchTab({ url: '/pages/user/user' }); }, 1500);
      })
      .catch(err => {
        wx.hideLoading();
        that.setData({ isPaying: false });
        wx.showToast({
          title: (err && (err.message || err.msg)) || '支付失败，请重试',
          icon: 'none',
          duration: 2000
        });
      });
  }),

  // 稍后支付：仅结算，订单留在待付款
  cancelPayment: debounce(function () {
    if (this.data.isPaying) return;

    this.setData({ isPaying: true });
    wx.showLoading({ title: '提交中...', mask: true });

    const that = this;
    var checkoutPayload = { items: (this.data.checkoutItems || []).map(function (item) { return { id: item.id, count: item.count }; }) };
    post('/applet/order/checkout', checkoutPayload, false)
      .then(res => {
        wx.hideLoading();
        that.setData({ isPaying: false });
        const orderId = res.data;

        app.clearCart();
        wx.removeStorageSync('checkoutData');

        wx.showToast({ title: '订单已生成，待付款', icon: 'none', duration: 2000 });
        setTimeout(() => { wx.switchTab({ url: '/pages/user/user' }); }, 2000);
      })
      .catch(err => {
        wx.hideLoading();
        that.setData({ isPaying: false });
        wx.showToast({
          title: (err && (err.message || err.msg)) || '提交失败',
          icon: 'none',
          duration: 2000
        });
      });
  }),

});
