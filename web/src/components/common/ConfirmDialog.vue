<template>
  <v-dialog v-model="isOpen" max-width="480">
    <v-card rounded="xl" elevation="0" border>
      <v-card-text class="pa-8">
        <div class="d-flex align-start mb-6">
          <v-avatar :color="iconColor" size="48" rounded="lg" class="mr-4">
            <v-icon :icon="icon" size="24" :color="iconForeground" />
          </v-avatar>
          <div class="flex-grow-1">
            <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">{{ title }}</h3>
            <p class="text-body-2 text-grey mb-0">
              {{ message }}
            </p>
          </div>
        </div>

        <div class="d-flex align-center" style="gap: 12px">
          <v-btn variant="outlined" rounded="lg" size="large" class="flex-grow-1" @click="handleCancel">
            {{ cancelText }}
          </v-btn>
          <v-btn
            :color="confirmColor"
            variant="flat"
            rounded="lg"
            size="large"
            class="flex-grow-1"
            @click="handleConfirm"
          >
            {{ confirmText }}
          </v-btn>
        </div>
      </v-card-text>
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
