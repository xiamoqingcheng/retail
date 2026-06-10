const request = require('../../utils/request');

Page({
  data: {
    feedbackTypes: ['功能异常', '商品/订单', '支付/余额', '连接问题', '其他建议'],
    feedbackTypeIndex: 0,
    feedbackContent: '',
    feedbackContact: '',
    faqList: [
      {
        question: '小程序连不上服务器怎么办？',
        answer: '先确认后端服务已启动，再到系统设置检测接口地址。真机调试时请把 localhost 改为电脑局域网 IP。'
      },
      {
        question: '登录后余额或订单没有刷新？',
        answer: '返回“我的”页面会自动同步余额；如果仍异常，可以退出后重新登录，或检查服务连接状态。'
      },
      {
        question: '商品图片显示不出来？',
        answer: '通常是接口地址或图片域名不一致导致，可在系统设置里保存当前服务地址后重新进入页面。'
      },
      {
        question: '购物车数据不一致？',
        answer: '登录后会尝试和后端同步；必要时可在系统设置中清理购物车缓存后重新添加商品。'
      }
    ]
  },

  onTypeChange(e) {
    this.setData({
      feedbackTypeIndex: Number(e.detail.value)
    });
  },

  onFeedbackInput(e) {
    this.setData({
      feedbackContent: e.detail.value
    });
  },

  onContactInput(e) {
    this.setData({
      feedbackContact: e.detail.value
    });
  },

  submitFeedback() {
    const content = (this.data.feedbackContent || '').trim();
    if (content.length < 6) {
      wx.showToast({
        title: '请补充更具体的问题',
        icon: 'none'
      });
      return;
    }

    const type = this.data.feedbackTypes[this.data.feedbackTypeIndex];
    const payload = {
      type,
      content,
      contact: (this.data.feedbackContact || '').trim(),
      apiBaseUrl: request.getBaseUrl(),
      systemInfo: this.getSystemText(),
      diagnosticInfo: this.getDiagnosticText()
    };

    wx.showLoading({
      title: '提交中...'
    });

    request.post('/applet/feedback', payload, false)
      .then(() => {
        this.setData({
          feedbackContent: '',
          feedbackContact: ''
        });
        wx.showToast({
          title: '反馈已提交',
          icon: 'success'
        });
      })
      .catch(() => {
        this.saveFeedbackDraft(payload);
        wx.showToast({
          title: '已离线保存',
          icon: 'none'
        });
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  copyDebugInfo() {
    let system = '未知设备';
    try {
      const info = wx.getSystemInfoSync();
      system = [info.brand, info.model, info.platform, info.SDKVersion].filter(Boolean).join(' / ');
    } catch (e) {}

    const text = [
      '智能零售小程序问题诊断',
      'API: ' + request.getBaseUrl(),
      '设备: ' + system,
      '登录: ' + (wx.getStorageSync('token') ? '已登录' : '未登录')
    ].join('\n');

    wx.setClipboardData({
      data: text
    });
  },

  getSystemText() {
    try {
      const info = wx.getSystemInfoSync();
      return [info.brand, info.model, info.platform, info.SDKVersion].filter(Boolean).join(' / ');
    } catch (e) {
      return '未知设备';
    }
  },

  getDiagnosticText() {
    return [
      'API: ' + request.getBaseUrl(),
      '登录: ' + (wx.getStorageSync('token') ? '已登录' : '未登录'),
      '时间: ' + this.formatTime(new Date())
    ].join('\n');
  },

  saveFeedbackDraft(payload) {
    const feedbackList = wx.getStorageSync('feedbackDrafts') || [];
    feedbackList.unshift({
      ...payload,
      createdAt: this.formatTime(new Date())
    });
    wx.setStorageSync('feedbackDrafts', feedbackList.slice(0, 10));
  },

  formatTime(date) {
    const pad = (value) => String(value).padStart(2, '0');
    return [
      date.getFullYear(),
      pad(date.getMonth() + 1),
      pad(date.getDate())
    ].join('-') + ' ' + [pad(date.getHours()), pad(date.getMinutes())].join(':');
  },

  goSettings() {
    wx.navigateTo({
      url: '/pages/settings/settings'
    });
  },

  goLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
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
