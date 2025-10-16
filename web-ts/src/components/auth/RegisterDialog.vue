<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { emailRules, passwordRules } from '@/utils/validationRules'
import { USER_VALIDATION } from '@/types/validation'

// 类型定义
interface RegisterForm {
  email: string
  password: string
  passwordRepeat: string
  validateCode: string
}

interface Props {
  modelValue: boolean
  registerForm: RegisterForm
  showPassword: boolean
  showPasswordRepeat: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'update:registerForm', value: RegisterForm): void
  (e: 'update:showPassword', value: boolean): void
  (e: 'update:showPasswordRepeat', value: boolean): void
  (e: 'submit'): void
  (e: 'togglePassword', repeat: boolean): void
}

const { t } = useI18n()

// Props
const props = defineProps<Props>()

// Emits
const emit = defineEmits<Emits>()

// 确认密码验证规则
const passwordRepeatRules = [
  (v: string) => !!v || '请再次输入密码',
  (v: string) => v === props.registerForm.password || '两次输入的密码不一致'
]

// Computed properties for v-model
const dialogModel = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const registerFormModel = computed({
  get: () => props.registerForm,
  set: (value: RegisterForm) => emit('update:registerForm', value),
})

// 处理密码可见性切换
const handleTogglePassword = (repeat: boolean = false): void => {
  emit('togglePassword', repeat)
}

// 处理表单提交
const handleSubmit = (): void => {
  emit('submit')
}

// 关闭对话框
const closeDialog = (): void => {
  dialogModel.value = false
}
</script>

<template>
  <v-dialog
    max-width="600"
    :model-value="modelValue"
    persistent
    @update:model-value="dialogModel = $event"
  >
    <template #default>
      <v-card rounded="xl" elevation="0">
        <v-card-title class="pa-6 pb-4 position-relative">
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
            class="close-btn"
            @click="closeDialog"
          >
          </v-btn>
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <div class="mb-4 pa-3 bg-primary-lighten-5 rounded text-primary d-flex align-center">
            <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
            <span class="text-body-2">{{ t('user.register.instructions') }}</span>
          </div>

          <v-text-field
            :model-value="registerForm.email"
            :label="t('user.register.email')"
            :rules="emailRules"
            variant="outlined"
            class="mb-4"
            prepend-inner-icon="mdi-email-outline"
            hint="请输入常用邮箱，用于接收验证码"
            persistent-hint
            :counter="USER_VALIDATION.EMAIL_MAX_LENGTH"
            rounded="lg"
            density="comfortable"
            clearable
            @update:model-value="registerFormModel.email = $event"
          >
          </v-text-field>

          <v-text-field
            :model-value="registerForm.password"
            :type="showPassword ? 'text' : 'password'"
            :label="t('user.register.password')"
            :rules="passwordRules"
            variant="outlined"
            class="mb-4"
            prepend-inner-icon="mdi-lock-outline"
            :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
            :hint="`密码长度 ${USER_VALIDATION.PASSWORD_MIN_LENGTH}-${USER_VALIDATION.PASSWORD_MAX_LENGTH} 个字符`"
            :counter="USER_VALIDATION.PASSWORD_MAX_LENGTH"
            persistent-hint
            rounded="lg"
            density="comfortable"
            clearable
            @update:model-value="registerFormModel.password = $event"
            @click:append-inner="handleTogglePassword(false)"
          >
          </v-text-field>

          <v-text-field
            :model-value="registerForm.passwordRepeat"
            :type="showPasswordRepeat ? 'text' : 'password'"
            :label="t('user.register.confirmPassword')"
            :rules="passwordRepeatRules"
            variant="outlined"
            class="mb-6"
            prepend-inner-icon="mdi-lock-check-outline"
            :append-inner-icon="showPasswordRepeat ? 'mdi-eye' : 'mdi-eye-off'"
            :counter="USER_VALIDATION.PASSWORD_MAX_LENGTH"
            rounded="lg"
            density="comfortable"
            clearable
            @update:model-value="registerFormModel.passwordRepeat = $event"
            @click:append-inner="handleTogglePassword(true)"
          >
          </v-text-field>

          <v-btn
            block
            size="large"
            color="primary"
            rounded="lg"
            class="font-weight-bold"
            @click="handleSubmit"
          >
            <v-icon icon="mdi-rocket-launch" class="mr-2"></v-icon>
            创建账户
          </v-btn>
        </v-card-text>
      </v-card>
    </template>
  </v-dialog>
</template>

<style scoped>
.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
}
</style>