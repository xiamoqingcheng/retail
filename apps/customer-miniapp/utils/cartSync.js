/**
 * 购物车合并工具：将后端购物车与本地购物车合并。
 *
 * 合并规则：本地条目优先，本地有的保留本地的数量/名称/价格/图片，
 *           后端有而本地没有的补入本地。
 *
 * @param {Array}  backendItems - 后端返回的购物车条目列表
 * @param {Array}  localCart    - 本地缓存的购物车列表
 * @param {Function} getImageUrl - 图片 URL 转换函数 (relativePath => fullUrl)
 * @returns {Array} 合并后的购物车列表
 */
function mergeCart(backendItems, localCart, getImageUrl) {
  var map = {};

  // 1. 后端条目入 map
  (backendItems || []).forEach(function (item) {
    var goodsId = item.goodsId;
    map[goodsId] = {
      id: goodsId,
      name: item.goodsName || '',
      price: Number(item.price || 0),
      count: Number(item.quantity || 1),
      image: getImageUrl(item.imageUrl || item.goodsImage),
      selected: true
    };
  });

  // 2. 本地条目覆盖/补充 map
  (localCart || []).forEach(function (item) {
    var existing = map[item.id];
    map[item.id] = {
      id: item.id,
      name: item.name || (existing ? existing.name : ''),
      price: Number(item.price || (existing ? existing.price : 0)),
      count: Number(item.count || 1),
      image: item.image || (existing ? existing.image : '/assets/goods/default.png'),
      selected: item.selected !== false
    };
  });

  // 3. 过滤零数量项，转数组
  return Object.keys(map).reduce(function (result, key) {
    var entry = map[key];
    if (entry.count > 0) {
      result.push(entry);
    }
    return result;
  }, []);
}

module.exports = { mergeCart: mergeCart };
