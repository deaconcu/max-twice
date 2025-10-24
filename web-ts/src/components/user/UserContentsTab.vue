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
const targetUserId = computed(() => props.userId || userStore.userId)

// 是否查看自己
const isSelf = computed(() => targetUserId.value === userStore.userId)

// 内部状态管理
const mainArea: Ref<'list' | 'detail' | 'edit'> = ref('list')
const currPosting: Ref<Post | null> = ref(null)
const scrollPosition: Ref<number> = ref(0)
const lastPage: Ref<string> = ref('')
const editorRef: Ref<TiptapRef | null> = ref(null)

// 目录列表数据
const contentsList: Ref<Post[]> = ref([])
const lastContentsId: Ref<number> = ref(0x7fffffff)

// 加载目录数据
const loadContents = async ({ done }: LoadEventData): Promise<void> => {
  try {
    // 查看自己：获取所有状态；查看别人：只获取已发布
    const response = isSelf.value
      ? await userServiceV1.getCurrentUserAllPosts(lastContentsId.value, PostType.CONTENTS)
      : await userServiceV1.getUserPosts(targetUserId.value, lastContentsId.value, PostType.CONTENTS)

    if (response.code === 401) {
      console.log('not login')
    } else if (response.code === 200) {
      console.log(`get data:${JSON.stringify(response.data)}`)
      contentsList.value.push(...response.data)

      if (response.data.length > 0) {
        lastContentsId.value = response.data[response.data.length - 1].id
        done('ok')
      } else {
        done('empty')
      }
    }
  } catch (error) {
    console.error('Error get message:', error)
  }
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

// 修改文章
const modifyPosting = async (): Promise<void> => {
  if (!currPosting.value || !editorRef.value) return

  try {
    console.log('begin post')

    const response = await postServiceV1.updatePost(currPosting.value.id, {
      content: editorRef.value.editor.getHTML(),
    })
    console.log(`response: ${JSON.stringify(response)}`)

    if (response.code === 200 && response.data) {
      console.log('Form submitted successfully')
      // 使用后端返回的数据更新状态
      currPosting.value.content = response.data.content
      currPosting.value.state = response.data.state
      switchToLastPage()
    }
  } catch (error) {
    console.error('Error submitting form:', error)
  }
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