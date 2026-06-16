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

    <el-dialog v-model="createDialogVisible" :title="$t('camera.addTitle')" width="680px" destroy-on-close>
      <el-form :model="createForm" label-width="108px">
        <el-form-item label="摄像头来源">
          <el-tabs v-model="cameraSourceMode" class="camera-source-tabs">
            <el-tab-pane label="主机本机" name="local">
              <div class="camera-source-line">
                <el-select
                  v-model="createForm.cameraNo"
                  :placeholder="$t('camera.selectCameraPlaceholder')"
                  class="camera-source-select"
                  :loading="hardwareLoading"
                  filterable
                >
                  <el-option v-for="item in hardwareOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
                <el-button :icon="Refresh" :loading="hardwareLoading" @click="loadHardwareCameras()">刷新</el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="扩展端扫描" name="agent">
              <div class="camera-source-line">
                <el-select
                  v-model="createForm.cameraNo"
                  placeholder="请选择扫描到的扩展端摄像头"
                  class="camera-source-select"
                  :loading="agentLoading"
                  filterable
                >
                  <el-option v-for="item in agentOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
                <el-button type="primary" :icon="Search" :loading="agentLoading" @click="scanCameraAgents()">自动扫描</el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="手动填写" name="manual">
              <div class="manual-agent-grid">
                <el-input v-model.trim="manualAgentForm.host" placeholder="扩展端 IP，例如 192.168.1.23" clearable />
                <el-input-number v-model="manualAgentForm.port" :min="1" :max="65535" controls-position="right" />
                <el-input-number v-model="manualAgentForm.index" :min="0" :max="32" controls-position="right" />
              </div>
              <div class="manual-agent-actions">
                <el-button type="primary" :icon="EditPen" @click="applyManualCameraNo">生成编号</el-button>
                <el-button :icon="Search" :loading="manualProbeLoading" @click="probeManualAgent">查询扩展端</el-button>
              </div>
              <el-input v-model="createForm.cameraNo" readonly placeholder="生成后的摄像头编号会显示在这里" />
            </el-tab-pane>
          </el-tabs>
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
import { onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import { EditPen, Refresh, Search } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/modules/user";

const { t } = useI18n();
const userStore = useUserStore();
import {
  createCameraBinding,
  deleteCamera,
  discoverCameraAgents,
  getAvailableHardwareCameras,
  getCameraAgentAvailable,
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

type CameraSourceMode = "local" | "agent" | "manual";

const saving = ref<boolean>(false);
const triggering = ref<boolean>(false);
const tableLoading = ref<boolean>(false);
const editSaving = ref<boolean>(false);
const editDialogVisible = ref<boolean>(false);
const createDialogVisible = ref<boolean>(false);
const createSaving = ref<boolean>(false);
const statusSavingCameraId = ref<number | string | null>(null);
const hardwareLoading = ref<boolean>(false);
const agentLoading = ref<boolean>(false);
const manualProbeLoading = ref<boolean>(false);
const previewDialogVisible = ref<boolean>(false);
const previewStreamUrl = ref<string>("");
const previewCameraNo = ref<string>("");
const previewShelfId = ref<string>("");
const cameraSourceMode = ref<CameraSourceMode>("local");

const intervalMinutes = ref<number>(5);
const batchSize = ref<number>(10);

const cameraList = ref<CameraRow[]>([]);
const hardwareOptions = ref<HardwareOption[]>([]);
const agentOptions = ref<HardwareOption[]>([]);
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

const manualAgentForm = reactive({
  host: "",
  port: 8765,
  index: 0
});

const syncCameraSourceSelection = (mode = cameraSourceMode.value) => {
  if (mode === "local") {
    const current = hardwareOptions.value.find(item => item.value === createForm.cameraNo);
    createForm.cameraNo = current?.value || hardwareOptions.value[0]?.value || "";
    return;
  }

  if (mode === "agent") {
    const current = agentOptions.value.find(item => item.value === createForm.cameraNo);
    createForm.cameraNo = current?.value || agentOptions.value[0]?.value || "";
    return;
  }

  if (!createForm.cameraNo.startsWith("agent:")) {
    createForm.cameraNo = "";
  }
};

watch(cameraSourceMode, mode => {
  syncCameraSourceSelection(mode);
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

const cameraAgentToOptions = (agents: any[]): HardwareOption[] => {
  const optionMap = new Map<string, HardwareOption>();

  agents.forEach(agent => {
    const host = String(agent?.host || "").trim();
    const port = Number(agent?.port || 8765);
    if (!host || !Number.isFinite(port)) return;

    const cameras = Array.isArray(agent?.cameras) ? agent.cameras : [];
    if (cameras.length > 0) {
      cameras.forEach((camera: any) => {
        const index = camera?.index ?? "";
        const value = String(camera?.id || `agent:${host}:${port}:${index}`);
        if (!value || optionMap.has(value)) return;
        const name = camera?.name ? ` - ${camera.name}` : "";
        optionMap.set(value, {
          label: `${host}:${port} / ${t("camera.camera")} ${index}${name}`,
          value
        });
      });
      return;
    }

    const indexes = Array.isArray(agent?.available_indexes) ? agent.available_indexes : [];
    indexes.forEach((index: number | string) => {
      const value = `agent:${host}:${port}:${index}`;
      if (optionMap.has(value)) return;
      optionMap.set(value, {
        label: `${host}:${port} / ${t("camera.camera")} ${index}`,
        value
      });
    });
  });

  return Array.from(optionMap.values());
};

const loadHardwareCameras = async (showEmptyMessage = true) => {
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
    if (cameraSourceMode.value === "local") {
      syncCameraSourceSelection("local");
    }

    if (showEmptyMessage && hardwareOptions.value.length === 0) {
      ElMessage.warning(t("camera.noHardware"));
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || t("camera.bindFailed"));
  } finally {
    hardwareLoading.value = false;
  }
};

const scanCameraAgents = async (showResultMessage = true) => {
  agentLoading.value = true;
  try {
    const res: any = await discoverCameraAgents(1800);
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || res?.msg || "扫描扩展端失败");
      return;
    }

    const agents = Array.isArray(res?.data) ? res.data : [];
    agentOptions.value = cameraAgentToOptions(agents);
    if (agentOptions.value.length > 0) {
      cameraSourceMode.value = "agent";
      syncCameraSourceSelection("agent");
      if (showResultMessage) {
        ElMessage.success(`已发现 ${agentOptions.value.length} 个扩展端摄像头`);
      }
    } else {
      if (cameraSourceMode.value === "agent") {
        createForm.cameraNo = "";
      }
      if (showResultMessage) {
        ElMessage.warning("未发现扩展端，可使用手动填写");
      }
    }
  } catch (error: any) {
    if (showResultMessage) {
      ElMessage.error(error?.response?.data?.message || error?.message || "扫描扩展端失败");
    }
  } finally {
    agentLoading.value = false;
  }
};

const normalizeManualAgent = () => {
  const host = manualAgentForm.host.trim();
  const port = Number(manualAgentForm.port);
  const index = Number(manualAgentForm.index);

  if (!host) {
    ElMessage.warning("请输入扩展端 IP");
    return null;
  }
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    ElMessage.warning("端口必须在 1 到 65535 之间");
    return null;
  }
  if (!Number.isInteger(index) || index < 0) {
    ElMessage.warning("摄像头索引必须大于等于 0");
    return null;
  }

  return { host, port, index };
};

const applyManualCameraNo = () => {
  const manual = normalizeManualAgent();
  if (!manual) return;
  createForm.cameraNo = `agent:${manual.host}:${manual.port}:${manual.index}`;
};

const probeManualAgent = async () => {
  const manual = normalizeManualAgent();
  if (!manual) return;

  manualProbeLoading.value = true;
  try {
    const res: any = await getCameraAgentAvailable(manual.host, manual.port);
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || res?.msg || "查询扩展端失败");
      return;
    }

    const options = cameraAgentToOptions([res?.data]);
    if (options.length === 0) {
      ElMessage.warning("该扩展端未返回可用摄像头");
      return;
    }
    agentOptions.value = options;
    cameraSourceMode.value = "agent";
    createForm.cameraNo = options.find(item => item.value.endsWith(`:${manual.index}`))?.value || options[0].value;
    ElMessage.success(`已获取 ${options.length} 个扩展端摄像头`);
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "查询扩展端失败");
  } finally {
    manualProbeLoading.value = false;
  }
};

const openCreateDialog = async () => {
  createDialogVisible.value = true;
  cameraSourceMode.value = "local";
  createForm.cameraNo = "";
  createForm.shelfId = "";
  hardwareOptions.value = [];
  agentOptions.value = [];
  manualAgentForm.host = "";
  manualAgentForm.port = 8765;
  manualAgentForm.index = 0;

  await Promise.all([loadHardwareCameras(false), scanCameraAgents(false)]);
  if (!createForm.cameraNo && hardwareOptions.value.length > 0) {
    cameraSourceMode.value = "local";
    createForm.cameraNo = hardwareOptions.value[0].value;
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
    if (nextStatus === 0) {
      closePreviewIfCamera(row.cameraNo);
    }
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
    closePreviewIfCamera(row.cameraNo);
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

const closePreviewIfCamera = (cameraNo: string) => {
  if (previewCameraNo.value !== cameraNo) {
    return;
  }
  previewDialogVisible.value = false;
  stopPreview();
};

const handleRowClick = (row: CameraRow) => {
  if (!row?.cameraNo) {
    return;
  }
  if (row.status !== 1) {
    ElMessage.warning("摄像头已离线，不能预览");
    return;
  }

  stopPreview();
  previewCameraNo.value = row.cameraNo;
  previewShelfId.value = row.shelfId || "";
  const params = new URLSearchParams({
    cameraNo: row.cameraNo,
    token: userStore.token || ""
  });
  previewStreamUrl.value = `/api/admin/camera/stream?${params.toString()}`;
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

.camera-source-tabs {
  width: 100%;
}

.camera-source-line {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.camera-source-select {
  flex: 1;
  min-width: 0;
}

.manual-agent-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 130px 120px;
  gap: 8px;
  margin-bottom: 10px;
}

.manual-agent-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

@media (max-width: 720px) {
  .camera-source-line,
  .manual-agent-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .manual-agent-grid {
    grid-template-columns: 1fr;
  }
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
