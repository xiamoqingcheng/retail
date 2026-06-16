<template>
  <div class="report-view" v-if="content">
    <div class="report-title">{{ content.title }}</div>
    <div class="report-sub">统计区间：{{ fmtTime(content.periodStart) }} ~ {{ fmtTime(content.periodEnd) }}</div>

    <!-- 经营概览 KPI -->
    <el-row :gutter="12" class="kpi-row">
      <el-col v-for="kpi in kpis" :key="kpi.label" :xs="12" :sm="8" :md="4">
        <div class="kpi-card">
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-value">{{ kpi.value }}</div>
          <div class="kpi-growth" :class="growthClass(kpi.rate)">
            环比 {{ growthText(kpi.rate) }}
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="12">
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="block-title">销售趋势</span></template>
          <ECharts :option="trendOption" height="280" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="block-title">分类销售占比</span></template>
          <ECharts :option="categoryOption" height="280" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 热销 / 滞销 -->
    <el-row :gutter="12">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="table-card">
          <template #header><span class="block-title">热销商品 TOP10</span></template>
          <el-table :data="content.hotGoods || []" size="small" border max-height="320">
            <el-table-column type="index" label="#" width="46" />
            <el-table-column prop="name" label="商品" min-width="120" show-overflow-tooltip />
            <el-table-column prop="quantity" label="销量" width="80" align="right" />
            <el-table-column label="销售额" width="100" align="right">
              <template #default="{ row }">¥{{ num(row.amount).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="占比" width="80" align="right">
              <template #default="{ row }">{{ num(row.ratio).toFixed(1) }}%</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="table-card">
          <template #header><span class="block-title">滞销商品（区间内零动销）</span></template>
          <el-table :data="content.slowGoods || []" size="small" border max-height="320">
            <el-table-column prop="name" label="商品" min-width="120" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="分类" width="110" show-overflow-tooltip />
            <el-table-column prop="stock" label="库存" width="80" align="right" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 库存预警 / 补货建议 -->
    <el-row :gutter="12">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="table-card">
          <template #header>
            <span class="block-title">库存预警</span>
            <el-tag type="danger" size="small" class="hdr-tag">归零 {{ inventory.zeroStockCount || 0 }}</el-tag>
            <el-tag type="warning" size="small" class="hdr-tag">急需补货 {{ inventory.lowStockCount || 0 }}</el-tag>
          </template>
          <el-table :data="warnStockRows" size="small" border max-height="320">
            <el-table-column prop="name" label="商品" min-width="120" show-overflow-tooltip />
            <el-table-column label="库存" width="80" align="right">
              <template #default="{ row }">
                <el-tag :type="num(row.stock) <= 0 ? 'danger' : 'warning'" size="small">{{ row.stock }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="safeStock" label="安全库存" width="90" align="right" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="table-card">
          <template #header><span class="block-title">补货建议</span></template>
          <el-table :data="content.replenishment || []" size="small" border max-height="320">
            <el-table-column prop="name" label="商品" min-width="110" show-overflow-tooltip />
            <el-table-column prop="stock" label="库存" width="70" align="right" />
            <el-table-column prop="soldInWindow" label="区间销量" width="90" align="right" />
            <el-table-column label="建议补货" width="90" align="right">
              <template #default="{ row }"><strong>{{ row.suggestQty }}</strong></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 异常与告警 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <span class="block-title">异常与告警</span>
        <el-tag type="info" size="small" class="hdr-tag">取消订单 {{ cancelled.count || 0 }} 单 / ¥{{ num(cancelled.amount).toFixed(2) }}</el-tag>
      </template>
      <el-row :gutter="12">
        <el-col :xs="24" :md="12">
          <div class="sub-title">库存告警</div>
          <el-table :data="content.warnings || []" size="small" border max-height="260">
            <el-table-column prop="goodsName" label="商品" min-width="110" show-overflow-tooltip />
            <el-table-column prop="warningType" label="类型" width="120" />
            <el-table-column prop="warningMsg" label="内容" min-width="140" show-overflow-tooltip />
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </el-col>
        <el-col :xs="24" :md="12">
          <div class="sub-title">异常库存调整（人工/AI）</div>
          <el-table :data="adjustRows" size="small" border max-height="260">
            <el-table-column prop="goodsName" label="商品" min-width="110" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="changeAmount" label="变动量" width="90" align="right" />
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </el-col>
      </el-row>
      <div class="behavior-line" v-if="behaviorRows.length">
        用户行为：
        <el-tag v-for="b in behaviorRows" :key="b.type" size="small" class="hdr-tag">{{ b.type }}: {{ b.cnt }}</el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="ReportView">
import { computed } from "vue";
import { ECOption } from "@/components/ECharts/config";
import ECharts from "@/components/ECharts/index.vue";

const props = defineProps<{ content: any }>();

const num = (v: any): number => {
  const n = Number(v);
  return Number.isFinite(n) ? n : 0;
};

const fmtTime = (v: any): string => {
  if (!v) return "--";
  return String(v).replace("T", " ").slice(0, 16);
};

const overview = computed(() => props.content?.overview || {});
const inventory = computed(() => props.content?.inventory || {});
const cancelled = computed(() => props.content?.anomalies?.cancelledOrders || {});

const kpis = computed(() => {
  const o = overview.value;
  return [
    { label: "营业额", value: "¥" + num(o.revenue).toFixed(2), rate: o.revenueGrowthRate },
    { label: "订单数", value: num(o.orderCount), rate: o.orderCountGrowthRate },
    { label: "客流量", value: num(o.visitors), rate: o.visitorsGrowthRate },
    { label: "销售件数", value: num(o.unitsSold), rate: o.unitsSoldGrowthRate },
    { label: "客单价", value: "¥" + num(o.avgOrderValue).toFixed(2), rate: o.avgOrderValueGrowthRate }
  ];
});

const warnStockRows = computed(() => {
  const inv = inventory.value;
  return [...(inv.zeroStock || []), ...(inv.lowStock || [])];
});

const adjustRows = computed(() => props.content?.anomalies?.inventoryAdjust || []);
const behaviorRows = computed(() => props.content?.anomalies?.behavior || []);

const growthText = (rate: any): string => {
  if (rate === null || rate === undefined) return "新增";
  const pct = num(rate) * 100;
  return (pct >= 0 ? "+" : "") + pct.toFixed(2) + "%";
};

const growthClass = (rate: any): string => {
  if (rate === null || rate === undefined) return "up";
  return num(rate) >= 0 ? "up" : "down";
};

const trendOption = computed<ECOption>(() => {
  const data = props.content?.trend || [];
  const days = data.map((d: any) => String(d.d || "").slice(5));
  return {
    tooltip: { trigger: "axis", axisPointer: { type: "cross" } },
    legend: { data: ["销售额", "订单数"], top: 0 },
    grid: { left: "3%", right: "4%", bottom: "3%", containLabel: true },
    xAxis: { type: "category", boundaryGap: false, data: days },
    yAxis: [
      { type: "value", name: "销售额" },
      { type: "value", name: "订单数" }
    ],
    series: [
      {
        name: "销售额",
        type: "line",
        smooth: true,
        yAxisIndex: 0,
        data: data.map((d: any) => num(d.sales)),
        itemStyle: { color: "#6366f1" },
        areaStyle: { color: "rgba(99,102,241,0.15)" }
      },
      {
        name: "订单数",
        type: "line",
        smooth: true,
        yAxisIndex: 1,
        data: data.map((d: any) => num(d.orders)),
        itemStyle: { color: "#10b981" }
      }
    ]
  };
});

const categoryOption = computed<ECOption>(() => {
  const data = (props.content?.categorySales || []).map((c: any) => ({
    name: c.name,
    value: num(c.amount)
  }));
  return {
    tooltip: { trigger: "item", formatter: "{b}: ¥{c} ({d}%)" },
    legend: { type: "scroll", orient: "vertical", right: 0, top: "middle" },
    series: [
      {
        type: "pie",
        radius: ["40%", "70%"],
        center: ["38%", "50%"],
        data,
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: "bold" } }
      }
    ]
  };
});
</script>

<style scoped>
.report-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.report-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  text-align: center;
}
.report-sub {
  text-align: center;
  color: #6b7280;
  font-size: 13px;
  margin-top: -4px;
}
.kpi-row {
  margin-bottom: 4px;
}
.kpi-card {
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  margin-bottom: 8px;
}
.kpi-label {
  color: #6b7280;
  font-size: 13px;
}
.kpi-value {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 4px 0;
}
.kpi-growth {
  font-size: 12px;
}
.kpi-growth.up {
  color: #10b981;
}
.kpi-growth.down {
  color: #ef4444;
}
.chart-card,
.table-card {
  border-radius: 8px;
  margin-bottom: 4px;
}
.block-title {
  font-weight: 600;
  color: #111827;
}
.hdr-tag {
  margin-left: 8px;
}
.sub-title {
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}
.behavior-line {
  margin-top: 10px;
  color: #6b7280;
  font-size: 13px;
}
</style>
