<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">我的卡片组</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">
            使用间隔重复算法高效记忆知识点。
          </p>
          <v-divider class="my-3"></v-divider>
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-brain" size="14" class="mr-1"></v-icon>
              科学记忆方法
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-calendar-clock" size="14" class="mr-1"></v-icon>
              定期复习提醒
            </div>
            <div>
              <v-icon icon="mdi-chart-line" size="14" class="mr-1"></v-icon>
              进度统计分析
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <div class="d-flex align-center justify-space-between mb-4">
          <v-icon icon="mdi-menu" size="18" color="grey-lighten-1"></v-icon>
        </div>

        <!-- 卡片组列表 -->
        <div v-if="decks.length > 0">
          <v-row>
            <v-col
              v-for="deck in decks"
              :key="deck.id"
              cols="12"
              md="6"
            >
              <v-card
                border
                rounded="lg"
                class="hoverable"
                @click="goToReview(deck.id)"
              >
                <v-card-text class="pa-4">
                <!-- 卡片组头部 -->
                <div class="d-flex align-start justify-space-between mb-3">
                  <div class="flex-grow-1">
                    <div class="d-flex align-center mb-2">
                      <v-avatar :color="deck.color" size="40" rounded="md" class="mr-3">
                        <v-icon :icon="deck.icon" color="white" size="20"></v-icon>
                      </v-avatar>
                      <div>
                        <h4 class="text-body-1 font-weight-bold mb-1">{{ deck.name }}</h4>
                        <p class="text-caption text-grey mb-0">
                          {{ deck.cardCount }} 张卡片
                        </p>
                      </div>
                    </div>

                    <!-- 卡片组描述 -->
                    <p v-if="deck.description" class="text-body-2 text-grey-darken-2 mb-3">
                      {{ deck.description }}
                    </p>

                    <!-- 所属节点和文章 -->
                    <div class="d-flex align-center mb-3" style="gap: 8px;">
                      <!-- 所属节点 -->
                      <v-chip
                        v-if="deck.node"
                        size="small"
                        variant="tonal"
                        color="grey-darken-2"
                        class="cursor-pointer"
                      >
                        <v-icon icon="mdi-file-document-outline" size="14" class="mr-1"></v-icon>
                        {{ deck.node.name }}
                      </v-chip>

                      <!-- 所属文章 -->
                      <v-chip
                        v-if="deck.article"
                        size="small"
                        variant="tonal"
                        color="primary"
                        class="cursor-pointer"
                        @click.stop="goToArticle(deck.article.id)"
                      >
                        <v-icon icon="mdi-note-text-outline" size="14" class="mr-1"></v-icon>
                        查看文章
                      </v-chip>
                    </div>

                    <!-- 创建时间 -->
                    <div class="text-caption text-grey">
                      <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                      创建于 {{ formatDate(deck.createdAt) }}
                    </div>
                  </div>

                  <!-- 删除按钮 -->
                  <v-btn
                    color="grey"
                    variant="tonal"
                    size="x-small"
                    icon="mdi-delete"
                    @click.stop="deleteDeck(deck.id)"
                  >
                    <v-icon>mdi-delete</v-icon>
                    <v-tooltip activator="parent" location="top">删除卡片组</v-tooltip>
                  </v-btn>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </div>

      <!-- 空状态 -->
      <div v-else class="text-center py-12">
        <v-icon icon="mdi-cards" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
        <p class="text-body-1 text-grey-darken-2">暂无卡片组</p>
        <p class="text-body-2 text-grey">创建记忆卡片，高效复习知识点</p>
        <v-btn color="primary" variant="outlined" rounded="md" density="compact" class="mt-4" to="/memory-review">
          <v-icon icon="mdi-brain" size="18" class="mr-2"></v-icon>
          前往复习中心
        </v-btn>
      </div>

      <!-- 删除确认对话框 -->
      <ConfirmDialog
        v-model="showDeleteDialog"
        title="确认删除"
        message="确定要删除该卡片组吗？此操作不可恢复。"
        confirm-text="确认删除"
        @confirm="confirmDelete"
      />
    </div>
  </v-col>
</v-row>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()

// Mock 卡片组数据
const decks = ref([
  {
    id: 1,
    name: 'Vue 3 核心概念',
    description: 'Vue 3 Composition API 和响应式系统的核心知识点',
    cardCount: 45,
    createdAt: '2024-10-15',
    icon: 'mdi-vuejs',
    color: '#42b883',
    node: { id: 101, name: 'Composition API 设计理念' },
    article: { id: 1001 }
  },
  {
    id: 2,
    name: 'TypeScript 类型系统',
    description: 'TypeScript 高级类型和泛型使用技巧',
    cardCount: 38,
    createdAt: '2024-10-20',
    icon: 'mdi-language-typescript',
    color: '#3178c6',
    node: { id: 102, name: '高级类型系统' },
    article: { id: 1002 }
  },
  {
    id: 3,
    name: 'JavaScript 设计模式',
    description: '常用设计模式及其在 JavaScript 中的实现',
    cardCount: 28,
    createdAt: '2024-10-25',
    icon: 'mdi-language-javascript',
    color: '#f7df1e',
    node: { id: 103, name: '设计模式详解' },
    article: { id: 1003 }
  },
  {
    id: 4,
    name: 'React Hooks',
    description: 'React Hooks 的使用方法和最佳实践',
    cardCount: 32,
    createdAt: '2024-11-01',
    icon: 'mdi-react',
    color: '#61dafb',
    node: { id: 104, name: 'Hooks 原理与实践' },
    article: { id: 1004 }
  }
])

// 删除对话框
const showDeleteDialog = ref(false)
const deckToDelete = ref<number | null>(null)

// 格式化日期
const formatDate = (date: string | null) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}

// 跳转到复习页面
const goToReview = (deckId: number) => {
  router.push(`/memory-review?deckId=${deckId}`)
}

// 跳转到文章详情
const goToArticle = (articleId: number) => {
  // TODO: 跳转到文章详情页
  console.log('Go to article:', articleId)
}

// 编辑卡片组
const editDeck = (deckId: number) => {
  router.push(`/memory-review?deckId=${deckId}&mode=edit`)
}

// 删除卡片组
const deleteDeck = (deckId: number) => {
  deckToDelete.value = deckId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = () => {
  if (deckToDelete.value !== null) {
    decks.value = decks.value.filter(d => d.id !== deckToDelete.value)
  }
  deckToDelete.value = null
}
</script>

<style scoped>
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

.stat-box {
  text-align: center;
  padding: 8px 0;
}

/* 移动端取消 sticky */
@media (max-width: 960px) {
  .sticky-sidebar {
    position: relative;
    top: 0;
    max-height: none;
    margin-bottom: 16px;
  }
}
</style>
