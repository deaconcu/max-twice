<script setup lang="ts">
  import { inject, onMounted, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { useI18n } from 'vue-i18n'
  import { courseServiceV1 } from '@/services/api/v1/apiServiceV1'

  interface Course {
    id: string | number
    name: string
    description?: string
    learnerCount?: number
    subscriptionCount?: number
    [key: string]: any
  }

  interface SortOption {
    value: string
    title: string
  }

  const { t } = useI18n()
  const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')
  const router = useRouter()

  const courses = ref<Course[]>([])
  const loading = ref<boolean>(false)
  const selectedSortBy = ref<string>('total')

  const sortOptions: SortOption[] = [
    { value: 'total', title: t('hotRanking.sortOptions.total') },
    { value: 'learning', title: t('hotRanking.sortOptions.learning') },
    { value: 'subscription', title: t('hotRanking.sortOptions.subscription') },
  ]

  onMounted((): void => {
    loadHotCoursesRanking()
  })

  const loadHotCoursesRanking = async (): Promise<void> => {
    try {
      loading.value = true
      console.log('加载热门课程排行榜')
      const response = await courseServiceV1.getCoursesRanking()

      console.log('完整API响应:', response)
      console.log('响应码:', response.code)
      console.log('响应数据:', response.data)
      console.log('数据类型:', typeof response.data)
      console.log('是否为数组:', Array.isArray(response.data))
      if (response.data) {
        console.log('数据长度:', response.data.length)
      }

      if (response.code === 401) {
        console.log('未登录')
        courses.value = []
      } else if (response.code === 200) {
        console.log('获取热门课程排行榜数据:', response.data)
        courses.value = response.data || []
      } else {
        console.error('获取排行榜失败:', response)
        showSnackbar?.(t('hotRanking.getRankingFailed'), 'error')
        courses.value = []
      }
    } catch (error) {
      console.error('Error loading hot courses ranking:', error)
      showSnackbar?.(t('hotRanking.networkError'), 'error')
      courses.value = []
    } finally {
      loading.value = false
    }
  }

  const sortCourses = (): void => {
    const sortedCourses = [...courses.value]

    switch (selectedSortBy.value) {
      case 'learning':
        sortedCourses.sort((a, b) => (b.learnerCount || 0) - (a.learnerCount || 0))
        break
      case 'subscription':
        sortedCourses.sort((a, b) => (b.subscriptionCount || 0) - (a.subscriptionCount || 0))
        break
      case 'total':
      default:
        sortedCourses.sort(
          (a, b) =>
            (b.learnerCount || 0) +
            (b.subscriptionCount || 0) -
            ((a.learnerCount || 0) + (a.subscriptionCount || 0))
        )
        break
    }

    courses.value = sortedCourses
  }

  const openCourse = (courseId: string | number): void => {
    const url = router.resolve({ path: '/read', query: { courseId } }).href
    window.open(url, '_blank')
  }

  const goBack = (): void => {
    router.push('/course/list')
  }

  const getRankIcon = (index: number): string | null => {
    if (index === 0) return 'mdi-trophy'
    if (index === 1) return 'mdi-medal'
    if (index === 2) return 'mdi-medal-outline'
    return null
  }

  const getRankColor = (index: number): string => {
    if (index === 0) return 'amber'
    if (index === 1) return 'amber'
    if (index === 2) return 'amber'
    return 'grey-lighten-3'
  }
</script>

<template>
  <v-container fluid>
    <v-row class="mt-2">
      <v-col cols="12">
        <!-- 页面头部 -->
        <div class="d-flex align-center justify-space-between mb-6">
          <div class="d-flex align-center">
            <v-btn
              icon="mdi-arrow-left"
              variant="text"
              color="grey-darken-2"
              class="mr-3"
              @click="goBack"
            ></v-btn>
            <div>
              <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">
                {{ t('hotRanking.title') }}
              </h1>
              <p class="text-body-2 text-grey-darken-2 mb-0">
                <v-icon icon="mdi-fire" color="primary" size="16" class="mr-1"></v-icon>
                {{ t('hotRanking.subtitle') }}
              </p>
            </div>
          </div>

          <!-- 排序选择器 -->
          <div class="d-flex align-center">
            <span class="text-body-2 text-grey-darken-2 mr-3">{{ t('hotRanking.sortBy') }}</span>
            <v-select
              v-model="selectedSortBy"
              :items="sortOptions"
              item-title="title"
              item-value="value"
              variant="outlined"
              density="compact"
              hide-details
              class="sort-select"
              @update:model-value="sortCourses"
            ></v-select>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
          <p class="text-body-1 text-grey-darken-2 mt-4">{{ t('hotRanking.loading') }}</p>
        </div>

        <!-- 排行榜内容 -->
        <div v-else-if="courses.length > 0">
          <!-- 前三名特殊显示 -->
          <v-card flat color="grey-lighten-5" rounded="xl" class="mb-6">
            <v-card-text class="pa-6">
              <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-4 text-center">
                {{ t('hotRanking.topThree') }}
              </h3>
              <v-row class="mb-2">
                <v-col v-for="(course, index) in courses.slice(0, 3)" :key="course.id" cols="4">
                  <div
                    class="top-card text-center pa-4 rounded-lg"
                    :class="{
                      'winner-card': index === 0,
                      'second-place-card': index === 1,
                      'third-place-card': index === 2,
                    }"
                    @click="openCourse(course.id)"
                  >
                    <v-avatar :color="getRankColor(index)" size="40" class="mb-3">
                      <v-icon
                        v-if="getRankIcon(index)"
                        :icon="getRankIcon(index)"
                        color="white"
                        size="20"
                      ></v-icon>
                      <span v-else class="text-white font-weight-bold">{{ index + 1 }}</span>
                    </v-avatar>

                    <h4 class="text-h6 font-weight-bold text-grey-darken-4 mb-2">
                      {{ course.name }}
                    </h4>

                    <div class="text-body-2 text-grey-darken-2 mb-3">
                      {{ course.description || t('hotRanking.noDescription') }}
                    </div>

                    <div class="d-flex justify-space-around">
                      <div class="text-center">
                        <div class="text-h6 font-weight-bold text-primary">
                          {{ (course.learnerCount || 0).toLocaleString() }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          {{ t('hotRanking.learners') }}
                        </div>
                      </div>
                      <div class="text-center">
                        <div class="text-h6 font-weight-bold text-success">
                          {{ (course.subscriptionCount || 0).toLocaleString() }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          {{ t('hotRanking.subscribers') }}
                        </div>
                      </div>
                    </div>
                  </div>
                </v-col>
              </v-row>
            </v-card-text>
          </v-card>

          <!-- 完整排行榜列表 -->
          <v-card flat color="grey-lighten-5" rounded="xl">
            <v-card-text class="pa-6">
              <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-4">
                {{ t('hotRanking.fullRanking', { count: courses.length }) }}
              </h3>

              <v-list bg-color="transparent" class="pa-0">
                <v-list-item
                  v-for="(course, index) in courses"
                  :key="course.id"
                  class="ranking-item ma-1 pa-4 rounded-lg"
                  :class="index < 3 ? 'top-three-item' : 'regular-item'"
                  @click="openCourse(course.id)"
                >
                  <template #prepend>
                    <div class="rank-number mr-4 text-center rank-number-container">
                      <v-avatar v-if="index < 3" :color="getRankColor(index)" size="32">
                        <v-icon
                          v-if="getRankIcon(index)"
                          :icon="getRankIcon(index)"
                          color="white"
                          size="16"
                        ></v-icon>
                      </v-avatar>
                      <div
                        v-else
                        class="text-h6 font-weight-bold"
                        :class="index < 10 ? 'text-grey-darken-2' : 'text-grey-lighten-1'"
                      >
                        {{ index + 1 }}
                      </div>
                    </div>
                  </template>

                  <v-list-item-title class="text-h6 font-weight-medium">
                    {{ course.name }}
                  </v-list-item-title>

                  <v-list-item-subtitle class="text-body-2 mt-1">
                    {{ course.description || t('hotRanking.noDescription') }}
                  </v-list-item-subtitle>

                  <template #append>
                    <div class="d-flex align-center">
                      <div class="text-center mr-6">
                        <div class="text-h6 font-weight-bold text-primary">
                          {{ (course.learnerCount || 0).toLocaleString() }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          <v-icon icon="mdi-school" size="12" class="mr-1"></v-icon>
                          {{ t('hotRanking.learners') }}
                        </div>
                      </div>

                      <div class="text-center mr-6">
                        <div class="text-h6 font-weight-bold text-success">
                          {{ (course.subscriptionCount || 0).toLocaleString() }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          <v-icon icon="mdi-heart" size="12" class="mr-1"></v-icon>
                          {{ t('hotRanking.subscribers') }}
                        </div>
                      </div>

                      <div class="text-center">
                        <div class="text-h6 font-weight-bold text-grey-darken-2">
                          {{
                            (
                              (course.learnerCount || 0) + (course.subscriptionCount || 0)
                            ).toLocaleString()
                          }}
                        </div>
                        <div class="text-caption text-grey-darken-1">
                          <v-icon icon="mdi-trending-up" size="12" class="mr-1"></v-icon>
                          {{ t('hotRanking.total') }}
                        </div>
                      </div>

                      <v-icon icon="mdi-chevron-right" color="grey-lighten-1" class="ml-4"></v-icon>
                    </div>
                  </template>
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon
            icon="mdi-chart-line-stacked"
            size="64"
            color="grey-lighten-1"
            class="mb-4"
          ></v-icon>
          <h3 class="text-h5 font-weight-medium text-grey-darken-2 mb-2">
            {{ t('hotRanking.noData') }}
          </h3>
          <p class="text-body-1 text-grey-darken-1">{{ t('hotRanking.noDataDesc') }}</p>
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
  .top-card {
    cursor: pointer;
    transition: all 0.2s ease;
    background: white;
    border: 1px solid rgba(0, 0, 0, 0.06);
  }

  .top-card:hover {
    transform: translateY(-2px);
    border-color: rgba(25, 118, 210, 0.2);
  }

  .winner-card {
    border: 2px solid #ffc107;
    box-shadow: 0 0 8px rgba(255, 193, 7, 0.2);
  }

  .second-place-card {
    border: 2px solid #c0c0c0;
    box-shadow: 0 0 6px rgba(192, 192, 192, 0.15);
  }

  .third-place-card {
    border: 2px solid #cd7f32;
    box-shadow: 0 0 6px rgba(205, 127, 50, 0.15);
  }

  .ranking-item {
    cursor: pointer;
    transition: all 0.2s ease;
    border: 1px solid rgba(0, 0, 0, 0.04);
  }

  .ranking-item:hover {
    transform: translateX(4px);
    border-color: rgba(25, 118, 210, 0.3) !important;
  }

  .top-three-item {
    background: rgba(25, 118, 210, 0.02) !important;
    border-color: rgba(25, 118, 210, 0.08) !important;
  }

  .regular-item {
    background: white !important;
  }

  /* 改善字体渲染 */
  * {
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    text-rendering: optimizeLegibility;
  }

  /* 文字对比度 */
  .text-grey-darken-1,
  .text-grey-darken-2,
  .text-grey-darken-3,
  .text-grey-darken-4 {
    font-weight: 500 !important;
  }

  h1,
  h2,
  h3,
  h4,
  h5,
  h6 {
    font-weight: 700 !important;
    letter-spacing: -0.01em;
  }

  /* 为v-card添加细节 */
  .v-card {
    border: 1px solid rgba(0, 0, 0, 0.04);
  }

  .sort-select {
    width: 150px;
  }

  .rank-number-container {
    min-width: 40px;
  }
</style>