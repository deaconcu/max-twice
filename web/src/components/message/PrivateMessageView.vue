<script setup>
import { ref } from 'vue';

// Props
const props = defineProps({
  users: {
    type: Array,
    default: () => []
  }
});

// Emits
const emit = defineEmits(['sendMessage', 'selectUser']);

// 响应式数据
const messageInput = ref('');

// 处理用户点击
const handleUserClick = (user) => {
  emit('selectUser', user);
};

// 处理发送消息
const handleSendMessage = () => {
  if (messageInput.value.trim()) {
    emit('sendMessage', messageInput.value);
    messageInput.value = '';
  }
};
</script>

<template>
  <v-slide-y-reverse-transition hide-on-leave>
    <div class="text-body-1 d-flex justify-start">
      <v-row class="" style="max-width: 1050px;">
        <!-- 用户列表 -->
        <v-col cols="auto" class="pe-7 mt-0 pt-0 text-end">
          <v-list>
            <v-list-item 
              v-for="user, index in users" 
              :key="index"
              class="py-0 ps-1 pe-4 my-3 bg-grey-lighten-5 rounded-lg"
              active-color="teal" 
              :active="index == 0 ? true : false" 
              :ripple="false" 
              density="comfortable"
              @click="handleUserClick(user)">
              <template v-slot:prepend>
                <v-list-item-media>
                  <v-img :src="user.prependAvatar" width="35" height="35" class="rounded-lg me-3"></v-img>
                </v-list-item-media>
              </template>
              <template v-slot:default>
                <span class="font-weight-bold text-body-2">{{ user.title }}</span>
              </template>
            </v-list-item>
          </v-list>
        </v-col>
        
        <!-- 聊天内容区域 -->
        <v-col cols="" class="mt-0">
          <div class="border-s px-0 mx-0 pt-5">
            <div class="px-8">
              <!-- 聊天消息列表 -->
              <div v-for="i in 12" :key="i">
                <!-- 对方消息 -->
                <div class="pb-5">
                  <v-avatar class="me-5">
                    <v-img alt="John" src="https://cdn.vuetifyjs.com/images/john.jpg"></v-img>
                  </v-avatar>
                  <div class="border px-4 py-3 rounded-xl d-inline-block">
                    你今天怎么样
                  </div>
                  <span class="ms-5 text-center text-caption text-grey-lighten-1">5小时前</span>
                </div>
                
                <!-- 自己的消息 -->
                <div class="pb-5 text-end">
                  <span class="me-5 text-center text-caption text-grey-lighten-1">5小时前</span>
                  <div class="border px-4 py-3 rounded-xl d-inline-block">
                    今天过得很好
                  </div>
                  <v-avatar class="ms-5">
                    <v-img alt="John" src="https://cdn.vuetifyjs.com/images/lists/2.jpg"></v-img>
                  </v-avatar>
                </div>
              </div>
            </div>
            
            <!-- 消息输入框 -->
            <v-text-field 
              v-model="messageInput"
              variant="underlined" 
              append-inner-icon="mdi-email-fast-outline"
              @click:append-inner="handleSendMessage" 
              @keyup.enter="handleSendMessage"
              single-line 
              label="说点什么"
              class="rounded-0 position-sticky bottom-0 bg-white mb-3 mx-8 pt-5 mb-10 text-h5">
            </v-text-field>
          </div>
        </v-col>
      </v-row>
    </div>
  </v-slide-y-reverse-transition>
</template>

<style scoped>
/* 用户列表样式 */
.v-list-item {
  transition: all 0.2s ease;
}

.v-list-item:hover {
  transform: translateX(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 聊天气泡样式 */
.rounded-xl {
  border-radius: 16px;
  background-color: #f5f5f5;
  max-width: 70%;
}

/* 边框样式 */
.border-s {
  border-left: 1px solid rgba(0, 0, 0, 0.08);
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.12);
}

/* 消息时间样式 */
.text-caption {
  font-size: 0.75rem;
}

/* 输入框位置固定 */
.position-sticky {
  position: sticky !important;
}

/* 头像样式 */
.v-avatar {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 聊天区域滚动 */
.px-8 {
  max-height: 500px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.1) transparent;
}

.px-8::-webkit-scrollbar {
  width: 4px;
}

.px-8::-webkit-scrollbar-track {
  background: transparent;
}

.px-8::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}
</style>