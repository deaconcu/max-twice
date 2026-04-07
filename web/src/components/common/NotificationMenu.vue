<script setup lang="ts">
import { ref, computed, onMounted, inject, watch } from 'vue'
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

// 底部提示状态
const showBottomTip = ref(false)

// 下拉刷新状态
const isRefreshing = ref(false)
const isPulling = ref(false)

// 消息数据
const messages = ref<Message[]>([])
const loading = ref(false)
const lastId = ref<number | undefined>(undefined)
const hasMore = ref(true)

// 会话期间的 lastViewedMessageId（用于标记 NEW）
const sessionLastId = ref<number>(0)

// 菜单状态
const menuOpen = ref(false)

// 未读通知数量
const unreadCount = ref<number>(0)

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

  if (type === MessageType.FOLLOW) return t('notification.newFollow')
  if (type === MessageType.UPVOTE) return t('notification.receivedUpvote')
  if (type === MessageType.NODE_COMMENT) return t('notification.nodeComment')
  if (type === MessageType.POST_COMMENT) return t('notification.postComment')
  if (type === MessageType.ROADMAP_COMMENT) return t('notification.roadmapComment')
  if (type === MessageType.REPLY_NODE_COMMENT) return t('notification.commentReply')
  if (type === MessageType.REPLY_POSTING_COMMENT) return t('notification.commentReply')
  if (type === MessageType.REPLY_ROADMAP_COMMENT) return t('notification.commentReply')
  if (type === MessageType.INVITE) return t('notification.inviteNotice')

  if (type === MessageType.COURSE_APPROVED) return t('notification.courseApproved')
  if (type === MessageType.PROFESSION_APPROVED) return t('notification.professionApproved')
  if (type === MessageType.COURSE_REJECTED) return t('notification.courseRejected')
  if (type === MessageType.COURSE_BANNED) return t('notification.courseBanned')
  if (type === MessageType.POST_REJECTED) return t('notification.postRejected')
  if (type === MessageType.POST_BANNED) return t('notification.postBanned')
  if (type === MessageType.COMMENT_REJECTED) return t('notification.commentRejected')
  if (type === MessageType.COMMENT_BANNED) return t('notification.commentBanned')
  if (type === MessageType.PROFESSION_REJECTED) return t('notification.professionRejected')
  if (type === MessageType.PROFESSION_BANNED) return t('notification.professionBanned')
  if (type === MessageType.ROADMAP_REJECTED) return t('notification.roadmapRejected')
  if (type === MessageType.ROADMAP_BANNED) return t('notification.roadmapBanned')
  if (type === MessageType.MEMORY_DECK_REJECTED) return t('notification.deckRejected')
  if (type === MessageType.MEMORY_DECK_BANNED) return t('notification.deckBanned')
  if (type === MessageType.NODE_REJECTED) return t('notification.nodeRejected')
  if (type === MessageType.NODE_BANNED) return t('notification.nodeBanned')

  return t('notification.systemNotice')
}

// 格式化消息内容
const getMessageContent = (message: Message): string => {
  try {
    const data = message.content ? JSON.parse(message.content) : {}
    const type = message.type
    // 优先使用 JSON 中的名称，然后使用 sender.name，最后使用默认值
    const senderName = data.commenterName || data.voterName || data.inviterName || message.sender?.name || t('notification.user')

    // 互动消息
    if (type === MessageType.FOLLOW) {
      return t('notification.followedYou', { name: senderName })
    }

    if (type === MessageType.UPVOTE) {
      const contentType = data.contentType

      if (contentType === 'roadmap') {
        // 路线图点赞
        const voterName = data.voterName || t('notification.user')
        const professionName = data.professionName || t('notification.profession')
        return t('notification.upvotedRoadmap', { name: voterName, profession: professionName })
      }

      // 原有逻辑：帖子和评论
      const objectType = data.objectType
      const voteType = data.voteType
      const nodeName = data.nodeName || t('notification.node')

      if (objectType === 1) {
        // POST
        if (voteType === 1) {
          // TWICE
          return t('notification.twiceArticle', { name: senderName, node: nodeName })
        } else if (voteType === 2) {
          // HELPFUL
          return t('notification.helpfulArticle', { name: senderName, node: nodeName })
        }
      } else if (objectType === 2) {
        // COMMENT
        return t('notification.upvotedComment', { name: senderName, node: nodeName })
      }
      return t('notification.upvotedContent', { name: senderName })
    }

    if (type === MessageType.INVITE) {
      const nodeName = data.nodeName || t('notification.node')
      return t('notification.invitedToNode', { name: senderName, node: nodeName })
    }

    if (type === MessageType.NODE_COMMENT) {
      const nodeName = data.nodeName || t('notification.node')
      return t('notification.commentedOnNode', { name: senderName, node: nodeName })
    }

    if (type === MessageType.POST_COMMENT) {
      const nodeName = data.nodeName || t('notification.node')
      return t('notification.commentedOnPost', { name: senderName, node: nodeName })
    }

    if (type === MessageType.REPLY_NODE_COMMENT) {
      const nodeName = data.nodeName || t('notification.node')
      return t('notification.repliedNodeComment', { name: senderName, node: nodeName })
    }

    if (type === MessageType.REPLY_POSTING_COMMENT) {
      const nodeName = data.nodeName || t('notification.node')
      return t('notification.repliedPostComment', { name: senderName, node: nodeName })
    }

    if (type === MessageType.REPLY_ROADMAP_COMMENT) {
      const professionName = data.professionName || t('notification.roadmap')
      return t('notification.repliedRoadmapComment', { name: senderName, profession: professionName })
    }

    if (type === MessageType.ROADMAP_COMMENT) {
      const professionName = data.professionName || t('notification.roadmap')
      return t('notification.commentedOnRoadmap', { name: senderName, profession: professionName })
    }

    // 审核消息
    if (type === MessageType.COURSE_APPROVED) {
      return t('notification.courseApprovedContent', { name: data.courseName || '' })
    }
    if (type === MessageType.PROFESSION_APPROVED) {
      return t('notification.professionApprovedContent', { name: data.professionName || '' })
    }

    if (type === MessageType.COURSE_REJECTED) {
      return t('notification.courseRejectedContent', { name: data.courseName || '', reason: data.reason || '' })
    }
    if (type === MessageType.COURSE_BANNED) {
      return t('notification.courseBannedContent', { name: data.courseName || '', reason: data.reason || '' })
    }

    if (type === MessageType.POST_REJECTED) {
      const nodeName = data.nodeName || ''
      return t('notification.postRejectedContent', { node: nodeName, reason: data.reason || '' })
    }
    if (type === MessageType.POST_BANNED) {
      const nodeName = data.nodeName || ''
      return t('notification.postBannedContent', { node: nodeName, reason: data.reason || '' })
    }

    if (type === MessageType.COMMENT_REJECTED) {
      const objectTypeName = getObjectTypeName(data.objectType)
      const objectTitle = data.objectTitle || ''
      const commentPreview = data.commentPreview || t('notification.comment')
      return t('notification.commentRejectedContent', { type: objectTypeName, title: objectTitle, preview: commentPreview, reason: data.reason || '' })
    }
    if (type === MessageType.COMMENT_BANNED) {
      const objectTypeName = getObjectTypeName(data.objectType)
      const objectTitle = data.objectTitle || ''
      const commentPreview = data.commentPreview || t('notification.comment')
      return t('notification.commentBannedContent', { type: objectTypeName, title: objectTitle, preview: commentPreview, reason: data.reason || '' })
    }

    if (type === MessageType.PROFESSION_REJECTED) {
      return t('notification.professionRejectedContent', { name: data.professionName || '', reason: data.reason || '' })
    }
    if (type === MessageType.PROFESSION_BANNED) {
      return t('notification.professionBannedContent', { name: data.professionName || '', reason: data.reason || '' })
    }

    if (type === MessageType.ROADMAP_REJECTED) {
      return t('notification.roadmapRejectedContent', { profession: data.professionName || '', reason: data.reason || '' })
    }
    if (type === MessageType.ROADMAP_BANNED) {
      return t('notification.roadmapBannedContent', { profession: data.professionName || '', reason: data.reason || '' })
    }

    if (type === MessageType.MEMORY_DECK_REJECTED) {
      return t('notification.deckRejectedContent', { node: data.postTitle || '', reason: data.reason || '' })
    }
    if (type === MessageType.MEMORY_DECK_BANNED) {
      return t('notification.deckBannedContent', { node: data.postTitle || '', reason: data.reason || '' })
    }

    if (type === MessageType.NODE_REJECTED) {
      return t('notification.nodeRejectedContent', { name: data.nodeName || '', reason: data.reason || '' })
    }
    if (type === MessageType.NODE_BANNED) {
      return t('notification.nodeBannedContent', { name: data.nodeName || '', reason: data.reason || '' })
    }

    return message.content || t('notification.systemNotice')
  } catch (error) {
    return message.content || t('notification.systemNotice')
  }
}

// 获取对象类型名称
const getObjectTypeName = (objectType: number): string => {
  switch (objectType) {
    case 1:
      return t('notification.objectTypePost')
    case 2:
      return t('notification.comment')
    case 3:
      return t('notification.objectTypeNode')
    case 4:
      return t('notification.objectTypeCourse')
    case 5:
      return t('notification.profession')
    case 6:
      return t('notification.objectTypeRoadmap')
    default:
      return t('notification.content')
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

  if (minutes < 1) return t('notification.justNow')
  if (minutes < 60) return t('notification.minutesAgo', { n: minutes })
  if (hours < 24) return t('notification.hoursAgo', { n: hours })
  if (days < 7) return t('notification.daysAgo', { n: days })

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

    // 根据筛选获取消息分类
    let category = MessageCategory.ALL
    if (filterCategory.value === 'interaction') {
      category = MessageCategory.INTERACTION
    } else if (filterCategory.value === 'system') {
      category = MessageCategory.SYSTEM
    }

    // 调用接口（返回 MessageListResponse）
    const res = await messageApi.getMessagesByCategory(category, lastId.value)

    if (res.code === 200 && res.data) {
      const { messages: newMessages, lastViewedMessageId } = res.data

      // 第一页时保存 lastViewedMessageId
      if (reset && lastViewedMessageId != null) {
        sessionLastId.value = lastViewedMessageId
      }

      // 合并消息
      if (reset) {
        messages.value = newMessages
      } else {
        messages.value.push(...newMessages)
      }

      // 更新分页信息
      if (newMessages.length > 0) {
        lastId.value = newMessages[newMessages.length - 1].id
      }

      hasMore.value = newMessages.length >= 10

      // 标记新消息
      markNewMessages()
    }
  } catch (error) {
    console.error('Error loading messages:', error)
    showSnackbar?.('加载消息失败', 'error')
  } finally {
    loading.value = false
  }
}

// 标记新消息（id > sessionLastId 显示 NEW 标签）
const markNewMessages = () => {
  messages.value.forEach((msg) => {
    msg.isNew = msg.id! > sessionLastId.value

    // 30秒后移除 NEW 标签
    if (msg.isNew) {
      setTimeout(() => {
        msg.isNew = false
      }, 30000)
    }
  })
}

// 监听菜单打开/关闭
watch(menuOpen, (isOpen) => {
  if (!isOpen) {
    // 关闭菜单时清空数据
    messages.value = []
    sessionLastId.value = 0
    lastId.value = undefined
    hasMore.value = true
  } else {
    // 打开菜单时加载第一页
    loadMessages(true)
  }
})

// 触发刷新（等同于关闭再打开）
const triggerRefresh = async () => {
  if (loading.value) return

  // 清空数据，重新加载第一页
  sessionLastId.value = 0
  await loadMessages(true)
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

// 点击消息项
const handleMessageClick = (message: Message) => {
  try {
    const data = message.content ? JSON.parse(message.content) : {}
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
      const contentType = data.contentType

      if (contentType === 'roadmap') {
        // 路线图点赞 - 跳转到路线图详情
        const roadmapId = data.roadmapId
        if (roadmapId) {
          url = `/roadmap/${roadmapId}`
        }
      } else {
        // 原有逻辑：帖子和评论点赞
        const objectType = data.objectType
        const objectId = data.objectId

        if (objectType === 1 && objectId) {
          // POST - 跳转到帖子
          url = `/read?postId=${objectId}`
        } else if (objectType === 2 && objectId) {
          // COMMENT - 跳转到评论
          url = `/read?commentId=${objectId}`
        }
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
    } else if (type === MessageType.POST_REJECTED || type === MessageType.POST_BANNED) {
      // 帖子审核 - 跳转到帖子
      const postId = data.postId
      if (postId) {
        url = `/read?postId=${postId}`
      }
    } else if (type === MessageType.COMMENT_REJECTED || type === MessageType.COMMENT_BANNED) {
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
    } else if (type === MessageType.ROADMAP_REJECTED || type === MessageType.ROADMAP_BANNED) {
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
    } else if (type === MessageType.NODE_REJECTED || type === MessageType.NODE_BANNED) {
      // 节点审核 - 跳转到节点
      const nodeId = data.nodeId
      if (nodeId) {
        url = `/read?nodeId=${nodeId}`
      }
    }

    // 如果有URL则跳转
    if (url) {
      router.push(url)
    }
  } catch (error) {
    console.error('处理消息点击失败:', error)
  }
}

// 获取未读消息数量
const fetchUnreadCount = async () => {
  try {
    const res = await messageApi.getUnreadCount()
    if (res.code === 200 && res.data != null) {
      unreadCount.value = res.data
    }
  } catch (error) {
    console.error('获取未读数量失败:', error)
  }
}

// 组件挂载时获取未读数量
onMounted(() => {
  fetchUnreadCount()

  // 每30秒轮询一次未读数量
  setInterval(fetchUnreadCount, 30000)
})

defineExpose({
  unreadCount,
})
</script>

<template>
  <v-menu v-model="menuOpen" :close-on-content-click="false" location="bottom end" offset="8">
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

    <v-card rounded="xl" class="notification-menu" width="420">
      <!-- 标题栏 -->
      <v-card-title class="notification-header">
        <span class="notification-title">{{ t('notification.title') }}</span>
        <v-chip size="x-small" color="error" variant="flat">{{ unreadCount }}</v-chip>
      </v-card-title>

      <v-divider></v-divider>

      <!-- 筛选按钮 -->
      <div class="filter-bar">
        <v-chip-group v-model="filterCategory" mandatory>
          <v-chip value="all" size="small" variant="flat">{{ t('notification.all') }}</v-chip>
          <v-chip value="interaction" size="small" variant="flat">{{ t('notification.interaction') }}</v-chip>
          <v-chip value="system" size="small" variant="flat">{{ t('notification.system') }}</v-chip>
        </v-chip-group>
        <v-btn
          icon
          size="small"
          variant="text"
          color="primary"
          :loading="loading"
          @click="triggerRefresh"
        >
          <v-icon size="20">mdi-refresh</v-icon>
        </v-btn>
      </div>

      <div
        ref="notificationList"
        class="notification-list"
        @scroll="handleScroll"
      >
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
              <div class="d-flex align-center mb-1">
                <div class="notification-title">{{ getMessageTitle(message) }}</div>
                <v-chip v-if="message.isNew" size="x-small" color="error" class="ml-2">{{ t('notification.new') }}</v-chip>
              </div>
              <div class="notification-content">{{ getMessageContent(message) }}</div>
              <div class="notification-time">{{ formatTime(message.createdAt) }}</div>
            </div>
            <div v-if="message.isRead === 0" class="unread-dot"></div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && filteredMessages.length === 0" class="empty-state">
          <v-icon size="48" color="grey-lighten-2">mdi-bell-outline</v-icon>
          <p class="text-grey-darken-1 mt-2">{{ t('notification.noMessages') }}</p>
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
}

.notification-list {
  max-height: 640px;
  overflow-y: auto;
}

/* 自定义滚动条样式 */
.notification-list::-webkit-scrollbar {
  width: 6px;
}

.notification-list::-webkit-scrollbar-track {
  background: transparent;
}

.notification-list::-webkit-scrollbar-thumb {
  background-color: rgba(var(--v-theme-on-surface), 0.2);
  border-radius: 3px;
}

.notification-list::-webkit-scrollbar-thumb:hover {
  background-color: rgba(var(--v-theme-on-surface), 0.3);
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
  line-height: 1.4;
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
