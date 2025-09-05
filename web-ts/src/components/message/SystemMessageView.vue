<script setup lang="ts">
  import { ref, watch } from 'vue'
  import { useI18n } from 'vue-i18n'
  import { MessageType, ObjectType, VoteType } from '@/types/enums'
  import type { User } from '@/types/user'
  import type { Node } from '@/types/node'

  const { t } = useI18n()

  interface SystemMessage {
    id: string | number
    type: number
    createdAt: string
    objectType?: number
    objectId?: string | number
    voteType?: number
    commentId?: string | number
    follower?: User
    receiver?: User
    inviter?: User
    commenter?: User
    node?: Node
  }

  interface Props {
    messageList?: SystemMessage[]
    lastReadTime?: Date | null
  }

  // Props
  const props = withDefaults(defineProps<Props>(), {
    messageList: () => [],
    lastReadTime: null,
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

          <div class="mb-4">
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
            :on-load="handleLoadData"
          >
            <div v-for="(message, index) in messageList" :key="message.id || index" class="mb-0">
              <div
                v-if="
                  getDatePart(message.createdAt, 0) !==
                  getDatePart(messageList[index - 1]?.createdAt, 0)
                "
                class="pe-6 py-5 my-0 border-e text-end date-column"
              >
                <v-chip color="grey" variant="text" size="default" rounded="lg" class="pe-0">
                  <font class="font-weight-bold">
                    {{ getDatePart(message.createdAt, 0) }}
                  </font>
                </v-chip>
              </div>
              <div class="d-flex justify-start align-center">
                <div class="border-e text-end pe-6 py-3 time-column">
                  {{ getDatePart(message.createdAt, 1) }}
                </div>
                <div class="mt-2">
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
                </div>
              </div>
              <div class="ps-6 w-100">
                <v-badge
                  class="w-100"
                  color="error"
                  dot
                  offset-x="3"
                  offset-y="2"
                  :model-value="showDot(message.createdAt)"
                >
                  <div
                    class="d-flex align-center px-3 py-3 rounded-lg d-inline-flex bg-grey-lighten-5 w-100"
                    :class="{
                      'border-primary bg-blue-lighten-5 shadow-sm': showDot(message.createdAt),
                    }"
                  >
                    <v-avatar
                      image="https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg"
                      rounded="lg"
                      size="28"
                      class="me-3"
                    ></v-avatar>

                    <!-- 关注消息 -->
                    <div v-if="message.type == MessageType.FOLLOW" class="message-content">
                      <a :href="`/user?id=${message.follower?.id}`" target="_blank">{{
                        message.follower?.name
                      }}</a>
                      关注了您
                    </div>

                    <!-- 点赞消息 -->
                    <div v-if="message.type == MessageType.UPVOTE" class="message-content">
                      <div v-if="message.objectType == ObjectType.POST">
                        <div v-if="message.voteType == VoteType.TWICE">
                          <a :href="`/user?id=${message.receiver?.id}`" target="_blank">{{
                            message.receiver?.name
                          }}</a>
                          认为您在目录
                          <a :href="`/read?postId=${message.objectId}`" target="_blank">{{
                            message.node?.name
                          }}</a>
                          的文章能被两次读懂
                        </div>
                        <div v-if="message.voteType == VoteType.HELPFUL">
                          <a :href="`/user?id=${message.receiver?.id}`" target="_blank">{{
                            message.receiver?.name
                          }}</a>
                          认为您在目录
                          <a :href="`/read?postId=${message.objectId}`" target="_blank">{{
                            message.node?.name
                          }}</a>
                          的文章有帮助
                        </div>
                      </div>
                      <div v-if="message.objectType == ObjectType.COMMENT">
                        <a :href="`/user?id=${message.receiver?.id}`" target="_blank">{{
                          message.receiver?.name
                        }}</a>
                        点赞了您在目录
                        <a :href="`/read?commentId=${message.objectId}`" target="_blank">{{
                          message.node?.name
                        }}</a>
                        下的评论
                      </div>
                    </div>

                    <!-- 邀请回答消息 -->
                    <div v-if="message.type == MessageType.INVITE" class="message-content">
                      <a :href="`/user?id=${message.inviter?.id}`" target="_blank">{{
                        message.inviter?.name
                      }}</a>
                      邀请您给目录
                      <a :href="`/read?nodeId=${message.node?.id}`" target="_blank">{{
                        message.node?.name
                      }}</a>
                      添加文章
                    </div>

                    <!-- 评论相关消息 -->
                    <div v-if="message.type == MessageType.NODE_COMMENT" class="message-content">
                      <a :href="`/user?id=${message.commenter?.id}`" target="_blank">{{
                        message.commenter?.name
                      }}</a>
                      评论了您创建的目录
                      <a :href="`/read?commentId=${message.commentId}`" target="_blank">{{
                        message.node?.name
                      }}</a>
                    </div>
                    <div v-if="message.type == MessageType.POST_COMMENT" class="message-content">
                      <a :href="`/user?id=${message.commenter?.id}`" target="_blank">{{
                        message.commenter?.name
                      }}</a>
                      评论了您在目录
                      <a :href="`/read?commentId=${message.commentId}`" target="_blank">{{
                        message.node?.name
                      }}</a>
                      下的文章
                    </div>
                    <div
                      v-if="message.type == MessageType.REPLY_NODE_COMMENT"
                      class="message-content"
                    >
                      <a :href="`/user?id=${message.commenter?.id}`" target="_blank">{{
                        message.commenter?.name
                      }}</a>
                      回复了您在目录
                      <a :href="`/read?commentId=${message.commentId}`" target="_blank">{{
                        message.node?.name
                      }}</a
                      >下的评论
                    </div>
                    <div
                      v-if="message.type == MessageType.REPLY_POSTING_COMMENT"
                      class="message-content"
                    >
                      <a :href="`/user?id=${message.commenter?.id}`" target="_blank">{{
                        message.commenter?.name
                      }}</a>
                      回复了您在目录
                      <a :href="`/read?commentId=${message.commentId}`" target="_blank">{{
                        message.node?.name
                      }}</a>
                      的文章下的评论
                    </div>
                  </div>
                </v-badge>
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
  content a {
    color: #225a9b !important;
    text-decoration: none;
    font-weight: 500;
    transition: color 0.3s ease;
  }

  .message-content a:hover {
    color: #1565c0 !important;
    text-decoration: underline;
  }

  /* 消息卡片悬浮效果 */
  .d-inline-flex:hover {
    transform: translateY(-1px);
    transition: all 0.2s ease;
  }

  /* 芯片选中状态增强 */
  .v-chip.v-chip--selected {
    box-shadow: 0 2px 4px rgba(25, 118, 210, 0.3);
  }

  /* 边框线条优化 */
  .border-e {
    border-right: 1px solid rgba(0, 0, 0, 0.08) !important;
  }

  /* 日期和时间列宽度 */
  .date-column {
    width: 138px;
  }

  .time-column {
    min-width: 138px;
  }

  /* 筛选器标题样式 */
  .d-flex.align-center .v-icon {
    opacity: 0.7;
  }
</style>