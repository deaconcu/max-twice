<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 - 宽度不够时隐藏 -->
    <v-col cols="12" md="2" class="d-none d-lg-block">
      <div class="sticky-sidebar">
        <div class="pa-3 pa-md-4">
          <div class="mb-4">
            <h4 class="text-body-1 text-md-h6 font-weight-bold text-grey-darken-4 mb-2">关注的课程</h4>
            <p class="text-caption text-md-body-2 text-grey mb-0">管理您关注的所有课程，获取最新动态。</p>
          </div>
          <v-divider class="my-3 my-md-4" />
          <div class="text-caption text-md-body-2 text-grey">
            <div class="d-flex align-start mb-2 mb-md-3">
              <v-icon icon="mdi-bell-outline" size="16" color="grey" class="mr-2 mt-1" />
              <span>接收课程更新通知</span>
            </div>
            <div class="d-flex align-start mb-2 mb-md-3">
              <v-icon icon="mdi-heart" size="16" color="grey" class="mr-2 mt-1" />
              <span>快速取消关注</span>
            </div>
            <div class="d-flex align-start">
              <v-icon icon="mdi-view-grid" size="16" color="grey" class="mr-2 mt-1" />
              <span>网格化浏览</span>
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" lg="10">
      <div class="pa-0 pa-sm-2">
        <!-- 顶部操作栏 -->
        <div class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between mb-4 mb-md-6 ga-3">
          <div></div>
          <v-btn color="primary" variant="text" rounded="lg" :size="$vuetify.display.mobile ? 'small' : 'default'" to="/learning">
            <span class="d-none d-sm-inline">浏览更多课程</span>
            <span class="d-sm-none">浏览更多</span>
            <v-icon icon="mdi-chevron-right" :size="$vuetify.display.mobile ? 16 : 18" class="ml-1" />
          </v-btn>
        </div>

        <!-- 课程网格 -->
        <div v-if="formattedSubscriptions.length > 0">
          <v-row>
            <v-col v-for="course in formattedSubscriptions" :key="course.id" cols="12" md="6">
              <v-card rounded="xl" hover border elevation="0" class="subscription-card hoverable">
                <v-card-text class="pa-4 pa-sm-6" @click="goToCourse(course.courseId)">
                  <!-- 课程图标和取消关注按钮 -->
                  <div class="d-flex align-start justify-space-between mb-3 mb-md-4">
                    <div class="d-flex align-center flex-grow-1">
                      <v-avatar
                        :color="course.course.iconColor"
                        :size="$vuetify.display.mobile ? 48 : 56"
                        rounded="lg"
                        class="mr-3 mr-sm-4 flex-shrink-0"
                      >
                        <v-icon :icon="course.course.icon" color="white" :size="$vuetify.display.mobile ? 24 : 28" />
                      </v-avatar>
                      <div class="min-w-0">
                        <h4 class="text-body-1 text-md-h6 font-weight-bold mb-1 text-truncate">{{ course.course.name }}</h4>
                        <p class="text-caption text-grey mb-0">
                          {{ course.course.learnerCount || 0 }} 人学习
                        </p>
                      </div>
                    </div>
                    <v-btn
                      color="grey"
                      variant="text"
                      :size="$vuetify.display.mobile ? 'x-small' : 'small'"
                      icon="mdi-close"
                      @click.stop="unsubscribe(course.courseId)"
                    />
                  </div>

                  <!-- 课程描述 -->
                  <p class="text-caption text-md-body-2 text-grey-darken-2 mb-0 course-description">
                    {{ course.course.description }}
                  </p>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-8 py-md-12">
          <v-icon icon="mdi-book-multiple" :size="$vuetify.display.mobile ? 48 : 64" color="grey-lighten-2" class="mb-3 mb-md-4" />
          <p class="text-body-2 text-md-body-1 text-grey-darken-2">暂无关注的课程</p>
          <p class="text-caption text-md-body-2 text-grey">关注感兴趣的课程，及时获取更新</p>
          <v-btn
            color="primary"
            variant="flat"
            rounded="md"
            :size="$vuetify.display.mobile ? 'small' : 'default'"
            class="mt-4"
            to="/learning"
          >
            <v-icon icon="mdi-plus" :size="$vuetify.display.mobile ? 16 : 18" class="mr-2" />
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
import { useUserStore } from '@/stores/modules/user'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { subscriptionApi } from '@/api'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const router = useRouter()
const userStore = useUserStore()

// 获取订阅课程数据
const {
  data: subscriptions,
  loading,
  execute: fetchSubscriptions,
} = useFetch({
  fetchFn: () => {
    const userId = userStore.userId
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

  // 后端直接返回 Course[] 而不是 UserCourse[]
  return subscriptions.value.map((course, index) => {
    return {
      id: course.id,
      courseId: course.id,
      course: {
        id: course.id,
        name: course.name || '未知课程',
        description: course.description || '暂无描述',
        icon: 'mdi-school',
        iconColor: '#42b883',
        learnerCount: course.learnerCount || 0,
        category: '未分类',
      },
      order: index + 1,
    }
  })
})

// 取消关注对话框
const showUnsubscribeDialog = ref(false)
const unsubscribeCourseId = ref<number | null>(null)

// 跳转到课程阅读页
const goToCourse = (courseId: number) => {
  router.push({
    path: '/read',
    query: {
      courseId: String(courseId),
    },
  })
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
  top: 140px;
  align-self: flex-start;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
}

.subscription-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: rgb(var(--v-theme-surface));
  border: 1.5px solid rgb(var(--v-theme-outline)) !important;
}

.course-description {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 32px;
}

@media (min-width: 600px) {
  .course-description {
    min-height: 40px;
  }
}

.min-w-0 {
  min-width: 0;
}
</style>
