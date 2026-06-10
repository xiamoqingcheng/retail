package com.retail.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.retail.server.common.Result;
import com.retail.server.entity.Goods;
import com.retail.server.entity.GoodsCategory;
import com.retail.server.entity.Order;
import com.retail.server.entity.Warning;
import com.retail.server.mapper.GoodsCategoryMapper;
import com.retail.server.mapper.GoodsMapper;
import com.retail.server.mapper.OrderItemMapper;
import com.retail.server.mapper.OrderMapper;
import com.retail.server.mapper.WarningMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final GoodsMapper goodsMapper;
    private final WarningMapper warningMapper;
    private final GoodsCategoryMapper goodsCategoryMapper;

    public AdminDashboardController(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                                   GoodsMapper goodsMapper, WarningMapper warningMapper,
                                   GoodsCategoryMapper goodsCategoryMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.goodsMapper = goodsMapper;
        this.warningMapper = warningMapper;
        this.goodsCategoryMapper = goodsCategoryMapper;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(defaultValue = "week") String range) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime todayStart = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(today, LocalTime.MAX);
        LocalDateTime yestStart = LocalDateTime.of(yesterday, LocalTime.MIN);
        LocalDateTime yestEnd = LocalDateTime.of(yesterday, LocalTime.MAX);

        // 今日 + 昨日销售额/订单数/客流量（单条聚合 SQL）
        Map<String, Object> summary = orderMapper.selectDailySummary(todayStart, todayEnd, yestStart, yestEnd);
        BigDecimal todaySales = summary.get("todaySales") instanceof BigDecimal b ? b : BigDecimal.ZERO;
        long todayOrderCount = summary.get("todayCount") instanceof Number n ? n.longValue() : 0;
        long todayVisitors = summary.get("todayVisitors") instanceof Number n ? n.longValue() : 0;
        BigDecimal yesterdaySales = summary.get("yestSales") instanceof BigDecimal b ? b : BigDecimal.ZERO;
        long yesterdayOrderCount = summary.get("yestCount") instanceof Number n ? n.longValue() : 0;
        long yesterdayVisitors = summary.get("yestVisitors") instanceof Number n ? n.longValue() : 0;

        // 销售趋势：按 range 参数查询近7日或近30日
        int trendDays = "month".equals(range) ? 30 : 7;
        LocalDateTime trendStart = LocalDateTime.of(today.minusDays(trendDays - 1), LocalTime.MIN);
        List<Map<String, Object>> trendRows = orderMapper.selectMaps(new QueryWrapper<Order>()
                .select("DATE(create_time) as order_date",
                        "SUM(total_amount) as total_sales",
                        "COUNT(*) as order_count")
                .ge("create_time", trendStart)
                .le("create_time", todayEnd)
                .groupBy("DATE(create_time)")
                .orderByAsc("order_date"));
        Map<String, Map<String, Object>> trendMap = new LinkedHashMap<>();
        for (Map<String, Object> row : trendRows) {
            String date = row.get("order_date") != null ? row.get("order_date").toString() : "";
            trendMap.put(date, row);
        }
        List<Map<String, Object>> trendData = new ArrayList<>();
        for (int i = trendDays - 1; i >= 0; i--) {
            String dateKey = today.minusDays(i).toString();
            Map<String, Object> row = trendMap.get(dateKey);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateKey);
            item.put("sales", row != null ? row.get("total_sales") : BigDecimal.ZERO);
            item.put("orders", row != null ? ((Number) row.get("order_count")).intValue() : 0);
            trendData.add(item);
        }

        // 热销商品 TOP 8（近7日订单商品汇总，使用 SQL 聚合替代 N+1 查询）
        List<Map<String, Object>> hotGoods = orderItemMapper.selectHotGoodsTop8();

        // 库存告警统计（使用每个商品自身的 safeStock 阈值，safeStock=0 时回退到默认值 10）
        List<Goods> allActiveGoods = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, 1));
        List<Map<String, Object>> urgentList = allActiveGoods.stream()
                .filter(g -> {
                    int stock = g.getStock() != null ? g.getStock() : 0;
                    int safe = g.getSafeStock() != null && g.getSafeStock() > 0 ? g.getSafeStock() : 10;
                    return stock < safe;
                })
                .sorted(Comparator.comparingInt(g -> g.getStock() != null ? g.getStock() : 0))
                .limit(10)
                .map(g -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", g.getId());
                    m.put("name", g.getName());
                    m.put("stock", g.getStock());
                    m.put("safeStock", g.getSafeStock());
                    m.put("categoryId", g.getCategoryId());
                    return m;
                }).collect(Collectors.toList());

        // 最近告警（仅保留近7天且商品仍存在的告警）
        List<Warning> warnings = warningMapper.selectList(new LambdaQueryWrapper<Warning>()
                .ge(Warning::getCreateTime, LocalDateTime.now().minusDays(7))
                .orderByDesc(Warning::getCreateTime)
                .last("LIMIT 50"));
        // 收集告警中的商品ID，批量查询存在的商品
        Set<Long> warningGoodsIds = warnings.stream()
                .map(Warning::getGoodsId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> validGoodsIds = Set.of();
        if (!warningGoodsIds.isEmpty()) {
            validGoodsIds = goodsMapper.selectBatchIds(warningGoodsIds).stream()
                    .map(Goods::getId).collect(Collectors.toSet());
        }
        Set<Long> finalValidGoodsIds = validGoodsIds;
        List<Map<String, Object>> alertList = warnings.stream()
                .filter(w -> w.getGoodsId() == null || finalValidGoodsIds.contains(w.getGoodsId()))
                .limit(10)
                .map(w -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", w.getId());
                    m.put("msg", w.getWarningMsg());
                    m.put("level", "OUT_OF_STOCK".equals(w.getWarningType()) ? "danger" : "warning");
                    m.put("time", w.getCreateTime() != null ? w.getCreateTime().toString() : "");
                    m.put("resolved", w.getStatus() != null && w.getStatus() == 1);
                    return m;
                }).collect(Collectors.toList());

        // 统计所有未解决告警数（非仅前10条）
        long alertCount = warnings.stream()
                .filter(w -> w.getGoodsId() == null || finalValidGoodsIds.contains(w.getGoodsId()))
                .filter(w -> w.getStatus() == null || w.getStatus() != 1)
                .count();

        // 今日分时段客流量
        List<Map<String, Object>> hourlyTraffic = buildHourlyTraffic(today, yesterday);

        // 分类库存健康度（按分类汇总在售商品库存 vs 安全库存）
        List<GoodsCategory> allCategories = goodsCategoryMapper.selectList(
                new LambdaQueryWrapper<GoodsCategory>().eq(GoodsCategory::getStatus, 1));
        List<Map<String, Object>> categoryStockData = buildCategoryStockData(allActiveGoods, allCategories);

        // 分类销售占比
        List<Map<String, Object>> categoryPieData = buildCategoryPieDataFromSql(allCategories);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todaySales", todaySales);
        result.put("todayOrderCount", todayOrderCount);
        result.put("todayVisitors", todayVisitors);
        result.put("alertCount", alertCount);
        result.put("yesterdaySales", yesterdaySales);
        result.put("yesterdayOrderCount", yesterdayOrderCount);
        result.put("yesterdayVisitors", yesterdayVisitors);
        result.put("trendData", trendData);
        result.put("hotGoods", hotGoods);
        result.put("urgentList", urgentList);
        result.put("alertList", alertList);
        result.put("categoryPieData", categoryPieData);
        result.put("hourlyTraffic", hourlyTraffic);
        result.put("categoryStockData", categoryStockData);

        return Result.success(result);
    }

    private List<Map<String, Object>> buildCategoryPieDataFromSql(List<GoodsCategory> allCategories) {
        List<Map<String, Object>> rows = orderItemMapper.selectCategorySalesTop7();
        Map<String, Integer> nameToQty = new LinkedHashMap<>();
        for (GoodsCategory cat : allCategories) {
            nameToQty.put(cat.getName(), 0);
        }
        for (Map<String, Object> row : rows) {
            String name = row.getOrDefault("name", "未分类").toString();
            int qty = row.get("quantity") instanceof Number n ? n.intValue() : 0;
            nameToQty.put(name, nameToQty.getOrDefault(name, 0) + qty);
        }
        int totalQty = nameToQty.values().stream().mapToInt(Integer::intValue).sum();
        if (totalQty == 0) return List.of();
        return nameToQty.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", Math.round(e.getValue() * 100.0 / totalQty));
                    return m;
                }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildHourlyTraffic(LocalDate today, LocalDate yesterday) {
        LocalDateTime ts = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime te = LocalDateTime.of(today, LocalTime.MAX);
        LocalDateTime ys = LocalDateTime.of(yesterday, LocalTime.MIN);
        LocalDateTime ye = LocalDateTime.of(yesterday, LocalTime.MAX);
        Map<Integer, Long> tm = hourlyMap(orderMapper.selectHourlyVisitors(ts, te));
        Map<Integer, Long> ym = hourlyMap(orderMapper.selectHourlyVisitors(ys, ye));
        List<Map<String, Object>> result = new ArrayList<>();
        for (int h = 8; h <= 20; h++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("hour", h + ":00");
            m.put("today", tm.getOrDefault(h, 0L));
            m.put("yesterday", ym.getOrDefault(h, 0L));
            result.add(m);
        }
        return result;
    }

    private Map<Integer, Long> hourlyMap(List<Map<String, Object>> rows) {
        return rows.stream().collect(Collectors.toMap(
                r -> r.get("h") instanceof Number n ? n.intValue() : 0,
                r -> r.get("visitors") instanceof Number n ? n.longValue() : 0L
        ));
    }

    private List<Map<String, Object>> buildCategoryStockData(List<Goods> allActiveGoods, List<GoodsCategory> categories) {
        Map<Long, String> catNameMap = new HashMap<>();
        for (GoodsCategory cat : categories) {
            catNameMap.put(cat.getId(), cat.getName());
        }

        Map<Long, Integer> catStock = new LinkedHashMap<>();
        Map<Long, Integer> catSafeStock = new LinkedHashMap<>();
        for (Goods g : allActiveGoods) {
            Long cid = g.getCategoryId() != null ? g.getCategoryId() : 0L;
            catStock.merge(cid, g.getStock() != null ? g.getStock() : 0, Integer::sum);
            catSafeStock.merge(cid, g.getSafeStock() != null ? g.getSafeStock() : 0, Integer::sum);
        }

        int maxScale = Math.max(
                catStock.values().stream().mapToInt(Integer::intValue).max().orElse(100), 100);

        return catStock.keySet().stream()
                .map(cid -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", catNameMap.getOrDefault(cid, "未分类"));
                    m.put("stock", catStock.getOrDefault(cid, 0));
                    m.put("safeStock", catSafeStock.getOrDefault(cid, 0));
                    m.put("max", maxScale);
                    return m;
                }).collect(Collectors.toList());
    }
}
