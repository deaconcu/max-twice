<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">关注的课程</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">管理您关注的所有课程，获取最新动态。</p>
          <v-divider class="my-3" />
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-bell-outline" size="14" class="mr-1" />
              接收课程更新通知
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-heart" size="14" class="mr-1" />
              快速取消关注
            </div>
            <div>
              <v-icon icon="mdi-view-grid" size="14" class="mr-1" />
              网格化浏览
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

          <v-btn color="primary" variant="text" rounded="md" density="compact" to="/learning">
            浏览更多课程
            <v-icon icon="mdi-chevron-right" class="ml-1" />
          </v-btn>
        </div>

        <!-- 课程网格 -->
        <div v-if="formattedSubscriptions.length > 0">
          <v-row>
            <v-col
              v-for="course in formattedSubscriptions"
              :key="course.id"
              cols="12"
              sm="6"
              md="4"
              lg="3"
            >
              <v-card border rounded="lg" hover class="hoverable">
                <v-card-text class="pa-4" @click="goToCourse(course.courseId)">
                  <!-- 课程图标和取消关注按钮 -->
                  <div class="d-flex align-start justify-space-between mb-3">
                    <v-avatar :color="course.course.iconColor" size="48" rounded="md">
                      <v-icon :icon="course.course.icon" color="white" size="24" />
                    </v-avatar>
                    <v-btn
                      color="grey"
                      variant="tonal"
                      size="x-small"
                      icon="mdi-close"
                      @click.stop="unsubscribe(course.courseId)"
                    />
                  </div>

                  <!-- 课程信息 -->
                  <h4 class="text-body-1 font-weight-bold mb-2 text-truncate">
                    {{ course.course.name }}
                  </h4>
                  <p class="text-caption text-grey mb-3 course-description">
                    {{ course.course.description }}
                  </p>

                  <!-- 统计信息 -->
                  <div class="d-flex align-center justify-space-between">
                    <div class="d-flex align-center text-caption text-grey">
                      <v-icon icon="mdi-account-multiple" size="14" class="mr-1" />
                      {{ course.course.learnerCount || 0 }} 人学习
                    </div>
                    <v-chip
                      v-if="course.course.category"
                      size="x-small"
                      variant="outlined"
                      color="grey"
                    >
                      {{ course.course.category }}
                    </v-chip>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-book-multiple" size="64" color="grey-lighten-2" class="mb-4" />
          <p class="text-body-1 text-grey-darken-2">暂无关注的课程</p>
          <p class="text-body-2 text-grey">关注感兴趣的课程，及时获取更新</p>
          <v-btn
            color="primary"
            variant="flat"
            rounded="md"
            density="compact"
            class="mt-4"
            to="/learning"
          >
            <v-icon icon="mdi-plus" size="18" class="mr-2" />
            浏览课程
          </v-btn>
        </div>

        <!-- 取消关注确认对话框 -->
        <ConfirmDialog
          v-model="showUnsubscribeDialog"
          title="确认取消关注"
          message="确定要取消关注该课程吗？"
          confirm-text="确认取消"
          @confirm="confirmUnsubscribe"
        />
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { subscriptionApi } from '@/api'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()
const authStore = useAuthStore()

// 获取订阅课程数据
const {
  data: subscriptions,
  loading,
  execute: fetchSubscriptions,
} = useFetch({
  fetchFn: () => {
    const userId = authStore.user?.id
    if (!userId) throw new Error('User ID not found')
    return subscriptionApi.getUserSubscriptions(userId)
  },
  immediate: true,
  defaultValue: [],
})

// 取消订阅
const { execute: unsubscribeAction } = useMutation(
  (courseId: number) => subscriptionApi.unsubscribe(courseId),
  {
    successMessage: '已取消关注该课程',
    onSuccess: () => {
      fetchSubscriptions()
    },
  }
)

// 转换订阅数据为组件所需格式
const formattedSubscriptions = computed(() => {
  if (!subscriptions.value) return []

  return subscriptions.value.map((userCourse, index) => {
    const course = userCourse.course
    return {
      id: userCourse.id,
      courseId: course?.id || 0,
      course: {
        id: course?.id || 0,
        name: course?.name || '未知课程',
        description: course?.description || '暂无描述',
        icon: 'mdi-school',
        iconColor: '#42b883',
        learnerCount: course?.learnerCount || 0,
        category: '未分类',
      },
      order: index + 1,
    }
  })
})

// 取消关注对话框
const showUnsubscribeDialog = ref(false)
const unsubscribeCourseId = ref<number | null>(null)

// 跳转到课程详情
const goToCourse = (courseId: number) => {
  router.push(`/course/${courseId}`)
}

// 取消关注
const unsubscribe = (courseId: number) => {
  unsubscribeCourseId.value = courseId
  showUnsubscribeDialog.value = true
}

// 确认取消关注
const confirmUnsubscribe = async () => {
  if (unsubscribeCourseId.value !== null) {
    await unsubscribeAction(unsubscribeCourseId.value)
  }
  unsubscribeCourseId.value = null
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

.course-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 32px;
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
