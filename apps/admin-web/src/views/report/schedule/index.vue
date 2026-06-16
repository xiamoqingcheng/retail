<template>
  <div class="report-page">
    <el-tabs v-model="activeTab" class="report-tabs">
      <!-- ============ 定时配置 ============ -->
      <el-tab-pane label="定时配置" name="schedule">
        <el-card shadow="never">
          <el-form label-width="120px" class="schedule-form">
            <el-form-item label="启用定时生成">
              <el-switch v-model="scheduleForm.enabled" :active-value="1" :inactive-value="0" />
              <span class="form-tip">开启后，系统将按下方间隔自动生成区间销售报表</span>
            </el-form-item>
            <el-form-item label="生成间隔">
              <div class="interval-line">
                <el-input-number v-model="scheduleForm.intervalDays" :min="0" :max="365" /> <span class="unit">天</span>
                <el-input-number v-model="scheduleForm.intervalHours" :min="0" :max="23" /> <span class="unit">时</span>
                <el-input-number v-model="scheduleForm.intervalMinutes" :min="0" :max="59" /> <span class="unit">分</span>
              </div>
            </el-form-item>
            <el-form-item label="上次生成时间">
              <span>{{ fmtTime(scheduleForm.lastRunTime) }}</span>
            </el-form-item>
            <el-form-item label="下次生成时间">
              <span>{{ fmtTime(scheduleForm.nextRunTime) }}</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingSchedule" @click="handleSaveSchedule">保存配置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- ============ 手动生成 ============ -->
      <el-tab-pane label="手动生成" name="manual">
        <el-card shadow="never" class="manual-bar">
          <el-form :inline="true">
            <el-form-item label="统计区间">
              <el-date-picker
                v-model="manualRange"
                type="datetimerange"
                value-format="YYYY-MM-DDTHH:mm:ss"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                :shortcuts="rangeShortcuts"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="generating" @click="handleGenerate">生成报表</el-button>
              <el-button :disabled="!manualReportId" @click="handleExport(manualReportId, 'xlsx')">导出 Excel</el-button>
              <el-button :disabled="!manualReportId" @click="handleExport(manualReportId, 'pdf')">导出 PDF</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="manual-report" v-loading="generating">
          <ReportView v-if="manualReport" :content="manualReport" />
          <el-empty v-else description="请选择时间区间并点击「生成报表」" />
        </el-card>
      </el-tab-pane>

      <!-- ============ 报表记录 ============ -->
      <el-tab-pane label="报表记录" name="records">
        <el-card shadow="never">
          <div class="records-header">
            <span class="block-title">历史报表</span>
            <el-button :icon="Refresh" @click="loadRecords">刷新</el-button>
          </div>
          <el-table :data="records" border v-loading="recordsLoading">
            <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.reportType === 'SCHEDULED' ? 'success' : 'info'" size="small">
                  {{ row.reportType === "SCHEDULED" ? "定时" : "手动" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="统计区间" min-width="280">
              <template #default="{ row }">{{ fmtTime(row.periodStart) }} ~ {{ fmtTime(row.periodEnd) }}</template>
            </el-table-column>
            <el-table-column label="生成时间" width="170">
              <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleView(row.id)">查看</el-button>
                <el-button type="success" link @click="handleExport(row.id, 'xlsx')">Excel</el-button>
                <el-button type="warning" link @click="handleExport(row.id, 'pdf')">PDF</el-button>
                <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="records-pager"
            layout="prev, pager, next, total"
            :total="recordsTotal"
            :current-page="recordsPage"
            :page-size="recordsSize"
            @current-change="onPageChange"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="viewDialogVisible" title="报表详情" width="1100px" top="5vh" destroy-on-close>
      <ReportView v-if="viewReport" :content="viewReport" />
      <template #footer>
        <el-button @click="handleExport(viewReportId, 'xlsx')">导出 Excel</el-button>
        <el-button @click="handleExport(viewReportId, 'pdf')">导出 PDF</el-button>
        <el-button type="primary" @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="reportSchedule">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Refresh } from "@element-plus/icons-vue";
import ReportView from "./components/ReportView.vue";
import {
  deleteReport,
  exportReportFile,
  generateReport,
  getReportDetail,
  getReportList,
  getReportSchedule,
  updateReportSchedule
} from "@/api/modules/report";

const MANUAL_RANGE_KEY = "report-manual-range";

const activeTab = ref<string>("schedule");

// ====== 定时配置 ======
const savingSchedule = ref(false);
const scheduleForm = reactive({
  enabled: 0,
  intervalDays: 1,
  intervalHours: 0,
  intervalMinutes: 0,
  lastRunTime: "" as string | null,
  nextRunTime: "" as string | null
});

const fmtTime = (v: any): string => (v ? String(v).replace("T", " ").slice(0, 16) : "--");

const loadSchedule = async () => {
  try {
    const res: any = await getReportSchedule();
    if (Number(res?.code) === 200 && res.data) {
      scheduleForm.enabled = Number(res.data.enabled ?? 0);
      scheduleForm.intervalDays = Number(res.data.intervalDays ?? 1);
      scheduleForm.intervalHours = Number(res.data.intervalHours ?? 0);
      scheduleForm.intervalMinutes = Number(res.data.intervalMinutes ?? 0);
      scheduleForm.lastRunTime = res.data.lastRunTime;
      scheduleForm.nextRunTime = res.data.nextRunTime;
    }
  } catch {
    // 保持默认值
  }
};

const handleSaveSchedule = async () => {
  if (
    scheduleForm.enabled === 1 &&
    scheduleForm.intervalDays === 0 &&
    scheduleForm.intervalHours === 0 &&
    scheduleForm.intervalMinutes === 0
  ) {
    ElMessage.warning("启用时生成间隔不能为 0，至少 1 分钟");
    return;
  }
  savingSchedule.value = true;
  try {
    const res: any = await updateReportSchedule({
      enabled: scheduleForm.enabled,
      intervalDays: scheduleForm.intervalDays,
      intervalHours: scheduleForm.intervalHours,
      intervalMinutes: scheduleForm.intervalMinutes
    });
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || res?.msg || "保存失败");
      return;
    }
    scheduleForm.lastRunTime = res.data?.lastRunTime;
    scheduleForm.nextRunTime = res.data?.nextRunTime;
    ElMessage.success("配置已保存");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "保存失败");
  } finally {
    savingSchedule.value = false;
  }
};

// ====== 手动生成 ======
const generating = ref(false);
const manualRange = ref<[string, string] | null>(null);
const manualReport = ref<any>(null);
const manualReportId = ref<number | null>(null);

const rangeShortcuts = [
  { text: "今天", value: () => dayRange(0) },
  { text: "近7天", value: () => recentRange(7) },
  { text: "近30天", value: () => recentRange(30) }
];

function dayRange(offset: number): [Date, Date] {
  const start = new Date();
  start.setDate(start.getDate() - offset);
  start.setHours(0, 0, 0, 0);
  const end = new Date();
  end.setHours(23, 59, 59, 0);
  return [start, end];
}
function recentRange(days: number): [Date, Date] {
  const end = new Date();
  const start = new Date();
  start.setDate(start.getDate() - days);
  return [start, end];
}

const handleGenerate = async () => {
  if (!manualRange.value || manualRange.value.length !== 2) {
    ElMessage.warning("请选择统计时间区间");
    return;
  }
  localStorage.setItem(MANUAL_RANGE_KEY, JSON.stringify(manualRange.value));
  generating.value = true;
  try {
    const res: any = await generateReport({ start: manualRange.value[0], end: manualRange.value[1] });
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || res?.msg || "生成失败");
      return;
    }
    manualReport.value = res.data?.content || null;
    manualReportId.value = res.data?.id ?? null;
    ElMessage.success("报表已生成");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "生成失败");
  } finally {
    generating.value = false;
  }
};

// ====== 报表记录 ======
const records = ref<any[]>([]);
const recordsTotal = ref(0);
const recordsPage = ref(1);
const recordsSize = ref(10);
const recordsLoading = ref(false);

const loadRecords = async () => {
  recordsLoading.value = true;
  try {
    const res: any = await getReportList(recordsPage.value, recordsSize.value);
    if (Number(res?.code) === 200 && res.data) {
      records.value = res.data.records || [];
      recordsTotal.value = Number(res.data.total || 0);
    }
  } catch (error: any) {
    ElMessage.error(error?.message || "加载报表记录失败");
  } finally {
    recordsLoading.value = false;
  }
};

const onPageChange = (page: number) => {
  recordsPage.value = page;
  loadRecords();
};

// ====== 查看 / 导出 / 删除 ======
const viewDialogVisible = ref(false);
const viewReport = ref<any>(null);
const viewReportId = ref<number | null>(null);

const handleView = async (id: number) => {
  try {
    const res: any = await getReportDetail(id);
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || "加载失败");
      return;
    }
    viewReport.value = res.data?.content || null;
    viewReportId.value = id;
    viewDialogVisible.value = true;
  } catch (error: any) {
    ElMessage.error(error?.message || "加载失败");
  }
};

const handleExport = async (id: number | null, format: "xlsx" | "pdf") => {
  if (!id) {
    ElMessage.warning("请先生成或选择报表");
    return;
  }
  try {
    const blob: any = await exportReportFile(id, format);
    triggerDownload(blob as Blob, `报表_${id}.${format}`);
  } catch (error: any) {
    ElMessage.error(error?.message || "导出失败");
  }
};

const triggerDownload = (blob: Blob, filename: string) => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  window.URL.revokeObjectURL(url);
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm("确认删除该报表记录吗？", "删除确认", { type: "warning" });
  } catch {
    return;
  }
  try {
    const res: any = await deleteReport(id);
    if (Number(res?.code) !== 200) {
      ElMessage.error(res?.message || "删除失败");
      return;
    }
    ElMessage.success("已删除");
    loadRecords();
  } catch (error: any) {
    ElMessage.error(error?.message || "删除失败");
  }
};

onMounted(() => {
  void loadSchedule();
  void loadRecords();
  const cached = localStorage.getItem(MANUAL_RANGE_KEY);
  if (cached) {
    try {
      const parsed = JSON.parse(cached);
      if (Array.isArray(parsed) && parsed.length === 2) {
        manualRange.value = parsed as [string, string];
      }
    } catch {
      // 忽略损坏的缓存
    }
  }
});
</script>

<style scoped>
.report-page {
  display: flex;
  flex-direction: column;
}
.report-tabs {
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
}
.schedule-form {
  max-width: 640px;
}
.interval-line {
  display: flex;
  align-items: center;
  gap: 6px;
}
.interval-line .unit {
  margin-right: 10px;
  color: #6b7280;
}
.form-tip {
  margin-left: 12px;
  color: #9ca3af;
  font-size: 13px;
}
.manual-bar {
  margin-bottom: 12px;
}
.manual-report {
  min-height: 200px;
}
.records-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.block-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}
.records-pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
