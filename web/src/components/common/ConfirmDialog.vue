<template>
  <v-dialog v-model="isOpen" max-width="420">
    <v-card rounded="lg">
      <v-card-text class="pa-6">
        <div class="text-center mb-4">
          <v-avatar :color="iconColor" size="56">
            <v-icon :icon="icon" size="32" :color="iconForeground" />
          </v-avatar>
        </div>
        <h3 class="text-h6 font-weight-bold text-center mb-2">{{ title }}</h3>
        <p class="text-body-2 text-grey-darken-2 text-center mb-0">
          {{ message }}
        </p>
      </v-card-text>
      <v-card-actions class="px-6 pb-6 pt-0" style="gap: 12px">
        <v-btn variant="outlined" rounded="md" class="flex-grow-1" @click="handleCancel">
          {{ cancelText }}
        </v-btn>
        <v-btn
          :color="confirmColor"
          variant="flat"
          rounded="md"
          class="flex-grow-1"
          @click="handleConfirm"
        >
          {{ confirmText }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  modelValue: boolean
  title?: string
  message?: string
  confirmText?: string
  cancelText?: string
  confirmColor?: string
  icon?: string
  iconColor?: string
  iconForeground?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '确认操作',
  message: '确定要执行此操作吗？',
  confirmText: '确认',
  cancelText: '取消',
  confirmColor: 'error',
  icon: 'mdi-alert-circle-outline',
  iconColor: 'error-lighten-4',
  iconForeground: 'error',
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
  cancel: []
}>()

const isOpen = computed({
  get: () => props.modelValue,
  set: (value) => {
    emit('update:modelValue', value)
  },
})

const handleConfirm = () => {
  emit('confirm')
  isOpen.value = false
}

const handleCancel = () => {
  emit('cancel')
  isOpen.value = false
}
</script>
