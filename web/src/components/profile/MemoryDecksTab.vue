<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-4">
          <div class="mb-4">
            <h4 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">我的卡片组</h4>
            <p class="text-body-2 text-grey mb-0">
              使用间隔重复算法高效记忆知识点。
            </p>
          </div>
          <v-divider class="my-4" />
          <div class="text-body-2 text-grey">
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-brain" size="18" color="grey" class="mr-2 mt-1" />
              <span>科学记忆方法</span>
            </div>
            <div class="d-flex align-start mb-3">
              <v-icon icon="mdi-calendar-clock" size="18" color="grey" class="mr-2 mt-1" />
              <span>定期复习提醒</span>
            </div>
            <div class="d-flex align-start">
              <v-icon icon="mdi-chart-line" size="18" color="grey" class="mr-2 mt-1" />
              <span>进度统计分析</span>
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <div class="d-flex align-center justify-space-between mb-6">
          <div></div>
        </div>

        <!-- 卡片组列表 -->
        <div v-if="decks.length > 0">
          <v-row>
            <v-col v-for="deck in decks" :key="deck.id" cols="12" md="6">
              <v-card rounded="xl" hover border elevation="0" class="deck-card hoverable" @click="goToReview(deck.id)">
                <v-card-text class="pa-6">
                  <!-- 卡片组头部 -->
                  <div class="d-flex align-start justify-space-between mb-4">
                    <div class="d-flex align-center flex-grow-1">
                      <v-avatar :color="deck.color" size="56" rounded="lg" class="mr-4">
                        <v-icon :icon="deck.icon" color="white" size="28" />
                      </v-avatar>
                      <div>
                        <h4 class="text-h6 font-weight-bold mb-1">{{ deck.name }}</h4>
                        <p class="text-caption text-grey mb-0">{{ deck.cardCount }} 张卡片</p>
                      </div>
                    </div>

                    <!-- 删除按钮 -->
                    <v-btn
                      color="grey"
                      variant="text"
                      size="small"
                      icon="mdi-delete"
                      @click.stop="deleteDeck(deck.id)"
                    >
                      <v-icon>mdi-delete</v-icon>
                      <v-tooltip activator="parent" location="top">删除卡片组</v-tooltip>
                    </v-btn>
                  </div>

                  <!-- 卡片组描述 -->
                  <p v-if="deck.description" class="text-body-2 text-grey-darken-2 mb-3 deck-description">
                    {{ deck.description }}
                  </p>

                  <!-- 创建时间 -->
                  <div class="text-caption text-grey">
                    <v-icon icon="mdi-clock-outline" size="14" class="mr-1" />
                    创建于 {{ formatDate(deck.createdAt) }}
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-cards" size="64" color="grey-lighten-2" class="mb-4" />
          <p class="text-body-1 text-grey-darken-2">暂无卡片组</p>
          <p class="text-body-2 text-grey">创建记忆卡片，高效复习知识点</p>
          <v-btn
            color="primary"
            variant="outlined"
            rounded="md"
            density="compact"
            class="mt-4"
            to="/memory-review"
          >
            <v-icon icon="mdi-brain" size="18" class="mr-2" />
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
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { memoryApi } from '@/api'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()

// 获取记忆库课程列表
const {
  data: memoryBankCourses,
  loading,
  execute: fetchMemoryBanks,
} = useFetch({
  fetchFn: memoryApi.getMemoryBankCourses,
  immediate: true,
  defaultValue: [],
})

// 删除课程记忆库
const { execute: removeMemoryBank } = useMutation(
  (courseId: number) => memoryApi.removeCourseMemoryBank(courseId),
  {
    successMessage: '已删除该卡片组',
    onSuccess: () => {
      fetchMemoryBanks()
    },
  }
)

// 转换为卡片组格式
const decks = computed(() => {
  if (!memoryBankCourses.value) return []

  return memoryBankCourses.value.map((memoryBank) => {
    const course = memoryBank.course

    return {
      id: memoryBank.courseId,
      name: course?.name || '未知课程',
      description: course?.description || '',
      cardCount: memoryBank.totalCards || 0,
      createdAt: memoryBank.firstReviewDate || null,
      icon: 'mdi-cards',
      color: '#42b883',
      node: undefined, // Memory bank不关联特定节点
      article: undefined, // Memory bank不关联特定文章
    }
  })
})

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
  router.push(`/memory-review?courseId=${deckId}`)
}

// 跳转到文章详情
const goToArticle = (articleId: number) => {
  console.log('Go to article:', articleId)
}

// 删除卡片组
const deleteDeck = (deckId: number) => {
  deckToDelete.value = deckId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (deckToDelete.value !== null) {
    await removeMemoryBank(deckToDelete.value)
  }
  deckToDelete.value = null
}
</script>

<style scoped>
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 140px;
  align-self: flex-start;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.deck-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: #ffffff;
  border: 1px solid #e9ecef !important;
}

.deck-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
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
