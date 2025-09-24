<script setup lang="ts">
  import { inject, nextTick, onMounted, ref, watch } from 'vue'
  import { contentServiceV1, upvoteServiceV1 } from '@/services/api/v1/apiServiceV1'
  import { useI18n } from 'vue-i18n'
  import { PostType, VoteType } from '@/types/enums'
  import type { Post } from '@/types/post'
  import type { Course } from '@/types/course'
  import type { Node } from '@/types/node'

  import UserCard from '../user/UserCard.vue'
  import TextSelectionAI from '@/components/common/TextSelectionAI.vue'
  import RichContentRenderer from '@/components/common/RichContentRenderer.vue'

  // 🔴 导入Post浏览量跟踪服务
  import postViewTracking from '@/services/postViewTracking'

  interface DataProps {
    path: string
    course: Course
    node: Node
  }

  interface Props {
    data: DataProps
    posting: Post
    currNode: Record<string, any>
    detail?: boolean
    isLearning?: boolean
  }

  interface Emits {
    (e: 'loadData', parts: string[]): void
    (e: 'switchTab', tab: string, posting: Post | string): void
    (e: 'markNodeCompleted'): void
  }

  const props = withDefaults(defineProps<Props>(), {
    detail: false,
    isLearning: false,
  })

  const emit = defineEmits<Emits>()

  const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')
  const { t } = useI18n()

  const isOverflow = ref<boolean>(false)
  const contentRef = ref<HTMLElement | null>(null)
  const updateOverflowState = () => {
    const el = contentRef.value
    if (!el || props.detail) {
      isOverflow.value = false
      return
    }
    isOverflow.value = el.scrollHeight > el.clientHeight + 1
  }

  const handleContentRendered = () => {
    nextTick(() => {
      updateOverflowState()
    })
  }

  onMounted(() => {
    nextTick(() => {
      updateOverflowState()
    })
  })

  watch(
    () => props.detail,
    () => {
      nextTick(() => {
        updateOverflowState()
      })
    }
  )

  watch(
    () => props.posting.content,
    () => {
      nextTick(() => {
        updateOverflowState()
      })
    }
  )

  const modifyContents = async (postingId: number, action: number): Promise<void> => {
    try {
      console.log('begin post')

      const requestData = {
        path: props.data.path,
        courseId: props.data.course.id,
        postingId: postingId,
        action: action
      }

      const response = await contentServiceV1.operateContent(requestData)
      console.log(`response: ${JSON.stringify(response)}`)

      if (response.code === 200) {
        console.log('Form submitted successfully')
        showSnackbar && showSnackbar(t('posting.operationSuccess'))

        if (action === 1 || action === 2) {
          emit('loadData', ['contents', 'chosenPosting'])
        } else if (action === 3 || action === 4) {
          emit('loadData', ['contents', 'fixedPostings'])
        }
      }
    } catch (error) {
      // todo
      console.error('Error submitting form:', error)
    }
  }

  const upvote = async (posting: Post, type: VoteType): Promise<void> => {
    try {
      console.log('begin post')

      const response = await upvoteServiceV1.upvote(posting.id, 0, type)
      console.log(`response: ${JSON.stringify(response)}`)

      if (response.code === 200) {
        console.log('Form submitted successfully')
        // 更新帖子的点赞统计数据
        posting.twice = response.data.twiceUpvotes || 0
        posting.helpful = response.data.helpfulUpvotes || 0
        
        // 根据点赞状态设置 voteType
        if (response.data.twiceUpvoted) {
          posting.voteType = VoteType.TWICE
        } else if (response.data.helpfulUpvoted) {
          posting.voteType = VoteType.HELPFUL
        } else {
          posting.voteType = VoteType.NONE
        }
        
        if (posting.voteType === VoteType.NONE) posting.voteType = null

        // 如果是"看两遍就懂"(type=2)，只有在学习模式下才标记节点完成
        if (type === VoteType.TWICE && response.data.twiceUpvoted) {
          if (props.isLearning) {
            console.log('看两遍就懂被点击，用户在学习模式下，标记节点完成')
            emit('markNodeCompleted')
          } else {
            console.log('看两遍就懂被点击，但用户未在学习模式下，不标记节点完成')
          }
        }
      }
    } catch (error) {
      // todo
      console.error('Error submitting form:', error)
    }
  }

  /**
   * 处理"查看全部内容"按钮点击
   * 在切换到详情页面的同时记录浏览量
   */
  const handleViewFullContent = (): void => {
    // 记录查看全部内容的浏览量
    const success = postViewTracking.recordManualView(props.posting.id, 'view_full_content')

    if (success) {
      console.log(`[Posting] 记录"查看全部内容"浏览: Post ${props.posting.id}`)
    } else {
      console.log(`[Posting] 跳过重复浏览记录: Post ${props.posting.id}`)
    }

    // 切换到详情页面
    emit('switchTab', 'two', props.posting)
  }
</script>

<template>
  <!-- Post容器：添加data-post-id属性用于浏览量跟踪 -->
  <div :data-post-id="posting.id" class="posting-container">
    <v-row
      class="ma-0 pb-0 d-flex align-center bg-white"
      :class="[props.detail ? 'sticky-top detail-transform' : '']"
    >
      <v-btn
        v-if="props.detail"
        variant="flat"
        class="me-2"
        color=""
        density="comfortable"
        icon="mdi-chevron-left"
        @click="emit('switchTab', 'list', '')"
      ></v-btn>
      <v-avatar size="36" color="primary">
        <v-icon icon="mdi-account-circle" size="24" color="white"></v-icon>
      </v-avatar>
      <div class="pl-3">
        <UserCard 
          v-if="posting.creator"
          :id="posting.creator.id" 
          :name="posting.creator.name"
        ></UserCard>
        <div class="text-body-2 text-grey-darken-2 font-weight-medium">
          {{ posting.createdAt || '' }}
        </div>
      </div>
    </v-row>

    <v-row class="ma-0 pa-0 pt-3">
      <!-- is contents -->
      <template v-if="posting.type === PostType.CONTENTS">
        <div class="w-100 d-flex justify-space-between align-end">
          <v-list class="">
            <v-list-item
              v-for="(item, index) in posting.content.split(',')"
              :key="index"
              class="px-0 py-0"
              min-height="48"
            >
              <v-list-item-title class="pb-1 dashed-border">{{ item }}</v-list-item-title>
            </v-list-item>
          </v-list>
        </div>
      </template>

      <!-- is article -->
      <template v-else>
        <div ref="contentRef" :class="!props.detail ? 'text-limited' : ''">
          <RichContentRenderer
            class="tiptap pt-3 w-100"
            :class="props.detail ? 'full-article' : ''"
            :html="posting.content"
            @rendered="handleContentRendered"
          />
        </div>

        <v-btn
          v-if="isOverflow"
          variant="text"
          density="comfortable"
          class="px-0 mt-3 text-body-2 text-primary"
          @click="!props.detail ? handleViewFullContent() : ''"
        >
          <v-icon icon="mdi-chevron-down" size="16" class="mr-1"></v-icon>
          {{ t('posting.viewFullContent') }}
        </v-btn>
      </template>
    </v-row>

    <TextSelectionAI v-if="props.detail" />

    <v-row class="ma-0 pt-5 d-flex justify-space-between" align="center">
      <!-- is contents -->
      <template v-if="posting.type === PostType.CONTENTS">
        <v-btn
          :variant="posting.voteType === VoteType.HELPFUL ? 'flat' : 'flat'"
          rounded="lg"
          density="comfortable"
          :color="posting.voteType === VoteType.HELPFUL ? 'green-lighten-4' : 'grey-lighten-3'"
          class="px-4"
          @click="upvote(posting, VoteType.HELPFUL)"
        >
          <v-icon
            v-if="posting.voteType === VoteType.HELPFUL"
            icon="mdi-check"
            size="16"
            class="mr-2"
            color="green-darken-2"
          ></v-icon>
          <v-icon
            v-else
            icon="mdi-thumb-up-outline"
            size="16"
            class="mr-2"
            :color="(posting.voteType as VoteType) === VoteType.HELPFUL ? 'green-darken-2' : 'grey-darken-2'"
          ></v-icon>
          <span
            :class="
              posting.voteType === VoteType.HELPFUL
                ? 'font-weight-medium text-green-darken-2'
                : 'font-weight-medium text-grey-darken-2'
            "
            >{{ t('posting.agree') }} {{ posting.helpful || 0 }}</span
          >
        </v-btn>
      </template>

      <!-- is article -->
      <template v-else>
        <div class="d-flex">
          <v-btn
            :variant="posting.voteType === VoteType.TWICE ? 'flat' : 'flat'"
            rounded="lg"
            density="comfortable"
            :color="posting.voteType === VoteType.TWICE ? 'teal-lighten-4' : 'grey-lighten-3'"
            class="px-3"
            @click="upvote(posting, VoteType.TWICE)"
          >
            <v-icon
              v-if="posting.voteType === VoteType.TWICE"
              icon="mdi-check"
              size="14"
              class="mr-2"
              color="teal-darken-2"
            ></v-icon>
            <v-icon
              v-else
              icon="mdi-lightbulb-outline"
              size="14"
              class="mr-2"
              :color="(posting.voteType as VoteType)  === VoteType.TWICE ? 'teal-darken-2' : 'grey-darken-2'"
            ></v-icon>
            <span
              :class="
                posting.voteType === VoteType.TWICE
                  ? 'font-weight-medium text-teal-darken-3'
                  : 'font-weight-medium text-grey-darken-2'
              "
            >
              {{ t('posting.twiceUnderstand') }} {{ posting.twice || 0 }}
            </span>
          </v-btn>

          <v-btn
            :variant="posting.voteType === VoteType.HELPFUL ? 'flat' : 'flat'"
            rounded="lg"
            density="comfortable"
            :color="posting.voteType === VoteType.HELPFUL ? 'brown-lighten-4' : 'grey-lighten-3'"
            class="px-3 ms-3"
            @click="upvote(posting, VoteType.HELPFUL)"
          >
            <v-icon
              v-if="posting.voteType === VoteType.HELPFUL"
              icon="mdi-check"
              size="14"
              class="mr-2"
              color="brown-darken-2"
            ></v-icon>
            <v-icon
              v-else
              icon="mdi-thumb-up-outline"
              size="14"
              class="mr-2"
              :color="(posting.voteType as VoteType) === VoteType.HELPFUL ? 'brown-darken-2' : 'grey-darken-2'"
            ></v-icon>
            <span
              :class="
                posting.voteType === VoteType.HELPFUL
                  ? 'font-weight-medium text-brown-darken-2'
                  : 'font-weight-medium text-grey-darken-2'
              "
              >{{ t('posting.helpful') }} {{ posting.helpful || 0 }}</span
            >
          </v-btn>
        </div>
      </template>

      <v-spacer></v-spacer>

      <!-- Memory Card Deck Count Preview -->
      <v-tooltip location="top">
        <template v-slot:activator="{ props: tooltipProps }">
          <v-btn
            v-bind="tooltipProps"
            variant="text"
            color="grey-lighten-3"
            rounded="lg"
            density="comfortable"
            class="px-3"
          >
            <v-icon icon="mdi-cards-outline" size="14" class="mr-2" color="purple-darken-2"></v-icon>
            <span class="font-weight-medium text-purple-darken-2">
              {{ posting.deckCount || 0 }}
            </span>
          </v-btn>
        </template>
        <span>查看详情页以浏览和添加记忆卡片组</span>
      </v-tooltip>

      <v-btn
        v-if="!props.detail"
        variant="text"
        color="grey-lighten-3"
        rounded="lg"
        density="comfortable"
        class="px-3 ms-3"
        @click="emit('switchTab', 'two', posting)"
      >
        <v-icon icon="mdi-comment-outline" size="14" class="mr-2" color="grey-darken-2"></v-icon>
        <span class="font-weight-medium text-grey-darken-2">{{ posting.commentCount || 0 }}</span>
      </v-btn>

      <v-btn
        v-if="props.detail"
        variant="text"
        color="grey-lighten-3"
        rounded="lg"
        density="comfortable"
        class="px-3 ms-3"
        :ripple="false"
        @click.stop.prevent="null"
      >
        <v-icon icon="mdi-comment-outline" size="14" class="mr-2" color="grey-darken-2"></v-icon>
        <span class="font-weight-medium text-grey-darken-2">{{ posting.commentCount || 0 }}</span>
      </v-btn>

      <!-- is contents -->
      <template v-if="posting.type === PostType.CONTENTS">
        <v-btn
          variant="text"
          rounded="lg"
          :color="
            props.currNode['+'] && props.currNode['+'] === posting.id
              ? 'red-lighten-4'
              : 'grey-lighten-3'
          "
          density="comfortable"
          class="px-3 mx-3"
          @click="
            modifyContents(
              posting.id,
              props.currNode['+'] && props.currNode['+'] === posting.id ? 2 : 1
            )
          "
        >
          <v-icon
            :icon="
              props.currNode['+'] && props.currNode['+'] === posting.id
                ? 'mdi-playlist-remove'
                : 'mdi-playlist-plus'
            "
            size="14"
            class="mr-2"
            :color="
              props.currNode['+'] && props.currNode['+'] === posting.id
                ? 'red-darken-2'
                : 'grey-darken-2'
            "
          ></v-icon>
          <span
            :class="
              props.currNode['+'] && props.currNode['+'] === posting.id
                ? 'font-weight-medium text-red-darken-2'
                : 'font-weight-medium text-grey-darken-2'
            "
          >
            <template v-if="props.currNode['+'] && props.currNode['+'] === posting.id">
              {{ t('posting.cancelSetAsCatalog') }}
            </template>
            <template v-else>
              {{ t('posting.setAsCatalog') }}
            </template>
          </span>
        </v-btn>
      </template>

      <!-- is article -->
      <template v-else>
        <v-btn
          variant="text"
          rounded="lg"
          :color="
            props.currNode['^'] && props.currNode['^'].includes(posting.id)
              ? 'orange-lighten-4'
              : 'grey-lighten-3'
          "
          density="comfortable"
          class="px-3 ms-3"
          @click="
            modifyContents(
              posting.id,
              props.currNode['^'] && props.currNode['^'].includes(posting.id) ? 4 : 3
            )
          "
        >
          <v-icon
            :icon="
              props.currNode['^'] && props.currNode['^'].includes(posting.id)
                ? 'mdi-arrow-collapse-down'
                : 'mdi-arrow-collapse-up'
            "
            size="14"
            class="mr-2"
            :color="
              props.currNode['^'] && props.currNode['^'].includes(posting.id)
                ? 'orange-darken-2'
                : 'grey-darken-2'
            "
          ></v-icon>
          <span
            :class="
              props.currNode['^'] && props.currNode['^'].includes(posting.id)
                ? 'font-weight-medium text-orange-darken-2'
                : 'font-weight-medium text-grey-darken-2'
            "
          >
            <template v-if="props.currNode['^'] && props.currNode['^'].includes(posting.id)">
              {{ t('posting.unpin') }}
            </template>
            <template v-else>
              {{ t('posting.pin') }}
            </template>
          </span>
        </v-btn>
      </template>
    </v-row>
  </div>
</template>

<style lang="scss">
  @use '@/styles/style.scss' as *;

  .bg-white {
    background-color: white;
  }

  .dashed-border {
    border-bottom: 1px dashed #ddd;
  }

  .detail-transform {
    transform: translateX(-37px);
    width: 110%;
  }

  .text-limited {
    max-height: 800px;
    overflow: hidden;
    -webkit-mask-image: linear-gradient(to bottom, black 85%, transparent 100%);
    mask-image: linear-gradient(to bottom, black 85%, transparent 100%);
  }
  .tiptap {
    :first-child {
      margin-top: 0;
    }

    /* List styles */
    ul,
    ol {
      padding: 0 1rem;
      margin: 1.25rem 1rem 1.25rem 0.4rem;

      li p {
        margin-top: 0.25em;
        margin-bottom: 0.25em;
      }
    }

    /* Heading styles */
    h1,
    h2,
    h3,
    h4,
    h5,
    h6 {
      line-height: 1.1;
      margin-top: 2.5rem;
      text-wrap: pretty;
      font-weight: bold;
    }

    h1,
    h2 {
      margin-top: 3.5rem;
      margin-bottom: 1.5rem;
    }

    h1 {
      font-size: 1.4rem;
    }

    h2 {
      font-size: 1.2rem;
    }

    h3 {
      font-size: 1.1rem;
    }

    h4,
    h5,
    h6 {
      font-size: 1rem;
    }

    /* Code and preformatted text styles */
    code {
      background-color: var(--purple-light);
      border-radius: 0.4rem;
      color: var(--black);
      font-size: 0.85rem;
      padding: 0.25em 0.3em;
    }

    pre {
      background: var(--black);
      border-radius: 0.5rem;
      color: var(--white);
      font-family: 'JetBrainsMono', monospace;
      margin: 1.5rem 0;
      padding: 0.75rem 1rem;
      overflow-x: scroll;
      width: 100%;

      code {
        background: none;
        color: inherit;
        font-size: 0.8rem;
        padding: 0;
      }

      /* Code styling */
      .hljs-comment,
      .hljs-quote {
        color: #616161;
      }

      .hljs-variable,
      .hljs-template-variable,
      .hljs-attribute,
      .hljs-tag,
      .hljs-name,
      .hljs-regexp,
      .hljs-link,
      .hljs-name,
      .hljs-selector-id,
      .hljs-selector-class {
        color: #f98181;
      }

      .hljs-number,
      .hljs-meta,
      .hljs-built_in,
      .hljs-builtin-name,
      .hljs-literal,
      .hljs-type,
      .hljs-params {
        color: #fbbc88;
      }

      .hljs-string,
      .hljs-symbol,
      .hljs-bullet {
        color: #b9f18d;
      }

      .hljs-title,
      .hljs-section {
        color: #faf594;
      }

      .hljs-keyword,
      .hljs-selector-tag {
        color: #70cff8;
      }

      .hljs-emphasis {
        font-style: italic;
      }

      .hljs-strong {
        font-weight: 700;
      }
    }

    blockquote {
      border-left: 3px solid var(--gray-3);
      margin: 1.5rem 0;
      padding-left: 1rem;
    }

    hr {
      border: none;
      border-top: 1px solid var(--gray-2);
      cursor: pointer;
      margin: 2rem 0;

      &.ProseMirror-selectednode {
        border-top: 1px solid var(--purple);
      }
    }

    mark {
      background-color: #faf594;
      border-radius: 0.4rem;
      box-decoration-break: clone;
      padding: 0.1rem 0.3rem;
    }

    a {
      color: var(--purple);
      cursor: pointer;

      &:hover {
        color: var(--purple-contrast);
      }
    }

    img {
      display: block;
      height: auto;
      margin: 1.5rem 0;
      max-width: 100%;

      &.ProseMirror-selectednode {
        outline: 3px solid var(--purple);
      }
    }

    table {
      border-collapse: collapse;
      margin: 0;
      overflow: hidden;
      table-layout: fixed;
      width: 100%;

      td,
      th {
        border: 1px solid var(--gray-3);
        box-sizing: border-box;
        min-width: 1em;
        padding: 6px 8px;
        position: relative;
        vertical-align: top;

        > * {
          margin-bottom: 0;
        }
      }

      th {
        background-color: var(--gray-1);
        font-weight: bold;
        text-align: left;
      }

      .selectedCell:after {
        background: var(--gray-2);
        content: '';
        left: 0;
        right: 0;
        top: 0;
        bottom: 0;
        pointer-events: none;
        position: absolute;
        z-index: 2;
      }

      .column-resize-handle {
        background-color: var(--purple);
        bottom: -2px;
        pointer-events: none;
        position: absolute;
        right: -2px;
        top: 0;
        width: 4px;
      }
    }

    .tableWrapper {
      margin: 1.5rem 0;
      overflow-x: auto;
    }

    &.resize-cursor {
      cursor: ew-resize;
      cursor: col-resize;
    }

    p.is-editor-empty:first-child::before {
      color: var(--gray-4);
      content: attr(data-placeholder);
      float: left;
      height: 0;
      pointer-events: none;
    }

    .Tiptap-mathematics-editor {
      background: #202020;
      color: #fff;
      font-family: monospace;
      padding: 0.2rem 0.5rem;
    }

    .Tiptap-mathematics-render {
      padding: 0 0.25rem;

      &--editable {
        cursor: pointer;
        transition: background 0.2s;

        &:hover {
          background: #eee;
        }
      }
    }

    .Tiptap-mathematics-editor,
    .Tiptap-mathematics-render {
      border-radius: 0.25rem;
      display: inline-block;
    }

    [aria-hidden='true'] {
      display: none;
    }
  }
</style>