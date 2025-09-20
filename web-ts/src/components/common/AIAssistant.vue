<script setup lang="ts">
import { ref } from 'vue'

const isExpanded = ref(true)

const engines = [
  { name: 'ChatGPT', href: 'https://chatgpt.com', color: 'green-darken-2' },
  { name: 'Claude', href: 'https://claude.ai', color: 'indigo-darken-2' },
  { name: 'Gemini', href: 'https://gemini.google.com', color: 'blue-darken-2' },
  { name: 'DeepSeek', href: 'https://chat.deepseek.com', color: 'deep-purple-darken-2' }
]
</script>

<template>
  <div class="ai-assistant-container">
    <div class="ai-header pa-2 bg-grey-lighten-5">
      <div class="d-flex align-center justify-space-between">
        <div class="d-flex align-center">
          <v-icon icon="mdi-robot-excited" color="primary" size="20" class="mr-2"></v-icon>
          <h3 class="text-body-1 font-weight-bold text-grey-darken-2">答疑助手</h3>
        </div>
        <v-btn
          :icon="isExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down'"
          variant="text"
          color="grey-darken-1"
          size="x-small"
          @click="isExpanded = !isExpanded"
        ></v-btn>
      </div>
    </div>

    <v-expand-transition>
      <div v-show="isExpanded" class="pa-3">
        <div class="text-body-2 text-grey-darken-2 mb-3">
          <div class="font-weight-bold w-100">用法：</div>
          <div class="mt-2">1）在文章中选中您不太理解的内容</div>
          <div>2）在左侧竖线处拖动手柄，调整上下文范围</div>
          <div>3）点击面板中的“复制”，将引用和问题复制到剪贴板</div>
          <div>4）用复制的内容询问你常用的 AI 引擎</div>
        </div>

        <div class="d-flex flex-wrap mt-8" style="gap: 8px;">
          <div class="text-body-2 w-100 text-grey-darken-2 font-weight-bold">常用 AI 引擎：</div>
          <v-btn
            v-for="e in engines"
            :key="e.name"
            :href="e.href"
            target="_blank"
            rel="noopener"
            :color="e.color"
            variant="flat"
            rounded="lg"
            size="small"
            class="engine-link"
          >
            <v-icon icon="mdi-web" size="14" class="mr-2"></v-icon>
            <span class="font-weight-medium text-white">{{ e.name }}</span>
          </v-btn>
        </div>
      </div>
    </v-expand-transition>
  </div>
</template>

<style scoped>
.ai-assistant-container {
  border: 0px solid #e0e0e0;
  border-radius: 12px;
  overflow: hidden;
  background: #fafafa;
}

.ai-header {
  border-bottom: 1px solid #eee;
}

.chat-messages {
  background: #fafafa;
}

.message-wrapper {
  max-width: 100%;
}

.message-bubble {
  max-width: 85%;
  padding: 12px 16px;
  border-radius: 16px;
  word-wrap: break-word;
}

.user-bubble {
  background: #1976d2;
  color: white;
}

.ai-bubble {
  background: white;
  color: #333;
  border: 1px solid #e0e0e0;
}

.input-area {
  border-top: 0px solid #e0e0e0;
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.3);
}
.engine-link {
  text-decoration: none !important;
}
.engine-link:hover,
.engine-link:focus,
.engine-link:active {
  text-decoration: none !important;
}
</style>