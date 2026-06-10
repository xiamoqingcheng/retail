const app = getApp();
const { get, post } = require('../../utils/request');
const { debounce, throttle } = require('../../utils/utils');

const PRICE_RANGES = [
  { label: '全部', min: -1, max: -1 },
  { label: '¥0-5', min: 0, max: 5 },
  { label: '¥5-10', min: 5, max: 10 },
  { label: '¥10-20', min: 10, max: 20 },
  { label: '¥20-50', min: 20, max: 50 },
  { label: '¥50+', min: 50, max: Infinity }
];

Page({
  data: {
    searchKey: '',
    searchFocus: false,
    showHistory: false,
    hotSearch: ['农夫山泉', '奥利奥', '可口可乐', '士力架', '百事可乐', '绿箭'],
    historySearch: [],
    goodsList: [],
    recommendations: [],
    categories: [],
    currentCategoryId: 0,
    priceRanges: PRICE_RANGES,
    currentPriceIdx: 0,
    sortOrder: 0
  },

  onLoad() {
    const historySearch = wx.getStorageSync('historySearch') || [];
    this.setData({ historySearch: historySearch });
    this.loadCategories();
    this.loadRecommendations();
  },

  loadCategories() {
    get('/goods/categories', {}, false)
      .then(res => {
        const categories = res.data || [];
        this.setData({ categories: categories });
      })
      .catch(() => {});
  },

  onCategoryTap(e) {
    const id = Number(e.currentTarget.dataset.id || 0);
    this.setData({ currentCategoryId: id });
    if (this.data.searchKey) {
      this.performSearch(this.data.searchKey);
    } else {
      this.loadGoodsByCategory(id);
    }
  },

  onPriceFilterTap(e) {
    const idx = Number(e.currentTarget.dataset.idx || 0);
    this.setData({ currentPriceIdx: idx });
    if (this.data.searchKey) {
      this.performSearch(this.data.searchKey);
    } else {
      this.loadGoodsByCategory(this.data.currentCategoryId);
    }
  },

  onSortTap(e) {
    const order = Number(e.currentTarget.dataset.order);
    this.setData({ sortOrder: order });
    if (this.data.searchKey) {
      this.performSearch(this.data.searchKey);
    } else {
      this.loadGoodsByCategory(this.data.currentCategoryId);
    }
  },

  filterAndSort(list) {
    const range = PRICE_RANGES[this.data.currentPriceIdx];
    let result = list;
    if (range && range.min >= 0) {
      result = list.filter(item => {
        const p = Number(item.price || 0);
        return p >= range.min && p < range.max;
      });
    }
    if (this.data.sortOrder === 1) {
      result.sort((a, b) => Number(a.price || 0) - Number(b.price || 0));
    } else if (this.data.sortOrder === 2) {
      result.sort((a, b) => Number(b.price || 0) - Number(a.price || 0));
    }
    return result;
  },

  loadGoodsByCategory(categoryId) {
    wx.showLoading({ title: '加载中...', mask: true });
    const params = { page: 1, size: 50 };
    if (categoryId > 0) params.categoryId = categoryId;
    get('/goods/page', params, false)
      .then(res => {
        wx.hideLoading();
        const records = (res.data && res.data.records) || [];
        const goodsList = records.map(item => ({
          id: item.id, name: item.name, price: item.price,
          image: item.imageUrl ? app.getImageUrl(item.imageUrl) : '/assets/goods/default.png',
          shelfId: item.shelfId, categoryName: item.categoryName || ''
        }));
        const filtered = this.filterAndSort(goodsList);
        app.cacheImages(filtered, 'image').then(() => {
          this.setData({ goodsList: filtered, showHistory: false });
        });
      })
      .catch(() => { wx.hideLoading(); });
  },

  onSearchFocus() {
    this.setData({
      showHistory: true,
      searchFocus: true
    });
  },

  onSearchInput(e) {
    this.setData({
      searchKey: e.detail.value
    });

    if (e.detail.value) {
      this.throttleSearch(e.detail.value);
    } else {
      this.setData({
        goodsList: []
      });
    }
  },

  throttleSearch: throttle(function (value) {
    this.performSearch(value);
  }, 500),

  onSearch: debounce(function () {
    const searchKey = this.data.searchKey;
    if (searchKey) {
      this.performSearch(searchKey);
    }
  }),

  performSearch(key) {
    this.saveSearchHistory(key);
    this.setData({
      showHistory: false
    });

    wx.showLoading({
      title: '搜索中...',
      mask: true
    });

    const trimmedKey = (key || '').trim();
    const params = { page: 1, size: 20, name: trimmedKey };
    if (this.data.currentCategoryId > 0) {
      params.categoryId = this.data.currentCategoryId;
    }

    get('/goods/page', params, false)
      .then(res => {
        wx.hideLoading();
        const records = (res.data && res.data.records) || [];
        if (records.length > 0) {
          const lowerKey = trimmedKey.toLowerCase();
          const sorted = records
            .map(item => ({
              id: item.id, name: item.name, price: item.price,
              image: item.imageUrl ? app.getImageUrl(item.imageUrl) : '/assets/goods/default.png',
              shelfId: item.shelfId, categoryName: item.categoryName || '',
              _score: item.name && item.name.toLowerCase().includes(lowerKey) ? 1 : 0
            }))
            .sort((a, b) => b._score - a._score)
            .map(({ _score, ...rest }) => rest);
          const filtered = this.filterAndSort(sorted);
          app.cacheImages(filtered, 'image').then(() => {
            this.setData({ goodsList: filtered });
          });
        } else {
          this.setData({ goodsList: [] });
          wx.showToast({ title: '未找到相关商品', icon: 'none' });
        }
      })
      .catch((err) => {
        wx.hideLoading();
        wx.showToast({
          title: '网络连接失败，请检查网络',
          icon: 'none',
          duration: 2000
        });
        console.error('搜索失败:', err);
      });
  },

  loadRecommendations() {
    get('/applet/home/recommend', { k: 10 }, false)
      .then(res => {
        const goodsIds = res.data || [];
        if (goodsIds.length > 0) {
          return this.loadRecommendDetails(goodsIds);
        }
      })
      .catch((err) => {
        console.error('加载推荐失败:', err);
      });
  },

  loadRecommendDetails(goodsIds) {
    return post('/applet/goods/listByIds', goodsIds, false)
      .then(res => {
        const recommendations = (res.data || []).map(item => ({
          id: item.id,
          name: item.name,
          price: item.price,
          image: app.getImageUrl(item.imageUrl),
          shelfId: item.shelfId
        }));
        app.cacheImages(recommendations, 'image').then(() => {
          this.setData({ recommendations: recommendations });
        });
      })
      .catch((err) => {
        console.error('加载推荐详情失败:', err);
      });
  },

  saveSearchHistory(key) {
    let history = this.data.historySearch;
    history = history.filter(item => item !== key);
    history.unshift(key);
    if (history.length > 10) {
      history = history.slice(0, 10);
    }
    this.setData({
      historySearch: history
    });
    wx.setStorageSync('historySearch', history);
  },

  clearHistory() {
    wx.showModal({
      title: '提示',
      content: '确定要清除搜索历史吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            historySearch: []
          });
          wx.removeStorageSync('historySearch');
        }
      }
    });
  },

  onHotSearch(e) {
    const key = e.currentTarget.dataset.key;
    this.setData({
      searchKey: key,
      showHistory: false
    });
    this.performSearch(key);
  },

  onHistorySearch(e) {
    const key = e.currentTarget.dataset.key;
    this.setData({
      searchKey: key,
      showHistory: false
    });
    this.performSearch(key);
  },

  cancelSearch() {
    this.setData({
      searchKey: '',
      goodsList: [],
      searchFocus: false,
      showHistory: false,
      currentCategoryId: 0,
      currentPriceIdx: 0,
      sortOrder: 0
    });
  },

  viewGoodsDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: '/pages/goods/goods?id=' + id
    });
  },

  _resolveArr(listName) {
    return listName === 'recommendations' ? this.data.recommendations : this.data.goodsList;
  },

  _resolveKey(listName) {
    return listName === 'recommendations' ? 'recommendations' : 'goodsList';
  },

  toggleQuantitySelector(e) {
    const id = e.currentTarget.dataset.id;
    const listName = e.currentTarget.dataset.list || 'goodsList';
    const arr = this._resolveArr(listName);
    const index = arr.findIndex(item => item.id === id);
    if (index > -1) {
      arr[index].showQuantitySelector = true;
      arr[index].quantity = 1;
      this.setData({ [this._resolveKey(listName)]: arr });
      app.addToCart(arr[index], 1);
    }
  },

  decreaseQuantity(e) {
    const id = e.currentTarget.dataset.id;
    const listName = e.currentTarget.dataset.list || 'goodsList';
    const arr = this._resolveArr(listName);
    const index = arr.findIndex(item => item.id === id);
    if (index > -1 && arr[index].quantity > 0) {
      arr[index].quantity -= 1;
      if (arr[index].quantity <= 0) {
        arr[index].showQuantitySelector = false;
        delete arr[index].quantity;
        app.removeFromCart(id);
      } else {
        app.updateCartCount(id, arr[index].quantity);
      }
      this.setData({ [this._resolveKey(listName)]: arr });
    }
  },

  increaseQuantity(e) {
    const id = e.currentTarget.dataset.id;
    const listName = e.currentTarget.dataset.list || 'goodsList';
    const arr = this._resolveArr(listName);
    const index = arr.findIndex(item => item.id === id);
    if (index > -1) {
      arr[index].quantity = (arr[index].quantity || 1) + 1;
      app.updateCartCount(id, arr[index].quantity);
      this.setData({ [this._resolveKey(listName)]: arr });
    }
  }
});
