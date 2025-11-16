<script setup lang="ts">
import { ref, computed, onMounted, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { messageApi } from '@/api'
import { MessageCategory, MessageType, ObjectType } from '@/enums'
import type { Message } from '@/types/message'

const { t } = useI18n()
const router = useRouter()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 通知列表引用
const notificationList = ref<HTMLElement | null>(null)

// 筛选状态
const filterCategory = ref<'all' | 'interaction' | 'system'>('all')

// 下拉刷新状态
const isPulling = ref(false)
const isRefreshing = ref(false)
const startY = ref(0)

// 底部提示状态
const showBottomTip = ref(false)

// 消息数据
const messages = ref<Message[]>([])
const loading = ref(false)
const lastId = ref<number | undefined>(undefined)
const hasMore = ref(true)

// 未读通知数量
const unreadCount = computed(() => messages.value.filter((m) => m.isRead === 0).length)

// 根据筛选条件过滤消息
const filteredMessages = computed(() => {
  if (filterCategory.value === 'all') {
    return messages.value
  }

  const categoryTypes = {
    interaction: [
      MessageType.FOLLOW,
      MessageType.UPVOTE,
      MessageType.INVITE,
      MessageType.NODE_COMMENT,
      MessageType.POST_COMMENT,
      MessageType.REPLY_NODE_COMMENT,
      MessageType.REPLY_POSTING_COMMENT,
      MessageType.REPLY_ROADMAP_COMMENT,
      MessageType.ROADMAP_COMMENT,
    ],
    system: [
      MessageType.APPLY_COURSE,
      MessageType.COURSE_REJECTED,
      MessageType.COURSE_BANNED,
      MessageType.COURSE_APPROVED,
      MessageType.POST_REJECTED,
      MessageType.POST_BANNED,
      MessageType.COMMENT_REJECTED,
      MessageType.COMMENT_BANNED,
      MessageType.PROFESSION_REJECTED,
      MessageType.PROFESSION_BANNED,
      MessageType.PROFESSION_APPROVED,
      MessageType.ROADMAP_REJECTED,
      MessageType.ROADMAP_BANNED,
      MessageType.MEMORY_DECK_REJECTED,
      MessageType.MEMORY_DECK_BANNED,
      MessageType.NODE_REJECTED,
      MessageType.NODE_BANNED,
      MessageType.SYSTEM,
    ],
  }

  const types = categoryTypes[filterCategory.value] || []
  return messages.value.filter((m) => m.type && types.includes(m.type))
})

// 获取消息图标和颜色
const getMessageIcon = (message: Message): { icon: string; color: string } => {
  const type = message.type

  // 互动消息
  if (type === MessageType.FOLLOW) {
    return { icon: 'mdi-account-plus', color: 'primary' }
  }
  if (type === MessageType.UPVOTE) {
    return { icon: 'mdi-thumb-up', color: 'primary' }
  }
  if (
    [MessageType.NODE_COMMENT, MessageType.POST_COMMENT, MessageType.ROADMAP_COMMENT].includes(
      type as number
    )
  ) {
    return { icon: 'mdi-comment', color: 'primary' }
  }
  if (
    [
      MessageType.REPLY_NODE_COMMENT,
      MessageType.REPLY_POSTING_COMMENT,
      MessageType.REPLY_ROADMAP_COMMENT,
    ].includes(type as number)
  ) {
    return { icon: 'mdi-reply', color: 'primary' }
  }
  if (type === MessageType.INVITE) {
    return { icon: 'mdi-email-open', color: 'primary' }
  }

  // 审核通过消息
  if ([MessageType.COURSE_APPROVED, MessageType.PROFESSION_APPROVED].includes(type as number)) {
    return { icon: 'mdi-check-circle', color: 'success' }
  }

  // 拒绝消息
  if (
    [
      MessageType.COURSE_REJECTED,
      MessageType.POST_REJECTED,
      MessageType.COMMENT_REJECTED,
      MessageType.PROFESSION_REJECTED,
      MessageType.ROADMAP_REJECTED,
      MessageType.MEMORY_DECK_REJECTED,
      MessageType.NODE_REJECTED,
    ].includes(type as number)
  ) {
    return { icon: 'mdi-alert-circle', color: 'warning' }
  }

  // 封禁消息
  if (
    [
      MessageType.COURSE_BANNED,
      MessageType.POST_BANNED,
      MessageType.COMMENT_BANNED,
      MessageType.PROFESSION_BANNED,
      MessageType.ROADMAP_BANNED,
      MessageType.MEMORY_DECK_BANNED,
      MessageType.NODE_BANNED,
    ].includes(type as number)
  ) {
    return { icon: 'mdi-cancel', color: 'error' }
  }

  // 默认系统消息
  return { icon: 'mdi-information', color: 'info' }
}

// 格式化消息标题
const getMessageTitle = (message: Message): string => {
  const type = message.type

  if (type === MessageType.FOLLOW) return '新增关注'
  if (type === MessageType.UPVOTE) return '收到点赞'
  if (type === MessageType.NODE_COMMENT) return '节点评论'
  if (type === MessageType.POST_COMMENT) return '帖子评论'
  if (type === MessageType.ROADMAP_COMMENT) return '路线图评论'
  if (type === MessageType.REPLY_NODE_COMMENT) return '评论回复'
  if (type === MessageType.REPLY_POSTING_COMMENT) return '评论回复'
  if (type === MessageType.REPLY_ROADMAP_COMMENT) return '评论回复'
  if (type === MessageType.INVITE) return '邀请通知'

  if (type === MessageType.COURSE_APPROVED) return '课程审核通过'
  if (type === MessageType.PROFESSION_APPROVED) return '职业审核通过'
  if (type === MessageType.COURSE_REJECTED) return '课程审核未通过'
  if (type === MessageType.COURSE_BANNED) return '课程已被封禁'
  if (type === MessageType.POST_REJECTED) return '帖子审核未通过'
  if (type === MessageType.POST_BANNED) return '帖子已被封禁'
  if (type === MessageType.COMMENT_REJECTED) return '评论审核未通过'
  if (type === MessageType.COMMENT_BANNED) return '评论已被封禁'
  if (type === MessageType.PROFESSION_REJECTED) return '职业审核未通过'
  if (type === MessageType.PROFESSION_BANNED) return '职业已被封禁'
  if (type === MessageType.ROADMAP_REJECTED) return '路线图审核未通过'
  if (type === MessageType.ROADMAP_BANNED) return '路线图已被封禁'
  if (type === MessageType.MEMORY_DECK_REJECTED) return '卡片组审核未通过'
  if (type === MessageType.MEMORY_DECK_BANNED) return '卡片组已被封禁'
  if (type === MessageType.NODE_REJECTED) return '节点审核未通过'
  if (type === MessageType.NODE_BANNED) return '节点已被封禁'

  return '系统通知'
}

// 格式化消息内容
const getMessageContent = (message: Message): string => {
  try {
    const data = message.content ? JSON.parse(message.content) : {}
    const type = message.type
    const senderName = message.sender?.name || '用户'

    // 互动消息
    if (type === MessageType.FOLLOW) {
      return `${senderName} 关注了你`
    }

    if (type === MessageType.UPVOTE) {
      const objectType = data.objectType
      const voteType = data.voteType
      const nodeName = data.nodeName || '节点'

      if (objectType === 1) {
        // POST
        if (voteType === 1) {
          // TWICE
          return `${senderName} 认为您在目录《${nodeName}》的文章能被两次读懂`
        } else if (voteType === 2) {
          // HELPFUL
          return `${senderName} 认为您在目录《${nodeName}》的文章有帮助`
        }
      } else if (objectType === 2) {
        // COMMENT
        return `${senderName} 点赞了您在目录《${nodeName}》下的评论`
      }
      return `${senderName} 赞了你的内容`
    }

    if (type === MessageType.INVITE) {
      const nodeName = data.nodeName || '节点'
      return `${senderName} 邀请您给目录《${nodeName}》添加文章`
    }

    if (type === MessageType.NODE_COMMENT) {
      const nodeName = data.nodeName || '节点'
      return `${senderName} 评论了您创建的目录《${nodeName}》`
    }

    if (type === MessageType.POST_COMMENT) {
      const nodeName = data.nodeName || '节点'
      return `${senderName} 评论了您在目录《${nodeName}》下的文章`
    }

    if (type === MessageType.REPLY_NODE_COMMENT) {
      const nodeName = data.nodeName || '节点'
      return `${senderName} 回复了您在目录《${nodeName}》下的评论`
    }

    if (type === MessageType.REPLY_POSTING_COMMENT) {
      const nodeName = data.nodeName || '节点'
      return `${senderName} 回复了您在目录《${nodeName}》的文章下的评论`
    }

    if (type === MessageType.REPLY_ROADMAP_COMMENT) {
      const professionName = data.professionName || '路线图'
      return `${senderName} 回复了您在路线图《${professionName}》下的评论`
    }

    if (type === MessageType.ROADMAP_COMMENT) {
      const professionName = data.professionName || '路线图'
      return `${senderName} 评论了您创建的路线图《${professionName}》`
    }

    // 审核消息
    if (type === MessageType.COURSE_APPROVED) {
      return `您提交的课程《${data.courseName || ''}》审核通过！`
    }
    if (type === MessageType.PROFESSION_APPROVED) {
      return `您提交的职业《${data.professionName || ''}》审核通过！`
    }

    if (type === MessageType.COURSE_REJECTED) {
      return `您提交的课程《${data.courseName || ''}》审核未通过。原因：${data.reason || ''}`
    }
    if (type === MessageType.COURSE_BANNED) {
      return `您提交的课程《${data.courseName || ''}》已被封禁。原因：${data.reason || ''}`
    }

    if (type === MessageType.POST_REJECTED) {
      const courseName = data.courseName || ''
      const nodeName = data.nodeName || ''
      const postPreview = data.postPreview || '帖子'
      return `您在《${courseName} - ${nodeName}》下的帖子"${postPreview}"审核未通过。原因：${data.reason || ''}`
    }
    if (type === MessageType.POST_BANNED) {
      const courseName = data.courseName || ''
      const nodeName = data.nodeName || ''
      const postPreview = data.postPreview || '帖子'
      return `您在《${courseName} - ${nodeName}》下的帖子"${postPreview}"已被封禁。原因：${data.reason || ''}`
    }

    if (type === MessageType.COMMENT_REJECTED) {
      const objectTypeName = getObjectTypeName(data.objectType)
      const objectTitle = data.objectTitle || ''
      const commentPreview = data.commentPreview || '评论'
      return `您在${objectTypeName}《${objectTitle}》下的评论"${commentPreview}"审核未通过。原因：${data.reason || ''}`
    }
    if (type === MessageType.COMMENT_BANNED) {
      const objectTypeName = getObjectTypeName(data.objectType)
      const objectTitle = data.objectTitle || ''
      const commentPreview = data.commentPreview || '评论'
      return `您在${objectTypeName}《${objectTitle}》下的评论"${commentPreview}"已被封禁。原因：${data.reason || ''}`
    }

    if (type === MessageType.PROFESSION_REJECTED) {
      return `您提交的职业《${data.professionName || ''}》审核未通过。原因：${data.reason || ''}`
    }
    if (type === MessageType.PROFESSION_BANNED) {
      return `您提交的职业《${data.professionName || ''}》已被封禁。原因：${data.reason || ''}`
    }

    if (type === MessageType.ROADMAP_REJECTED) {
      return `您为职业《${data.professionName || ''}》提交的路线图审核未通过。原因：${data.reason || ''}`
    }
    if (type === MessageType.ROADMAP_BANNED) {
      return `您为职业《${data.professionName || ''}》提交的路线图已被封禁。原因：${data.reason || ''}`
    }

    if (type === MessageType.MEMORY_DECK_REJECTED) {
      return `您为帖子《${data.postTitle || ''}》创建的卡片组《${data.deckTitle || ''}》审核未通过。原因：${data.reason || ''}`
    }
    if (type === MessageType.MEMORY_DECK_BANNED) {
      return `您为帖子《${data.postTitle || ''}》创建的卡片组《${data.deckTitle || ''}》已被封禁。原因：${data.reason || ''}`
    }

    if (type === MessageType.NODE_REJECTED) {
      return `您在课程《${data.courseName || ''}》中创建的节点《${data.nodeName || ''}》审核未通过。原因：${data.reason || ''}`
    }
    if (type === MessageType.NODE_BANNED) {
      return `您在课程《${data.courseName || ''}》中创建的节点《${data.nodeName || ''}》已被封禁。原因：${data.reason || ''}`
    }

    return message.content || '新消息'
  } catch (error) {
    console.error('解析消息内容失败:', error)
    return message.content || '新消息'
  }
}

// 获取对象类型名称
const getObjectTypeName = (objectType: number): string => {
  switch (objectType) {
    case 1:
      return '帖子'
    case 2:
      return '评论'
    case 3:
      return '节点'
    case 4:
      return '课程'
    case 5:
      return '职业'
    case 6:
      return '路线图'
    default:
      return '内容'
  }
}

// 格式化时间
const formatTime = (timeStr?: string): string => {
  if (!timeStr) return ''

  const messageTime = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - messageTime.getTime()

  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  return timeStr.split(' ')[0] || timeStr
}

// 加载消息
const loadMessages = async (reset = false) => {
  if (loading.value || (!hasMore.value && !reset)) return

  try {
    loading.value = true
    if (reset) {
      lastId.value = undefined
      messages.value = []
      hasMore.value = true
    }

    // 获取所有类型的消息（互动 + 系统）
    const [interactionRes, systemRes] = await Promise.all([
      messageApi.getMessagesByCategory(MessageCategory.INTERACTION, lastId.value),
      messageApi.getMessagesByCategory(MessageCategory.SYSTEM, lastId.value),
    ])

    const newMessages: Message[] = []
    if (interactionRes.code === 200 && interactionRes.data) {
      newMessages.push(...interactionRes.data)
    }
    if (systemRes.code === 200 && systemRes.data) {
      newMessages.push(...systemRes.data)
    }

    // 按时间排序
    newMessages.sort((a, b) => {
      const timeA = a.createdAt ? new Date(a.createdAt).getTime() : 0
      const timeB = b.createdAt ? new Date(b.createdAt).getTime() : 0
      return timeB - timeA
    })

    if (reset) {
      messages.value = newMessages
    } else {
      messages.value.push(...newMessages)
    }

    if (newMessages.length > 0) {
      lastId.value = newMessages[newMessages.length - 1].id
    }

    hasMore.value = newMessages.length >= 10
  } catch (error) {
    console.error('Error loading messages:', error)
    showSnackbar?.('加载消息失败', 'error')
  } finally {
    loading.value = false
  }
}

// 触发刷新
const triggerRefresh = async () => {
  if (isRefreshing.value) return

  isRefreshing.value = true
  await loadMessages(true)
  isRefreshing.value = false
  isPulling.value = false
}

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

  // 检测是否滚动到底部，加载更多
  if (scrollTop + clientHeight >= scrollHeight - 5 && hasMore.value && !loading.value) {
    loadMessages(false)
  }

  // 显示底部提示
  if (scrollTop + clientHeight >= scrollHeight - 5) {
    showBottomTip.value = !hasMore.value
  } else {
    showBottomTip.value = false
  }
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

// 点击消息项
const handleMessageClick = (message: Message) => {
  console.log('点击的消息:', message)
  console.log('消息类型:', message.type)

  try {
    const data = message.content ? JSON.parse(message.content) : {}
    console.log('解析的内容:', data)
    const type = message.type

    // 构建跳转链接
    let url = ''

    // 互动消息
    if (type === MessageType.FOLLOW) {
      // 跳转到用户主页
      if (message.sender?.id) {
        url = `/user/${message.sender.id}`
      }
    } else if (type === MessageType.UPVOTE) {
      const objectType = data.objectType
      const objectId = data.objectId

      if (objectType === 1 && objectId) {
        // POST - 跳转到帖子
        url = `/read?postId=${objectId}`
      } else if (objectType === 2 && objectId) {
        // COMMENT - 跳转到评论
        url = `/read?commentId=${objectId}`
      }
    } else if (type === MessageType.INVITE) {
      // 邀请回答 - 跳转到节点
      const nodeId = data.nodeId
      if (nodeId) {
        url = `/read?nodeId=${nodeId}`
      }
    } else if (
      type === MessageType.NODE_COMMENT ||
      type === MessageType.POST_COMMENT ||
      type === MessageType.REPLY_NODE_COMMENT ||
      type === MessageType.REPLY_POSTING_COMMENT ||
      type === MessageType.REPLY_ROADMAP_COMMENT ||
      type === MessageType.ROADMAP_COMMENT
    ) {
      // 所有评论相关消息 - 跳转到评论
      const commentId = data.commentId
      if (commentId) {
        url = `/read?commentId=${commentId}`
      }
    }
    // 审核消息
    else if (
      type === MessageType.COURSE_APPROVED ||
      type === MessageType.COURSE_REJECTED ||
      type === MessageType.COURSE_BANNED
    ) {
      // 课程审核 - 跳转到课程
      const courseId = data.courseId
      if (courseId) {
        url = `/course/${courseId}`
      }
    } else if (
      type === MessageType.POST_REJECTED ||
      type === MessageType.POST_BANNED
    ) {
      // 帖子审核 - 跳转到帖子
      const postId = data.postId
      if (postId) {
        url = `/read?postId=${postId}`
      }
    } else if (
      type === MessageType.COMMENT_REJECTED ||
      type === MessageType.COMMENT_BANNED
    ) {
      // 评论审核 - 跳转到评论
      const commentId = data.commentId
      if (commentId) {
        url = `/read?commentId=${commentId}`
      }
    } else if (
      type === MessageType.PROFESSION_APPROVED ||
      type === MessageType.PROFESSION_REJECTED ||
      type === MessageType.PROFESSION_BANNED
    ) {
      // 职业审核 - 跳转到职业
      const professionId = data.professionId
      if (professionId) {
        url = `/profession/${professionId}`
      }
    } else if (
      type === MessageType.ROADMAP_REJECTED ||
      type === MessageType.ROADMAP_BANNED
    ) {
      // 路线图审核 - 跳转到职业/路线图
      const professionId = data.professionId
      if (professionId) {
        url = `/profession/${professionId}`
      }
    } else if (
      type === MessageType.MEMORY_DECK_REJECTED ||
      type === MessageType.MEMORY_DECK_BANNED
    ) {
      // 卡片组审核 - 跳转到帖子
      const postId = data.postId
      if (postId) {
        url = `/read?postId=${postId}`
      }
    } else if (
      type === MessageType.NODE_REJECTED ||
      type === MessageType.NODE_BANNED
    ) {
      // 节点审核 - 跳转到节点
      const nodeId = data.nodeId
      if (nodeId) {
        url = `/read?nodeId=${nodeId}`
      }
    }

    // 如果有URL则跳转
    console.log('跳转URL:', url)
    if (url) {
      router.push(url)
    }
  } catch (error) {
    console.error('处理消息点击失败:', error)
  }
}

// 组件挂载时加载消息
onMounted(() => {
  loadMessages(true)
})

defineExpose({
  unreadCount,
})
</script>

<template>
  <v-menu :close-on-content-click="false" location="bottom end" offset="8">
    <template #activator="{ props }">
      <button class="icon-btn" :title="t('notification.title')" v-bind="props">
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

    <v-card rounded="lg" class="notification-menu" width="380">
      <!-- 标题栏 -->
      <v-card-title class="notification-header">
        <span class="notification-title">{{ t('notification.title') }}</span>
        <v-chip size="x-small" color="error" variant="flat">{{ unreadCount }}</v-chip>
      </v-card-title>

      <v-divider></v-divider>

      <!-- 筛选按钮 -->
      <div class="filter-bar">
        <v-chip-group v-model="filterCategory" mandatory>
          <v-chip value="all" size="small" variant="flat">全部</v-chip>
          <v-chip value="interaction" size="small" variant="flat">互动</v-chip>
          <v-chip value="system" size="small" variant="flat">系统</v-chip>
        </v-chip-group>
      </div>

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
          <span class="ml-2">{{
            isRefreshing ? t('notification.refreshing') : t('notification.pullToRefresh')
          }}</span>
        </div>

        <!-- 消息列表 -->
        <div
          v-for="message in filteredMessages"
          :key="message.id"
          class="notification-item"
          :class="{ unread: message.isRead === 0 }"
          @click="handleMessageClick(message)"
        >
          <div class="d-flex align-start">
            <v-avatar size="36" class="mr-3 flex-shrink-0" :color="getMessageIcon(message).color">
              <v-icon size="18" color="white">{{ getMessageIcon(message).icon }}</v-icon>
            </v-avatar>
            <div class="flex-grow-1">
              <div class="notification-title">{{ getMessageTitle(message) }}</div>
              <div class="notification-content">{{ getMessageContent(message) }}</div>
              <div class="notification-time">{{ formatTime(message.createdAt) }}</div>
            </div>
            <div v-if="message.isRead === 0" class="unread-dot"></div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && filteredMessages.length === 0" class="empty-state">
          <v-icon size="48" color="grey-lighten-2">mdi-bell-outline</v-icon>
          <p class="text-grey-darken-1 mt-2">暂无消息</p>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="loading-state">
          <v-progress-circular
            indeterminate
            size="24"
            width="2"
            color="primary"
          ></v-progress-circular>
        </div>

        <!-- 底部提示 -->
        <div v-if="showBottomTip" class="bottom-tip">
          <v-divider class="mb-2"></v-divider>
          <div class="text-center py-3">
            <v-icon size="16" color="grey">mdi-check-circle-outline</v-icon>
            <span class="text-caption text-grey ml-1">{{ t('notification.bottomTip') }}</span>
          </div>
        </div>
      </div>

      <v-divider></v-divider>

      <!-- 底部操作 -->
      <div class="notification-footer">
        <v-btn variant="text" size="small" color="primary" block> 查看全部消息 </v-btn>
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
  background-color: rgb(var(--v-theme-surface-variant));
}

.notification-menu {
  border: 1px solid rgb(var(--v-theme-border)) !important;
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

.filter-bar {
  padding: 8px 16px;
}

.notification-list {
  max-height: 420px;
  overflow-y: auto;
}

.pull-to-refresh {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  font-size: 13px;
  color: rgb(var(--v-theme-on-surface-variant));
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid rgb(var(--v-theme-border));
  cursor: pointer;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: rgb(var(--v-theme-surface-variant));
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item.unread {
  background-color: rgba(var(--v-theme-primary), 0.05);
}

.notification-item .notification-title {
  font-size: 14px;
  font-weight: 600;
  color: rgb(var(--v-theme-on-surface));
  margin-bottom: 4px;
}

.notification-item .notification-content {
  font-size: 13px;
  color: rgb(var(--v-theme-on-surface-variant));
  margin-bottom: 4px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-item .notification-time {
  font-size: 12px;
  color: rgb(var(--v-theme-on-surface-variant));
  opacity: 0.7;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: rgb(var(--v-theme-error));
  flex-shrink: 0;
  margin-top: 4px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.bottom-tip {
  padding: 0 16px;
}

.notification-footer {
  padding: 8px;
}
</style>
