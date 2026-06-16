<template>
  <div class="log-page">
    <el-card shadow="never">
      <el-form :inline="true" :model="searchForm" class="filter-form" @submit.prevent>
        <el-form-item label="日志类型">
          <el-select v-model="searchForm.type" placeholder="全部" clearable class="type-select" @change="handleSearch">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无库存日志">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="id" label="日志ID" width="90" align="center" />
        <el-table-column prop="goodsId" label="商品ID" width="90" align="center" />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变动数量" width="110" align="center">
          <template #default="{ row }">
            <span :class="changeClass(row.changeAmount)">
              {{ formatChange(row.changeAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="currentStock" label="当前库存" width="110" align="center" />
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || "-" }}</template>
        </el-table-column>
        <el-table-column label="时间" width="180" align="center">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
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
  </div>
</template>

<script setup lang="ts" name="inventoryLog">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import http from "@/api";

type InventoryLogType = "SALE" | "RESTOCK" | "ADJUST" | "AI_DETECT";
type TagType = "danger" | "success" | "warning" | "info";

interface LogItem {
  id: number;
  goodsId: number;
  changeAmount: number;
  currentStock: number;
  type: InventoryLogType | string;
  remark?: string;
  createTime?: string;
}

interface PageResult<T> {
  records?: T[];
  total?: number;
  page?: number;
  size?: number;
}

const typeOptions: Array<{ label: string; value: InventoryLogType; tag: TagType }> = [
  { label: "销售扣减", value: "SALE", tag: "danger" },
  { label: "补货入库", value: "RESTOCK", tag: "success" },
  { label: "手动调整", value: "ADJUST", tag: "warning" },
  { label: "AI检测", value: "AI_DETECT", tag: "info" }
];

const typeMap = new Map(typeOptions.map(item => [item.value, item]));

const loading = ref(false);
const total = ref(0);
const tableData = ref<LogItem[]>([]);
const searchForm = reactive({ page: 1, size: 10, type: "" });

const typeText = (type: string) => typeMap.get(type as InventoryLogType)?.label || type || "-";
const typeTag = (type: string): TagType => typeMap.get(type as InventoryLogType)?.tag || "info";

const changeClass = (value: number | null | undefined) => {
  const amount = Number(value || 0);
  return amount >= 0 ? "change-positive" : "change-negative";
};

const formatChange = (value: number | null | undefined) => {
  const amount = Number(value || 0);
  return `${amount >= 0 ? "+" : ""}${amount}`;
};

const formatTime = (value?: string) => {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
};

const loadData = async () => {
  loading.value = true;
  try {
    const params: Record<string, number | string> = {
      page: searchForm.page,
      size: searchForm.size
    };
    if (searchForm.type) params.type = searchForm.type;

    const res = await http.get<PageResult<LogItem>>("/api/admin/inventory-log/page", params);
    const data = res?.data || {};
    tableData.value = Array.isArray(data.records) ? data.records : [];
    total.value = Number(data.total || 0);
  } catch (error: any) {
    tableData.value = [];
    total.value = 0;
    ElMessage.error(error?.message || "库存日志加载失败");
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  searchForm.page = 1;
  void loadData();
};

const handleReset = () => {
  searchForm.type = "";
  searchForm.page = 1;
  void loadData();
};

const handleSizeChange = (size: number) => {
  searchForm.size = size;
  searchForm.page = 1;
  void loadData();
};

const handleCurrentChange = (page: number) => {
  searchForm.page = page;
  void loadData();
};

onMounted(() => {
  void loadData();
});
</script>

<style scoped>
.log-page {
  padding: 0;
}

.filter-form {
  margin-bottom: 12px;
}

.type-select {
  width: 180px;
}

.change-positive {
  color: #10b981;
  font-weight: 600;
}

.change-negative {
  color: #ef4444;
  font-weight: 600;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
