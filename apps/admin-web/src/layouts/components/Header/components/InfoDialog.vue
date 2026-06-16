<template>
  <el-dialog v-model="dialogVisible" :title="$t('info.title')" width="500px" draggable>
    <el-descriptions :column="1" border v-loading="loading">
      <el-descriptions-item :label="$t('info.username')">{{ info.username }}</el-descriptions-item>
      <el-descriptions-item :label="$t('info.role')">
        <el-tag type="primary" size="small">{{ $t("info.admin") }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item :label="$t('info.status')">
        <el-tag :type="info.status === 1 ? 'success' : 'danger'" size="small">
          {{ info.status === 1 ? $t("info.enabled") : $t("info.disabled") }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item :label="$t('info.createTime')">{{ info.createTime || "-" }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <span class="dialog-footer">
        <el-button type="primary" @click="dialogVisible = false">{{ $t("info.confirm") }}</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { getAdminUserInfo } from "@/api/modules/user";

const dialogVisible = ref(false);
const loading = ref(false);
const info = reactive({
  username: "",
  status: 1,
  createTime: ""
});

const openDialog = async () => {
  dialogVisible.value = true;
  loading.value = true;
  try {
    const { data } = await getAdminUserInfo();
    info.username = data.username;
    info.status = data.status;
    info.createTime = data.createTime;
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false;
  }
};

defineExpose({ openDialog });
</script>
