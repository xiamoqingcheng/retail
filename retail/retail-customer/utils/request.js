// 开发环境 API 地址
// 模拟器：localhost 即可；真机调试可在“系统设置”中改成电脑局域网 IP。
var STORAGE_KEY = 'apiBaseUrl';
var DEFAULT_BASE_URL = 'https://zestfully-pushup-unraveled.ngrok-free.dev/api';

function normalizeBaseUrl(url) {
  if (!url || typeof url !== 'string') return DEFAULT_BASE_URL;

  var value = url.trim();
  if (!value) return DEFAULT_BASE_URL;
  if (value.indexOf('http://') !== 0 && value.indexOf('https://') !== 0) {
    value = 'http://' + value;
  }

  value = value.replace(/\/+$/, '');
  if (!/\/api$/i.test(value)) {
    value += '/api';
  }
  return value;
}

function isValidBaseUrl(url) {
  if (!url || typeof url !== 'string' || !url.trim()) return false;
  var value = normalizeBaseUrl(url);
  return /^https?:\/\/[^\s]+\/api$/i.test(value);
}

function syncAppBaseUrl(baseUrl) {
  try {
    var app = getApp();
    if (app && app.globalData) {
      app.globalData.apiBaseUrl = baseUrl;
    }
  } catch (e) {}
}

// 获取实际使用的 baseUrl
function getBaseUrl() {
  try {
    var app = getApp();
    if (app && app.globalData && app.globalData.apiBaseUrl) {
      return normalizeBaseUrl(app.globalData.apiBaseUrl);
    }
  } catch (e) {}

  try {
    var stored = wx.getStorageSync(STORAGE_KEY);
    if (stored) {
      var normalized = normalizeBaseUrl(stored);
      syncAppBaseUrl(normalized);
      return normalized;
    }
  } catch (e) {}

  return DEFAULT_BASE_URL;
}

function setBaseUrl(url) {
  var normalized = normalizeBaseUrl(url);
  wx.setStorageSync(STORAGE_KEY, normalized);
  syncAppBaseUrl(normalized);
  return normalized;
}

function resetBaseUrl() {
  wx.removeStorageSync(STORAGE_KEY);
  syncAppBaseUrl(DEFAULT_BASE_URL);
  return DEFAULT_BASE_URL;
}

// 连续网络失败计数器（避免真机上反复弹 toast）
var networkFailCount = 0;
var lastFailToastTime = 0;

function request(options) {
  return new Promise((resolve, reject) => {
    var token = wx.getStorageSync('token');
    var baseUrl = getBaseUrl();

    console.log('发送请求:', {
      url: baseUrl + options.url,
      method: options.method,
      data: options.data
    });

    wx.request({
      url: baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : '',
        'ngrok-skip-browser-warning': 'true',
        ...options.header
      },
      success: (res) => {
        if (options.loading !== false) {
          wx.hideLoading();
        }

        if (res.data.code === 200) {
          resolve(res.data);
        } else if (res.data.code === 401) {
          var hadToken = !!wx.getStorageSync('token');
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          if (hadToken) {
            wx.showToast({
              title: '登录已过期，请重新登录',
              icon: 'none'
            });
            var app = getApp();
            if (app && app.globalData) {
              app.globalData.userInfo = null;
            }
          }
          setTimeout(function () {
            var pages = getCurrentPages();
            var currentPage = pages[pages.length - 1];
            if (!hadToken || (currentPage && currentPage.route && currentPage.route.indexOf('login') === -1)) {
              wx.navigateTo({
                url: '/pages/login/login'
              });
            }
          }, 1500);
          reject(res.data);
        } else {
          wx.showToast({
            title: res.data.message || res.data.msg || '请求失败',
            icon: 'none'
          });
          reject(res.data);
        }
      },
      fail: (err) => {
        if (options.loading !== false) {
          wx.hideLoading();
        }
        console.error('请求失败:', err);
        // 真机调试时避免频繁弹 toast，10 秒内最多提示一次
        var now = Date.now();
        networkFailCount++;
        if (networkFailCount <= 3 || now - lastFailToastTime > 10000) {
          lastFailToastTime = now;
          wx.showToast({
            title: '服务器连接失败，请检查系统设置',
            icon: 'none',
            duration: 2500
          });
        }
        reject(err);
      }
    });
  });
}

function get(url, data, loading = true) {
  if (loading) {
    wx.showLoading({
      title: '加载中...',
      mask: true
    });
  }
  return request({
    url,
    method: 'GET',
    data,
    loading
  });
}

function post(url, data, loading = true) {
  if (loading) {
    wx.showLoading({
      title: '加载中...',
      mask: true
    });
  }
  return request({
    url,
    method: 'POST',
    data,
    loading
  });
}

function put(url, data, loading = true) {
  if (loading) {
    wx.showLoading({
      title: '加载中...',
      mask: true
    });
  }
  return request({
    url,
    method: 'PUT',
    data,
    loading
  });
}

function del(url, data, loading = true) {
  if (loading) {
    wx.showLoading({
      title: '加载中...',
      mask: true
    });
  }
  return request({
    url,
    method: 'DELETE',
    data,
    loading
  });
}

module.exports = {
  request,
  get,
  post,
  put,
  del,
  getBaseUrl,
  setBaseUrl,
  resetBaseUrl,
  normalizeBaseUrl,
  isValidBaseUrl,
  DEFAULT_BASE_URL,
  BASE_URL: DEFAULT_BASE_URL
};
