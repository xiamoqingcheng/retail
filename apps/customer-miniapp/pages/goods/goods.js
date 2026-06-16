const app = getApp();
const { post } = require('../../utils/request');
const { debounce } = require('../../utils/utils');

Page({
  data: {
    goods: null,
    loading: true
  },

  onLoad(options) {
    if (options.id) {
      this.loadGoodsDetail(options.id);
    } else {
      wx.showToast({
        title: '商品信息加载失败',
        icon: 'none'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    }
  },

  loadGoodsDetail(id) {
    wx.showLoading({
      title: '加载中...',
      mask: true
    });

    post('/applet/goods/listByIds', [parseInt(id)], false)
      .then(res => {
        wx.hideLoading();
        const goodsList = res.data || [];
        if (goodsList.length > 0) {
          const item = goodsList[0];
          const goods = {
            id: item.id,
            name: item.name,
            price: item.price,
            image: app.getImageUrl(item.imageUrl),
            shelfId: item.shelfId
          };
          // 真机需要下载图片到本地再显示
          if (app.resolveDisplayImage) {
            app.resolveDisplayImage(goods.image).then(image => {
              goods.image = image;
              this.setData({ goods: goods, loading: false });
              this.recordViewBehavior(goods.id);
            });
          } else {
            this.setData({ goods: goods, loading: false });
            this.recordViewBehavior(goods.id);
          }
        } else {
          wx.showToast({
            title: '商品不存在',
            icon: 'none'
          });
          setTimeout(() => {
            wx.navigateBack();
          }, 1500);
        }
      })
      .catch(() => {
        wx.hideLoading();
        this.setData({
          loading: false
        });
        wx.showToast({
          title: '加载失败',
          icon: 'none'
        });
      });
  },

  addToCart: debounce(function () {
    const goods = this.data.goods;
    if (goods) {
      app.addToCart(goods);
      wx.showToast({
        title: '添加成功',
        icon: 'success',
        duration: 1500
      });
    }
  }),

  recordViewBehavior(goodsId) {
    const token = wx.getStorageSync('token');
    if (!token || !goodsId) return;
    post('/applet/recommend/behavior', {
      eventType: 'VIEW',
      goodsId: Number(goodsId)
    }, false).catch(() => {});
  },

  goBack() {
    wx.navigateBack();
  }
});
