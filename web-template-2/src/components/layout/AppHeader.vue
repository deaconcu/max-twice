<template>
  <v-app-bar :elevation="0" class="app-header" height="56">
    <v-container fluid class="px-4">
      <v-row align="center" no-gutters>
        <!-- Logo -->
        <v-col cols="auto">
          <div class="d-flex align-center" style="cursor: pointer" @click="$router.push('/home')">
            <div class="logo-icon">
              <v-icon size="28" color="primary">mdi-reddit</v-icon>
            </div>
            <h2 class="logo-text ml-2">MaxTwice</h2>
          </div>
        </v-col>

        <v-spacer />

        <!-- Right side actions -->
        <v-col cols="auto">
          <div class="d-flex align-center ga-4">
            <button class="icon-btn" title="搜索">
              <v-icon size="20">mdi-magnify</v-icon>
            </button>

            <!-- 通知菜单 -->
            <v-menu
              :close-on-content-click="false"
              location="bottom end"
              offset="8"
              open-on-hover
              :open-delay="100"
            >
              <template #activator="{ props }">
                <button class="icon-btn notification-btn" title="通知" v-bind="props">
                  <v-icon size="20">mdi-bell-outline</v-icon>
                  <span v-if="unreadCount > 0" class="notification-badge">{{ unreadCount }}</span>
                </button>
              </template>

              <v-card rounded="lg" class="notification-menu" min-width="480" max-width="520">
                <v-card-title class="pa-4 pb-3">
                  <div class="d-flex align-center justify-space-between">
                    <span class="text-h6 font-weight-bold">通知</span>
                    <v-btn
                      variant="text"
                      size="small"
                      color="primary"
                      class="text-caption"
                      @click="markAllAsRead"
                    >
                      全部已读
                    </v-btn>
                  </div>
                </v-card-title>

                <v-divider></v-divider>

                <div
                  ref="notificationList"
                  class="notification-list"
                  @scroll="handleScroll"
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
                    :class="{ 'unread': !notification.read }"
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

            <!-- 用户菜单 -->
            <v-menu
              :close-on-content-click="false"
              location="bottom end"
              offset="8"
              open-on-hover
              :open-delay="100"
            >
              <template #activator="{ props }">
                <button class="icon-btn" title="个人中心" v-bind="props">
                  <v-icon size="20">mdi-account-circle-outline</v-icon>
                </button>
              </template>

              <v-card rounded="lg" class="user-menu" width="220">
                <!-- 用户信息 -->
                <v-card-text class="pa-4">
                  <div class="d-flex flex-column align-center">
                    <v-avatar size="56" color="primary" class="mb-3 user-avatar">
                      <v-icon size="28" color="white">mdi-account</v-icon>
                    </v-avatar>
                    <div class="user-name text-center mb-1">{{ userInfo.name }}</div>
                    <div class="user-role">学习者</div>
                  </div>
                </v-card-text>

                <v-divider></v-divider>

                <!-- 菜单项 -->
                <div class="menu-items py-2">
                  <div class="menu-item" @click="goToSettings">
                    <v-icon icon="mdi-cog-outline" size="18" color="grey-darken-1" class="menu-icon"></v-icon>
                    <span class="menu-title">个人设置</span>
                  </div>
                  <div class="menu-item" @click="handleLogout">
                    <v-icon icon="mdi-logout" size="18" color="error" class="menu-icon"></v-icon>
                    <span class="menu-title text-error">退出登录</span>
                  </div>
                </div>
              </v-card>
            </v-menu>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </v-app-bar>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 通知列表引用
const notificationList = ref<HTMLElement | null>(null)

// 用户信息
const userInfo = ref({
  name: '张三'
})

// 跳转到设置页面
const goToSettings = () => {
  console.log('跳转到个人设置')
  router.push('/settings')
}

// 退出登录
const handleLogout = () => {
  console.log('退出登录')
  // 这里添加实际的登出逻辑
  // 比如清除 token、跳转到登录页等
  router.push('/login')
}

// 下拉刷新状态
const isPulling = ref(false)
const isRefreshing = ref(false)
const startY = ref(0)

// 底部提示状态
const showBottomTip = ref(false)

// 通知数据
const notifications = ref([
  {
    id: 1,
    title: '新课程发布',
    content: 'Vue 3 高级实战课程已上线，快来学习吧！',
    time: '5分钟前',
    read: false,
    icon: 'mdi-book-open-variant',
    iconColor: 'primary'
  },
  {
    id: 2,
    title: '学习提醒',
    content: '您已经3天没有学习了，继续保持学习习惯吧',
    time: '2小时前',
    read: false,
    icon: 'mdi-clock-outline',
    iconColor: 'warning'
  },
  {
    id: 3,
    title: '评论回复',
    content: '用户 @张三 回复了你的评论',
    time: '昨天',
    read: true,
    icon: 'mdi-comment-outline',
    iconColor: 'success'
  },
  {
    id: 4,
    title: '系统通知',
    content: '您的课程进度已达到80%，继续加油！',
    time: '2天前',
    read: true,
    icon: 'mdi-trophy-outline',
    iconColor: 'error'
  },
  {
    id: 5,
    title: '课程更新',
    content: 'JavaScript 进阶课程新增了3个章节',
    time: '3天前',
    read: true,
    icon: 'mdi-update',
    iconColor: 'info'
  },
  {
    id: 6,
    title: '作业提交',
    content: '您的作业已提交成功，等待老师批改',
    time: '4天前',
    read: true,
    icon: 'mdi-file-document-outline',
    iconColor: 'primary'
  },
  {
    id: 7,
    title: '点赞通知',
    content: '您的评论收到了10个赞',
    time: '5天前',
    read: true,
    icon: 'mdi-thumb-up-outline',
    iconColor: 'error'
  },
  {
    id: 8,
    title: '课程推荐',
    content: '根据您的学习记录，为您推荐了新课程',
    time: '6天前',
    read: true,
    icon: 'mdi-lightbulb-outline',
    iconColor: 'warning'
  }
])

// 未读消息数量
const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.read).length
})

// 全部标记为已读
const markAllAsRead = () => {
  notifications.value.forEach(n => {
    n.read = true
  })
}

// 滚动事件处理
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

// 触发刷新
const triggerRefresh = () => {
  if (isRefreshing.value) return

  isRefreshing.value = true

  // 模拟刷新延迟
  setTimeout(() => {
    // 这里可以添加实际的刷新逻辑，比如重新获取通知数据
    console.log('刷新通知列表')

    isRefreshing.value = false
    isPulling.value = false
  }, 1500)
}

// 监听下拉动作
const handleTouchStart = (event: TouchEvent) => {
  startY.value = event.touches[0].clientY
}

const handleTouchMove = (event: TouchEvent) => {
  if (!notificationList.value) return

  const currentY = event.touches[0].clientY
  const scrollTop = notificationList.value.scrollTop

  // 只有在顶部且向下滑动时才触发
  if (scrollTop === 0 && currentY > startY.value) {
    const distance = currentY - startY.value
    if (distance > 50 && !isRefreshing.value) {
      triggerRefresh()
    }
  }
}
</script>

<style scoped>
.app-header {
  background-color: rgb(var(--v-theme-surface)) !important;
}

.logo-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(var(--v-theme-surface-variant));
  border: 2px solid rgb(var(--v-theme-border));
  border-radius: 50%;
}

.logo-text {
  font-size: 1.25rem;
  font-weight: 700;
}

.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #656D76;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
}

.icon-btn:hover {
  background-color: rgb(var(--v-theme-surface-variant));
  color: rgb(var(--v-theme-on-surface));
}

/* 通知按钮 */
.notification-btn {
  position: relative;
}

/* 未读数量徽章 */
.notification-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background-color: #FF4444;
  color: white;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgb(var(--v-theme-surface));
}

/* 通知菜单 */
.notification-menu {
  border: 1px solid #E5E5E5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 用户菜单 */
.user-menu {
  border: 1px solid #E0E0E0;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

/* 用户头像 */
.user-avatar {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 用户信息 */
.user-name {
  font-size: 15px;
  font-weight: 600;
  color: #1A1A1B;
  line-height: 1.3;
}

.user-role {
  font-size: 12px;
  color: #999;
  padding: 2px 10px;
  background-color: #F5F5F5;
  border-radius: 12px;
}

/* 菜单项列表 */
.menu-items {
  padding: 0;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.menu-item:hover {
  background-color: #F8F9FA;
}

.menu-item:active {
  background-color: #F0F0F0;
  transform: scale(0.98);
}

.menu-icon {
  flex-shrink: 0;
  margin-right: 12px;
}

.menu-title {
  flex: 1;
  font-size: 14px;
  color: #1A1A1B;
  font-weight: 500;
}

/* 通知列表 */
.notification-list {
  max-height: 70vh;
  overflow-y: auto;
  position: relative;
}

.notification-list::-webkit-scrollbar {
  width: 4px;
}

.notification-list::-webkit-scrollbar-track {
  background: transparent;
}

.notification-list::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.notification-list::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.2);
}

/* 下拉刷新提示 */
.pull-to-refresh {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  background-color: #F5F5F5;
  border-bottom: 1px solid #E5E5E5;
  color: #666;
  font-size: 13px;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 底部提示 */
.bottom-tip {
  position: sticky;
  bottom: 0;
  background-color: white;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 通知项 */
.notification-item {
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  position: relative;
}

.notification-item:hover {
  background-color: #F5F5F5;
}

.notification-item.unread {
  background-color: #F0F7FF;
}

.notification-item.unread:hover {
  background-color: #E6F2FF;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: #1A1A1B;
  margin-bottom: 4px;
}

.notification-content {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
  margin-bottom: 4px;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

/* 未读标记点 */
.unread-dot {
  width: 8px;
  height: 8px;
  background-color: #1976D2;
  border-radius: 50%;
  flex-shrink: 0;
  margin-left: 8px;
  margin-top: 4px;
}
</style>
