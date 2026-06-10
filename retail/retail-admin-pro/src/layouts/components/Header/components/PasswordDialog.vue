<template>
  <el-dialog v-model="dialogVisible" :title="$t('password.title')" width="500px" draggable>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item :label="$t('password.oldPassword')" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item :label="$t('password.newPassword')" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item :label="$t('password.confirmPassword')" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">{{ $t("password.cancel") }}</el-button>
        <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t("password.confirm") }}</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import { changePassword } from "@/api/modules/user";

const { t } = useI18n();
const dialogVisible = ref(false);
const loading = ref(false);
const formRef = ref();

const form = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: ""
});

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== form.newPassword) {
    callback(new Error(t("password.passwordMismatch")));
  } else {
    callback();
  }
};

const rules = {
  oldPassword: [{ required: true, message: () => t("password.oldPasswordRequired"), trigger: "blur" }],
  newPassword: [
    { required: true, message: () => t("password.newPasswordRequired"), trigger: "blur" },
    { min: 6, message: () => t("password.passwordMinLength"), trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, message: () => t("password.confirmPasswordRequired"), trigger: "blur" },
    { validator: validateConfirm, trigger: "blur" }
  ]
};

const openDialog = () => {
  form.oldPassword = "";
  form.newPassword = "";
  form.confirmPassword = "";
  formRef.value?.resetFields();
  dialogVisible.value = true;
};

const handleConfirm = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    await changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword });
    ElMessage.success(t("password.changeSuccess"));
    dialogVisible.value = false;
  } finally {
    loading.value = false;
  }
};

defineExpose({ openDialog });
</script>
