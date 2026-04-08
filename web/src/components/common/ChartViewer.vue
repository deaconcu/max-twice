<template>
  <v-dialog v-model="visible" @click:outside="close">
    <v-card rounded="xl" class="chart-dialog-card">
      <v-card-title class="pa-4 d-flex align-center justify-space-between">
        <span class="text-h6">{{ t('chart.preview') }}</span>
        <v-btn icon="mdi-close" variant="text" size="small" @click="close"></v-btn>
      </v-card-title>
      <v-card-text class="pa-6 pt-2">
        <div class="chart-container" v-html="svgContent"></div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from '@/composables/useI18n'

const { t } = useI18n()

const props = defineProps<{
  modelValue: boolean
  svgContent: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = ref(props.modelValue)

watch(
  () => props.modelValue,
  (newVal) => {
    visible.value = newVal
  }
)

watch(visible, (newVal) => {
  emit('update:modelValue', newVal)
})

const close = () => {
  visible.value = false
}
</script>

<style scoped>
.chart-dialog-card {
  width: auto;
  max-width: calc(100vw - 48px);
}

.chart-container {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  max-height: calc(100vh - 200px);
}

.chart-container :deep(svg) {
  display: block;
}
</style>
