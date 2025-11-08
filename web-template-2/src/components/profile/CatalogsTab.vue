<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">创建的目录</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">
            组织和管理您的学习内容集合。
          </p>
          <v-divider class="my-3"></v-divider>
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-folder-plus" size="14" class="mr-1"></v-icon>
              创建内容目录
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-lock" size="14" class="mr-1"></v-icon>
              公开/私密设置
            </div>
            <div>
              <v-icon icon="mdi-tag-multiple" size="14" class="mr-1"></v-icon>
              内容分类管理
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

          <!-- 搜索框 -->
          <v-text-field
            v-model="searchQuery"
            placeholder="搜索课程目录..."
            prepend-inner-icon="mdi-magnify"
            variant="outlined"
            density="compact"
            rounded="md"
            clearable
            hide-details
            max-width="400"
          ></v-text-field>
        </div>

        <!-- 目录列表 -->
        <div v-if="filteredCatalogs.length > 0">
          <v-row>
            <v-col
              v-for="catalog in filteredCatalogs"
              :key="catalog.id"
              cols="12"
              md="6"
            >
              <v-card
                border
                rounded="lg"
                hover
                class="hoverable"
              >
                <v-card-text class="pa-4">
                  <div class="d-flex align-start justify-space-between mb-3">
                    <div class="flex-grow-1" @click="goToCatalog(catalog.id)" style="cursor: pointer;">
                      <h4 class="text-body-1 font-weight-bold mb-1">{{ catalog.name }}</h4>
                      <p class="text-body-2 text-grey mb-0">{{ catalog.description }}</p>
                    </div>
                    <v-btn
                      color="grey"
                      variant="tonal"
                      size="x-small"
                      icon="mdi-delete"
                      @click.stop="deleteCatalog(catalog.id)"
                    >
                      <v-icon>mdi-delete</v-icon>
                      <v-tooltip activator="parent" location="top">删除目录</v-tooltip>
                    </v-btn>
                  </div>

                  <!-- 所属课程 -->
                  <div v-if="catalog.course" class="mb-3">
                    <v-chip
                      size="small"
                      variant="tonal"
                      color="grey-darken-2"
                      @click.stop="goToCourse(catalog.course.id)"
                      class="cursor-pointer"
                    >
                      <v-icon icon="mdi-book-outline" size="14" class="mr-1"></v-icon>
                      {{ catalog.course.name }}
                    </v-chip>
                  </div>

                  <div class="d-flex align-center justify-space-between text-caption text-grey">
                    <div class="d-flex align-center ga-3">
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-comment-text-outline" size="14" class="mr-1"></v-icon>
                        {{ catalog.commentCount }} 评论
                      </div>
                      <div class="d-flex align-center">
                        <v-icon icon="mdi-cards-outline" size="14" class="mr-1"></v-icon>
                        {{ catalog.deckCount }} 卡片组
                      </div>
                    </div>
                    <div>{{ formatDate(catalog.createdAt) }}</div>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-folder-multiple" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-2">
            {{ searchQuery ? '未找到匹配的目录' : '暂无创建的目录' }}
          </p>
          <p class="text-body-2 text-grey">
            {{ searchQuery ? '尝试使用其他关键词搜索' : '创建目录来组织您的学习内容' }}
          </p>
        </div>

        <!-- 删除确认对话框 -->
        <ConfirmDialog
          v-model="showDeleteDialog"
          title="确认删除"
          message="确定要删除该目录吗？此操作不可恢复。"
          confirm-text="确认删除"
          @confirm="confirmDelete"
        />
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

// 搜索关键词
const searchQuery = ref('')

// 删除确认对话框
const showDeleteDialog = ref(false)
const catalogToDelete = ref<number | null>(null)

// Mock 目录数据
const catalogs = ref([
  {
    id: 1,
    name: '前端开发精选',
    description: '前端开发相关的优质课程和文章合集',
    commentCount: 24,
    deckCount: 12,
    createdAt: '2024-10-15',
    course: { id: 101, name: 'Vue 3 完整教程' }
  },
  {
    id: 2,
    name: 'JavaScript 进阶',
    description: 'JavaScript 高级主题和最佳实践',
    commentCount: 18,
    deckCount: 8,
    createdAt: '2024-10-20',
    course: { id: 102, name: 'TypeScript 进阶' }
  },
  {
    id: 3,
    name: '个人学习笔记',
    description: '日常学习记录和心得体会',
    commentCount: 45,
    deckCount: 23,
    createdAt: '2024-09-05',
    course: { id: 103, name: 'Python 数据分析' }
  }
])

// 根据搜索关键词过滤目录
const filteredCatalogs = computed(() => {
  if (!searchQuery.value) {
    return catalogs.value
  }

  const query = searchQuery.value.toLowerCase()
  return catalogs.value.filter(catalog => {
    // 搜索目录名称
    const matchName = catalog.name.toLowerCase().includes(query)
    // 搜索目录描述
    const matchDescription = catalog.description.toLowerCase().includes(query)
    // 搜索课程名称
    const matchCourse = catalog.course?.name.toLowerCase().includes(query)

    return matchName || matchDescription || matchCourse
  })
})

// 跳转到目录详情
const goToCatalog = (catalogId: number) => {
  // TODO: 跳转到目录详情页
  console.log('Go to catalog:', catalogId)
}

// 跳转到课程详情
const goToCourse = (courseId: number) => {
  // TODO: 跳转到课程详情页
  console.log('Go to course:', courseId)
}

// 删除目录
const deleteCatalog = (catalogId: number) => {
  catalogToDelete.value = catalogId
  showDeleteDialog.value = true
}

// 确认删除
const confirmDelete = () => {
  if (catalogToDelete.value !== null) {
    const index = catalogs.value.findIndex(c => c.id === catalogToDelete.value)
    if (index !== -1) {
      // TODO: 调用 API 删除目录
      catalogs.value.splice(index, 1)
    }
  }
  catalogToDelete.value = null
}

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString('zh-CN')
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
