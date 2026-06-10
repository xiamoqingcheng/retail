<template>
  <div class="echarts">
    <ECharts :option="option" />
  </div>
</template>

<script setup lang="ts" name="trafficLine">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { ECOption } from "@/components/ECharts/config";
import ECharts from "@/components/ECharts/index.vue";

export interface HourlyTrafficItem {
  hour: string;
  today: number;
  yesterday: number;
}

const { t } = useI18n();

const props = defineProps<{
  hourlyTraffic?: HourlyTrafficItem[];
}>();

const option = computed<ECOption>(() => {
  const data = props.hourlyTraffic || [];
  const hours = data.map(d => d.hour);
  const todayFlow = data.map(d => d.today);
  const yesterdayFlow = data.map(d => d.yesterday);

  return {
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" }
    },
    legend: {
      data: [t("dashboard.todayFlow"), t("dashboard.yesterdayFlow")],
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
      data: hours,
      axisLine: { lineStyle: { color: "#ddd" } },
      axisLabel: { color: "#666" }
    },
    yAxis: {
      type: "value",
      name: t("dashboard.peopleCount"),
      axisLine: { show: false },
      splitLine: { lineStyle: { type: "dashed", color: "#eee" } },
      axisLabel: { color: "#999" }
    },
    series: [
      {
        name: t("dashboard.todayFlow"),
        type: "bar",
        barWidth: "35%",
        data: todayFlow,
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: "#818cf8" },
              { offset: 1, color: "#6366f1" }
            ]
          }
        }
      },
      {
        name: t("dashboard.yesterdayFlow"),
        type: "line",
        smooth: true,
        data: yesterdayFlow,
        lineStyle: { width: 2, type: "dashed" },
        itemStyle: { color: "#f59e0b" },
        symbol: "circle",
        symbolSize: 6
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
