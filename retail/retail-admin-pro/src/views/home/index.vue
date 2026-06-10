<template>
  <div class="dashboard-box">
    <!-- KPI 指标卡片 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="12" :md="6" :lg="6" v-for="(item, index) in kpiCards" :key="index">
        <div class="kpi-card" :class="item.theme">
          <div class="kpi-icon">
            <el-icon :size="28"><component :is="item.icon" /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-label">{{ item.label }}</span>
            <span class="kpi-value">
              <count-to
                :start-val="0"
                :end-val="item.value"
                :duration="1500"
                :decimals="item.decimals || 0"
                :prefix="item.prefix || ''"
                :suffix="item.suffix || ''"
              />
            </span>
            <div class="kpi-trend" :class="item.trendUp ? 'up' : 'down'">
              <el-icon :size="12"><component :is="item.trendUp ? Top : Bottom" /></el-icon>
              <span>{{ item.trendPercent }}% {{ $t("dashboard.vsYesterday") }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 中间行：销售趋势 + 商品分类占比 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :sm="24" :md="16" :lg="16">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">{{ $t("dashboard.weeklyTrend") }}</span>
              <el-radio-group v-model="salesRange" size="small">
                <el-radio-button label="week">{{ $t("dashboard.last7days") }}</el-radio-button>
                <el-radio-button label="month">{{ $t("dashboard.last30days") }}</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-body" style="height: 340px">
            <SalesLine :range="salesRange" :trend-data="trendData" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="8" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span class="chart-title">{{ $t("dashboard.categoryShare") }}</span>
          </template>
          <div class="chart-body" style="height: 340px">
            <CategoryPie :category-pie-data="categoryPieData" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三行：人流量 + 库存雷达 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :sm="24" :md="14" :lg="14">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">{{ $t("dashboard.todayTraffic") }}</span>
              <el-tag type="success" size="small" effect="plain">{{ $t("dashboard.realtime") }}</el-tag>
            </div>
          </template>
          <div class="chart-body" style="height: 320px">
            <TrafficLine :hourly-traffic="hourlyTraffic" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="10" :lg="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span class="chart-title">{{ $t("dashboard.stockHealth") }}</span>
          </template>
          <div class="chart-body" style="height: 320px">
            <StockRadar :category-stock-data="categoryStockData" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第四行：热销排行 + 急需补货 + 最近告警 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :sm="24" :md="10" :lg="10">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">{{ $t("dashboard.hotGoods") }}</span>
              <el-tag size="small" effect="plain">{{ $t("dashboard.thisWeek") }}</el-tag>
            </div>
          </template>
          <div class="chart-body" style="height: 360px">
            <HotGoodsBar :hot-goods="hotGoods" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="7" :lg="7">
        <el-card shadow="never" class="chart-card list-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">{{ $t("dashboard.urgentRestock") }}</span>
              <el-tag type="danger" size="small" effect="plain">{{ urgentList.length }} {{ $t("dashboard.items") }}</el-tag>
            </div>
          </template>
          <div class="urgent-list">
            <div class="urgent-item" v-for="(item, i) in urgentList" :key="i">
              <div class="urgent-rank" :class="{ 'rank-danger': item.stock <= 5 }">{{ i + 1 }}</div>
              <div class="urgent-info">
                <span class="urgent-name sle">{{ item.name }}</span>
                <el-progress
                  :percentage="Math.min((item.stock / (item.safeStock || 10)) * 100, 100)"
                  :color="item.stock <= 5 ? '#ef4444' : '#f59e0b'"
                  :stroke-width="6"
                  :show-text="false"
                />
              </div>
              <div class="urgent-stock">
                <span class="stock-num" :class="{ 'text-danger': item.stock <= 5 }">{{ item.stock }}</span>
                <span class="stock-unit">/ {{ item.safeStock || 10 }}</span>
              </div>
              <el-button type="primary" size="small" link @click="openReplenish(item)">{{ $t("dashboard.restock") }}</el-button>
            </div>
            <el-empty v-if="urgentList.length === 0" :image-size="48" :description="$t('dashboard.stockSufficient')" />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="7" :lg="7">
        <el-card shadow="never" class="chart-card list-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">{{ $t("dashboard.recentAlerts") }}</span>
              <el-tag type="warning" size="small" effect="plain">{{ alertList.length }} {{ $t("dashboard.alerts") }}</el-tag>
            </div>
          </template>
          <div class="alert-list">
            <div class="alert-item" v-for="(item, i) in alertList" :key="i">
              <el-icon :size="16" class="alert-icon" :class="item.level"><WarningFilled /></el-icon>
              <div class="alert-info">
                <span class="alert-msg sle">{{ item.msg }}</span>
                <span class="alert-time">{{ item.time }}</span>
              </div>
              <el-tag :type="item.resolved ? 'success' : 'danger'" size="small" effect="plain">
                {{ item.resolved ? $t("dashboard.resolved") : $t("dashboard.pending") }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 补货弹窗 -->
    <el-dialog v-model="replenishVisible" :title="$t('dashboard.restockDialog')" width="420px" :close-on-click-modal="false">
      <div class="replenish-info">
        <span class="replenish-label">{{ $t("dashboard.goodsName") }}</span>
        <span>{{ replenishItem?.name }}</span>
      </div>
      <div class="replenish-info">
        <span class="replenish-label">{{ $t("dashboard.currentStock") }}</span>
        <span class="stock-num">{{ replenishItem?.stock ?? 0 }}</span>
      </div>
      <div class="replenish-info">
        <span class="replenish-label">{{ $t("dashboard.safeStock") }}</span>
        <span>{{ replenishItem?.safeStock ?? 10 }}</span>
      </div>
      <div class="replenish-input-row">
        <span class="replenish-label">{{ $t("dashboard.restockQty") }}</span>
        <el-input-number v-model="replenishQty" :min="1" :max="9999" :step="10" />
      </div>
      <template #footer>
        <el-button @click="replenishVisible = false">{{ $t("header.cancel") }}</el-button>
        <el-button type="primary" :loading="replenishLoading" @click="doReplenish">{{
          $t("dashboard.confirmRestock")
        }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="home">
import { ref, reactive, computed, defineAsyncComponent, h, onMounted, onUnmounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { WarningFilled, Top, Bottom } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { getDashboardStats } from "@/api/modules/dashboard";
import { updateGoods } from "@/api/modules/goods";

const { t } = useI18n();

const SalesLine = defineAsyncComponent(() => import("@/views/dashboard/dataVisualize/components/salesLine.vue"));
const CategoryPie = defineAsyncComponent(() => import("@/views/dashboard/dataVisualize/components/categoryPie.vue"));
const TrafficLine = defineAsyncComponent(() => import("@/views/dashboard/dataVisualize/components/trafficLine.vue"));
const StockRadar = defineAsyncComponent(() => import("@/views/dashboard/dataVisualize/components/stockRadar.vue"));
const HotGoodsBar = defineAsyncComponent(() => import("@/views/dashboard/dataVisualize/components/hotGoodsBar.vue"));

/* ---------- CountTo 数字滚动动画 ---------- */
const CountTo = {
  name: "CountTo",
  props: {
    startVal: { type: Number, default: 0 },
    endVal: { type: Number, default: 0 },
    duration: { type: Number, default: 1500 },
    decimals: { type: Number, default: 0 },
    prefix: { type: String, default: "" },
    suffix: { type: String, default: "" }
  },
  setup(props: any) {
    const display = ref(props.startVal);
    let raf: number | null = null;
    const start = () => {
      if (raf) cancelAnimationFrame(raf);
      const startTime = performance.now();
      const diff = props.endVal - display.value;
      const baseVal = display.value;
      const animate = (now: number) => {
        const elapsed = now - startTime;
        const progress = Math.min(elapsed / props.duration, 1);
        const ease = 1 - Math.pow(1 - progress, 4);
        display.value = baseVal + diff * ease;
        if (progress < 1) raf = requestAnimationFrame(animate);
      };
      raf = requestAnimationFrame(animate);
    };
    onMounted(start);
    watch(
      () => props.endVal,
      () => {
        start();
      }
    );
    onUnmounted(() => {
      if (raf) cancelAnimationFrame(raf);
    });
    return () =>
      h("span", {}, props.prefix + display.value.toFixed(props.decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ",") + props.suffix);
  }
};

const salesRange = ref("week");

const kpiData = reactive([
  { value: 0, trendUp: true, trendPercent: 0 },
  { value: 0, trendUp: true, trendPercent: 0 },
  { value: 0, trendUp: false, trendPercent: 0 },
  { value: 0, trendUp: false, trendPercent: 0 }
]);

const kpiCards = computed(() => [
  {
    ...kpiData[0],
    label: t("dashboard.todaySales"),
    prefix: "¥",
    suffix: "",
    decimals: 0,
    theme: "theme-indigo",
    icon: "TrendCharts"
  },
  {
    ...kpiData[1],
    label: t("dashboard.todayOrders"),
    prefix: "",
    suffix: ` ${t("goods.orders")}`,
    decimals: 0,
    theme: "theme-emerald",
    icon: "ShoppingCart"
  },
  {
    ...kpiData[2],
    label: t("dashboard.todayVisitors"),
    prefix: "",
    suffix: ` ${t("dashboard.peopleCount")}`,
    decimals: 0,
    theme: "theme-amber",
    icon: "User"
  },
  {
    ...kpiData[3],
    label: t("dashboard.stockAlert"),
    prefix: "",
    suffix: ` ${t("dashboard.items")}`,
    decimals: 0,
    theme: "theme-rose",
    icon: "Warning"
  }
]);

const urgentList = ref<any[]>([]);
const alertList = ref<any[]>([]);
const trendData = ref<any[]>([]);
const hotGoods = ref<any[]>([]);
const categoryPieData = ref<any[]>([]);
const hourlyTraffic = ref<any[]>([]);
const categoryStockData = ref<any[]>([]);

// 补货相关
const replenishVisible = ref(false);
const replenishItem = ref<any>(null);
const replenishQty = ref(10);
const replenishLoading = ref(false);

const openReplenish = (item: any) => {
  replenishItem.value = item;
  replenishQty.value = Math.max((item.safeStock || 10) - (item.stock || 0), 1);
  replenishVisible.value = true;
};

const doReplenish = async () => {
  if (!replenishItem.value) return;
  replenishLoading.value = true;
  try {
    const newStock = (replenishItem.value.stock || 0) + replenishQty.value;
    await updateGoods({ id: replenishItem.value.id, stock: newStock });
    ElMessage.success(`${t("dashboard.restockSuccess")}${replenishItem.value.name} ${t("dashboard.stockUpdated")} ${newStock}`);
    replenishVisible.value = false;
    loadDashboard();
  } catch (e: any) {
    ElMessage.error(e?.message || t("dashboard.restockFailed"));
  } finally {
    replenishLoading.value = false;
  }
};

const calcTrend = (today: number, yesterday: number) => {
  if (yesterday === 0) return { up: today > 0, pct: today > 0 ? 100 : 0 };
  const pct = Math.round(((today - yesterday) / yesterday) * 100);
  return { up: pct >= 0, pct: Math.abs(pct) };
};

const loadDashboard = async () => {
  try {
    const res = await getDashboardStats(salesRange.value);
    const d = res.data;
    if (!d) return;
    const todaySales = Number(d.todaySales ?? 0);
    const todayOrders = Number(d.todayOrderCount ?? 0);
    const todayVisitors = Number(d.todayVisitors ?? 0);
    const yestSales = Number(d.yesterdaySales ?? 0);
    const yestOrders = Number(d.yesterdayOrderCount ?? 0);
    const yestVisitors = Number(d.yesterdayVisitors ?? 0);

    kpiData[0].value = todaySales;
    kpiData[1].value = todayOrders;
    kpiData[2].value = todayVisitors;
    kpiData[3].value = Number(d.alertCount ?? 0);

    const salesTrend = calcTrend(todaySales, yestSales);
    kpiData[0].trendUp = salesTrend.up;
    kpiData[0].trendPercent = salesTrend.pct;

    const orderTrend = calcTrend(todayOrders, yestOrders);
    kpiData[1].trendUp = orderTrend.up;
    kpiData[1].trendPercent = orderTrend.pct;

    const visitorTrend = calcTrend(todayVisitors, yestVisitors);
    kpiData[2].trendUp = visitorTrend.up;
    kpiData[2].trendPercent = visitorTrend.pct;

    urgentList.value = d.urgentList || [];
    alertList.value = d.alertList || [];
    trendData.value = d.trendData || [];
    hotGoods.value = d.hotGoods || [];
    categoryPieData.value = d.categoryPieData || [];
    hourlyTraffic.value = d.hourlyTraffic || [];
    categoryStockData.value = d.categoryStockData || [];
  } catch (e) {
    console.error("dashboard load failed:", e);
  }
};

let dashboardTimer: ReturnType<typeof setInterval> | null = null;

onMounted(() => {
  loadDashboard();
  dashboardTimer = setInterval(loadDashboard, 30000);
});

watch(salesRange, () => {
  loadDashboard();
});

onUnmounted(() => {
  if (dashboardTimer) {
    clearInterval(dashboardTimer);
    dashboardTimer = null;
  }
});
</script>

<style scoped lang="scss">
.dashboard-box {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.kpi-row {
  margin-bottom: 0 !important;
}

.kpi-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 24px 22px;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition:
    transform 0.25s ease,
    box-shadow 0.25s ease;
  cursor: default;
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  }

  .kpi-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    border-radius: 14px;
    flex-shrink: 0;
  }
  .kpi-info {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }
  .kpi-label {
    font-size: 13px;
    color: #999;
  }
  .kpi-value {
    font-size: 26px;
    font-weight: 700;
    color: #1a1a37;
    font-family: "DIN", sans-serif;
    line-height: 1.2;
  }
  .kpi-trend {
    display: flex;
    align-items: center;
    gap: 3px;
    font-size: 12px;
    &.up {
      color: #10b981;
    }
    &.down {
      color: #ef4444;
    }
  }

  &.theme-indigo .kpi-icon {
    background: rgba(99, 102, 241, 0.1);
    color: #6366f1;
  }
  &.theme-emerald .kpi-icon {
    background: rgba(16, 185, 129, 0.1);
    color: #10b981;
  }
  &.theme-amber .kpi-icon {
    background: rgba(245, 158, 11, 0.1);
    color: #f59e0b;
  }
  &.theme-rose .kpi-icon {
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;
  }
}

.chart-row {
  margin-bottom: 0 !important;
}

.chart-card {
  border-radius: 14px;
  overflow: hidden;
  :deep(.el-card__header) {
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
  }
  :deep(.el-card__body) {
    padding: 16px 20px 20px;
  }
}
.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
.chart-body {
  width: 100%;
}

.list-card {
  :deep(.el-card__body) {
    padding: 8px 20px 20px;
  }
}

.urgent-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow-y: auto;
}
.urgent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px solid #f8f8f8;
  &:last-child {
    border-bottom: none;
  }
}
.urgent-rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #f0f0f0;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  flex-shrink: 0;
  &.rank-danger {
    background: #fef2f2;
    color: #ef4444;
  }
}
.urgent-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.urgent-name {
  font-size: 13px;
  color: #333;
}
.urgent-stock {
  display: flex;
  align-items: baseline;
  gap: 2px;
  flex-shrink: 0;
}
.stock-num {
  font-size: 18px;
  font-weight: 700;
  color: #f59e0b;
  font-family: "DIN", sans-serif;
  &.text-danger {
    color: #ef4444;
  }
}
.stock-unit {
  font-size: 12px;
  color: #bbb;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 360px;
  overflow-y: auto;
}
.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f8f8f8;
  &:last-child {
    border-bottom: none;
  }
}
.alert-icon {
  margin-top: 2px;
  flex-shrink: 0;
  &.danger {
    color: #ef4444;
  }
  &.warning {
    color: #f59e0b;
  }
  &.info {
    color: #6366f1;
  }
}
.alert-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.alert-msg {
  font-size: 13px;
  color: #333;
  line-height: 1.4;
}
.alert-time {
  font-size: 11px;
  color: #bbb;
}
.sle {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.replenish-info {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
}
.replenish-label {
  width: 80px;
  color: #888;
  text-align: right;
  margin-right: 8px;
}
.replenish-input-row {
  display: flex;
  align-items: center;
  margin-top: 16px;
}

html.dark {
  .kpi-card {
    background: var(--el-bg-color);
    .kpi-value {
      color: var(--el-text-color-primary);
    }
  }
}
</style>
