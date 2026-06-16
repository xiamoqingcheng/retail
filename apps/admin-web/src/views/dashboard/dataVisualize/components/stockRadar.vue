<template>
  <div class="echarts">
    <ECharts :option="option" />
  </div>
</template>

<script setup lang="ts" name="stockRadar">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { ECOption } from "@/components/ECharts/config";
import ECharts from "@/components/ECharts/index.vue";

export interface CategoryStockItem {
  name: string;
  stock: number;
  safeStock: number;
  max: number;
}

const { t } = useI18n();

const props = defineProps<{
  categoryStockData?: CategoryStockItem[];
}>();

const option = computed<ECOption>(() => {
  const data = props.categoryStockData || [];
  const indicator = data.map(d => ({ name: d.name, max: d.max || Math.max(d.stock, d.safeStock, 100) }));
  const stockValues = data.map(d => d.stock);
  const safeValues = data.map(d => d.safeStock);

  return {
    tooltip: { trigger: "item" },
    legend: {
      data: [t("dashboard.currentStockLabel"), t("dashboard.safeStockLabel")],
      bottom: 0,
      textStyle: { color: "#666" }
    },
    radar: {
      indicator:
        indicator.length > 0
          ? indicator
          : [
              { name: t("dashboard.catFood"), max: 500 },
              { name: t("dashboard.catBeverage"), max: 500 },
              { name: t("dashboard.catDaily"), max: 500 },
              { name: t("dashboard.catSnack"), max: 500 },
              { name: t("dashboard.catBeauty"), max: 500 },
              { name: t("dashboard.catCondiment"), max: 500 }
            ],
      shape: "circle",
      splitNumber: 4,
      axisName: { color: "#666", fontSize: 13 },
      splitLine: { lineStyle: { color: "#eee" } },
      splitArea: { areaStyle: { color: ["rgba(99,102,241,0.02)", "rgba(99,102,241,0.05)"] } },
      axisLine: { lineStyle: { color: "#ddd" } }
    },
    series: [
      {
        type: "radar",
        data: [
          {
            value: stockValues.length > 0 ? stockValues : [380, 120, 260, 310, 180, 220],
            name: t("dashboard.currentStockLabel"),
            lineStyle: { width: 2, color: "#6366f1" },
            itemStyle: { color: "#6366f1" },
            areaStyle: { color: "rgba(99, 102, 241, 0.15)" }
          },
          {
            value: safeValues.length > 0 ? safeValues : [200, 200, 200, 200, 200, 200],
            name: t("dashboard.safeStockLabel"),
            lineStyle: { width: 2, type: "dashed", color: "#ef4444" },
            itemStyle: { color: "#ef4444" },
            areaStyle: { color: "rgba(239, 68, 68, 0.06)" }
          }
        ]
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
