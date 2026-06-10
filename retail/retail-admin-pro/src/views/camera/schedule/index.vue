<template>
  <div class="camera-page">
    <el-card shadow="never" class="config-card">
      <template #header>
        <div class="config-title">{{ $t("camera.aiConfig") }}</div>
      </template>

      <el-form :inline="true" class="config-form">
        <el-form-item :label="$t('camera.interval')">
          <el-input-number v-model="intervalMinutes" :min="1" :max="1440" />
        </el-form-item>
        <el-form-item :label="$t('camera.batchSize')">
          <el-input-number v-model="batchSize" :min="1" :max="200" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleUpdateScheduler">{{ $t("camera.updateConfig") }}</el-button>
          <el-button type="success" :loading="triggering" @click="handleTriggerNow">{{ $t("camera.triggerNow") }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="table-header">
          <span class="table-title">{{ $t("camera.cameraList") }}</span>
          <div class="table-actions">
            <el-button type="primary" @click="openCreateDialog">{{ $t("camera.addCamera") }}</el-button>
            <el-button @click="loadCameraList">{{ $t("camera.refresh") }}</el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="cameraList"
        border
        stripe
        v-loading="tableLoading"
        :row-class-name="getRowClassName"
        @row-click="handleRowClick"
      >
        <el-table-column prop="cameraNo" :label="$t('camera.cameraNo')" width="160" />
        <el-table-column prop="shelfId" :label="$t('camera.shelfBind')" min-width="180" />

        <el-table-column :label="$t('camera.status')" width="130" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="statusSavingCameraId === row.id"
              :active-text="$t('camera.normal')"
              :inactive-text="$t('camera.offline')"
              inline-prompt
              @click.stop
              @change="val => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>

        <el-table-column :label="$t('camera.lastScanTime')" min-width="220">
          <template #default="{ row }">
            {{ row.lastScanTime || "--" }}
          </template>
        </el-table-column>

        <el-table-column :label="$t('camera.operation')" width="190" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="openEditDialog(row)">{{ $t("camera.editShelf") }}</el-button>
            <el-button type="danger" link @click.stop="handleDelete(row)">{{ $t("camera.deleteCamera") }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editDialogVisible" :title="$t('camera.editShelfTitle')" width="420px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item :label="$t('camera.cameraNo')">
          <el-input :model-value="editForm.cameraNo" disabled />
        </el-form-item>
        <el-form-item :label="$t('camera.shelfId')">
          <el-input v-model="editForm.shelfId" :placeholder="$t('camera.shelfIdPlaceholder')" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">{{ $t("header.cancel") }}</el-button>
        <el-button type="primary" :loading="editSaving" @click="handleSaveShelfBinding">{{ $t("camera.save") }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createDialogVisible" :title="$t('camera.addTitle')" width="460px" destroy-on-close>
      <el-form :model="createForm" label-width="108px">
        <el-form-item :label="$t('camera.hardwareCamera')">
          <el-select
            v-model="createForm.cameraNo"
            :placeholder="$t('camera.selectCameraPlaceholder')"
            style="width: 100%"
            :loading="hardwareLoading"
          >
            <el-option v-for="item in hardwareOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item :label="$t('camera.bindShelf')">
          <el-input v-model="createForm.shelfId" :placeholder="$t('camera.shelfIdPlaceholder')" clearable />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">{{ $t("header.cancel") }}</el-button>
        <el-button type="primary" :loading="createSaving" @click="handleCreateBinding">{{ $t("camera.confirm") }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="previewDialogVisible"
      :title="$t('camera.previewTitle')"
      width="760px"
      destroy-on-close
      @closed="stopPreview"
    >
      <div class="preview-meta">
        {{ $t("camera.currentCamera") }}{{ previewCameraNo }} | {{ $t("camera.shelfLabel") }}{{ previewShelfId }}
      </div>
      <div class="preview-wrap">
        <img v-if="previewStreamUrl" :src="previewStreamUrl" alt="camera preview" class="preview-image" @error="onStreamError" />
        <el-empty v-else :description="$t('camera.noPreview')" />
      </div>
      <template #footer>
        <el-button @click="previewDialogVisible = false">{{ $t("camera.close") }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="cameraSchedule">
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import { useUserStore } from "@/stores/modules/user";

const { t } = useI18n();
const userStore = useUserStore();
import {
  createCameraBinding,
  deleteCamera,
  getAvailableHardwareCameras,
  getCameraList,
  getSchedulerConfig,
  stopCameraStream,
  triggerSchedulerOnce,
  updateCameraShelf,
  updateCameraStatus,
  updateSchedulerConfig
} from "@/api/modules/camera";

interface CameraRow {
  id: number | string | null;
  cameraNo: string;
  shelfId: string;
  status: number;
  lastScanTime: string;
}

interface CameraForm {
  id: number | string | null;
  cameraNo: string;
  shelfId: string;
}

interface CreateCameraForm {
  cameraNo: string;
  shelfId: string;
}

interface HardwareOption {
  label: string;
  value: string;
}

const saving = ref<boolean>(false);
const triggering = ref<boolean>(false);
const tableLoading = ref<boolean>(false);
const editSaving = ref<boolean>(false);
const editDialogVisible = ref<boolean>(false);
const createDialogVisible = ref<boolean>(false);
const createSaving = ref<boolean>(false);
const statusSavingCameraId = ref<number | string | null>(null);
const hardwareLoading = ref<boolean>(false);
const previewDialogVisible = ref<boolean>(false);
const previewStreamUrl = ref<string>("");
const previewCameraNo = ref<string>("");
const previewShelfId = ref<string>("");

const intervalMinutes = ref<number>(5);
const batchSize = ref<number>(10);

const cameraList = ref<CameraRow[]>([]);
const hardwareOptions = ref<HardwareOption[]>([]);
const highlightedCameraId = ref<number | string | null>(null);
let highlightTimer: ReturnType<typeof setTimeout> | null = null;

const editForm = reactive<CameraForm>({
  id: null,
  cameraNo: "",
  shelfId: ""
});

const createForm = reactive<CreateCameraForm>({
  cameraNo: "",
  shelfId: ""
});

const normalizeCameraRow = (item: any): CameraRow => {
  return {
    id: item?.id ?? null,
    cameraNo: item?.cameraNo || item?.camera_no || "",
    shelfId: item?.shelfId || item?.shelf_id || "",
    status: Number(item?.status ?? 0),
    lastScanTime: item?.lastScanTime || item?.last_scan_time || item?.lastInspectTime || "--"
  };
};

const getRowClassName = ({ row }: { row: CameraRow }) => {
  if (highlightedCameraId.value !== null && row?.id === highlightedCameraId.value) {
    return "new-bound-row";
  }
  return "";
};

const highlightNewRow = (id: number | string | null) => {
  if (!id) return;

  highlightedCameraId.value = id;
  if (highlightTimer) {
    clearTimeout(highlightTimer);
  }
  highlightTimer = setTimeout(() => {
    highlightedCameraId.value = null;
    highlightTimer = null;
  }, 3000);
};

const loadSchedulerConfig = async () => {
  try {
    const res: any = await getSchedulerConfig();
    if (Number(res?.code) !== 200 || !res?.data) {
      return;
    }

    const nextInterval = Number(res.data.intervalMinutes);
    const nextBatchSize = Number(res.data.batchSize);

    if (Number.isFinite(nextInterval) && nextInterval > 0) {
      intervalMinutes.value = nextInterval;
    }
    if (Number.isFinite(nextBatchSize) && nextBatchSize > 0) {
      batchSize.value = nextBatchSize;
    }
  } catch {
    // 保持默认值，避免初始化弹错打扰用户。
  }
};

const loadCameraList = async () => {
  tableLoading.value = true;
  try {
    const res: any = await getCameraList();
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || res?.msg || t("camera.loadFailed"));
      return;
    }
    const list = Array.isArray(res?.data) ? res.data : [];
    cameraList.value = list.map((item: any) => normalizeCameraRow(item));
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || t("camera.loadFailed"));
  } finally {
    tableLoading.value = false;
  }
};

const handleUpdateScheduler = async () => {
  saving.value = true;
  try {
    const res: any = await updateSchedulerConfig({
      intervalMinutes: intervalMinutes.value,
      batchSize: batchSize.value
    });
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || res?.msg || t("camera.configFailed"));
      return;
    }
    const nextInterval = Number(res?.data?.intervalMinutes);
    const nextBatchSize = Number(res?.data?.batchSize);
    if (Number.isFinite(nextInterval) && nextInterval > 0) {
      intervalMinutes.value = nextInterval;
    }
    if (Number.isFinite(nextBatchSize) && nextBatchSize > 0) {
      batchSize.value = nextBatchSize;
    }
    ElMessage.success(t("camera.configSuccess"));
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || t("camera.configFailed"));
  } finally {
    saving.value = false;
  }
};

const handleTriggerNow = async () => {
  triggering.value = true;
  try {
    const res: any = await triggerSchedulerOnce();
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || res?.msg || t("camera.triggerFailed"));
      return;
    }
    ElMessage.success(t("camera.triggerSuccess"));
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || t("camera.triggerFailed"));
  } finally {
    triggering.value = false;
  }
};

const openEditDialog = (row: CameraRow) => {
  editForm.id = row.id;
  editForm.cameraNo = row.cameraNo;
  editForm.shelfId = row.shelfId;
  editDialogVisible.value = true;
};

const openCreateDialog = async () => {
  createDialogVisible.value = true;
  createForm.cameraNo = "";
  createForm.shelfId = "";
  hardwareOptions.value = [];

  hardwareLoading.value = true;
  try {
    const res: any = await getAvailableHardwareCameras();
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || res?.msg || t("camera.bindFailed"));
      return;
    }

    const indexes = Array.isArray(res?.data) ? res.data : [];
    hardwareOptions.value = indexes.map((index: number | string) => ({
      label: `${t("camera.camera")} ${index}`,
      value: String(index)
    }));

    if (hardwareOptions.value.length === 0) {
      ElMessage.warning(t("camera.noHardware"));
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || t("camera.bindFailed"));
  } finally {
    hardwareLoading.value = false;
  }
};

const handleCreateBinding = async () => {
  if (!createForm.cameraNo) {
    ElMessage.warning(t("camera.selectCamera"));
    return;
  }
  if (!createForm.shelfId || !createForm.shelfId.trim()) {
    ElMessage.warning(t("camera.inputShelf"));
    return;
  }

  createSaving.value = true;
  try {
    const res: any = await createCameraBinding({
      camera_no: createForm.cameraNo,
      shelf_id: createForm.shelfId.trim()
    });
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || res?.msg || t("camera.bindFailed"));
      return;
    }

    ElMessage.success(t("camera.bindSuccess"));
    createDialogVisible.value = false;
    const newId = Number(res?.data);
    await loadCameraList();
    if (Number.isFinite(newId) && newId > 0) {
      highlightNewRow(newId);
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || t("camera.bindFailed"));
  } finally {
    createSaving.value = false;
  }
};

const handleSaveShelfBinding = async () => {
  if (!editForm.shelfId || !editForm.shelfId.trim()) {
    ElMessage.warning(t("camera.inputShelfId"));
    return;
  }

  editSaving.value = true;
  try {
    const res: any = await updateCameraShelf({
      id: editForm.id,
      shelfId: editForm.shelfId.trim()
    });
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || res?.msg || t("camera.updateBindFailed"));
      return;
    }

    ElMessage.success(t("camera.updateBindSuccess"));
    editDialogVisible.value = false;
    await loadCameraList();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || t("camera.updateBindFailed"));
  } finally {
    editSaving.value = false;
  }
};

const handleStatusChange = async (row: CameraRow, val: string | number | boolean) => {
  const nextStatus = val === true || val === 1 || val === "1" ? 1 : 0;
  const previousStatus = row.status;
  statusSavingCameraId.value = row.id;
  try {
    await updateCameraStatus(String(row.id), nextStatus);
    row.status = nextStatus;
    ElMessage.success(nextStatus === 1 ? t("camera.enabledSuccess") : t("camera.disabledSuccess"));
  } catch {
    row.status = previousStatus;
  } finally {
    statusSavingCameraId.value = null;
  }
};

const handleDelete = async (row: CameraRow) => {
  try {
    await ElMessageBox.confirm(t("camera.deleteConfirm", { cameraNo: row.cameraNo }), t("camera.deleteTitle"), {
      type: "warning",
      confirmButtonText: t("camera.delete"),
      cancelButtonText: t("header.cancel")
    });
  } catch {
    return;
  }

  try {
    const res: any = await deleteCamera(String(row.id));
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || res?.msg || t("camera.deleteFailed"));
      return;
    }

    ElMessage.success(t("camera.deleteSuccess"));
    await loadCameraList();
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || t("camera.deleteFailed"));
  }
};

const stopPreview = () => {
  const cameraNo = previewCameraNo.value;
  previewStreamUrl.value = "";
  previewCameraNo.value = "";
  // 主动通知后端释放摄像头资源
  if (cameraNo) {
    stopCameraStream(cameraNo).catch(() => {
      console.warn("停止摄像头流失败:", cameraNo);
    });
  }
};

const onStreamError = () => {
  ElMessage.warning(t("camera.streamError"));
  previewStreamUrl.value = "";
};

const handleRowClick = (row: CameraRow) => {
  if (!row?.cameraNo) {
    return;
  }

  stopPreview();
  previewCameraNo.value = row.cameraNo;
  previewShelfId.value = row.shelfId || "";
  previewStreamUrl.value = `/api/admin/camera/stream/${row.cameraNo}?token=${userStore.token}`;
  previewDialogVisible.value = true;
};

let refreshTimer: ReturnType<typeof setInterval> | null = null;

onMounted(() => {
  void loadSchedulerConfig();
  void loadCameraList();
  refreshTimer = setInterval(loadCameraList, 30000);
});

onBeforeUnmount(() => {
  if (highlightTimer) {
    clearTimeout(highlightTimer);
    highlightTimer = null;
  }
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
  stopPreview();
});
</script>

<style scoped>
.camera-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-card {
  border-radius: 8px;
}

.table-card {
  border-radius: 8px;
}

.config-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-meta {
  margin-bottom: 8px;
  color: #374151;
  font-size: 14px;
}

.preview-wrap {
  min-height: 380px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: #0f172a;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-image {
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.config-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.new-bound-row td) {
  background-color: #ecfdf3 !important;
}
</style>
