<script setup lang="ts">
  import { onMounted, ref } from 'vue'
  import { useI18n } from 'vue-i18n'
  import ProfessionManagement from '@/components/admin/ProfessionManagement.vue'
  import SystemConfiguration from '@/components/admin/SystemConfiguration.vue'
  import SystemOperations from '@/components/admin/SystemOperations.vue'
  import CourseManagement from '@/components/admin/CourseManagement.vue'
  import PostReview from '@/components/admin/PostReview.vue'
  import CommentReview from '@/components/admin/CommentReview.vue'
  import MemoryCardReview from '@/components/admin/MemoryCardReview.vue'
  import UserManagement from '@/components/admin/UserManagement.vue'

  const { t } = useI18n()
  const tab = ref<string>('system-config')

</script>

<template>
  <v-container class="ma-0 pa-0 bg-white" fluid>
    <!-- 页面头部 -->
    <div class="admin-header px-6 pb-4 pt-6">
      <div class="d-flex align-center">
        <v-avatar color="teal-lighten-1" size="30" class="mr-3">
          <v-icon icon="mdi-cog" color="white" size="22"></v-icon>
        </v-avatar>
        <div>
          <span class="text-h6 font-weight-bold text-grey-darken-3">{{ t('admin.title') }}</span>
          <br />
          <span class="text-body-2 text-grey-darken-1">{{ t('admin.subtitle') }}</span>
        </div>
      </div>
    </div>

    <div class="d-flex flex-row pt-4">
      <!-- 侧边栏 -->
      <div class="sidebar-container">
        <div class="pa-3 pb-2">
          <h3 class="text-subtitle-1 font-weight-bold text-grey-darken-2 mb-3">
            {{ t('admin.modules') }}
          </h3>
        </div>
        <v-tabs v-model="tab" color="teal" direction="vertical" class="px-2" flat>
          <v-tab
            prepend-icon="mdi-cog-outline"
            :text="t('admin.systemConfig')"
            value="system-config"
            class="text-body-1 text-grey-darken-2 my-2 rounded-lg justify-start"
          ></v-tab>
          <v-tab
            prepend-icon="mdi-cog-sync"
            :text="t('admin.systemOperations')"
            value="system-operations"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"
          ></v-tab>

          <div class="px-3 py-3">
            <div class="text-caption text-grey-darken-1 font-weight-medium mb-2">
              用户管理
            </div>
            <v-divider class="border-opacity-40"></v-divider>
          </div>

          <v-tab
            prepend-icon="mdi-account-multiple"
            text="用户管理"
            value="user-management"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"
          ></v-tab>

          <div class="px-3 py-3">
            <div class="text-caption text-grey-darken-1 font-weight-medium mb-2">
              {{ t('admin.courseManagement') }}
            </div>
            <v-divider class="border-opacity-40"></v-divider>
          </div>

          <v-tab
            prepend-icon="mdi-briefcase-check-outline"
            :text="t('admin.professionManagement')"
            value="profession-management"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"
          ></v-tab>
          <v-tab
            prepend-icon="mdi-email-outline"
            :text="t('admin.courseManagement')"
            value="course-management"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"
          ></v-tab>

          <div class="px-3 py-3">
            <div class="text-caption text-grey-darken-1 font-weight-medium mb-2">
              {{ t('admin.contentReview') }}
            </div>
            <v-divider class="border-opacity-40"></v-divider>
          </div>

          <v-tab
            prepend-icon="mdi-note-check-outline"
            :text="t('admin.reviewArticles')"
            value="post-review"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"
          ></v-tab>
          <v-tab
            prepend-icon="mdi-comment-check-outline"
            :text="t('admin.reviewComments')"
            value="comment-review"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"
          ></v-tab>
          <v-tab
            prepend-icon="mdi-cards-variant"
            text="审核记忆卡片"
            value="memory-card-review"
            class="text-body-1 text-grey-darken-2 my-1 rounded-lg justify-start"
          ></v-tab>
        </v-tabs>
      </div>

      <!-- 主内容区域 -->
      <div class="flex-grow-1 px-4 pl-6">
        <!-- 系统配置 -->
        <v-card v-if="tab == 'system-config'" flat class="pa-6" rounded="lg">
          <SystemConfiguration />
        </v-card>

        <!-- 系统操作 -->
        <v-card v-if="tab == 'system-operations'" flat class="pa-6" rounded="lg">
          <SystemOperations />
        </v-card>

        <!-- 职业申请管理 -->
        <v-card v-if="tab == 'profession-management'" flat class="pa-6" rounded="lg">
          <ProfessionManagement />
        </v-card>

        <!-- 课程申请管理 -->
        <v-card v-if="tab == 'course-management'" flat class="pa-6" rounded="lg">
          <CourseManagement />
        </v-card>

        <!-- 文章审核 -->
        <v-card v-if="tab == 'post-review'" flat class="pa-6" rounded="lg">
          <PostReview />
        </v-card>

        <!-- 评论审核 -->
        <v-card v-if="tab == 'comment-review'" flat class="pa-6" rounded="lg">
          <CommentReview />
        </v-card>

        <!-- 记忆卡片审核 -->
        <v-card v-if="tab == 'memory-card-review'" flat class="pa-6" rounded="lg">
          <MemoryCardReview />
        </v-card>

        <!-- 用户管理 -->
        <v-card v-if="tab == 'user-management'" flat class="pa-6" rounded="lg">
          <UserManagement />
        </v-card>
      </div>
    </div>
  </v-container>
</template>

<style scoped>
  /* 页面头部样式 */
  .admin-header {
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  }

  /* 侧边栏样式 */
  .sidebar-container {
    border-right: 1px solid rgba(0, 0, 0, 0.08);
    padding-right: 1rem;
    min-width: 260px;
  }

  /* Tab样式优化 */
  :deep(.v-tab--selected) {
    background-color: rgb(var(--v-theme-teal-lighten-5)) !important;
    color: rgb(var(--v-theme-teal-darken-2)) !important;
  }

  :deep(.v-tab) {
    color: rgb(var(--v-theme-grey-darken-1));
    margin-bottom: 4px;
    text-transform: none !important;
    font-weight: 500 !important;
    justify-content: flex-start !important;
    padding-left: 16px !important;
  }

  :deep(.v-tab:hover) {
    background-color: rgba(0, 150, 136, 0.04) !important;
  }

  /* 卡片边框样式 */
  .border {
    border: 1px solid rgba(0, 0, 0, 0.08) !important;
  }

  /* 卡片悬停效果 */
  :deep(.v-card[hover]:hover) {
    transform: translateY(-2px);
    transition: all 0.2s ease;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
  }

  /* 文章内容样式 */
  .post-content {
    max-height: 700px;
    overflow-y: auto;
    line-height: 1.6;
    padding: 2px 12px;
  }

  .post-content :deep(p) {
    margin-bottom: 8px;
  }

  .post-content :deep(h1),
  .post-content :deep(h2),
  .post-content :deep(h3) {
    margin-bottom: 8px;
    margin-top: 16px;
  }

  /* 行高优化 */
  .line-height-relaxed {
    line-height: 1.7;
  }

  /* 文本区域样式 */
  .config-textarea :deep(.v-field__input) {
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace !important;
    font-size: 13px !important;
    line-height: 1.5 !important;
  }

  :deep(.v-field__input) {
    font-size: 14px !important;
  }

  :deep(.tiptap) {
    line-height: 1.6;
  }

  .border-b {
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  }

  .border-dashed {
    border-style: dashed;
  }

  .w-85 {
    max-width: 85%;
  }
</style>