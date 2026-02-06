<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SinglePost from './SinglePost.vue'
import CommentSection from '@/components/common/CommentSection.vue'
import MemoryCardList from './MemoryCardList.vue'
import NodeSelectorDialog from './NodeSelectorDialog.vue'
import ArticleEditModal from '@/components/profile/ArticleEditModal.vue'
import InviteUserDialog from './InviteUserDialog.vue'
import { ObjectType } from '@/enums'
import type { MemoryCardDeck } from '@/types/memory'
import type { Node } from '@/types/node'

interface Props {
  data: any
  nodes: any[]
  currNodeId: number
  currNode: any
  pathText: string
  isLearning?: boolean
  loadingMore?: boolean
  hasMore?: boolean
}

interface Emits {
  (e: 'switch-tab', tab: string, posting?: any): void
  (e: 'view-deck', deck: MemoryCardDeck): void
  (e: 'load-data', fields?: string[]): void
  (e: 'mark-node-completed'): void
}

const props = withDefaults(defineProps<Props>(), {
  isLearning: false,
  loadingMore: false,
  hasMore: true,
})

const emit = defineEmits<Emits>()
const route = useRoute()

const tab = ref('list')
const currPosting = ref<any>(null)
const nodeSelectorDialog = ref()
const showAddArticleDialog = ref(false)
const showInviteUserDialog = ref(false)
const showFavoritePosts = ref(false)
const showFavoriteDecks = ref(false)
const showCreateDeckDialog = ref(false)

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
const handleNodeConfirm = (nodes: Node[]) => {
  console.log('选择/创建的节点列表:', nodes)
  // TODO: 调用 API 批量添加节点到当前目录
  // TODO: 刷新页面数据
  emit('load-data')
}

// 处理添加文章后的数据加载
const handleLoadData = (data?: any[]) => {
  console.log('Reload data after adding:', data)
  emit('load-data')
}

// 处理查看卡片组详情
const handleViewDeck = (deck: MemoryCardDeck) => {
  emit('view-deck', deck)
}

// 处理节点完成
const handleMarkNodeCompleted = () => {
  console.log('PostingList: 接收到 mark-node-completed 事件，向上传递')
  emit('mark-node-completed')
}

// 判断当前节点是否可以完成
// 如果是目录节点且不在可完成列表中，则不允许完成
const canCompleteNode = () => {
  if (!props.data?.node) return false

  // 如果节点已完成，允许取消完成
  if (props.data.node.isCompleted) return true

  // 检查是否是目录节点（有子节点）
  const hasChildren = props.currNode && Object.keys(props.currNode).some(k => k !== '^' && k !== '+')

  // 如果是叶子节点，允许完成
  if (!hasChildren) return true

  // 如果是目录节点，检查是否在可完成列表中
  return props.data.node.canComplete === true
}

// 完成按钮的禁用提示
const completeButtonTooltip = () => {
  if (!props.data?.node) return ''

  if (props.data.node.isCompleted) return '点击取消完成'

  const hasChildren = props.currNode && Object.keys(props.currNode).some(k => k !== '^' && k !== '+')

  if (hasChildren && !props.data.node.canComplete) {
    return '请先完成该目录下的所有节点'
  }

  return '标记节点为已完成'
}
</script>

<template>
  <div class="posting-list">
    <!-- 以下内容只在 list/comment/memoryCards 时显示 -->
    <template v-if="tab === 'list' || tab === 'comment' || tab === 'memoryCards'">
      <!-- 节点路径（面包屑） -->
      <v-row
        v-if="nodes && nodes.length > 0"
        class="node-breadcrumb ma-0 text-grey text-body-2 pb-4"
      >
        <div class="breadcrumb-wrapper d-flex align-center flex-wrap">
          <template v-for="(item, index) in nodes" :key="item">
            <span class="breadcrumb-item">
              {{ data.tocNodeInfos?.[item]?.name || item }}
              <v-icon v-if="index < nodes.length - 1" icon="mdi-chevron-right" class="px-5"></v-icon>
            </span>
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
          <div class="d-flex align-center ga-2">
            <!-- 引用次数 -->
            <div
              v-if="data.node?.nodeReferenceCount !== undefined && data.node.nodeReferenceCount > 0"
              class="d-flex align-center text-body-2 text-grey-darken-2 px-3 py-1 bg-grey-lighten-5 rounded-lg"
            >
              <v-icon icon="mdi-link-variant" size="small" color="grey-darken-1" class="mr-1"></v-icon>
              {{ data.node.nodeReferenceCount }}
              <v-tooltip activator="parent" location="top">
                被引用次数：{{ data.node.nodeReferenceCount }}
              </v-tooltip>
            </div>
            <!-- 完成学习按钮 -->
            <div v-if="isLearning" class="d-inline-block">
              <v-btn
                :color="data.node?.isCompleted ? 'grey-lighten-2' : 'success'"
                :variant="data.node?.isCompleted ? 'outlined' : 'flat'"
                :disabled="!canCompleteNode()"
                rounded="lg"
                size="small"
                class="px-4"
                :prepend-icon="data.node?.isCompleted ? 'mdi-check-circle' : 'mdi-circle-outline'"
                @click="emit('mark-node-completed')"
              >
                <span
                  class="font-weight-medium"
                  :class="data.node?.isCompleted ? 'text-grey-darken-2' : 'text-white'"
                >
                  {{ data.node?.isCompleted ? '已完成' : '完成学习' }}
                </span>
              </v-btn>
              <v-tooltip activator="parent" location="bottom">
                {{ completeButtonTooltip() }}
              </v-tooltip>
            </div>
          </div>
        </div>
        <div v-if="data.node?.description" class="ms-0 mt-4">
          <p class="text-body-2 text-grey-darken-1 mb-0">
            {{ data.node.description }}
          </p>
        </div>
      </div>

      <!-- Tab栏和操作按钮 -->
      <v-row class="tabs-actions-bar mt-4 mb-0 mx-0 justify-space-between align-center">
        <div>
          <v-tabs v-model="tab" density="compact" color="primary">
            <v-tab value="list" class="px-3" @click="switchTab('list')">
              <v-icon icon="mdi-list-box-outline" size="16" class="mr-2"></v-icon>
              <span class="font-weight-medium text-grey-darken-3">文章列表</span>
            </v-tab>
            <v-tab value="memoryCards" class="px-3" @click="switchTab('memoryCards')">
              <v-icon icon="mdi-cards-outline" size="16" class="mr-2"></v-icon>
              <span class="font-weight-medium text-grey-darken-3">记忆卡片</span>
            </v-tab>
            <v-tab value="comment" class="px-3" @click="switchTab('comment')">
              <v-icon icon="mdi-comment-outline" size="16" class="mr-2"></v-icon>
              <span class="font-weight-medium text-grey-darken-3"
                >{{ data.node?.commentCount || 0 }} 评论</span
              >
            </v-tab>
          </v-tabs>
        </div>

        <!-- 右侧：精简操作按钮（根据 tab 动态显示） -->
        <div class="d-flex align-center ga-2">
          <!-- 文章列表 Tab：主按钮 + 更多菜单 -->
          <template v-if="tab === 'list'">
            <!-- 添加目录按钮：桌面端显示文字，移动端只显示图标 -->
            <v-btn
              variant="flat"
              color="surface-variant"
              rounded="lg"
              size="default"
              :class="$vuetify.display.smAndDown ? 'px-2' : 'px-3'"
              @click="nodeSelectorDialog?.open()"
            >
              <v-icon icon="mdi-format-list-group-plus" size="16" :class="$vuetify.display.mdAndUp ? 'mr-1' : ''"></v-icon>
              <span v-if="$vuetify.display.mdAndUp" class="font-weight-medium text-grey-darken-3">添加目录</span>
            </v-btn>

            <!-- 添加文章按钮：桌面端显示文字，移动端只显示图标 -->
            <v-btn
              variant="flat"
              color="surface-variant"
              rounded="lg"
              size="default"
              :class="$vuetify.display.smAndDown ? 'px-2' : 'px-3'"
              @click="showAddArticleDialog = true"
            >
              <v-icon icon="mdi-note-plus-outline" size="16" :class="$vuetify.display.mdAndUp ? 'mr-1' : ''"></v-icon>
              <span v-if="$vuetify.display.mdAndUp" class="font-weight-medium text-grey-darken-3">添加文章</span>
            </v-btn>

            <!-- 桌面端（md及以上）：显示更多按钮 -->
            <template v-if="$vuetify.display.mdAndUp">
              <v-menu>
                <template #activator="{ props }">
                  <v-btn v-bind="props" variant="text" rounded="lg" density="comfortable" icon>
                    <v-icon size="16">mdi-dots-vertical</v-icon>
                  </v-btn>
                </template>
                <v-card rounded="lg" class="elevation-0 mt-2 menu-card">
                  <v-list density="compact" min-width="160" class="elevation-0 py-0">
                    <v-list-item class="border-b" @click="showInviteUserDialog = true">
                      <template #prepend>
                        <v-icon icon="mdi-account-plus-outline" size="18"></v-icon>
                      </template>
                      <v-list-item-title>邀请回答</v-list-item-title>
                    </v-list-item>
                    <v-list-item @click="showFavoritePosts = true">
                      <template #prepend>
                        <v-icon icon="mdi-star-outline" size="18"></v-icon>
                      </template>
                      <v-list-item-title>收藏的文章</v-list-item-title>
                    </v-list-item>
                  </v-list>
                </v-card>
              </v-menu>
            </template>

            <!-- 移动端（sm及以下）-->
            <template v-else>
              <v-menu>
                <template v-slot:activator="{ props }">
                  <v-btn
                    v-bind="props"
                    variant="text"
                    rounded="lg"
                    size="small"
                    icon
                  >
                    <v-icon>mdi-dots-vertical</v-icon>
                  </v-btn>
                </template>
                <v-card rounded="lg" class="elevation-0 mt-2 menu-card">
                  <v-list density="compact" min-width="160" class="elevation-0 py-0">
                    <v-list-item @click="showInviteUserDialog = true">
                      <template v-slot:prepend>
                        <v-icon icon="mdi-account-plus-outline" size="18"></v-icon>
                      </template>
                      <v-list-item-title>邀请回答</v-list-item-title>
                    </v-list-item>
                    <v-list-item @click="showFavoritePosts = true">
                      <template v-slot:prepend>
                        <v-icon icon="mdi-star-outline" size="18"></v-icon>
                      </template>
                      <v-list-item-title>收藏的文章</v-list-item-title>
                    </v-list-item>
                  </v-list>
                </v-card>
              </v-menu>
            </template>
          </template>

          <!-- 记忆卡片 Tab：主按钮 + 更多菜单 -->
          <template v-else-if="tab === 'memoryCards'">
            <!-- 主按钮：桌面端显示文字，移动端只显示图标 -->
            <v-btn
              variant="flat"
              color="surface-variant"
              rounded="lg"
              size="default"
              :class="$vuetify.display.smAndDown ? 'px-2' : 'px-3'"
              @click="showCreateDeckDialog = true"
            >
              <v-icon icon="mdi-plus" size="16" :class="$vuetify.display.mdAndUp ? 'mr-1' : ''"></v-icon>
              <span v-if="$vuetify.display.mdAndUp" class="font-weight-medium text-grey-darken-3">新增卡片组</span>
            </v-btn>

            <!-- 桌面端（md及以上）：显示更多按钮 -->
            <template v-if="$vuetify.display.mdAndUp">
              <v-menu>
                <template v-slot:activator="{ props }">
                  <v-btn
                    v-bind="props"
                    variant="text"
                    rounded="lg"
                    density="comfortable"
                    icon
                  >
                    <v-icon size="16">mdi-dots-vertical</v-icon>
                  </v-btn>
                </template>
                <v-card rounded="lg" class="elevation-0 mt-2 menu-card">
                  <v-list density="compact" min-width="160" class="elevation-0 py-0">
                    <v-list-item @click="showFavoriteDecks = true">
                      <template v-slot:prepend>
                        <v-icon icon="mdi-star-outline" size="18"></v-icon>
                      </template>
                      <v-list-item-title>收藏的卡片组</v-list-item-title>
                    </v-list-item>
                  </v-list>
                </v-card>
              </v-menu>
            </template>

            <!-- 移动端（sm及以下）-->
            <template v-else>
              <v-menu>
                <template v-slot:activator="{ props }">
                  <v-btn
                    v-bind="props"
                    variant="text"
                    rounded="lg"
                    size="small"
                    icon
                  >
                    <v-icon>mdi-dots-vertical</v-icon>
                  </v-btn>
                </template>
                <v-card rounded="lg" class="elevation-0 mt-2 menu-card">
                  <v-list density="compact" min-width="160" class="elevation-0 py-0">
                    <v-list-item @click="showFavoriteDecks = true">
                      <template v-slot:prepend>
                        <v-icon icon="mdi-star-outline" size="18"></v-icon>
                      </template>
                      <v-list-item-title>收藏的卡片组</v-list-item-title>
                    </v-list-item>
                  </v-list>
                </v-card>
              </v-menu>
            </template>
          </template>

          <!-- 评论 Tab：无操作按钮 -->
        </div>
      </v-row>
    </template>

    <!-- 内容区域 -->
    <template v-if="tab === 'list'">
      <div class="mt-4">
        <!-- 其他文章 -->
        <div v-if="data.otherPostings && data.otherPostings.length > 0">
          <div v-for="(posting, index) in data.otherPostings" :key="posting.id" :class="index == 0 ? 'pt-4' : 'pt-8'">
            <SinglePost
              :posting="posting"
              :curr-node="currNode"
              :data="data"
              :is-learning="isLearning"
              @switch-tab="handlePostSwitchTab"
              @load-data="(fields) => emit('load-data', fields)"
              @mark-node-completed="emit('mark-node-completed')"
            />
            <v-divider class="mt-11" color="grey-darken-2"></v-divider>
          </div>

          <!-- 加载更多状态 -->
          <div class="text-center py-8">
            <!-- 加载中 -->
            <div v-if="loadingMore" class="d-flex flex-column align-center">
              <v-progress-circular indeterminate color="primary" size="32"></v-progress-circular>
              <p class="text-body-2 text-grey mt-4">加载中...</p>
            </div>
            <!-- 已到底 -->
            <div v-else-if="!hasMore" class="text-grey">
              <v-icon icon="mdi-check-circle-outline" size="20" class="mr-1"></v-icon>
              <span class="text-body-2">已经到底了</span>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!data.otherPostings || data.otherPostings.length === 0" class="text-center pa-12">
          <v-icon icon="mdi-text-box-outline" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
          <h4 class="text-h6 font-weight-medium text-grey-darken-2 mb-2">暂无文章</h4>
          <p class="text-body-2 text-grey-darken-1 mb-4">还没有人为这个节点创建文章</p>
          <div class="d-flex justify-center">
            <v-btn
              color="primary"
              variant="tonal"
              rounded="lg"
              prepend-icon="mdi-format-list-group-plus"
              class="mr-4"
              @click="nodeSelectorDialog?.open()"
            >
              添加目录
            </v-btn>
            <v-btn
              color="primary"
              variant="tonal"
              rounded="lg"
              prepend-icon="mdi-note-plus-outline"
              @click="showAddArticleDialog = true"
            >
              添加文章
            </v-btn>
          </div>
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
          @mark-node-completed="emit('mark-node-completed')"
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

    <!-- 节点选择器对话框 -->
    <NodeSelectorDialog
      ref="nodeSelectorDialog"
      :course-id="data.course?.id"
      :node-id="currNodeId"
      @confirm="handleNodeConfirm"
      @load-data="handleLoadData"
    />

    <!-- 添加文章对话框 -->
    <ArticleEditModal
      v-model="showAddArticleDialog"
      :article="null"
      :node-id="currNodeId"
      @success="handleLoadData"
    />

    <!-- 邀请回答对话框 -->
    <InviteUserDialog v-model="showInviteUserDialog" :node-id="currNodeId" />

    <!-- TODO: 收藏的文章对话框（待实现） -->
    <v-dialog v-model="showFavoritePosts" max-width="800">
      <v-card rounded="xl">
        <v-card-title class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <span>收藏的文章</span>
            <v-btn icon="mdi-close" variant="text" size="small" @click="showFavoritePosts = false"></v-btn>
          </div>
        </v-card-title>
        <v-card-text class="pa-4">
          <div class="text-center py-8">
            <v-icon icon="mdi-note-text-outline" size="64" color="grey-lighten-2"></v-icon>
            <p class="text-body-1 text-grey-darken-1 mt-4">收藏功能开发中...</p>
          </div>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- TODO: 收藏的卡片组对话框（待实现） -->
    <v-dialog v-model="showFavoriteDecks" max-width="800">
      <v-card rounded="xl">
        <v-card-title class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <span>收藏的卡片组</span>
            <v-btn icon="mdi-close" variant="text" size="small" @click="showFavoriteDecks = false"></v-btn>
          </div>
        </v-card-title>
        <v-card-text class="pa-4">
          <div class="text-center py-8">
            <v-icon icon="mdi-cards-outline" size="64" color="grey-lighten-2"></v-icon>
            <p class="text-body-1 text-grey-darken-1 mt-4">收藏功能开发中...</p>
          </div>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- TODO: 新增卡片组对话框（待实现） -->
    <v-dialog v-model="showCreateDeckDialog" max-width="600">
      <v-card rounded="xl">
        <v-card-title class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <span>新增卡片组</span>
            <v-btn icon="mdi-close" variant="text" size="small" @click="showCreateDeckDialog = false"></v-btn>
          </div>
        </v-card-title>
        <v-card-text class="pa-4">
          <div class="text-center py-8">
            <v-icon icon="mdi-cards-plus-outline" size="64" color="grey-lighten-2"></v-icon>
            <p class="text-body-1 text-grey-darken-1 mt-4">新增卡片组功能开发中...</p>
          </div>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.posting-list {
  width: 100%;
}

/* 面包屑容器 - 增加行间距 */
.breadcrumb-wrapper {
  gap: 0;
  row-gap: 6px;
}

/* 面包屑项 - 防止内部换行 */
.breadcrumb-item {
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
}

/* Tab栏和操作按钮行 - 固定在顶部 */
.tabs-actions-bar {
  position: sticky;
  top: 92px;
  background-color: white;
  z-index: 900;
  padding: 12px 0 !important;
}

/* 更多菜单卡片边框 */
.menu-card {
  border: 1px solid rgb(var(--v-theme-border)) !important;
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
