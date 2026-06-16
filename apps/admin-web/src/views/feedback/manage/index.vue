<template>
  <div class="feedback-page">
    <el-card shadow="never" class="feedback-card">
      <template #header>
        <div class="table-header">
          <div>
            <div class="table-title">反馈处理</div>
            <div class="table-subtitle">小程序用户提交的问题、建议和诊断信息</div>
          </div>
          <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部状态" style="width: 150px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已处理" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.feedbackType" clearable placeholder="全部类型" style="width: 170px">
            <el-option v-for="item in feedbackTypes" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border stripe v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="feedbackType" label="类型" width="130" />
        <el-table-column label="内容" min-width="260">
          <template #default="{ row }">
            <span class="content-text">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contact" label="联系方式" min-width="150">
          <template #default="{ row }">{{ row.contact || "-" }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row)">查看</el-button>
            <el-button v-if="row.status === 'PENDING'" type="warning" link :icon="Clock" @click="quickUpdate(row, 'PROCESSING')">
              处理中
            </el-button>
            <el-button v-if="row.status !== 'RESOLVED'" type="success" link :icon="Check" @click="openDetail(row, 'RESOLVED')">
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="searchForm.page"
          v-model:page-size="searchForm.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="反馈详情" size="520px" destroy-on-close>
      <div v-if="current" class="detail-panel">
        <div class="detail-row">
          <span>类型</span>
          <strong>{{ current.feedbackType }}</strong>
        </div>
        <div class="detail-row">
          <span>状态</span>
          <el-tag :type="statusMeta(current.status).type">{{ statusMeta(current.status).label }}</el-tag>
        </div>
        <div class="detail-row">
          <span>提交时间</span>
          <strong>{{ current.createTime || "-" }}</strong>
        </div>
        <div class="detail-row">
          <span>联系方式</span>
          <strong>{{ current.contact || "-" }}</strong>
        </div>

        <div class="detail-block">
          <div class="block-title">反馈内容</div>
          <p>{{ current.content }}</p>
        </div>

        <div class="detail-block">
          <div class="block-title">
            诊断信息
            <el-button link type="primary" :icon="DocumentCopy" @click="copyDiagnostics">复制</el-button>
          </div>
          <pre>{{ diagnosticsText }}</pre>
        </div>

        <el-form label-position="top">
          <el-form-item label="处理状态">
            <el-radio-group v-model="detailForm.status">
              <el-radio-button label="PROCESSING">处理中</el-radio-button>
              <el-radio-button label="RESOLVED">已处理</el-radio-button>
              <el-radio-button label="CLOSED">关闭</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理备注">
            <el-input v-model="detailForm.reply" type="textarea" :rows="4" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>

        <div class="drawer-actions">
          <el-button @click="detailVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveDetailStatus">保存处理结果</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="feedbackManage">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Check, Clock, DocumentCopy, Refresh, Search, View } from "@element-plus/icons-vue";
import { AdminFeedback, getFeedbackPage, updateFeedbackStatus } from "@/api/modules/feedback";

const feedbackTypes = ["功能异常", "商品/订单", "支付/余额", "连接问题", "其他建议", "OTHER"];

const loading = ref(false);
const saving = ref(false);
const total = ref(0);
const tableData = ref<AdminFeedback[]>([]);
const detailVisible = ref(false);
const current = ref<AdminFeedback | null>(null);

const searchForm = reactive({
  page: 1,
  size: 10,
  status: "",
  feedbackType: ""
});

const detailForm = reactive({
  status: "PROCESSING",
  reply: ""
});

const statusMeta = (status: string) => {
  const map: Record<string, { label: string; type: "success" | "warning" | "info" | "danger" | "primary" }> = {
    PENDING: { label: "待处理", type: "danger" },
    PROCESSING: { label: "处理中", type: "warning" },
    RESOLVED: { label: "已处理", type: "success" },
    CLOSED: { label: "已关闭", type: "info" }
  };
  return map[status] || { label: status || "未知", type: "info" };
};

const diagnosticsText = computed(() => {
  const item = current.value;
  if (!item) return "";
  return [`API: ${item.apiBaseUrl || "-"}`, `设备: ${item.systemInfo || "-"}`, item.diagnosticInfo || ""]
    .filter(Boolean)
    .join("\n");
});

const loadPage = async () => {
  loading.value = true;
  try {
    const res: any = await getFeedbackPage({
      page: searchForm.page,
      size: searchForm.size,
      status: searchForm.status || undefined,
      feedbackType: searchForm.feedbackType || undefined
    });
    const data = res?.data || {};
    tableData.value = data.records || [];
    total.value = data.total || 0;
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  searchForm.page = 1;
  void loadPage();
};

const handleReset = () => {
  searchForm.status = "";
  searchForm.feedbackType = "";
  searchForm.page = 1;
  void loadPage();
};

const handleSizeChange = (size: number) => {
  searchForm.size = size;
  searchForm.page = 1;
  void loadPage();
};

const handleCurrentChange = (page: number) => {
  searchForm.page = page;
  void loadPage();
};

const openDetail = (row: AdminFeedback, nextStatus?: string) => {
  current.value = row;
  detailForm.status = nextStatus || (row.status === "PENDING" ? "PROCESSING" : row.status);
  detailForm.reply = row.reply || "";
  detailVisible.value = true;
};

const quickUpdate = async (row: AdminFeedback, status: string) => {
  await updateFeedbackStatus(row.id, { status, reply: row.reply || "" });
  ElMessage.success("状态已更新");
  await loadPage();
};

const saveDetailStatus = async () => {
  if (!current.value) return;
  saving.value = true;
  try {
    await updateFeedbackStatus(current.value.id, {
      status: detailForm.status,
      reply: detailForm.reply
    });
    ElMessage.success("处理结果已保存");
    detailVisible.value = false;
    await loadPage();
  } finally {
    saving.value = false;
  }
};

const copyDiagnostics = async () => {
  try {
    await navigator.clipboard.writeText(diagnosticsText.value);
    ElMessage.success("诊断信息已复制");
  } catch {
    ElMessage.warning("复制失败，请手动选择文本");
  }
};

onMounted(() => {
  void loadPage();
});
</script>

<style scoped>
.feedback-page {
  padding: 0;
}

.feedback-card {
  border-radius: 8px;
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

.table-subtitle {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.filter-form {
  margin-bottom: 12px;
}

.content-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #edf0f5;
  color: #6b7280;
}

.detail-row strong {
  color: #111827;
  font-weight: 600;
  word-break: break-all;
}

.detail-block {
  border: 1px solid #edf0f5;
  border-radius: 8px;
  padding: 14px;
  background: #fafafa;
}

.block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #111827;
  font-weight: 600;
}

.detail-block p,
.detail-block pre {
  margin: 0;
  color: #374151;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}
</style>
