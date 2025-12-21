<template>
  <v-dialog v-model="visible" fullscreen @click:outside="close">
    <div class="image-viewer-container">
      <v-btn
        icon="mdi-close"
        color="white"
        variant="text"
        class="close-btn"
        @click="close"
      ></v-btn>
      <div class="image-content" @click="close">
        <img :src="imageSrc" alt="预览图片" class="preview-image" />
      </div>
    </div>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: boolean
  src: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = ref(props.modelValue)
const imageSrc = ref(props.src)

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
</style>
