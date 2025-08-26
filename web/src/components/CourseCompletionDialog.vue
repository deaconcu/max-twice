<script setup>
import { defineProps, defineEmits } from 'vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  courseName: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['update:modelValue']);

const closeDialog = () => {
  emit('update:modelValue', false);
};
</script>

<template>
  <!-- 恭喜完成课程弹窗 -->
  <v-dialog :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" width="600" persistent>
    <v-card rounded="xl" elevation="12" class="congratulations-card">
      <!-- 头部装饰 -->
      <div class="celebration-header">
        <div class="confetti-bg"></div>
        <div class="celebration-content pa-8 text-center">
          <div class="celebration-icon-container mb-4">
            <v-icon icon="mdi-trophy" size="80" color="yellow-lighten-3" class="celebration-icon"></v-icon>
          </div>
          <h2 class="text-h4 font-weight-bold text-white mb-2">🎉 恭喜完成课程！</h2>
          <p class="text-h6 text-white mb-4">
            您已经完成了课程《{{ courseName }}》的所有学习内容
          </p>
        </div>
      </div>
      
      <!-- 内容区域 -->
      <v-card-text class="pa-8">
        <div class="achievement-stats">
          <div class="text-start mb-6">
            <p class="text-body-1 text-grey-darken-2 mb-4">
              通过坚持不懈的努力，您已经掌握了本课程的所有知识点。这是一个值得庆祝的重要里程碑！
            </p>
            
            <!-- 成就徽章 -->
             <!--
            <div class="achievement-badges d-flex justify-center gap-3 mb-4">
              <v-chip 
                color="success" 
                variant="flat" 
                prepend-icon="mdi-check-all"
                rounded="xl"
                size="large"
                class="px-4 me-8"
              >
                <span class="font-weight-bold">课程完成</span>
              </v-chip>
              <v-chip 
                color="blue" 
                variant="flat" 
                prepend-icon="mdi-brain"
                rounded="xl"
                size="large"
                class="px-4"
              >
                <span class="font-weight-bold">知识掌握</span>
              </v-chip>
            </div>-->
          </div>
        </div>
      </v-card-text>
      
      <!-- 底部操作 -->
      <v-card-actions class="pa-6 pt-0">
        <v-spacer></v-spacer>
        <v-btn
          color="primary"
          variant="flat"
          rounded="lg"
          size="large"
          @click="closeDialog"
          prepend-icon="mdi-check"
          class="px-8"
        >
          <span class="font-weight-bold">继续学习</span>
        </v-btn>
        <v-spacer></v-spacer>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
/* 恭喜完成课程弹窗样式 */
.congratulations-card {
  overflow: hidden;
}

.celebration-header {
  position: relative;
  background: linear-gradient(135deg, #e65100 0%, #ff9800 50%, #ffc107 100%);
  overflow: hidden;
}

.confetti-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 20% 30%, rgba(255, 255, 255, 0.3) 2px, transparent 2px),
    radial-gradient(circle at 80% 70%, rgba(255, 255, 255, 0.4) 1px, transparent 1px),
    radial-gradient(circle at 40% 80%, rgba(255, 255, 255, 0.2) 3px, transparent 3px),
    radial-gradient(circle at 90% 20%, rgba(255, 255, 255, 0.3) 2px, transparent 2px);
  animation: sparkle 3s ease-in-out infinite;
}

.celebration-content {
  position: relative;
  z-index: 1;
}

.celebration-icon-container {
  position: relative;
}

.celebration-icon {
  animation: bounce 2s ease-in-out infinite;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.achievement-badges {
  margin: 16px 0;
}

.achievement-badges .v-chip {
  transition: transform 0.2s ease;
}

.achievement-badges .v-chip:hover {
  transform: scale(1.05);
}

@keyframes sparkle {
  0%, 100% {
    opacity: 0.7;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}
</style>