<script setup>
import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

// Props
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  registerForm: {
    type: Object,
    required: true
  },
  showPassword: {
    type: Boolean,
    default: false
  },
  showPasswordRepeat: {
    type: Boolean,
    default: false
  }
});

// Emits
const emit = defineEmits([
  'update:modelValue',
  'update:registerForm',
  'update:showPassword', 
  'update:showPasswordRepeat',
  'submit',
  'togglePassword'
]);

// Computed properties for v-model
const dialogModel = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

const registerFormModel = computed({
  get: () => props.registerForm,
  set: (value) => emit('update:registerForm', value)
});

// 处理密码可见性切换
const handleTogglePassword = (repeat = false) => {
  emit('togglePassword', repeat);
};

// 处理表单提交
const handleSubmit = () => {
  emit('submit');
};

// 关闭对话框
const closeDialog = () => {
  dialogModel.value = false;
};
</script>

<template>
  <v-dialog max-width="600" v-model="dialogModel" persistent>
    <template v-slot:default="{ isActive }">
      <v-card rounded="xl" elevation="0">
        <v-card-title class="pa-6 pb-4">
          <div class="d-flex align-center">
            <v-avatar color="primary" size="40" class="mr-3">
              <v-icon icon="mdi-account-plus" color="white" size="20"></v-icon>
            </v-avatar>
            <div>
              <h3 class="text-h6 font-weight-bold">{{ t('user.register.title') }}</h3>
              <p class="text-body-2 text-grey-darken-2 mb-0">{{ t('user.register.subtitle') }}</p>
            </div>
          </div>
          <v-btn 
            icon="mdi-close" 
            variant="text" 
            size="small" 
            class="position-absolute" 
            style="top: 16px; right: 16px;"
            @click="closeDialog">
          </v-btn>
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <div class="mb-4 pa-3 bg-primary-lighten-5 rounded text-primary d-flex align-center">
            <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
            <span class="text-body-2">{{ t('user.register.instructions') }}</span>
          </div>

          <v-text-field 
            v-model="registerFormModel.email" 
            :label="t('user.register.email')" 
            variant="outlined" 
            class="mb-4"
            prepend-inner-icon="mdi-email-outline" 
            hint="请输入常用邮箱，用于接收验证码" 
            persistent-hint 
            maxlength="30"
            rounded="lg"
            density="comfortable"
            clearable>
          </v-text-field>

          <v-text-field 
            v-model="registerFormModel.name" 
            :label="t('user.register.username')" 
            variant="outlined" 
            class="mb-4"
            prepend-inner-icon="mdi-account-outline" 
            hint="用户名长度2-20个字符" 
            persistent-hint
            maxlength="20" 
            rounded="lg"
            density="comfortable"
            clearable>
          </v-text-field>

          <v-text-field 
            v-model="registerFormModel.password" 
            :type="showPassword ? 'text' : 'password'" 
            :label="t('user.register.password')" 
            variant="outlined" 
            class="mb-4"
            prepend-inner-icon="mdi-lock-outline"
            :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'" 
            hint="密码需包含大小写字母和特殊字符" 
            maxlength="20" 
            persistent-hint
            rounded="lg"
            density="comfortable"
            @click:append-inner="handleTogglePassword(false)"
            clearable>
          </v-text-field>

          <v-text-field 
            v-model="registerFormModel.passwordRepeat" 
            :type="showPasswordRepeat ? 'text' : 'password'"
            :label="t('user.register.confirmPassword')" 
            variant="outlined" 
            class="mb-6"
            prepend-inner-icon="mdi-lock-check-outline"
            :append-inner-icon="showPasswordRepeat ? 'mdi-eye' : 'mdi-eye-off'" 
            maxlength="20"
            rounded="lg"
            density="comfortable"
            @click:append-inner="handleTogglePassword(true)"
            clearable>
          </v-text-field>

          <v-btn 
            block 
            size="large" 
            @click="handleSubmit" 
            color="primary"
            rounded="lg" 
            class="font-weight-bold">
            <v-icon icon="mdi-rocket-launch" class="mr-2"></v-icon>
            创建账户
          </v-btn>
        </v-card-text>
      </v-card>
    </template>
  </v-dialog>
</template>