<template>
  <div class="echarts">
    <ECharts :option="option" />
  </div>
</template>

<script setup lang="ts" name="hotGoodsBar">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { ECOption } from "@/components/ECharts/config";
import ECharts from "@/components/ECharts/index.vue";

export interface HotGoodsItem {
  goodsId: number;
  name: string;
  quantity: number;
}

const { t } = useI18n();

const props = defineProps<{
  hotGoods?: HotGoodsItem[];
}>();

const option = computed<ECOption>(() => {
  const data = (props.hotGoods || []).slice(0, 8);
  const names = data.map(d => d.name);
  const quantities = data.map(d => d.quantity);

  return {
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" }
    },
    grid: {
      left: "3%",
      right: "10%",
      top: "3%",
      bottom: "3%",
      containLabel: true
    },
    xAxis: {
      type: "value",
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { type: "dashed", color: "#f0f0f0" } },
      axisLabel: { color: "#999" }
    },
    yAxis: {
      type: "category",
      data: names.reverse(),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: "#666", fontSize: 13 }
    },
    series: [
      {
        type: "bar",
        data: quantities.reverse(),
        barWidth: 18,
        showBackground: true,
        backgroundStyle: {
          color: "rgba(180, 180, 180, 0.08)",
          borderRadius: [0, 10, 10, 0]
        },
        itemStyle: {
          borderRadius: [0, 10, 10, 0],
          color: (params: any) => {
            const colors = [
              {
                type: "linear",
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: "#818cf8" },
                  { offset: 1, color: "#6366f1" }
                ]
              },
              {
                type: "linear",
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: "#34d399" },
                  { offset: 1, color: "#10b981" }
                ]
              },
              {
                type: "linear",
                x: 0,
                y: 0,
                x2: 1,
                y2: 0,
                colorStops: [
                  { offset: 0, color: "#fbbf24" },
                  { offset: 1, color: "#f59e0b" }
                ]
              }
            ];
            const idx = params.dataIndex >= names.length - 3 ? names.length - 1 - params.dataIndex : 2;
            return colors[Math.min(idx, 2)] as any;
          }
        },
        label: {
          show: true,
          position: "right",
          color: "#666",
          fontSize: 12,
          formatter: `{c} ${t("dashboard.units")}`
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
