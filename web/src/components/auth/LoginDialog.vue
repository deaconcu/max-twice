<script setup>
  import { computed } from 'vue'

  // Props
  const props = defineProps({
    modelValue: {
      type: Boolean,
      default: false,
    },
    loginForm: {
      type: Object,
      required: true,
    },
    showPassword: {
      type: Boolean,
      default: false,
    },
  })

  // Emits
  const emit = defineEmits(['update:modelValue', 'update:loginForm', 'submit', 'togglePassword'])

  // Computed properties for v-model
  const dialogModel = computed({
    get: () => props.modelValue,
    set: (value) => emit('update:modelValue', value),
  })

  const loginFormModel = computed({
    get: () => props.loginForm,
    set: (value) => emit('update:loginForm', value),
  })

  // 处理密码可见性切换
  const handleTogglePassword = () => {
    emit('togglePassword')
  }

  // 处理表单提交
  const handleSubmit = () => {
    emit('submit')
  }

  // 关闭对话框
  const closeDialog = () => {
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
        <v-card-title class="pa-6 pb-4">
          <div class="d-flex align-center">
            <v-avatar color="primary" size="40" class="mr-3">
              <v-icon icon="mdi-emoticon-wink" color="white" size="20"></v-icon>
            </v-avatar>
            <div>
              <h3 class="text-h6 font-weight-bold">欢迎回来</h3>
              <p class="text-body-2 text-grey-darken-2 mb-0">登录您的学习账户</p>
            </div>
          </div>
          <v-btn
            icon="mdi-close"
            variant="text"
            size="small"
            class="position-absolute close-button"
            @click="closeDialog"
          >
          </v-btn>
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <div class="mb-4 pa-3 bg-blue-lighten-5 rounded text-info d-flex align-center">
            <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
            <span class="text-body-2">使用邮箱和密码登录您的账户</span>
          </div>

          <v-text-field
            :model-value="loginForm.email"
            label="邮箱地址"
            variant="outlined"
            class="mb-4"
            prepend-inner-icon="mdi-email-outline"
            rounded="lg"
            density="comfortable"
            clearable
            @update:model-value="loginFormModel.email = $event"
          >
          </v-text-field>

          <v-text-field
            :model-value="loginForm.password"
            :type="showPassword ? 'text' : 'password'"
            label="密码"
            variant="outlined"
            class="mb-6"
            prepend-inner-icon="mdi-lock-outline"
            :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
            rounded="lg"
            density="comfortable"
            clearable
            @update:model-value="loginFormModel.password = $event"
            @click:append-inner="handleTogglePassword"
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
            <v-icon icon="mdi-login" class="mr-2"></v-icon>
            登录
          </v-btn>
        </v-card-text>
      </v-card>
    </template>
  </v-dialog>
</template>

<style scoped>
  .close-button {
    top: 16px;
    right: 16px;
  }
</style>
