<script setup lang="ts">
  // TODO: 修复 props 直接修改问题 - 需要重构为通过 emit 事件与父组件通信
  // 当前直接修改 props.data 违反 Vue 规范，应该通过事件通知父组件更新数据
  import { computed, inject, nextTick, onMounted, onUnmounted, ref, toRef, watch } from 'vue'
  import { postServiceV1, progressServiceV1 } from '@/services/api/v1/apiServiceV1'
  import { useRoute, useRouter } from 'vue-router'
  import { useI18n } from 'vue-i18n'
  import { ObjectType, PostType } from '@/types/enums'
  import type { Post } from '@/types/post'
  import type { Course } from '@/types/course'
  import type { Node } from '@/types/node'

  import AddContents from '../course/AddContents.vue'
  import AddArticle from './AddArticle.vue'
  import InviteUser from '../user/InviteUser.vue'
  import CommentArea from './CommentArea.vue'
  import SinglePost from './SinglePost.vue'
  import TiptapInput from './TiptapInput.vue'

  // 导入Post浏览量跟踪服务
  import postViewTracking from '@/services/postViewTracking'

  const route = useRoute()
  const router = useRouter()
  const { t } = useI18n()

  const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

  interface TocNodeInfo {
    name: string
    isCompleted: boolean
  }

  interface DataProps {
    node: Node
    course: Course
    tocNodeInfos: Record<string, TocNodeInfo>
    chosenPosting?: Post
    fixedPostings: Post[]
    otherPostings: Post[]
    post?: Post
    path: string
  }

  interface NextNodeInfo {
    name: string
    path: string
  }

  interface EditorRef {
    editor: {
      getHTML(): string
    }
  }

  interface Props {
    data: DataProps
    nodes: number[]
    currNodeId: number
    currNode: Record<string, any>
    pathText: string
    getNextNodeInfo: () => NextNodeInfo | null
    isLearning?: boolean
  }

  interface Emits {
    (e: 'load-data', data: any[]): void
    (e: 'node-completed', nodeId: number): void
    (e: 'start-learning'): void
    (e: 'loadData', data: any[]): void
  }

  const emit = defineEmits<Emits>()
  const props = withDefaults(defineProps<Props>(), {
    isLearning: false,
  })

  const lastPostingId = ref<number>(0)
  const lastScore = ref<number>(0)
  const createContentsDialog = ref<boolean>(false)
  const createArticleDialog = ref<boolean>(false)

  const inviteDialog = ref<boolean>(false)
  const tab = ref<string>('list')
  const scrollPosition = ref<number>(0)
  const scrollKey = ref<number>(0)
  const editorRef = ref<EditorRef | null>(null)

  const currPosting = ref<Post | null>(null)
  const nextNodeDialog = ref<boolean>(false)
  const nextNodeInfo = ref<NextNodeInfo | null>(null)

  // 计算当前节点是否有子节点
  const hasSubNodes = computed((): boolean => {
    if (!props.currNode || typeof props.currNode !== 'object') {
      return false
    }

    // 过滤掉特殊键 '^'，检查是否有其他子节点
    const childKeys = Object.keys(props.currNode).filter((key) => key !== '^')
    return childKeys.length > 0
  })

  onMounted(() => {
    window.addEventListener('popstate', restoreScrollPosition)

    // 以下处理路径直接定位到文章页或者评论的情况

    if ('tab' in route.query && route.query.tab === 'comment') {
      tab.value = 'comment'
    }

    if (props.data.post !== null && props.data.post !== undefined) {
      switchTab('detail', dataRef.value.post)
    }

    if (!('post' in props.data) && 'commentId' in route.query) {
      switchTab('comment', '')
    }

    // 🔴 初始化Post浏览量跟踪
    initializePostViewTracking()
  })

  /**
   * 初始化Post浏览量跟踪功能
   * 为当前页面上的所有posts设置浏览量统计
   */
  const initializePostViewTracking = (): void => {
    // 等待DOM渲染完成后开始跟踪
    nextTick(() => {
      // 自动扫描并开始跟踪页面上的posts
      postViewTracking.autoObserve()

      // 在开发环境下输出跟踪状态（用于调试）
      if (import.meta.env.DEV) {
        postViewTracking.getStatus()
      }
    })
  }

  /**
   * 组件卸载时的清理工作
   */
  onUnmounted(() => {
    // 提交剩余的浏览记录
    postViewTracking.flush()
  })

  const restoreScrollPosition = (): void => {
    // // console.log('xx')
  }

  const dataRef = toRef(props, 'data')

  watch(dataRef, () => {
    scrollKey.value++
    updateLastPostingId()

    // 🔴 数据更新后重新扫描新的posts进行跟踪
    handleDataUpdate()
  })

  /**
   * 处理数据更新
   * 当posts数据发生变化时（如加载更多），重新扫描新的posts
   */
  const handleDataUpdate = (): void => {
    // 等待DOM更新完成后重新扫描
    nextTick(() => {
      // 重新扫描并跟踪新加载的posts
      postViewTracking.autoObserve()

      // 在开发环境下输出更新后的状态
      if (import.meta.env.DEV) {
        postViewTracking.getStatus()
      }
    })
  }

  watch(
    () => route.fullPath,
    () => {
      tab.value = 'list'
    }
  )

  const loadData = (parts: any[]): void => {
    emit('loadData', parts)
  }

  const updateLastPostingId = (): void => {
    const { otherPostings } = props.data
    if (otherPostings.length > 0) {
      lastPostingId.value = otherPostings[otherPostings.length - 1].id
      lastScore.value = otherPostings[otherPostings.length - 1].score || 0
    }
  }

  // 初始化调用，放在函数定义之后
  updateLastPostingId()

  const switchTab = (tabName: string, posting: Post | string): void => {
    tab.value = tabName // 切换到指定 Tab

    if (tabName === 'list') {
      nextTick(() => {
        window.scrollTo(0, scrollPosition.value)
      })
    } else {
      scrollPosition.value = window.scrollY
      window.scrollTo(0, 0)
      if (typeof posting === 'object') {
        currPosting.value = posting
      }
    }
  }

  interface LoadMoreCallback {
    (status: 'ok' | 'empty'): void
  }

  const loadMore = async ({ done }: { done: LoadMoreCallback }): Promise<void> => {
    try {
      const response = await postServiceV1.getPosts(
        undefined,
        props.currNodeId,
        lastScore.value,
        lastPostingId.value
      )

      if (response.code === 200) {
        response.data.forEach((posting: Post) => {
          if (posting.voteType === 0) {
            posting.voteType = null
          }
        })
        // eslint-disable-next-line vue/no-mutating-props
        props.data.otherPostings.push(...response.data)

        if (response.data.length > 0) {
          lastPostingId.value = response.data[response.data.length - 1].id
          lastScore.value = response.data[response.data.length - 1].score || 0
          done('ok')
        } else {
          done('empty')
        }
      }
    } catch {
      // todo
    }
  }

  const submitAddArticle = async (): Promise<void> => {
    try {
      if (!editorRef.value) return

      const data = {
        content: editorRef.value.editor.getHTML(),
        nodeId: props.currNodeId,
        type: PostType.ARTICLE,
      }

      const response = await postServiceV1.createPost(data)

      if (response.code === 200) {
        emit('load-data', [])
        createArticleDialog.value = false
      }
    } catch {
      // todo
    }
  }

  // 切换节点完成状态
  const toggleNodeCompletion = async (): Promise<void> => {
    // 检查是否在学习模式下
    if (!props.isLearning) {
      // 询问用户是否开始学习
      // TODO: 实现更安全的确认方式
      const confirmed = true // 暂时默认确认
      if (!confirmed) {
        return // 用户取消
      }

      // 用户确认，先开始学习
      try {
        // 这里需要调用开始学习的方法，通过事件通知父组件
        emit('start-learning')
      } catch {
        showSnackbar && showSnackbar(t('postingList.startLearningFailed'), 'error')
        return
      }
    }

    try {
      if (props.data.node.isCompleted) {
        // 取消完成
        const response = await progressServiceV1.unmarkNodeComplete(props.currNodeId, props.data.course.id)

        if (response.code === 200) {
          // eslint-disable-next-line vue/no-mutating-props
          props.data.node.isCompleted = false

          // 同时更新 tocNodeInfos 中的节点状态
          if (props.data.tocNodeInfos && props.data.tocNodeInfos[props.currNodeId]) {
            // eslint-disable-next-line vue/no-mutating-props
            props.data.tocNodeInfos[props.currNodeId].isCompleted = false
          }

          // 更新课程进度（后端返回的是 progress*100 的值，直接使用）
          if (response.data && response.data.courseProgress !== undefined) {
            // eslint-disable-next-line vue/no-mutating-props
            props.data.course.progress = response.data.courseProgress
          }

          showSnackbar && showSnackbar(t('postingList.completionCancelled'), 'info')
        } else {
          showSnackbar && showSnackbar(t('postingList.cancelCompletionFailed'), 'error')
        }
      } else {
        // 标记完成
        const response = await progressServiceV1.markNodeComplete(
          props.currNodeId,
          props.data.course.id
        )

        if (response.code === 200) {
          // eslint-disable-next-line vue/no-mutating-props
          props.data.node.isCompleted = true

          // 同时更新 tocNodeInfos 中的节点状态
          if (props.data.tocNodeInfos && props.data.tocNodeInfos[props.currNodeId]) {
            // eslint-disable-next-line vue/no-mutating-props
            props.data.tocNodeInfos[props.currNodeId].isCompleted = true
          }

          // 更新课程进度（后端返回的是 progress*100 的值，直接使用）
          if (response.data && response.data.courseProgress !== undefined) {
            // eslint-disable-next-line vue/no-mutating-props
            props.data.course.progress = response.data.courseProgress
          }

          showSnackbar && showSnackbar(t('postingList.nodeCompleted'), 'success')

          // 通知父组件节点已完成，触发课程完成检查
          emit('node-completed', props.currNodeId)

          // 检查是否有下一个节点
          checkNextNode()
        } else {
          showSnackbar && showSnackbar(t('postingList.markCompletionFailed'), 'error')
        }
      }
    } catch {
      showSnackbar && showSnackbar(t('postingList.operationFailed'), 'error')
    }
  }

  // 检查下一个节点并弹出确认对话框
  const checkNextNode = (): void => {
    try {
      if (props.getNextNodeInfo) {
        const nextNode = props.getNextNodeInfo()
        if (nextNode) {
          nextNodeInfo.value = nextNode
          nextNodeDialog.value = true
        }
      }
    } catch {
      // console.error('Error checking next node:', error)
    }
  }

  // 跳转到下一个节点
  const goToNextNode = (): void => {
    if (nextNodeInfo.value && nextNodeInfo.value.path) {
      const { courseId } = route.query
      const nextPath = nextNodeInfo.value.path
      const url = `/read?courseId=${courseId}&path=${nextPath}`
      router.push(url)
    }
    nextNodeDialog.value = false
  }

  // 处理从Posting组件发出的markNodeCompleted事件
  const handleMarkNodeCompleted = async (): Promise<void> => {
    // 检查当前节点是否有子节点，有子节点的不能标记完成
    if (hasSubNodes.value) {
      return
    }

    if (props.data.node.isCompleted) {
      // 如果已经完成了，不需要重复执行
      return
    }

    try {
      // 标记完成
      const response = await progressServiceV1.markNodeComplete(
        props.currNodeId,
        props.data.course.id
      )

      if (response.code === 200) {
        // eslint-disable-next-line vue/no-mutating-props
        props.data.node.isCompleted = true
        showSnackbar && showSnackbar(t('postingList.nodeCompletedTwice'), 'success')

        // 检查是否有下一个节点
        checkNextNode()
      } else {
        showSnackbar && showSnackbar('标记完成失败，请重试', 'error')
      }
    } catch {
      showSnackbar && showSnackbar(t('postingList.operationFailed'), 'error')
    }
  }
</script>

<template>
  <template
    v-if="(tab == 'list' || tab == 'addArticle' || tab == 'comment') && data && data.tocNodeInfos"
  >
    <v-row class="ma-0 text-grey text-body-2 pb-2">
      <div v-if="!('nodeId' in route.query)" class="d-flex align-center">
        <template v-for="item in nodes" :key="item">
          <div class="d-flex align-center">
            {{ data.tocNodeInfos?.[item]?.name || item }}
            <v-icon icon="mdi-chevron-right" class="px-5"></v-icon>
          </div>
        </template>
      </div>
      <div v-else>
        <div class="d-flex align-center">
          {{ t('postingList.courseNode') }}
          <v-icon icon="mdi-chevron-right" class="px-4"></v-icon>
        </div>
      </div>
    </v-row>

    <div class="px-0 pb-1 pt-4 ma-0 mb-0 sticky-header">
      <div class="d-flex align-center justify-space-between mb-2">
        <div class="d-flex align-center">
          <v-icon icon="mdi-list-box-outline" color="primary-darken-1" size="24"></v-icon>
          <h2 class="text-h5 font-weight-bold text-grey-darken-4 ms-3">{{ data.node.name }}</h2>
        </div>
        <v-btn
          v-if="isLearning && !hasSubNodes"
          :color="data.node.isCompleted ? 'grey-lighten-2' : 'success'"
          :variant="data.node.isCompleted ? 'outlined' : 'flat'"
          rounded="lg"
          size="small"
          class="px-4"
          :prepend-icon="data.node.isCompleted ? 'mdi-check-circle' : 'mdi-circle-outline'"
          @click="toggleNodeCompletion"
        >
          <span
            class="font-weight-medium"
            :class="data.node.isCompleted ? 'text-grey-darken-2' : 'text-white'"
          >
            {{
              data.node.isCompleted ? t('postingList.completed') : t('postingList.completeStudy')
            }}
          </span>
        </v-btn>
      </div>
    </div>
    <p class="text-body-1 text-grey-darken-2 mb-0">{{ data.node.description }}</p>
    <v-row class="mt-8 mb-0 mx-0 justify-space-between">
      <div>
        <v-tabs density="compact" class="">
          <v-tab class="px-3" @click="switchTab('list', '')">
            <v-icon icon="mdi-list-box-outline" size="16" class="mr-2"></v-icon>
            <span class="font-weight-medium text-grey-darken-3">{{
              t('postingList.articleList')
            }}</span>
          </v-tab>
          <v-tab class="px-3" @click="switchTab('comment', '')">
            <v-icon icon="mdi-comment-outline" size="16" class="mr-2"></v-icon>
            <span class="font-weight-medium text-grey-darken-3"
              >{{ data.node.commentCount }} {{ t('postingList.comments') }}</span
            >
          </v-tab>
        </v-tabs>
      </div>
      <div class="d-flex align-center">
        <v-btn
          variant="flat"
          color="grey-lighten-4"
          rounded="lg"
          class="px-3 me-2"
          density="comfortable"
          @click="createContentsDialog = true"
        >
          <v-icon
            icon="mdi-format-list-group-plus"
            size="14"
            class="mr-2"
            color="grey-darken-3"
          ></v-icon>
          <span class="font-weight-medium text-grey-darken-3">{{
            t('postingList.addContent')
          }}</span>
        </v-btn>
        <AddContents
          v-model="createContentsDialog"
          :node-id="props.currNodeId"
          :path-text="props.pathText"
          @load-data="loadData"
        ></AddContents>

        <v-btn
          variant="flat"
          color="grey-lighten-4"
          rounded="lg"
          density="comfortable"
          class="px-3 me-2"
          @click="createArticleDialog = true"
        >
          <v-icon
            icon="mdi-note-plus-outline"
            size="14"
            class="mr-2"
            color="grey-darken-3"
          ></v-icon>
          <span class="font-weight-medium text-grey-darken-3">{{
            t('postingList.addArticle')
          }}</span>
        </v-btn>
        <AddArticle
          v-model="createArticleDialog"
          :node-id="props.currNodeId"
          :path-text="props.pathText"
          @load-data="loadData"
        ></AddArticle>

        <v-btn
          variant="flat"
          color="grey-lighten-4"
          rounded="lg"
          class="px-3"
          density="comfortable"
          @click="inviteDialog = true"
        >
          <v-icon
            icon="mdi-account-plus-outline"
            size="14"
            class="mr-2"
            color="grey-darken-3"
          ></v-icon>
          <span class="font-weight-medium text-grey-darken-3">{{
            t('postingList.inviteAnswer')
          }}</span>
        </v-btn>
        <InviteUser v-model="inviteDialog" :node-id="props.currNodeId"></InviteUser>
      </div>
    </v-row>
  </template>

  <!-- list -->
  <template v-if="tab === 'list'">
    <div>
      <div v-if="data.chosenPosting" class="pt-8">
        <SinglePost
          :posting="data.chosenPosting"
          :curr-node="currNode"
          :data="data"
          :is-learning="isLearning"
          @load-data="loadData"
          @switch-tab="switchTab"
          @mark-node-completed="handleMarkNodeCompleted"
        >
        </SinglePost>
        <v-divider class="mt-11" color="grey-darken-2"></v-divider>
      </div>

      <div v-for="(posting, key) in data.fixedPostings" :key="key" class="pt-8">
        <SinglePost
          :posting="posting"
          :curr-node="currNode"
          :data="data"
          :is-learning="isLearning"
          @load-data="loadData"
          @switch-tab="switchTab"
          @mark-node-completed="handleMarkNodeCompleted"
        ></SinglePost>
        <v-divider class="mt-11" color="grey-darken-2"></v-divider>
      </div>

      <v-infinite-scroll
        :key="scrollKey"
        :items="data.otherPostings"
        @load="loadMore"
        :no-more-text="t('postingList.reachedEnd')"
        class=""
      >
        <div
          v-for="(posting, index) in data.otherPostings"
          v-show="
            !(
              (currNode['+'] && currNode['+'] == posting.id) ||
              (currNode['^'] && currNode['^'].includes(posting.id))
            )
          "
          :key="`posting-${posting.id}-${index}`"
          :class="index == 0 ? 'pt-4' : 'pt-8'"
        >
          <SinglePost
            :posting="posting"
            :curr-node="currNode"
            :data="data"
            :is-learning="isLearning"
            @load-data="loadData"
            @switch-tab="switchTab"
            @mark-node-completed="handleMarkNodeCompleted"
          ></SinglePost>
          <v-divider class="mt-11" color="grey-darken-2"></v-divider>
        </div>
        <template #empty>
          <div class="text-body-2 text-grey py-8">- {{ t('postingList.reachedEnd') }} -</div>
        </template>
      </v-infinite-scroll>
    </div>
  </template>

  <template v-else-if="tab === 'addArticle'">
    <div>
      <TiptapInput ref="editorRef" :path-text="props.pathText" />
      <div class="pt-1 pb-2 px-0 sticky-footer">
        <v-btn
          variant="flat"
          color="grey-darken-2"
          size="large"
          class="rounded-lg"
          block
          @click="submitAddArticle"
        >
          <span class="text-white">{{ t('postingList.submitArticle') }}</span>
        </v-btn>
      </div>
    </div>
  </template>

  <template v-else-if="tab === 'comment'">
    <v-row class="pa-0 ma-0 my-8">
      <CommentArea 
        :object="{ id: currNodeId, commentCount: data.node.commentCount }" 
        :type=ObjectType.NODE
      ></CommentArea>
    </v-row>
  </template>

  <!-- detail -->
  <template v-else>
    <SinglePost
      :data="props.data"
      :posting="currPosting"
      :curr-node="currNode"
      :detail="true"
      :is-learning="isLearning"
      @load-data="loadData"
      @switch-tab="switchTab"
      @mark-node-completed="handleMarkNodeCompleted"
    >
    </SinglePost>

    <v-row class="pa-0 ma-0 my-5">
      <CommentArea 
        v-if="currPosting" 
        :object="{ id: currPosting.id, commentCount: currPosting.commentCount || 0 }" 
        :type=ObjectType.POST
      ></CommentArea>
    </v-row>
  </template>

  <!-- 跳转下一节点确认对话框 -->
  <v-dialog v-model="nextNodeDialog" width="400" persistent>
    <v-card rounded="xl" elevation="8">
      <!-- 头部 -->
      <div class="d-flex align-center justify-space-between pa-6 pb-4">
        <div class="d-flex align-center">
          <div class="pa-3 rounded-lg bg-success-lighten-5 mr-3">
            <v-icon icon="mdi-arrow-right-circle" color="success-darken-1" size="20"></v-icon>
          </div>
          <div>
            <h3 class="text-h6 font-weight-bold text-grey-darken-3">
              {{ t('postingList.studyCompleted') }}
            </h3>
            <p class="text-body-2 text-grey-darken-1 mb-0">
              {{ t('postingList.congratulations') }}
            </p>
          </div>
        </div>
      </div>

      <v-divider></v-divider>

      <!-- 内容 -->
      <v-card-text class="pa-6">
        <p class="text-body-1 mb-4">
          {{ t('postingList.studyCompletedMessage') }}
        </p>
        <div v-if="nextNodeInfo" class="d-flex align-center pa-4 bg-grey-lighten-5 rounded-lg">
          <v-icon icon="mdi-book-open-page-variant" color="primary" class="mr-3"></v-icon>
          <div>
            <div class="font-weight-medium text-grey-darken-3">{{ t('postingList.nextNode') }}</div>
            <div class="text-body-2 text-primary">{{ nextNodeInfo.name }}</div>
          </div>
        </div>
      </v-card-text>

      <!-- 底部操作按钮 -->
      <v-card-actions class="pa-6 pt-0">
        <v-spacer></v-spacer>
        <v-btn variant="outlined" rounded="lg" class="mr-3" @click="nextNodeDialog = false">
          {{ t('postingList.studyLater') }}
        </v-btn>
        <v-btn color="success" variant="flat" rounded="lg" @click="goToNextNode">
          <v-icon icon="mdi-arrow-right" size="16" class="mr-2"></v-icon>
          {{ t('postingList.continueStudy') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
  :deep(.sticky-top) {
    position: sticky;
    top: 0px;
    z-index: 10;
    height: 3.8vh;
    overflow-y: auto;
  }

  .sticky-header {
    position: sticky;
    top: 0px;
    background-color: #fff;
    z-index: 1;
  }

  .sticky-footer {
    position: sticky;
    bottom: 0px;
    background-color: #fff;
  }

  .custom-btn-toggle .v-btn:not(.v-btn--variant-elevated) {
    color: #000;
    /* 修改未选中按钮的字体颜色 */
  }
</style>