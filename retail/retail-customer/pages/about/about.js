const request = require('../../utils/request');

Page({
  data: {
    versionText: 'v1.0.0',
    envText: 'develop',
    apiBaseUrl: '',
    featureList: [
      '智能商品识别',
      '商品搜索与详情',
      '购物车与订单结算',
      '会员余额充值',
      '管理端商品与库存维护'
    ],
    techList: [
      '微信小程序',
      'Spring Boot API',
      'MySQL / Redis',
      'FastAPI AI 服务'
    ]
  },

  onShow() {
    this.loadAppMeta();
  },

  loadAppMeta() {
    let versionText = 'v1.0.0';
    let envText = 'develop';

    try {
      const accountInfo = wx.getAccountInfoSync && wx.getAccountInfoSync();
      if (accountInfo && accountInfo.miniProgram) {
        versionText = accountInfo.miniProgram.version || versionText;
        envText = accountInfo.miniProgram.envVersion || envText;
      }
    } catch (e) {}

    this.setData({
      versionText,
      envText,
      apiBaseUrl: request.getBaseUrl()
    });
  },

  copyVersionInfo() {
    const text = [
      '零售物品智能识别系统',
      '版本: ' + this.data.versionText,
      '环境: ' + this.data.envText,
      'API: ' + this.data.apiBaseUrl
    ].join('\n');

    wx.setClipboardData({
      data: text
    });
  },

  goSettings() {
    wx.navigateTo({
      url: '/pages/settings/settings'
    });
  },

  goHelp() {
    wx.navigateTo({
      url: '/pages/help/help'
    });
  }
});
