<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import type { Ref } from 'vue'
import { postServiceV1, userServiceV1 } from '@/services/api/v1/apiServiceV1'
import UserPosting from '@/components/user/UserPosting.vue'
import Comment from '../read/CommentArea.vue'
import Tiptap from '../read/TiptapInput.vue'
import { useUserStore } from '@/stores/user'
import type { Post } from '@/types/post'
import { ObjectType, PostType } from '@/types/enums'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'


interface TiptapRef {
  editor: {
    getHTML(): string
  }
}

interface LoadEventData {
  done: (status: 'ok' | 'empty') => void
}

// Props
const props = defineProps<{
  userId?: number
}>()

const userStore = useUserStore()

// 当前操作的用户ID
const targetUserId = computed(() => props.userId || userStore.currentUser?.id)

// 是否查看自己
const isSelf = computed(() => targetUserId.value === userStore.currentUser?.id)

// 内部状态管理
const mainArea: Ref<'list' | 'detail' | 'edit'> = ref('list')
const currPosting: Ref<Post | null> = ref(null)
const scrollPosition: Ref<number> = ref(0)
const lastPage: Ref<string> = ref('')
const editorRef: Ref<TiptapRef | null> = ref(null)

// 使用 useInfiniteScroll 加载目录列表
const {
  items: contentsList,
  loadMore: loadMoreContents,
  hasMore,
} = useInfiniteScroll<Post>({
  fetchFn: (params) => {
    return isSelf.value
      ? userServiceV1.getCurrentUserAllPosts(params.lastId, PostType.CONTENTS)
      : userServiceV1.getUserPosts(targetUserId.value, params.lastId, PostType.CONTENTS)
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: { lastId: 0x7fffffff },
})

// 适配 v-infinite-scroll 的 load 事件
const loadContents = async ({ done }: LoadEventData): Promise<void> => {
  await loadMoreContents()
  done(hasMore.value ? 'ok' : 'empty')
}

// 内部处理区域切换
const switchMainArea = (value: string, post?: Post | null): void => {
  lastPage.value = mainArea.value
  if (value === 'list') {
    mainArea.value = 'list'
    nextTick(() => {
      window.scrollTo(0, scrollPosition.value)
    })
  } else if (value === 'edit') {
    mainArea.value = 'edit'
    currPosting.value = post || null
    scrollPosition.value = window.scrollY
    window.scrollTo(0, 0)
  } else if (value === 'detail') {
    mainArea.value = 'detail'
    currPosting.value = post || null
    scrollPosition.value = window.scrollY
    window.scrollTo(0, 0)
  }
}

const switchToLastPage = (): void => {
  console.log(`lastPage: ${lastPage.value}`)
  switchMainArea(lastPage.value, currPosting.value)
}

// 使用 useMutation 修改文章
const { execute: updatePost } = useMutation(
  (data: { id: number; content: string }) => postServiceV1.updatePost(data.id, { content: data.content }),
  {
    onSuccess: (response) => {
      if (currPosting.value && response) {
        currPosting.value.content = response.content
        currPosting.value.state = response.state
      }
      switchToLastPage()
    },
  },
)

// 修改文章
const modifyPosting = async (): Promise<void> => {
  if (!currPosting.value || !editorRef.value) return

  await updatePost({
    id: currPosting.value.id,
    content: editorRef.value.editor.getHTML(),
  })
}

// 处理删除目录
const handleDeletePosting = (id: string | number): void => {
  contentsList.value = contentsList.value && contentsList.value.filter((item) => item.id !== id)
}
</script>

<template>
  <div>
    <!-- 列表视图 -->
    <div v-if="mainArea == 'list'">
      <v-infinite-scroll
        key="contents"
        :items="contentsList"
        @load="loadContents"
        no-more-text="已经到底了"
        class="infinite-scroll-offset"
      >
        <div v-for="(posting, index) in contentsList" :key="posting.id">
          <v-row class="ma-0 border-b px-0 pb-6" :class="{ 'pt-9': index != 0 }">
            <div class="w-100 pb-8 d-flex justify-space-between align-end text-grey">
              <div class="d-flex align-center text-body-1">
                <a
                  class="text-grey pe-2"
                  :href="'/read?courseId=' + posting.node.course.id"
                  target="_blank"
                >
                  {{ posting.node.course.name }}
                </a>
                <v-icon icon="mdi-chevron-right" class="px-2 pe-2 text-body-1"></v-icon>
                <a
                  class="text-grey ps-2"
                  :href="'/read?courseId=' + posting.node.course.id + '&nodeId=' + posting.node.id"
                  target="_blank"
                >
                  {{ posting.node.name }}
                </a>
              </div>
            </div>

            <UserPosting
              :posting="posting"
              type="list"
              @switch-main-area="switchMainArea"
              @delete-posting="handleDeletePosting"
            >
            </UserPosting>
          </v-row>
        </div>

        <template #empty>
          <div class="text-body-2 text-grey py-5">已经到底了</div>
        </template>
      </v-infinite-scroll>
    </div>

    <!-- 详情视图 -->
    <div v-if="mainArea == 'detail'">
      <UserPosting
        :posting="currPosting"
        type="detail"
        @switch-main-area="switchMainArea"
        @delete-posting="handleDeletePosting"
      >
      </UserPosting>
      <v-row class="pa-0 ma-0 my-7">
        <Comment :object="currPosting as any" :type="ObjectType.POST"></Comment>
      </v-row>
    </div>

    <!-- 编辑视图 -->
    <div v-if="mainArea == 'edit'">
      <v-row class="mx-0 sticky-top mb-1 edit-header" align="center">
        <v-btn
          variant="flat"
          class="me-0"
          color=""
          density="comfortable"
          icon="mdi-chevron-left"
          @click="switchToLastPage"
        ></v-btn>
        <span class="ps-1 font-weight-bold text-body-1">修改文章</span>
      </v-row>
      <Tiptap ref="editorRef" path-text="" :content="currPosting?.content || ''" />
      <div class="pt-1 pb-2 px-0 sticky-bottom">
        <v-btn
          variant="tonal"
          color="teal"
          size="large"
          class="rounded-lg"
          block
          @click="modifyPosting"
          >写好了，提交</v-btn
        >
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.sticky-top) {
  position: sticky;
  top: 49px;
  z-index: 10;
  height: 3.8vh;
  overflow-y: auto;
}

.v-infinite-scroll__side {
  display: none !important;
}

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.infinite-scroll-offset {
  position: relative;
  top: -12px;
}

.edit-header {
  background-color: white;
  transform: translateX(-38px);
  width: 110%;
}

.sticky-bottom {
  position: sticky;
  bottom: 0px;
  background-color: #fff;
}
</style>