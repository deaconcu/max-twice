<template>
  <v-dialog v-model="visible" fullscreen @click:outside="close">
    <div class="image-viewer-container">
      <v-btn icon="mdi-close" color="white" variant="text" class="close-btn" @click="close"></v-btn>
      <div class="image-content" @click="close">
        <div v-if="isSvg" class="svg-container" v-html="imageSrc"></div>
        <img v-else :src="imageSrc" :alt="t('common.previewImage')" class="preview-image" />
      </div>
    </div>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useI18n } from '@/composables/useI18n'

const props = defineProps<{
  modelValue: boolean
  src: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { t } = useI18n()

const visible = ref(props.modelValue)
const imageSrc = ref(props.src)

// 判断是否是 SVG 内容
const isSvg = computed(() => {
  return typeof imageSrc.value === 'string' && imageSrc.value.trim().startsWith('<svg')
})

watch(
  () => props.modelValue,
  (newVal) => {
    visible.value = newVal
  }
)

watch(
  () => props.src,
  (newVal) => {
    imageSrc.value = newVal
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
.image-viewer-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10000;
}

.image-content {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  cursor: pointer;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  cursor: zoom-out;
}

.svg-container {
  max-width: 100%;
  max-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: zoom-out;
}

.svg-container :deep(svg) {
  max-width: 100%;
  max-height: 100%;
}
</style>
