<template>
  <el-dropdown trigger="click" @command="setAssemblySize">
    <i :class="'iconfont icon-contentright'" class="toolBar-icon"></i>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in assemblySizeList"
          :key="item.value"
          :command="item.value"
          :disabled="assemblySize === item.value"
        >
          {{ item.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { useGlobalStore } from "@/stores/modules/global";
import { AssemblySizeType } from "@/stores/interface";

const { t } = useI18n();
const globalStore = useGlobalStore();
const assemblySize = computed(() => globalStore.assemblySize);

const assemblySizeList = [
  { label: computed(() => t("header.default")), value: "default" },
  { label: computed(() => t("header.large")), value: "large" },
  { label: computed(() => t("header.small")), value: "small" }
];

const setAssemblySize = (item: AssemblySizeType) => {
  if (assemblySize.value === item) return;
  globalStore.setGlobalState("assemblySize", item);
};
</script>
