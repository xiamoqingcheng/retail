<template>
  <div class="echarts">
    <ECharts :option="option" />
  </div>
</template>

<script setup lang="ts" name="salesLine">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { ECOption } from "@/components/ECharts/config";
import ECharts from "@/components/ECharts/index.vue";

export interface TrendDataItem {
  date: string;
  sales: number;
  orders: number;
}

const { t } = useI18n();

const props = withDefaults(
  defineProps<{
    range?: string;
    trendData?: TrendDataItem[];
  }>(),
  { range: "week" }
);

const option = computed<ECOption>(() => {
  const data = props.trendData || [];
  const days = data.map(d => {
    const parts = d.date.split("-");
    return parts.length === 3 ? parts[1] + "/" + parts[2] : d.date;
  });
  const salesData = data.map(d => Number(d.sales || 0));
  const orderData = data.map(d => Number(d.orders || 0));

  return {
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "cross" }
    },
    legend: {
      data: [t("dashboard.salesAmount"), t("dashboard.orderCount")],
      top: 0,
      textStyle: { color: "#666" }
    },
    grid: {
      left: "3%",
      right: "4%",
      bottom: "3%",
      containLabel: true
    },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: days,
      axisLine: { lineStyle: { color: "#ddd" } },
      axisLabel: { color: "#666" }
    },
    yAxis: [
      {
        type: "value",
        name: t("dashboard.salesAmount"),
        axisLine: { show: false },
        splitLine: { lineStyle: { type: "dashed", color: "#eee" } },
        axisLabel: { color: "#999" }
      },
      {
        type: "value",
        name: t("dashboard.orderCount"),
        axisLine: { show: false },
        splitLine: { show: false },
        axisLabel: { color: "#999" }
      }
    ],
    series: [
      {
        name: t("dashboard.salesAmount"),
        type: "line",
        smooth: true,
        yAxisIndex: 0,
        data: salesData,
        lineStyle: { width: 3 },
        itemStyle: { color: "#6366f1" },
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: "rgba(99, 102, 241, 0.35)" },
              { offset: 1, color: "rgba(99, 102, 241, 0.02)" }
            ]
          }
        }
      },
      {
        name: t("dashboard.orderCount"),
        type: "line",
        smooth: true,
        yAxisIndex: 1,
        data: orderData,
        lineStyle: { width: 3 },
        itemStyle: { color: "#10b981" },
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: "rgba(16, 185, 129, 0.3)" },
              { offset: 1, color: "rgba(16, 185, 129, 0.02)" }
            ]
          }
        }
      }
    ]
  };
});
</script>

<style scoped>
.echarts {
  width: 100%;
  height: 100%;
}
</style>
