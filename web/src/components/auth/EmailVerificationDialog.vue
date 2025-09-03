<script setup>
  import { computed } from 'vue'

  // Props
  const props = defineProps({
    modelValue: {
      type: Boolean,
      default: false,
    },
    validateCode: {
      type: String,
      default: '',
    },
  })

  // Emits
  const emit = defineEmits(['update:modelValue', 'update:validateCode', 'submit', 'resend'])

  // Computed properties for v-model
  const dialogModel = computed({
    get: () => props.modelValue,
    set: (value) => emit('update:modelValue', value),
  })

  const validateCodeModel = computed({
    get: () => props.validateCode,
    set: (value) => emit('update:validateCode', value),
  })

  // 处理表单提交
  const handleSubmit = () => {
    emit('submit')
  }

  // 处理重新发送验证码
  const handleResend = () => {
    emit('resend')
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
            <v-avatar color="success" size="40" class="mr-3">
              <v-icon icon="mdi-email-check" color="white" size="20"></v-icon>
            </v-avatar>
            <div>
              <h3 class="text-h6 font-weight-bold">邮箱验证</h3>
              <p class="text-body-2 text-grey-darken-2 mb-0">请检查您的邮箱</p>
            </div>
          </div>
          <v-btn
            icon="mdi-close"
            variant="text"
            size="small"
            class="position-absolute close-btn"
            @click="closeDialog"
          >
          </v-btn>
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <div class="mb-4 pa-3 bg-warning-lighten-5 rounded text-warning d-flex align-center">
            <v-icon icon="mdi-email-outline" start size="16" class="mr-2"></v-icon>
            <span class="text-body-2">我们向您的邮箱发送了验证码，请查收并输入。</span>
          </div>

          <v-form @submit.prevent="handleSubmit">
            <v-text-field
              :model-value="validateCode"
              label="验证码"
              variant="outlined"
              class="mb-4"
              prepend-inner-icon="mdi-key"
              rounded="lg"
              density="comfortable"
              hint="请输入6位数字验证码"
              persistent-hint
              clearable
              @update:model-value="validateCodeModel = $event"
            >
            </v-text-field>

            <v-btn
              block
              size="large"
              color="success"
              rounded="lg"
              class="font-weight-bold mb-3"
              @click="handleSubmit"
            >
              <v-icon icon="mdi-check-circle" class="mr-2"></v-icon>
              验证邮箱
            </v-btn>
          </v-form>

          <div class="text-center">
            <v-btn variant="text" color="primary" class="text-body-2" @click="handleResend">
              重新发送验证码
            </v-btn>
          </div>
        </v-card-text>
      </v-card>
    </template>
  </v-dialog>
</template>

<style scoped>
  .close-btn {
    top: 16px;
    right: 16px;
  }
</style>
