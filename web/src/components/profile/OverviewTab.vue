<script lang="ts">
export default {
  name: 'OverviewTab',
}
</script>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import type { UserStatsDTO } from '@/types/user'

const { t } = useI18n()

interface Props {
  userStats: UserStatsDTO | null
  isOwnProfile: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'navigate', tab: string, mode: string): void
}>()

const router = useRouter()

// 学习中的职业和课程
const learningProgress = computed(() => ({
  inProgressRoles: props.userStats?.inProgressProfessionCount || 0,
  completedRoles: props.userStats?.completedProfessionCount || 0,
  learningCourses: props.userStats?.learningCourseCount || 0,
  completedCourses: props.userStats?.completedCourseCount || 0,
}))

// 连续学习数据
const streakData = computed(() => ({
  learningDays: props.userStats?.learningStreakDays || 0,
  reviewDays: props.userStats?.reviewStreakDays || 0,
  totalLearningDays: props.userStats?.totalLearningDays || 0,
  totalReviewDays: props.userStats?.totalReviewDays || 0,
}))

// 创作数据
const creationData = computed(() => ({
  articles: props.userStats?.createdArticleCount || 0,
  catalogs: props.userStats?.createdIndexCount || 0,
  roadmaps: props.userStats?.createdRoadmapCount || 0,
  decks: props.userStats?.createdCardDeckCount || 0,
}))

// 导航到具体 Tab
const navigateTo = (tab: string, mode: string) => {
  emit('navigate', tab, mode)
}

// 跳转到复习页面
const goToReview = () => {
  router.push('/review')
}
</script>

<template>
  <div class="overview-container">
    <!-- 学习进度概览 -->
    <div class="section">
      <div class="section-header">
        <h3 class="section-title">{{ t('profile.overview.learningProgress') }}</h3>
      </div>
      <v-row>
        <!-- 职业学习 -->
        <v-col cols="12" sm="6">
          <v-card
            class="progress-card clickable"
            rounded="lg"
            elevation="0"
            @click="navigateTo('roles', 'learner')"
          >
            <v-card-text class="pa-4">
              <div class="d-flex align-center justify-space-between mb-3">
                <div class="d-flex align-center">
                  <v-avatar color="primary" size="40" class="mr-3">
                    <v-icon icon="mdi-briefcase-outline" color="white" />
                  </v-avatar>
                  <div>
                    <div class="text-body-2 text-grey">{{ t('profile.overview.roleRoutes') }}</div>
                    <div class="text-h5 font-weight-bold">
                      {{ learningProgress.inProgressRoles + learningProgress.completedRoles }}
                    </div>
                  </div>
                </div>
                <v-icon icon="mdi-chevron-right" color="grey" />
              </div>
              <div class="d-flex ga-4 text-caption text-grey">
                <span>
                  <v-icon icon="mdi-clock-outline" size="14" class="mr-1" />
                  {{ t('profile.overview.inProgress', { count: learningProgress.inProgressRoles }) }}
                </span>
                <span>
                  <v-icon icon="mdi-check-circle-outline" size="14" class="mr-1" />
                  {{ t('profile.overview.completed', { count: learningProgress.completedRoles }) }}
                </span>
              </div>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- 课程学习 -->
        <v-col cols="12" sm="6">
          <v-card
            class="progress-card clickable"
            rounded="lg"
            elevation="0"
            @click="navigateTo('courses-learning', 'learner')"
          >
            <v-card-text class="pa-4">
              <div class="d-flex align-center justify-space-between mb-3">
                <div class="d-flex align-center">
                  <v-avatar color="success" size="40" class="mr-3">
                    <v-icon icon="mdi-book-open-page-variant-outline" color="white" />
                  </v-avatar>
                  <div>
                    <div class="text-body-2 text-grey">{{ t('profile.overview.courses') }}</div>
                    <div class="text-h5 font-weight-bold">
                      {{ learningProgress.learningCourses + learningProgress.completedCourses }}
                    </div>
                  </div>
                </div>
                <v-icon icon="mdi-chevron-right" color="grey" />
              </div>
              <div class="d-flex ga-4 text-caption text-grey">
                <span>
                  <v-icon icon="mdi-clock-outline" size="14" class="mr-1" />
                  {{ t('profile.overview.inProgress', { count: learningProgress.learningCourses }) }}
                </span>
                <span>
                  <v-icon icon="mdi-check-circle-outline" size="14" class="mr-1" />
                  {{ t('profile.overview.completed', { count: learningProgress.completedCourses }) }}
                </span>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>

    <!-- 连续学习 -->
    <div class="section">
      <div class="section-header">
        <h3 class="section-title">{{ t('profile.overview.learningRecord') }}</h3>
      </div>
      <v-row>
        <v-col cols="6" sm="3">
          <v-card class="streak-card" rounded="lg" elevation="0">
            <v-card-text class="text-center pa-4">
              <div class="text-h4 font-weight-bold text-primary mb-1">
                {{ streakData.learningDays }}
              </div>
              <div class="text-caption text-grey">{{ t('profile.overview.consecutiveLearning') }}</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="3">
          <v-card class="streak-card" rounded="lg" elevation="0">
            <v-card-text class="text-center pa-4">
              <div class="text-h4 font-weight-bold text-success mb-1">
                {{ streakData.reviewDays }}
              </div>
              <div class="text-caption text-grey">{{ t('profile.overview.consecutiveReview') }}</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="3">
          <v-card class="streak-card" rounded="lg" elevation="0">
            <v-card-text class="text-center pa-4">
              <div class="text-h4 font-weight-bold text-grey-darken-2 mb-1">
                {{ streakData.totalLearningDays }}
              </div>
              <div class="text-caption text-grey">{{ t('profile.overview.totalLearning') }}</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="3">
          <v-card class="streak-card" rounded="lg" elevation="0">
            <v-card-text class="text-center pa-4">
              <div class="text-h4 font-weight-bold text-grey-darken-2 mb-1">
                {{ streakData.totalReviewDays }}
              </div>
              <div class="text-caption text-grey">{{ t('profile.overview.totalReview') }}</div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- 去复习按钮 -->
      <div v-if="isOwnProfile" class="mt-4">
        <v-btn
          color="primary"
          variant="tonal"
          rounded="lg"
          @click="goToReview"
        >
          <v-icon icon="mdi-cards-outline" class="mr-2" />
          {{ t('profile.overview.goReview') }}
        </v-btn>
      </div>
    </div>

    <!-- 创作概览 -->
    <div class="section">
      <div class="section-header">
        <h3 class="section-title">{{ t('profile.overview.creationContent') }}</h3>
      </div>
      <v-row>
        <v-col cols="6" sm="3">
          <v-card
            class="creation-card clickable"
            rounded="lg"
            elevation="0"
            @click="navigateTo('articles', 'creator')"
          >
            <v-card-text class="text-center pa-4">
              <v-icon icon="mdi-file-document-outline" size="28" color="primary" class="mb-2" />
              <div class="text-h5 font-weight-bold mb-1">{{ creationData.articles }}</div>
              <div class="text-caption text-grey">{{ t('profile.overview.articles') }}</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="3">
          <v-card
            class="creation-card clickable"
            rounded="lg"
            elevation="0"
            @click="navigateTo('catalogs', 'creator')"
          >
            <v-card-text class="text-center pa-4">
              <v-icon icon="mdi-format-list-bulleted" size="28" color="success" class="mb-2" />
              <div class="text-h5 font-weight-bold mb-1">{{ creationData.catalogs }}</div>
              <div class="text-caption text-grey">{{ t('profile.overview.catalogs') }}</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="3">
          <v-card
            class="creation-card clickable"
            rounded="lg"
            elevation="0"
            @click="navigateTo('roadmaps', 'creator')"
          >
            <v-card-text class="text-center pa-4">
              <v-icon icon="mdi-map-marker-path" size="28" color="warning" class="mb-2" />
              <div class="text-h5 font-weight-bold mb-1">{{ creationData.roadmaps }}</div>
              <div class="text-caption text-grey">{{ t('profile.overview.roadmaps') }}</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="3">
          <v-card
            class="creation-card clickable"
            rounded="lg"
            elevation="0"
            @click="navigateTo('decks', 'creator')"
          >
            <v-card-text class="text-center pa-4">
              <v-icon icon="mdi-cards" size="28" color="info" class="mb-2" />
              <div class="text-h5 font-weight-bold mb-1">{{ creationData.decks }}</div>
              <div class="text-caption text-grey">{{ t('profile.overview.decks') }}</div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </div>
  </div>
</template>

<style scoped>
.overview-container {
  padding: 0;
}

.section {
  margin-bottom: 24px;
}

.section-header {
  margin-bottom: 10px;
}

.section-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: rgb(var(--v-theme-on-surface-variant));
  margin: 0;
}

.progress-card,
.streak-card,
.creation-card {
  height: 100%;
  border: 1px solid rgba(var(--v-theme-on-surface), 0.08);
  background: rgb(var(--v-theme-surface));
}

.progress-card :deep(.v-card-text) {
  padding: 14px !important;
}

.progress-card :deep(.v-avatar) {
  width: 34px !important;
  height: 34px !important;
}

.progress-card :deep(.v-avatar .v-icon) {
  font-size: 18px !important;
}

.progress-card :deep(.text-h5) {
  font-size: 1.2rem !important;
}

.streak-card :deep(.v-card-text) {
  padding: 14px 8px !important;
}

.streak-card :deep(.text-h4) {
  font-size: 1.4rem !important;
}

.creation-card :deep(.v-card-text) {
  padding: 14px 8px !important;
}

.creation-card :deep(.v-icon) {
  margin-bottom: 6px !important;
}

.creation-card :deep(.text-h5) {
  font-size: 1.1rem !important;
}

.clickable {
  cursor: pointer;
  transition: all 0.15s ease;
}

.clickable:hover {
  border-color: rgb(var(--v-theme-primary)) !important;
  background: rgba(var(--v-theme-primary), 0.04) !important;
}

.streak-card {
  background: rgb(var(--v-theme-surface));
}
</style>
