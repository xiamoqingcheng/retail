package com.retail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.server.entity.Goods;
import com.retail.server.entity.GoodsCategory;
import com.retail.server.exception.BusinessException;
import com.retail.server.mapper.GoodsCategoryMapper;
import com.retail.server.mapper.GoodsMapper;
import com.retail.server.mapper.InventoryLogMapper;
import com.retail.server.mapper.OrderItemMapper;
import com.retail.server.mapper.OrderMapper;
import com.retail.server.mapper.UserBehaviorEventMapper;
import com.retail.server.mapper.WarningMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 销售报表内容生成服务。给定时间区间 [start, end)，聚合产出商业报表所需的全部板块，
 * 并与「上一个等长周期」对比给出环比增长额/增长率。
 */
@Service
public class ReportGenerationService {

    private static final DateTimeFormatter TITLE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int DEFAULT_SAFE_STOCK = 10;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GoodsMapper goodsMapper;
    private final GoodsCategoryMapper goodsCategoryMapper;
    private final WarningMapper warningMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final UserBehaviorEventMapper behaviorMapper;

    public ReportGenerationService(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                                   GoodsMapper goodsMapper, GoodsCategoryMapper goodsCategoryMapper,
                                   WarningMapper warningMapper, InventoryLogMapper inventoryLogMapper,
                                   UserBehaviorEventMapper behaviorMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.goodsMapper = goodsMapper;
        this.goodsCategoryMapper = goodsCategoryMapper;
        this.warningMapper = warningMapper;
        this.inventoryLogMapper = inventoryLogMapper;
        this.behaviorMapper = behaviorMapper;
    }

    /**
     * 生成 [start, end) 区间的报表内容（LinkedHashMap，保持板块顺序，便于序列化/导出）。
     */
    public Map<String, Object> generate(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BusinessException(400, "报表时间区间非法：结束时间必须晚于开始时间");
        }

        Duration window = Duration.between(start, end);
        LocalDateTime prevStart = start.minus(window);
        LocalDateTime prevEnd = start;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("title", "销售经营报表 " + start.format(TITLE_FMT) + " ~ " + end.format(TITLE_FMT));
        report.put("periodStart", start.toString());
        report.put("periodEnd", end.toString());
        report.put("generatedAt", LocalDateTime.now().toString());

        report.put("overview", buildOverview(start, end, prevStart, prevEnd));

        // 商品销售明细 + 热销榜
        List<Map<String, Object>> goodsSales = orderItemMapper.selectGoodsSalesInRange(start, end);
        BigDecimal goodsTotalAmount = sumAmount(goodsSales);
        for (Map<String, Object> g : goodsSales) {
            g.put("ratio", ratioPercent(bd(g.get("amount")), goodsTotalAmount));
        }
        report.put("goodsSales", goodsSales);
        report.put("hotGoods", goodsSales.stream().limit(10).collect(Collectors.toList()));

        // 分类销售占比
        List<Map<String, Object>> categorySales = orderItemMapper.selectCategorySalesInRange(start, end);
        BigDecimal catTotalAmount = sumAmount(categorySales);
        for (Map<String, Object> c : categorySales) {
            c.put("ratio", ratioPercent(bd(c.get("amount")), catTotalAmount));
        }
        report.put("categorySales", categorySales);

        // 销售趋势 + 时段分布
        report.put("trend", orderMapper.selectDailyTrendInRange(start, end));
        report.put("hourly", buildHourly(start, end));

        // 库存健康 / 补货建议 / 滞销（基于当前库存快照）
        List<Goods> activeGoods = goodsMapper.selectList(new LambdaQueryWrapper<Goods>().eq(Goods::getStatus, 1));
        Map<Long, String> catNames = categoryNames();
        Map<Long, Long> soldQtyByGoods = goodsSales.stream().collect(Collectors.toMap(
                m -> lng(m.get("goodsId")), m -> lng(m.get("quantity")), (a, b) -> a));
        Set<Long> soldGoodsIds = soldQtyByGoods.keySet();

        report.put("inventory", buildInventory(activeGoods, catNames));
        report.put("replenishment", buildReplenishment(activeGoods, catNames, soldQtyByGoods));
        report.put("slowGoods", buildSlowGoods(activeGoods, catNames, soldGoodsIds));

        // 异常与告警
        report.put("warnings", warningMapper.selectInRange(start, end));
        report.put("restock", inventoryLogMapper.selectRestockInRange(start, end));
        report.put("anomalies", buildAnomalies(start, end));

        return report;
    }

    private Map<String, Object> buildOverview(LocalDateTime start, LocalDateTime end,
                                              LocalDateTime prevStart, LocalDateTime prevEnd) {
        Map<String, Object> cur = orderMapper.selectRangeSummary(start, end);
        Map<String, Object> prev = orderMapper.selectRangeSummary(prevStart, prevEnd);
        long curUnits = nz(orderItemMapper.selectUnitsInRange(start, end));
        long prevUnits = nz(orderItemMapper.selectUnitsInRange(prevStart, prevEnd));

        BigDecimal revenue = bd(cur.get("revenue"));
        BigDecimal prevRevenue = bd(prev.get("revenue"));
        long orderCount = lng(cur.get("orderCount"));
        long prevOrderCount = lng(prev.get("orderCount"));
        long visitors = lng(cur.get("visitors"));
        long prevVisitors = lng(prev.get("visitors"));

        BigDecimal avgOrder = orderCount > 0
                ? revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal prevAvgOrder = prevOrderCount > 0
                ? prevRevenue.divide(BigDecimal.valueOf(prevOrderCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        Map<String, Object> overview = new LinkedHashMap<>();
        putMetric(overview, "revenue", revenue, prevRevenue);
        putMetric(overview, "orderCount", BigDecimal.valueOf(orderCount), BigDecimal.valueOf(prevOrderCount));
        putMetric(overview, "visitors", BigDecimal.valueOf(visitors), BigDecimal.valueOf(prevVisitors));
        putMetric(overview, "unitsSold", BigDecimal.valueOf(curUnits), BigDecimal.valueOf(prevUnits));
        putMetric(overview, "avgOrderValue", avgOrder, prevAvgOrder);
        overview.put("cancelledCount", lng(cur.get("cancelledCount")));
        overview.put("cancelledAmount", bd(cur.get("cancelledAmount")));
        return overview;
    }

    /** 写入一个指标：当前值 + 环比增长额 + 环比增长率。 */
    private void putMetric(Map<String, Object> target, String key, BigDecimal current, BigDecimal previous) {
        target.put(key, current);
        target.put(key + "Growth", current.subtract(previous));
        target.put(key + "GrowthRate", growthRate(current, previous));
    }

    private Map<String, Object> buildInventory(List<Goods> activeGoods, Map<Long, String> catNames) {
        List<Map<String, Object>> zeroStock = new ArrayList<>();
        List<Map<String, Object>> lowStock = new ArrayList<>();
        BigDecimal totalStockValue = BigDecimal.ZERO;
        for (Goods g : activeGoods) {
            int stock = g.getStock() != null ? g.getStock() : 0;
            int safe = safeStockOf(g);
            if (g.getPrice() != null) {
                totalStockValue = totalStockValue.add(g.getPrice().multiply(BigDecimal.valueOf(stock)));
            }
            if (stock <= 0) {
                zeroStock.add(goodsInvRow(g, catNames));
            } else if (stock < safe) {
                lowStock.add(goodsInvRow(g, catNames));
            }
        }
        lowStock.sort(Comparator.comparingInt(m -> intVal(m.get("stock"))));
        Map<String, Object> inventory = new LinkedHashMap<>();
        inventory.put("skuCount", activeGoods.size());
        inventory.put("totalStockValue", totalStockValue);
        inventory.put("zeroStockCount", zeroStock.size());
        inventory.put("lowStockCount", lowStock.size());
        inventory.put("zeroStock", zeroStock);
        inventory.put("lowStock", lowStock);
        return inventory;
    }

    private List<Map<String, Object>> buildReplenishment(List<Goods> activeGoods, Map<Long, String> catNames,
                                                         Map<Long, Long> soldQtyByGoods) {
        List<Map<String, Object>> replenishment = new ArrayList<>();
        for (Goods g : activeGoods) {
            int stock = g.getStock() != null ? g.getStock() : 0;
            int safe = safeStockOf(g);
            if (stock >= safe) {
                continue;
            }
            long sold = soldQtyByGoods.getOrDefault(g.getId(), 0L);
            // 建议补货量 = 补到安全库存的缺口 + 覆盖本期销量（粗略销售速度）
            int suggest = Math.max(safe - stock, 0) + (int) sold;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("goodsId", g.getId());
            m.put("name", g.getName());
            m.put("stock", stock);
            m.put("safeStock", safe);
            m.put("soldInWindow", sold);
            m.put("suggestQty", suggest);
            m.put("categoryName", catNames.getOrDefault(g.getCategoryId(), "未分类"));
            replenishment.add(m);
        }
        replenishment.sort(Comparator.comparingInt((Map<String, Object> m) -> intVal(m.get("suggestQty"))).reversed());
        return replenishment;
    }

    private List<Map<String, Object>> buildSlowGoods(List<Goods> activeGoods, Map<Long, String> catNames,
                                                     Set<Long> soldGoodsIds) {
        return activeGoods.stream()
                .filter(g -> !soldGoodsIds.contains(g.getId()))
                .sorted(Comparator.comparingInt((Goods g) -> g.getStock() != null ? g.getStock() : 0).reversed())
                .limit(30)
                .map(g -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("goodsId", g.getId());
                    m.put("name", g.getName());
                    m.put("stock", g.getStock());
                    m.put("categoryName", catNames.getOrDefault(g.getCategoryId(), "未分类"));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildAnomalies(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> cur = orderMapper.selectRangeSummary(start, end);
        Map<String, Object> anomalies = new LinkedHashMap<>();
        Map<String, Object> cancelled = new LinkedHashMap<>();
        cancelled.put("count", lng(cur.get("cancelledCount")));
        cancelled.put("amount", bd(cur.get("cancelledAmount")));
        anomalies.put("cancelledOrders", cancelled);
        anomalies.put("inventoryAdjust", inventoryLogMapper.selectAdjustInRange(start, end));
        anomalies.put("behavior", behaviorMapper.countByTypeInRange(start, end));
        return anomalies;
    }

    private List<Map<String, Object>> buildHourly(LocalDateTime start, LocalDateTime end) {
        Map<Integer, Long> map = orderMapper.selectHourlyVisitors(start, end).stream().collect(Collectors.toMap(
                r -> intVal(r.get("h")), r -> lng(r.get("visitors")), (a, b) -> a));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("hour", String.format("%02d:00", h));
            m.put("visitors", map.getOrDefault(h, 0L));
            result.add(m);
        }
        return result;
    }

    private Map<Long, String> categoryNames() {
        Map<Long, String> map = new HashMap<>();
        for (GoodsCategory c : goodsCategoryMapper.selectList(new LambdaQueryWrapper<GoodsCategory>())) {
            map.put(c.getId(), c.getName());
        }
        return map;
    }

    private Map<String, Object> goodsInvRow(Goods g, Map<Long, String> catNames) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("goodsId", g.getId());
        m.put("name", g.getName());
        m.put("stock", g.getStock());
        m.put("safeStock", safeStockOf(g));
        m.put("price", g.getPrice());
        m.put("categoryName", catNames.getOrDefault(g.getCategoryId(), "未分类"));
        return m;
    }

    private int safeStockOf(Goods g) {
        return g.getSafeStock() != null && g.getSafeStock() > 0 ? g.getSafeStock() : DEFAULT_SAFE_STOCK;
    }

    // ============== 数值工具 ==============

    private BigDecimal sumAmount(List<Map<String, Object>> rows) {
        return rows.stream().map(m -> bd(m.get("amount"))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 环比增长率（保留 4 位小数，0.1234 表示 +12.34%）。基期为 0 时返回 null（前端展示「—/新增」）。 */
    private BigDecimal growthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP);
    }

    /** 占比百分比（保留 2 位小数，55.66 表示 55.66%）。 */
    private BigDecimal ratioPercent(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal bd(Object o) {
        if (o instanceof BigDecimal b) {
            return b;
        }
        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private long lng(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private int intVal(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    private long nz(Long o) {
        return o == null ? 0L : o;
    }
}
