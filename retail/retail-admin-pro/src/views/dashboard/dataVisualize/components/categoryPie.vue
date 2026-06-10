<template>
  <div class="echarts">
    <ECharts :option="option" />
  </div>
</template>

<script setup lang="ts" name="categoryPie">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { ECOption } from "@/components/ECharts/config";
import ECharts from "@/components/ECharts/index.vue";

export interface CategoryPieItem {
  name: string;
  value: number;
}

const { t } = useI18n();

const props = defineProps<{
  categoryPieData?: CategoryPieItem[];
}>();

const option = computed<ECOption>(() => {
  const data =
    props.categoryPieData && props.categoryPieData.length > 0
      ? props.categoryPieData
      : [
          { value: 35, name: t("dashboard.catFood") },
          { value: 25, name: t("dashboard.catDaily") },
          { value: 18, name: t("dashboard.catBeauty") },
          { value: 12, name: t("dashboard.catSnack") },
          { value: 10, name: t("dashboard.catOther") }
        ];

  return {
    tooltip: {
      trigger: "item",
      formatter: "{b}: {c}%"
    },
    legend: {
      orient: "vertical",
      right: "5%",
      top: "center",
      icon: "circle",
      itemGap: 16,
      textStyle: { color: "#666", fontSize: 13 }
    },
    series: [
      {
        type: "pie",
        radius: ["45%", "72%"],
        center: ["35%", "50%"],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: "#fff",
          borderWidth: 3
        },
        label: { show: false, position: "center" },
        emphasis: {
          label: { show: true, fontSize: 18, fontWeight: "bold", color: "#333" }
        },
        labelLine: { show: false },
        data: data,
        color: ["#6366f1", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899", "#06b6d4", "#84cc16"]
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
