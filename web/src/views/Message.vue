<script setup>
import { ref, onMounted, inject, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { messageServiceV1 } from '@/services/api/v1/apiServiceV1';
import { useUserStore } from "@/stores/user";
import RightSidebar from '@/components/common/RightSidebar.vue';
import MessageSidebar from '@/components/message/MessageSidebar.vue';
import SystemMessageView from '@/components/message/SystemMessageView.vue';
import CourseApplicationView from '@/components/message/CourseApplicationView.vue';
import PrivateMessageView from '@/components/message/PrivateMessageView.vue';

const { t } = useI18n();
const user = useUserStore();
const route = useRoute();
const router = useRouter();
const showSnackbar = inject('showSnackbar');

// 响应式数据
const selected = ref("system");
const messageList = ref([]);
const systemMessageType = ref(99);
const lastId = ref(0x7fffffff);
const lastReadTime = ref(null);

// 统计数据
const unreadCount = ref(5);
const totalMessages = ref(142);

// 私信用户列表
const users = ref([
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/1.jpg',
    title: 'John Leider'
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/2.jpg',
    title: 'Summer BBQ',
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/3.jpg',
    title: 'Oui oui',
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/4.jpg',
    title: 'Birthday gift',
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/5.jpg',
    title: 'Recipe to try',
  },
  {
    prependAvatar: 'https://cdn.vuetifyjs.com/images/lists/2.jpg',
    title: '数据结构与算法',
  }
]);

// 监听选中消息类型变化
watch(selected, (newValue, oldValue) => {
  console.log("selected changed");
  messageList.value = [];
  lastId.value = 0x7fffffff;
});

// 数据加载函数
async function loadData({ done }) {
  try {
    let response = '';
    response = await messageServiceV1.getSystemMessages(systemMessageType.value, lastId.value); 

    if (response.code === 401) {
      console.log('not login');
      done('error');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      const appendList = response.data;

      if (lastReadTime.value == null) {
        lastReadTime.value = new Date(response.data.lastReadTime);
      }
      messageList.value.push(...appendList);

      if (appendList.length > 0) {
        lastId.value = appendList[appendList.length - 1].id;
        done('ok')
      } else {
        done('empty')
      }
    } else {
      console.error('Unexpected response code:', response.code);
      done('error');
    }
  } catch (error) {
    console.error('Error get message:', error);
    done('error');
  }
};

// 处理系统消息类型更新
const handleSystemMessageTypeUpdate = (newType) => {
  messageList.value = [];
  lastId.value = 0x7fffffff;
  systemMessageType.value = newType;
};

// 处理发送私信
const handleSendMessage = (message) => {
  console.log('发送消息:', message);
  // TODO: 实现发送消息逻辑
};

// 处理选择私信用户
const handleSelectUser = (user) => {
  console.log('选择用户:', user);
  // TODO: 实现选择用户逻辑
};

onMounted(() => {
  // 初始化数据加载可以在这里进行
});
</script>

<template>
  <v-container class="ma-0" fluid>
    <v-row>
      <!-- 左侧导航栏 -->
      <MessageSidebar 
        v-model:selected-message-type="selected"
        :unread-count="unreadCount"
        :total-messages="totalMessages"
      />

      <!-- 主内容区域 -->
      <v-col class="py-9">
        <!-- 系统消息视图 -->
        <SystemMessageView
          v-if="selected === 'system'"
          :message-list="messageList"
          :last-read-time="lastReadTime"
          @load-data="loadData"
          @update-system-message-type="handleSystemMessageTypeUpdate"
        />

        <!-- 课程申请消息视图 -->
        <CourseApplicationView
          v-if="selected === 'courseApply'"
          :message-list="messageList"
          @load-data="loadData"
        />

        <!-- 私信消息视图 -->
        <PrivateMessageView
          v-if="selected === 'private'"
          :users="users"
          @send-message="handleSendMessage"
          @select-user="handleSelectUser"
        />
      </v-col>

      <!-- 右侧边栏 -->
      <v-col cols="3" class="ps-8 pt-9">
        <div class="sticky-right" style="position: sticky; top: 90px;">
          <RightSidebar />
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
/* 确保卡片无阴影 - 保持flat设计 */
.v-card {
  box-shadow: none !important;
  border: 0px solid rgba(0, 0, 0, 0.08) !important;
  transition: all 0.2s ease;
}

.v-card:hover {
  border-color: rgba(0, 0, 0, 0.12) !important;
}

/* 按钮样式优化 */
.v-btn {
  text-transform: none !important;
  font-weight: 500 !important;
}

/* 改善字体渲染和清晰度 */
* {
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-rendering: optimizeLegibility;
}

/* 增强文字对比度和清晰度 */
.text-grey-darken-1,
.text-grey-darken-2,
.text-grey-darken-3,
.text-grey-darken-4 {
  font-weight: 500 !important;
}

/* 确保主要文字有足够的对比度 */
h1, h2, h3, h4, h5, h6 {
  font-weight: 700 !important;
  letter-spacing: -0.01em;
}

/* 增加hover效果但保持flat风格 */
.v-btn:hover {
  transform: translateY(-1px);
  transition: transform 0.2s ease;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sticky-right {
    position: relative !important;
    top: unset !important;
    margin-bottom: 20px;
  }
  
  .ps-8 {
    padding-left: 16px !important;
  }
}

/* 无限滚动样式优化 */
.v-infinite-scroll__side {
  display: none !important;
}

.v-infinite-scroll__loading {
  border-top: none !important;
  box-shadow: none !important;
}
</style>