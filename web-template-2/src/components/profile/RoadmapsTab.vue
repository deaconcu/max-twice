<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">创建的路线图</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">
            规划您的学习路径，系统化掌握技能。
          </p>
          <v-divider class="my-3"></v-divider>
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-map" size="14" class="mr-1"></v-icon>
              绘制学习路线
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-share-variant" size="14" class="mr-1"></v-icon>
              分享给他人
            </div>
            <div>
              <v-icon icon="mdi-progress-check" size="14" class="mr-1"></v-icon>
              跟踪学习进度
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

        <!-- 路线图列表 -->
        <div v-if="roadmaps.length > 0">
          <v-row>
            <v-col
              v-for="roadmap in roadmaps"
              :key="roadmap.id"
              cols="12"
            >
              <v-card
                border
                rounded="lg"
                hover
                class="hoverable"
              >
                <v-card-text class="pa-4">
                  <div class="d-flex" style="gap: 16px;">
                    <!-- 左侧内容区 -->
                    <div class="flex-grow-1">
                      <!-- 路线图头部 -->
                      <div class="d-flex align-center justify-space-between mb-3">
                        <div class="d-flex align-center">
                          <v-avatar color="purple-lighten-5" size="40" rounded="md" class="mr-3">
                            <v-icon icon="mdi-map-marker-path" color="purple" size="20"></v-icon>
                          </v-avatar>
                          <div>
                            <h4 class="text-body-1 font-weight-bold mb-1">{{ roadmap.name }}</h4>
                            <p class="text-caption text-grey mb-0">{{ roadmap.profession }}</p>
                          </div>
                        </div>
                        <!-- 状态标签 -->
                        <v-chip
                          :color="getStatusColor(roadmap.status)"
                          size="small"
                          variant="flat"
                        >
                          {{ getStatusText(roadmap.status) }}
                        </v-chip>
                      </div>

                      <!-- 路线图描述 -->
                      <p class="text-body-2 text-grey-darken-2 mb-3">{{ roadmap.description }}</p>

                      <!-- 统计信息 -->
                      <div class="d-flex align-center mb-3 text-caption text-grey" style="gap: 16px;">
                        <div>
                          <v-icon icon="mdi-account-multiple" size="14" class="mr-1"></v-icon>
                          {{ roadmap.usageCount }} 人使用
                        </div>
                        <div>
                          <v-icon icon="mdi-star" size="14" class="mr-1"></v-icon>
                          {{ roadmap.starCount }} 收藏
                        </div>
                        <div>
                          <v-icon icon="mdi-timeline" size="14" class="mr-1"></v-icon>
                          {{ roadmap.nodeCount }} 节点
                        </div>
                      </div>

                      <!-- 操作按钮 -->
                      <div class="d-flex" style="gap: 8px;">
                        <v-btn
                          color="primary"
                          variant="outlined"
                          rounded="md"
                          density="compact"
                        >
                          <v-icon icon="mdi-eye" size="16" class="mr-1"></v-icon>
                          查看
                        </v-btn>
                        <v-btn
                          variant="text"
                          rounded="md"
                          density="compact"
                          color="grey"
                        >
                          <v-icon icon="mdi-pencil" size="16" class="mr-1"></v-icon>
                          编辑
                        </v-btn>
                        <v-btn
                          variant="text"
                          rounded="md"
                          density="compact"
                          color="error"
                        >
                          <v-icon icon="mdi-delete" size="16" class="mr-1"></v-icon>
                          删除
                        </v-btn>
                      </div>
                    </div>

                    <!-- 右侧缩略图 -->
                    <div class="roadmap-thumbnail">
                      <div class="d-flex align-center justify-center" style="width: 200px; height: 150px; background: #F5F5F5; border-radius: 8px;">
                        <v-icon icon="mdi-sitemap" size="48" color="grey-lighten-1"></v-icon>
                      </div>
                    </div>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

      <!-- 空状态 -->
      <div v-else class="text-center py-12">
        <v-icon icon="mdi-map-marker-path" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
        <p class="text-body-1 text-grey-darken-2">暂无创建的路线图</p>
        <p class="text-body-2 text-grey">创建学习路线图，规划职业发展路径</p>
      </div>
    </div>
  </v-col>
</v-row>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// Mock 路线图数据
const roadmaps = ref([
  {
    id: 1,
    name: '前端工程师学习路线',
    profession: '前端工程师',
    description: '从零基础到高级前端工程师的完整学习路径，包含 HTML、CSS、JavaScript、Vue、React 等技术栈',
    usageCount: 245,
    starCount: 89,
    nodeCount: 32,
    status: 'public',
    createdAt: '2024-09-15'
  },
  {
    id: 2,
    name: 'TypeScript 进阶路线',
    profession: '前端工程师',
    description: '深入学习 TypeScript 的类型系统和高级特性，提升代码质量和开发效率',
    usageCount: 156,
    starCount: 67,
    nodeCount: 24,
    status: 'public',
    createdAt: '2024-10-01'
  },
  {
    id: 3,
    name: '全栈开发学习计划',
    profession: '全栈工程师',
    description: '包含前端、后端、数据库、部署等全栈开发所需的核心技能',
    usageCount: 0,
    starCount: 0,
    nodeCount: 45,
    status: 'draft',
    createdAt: '2024-11-05'
  }
])

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    public: 'success',
    private: 'grey',
    draft: 'warning'
  }
  return colors[status] || 'grey'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    public: '公开',
    private: '私密',
    draft: '草稿'
  }
  return texts[status] || '未知'
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

.roadmap-thumbnail {
  cursor: pointer;
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
