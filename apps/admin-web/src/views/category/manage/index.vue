<template>
  <div class="category-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openAdd">新增分类</el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="name" label="分类名称" min-width="120" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
              @change="val => handleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ row.createTime || "-" }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="categoryManage">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Edit, Delete } from "@element-plus/icons-vue";
import http from "@/api";

interface Category {
  id: number;
  name: string;
  sortOrder: number;
  status: number;
  createTime: string;
}

const loading = ref(false);
const tableData = ref<Category[]>([]);
const dialogVisible = ref(false);
const isEdit = ref(false);
const editingId = ref<number>(0);
const submitting = ref(false);
const formRef = ref();
const form = reactive({ name: "", sortOrder: 0, status: 1 });
const rules = {
  name: [{ required: true, message: "请输入分类名称", trigger: "blur" }]
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await http.get<Category[]>("/api/admin/category/list");
    tableData.value = res.data || [];
  } finally {
    loading.value = false;
  }
};

const openAdd = () => {
  isEdit.value = false;
  editingId.value = 0;
  form.name = "";
  form.sortOrder = 0;
  form.status = 1;
  formRef.value?.resetFields();
  dialogVisible.value = true;
};

const openEdit = (row: Category) => {
  isEdit.value = true;
  editingId.value = row.id;
  form.name = row.name;
  form.sortOrder = row.sortOrder;
  form.status = row.status;
  formRef.value?.resetFields();
  dialogVisible.value = true;
};

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    if (isEdit.value) {
      await http.put(`/api/admin/category/${editingId.value}`, { ...form });
    } else {
      await http.post("/api/admin/category", { ...form });
    }
    ElMessage.success(isEdit.value ? "更新成功" : "创建成功");
    dialogVisible.value = false;
    loadData();
  } finally {
    submitting.value = false;
  }
};

const toSwitchEnabled = (val: string | number | boolean) => val === true || val === 1 || val === "1";

const handleStatus = async (row: Category, val: string | number | boolean) => {
  const enabled = toSwitchEnabled(val);
  try {
    await http.put(`/api/admin/category/${row.id}`, { status: enabled ? 1 : 0 });
    row.status = enabled ? 1 : 0;
    ElMessage.success("状态更新成功");
  } catch {
    row.status = enabled ? 0 : 1;
  }
};

const handleDelete = (row: Category) => {
  ElMessageBox.confirm(`确定删除分类【${row.name}】吗？`, "删除确认", { type: "warning" }).then(async () => {
    await http.delete(`/api/admin/category/${row.id}`);
    ElMessage.success("删除成功");
    loadData();
  });
};

onMounted(() => loadData());
</script>

<style scoped>
.category-page {
  padding: 0;
}
.toolbar {
  margin-bottom: 12px;
}
</style>
