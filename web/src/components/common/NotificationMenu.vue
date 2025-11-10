<script setup lang="ts">
import { ref } from 'vue'

// TODO: 定义通知接口，后续从 API 获取真实数据
interface Notification {
  id: number
  title: string
  content: string
  time: string
  read: boolean
  icon: string
  iconColor: string
}

// 通知列表引用
const notificationList = ref<HTMLElement | null>(null)

// 下拉刷新状态
const isPulling = ref(false)
const isRefreshing = ref(false)
const startY = ref(0)

// 底部提示状态
const showBottomTip = ref(false)

// TODO: 替换为真实 API 调用，从后端获取通知数据
// 通知数据（当前为模拟数据）
const notifications = ref<Notification[]>([
  {
    id: 1,
    title: '新课程发布',
    content: 'Vue 3 高级实战课程已上线，快来学习吧！',
    time: '5分钟前',
    read: false,
    icon: 'mdi-book-open-variant',
    iconColor: 'primary',
  },
  {
    id: 2,
    title: '学习提醒',
    content: '您有一门课程即将到期，请尽快完成学习。',
    time: '1小时前',
    read: false,
    icon: 'mdi-clock-alert-outline',
    iconColor: 'warning',
  },
  {
    id: 3,
    title: '系统通知',
    content: '系统将于今晚22:00进行维护，预计1小时。',
    time: '3小时前',
    read: true,
    icon: 'mdi-information-outline',
    iconColor: 'info',
  },
  {
    id: 4,
    title: '评论回复',
    content: '有人回复了你的评论："很有帮助，谢谢分享！"',
    time: '昨天',
    read: true,
    icon: 'mdi-message-reply-text-outline',
    iconColor: 'success',
  },
])

// 未读通知数量
const unreadCount = ref(notifications.value.filter((n) => !n.read).length)

// 滚动处理
const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement
  const scrollTop = target.scrollTop
  const scrollHeight = target.scrollHeight
  const clientHeight = target.clientHeight

  // 检测是否滚动到顶部 (下拉刷新)
  if (scrollTop === 0 && !isRefreshing.value) {
    isPulling.value = true
  } else {
    isPulling.value = false
  }

  // 检测是否滚动到底部
  if (scrollTop + clientHeight >= scrollHeight - 5) {
    showBottomTip.value = true
  } else {
    showBottomTip.value = false
  }
}

// TODO: 实现下拉刷新功能，调用 API 重新获取通知数据
// 触发刷新
const triggerRefresh = () => {
  if (isRefreshing.value) return

  isRefreshing.value = true

  // 模拟刷新延迟
  setTimeout(() => {
    // TODO: 这里需要添加实际的刷新逻辑，调用 API 重新获取通知数据
    isRefreshing.value = false
    isPulling.value = false
  }, 1500)
}

// 监听下拉动作
const handleTouchStart = (event: TouchEvent) => {
  const clientY = event.touches[0]?.clientY
  if (clientY !== undefined) {
    startY.value = clientY
  }
}

const handleTouchMove = (event: TouchEvent) => {
  if (!notificationList.value) return

  const currentY = event.touches[0]?.clientY
  if (currentY === undefined) return

  const scrollTop = notificationList.value.scrollTop

  // 只有在顶部且向下滑动时才触发
  if (scrollTop === 0 && currentY > startY.value) {
    const distance = currentY - startY.value
    if (distance > 50 && !isRefreshing.value) {
      triggerRefresh()
    }
  }
}

defineExpose({
  unreadCount,
})
</script>

<template>
  <v-menu :close-on-content-click="false" location="bottom end" offset="8">
    <template #activator="{ props }">
      <button class="icon-btn" title="消息通知" v-bind="props">
        <v-badge
          v-if="unreadCount > 0"
          :content="unreadCount"
          color="error"
          offset-x="-2"
          offset-y="2"
        >
          <v-icon size="22">mdi-bell-outline</v-icon>
        </v-badge>
        <v-icon v-else size="22">mdi-bell-outline</v-icon>
      </button>
    </template>

    <v-card rounded="lg" class="notification-menu" width="360">
      <!-- 标题栏 -->
      <v-card-title class="notification-header">
        <span class="notification-title">消息通知</span>
        <v-chip size="x-small" color="error" variant="flat">{{ unreadCount }}</v-chip>
      </v-card-title>

      <v-divider></v-divider>

      <div
        ref="notificationList"
        class="notification-list"
        @scroll="handleScroll"
        @touchstart="handleTouchStart"
        @touchmove="handleTouchMove"
      >
        <!-- 下拉刷新提示 -->
        <div v-if="isPulling" class="pull-to-refresh">
          <v-progress-circular
            v-if="isRefreshing"
            indeterminate
            size="20"
            width="2"
            color="primary"
          ></v-progress-circular>
          <v-icon v-else size="20" color="primary">mdi-refresh</v-icon>
          <span class="ml-2">{{ isRefreshing ? '正在刷新...' : '下拉刷新' }}</span>
        </div>

        <div
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-item"
          :class="{ unread: !notification.read }"
        >
          <div class="d-flex align-start">
            <v-avatar size="36" class="mr-3 flex-shrink-0" :color="notification.iconColor">
              <v-icon size="18" color="white">{{ notification.icon }}</v-icon>
            </v-avatar>
            <div class="flex-grow-1">
              <div class="notification-title">{{ notification.title }}</div>
              <div class="notification-content">{{ notification.content }}</div>
              <div class="notification-time">{{ notification.time }}</div>
            </div>
            <div v-if="!notification.read" class="unread-dot"></div>
          </div>
        </div>

        <!-- 底部提示 -->
        <div v-if="showBottomTip" class="bottom-tip">
          <v-divider class="mb-2"></v-divider>
          <div class="text-center py-3">
            <v-icon size="16" color="grey">mdi-check-circle-outline</v-icon>
            <span class="text-caption text-grey ml-1">已到底部</span>
          </div>
        </div>
      </div>
    </v-card>
  </v-menu>
</template>

<style scoped>
.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: transparent;
  border: none;
  cursor: pointer;
  transition: background-color 0.2s;
}

.icon-btn:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.notification-menu {
  border: 1px solid #e5e5e5 !important;
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px !important;
  font-size: 15px;
  font-weight: 600;
}

.notification-title {
  font-size: 15px;
  font-weight: 600;
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
}

.pull-to-refresh {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  font-size: 13px;
  color: #666;
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: #fafafa;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item.unread {
  background-color: #f8f9ff;
}

.notification-item .notification-title {
  font-size: 14px;
  font-weight: 600;
  color: #000;
  margin-bottom: 4px;
}

.notification-item .notification-content {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
  line-height: 1.4;
}

.notification-item .notification-time {
  font-size: 12px;
  color: #999;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ff5252;
  flex-shrink: 0;
  margin-top: 4px;
}

.bottom-tip {
  padding: 0 16px;
}
</style>
