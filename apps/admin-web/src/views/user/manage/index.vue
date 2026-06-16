<template>
  <div class="user-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openAddDialog">{{ $t("userManage.addUser") }}</el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" :label="$t('info.username')" min-width="150" />
        <el-table-column :label="$t('info.role')" width="100" align="center">
          <template #default>
            <el-tag type="primary" size="small">{{ $t("info.admin") }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('info.status')" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :active-text="$t('info.enabled')"
              :inactive-text="$t('info.disabled')"
              inline-prompt
              @change="val => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('info.createTime')" width="180" align="center">
          <template #default="{ row }">{{ row.createTime || "-" }}</template>
        </el-table-column>
        <el-table-column :label="$t('order.operation')" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="addVisible" :title="$t('userManage.addUser')" width="460px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <el-form-item :label="$t('info.username')" prop="username">
          <el-input v-model="addForm.username" />
        </el-form-item>
        <el-form-item :label="$t('password.newPassword')" prop="password">
          <el-input v-model="addForm.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">{{ $t("password.cancel") }}</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd">{{ $t("password.confirm") }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="userManage">
import { onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Delete } from "@element-plus/icons-vue";
import { getAdminUserPage, createAdminUser, updateAdminUserStatus, deleteAdminUser } from "@/api/modules/user";
import type { AdminUserInfo } from "@/api/interface/index";

const { t } = useI18n();

const loading = ref(false);
const total = ref(0);
const tableData = ref<AdminUserInfo[]>([]);
const addVisible = ref(false);
const addLoading = ref(false);
const addFormRef = ref();

const searchForm = reactive({ page: 1, size: 10 });
const addForm = reactive({ username: "", password: "" });
const addRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于6位", trigger: "blur" }
  ]
};

const loadPage = async () => {
  loading.value = true;
  try {
    const res = await getAdminUserPage(searchForm);
    if (res.data) {
      tableData.value = res.data.records ?? res.data.list ?? [];
      total.value = res.data.total ?? 0;
    }
  } finally {
    loading.value = false;
  }
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

const openAddDialog = () => {
  addForm.username = "";
  addForm.password = "";
  addFormRef.value?.resetFields();
  addVisible.value = true;
};

const handleAdd = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  addLoading.value = true;
  try {
    await createAdminUser(addForm);
    ElMessage.success(t("userManage.createSuccess"));
    addVisible.value = false;
    loadPage();
  } finally {
    addLoading.value = false;
  }
};

const toSwitchEnabled = (val: string | number | boolean) => val === true || val === 1 || val === "1";

const handleStatusChange = async (row: AdminUserInfo, val: string | number | boolean) => {
  const enabled = toSwitchEnabled(val);
  try {
    await updateAdminUserStatus({ id: row.id, status: enabled ? 1 : 0 });
    row.status = enabled ? 1 : 0;
    ElMessage.success(t("userManage.statusSuccess"));
  } catch {
    // revert UI
    row.status = enabled ? 0 : 1;
  }
};

const handleDelete = (row: AdminUserInfo) => {
  ElMessageBox.confirm(t("userManage.deleteConfirm"), t("header.logoutTip"), {
    confirmButtonText: t("password.confirm"),
    cancelButtonText: t("password.cancel"),
    type: "warning"
  }).then(async () => {
    try {
      await deleteAdminUser(row.id);
      ElMessage.success(t("userManage.deleteSuccess"));
      loadPage();
    } catch {
      // error handled by interceptor
    }
  });
};

onMounted(() => loadPage());
</script>

<style scoped>
.user-page {
  padding: 0;
}
.toolbar {
  margin-bottom: 12px;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
