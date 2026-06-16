const app = getApp();
const { get } = require('../../utils/request');
const { mergeCart } = require('../../utils/cartSync');

Page({
  data: {
    cartList: [],
    totalPrice: 0,
    selectedCount: 0,
    allSelected: true,
    isEdit: false
  },

  onShow() {
    this.loadCart();
    app.refreshCartBadge();
    const token = wx.getStorageSync('token');
    if (token) {
      this.syncWithBackend();
    }
  },

  onLoad() {
    this.loadCart();
  },

  // ── 从后端获取购物车，合并到本地 ──
  syncWithBackend() {
    const that = this;
    get('/cart/list', {}, false)
      .then(function (res) {
        const backendItems = res.data || [];
        if (backendItems.length === 0) {
          app.refreshCartBadge();
          return;
        }

        const localCart = wx.getStorageSync('cartList') || [];
        // 如果本地购物车为空，说明用户已清空，清除后端残留数据避免恢复
        if (localCart.length === 0) {
          const { del } = require('../../utils/request');
          del('/cart/clear', {}, false).catch(function () {});
          app.refreshCartBadge();
          return;
        }

        const mergedList = mergeCart(backendItems, localCart, app.getImageUrl);
        app.globalData.cartList = mergedList;
        wx.setStorageSync('cartList', mergedList);
        app.refreshCartBadge();
        that.loadCart();
      })
      .catch(function () {});
  },

  // ── 加载购物车 ──
  loadCart() {
    const cartList = app.getCartList();
    const formattedList = (cartList || []).map(function (item) {
      const price = Number(item.price || 0);
      const count = Number(item.count || 1);
      return {
        id: item.id,
        goodsId: item.id,
        name: item.name || '未知商品',
        price: price,
        count: count,
        image: item.image || '/assets/goods/default.png',
        selected: item.selected !== false,
        formattedPrice: price.toFixed(2),
        formattedSubtotal: (price * count).toFixed(2)
      };
    });

    app.cacheImages(formattedList, 'image').then(() => {
      this.setData({ cartList: formattedList });
      this.calculateTotal();
    });
  },

  // ── 计算合计 ──
  _calcTotals(list) {
    let totalPrice = 0;
    let selectedCount = 0;
    let allSelected = true;
    let hasItems = false;
    (list || []).forEach(function (item) {
      if (item.selected) {
        totalPrice += (Number(item.price) || 0) * (Number(item.count) || 0);
        selectedCount += Number(item.count) || 0;
        hasItems = true;
      }
      allSelected = allSelected && item.selected;
    });
    return {
      totalPrice: totalPrice.toFixed(2),
      selectedCount: selectedCount,
      allSelected: hasItems ? allSelected : true
    };
  },

  calculateTotal() {
    this.setData(this._calcTotals(this.data.cartList));
  },

  // ── 切换编辑模式 ──
  toggleEdit() {
    this.setData({ isEdit: !this.data.isEdit });
  },

  // ── 单个选中/取消 ──
  toggleSelect(e) {
    const id = e.currentTarget.dataset.id;
    const cartList = this.data.cartList;
    const index = cartList.findIndex(function (item) { return item.id === id; });
    if (index > -1) {
      cartList[index].selected = !cartList[index].selected;
      const totals = this._calcTotals(cartList);
      this.setData(Object.assign({ cartList: cartList }, totals));
      this.saveCart();
    }
  },

  // ── 全选切换 ──
  toggleAll() {
    const cartList = this.data.cartList;
    const newSelected = !this.data.allSelected;
    cartList.forEach(function (item) { item.selected = newSelected; });
    const totals = this._calcTotals(cartList);
    this.setData(Object.assign({ cartList: cartList, allSelected: newSelected }, totals));
    this.saveCart();
  },

  // ── 保存购物车到全局和本地 ──
  saveCart() {
    const cartList = this.data.cartList;
    const simplified = cartList.map(function (item) {
      return {
        id: item.id,
        name: item.name,
        price: Number(item.price || 0),
        count: Number(item.count || 1),
        image: item.image,
        selected: item.selected
      };
    });
    app.globalData.cartList = simplified;
    wx.setStorageSync('cartList', simplified);
    app.refreshCartBadge();
  },

  // ── 减数量 ──
  decreaseCount(e) {
    const id = e.currentTarget.dataset.id;
    const cartList = this.data.cartList;
    const index = cartList.findIndex(function (item) { return item.id === id; });
    if (index > -1) {
      const newCount = (Number(cartList[index].count) || 1) - 1;
      if (newCount <= 0) {
        this.deleteItem({ currentTarget: { dataset: { id: id } } });
      } else {
        cartList[index].count = newCount;
        cartList[index].formattedSubtotal = ((Number(cartList[index].price) || 0) * newCount).toFixed(2);
        const totals = this._calcTotals(cartList);
        this.setData(Object.assign({ cartList: cartList }, totals));
        this.saveCart();
        app.updateCartCount(id, newCount);
      }
    }
  },

  // ── 加数量 ──
  increaseCount(e) {
    const id = e.currentTarget.dataset.id;
    const cartList = this.data.cartList;
    const index = cartList.findIndex(function (item) { return item.id === id; });
    if (index > -1) {
      const newCount = (Number(cartList[index].count) || 1) + 1;
      cartList[index].count = newCount;
      cartList[index].formattedSubtotal = ((Number(cartList[index].price) || 0) * newCount).toFixed(2);
      const totals = this._calcTotals(cartList);
      this.setData(Object.assign({ cartList: cartList }, totals));
      this.saveCart();
      app.updateCartCount(id, newCount);
    }
  },

  // ── 删除单项 ──
  deleteItem(e) {
    const id = e.currentTarget.dataset.id;
    const cartList = this.data.cartList.filter(function (item) { return item.id !== id; });
    const totals = this._calcTotals(cartList);
    this.setData(Object.assign({ cartList: cartList }, totals));
    this.saveCart();
    app.removeFromCart(id);
    wx.showToast({ title: '已删除', icon: 'success', duration: 1000 });
  },

  // ── 清空购物车 ──
  clearCart() {
    const that = this;
    wx.showModal({
      title: '确认清空',
      content: '确定要清空购物车吗？',
      success: function (res) {
        if (res.confirm) {
          that.setData({ cartList: [], totalPrice: '0.00', selectedCount: 0 });
          app.clearCart();
          wx.showToast({ title: '已清空', icon: 'success', duration: 1000 });
        }
      }
    });
  },

  // ── 去结算 ──
  goSettlement() {
    const cartList = this.data.cartList;
    const selectedItems = cartList.filter(function (item) { return item.selected; });
    if (selectedItems.length === 0) {
      wx.showToast({ title: '请选择商品', icon: 'none' });
      return;
    }
    const totalAmount = selectedItems.reduce(function (sum, item) {
      return sum + (Number(item.price) || 0) * (Number(item.count) || 0);
    }, 0);
    wx.setStorageSync('checkoutData', { items: selectedItems, totalAmount: totalAmount });
    wx.navigateTo({ url: '/pages/order/order?type=checkout' });
  },

  // ── 导航 ──
  goShopping() { wx.switchTab({ url: '/pages/search/search' }); },

  viewDetail(e) {
    wx.navigateTo({ url: '/pages/goods/goods?id=' + e.currentTarget.dataset.id });
  },
});
