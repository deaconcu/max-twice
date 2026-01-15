<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SinglePost from './SinglePost.vue'
import CommentSection from '@/components/common/CommentSection.vue'
import MemoryCardList from './MemoryCardList.vue'
import AddCatalogDialog from './AddCatalogDialog.vue'
import AddArticleDialog from './AddArticleDialog.vue'
import InviteUserDialog from './InviteUserDialog.vue'
import { ObjectType } from '@/enums'
import type { MemoryCardDeck } from '@/types/memory'

interface Props {
  data: any
  nodes: any[]
  currNodeId: number
  currNode: any
  pathText: string
  isLearning?: boolean
}

interface Emits {
  (e: 'switch-tab', tab: string, posting?: any): void
  (e: 'view-deck', deck: MemoryCardDeck): void
  (e: 'load-data', fields?: string[]): void
}

const props = withDefaults(defineProps<Props>(), {
  isLearning: false,
})

const emit = defineEmits<Emits>()
const route = useRoute()

const tab = ref('list')
const currPosting = ref<any>(null)
const showAddCatalogDialog = ref(false)
const showAddArticleDialog = ref(false)
const showInviteUserDialog = ref(false)

// 监听路由变化，重置 tab 为 list
watch(
  () => [route.params.id, route.query.path, route.query.nodeId],
  () => {
    tab.value = 'list'
    currPosting.value = null
  }
)

// 切换 Tab
const switchTab = (newTab: string, posting?: any) => {
  tab.value = newTab

  if (typeof posting === 'object') {
    currPosting.value = posting
    emit('switch-tab', newTab, posting)
  } else {
    emit('switch-tab', newTab)
  }

  if (newTab === 'list') {
    currPosting.value = null
  } else {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

// 处理 SinglePost 的 switch-tab 事件
const handlePostSwitchTab = (newTab: string, posting?: any) => {
  switchTab(newTab, posting)
}

// 处理添加目录后的数据加载
const handleLoadData = (data: any[]) => {
  console.log('Reload data after adding catalog:', data)
  // TODO: 刷新页面数据
}

// 处理查看卡片组详情
const handleViewDeck = (deck: MemoryCardDeck) => {
  emit('view-deck', deck)
}
</script>

<template>
  <div class="posting-list">
    <!-- 以下内容只在 list/comment/memoryCards 时显示 -->
    <template v-if="tab === 'list' || tab === 'comment' || tab === 'memoryCards'">
      <!-- 节点路径（面包屑） -->
      <v-row
        v-if="nodes && nodes.length > 0"
        class="node-breadcrumb ma-0 text-grey text-body-2 pb-2"
      >
        <div class="d-flex align-center">
          <template v-for="(item, index) in nodes" :key="item">
            {{ data.tocNodeInfos?.[item]?.name || item }}
            <v-icon v-if="index < nodes.length - 1" icon="mdi-chevron-right" class="px-5"></v-icon>
          </template>
        </div>
      </v-row>

      <!-- 节点头部 -->
      <div class="px-0 pb-1 ma-0 mb-0">
        <div class="d-flex align-center justify-space-between mb-2">
          <div class="d-flex align-center">
            <v-icon icon="mdi-list-box-outline" color="primary-darken-1" size="24"></v-icon>
            <h2 class="text-h5 font-weight-bold text-grey-darken-4 ms-3">{{ data.node?.name }}</h2>
          </div>
          <div class="d-flex align-center">
            <v-btn
              v-if="isLearning"
              :color="data.node?.isCompleted ? 'grey-lighten-2' : 'success'"
              :variant="data.node?.isCompleted ? 'outlined' : 'flat'"
              rounded="lg"
              size="small"
              class="px-4"
              :prepend-icon="data.node?.isCompleted ? 'mdi-check-circle' : 'mdi-circle-outline'"
            >
              <span
                class="font-weight-medium"
                :class="data.node?.isCompleted ? 'text-grey-darken-2' : 'text-white'"
              >
                {{ data.node?.isCompleted ? '已完成' : '完成学习' }}
              </span>
            </v-btn>
          </div>
        </div>
        <div v-if="data.node?.description" class="ms-0 mt-4">
          <p class="text-body-2 text-grey-darken-1 mb-0">
            {{ data.node.description }}
          </p>
        </div>
      </div>

      <!-- Tab栏和操作按钮 -->
      <v-row class="tabs-actions-bar mt-4 mb-0 mx-0 justify-space-between">
        <div>
          <v-tabs v-model="tab" density="compact" color="primary">
            <v-tab value="list" class="px-3" @click="switchTab('list')">
              <v-icon icon="mdi-list-box-outline" size="16" class="mr-2"></v-icon>
              <span class="font-weight-medium text-grey-darken-3">文章列表</span>
            </v-tab>
            <v-tab value="comment" class="px-3" @click="switchTab('comment')">
              <v-icon icon="mdi-comment-outline" size="16" class="mr-2"></v-icon>
              <span class="font-weight-medium text-grey-darken-3"
                >{{ data.node?.commentCount || 0 }} 评论</span
              >
            </v-tab>
            <v-tab value="memoryCards" class="px-3" @click="switchTab('memoryCards')">
              <v-icon icon="mdi-cards-outline" size="16" class="mr-2"></v-icon>
              <span class="font-weight-medium text-grey-darken-3">记忆卡片</span>
            </v-tab>
          </v-tabs>
        </div>
        <div class="d-flex align-center action-buttons">
          <v-btn
            variant="flat"
            color="surface-variant"
            rounded="lg"
            class="px-3 me-2"
            density="comfortable"
            @click="showAddCatalogDialog = true"
          >
            <v-icon
              icon="mdi-format-list-group-plus"
              size="14"
              class="mr-2"
              color="grey-darken-3"
            ></v-icon>
            <span class="font-weight-medium text-grey-darken-3">添加目录</span>
          </v-btn>

          <v-btn
            variant="flat"
            color="surface-variant"
            rounded="lg"
            density="comfortable"
            class="px-3 me-2"
            @click="showAddArticleDialog = true"
          >
            <v-icon
              icon="mdi-note-plus-outline"
              size="14"
              class="mr-2"
              color="grey-darken-3"
            ></v-icon>
            <span class="font-weight-medium text-grey-darken-3">添加文章</span>
          </v-btn>

          <v-btn
            variant="flat"
            color="surface-variant"
            rounded="lg"
            class="px-3"
            density="comfortable"
            @click="showInviteUserDialog = true"
          >
            <v-icon
              icon="mdi-account-plus-outline"
              size="14"
              class="mr-2"
              color="grey-darken-3"
            ></v-icon>
            <span class="font-weight-medium text-grey-darken-3">邀请回答</span>
          </v-btn>
        </div>
      </v-row>
    </template>

    <!-- 内容区域 -->
    <template v-if="tab === 'list'">
      <div class="mt-4">
        <!-- 固定文章 -->
        <div v-if="data.fixedPostings && data.fixedPostings.length > 0">
          <div v-for="(posting, key) in data.fixedPostings" :key="key" class="pt-8">
            <SinglePost
              :posting="posting"
              :curr-node="currNode"
              :data="data"
              :is-learning="isLearning"
              @switch-tab="handlePostSwitchTab"
              @load-data="(fields) => emit('load-data', fields)"
            />
            <v-divider class="mt-11" color="grey-darken-2"></v-divider>
          </div>
        </div>

        <!-- 其他文章 -->
        <div v-if="data.otherPostings && data.otherPostings.length > 0">
          <div
            v-for="(posting, index) in data.otherPostings"
            :key="posting.id"
            :class="
              index == 0 && (!data.fixedPostings || data.fixedPostings.length === 0)
                ? 'pt-4'
                : 'pt-8'
            "
          >
            <SinglePost
              :posting="posting"
              :curr-node="currNode"
              :data="data"
              :is-learning="isLearning"
              @switch-tab="handlePostSwitchTab"
              @load-data="(fields) => emit('load-data', fields)"
            />
            <v-divider class="mt-11" color="grey-darken-2"></v-divider>
          </div>
        </div>

        <!-- 空状态 -->
        <div
          v-if="
            (!data.fixedPostings || data.fixedPostings.length === 0) &&
            (!data.otherPostings || data.otherPostings.length === 0)
          "
          class="text-body-2 text-grey py-8 text-center"
        >
          - 暂无文章 -
        </div>
      </div>
    </template>

    <!-- 评论区 -->
    <template v-else-if="tab === 'comment'">
      <CommentSection
        :post-id="currNodeId"
        :comment-count="data.node?.commentCount || 0"
        :object-type="ObjectType.NODE"
        class="mt-2"
      />
    </template>

    <!-- 记忆卡片 -->
    <template v-else-if="tab === 'memoryCards'">
      <MemoryCardList :node-id="currNodeId" @view-deck="handleViewDeck" />
    </template>

    <!-- 文章详情 -->
    <template v-else>
      <div class="pt-0">
        <SinglePost
          :posting="currPosting"
          :curr-node="currNode"
          :data="data"
          :is-learning="isLearning"
          :detail="true"
          @switch-tab="handlePostSwitchTab"
          @load-data="(fields) => emit('load-data', fields)"
        />

        <!-- 评论区 -->
        <CommentSection
          :post-id="currPosting?.id"
          :comment-count="currPosting?.commentCount || 0"
          :object-type="ObjectType.POST"
          class="mt-6"
        />
      </div>
    </template>

    <!-- 添加目录对话框 -->
    <AddCatalogDialog
      v-model="showAddCatalogDialog"
      :node-id="currNodeId"
      :path-text="pathText"
      @load-data="handleLoadData"
    />

    <!-- 添加文章对话框 -->
    <AddArticleDialog
      v-model="showAddArticleDialog"
      :node-id="currNodeId"
      :path-text="pathText"
      @load-data="handleLoadData"
    />

    <!-- 邀请回答对话框 -->
    <InviteUserDialog v-model="showInviteUserDialog" :node-id="currNodeId" />
  </div>
</template>

<style scoped>
.posting-list {
  width: 100%;
}

/* Tab栏和操作按钮行 - 固定在顶部 */
.tabs-actions-bar {
  position: sticky;
  top: 92px;
  background-color: white;
  z-index: 900;
  padding: 12px 0 !important;
}

/* 移动端：面包屑增加上边距 */
@media (max-width: 1280px) {
  .node-breadcrumb {
    padding-top: 16px !important;
  }
}

/* 移动端：操作按钮只显示图标 */
@media (max-width: 750px) {
  .tabs-actions-bar .action-buttons span {
    display: none !important;
  }

  .tabs-actions-bar .action-buttons .v-btn {
    min-width: 36px !important;
    padding: 0 8px !important;
  }

  .tabs-actions-bar .action-buttons .v-icon {
    margin-right: 0 !important;
  }
}
</style>
