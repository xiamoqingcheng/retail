<template>
  <div class="goods-page">
    <el-card shadow="never">
      <!-- ===================== 搜索栏 ===================== -->
      <el-form :inline="true" :model="searchForm" class="search-form" @submit.prevent>
        <el-form-item :label="$t('goods.name')">
          <el-input v-model="searchForm.name" :placeholder="$t('goods.namePlaceholder')" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item :label="$t('goods.category')">
          <el-select
            v-model="searchForm.categoryId"
            :placeholder="$t('goods.allCategory')"
            clearable
            style="width: 160px"
            @change="handleSearch"
          >
            <el-option v-for="c in categoryList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">{{ $t("goods.search") }}</el-button>
          <el-button :icon="Refresh" @click="handleReset">{{ $t("goods.reset") }}</el-button>
        </el-form-item>
        <el-form-item class="add-btn-wrap">
          <el-button type="success" :icon="Plus" @click="openAddDialog">{{ $t("goods.addGoods") }}</el-button>
        </el-form-item>
      </el-form>

      <!-- ===================== 数据表格 ===================== -->
      <el-table :data="tableData" border stripe v-loading="loading" class="goods-table" @sort-change="handleSortChange">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column :label="$t('goods.image')" width="120" align="center">
          <template #default="{ row }">
            <div class="goods-image-frame">
              <el-image
                v-if="hasGoodsImage(row.imageUrl)"
                :src="goodsImageUrl(row.imageUrl)"
                :preview-src-list="[goodsImageUrl(row.imageUrl)]"
                fit="cover"
                class="goods-image"
                preview-teleported
              >
                <template #error>
                  <div class="image-fallback">
                    <el-icon><Picture /></el-icon>
                    <span>{{ $t("goods.noImage") }}</span>
                  </div>
                </template>
              </el-image>
              <div v-else class="image-fallback">
                <el-icon><Picture /></el-icon>
                <span>{{ $t("goods.noImage") }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('goods.name')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="$t('goods.priceLabel')" width="130" align="center" sortable="custom" prop="price">
          <template #default="{ row }">
            <span class="price-text">{{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" :label="$t('goods.stock')" width="110" align="center" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="row.stock >= (row.safeStock || 10) ? 'success' : row.stock > 0 ? 'warning' : 'danger'" size="small">
              {{ row.stock }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('goods.category')"
          width="110"
          align="center"
          sortable="custom"
          :sort-method="(a: any, b: any) => categorySort(a, b)"
        >
          <template #default="{ row }">
            {{ categoryList.find(c => c.id === row.categoryId)?.name || "-" }}
          </template>
        </el-table-column>
        <el-table-column prop="shelfId" :label="$t('goods.shelfId')" min-width="150" align="center" sortable="custom">
          <template #default="{ row }">
            <template v-if="splitShelves(row.shelfId).length">
              <el-tag v-for="s in splitShelves(row.shelfId)" :key="s" size="small" class="shelf-tag">{{ s }}</el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('goods.operation')" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="openEditDialog(row)">{{ $t("goods.edit") }}</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">{{ $t("goods.delete") }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- ===================== 分页器 ===================== -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="searchForm.page"
          v-model:page-size="searchForm.size"
          :total="total"
          :page-sizes="[5, 10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- ===================== 新增 / 编辑弹窗 ===================== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close @closed="resetFormData">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="88px">
        <el-form-item :label="$t('goods.name')" prop="name">
          <el-input v-model="formData.name" :placeholder="$t('goods.namePlaceholder')" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item :label="$t('goods.price')" prop="price">
          <el-input-number
            v-model="formData.price"
            :min="0"
            :max="999999"
            :precision="2"
            :step="0.1"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item :label="$t('goods.stock')" prop="stock">
          <el-input-number
            v-model="formData.stock"
            :min="0"
            :max="999999"
            :step="1"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item :label="$t('goods.safeStock')" prop="safeStock">
          <el-input-number
            v-model="formData.safeStock"
            :min="0"
            :max="999999"
            :step="1"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item :label="$t('goods.shelfId')" prop="shelfId">
          <el-input v-model="formData.shelfId" :placeholder="$t('goods.shelfPlaceholder')" maxlength="255" />
        </el-form-item>

        <el-form-item :label="$t('goods.category')" prop="categoryId">
          <div class="category-field">
            <div class="category-select-line">
              <el-select v-model="formData.categoryId" :placeholder="$t('goods.categoryPlaceholder')" clearable class="category-select">
                <el-option v-for="c in categoryList" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
              <el-tooltip :content="$t('goods.quickCreateCategory')" placement="top">
                <el-button :icon="Plus" @click="quickCategoryVisible = !quickCategoryVisible" />
              </el-tooltip>
            </div>
            <div v-if="quickCategoryVisible" class="quick-category">
              <el-input
                v-model="quickCategoryForm.name"
                :placeholder="$t('goods.quickCategoryPlaceholder')"
                maxlength="30"
                clearable
                @keyup.enter="handleQuickCreateCategory"
              />
              <el-input-number
                v-model="quickCategoryForm.sortOrder"
                :min="0"
                :max="999"
                controls-position="right"
                class="quick-category-sort"
              />
              <el-button type="primary" :icon="Check" :loading="quickCategoryLoading" @click="handleQuickCreateCategory">
                {{ $t("goods.createCategory") }}
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item :label="$t('goods.image')" prop="imageUrl">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            accept="image/*"
          >
            <el-image v-if="hasGoodsImage(formData.imageUrl)" :src="goodsImageUrl(formData.imageUrl)" fit="cover" class="upload-preview">
              <template #error>
                <div class="upload-fallback">
                  <el-icon :size="26"><Picture /></el-icon>
                  <span>{{ $t("goods.noImage") }}</span>
                </div>
              </template>
            </el-image>
            <div v-else class="upload-placeholder">
              <el-icon :size="28"><Plus /></el-icon>
              <span>{{ $t("goods.uploadImage") }}</span>
            </div>
          </el-upload>
          <div class="upload-tip">{{ $t("goods.uploadTip") }}</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t("header.cancel") }}</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">{{ $t("header.confirm") }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="goodsManage">
import { computed, onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Refresh, Plus, Edit, Delete, Picture, Check } from "@element-plus/icons-vue";
import type { FormInstance, FormRules, UploadProps, UploadRawFile } from "element-plus";
import { getGoodsPage, addGoods, updateGoods, deleteGoods, getCategories, createCategory } from "@/api/modules/goods";
import type { Goods } from "@/api/modules/goods";
import { useUserStore } from "@/stores/modules/user";

const { t } = useI18n();

// ======================== 状态定义 ========================

const userStore = useUserStore();

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref(t("goods.addTitle"));
const isEdit = ref(false);
const total = ref(0);
const tableData = ref<Goods.GoodsItem[]>([]);
const formRef = ref<FormInstance>();

const categoryList = ref<Goods.CategoryItem[]>([]);
const quickCategoryVisible = ref(false);
const quickCategoryLoading = ref(false);
const quickCategoryForm = reactive({ name: "", sortOrder: 0 });

const searchForm = reactive<Goods.ReqGoodsParams & { categoryId?: number }>({
  page: 1,
  size: 10,
  name: "",
  categoryId: undefined,
  sortField: undefined,
  sortOrder: undefined
});

const initFormData: Goods.GoodsItem & { categoryId?: number | null } = {
  id: null,
  name: "",
  price: 0,
  stock: 0,
  safeStock: 10,
  categoryId: null,
  shelfId: "",
  imageUrl: ""
};

const formData = reactive<Goods.GoodsItem & { categoryId?: number | null }>({ ...initFormData });

// ======================== 表单校验规则 ========================

const rules: FormRules = {
  name: [{ required: true, message: () => t("goods.namePlaceholder"), trigger: "blur" }],
  price: [{ required: true, message: () => t("goods.pricePlaceholder"), trigger: "change" }],
  stock: [{ required: true, message: () => t("goods.stockPlaceholder"), trigger: "change" }],
  shelfId: [{ required: true, message: () => t("goods.shelfPlaceholder"), trigger: "blur" }]
};

// ======================== 图片上传 ========================

/** 上传地址 —— el-upload 不走 axios，需要完整路径 */
const uploadAction = "/api/upload";

/** 上传请求头 —— 手动携带 Token（Authorization + x-access-token 双保险） */
const uploadHeaders = computed(() => {
  const token = userStore.token;
  if (!token) return {};
  return {
    Authorization: `Bearer ${token}`,
    "x-access-token": token
  };
});

/** 上传前校验文件大小与类型 */
const beforeUpload: UploadProps["beforeUpload"] = (file: UploadRawFile) => {
  const isImage = file.type.startsWith("image/");
  const isLt5M = file.size / 1024 / 1024 < 5;
  if (!isImage) {
    ElMessage.error(t("goods.onlyImage"));
    return false;
  }
  if (!isLt5M) {
    ElMessage.error(t("goods.imageTooLarge"));
    return false;
  }
  return true;
};

/**
 * 上传成功回调
 * 后端返回结构: { code: 200, message: "...", data: "图片URL字符串" }
 * 注意: el-upload 直接拿到原始 response，不经过 axios 拦截器
 */
const handleUploadSuccess: UploadProps["onSuccess"] = (response: any) => {
  if (response?.code !== 200 || !response?.data) {
    ElMessage.error(response?.message || t("goods.uploadFailed"));
    return;
  }
  formData.imageUrl = response.data;
  // 手动触发 imageUrl 字段校验清除
  formRef.value?.clearValidate("imageUrl");
  ElMessage.success(t("goods.uploadSuccess"));
};

/** 上传失败回调 */
const handleUploadError: UploadProps["onError"] = () => {
  ElMessage.error(t("goods.uploadFailed"));
};

// ======================== 工具方法 ========================

/** 格式化价格为两位小数 */
const formatPrice = (price: number | string | null | undefined): string => {
  if (price === null || price === undefined || price === "") return "0.00";
  return Number(price).toFixed(2);
};

const hasGoodsImage = (url: string | null | undefined) => !!url && !!String(url).trim();

const goodsImageUrl = (url: string | null | undefined) => {
  const value = (url || "").trim();
  if (!value) return "";
  if (/^(https?:)?\/\//.test(value) || value.startsWith("data:") || value.startsWith("blob:")) {
    return value;
  }
  return value;
};

/** 分类名称排序 */
const categorySort = (a: Goods.GoodsItem, b: Goods.GoodsItem) => {
  const nameA = categoryList.value.find(c => c.id === a.categoryId)?.name || "";
  const nameB = categoryList.value.find(c => c.id === b.categoryId)?.name || "";
  return nameA.localeCompare(nameB, "zh");
};

// 货架号可能是「逗号分隔的多货架」（由全量扫描识别同一商品在多个货架时写入），拆分为标签展示
const splitShelves = (shelfId?: string | null): string[] => {
  if (!shelfId) return [];
  return String(shelfId)
    .split(",")
    .map(s => s.trim())
    .filter(Boolean);
};

// ======================== 数据加载 ========================

/**
 * 加载商品分页列表
 *
 * 关键说明:
 *   Retail-Admin 的 http 拦截器在响应成功时直接返回 response.data（即后端的整个 Result 对象）：
 *     { code: 200, message: "操作成功", data: { records: [...], total: 100 } }
 *
 *   http.get<T>() 返回 Promise<ResultData<T>>，其中 ResultData<T> = { code, msg, data: T }
 *   因此: res.data => { records, total }
 */
const loadPage = async () => {
  loading.value = true;
  try {
    const params: Goods.ReqGoodsParams = {
      page: searchForm.page,
      size: searchForm.size
    };
    if (searchForm.name?.trim()) {
      params.name = searchForm.name.trim();
    }
    if (searchForm.categoryId && searchForm.categoryId > 0) {
      params.categoryId = searchForm.categoryId;
    }
    if (searchForm.sortField) {
      params.sortField = searchForm.sortField;
      params.sortOrder = searchForm.sortOrder;
    }

    const res = await getGoodsPage(params);

    // res 已经是 ResultData<Goods.ResGoodsPage>，即 { code, msg/message, data: { records, total } }
    // 拦截器已处理了非 200 的 code，所以此处直接取 data
    tableData.value = res.data?.records ?? [];
    total.value = res.data?.total ?? 0;
  } catch (error: any) {
    // 拦截器已弹出 ElMessage.error，此处仅做兜底
    console.error("查询商品列表失败:", error);
  } finally {
    loading.value = false;
  }
};

// ======================== 搜索 & 分页 ========================

const handleSearch = () => {
  searchForm.page = 1;
  loadPage();
};

const handleReset = () => {
  searchForm.name = "";
  searchForm.categoryId = undefined;
  searchForm.page = 1;
  searchForm.size = 10;
  searchForm.sortField = undefined;
  searchForm.sortOrder = undefined;
  loadPage();
};

const handleSortChange = ({ prop, order }: { prop: string; order: string | null }) => {
  searchForm.sortField = order ? prop : undefined;
  searchForm.sortOrder = order ? (order === "ascending" ? "asc" : "desc") : undefined;
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

// ======================== 弹窗表单 ========================

/** 重置表单数据到初始状态 */
const resetFormData = () => {
  Object.assign(formData, { ...initFormData });
  quickCategoryVisible.value = false;
  quickCategoryForm.name = "";
  quickCategoryForm.sortOrder = 0;
  formRef.value?.resetFields();
};

/** 打开新增弹窗 */
const openAddDialog = () => {
  isEdit.value = false;
  dialogTitle.value = t("goods.addTitle");
  // 先 reset 再打开，保持干净状态
  Object.assign(formData, { ...initFormData });
  dialogVisible.value = true;
};

/** 打开编辑弹窗 */
const openEditDialog = (row: Goods.GoodsItem) => {
  isEdit.value = true;
  dialogTitle.value = t("goods.editTitle");
  Object.assign(formData, {
    id: row.id ?? null,
    name: row.name ?? "",
    price: Number(row.price ?? 0),
    stock: Number(row.stock ?? 0),
    safeStock: Number((row as any).safeStock ?? 10),
    categoryId: (row as any).categoryId ?? null,
    shelfId: row.shelfId ?? "",
    imageUrl: row.imageUrl ?? ""
  });
  dialogVisible.value = true;
};

/** 提交表单（新增 / 编辑） */
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitLoading.value = true;
  try {
    const payload: Partial<Goods.GoodsItem> & { categoryId?: number | null } = {
      name: formData.name,
      price: formData.price,
      stock: formData.stock,
      safeStock: formData.safeStock,
      categoryId: formData.categoryId,
      shelfId: formData.shelfId,
      imageUrl: formData.imageUrl
    };

    if (isEdit.value) {
      payload.id = formData.id;
      await updateGoods(payload as Goods.GoodsItem & { id: number });
      ElMessage.success(t("goods.updateSuccess"));
    } else {
      await addGoods(payload);
      ElMessage.success(t("goods.addSuccess"));
    }

    dialogVisible.value = false;
    await loadPage();
  } catch (error: any) {
    // 拦截器已弹出错误提示，此处做兜底日志
    console.error("保存商品失败:", error);
  } finally {
    submitLoading.value = false;
  }
};

const handleQuickCreateCategory = async () => {
  const name = quickCategoryForm.name.trim();
  if (!name) {
    ElMessage.warning(t("goods.quickCategoryPlaceholder"));
    return;
  }

  const existing = categoryList.value.find(item => item.name === name);
  if (existing) {
    formData.categoryId = existing.id;
    formRef.value?.clearValidate("categoryId");
    quickCategoryVisible.value = false;
    ElMessage.success(t("goods.categorySelected"));
    return;
  }

  quickCategoryLoading.value = true;
  try {
    const res = await createCategory({
      name,
      sortOrder: quickCategoryForm.sortOrder,
      status: 1
    });
    await loadCategories();
    const createdId = res.data?.id ?? categoryList.value.find(item => item.name === name)?.id;
    if (createdId) {
      formData.categoryId = createdId;
      formRef.value?.clearValidate("categoryId");
    }
    quickCategoryForm.name = "";
    quickCategoryForm.sortOrder = 0;
    quickCategoryVisible.value = false;
    ElMessage.success(t("goods.createCategorySuccess"));
  } finally {
    quickCategoryLoading.value = false;
  }
};

// ======================== 删除 ========================

const handleDelete = async (row: Goods.GoodsItem) => {
  try {
    await ElMessageBox.confirm(t("goods.deleteConfirm", { name: row.name }), t("goods.deleteTitle"), {
      type: "warning",
      confirmButtonText: t("goods.confirmDelete"),
      cancelButtonText: t("header.cancel"),
      confirmButtonClass: "el-button--danger"
    });

    if (!row.id) {
      ElMessage.error(t("goods.invalidId"));
      return;
    }
    await deleteGoods(row.id);
    ElMessage.success(t("goods.deleteSuccess"));

    // 删除当前页最后一条时，自动退到上一页
    if (tableData.value.length === 1 && searchForm.page > 1) {
      searchForm.page -= 1;
    }
    await loadPage();
  } catch (error: any) {
    // ElMessageBox 取消时 error === "cancel"，不需要提示
    if (error !== "cancel" && error !== "close") {
      console.error("删除商品失败:", error);
    }
  }
};

// ======================== 生命周期 ========================

const loadCategories = async () => {
  try {
    const res = await getCategories();
    categoryList.value = res.data ?? [];
  } catch (e) {
    console.error("加载分类失败:", e);
  }
};

onMounted(() => {
  loadPage();
  loadCategories();
});
</script>

<style scoped>
.goods-page {
  padding: 0;
  background: transparent;
}

.shelf-tag {
  margin: 2px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 0;
  margin-bottom: 12px;
}

.add-btn-wrap {
  margin-left: auto;
}

.goods-table {
  width: 100%;
}

.goods-image-frame {
  width: 56px;
  height: 56px;
  margin: 0 auto;
}

.goods-image {
  width: 56px;
  height: 56px;
  border-radius: 6px;
}

.image-fallback {
  width: 56px;
  height: 56px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
  color: #909399;
  font-size: 11px;
  line-height: 1;
}

.price-text {
  color: #e6a23c;
  font-weight: 600;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.upload-preview {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  transition: border-color 0.2s;
}

.upload-preview:hover {
  border-color: #409eff;
}

.upload-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100px;
  height: 100px;
  color: #909399;
  background: #f5f7fa;
  font-size: 12px;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  color: #909399;
  transition:
    border-color 0.2s,
    color 0.2s;
}

.upload-placeholder:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-placeholder span {
  margin-top: 6px;
  font-size: 12px;
}

.upload-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.category-field {
  width: 100%;
}

.category-select-line {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.category-select {
  flex: 1;
}

.quick-category {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 104px auto;
  gap: 8px;
  margin-top: 8px;
}

.quick-category-sort {
  width: 104px;
}

@media (max-width: 640px) {
  .quick-category {
    grid-template-columns: 1fr;
  }

  .quick-category-sort {
    width: 100%;
  }
}
</style>
