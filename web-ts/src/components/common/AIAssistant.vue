<script setup lang="ts">
import { ref, nextTick } from 'vue'

interface ChatMessage {
  id: number
  content: string
  isUser: boolean
  timestamp: string
}

const isExpanded = ref(true)
const inputMessage = ref('')
const loading = ref(false)
const chatContainer = ref()

// 模拟对话数据
const messages = ref<ChatMessage[]>([
  {
    id: 1,
    content: '你好！我是AI答疑助手，有什么关于这篇文章的问题可以问我。',
    isUser: false,
    timestamp: new Date().toLocaleTimeString()
  }
])

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return
  
  const userMessage: ChatMessage = {
    id: Date.now(),
    content: inputMessage.value.trim(),
    isUser: true,
    timestamp: new Date().toLocaleTimeString()
  }
  
  messages.value.push(userMessage)
  const question = inputMessage.value
  inputMessage.value = ''
  loading.value = true
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()
  
  // 模拟AI回复
  setTimeout(() => {
    const aiResponse: ChatMessage = {
      id: Date.now() + 1,
      content: generateMockResponse(question),
      isUser: false,
      timestamp: new Date().toLocaleTimeString()
    }
    
    messages.value.push(aiResponse)
    loading.value = false
    
    nextTick(() => {
      scrollToBottom()
    })
  }, 1000)
}

// 生成模拟回复
const generateMockResponse = (question: string): string => {
  const responses = [
    '这是一个很好的问题！根据文章内容，我认为...',
    '让我来解释一下这个概念。从文章中可以看出...',
    '这个问题涉及到文章中提到的几个关键点...',
    '根据我对文章的理解，这里的重点是...',
    '这是文章中的一个重要概念，让我详细说明一下...'
  ]
  
  return responses[Math.floor(Math.random() * responses.length)] + 
    '这只是一个UI演示，实际的AI回复会更加准确和详细。'
}

// 滚动到底部
const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

// 清空对话
const clearMessages = () => {
  messages.value = [
    {
      id: 1,
      content: '你好！我是AI答疑助手，有什么关于这篇文章的问题可以问我。',
      isUser: false,
      timestamp: new Date().toLocaleTimeString()
    }
  ]
}

// 回车发送消息
const handleKeyPress = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}
</script>

<template>
  <div class="ai-assistant-container">
    <!-- 标题栏 -->
    <div class="ai-header pa-2 bg-grey-lighten-5">
      <div class="d-flex align-center justify-space-between">
        <div class="d-flex align-center">
          <v-icon icon="mdi-robot-excited" color="primary" size="20" class="mr-2"></v-icon>
          <h3 class="text-body-1 font-weight-bold text-grey-darken-2">AI答疑助手</h3>
        </div>
        <div class="d-flex align-center">
          <v-btn
            icon="mdi-delete-outline"
            variant="text"
            color="grey-darken-1"
            size="x-small"
            @click="clearMessages"
          ></v-btn>
          <v-btn
            :icon="isExpanded ? 'mdi-chevron-up' : 'mdi-chevron-down'"
            variant="text"
            color="grey-darken-1"
            size="x-small"
            @click="isExpanded = !isExpanded"
          ></v-btn>
        </div>
      </div>
    </div>

    <!-- 对话区域 -->
    <v-expand-transition>
      <div v-show="isExpanded" class="ai-content">
        <!-- 消息列表 -->
        <div
          ref="chatContainer"
          class="chat-messages pa-3"
          style="height: 400px; overflow-y: auto;"
        >
          <div
            v-for="message in messages"
            :key="message.id"
            class="message-wrapper mb-3"
            :class="{ 'user-message': message.isUser, 'ai-message': !message.isUser }"
          >
            <div class="d-flex" :class="message.isUser ? 'justify-end' : 'justify-start'">
              <div class="message-bubble" :class="message.isUser ? 'user-bubble' : 'ai-bubble'">
                <p class="text-body-2 mb-1">{{ message.content }}</p>
                <div class="text-caption text-right" style="opacity: 0.7;">
                  {{ message.timestamp }}
                </div>
              </div>
            </div>
          </div>

          <!-- 加载状态 -->
          <div v-if="loading" class="d-flex justify-start mb-3">
            <div class="message-bubble ai-bubble">
              <div class="d-flex align-center">
                <v-progress-circular
                  indeterminate
                  size="16"
                  width="2"
                  color="primary"
                  class="mr-2"
                ></v-progress-circular>
                <span class="text-body-2">AI正在思考...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area pa-3 bg-grey-lighten-5">
          <div class="d-flex align-center" style="gap: 8px;">
            <v-text-field
              v-model="inputMessage"
              placeholder="输入你的问题..."
              variant="outlined"
              density="compact"
              class="flex-grow-1"
              hide-details
              @keydown="handleKeyPress"
            ></v-text-field>
            <v-btn
              color="primary"
              icon="mdi-send"
              size="x-small"
              variant="flat"
              :disabled="!inputMessage.trim() || loading"
              @click="sendMessage"
            ></v-btn>
          </div>
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
  background: white;
}

.ai-header {
  border-bottom: 0px solid #e0e0e0;
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
</style>