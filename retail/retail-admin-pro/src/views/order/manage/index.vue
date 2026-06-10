<template>
  <div class="order-page">
    <el-card shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form" @submit.prevent>
        <el-form-item :label="$t('order.status')">
          <el-select v-model="searchForm.status" :placeholder="$t('order.allStatus')" clearable @change="handleSearch">
            <el-option :label="$t('order.pending')" value="PENDING" />
            <el-option :label="$t('order.paid')" value="PAID" />
            <el-option :label="$t('order.completed')" value="COMPLETED" />
            <el-option :label="$t('order.cancelled')" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">{{ $t("order.search") }}</el-button>
          <el-button :icon="Refresh" @click="handleReset">{{ $t("order.reset") }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border stripe v-loading="loading" class="order-table">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="id" :label="$t('order.orderNo')" width="100" align="center" />
        <el-table-column :label="$t('order.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.amount')" width="120" align="center">
          <template #default="{ row }">￥{{ Number(row.totalAmount ?? 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column :label="$t('order.goods')" min-width="280">
          <template #default="{ row }">
            <div class="goods-mini" v-for="g in (row.goods || []).slice(0, 3)" :key="g.goodsId">
              <span class="goods-name">{{ g.goodsName }}</span>
              <span class="goods-qty">x{{ g.quantity }}</span>
            </div>
            <el-tag v-if="(row.goods || []).length > 3" size="small" type="info">
              +{{ row.goods.length - 3 }} {{ $t("order.moreItems") }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('order.orderTime')" width="180" align="center">
          <template #default="{ row }">{{ row.createTime || "-" }}</template>
        </el-table-column>
        <el-table-column :label="$t('order.operation')" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="openDetail(row)">{{ $t("order.detail") }}</el-button>
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

    <el-dialog v-model="detailVisible" :title="$t('order.detailTitle')" width="560px" destroy-on-close>
      <template v-if="currentOrder">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="$t('order.orderNo')">{{ currentOrder.id }}</el-descriptions-item>
          <el-descriptions-item :label="$t('order.status')">
            <el-tag :type="statusTag(currentOrder.status)" size="small">{{ statusText(currentOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('order.totalAmount')">
            ￥{{ Number(currentOrder.totalAmount ?? 0).toFixed(2) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('order.orderTime')">{{ currentOrder.createTime || "-" }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">{{ $t("order.goodsDetail") }}</el-divider>
        <el-table :data="currentOrder.goods || []" size="small" border stripe>
          <el-table-column :label="$t('order.goodsName')" min-width="200">
            <template #default="{ row: g }">{{ g.goodsName || $t("order.unknownGoods") }}</template>
          </el-table-column>
          <el-table-column :label="$t('order.unitPrice')" width="100" align="center">
            <template #default="{ row: g }">￥{{ Number(g.goodsPrice ?? 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column :label="$t('order.quantity')" width="80" align="center" prop="quantity" />
          <el-table-column :label="$t('order.subtotal')" width="100" align="center">
            <template #default="{ row: g }">￥{{ (Number(g.goodsPrice ?? 0) * Number(g.quantity ?? 0)).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="orderManage">
import { onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import { Search, Refresh, View } from "@element-plus/icons-vue";
import { getAdminOrderPage, getAdminOrderDetail } from "@/api/modules/order";
import type { AdminOrder } from "@/api/modules/order";

const { t } = useI18n();

const loading = ref(false);
const total = ref(0);
const tableData = ref<AdminOrder[]>([]);
const detailVisible = ref(false);
const currentOrder = ref<AdminOrder | null>(null);

const searchForm = reactive({ page: 1, size: 10, status: "" as string });

const statusText = (s: string) => {
  const m: Record<string, string> = {
    PENDING: t("order.pending"),
    PAID: t("order.paid"),
    COMPLETED: t("order.completed"),
    CANCELLED: t("order.cancelled")
  };
  return m[s] || s || "-";
};

const statusTag = (s: string): "success" | "warning" | "info" | "danger" => {
  const m: Record<string, "success" | "warning" | "info" | "danger"> = {
    PENDING: "warning",
    PAID: "success",
    COMPLETED: "info",
    CANCELLED: "danger"
  };
  return m[s] || "info";
};

const loadPage = async () => {
  loading.value = true;
  try {
    const params: { page: number; size: number; status?: string } = { page: searchForm.page, size: searchForm.size };
    if (searchForm.status) params.status = searchForm.status;
    const res = await getAdminOrderPage(params);
    tableData.value = res.data?.records ?? [];
    total.value = res.data?.total ?? 0;
  } catch (e: any) {
    console.error("load orders failed:", e);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  searchForm.page = 1;
  loadPage();
};
const handleReset = () => {
  searchForm.status = "";
  searchForm.page = 1;
  loadPage();
};
const handleSizeChange = (size: number) => {
  searchForm.size = size;
  searchForm.page = 1;
  loadPage();
};
const handleCurrentChange = (page: number) => {
  searchForm.page = page;
  loadPage();
};

const openDetail = async (row: AdminOrder) => {
  loading.value = true;
  try {
    const res = await getAdminOrderDetail(row.id);
    currentOrder.value = res.data ?? null;
    detailVisible.value = true;
  } catch {
    ElMessage.error(t("order.detailFailed"));
  } finally {
    loading.value = false;
  }
};

onMounted(() => loadPage());
</script>

<style scoped>
.order-page {
  padding: 0;
}
.search-form {
  margin-bottom: 12px;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.goods-mini {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
}
.goods-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.goods-qty {
  color: #999;
  flex-shrink: 0;
  margin-left: 8px;
}
</style>
