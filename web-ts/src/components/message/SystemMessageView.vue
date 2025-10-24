<script setup lang="ts">
  import { ref, watch } from 'vue'
  import { useI18n } from 'vue-i18n'
  import { MessageType, ObjectType, VoteType } from '@/types/enums'
  import type { User } from '@/types/user'
  import type { Node } from '@/types/node'
  import type { Message } from '@/types/message'
  import { getUserDisplayName, getObjectTypeName } from '@/utils/common'
  import UserCard from '@/components/user/UserCard.vue'

  const { t } = useI18n()

  interface SystemMessage extends Message {
    objectType?: number
    objectId?: string | number
    voteType?: number
    commentId?: string | number
    follower?: User
    inviter?: User
    commenter?: User
    node?: Node
  }

  interface Props {
    messageList?: SystemMessage[]
    lastReadTime?: Date | null
    showFilter?: boolean  // 是否显示筛选器
  }

  // Props
  const props = withDefaults(defineProps<Props>(), {
    messageList: () => [],
    lastReadTime: null,
    showFilter: true,  // 默认显示筛选器
  })

  interface LoadOptions {
    [key: string]: any
  }

  interface Emits {
    (e: 'loadData', loadOptions: LoadOptions): void
    (e: 'updateSystemMessageType', type: number): void
  }

  // Emits
  const emit = defineEmits<Emits>()

  // 响应式数据
  const messageTypeSelected = ref<number>(0)
  const systemMessageType = ref<number>(99)
  const systemMessageListKey = ref<number>(0)

  // 监听系统消息类型变化
  watch(systemMessageType, (newValue: number) => {
    console.log('system message type changed')
    systemMessageListKey.value++
    emit('updateSystemMessageType', newValue)
  })

  // 处理数据加载
  const handleLoadData = (loadOptions: LoadOptions): void => {
    emit('loadData', loadOptions)
  }

  const showDot = (time: string): boolean => {
    if (!props.lastReadTime) return false
    const date = new Date(time)
    return date > props.lastReadTime
  }

  const getDatePart = (time: string | undefined, index: number): string => {
    if (!time) return ''
    return time.split(' ')[index] || ''
  }

  // 设置系统消息类型
  const setSystemMessageType = (type: number): void => {
    systemMessageType.value = type
  }

  // 判断是否是拒绝消息
  const isRejectionMessage = (type: any): boolean => {
    return [
      MessageType.COURSE_REJECTED,
      MessageType.POST_REJECTED,
      MessageType.COMMENT_REJECTED,
      MessageType.PROFESSION_REJECTED,
      MessageType.ROADMAP_REJECTED,
      MessageType.MEMORY_DECK_REJECTED,
      MessageType.NODE_REJECTED
    ].includes(type)
  }

  // 判断是否是封禁消息
  const isBannedMessage = (type: any): boolean => {
    return [
      MessageType.COURSE_BANNED,
      MessageType.POST_BANNED,
      MessageType.COMMENT_BANNED,
      MessageType.PROFESSION_BANNED,
      MessageType.ROADMAP_BANNED,
      MessageType.MEMORY_DECK_BANNED,
      MessageType.NODE_BANNED
    ].includes(type)
  }

  // 判断是否是通过消息
  const isApprovedMessage = (type: any): boolean => {
    return [
      MessageType.COURSE_APPROVED,
      MessageType.PROFESSION_APPROVED
    ].includes(type)
  }

  // 获取审核消息文本
  const getModerationText = (message: SystemMessage): string => {
    try {
      const data = typeof message.content === 'string' ? JSON.parse(message.content) : message.content
      const type = message.type

      switch (type) {
        case MessageType.COURSE_REJECTED:
          return `您提交的课程《${data.courseName}》审核未通过。原因：${data.reason}`
        case MessageType.COURSE_BANNED:
          return `您提交的课程《${data.courseName}》已被封禁。原因：${data.reason}`
        case MessageType.COURSE_APPROVED:
          return `您提交的课程《${data.courseName}》审核通过！`

        case MessageType.POST_REJECTED:
          return `您在《${data.courseName} - ${data.nodeName}》下的帖子"${data.postPreview}"审核未通过。原因：${data.reason}`
        case MessageType.POST_BANNED:
          return `您在《${data.courseName} - ${data.nodeName}》下的帖子"${data.postPreview}"已被封禁。原因：${data.reason}`

        case MessageType.COMMENT_REJECTED:
          return `您在${getObjectTypeName(data.objectType)}《${data.objectTitle}》下的评论"${data.commentPreview}"审核未通过。原因：${data.reason}`
        case MessageType.COMMENT_BANNED:
          return `您在${getObjectTypeName(data.objectType)}《${data.objectTitle}》下的评论"${data.commentPreview}"已被封禁。原因：${data.reason}`

        case MessageType.PROFESSION_REJECTED:
          return `您提交的职业《${data.professionName}》审核未通过。原因：${data.reason}`
        case MessageType.PROFESSION_BANNED:
          return `您提交的职业《${data.professionName}》已被封禁。原因：${data.reason}`
        case MessageType.PROFESSION_APPROVED:
          return `您提交的职业《${data.professionName}》审核通过！`

        case MessageType.ROADMAP_REJECTED:
          return `您为职业《${data.professionName}》提交的路线图审核未通过。原因：${data.reason}`
        case MessageType.ROADMAP_BANNED:
          return `您为职业《${data.professionName}》提交的路线图已被封禁。原因：${data.reason}`

        case MessageType.MEMORY_DECK_REJECTED:
          return `您为帖子《${data.postTitle}》创建的卡片组《${data.deckTitle}》审核未通过。原因：${data.reason}`
        case MessageType.MEMORY_DECK_BANNED:
          return `您为帖子《${data.postTitle}》创建的卡片组《${data.deckTitle}》已被封禁。原因：${data.reason}`

        case MessageType.NODE_REJECTED:
          return `您在课程《${data.courseName}》中创建的节点《${data.nodeName}》审核未通过。原因：${data.reason}`
        case MessageType.NODE_BANNED:
          return `您在课程《${data.courseName}》中创建的节点《${data.nodeName}》已被封禁。原因：${data.reason}`

        default:
          return '审核通知'
      }
    } catch (error) {
      console.error('解析审核消息失败:', error)
      return '审核通知'
    }
  }
</script>

<template>
  <v-slide-y-reverse-transition hide-on-leave>
    <div class="text-body-1 d-flex justify-start">
      <v-row>
        <v-col>
          <v-alert
            :text="t('message.systemRetention')"
            type="warning"
            variant="tonal"
            density="compact"
            class="mb-5"
          ></v-alert>

          <!-- 消息筛选器 - 仅在互动消息中显示 -->
          <div v-if="showFilter" class="mb-4">
            <div class="d-flex align-center mb-3">
              <v-icon
                icon="mdi-filter-variant"
                color="grey-darken-2"
                size="16"
                class="mr-2"
              ></v-icon>
              <span class="text-body-2 font-weight-medium text-grey-darken-3">{{
                t('message.messageFilter')
              }}</span>
            </div>
            <div class="d-flex justify-center">
              <v-chip-group
                v-model="messageTypeSelected"
                selected-class="bg-orange text-white"
                mandatory
                class="my-2"
              >
                <v-chip variant="tonal" rounded="lg" @click="setSystemMessageType(99)">{{
                  t('message.all')
                }}</v-chip>
                <v-chip variant="tonal" rounded="lg" @click="setSystemMessageType(2)"
                  >新增关注</v-chip
                >
                <v-chip variant="tonal" rounded="lg" @click="setSystemMessageType(3)"
                  >新点赞</v-chip
                >
                <v-chip variant="tonal" rounded="lg" @click="setSystemMessageType(4)"
                  >邀请回答</v-chip
                >
                <v-chip variant="tonal" rounded="lg" @click="setSystemMessageType(5)"
                  >新评论</v-chip
                >
              </v-chip-group>
            </div>
          </div>

          <v-infinite-scroll
            :key="systemMessageListKey"
            :items="messageList"
            @load="handleLoadData"
          >
            <div v-for="(message, index) in messageList" :key="message.id || index" class="message-item mb-4">
              <!-- 日期分隔 -->
              <div
                v-if="
                  getDatePart(message.createdAt, 0) !==
                  getDatePart(messageList[index - 1]?.createdAt, 0)
                "
                class="date-divider mb-3"
              >
                <v-chip color="grey-darken-1" variant="flat" size="small" rounded="lg">
                  <v-icon icon="mdi-calendar" size="14" class="mr-1"></v-icon>
                  {{ getDatePart(message.createdAt, 0) }}
                </v-chip>
              </div>

              <!-- 消息卡片 -->
              <div class="message-card d-flex">
                <!-- 左侧时间和类型 -->
                <div class="message-left">
                  <div class="message-time text-body-2 text-grey-darken-1 mb-2">
                    <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                    {{ getDatePart(message.createdAt, 1) }}
                  </div>
                  <v-chip
                    v-if="message.type == MessageType.FOLLOW"
                    color="info"
                    variant="tonal"
                    size="small"
                    rounded="lg"
                    >新增关注</v-chip
                  >
                  <v-chip
                    v-if="message.type == MessageType.UPVOTE"
                    color="success"
                    variant="tonal"
                    size="small"
                    rounded="lg"
                    >点赞</v-chip
                  >
                  <v-chip
                    v-if="message.type == MessageType.INVITE"
                    color="warning"
                    variant="tonal"
                    size="small"
                    rounded="lg"
                    >邀请回答</v-chip
                  >
                  <v-chip
                    v-if="message.type == MessageType.NODE_COMMENT"
                    color="primary"
                    variant="tonal"
                    size="small"
                    rounded="lg"
                    >评论</v-chip
                  >
                  <v-chip
                    v-if="message.type == MessageType.POST_COMMENT"
                    color="primary"
                    variant="tonal"
                    size="small"
                    rounded="lg"
                    >评论</v-chip
                  >
                  <v-chip
                    v-if="message.type == MessageType.REPLY_NODE_COMMENT"
                    color="primary"
                    variant="tonal"
                    size="small"
                    rounded="lg"
                    >评论</v-chip
                  >
                  <v-chip
                    v-if="message.type == MessageType.REPLY_POSTING_COMMENT"
                    color="primary"
                    variant="tonal"
                    size="small"
                    rounded="lg"
                    >评论</v-chip
                  >
                  <!-- 审核消息类型标签 -->
                  <v-chip
                    v-if="isRejectionMessage(message.type)"
                    color="warning"
                    variant="tonal"
                    size="small"
                    rounded="lg"
                  >
                    <v-icon icon="mdi-alert-circle" size="14" class="mr-1"></v-icon>
                    审核未通过
                  </v-chip>
                  <v-chip
                    v-if="isBannedMessage(message.type)"
                    color="error"
                    variant="tonal"
                    size="small"
                    rounded="lg"
                  >
                    <v-icon icon="mdi-block-helper" size="14" class="mr-1"></v-icon>
                    内容封禁
                  </v-chip>
                  <v-chip
                    v-if="isApprovedMessage(message.type)"
                    color="success"
                    variant="tonal"
                    size="small"
                    rounded="lg"
                  >
                    <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                    审核通过
                  </v-chip>
                </div>

                <!-- 右侧消息内容 -->
                <div class="message-content-wrapper flex-grow-1">
                  <v-badge
                    color="error"
                    dot
                    :model-value="showDot(message.createdAt)"
                  >
                    <div
                      class="message-content-card pa-4 rounded-lg"
                      :class="{
                        'unread-message': showDot(message.createdAt),
                      }"
                    >
                      <div class="d-flex align-center">
                        <v-avatar
                          image="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg"
                          rounded="lg"
                          size="32"
                          class="me-3"
                        ></v-avatar>

                        <div class="flex-grow-1">
                          <!-- 关注消息 -->
                          <div v-if="message.type == MessageType.FOLLOW" class="message-text">
                      <UserCard :user-id="message.follower?.id || 0" :user-name="message.follower?.name || ''" />
                      关注了您
                          </div>

                          <!-- 点赞消息 -->
                          <div v-if="message.type == MessageType.UPVOTE" class="message-text">
                      <div v-if="message.objectType == ObjectType.POST">
                        <div v-if="message.voteType == VoteType.TWICE">
                          <UserCard :user-id="message.receiver?.id || 0" :user-name="message.receiver?.name || ''" />
                          认为您在目录
                          <a :href="`/read?postId=${message.objectId}`" target="_blank">{{
                            message.node?.name
                          }}</a>
                          的文章能被两次读懂
                        </div>
                        <div v-if="message.voteType == VoteType.HELPFUL">
                          <UserCard :user-id="message.receiver?.id || 0" :user-name="message.receiver?.name || ''" />
                          认为您在目录
                          <a :href="`/read?postId=${message.objectId}`" target="_blank">{{
                            message.node?.name
                          }}</a>
                          的文章有帮助
                        </div>
                      </div>
                      <div v-if="message.objectType == ObjectType.COMMENT">
                        <UserCard :user-id="message.receiver?.id || 0" :user-name="message.receiver?.name || ''" />
                        点赞了您在目录
                        <a :href="`/read?commentId=${message.objectId}`" target="_blank">{{
                          message.node?.name
                        }}</a>
                        下的评论
                      </div>
                          </div>

                          <!-- 邀请回答消息 -->
                          <div v-if="message.type == MessageType.INVITE" class="message-text">
                      <UserCard :user-id="message.inviter?.id || 0" :user-name="message.inviter?.name || ''" />
                      邀请您给目录
                      <a :href="`/read?nodeId=${message.node?.id}`" target="_blank">{{
                        message.node?.name
                      }}</a>
                      添加文章
                          </div>

                          <!-- 评论相关消息 -->
                          <div v-if="message.type == MessageType.NODE_COMMENT" class="message-text">
                      <UserCard :user-id="message.commenter?.id || 0" :user-name="message.commenter?.name || ''" />
                      评论了您创建的目录
                      <a :href="`/read?commentId=${message.commentId}`" target="_blank">{{
                        message.node?.name
                      }}</a>
                          </div>
                          <div v-if="message.type == MessageType.POST_COMMENT" class="message-text">
                      <UserCard :user-id="message.commenter?.id || 0" :user-name="message.commenter?.name || ''" />
                      评论了您在目录
                      <a :href="`/read?commentId=${message.commentId}`" target="_blank">{{
                        message.node?.name
                      }}</a>
                      下的文章
                          </div>
                          <div
                            v-if="message.type == MessageType.REPLY_NODE_COMMENT"
                            class="message-text"
                          >
                      <UserCard :user-id="message.commenter?.id || 0" :user-name="message.commenter?.name || ''" />
                      回复了您在目录
                      <a :href="`/read?commentId=${message.commentId}`" target="_blank">{{
                        message.node?.name
                      }}</a
                      >下的评论
                          </div>
                          <div
                            v-if="message.type == MessageType.REPLY_POSTING_COMMENT"
                            class="message-text"
                          >
                            <UserCard :user-id="message.commenter?.id || 0" :user-name="message.commenter?.name || ''" />
                            回复了您在目录
                            <a :href="`/read?commentId=${message.commentId}`" target="_blank">{{
                              message.node?.name
                            }}</a>
                            的文章下的评论
                          </div>

                          <!-- 审核消息 -->
                          <div v-if="isRejectionMessage(message.type)" class="message-text moderation-message rejected">
                            {{ getModerationText(message) }}
                          </div>
                          <div v-if="isBannedMessage(message.type)" class="message-text moderation-message banned">
                            {{ getModerationText(message) }}
                          </div>
                          <div v-if="isApprovedMessage(message.type)" class="message-text moderation-message approved">
                            {{ getModerationText(message) }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </v-badge>
                </div>
              </div>
            </div>
            <template #empty>
              <div class="text-body-2 text-grey py-9">{{ t('message.noMoreMessages') }}</div>
            </template>
          </v-infinite-scroll>
        </v-col>
      </v-row>
    </div>
  </v-slide-y-reverse-transition>
</template>

<style scoped>
  /* 日期分隔线 */
  .date-divider {
    text-align: center;
    padding: 16px 0;
    margin: 0;
  }

  /* 消息卡片 */
  .message-card {
    gap: 16px;
    align-items: flex-start;
  }

  /* 左侧时间和类型区域 */
  .message-left {
    min-width: 140px;
    max-width: 140px;
    flex-shrink: 0;
  }

  .message-time {
    display: flex;
    align-items: center;
    font-weight: 500;
  }

  /* 右侧消息内容区域 */
  .message-content-wrapper {
    flex: 1;
    min-width: 0;
  }

  .message-content-card {
    background: #f5f5f5;
    border: 1px solid transparent;
    transition: all 0.2s ease;
  }

  .message-content-card:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  /* 未读消息样式 */
  .unread-message {
    background: #e3f2fd !important;
    border-color: #2196f3 !important;
  }

  /* 消息文本样式 */
  .message-text {
    font-size: 14px;
    line-height: 1.6;
    color: #424242;
  }

  /* 审核消息样式 */
  .moderation-message {
    display: flex;
    align-items: center;
  }

  .message-text a {
    color: #1976d2 !important;
    text-decoration: none;
    font-weight: 500;
    transition: color 0.2s ease;
  }

  .message-text a:hover {
    color: #1565c0 !important;
    text-decoration: underline;
  }

  /* 芯片选中状态 */
  .v-chip.v-chip--selected {
    box-shadow: 0 2px 4px rgba(25, 118, 210, 0.3);
  }

  /* 筛选器标题 */
  .d-flex.align-center .v-icon {
    opacity: 0.7;
  }
</style>