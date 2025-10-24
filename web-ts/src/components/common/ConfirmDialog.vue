<script setup lang="ts">
import { ref, watch } from 'vue'

interface Props {
  modelValue: boolean
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  confirmColor?: string
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '确认操作',
  confirmText: '确认',
  cancelText: '取消',
  confirmColor: 'error',
  loading: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
  cancel: []
}>()

const dialog = ref(props.modelValue)

watch(
  () => props.modelValue,
  (newValue) => {
    dialog.value = newValue
  }
)

watch(dialog, (newValue) => {
  emit('update:modelValue', newValue)
})

const handleConfirm = () => {
  emit('confirm')
}

const handleCancel = () => {
  dialog.value = false
  emit('cancel')
}
</script>

<template>
  <v-dialog v-model="dialog" max-width="480" persistent>
    <v-card class="rounded-xl" elevation="8">
      <!-- 标题栏 -->
      <v-card-title class="d-flex align-center px-5 py-3">
        <v-icon
          :icon="confirmColor === 'error' ? 'mdi-alert-circle-outline' : 'mdi-help-circle-outline'"
          :color="confirmColor"
          size="24"
          class="mr-3"
        ></v-icon>
        <span class="text-h6 font-weight-bold text-grey-darken-3">{{ title }}</span>
      </v-card-title>

      <v-divider></v-divider>

      <!-- 内容区域 -->
      <v-card-text class="pa-6">
        <p class="text-body-1 text-grey-darken-2 mb-0" style="line-height: 1.6">
          {{ message }}
        </p>
      </v-card-text>

      <!-- 操作按钮 -->
      <v-card-actions class="pa-4">
        <v-spacer></v-spacer>
        <v-btn
          variant="text"
          size="large"
          rounded="lg"
          class="px-6"
          :disabled="loading"
          @click="handleCancel"
        >
          {{ cancelText }}
        </v-btn>
        <v-btn
          :color="confirmColor"
          variant="flat"
          size="large"
          rounded="lg"
          class="px-6"
          :loading="loading"
          @click="handleConfirm"
        >
          {{ confirmText }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.v-card {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12) !important;
}
</style>
