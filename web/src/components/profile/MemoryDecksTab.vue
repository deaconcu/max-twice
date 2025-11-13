<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">我的卡片组</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">使用间隔重复算法高效记忆知识点。</p>
          <v-divider class="my-3" />
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-brain" size="14" class="mr-1" />
              科学记忆方法
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-calendar-clock" size="14" class="mr-1" />
              定期复习提醒
            </div>
            <div>
              <v-icon icon="mdi-chart-line" size="14" class="mr-1" />
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
          <v-icon icon="mdi-menu" size="18" color="grey-lighten-1" />
        </div>

        <!-- 卡片组列表 -->
        <div v-if="decks.length > 0">
          <v-row>
            <v-col v-for="deck in decks" :key="deck.id" cols="12" md="6">
              <v-card border rounded="lg" class="hoverable" @click="goToReview(deck.id)">
                <v-card-text class="pa-4">
                  <!-- 卡片组头部 -->
                  <div class="d-flex align-start justify-space-between mb-3">
                    <div class="flex-grow-1">
                      <div class="d-flex align-center mb-2">
                        <v-avatar :color="deck.color" size="40" rounded="md" class="mr-3">
                          <v-icon :icon="deck.icon" color="white" size="20" />
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
                      <div class="d-flex align-center mb-3" style="gap: 8px">
                        <!-- 所属节点 -->
                        <v-chip
                          v-if="deck.node"
                          size="small"
                          variant="tonal"
                          color="grey-darken-2"
                          class="cursor-pointer"
                        >
                          <v-icon icon="mdi-file-document-outline" size="14" class="mr-1" />
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
                          <v-icon icon="mdi-note-text-outline" size="14" class="mr-1" />
                          查看文章
                        </v-chip>
                      </div>

                      <!-- 创建时间 -->
                      <div class="text-caption text-grey">
                        <v-icon icon="mdi-clock-outline" size="14" class="mr-1" />
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
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
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
