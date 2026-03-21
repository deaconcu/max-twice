<script lang="ts">
export default {
  name: 'OverviewTab',
}
</script>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { UserStatsDTO } from '@/types/user'

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
        <h3 class="section-title">学习进度</h3>
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
                    <div class="text-body-2 text-grey">职业路线</div>
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
                  学习中 {{ learningProgress.inProgressRoles }}
                </span>
                <span>
                  <v-icon icon="mdi-check-circle-outline" size="14" class="mr-1" />
                  已完成 {{ learningProgress.completedRoles }}
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
                    <div class="text-body-2 text-grey">课程</div>
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
                  学习中 {{ learningProgress.learningCourses }}
                </span>
                <span>
                  <v-icon icon="mdi-check-circle-outline" size="14" class="mr-1" />
                  已完成 {{ learningProgress.completedCourses }}
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
        <h3 class="section-title">学习记录</h3>
      </div>
      <v-row>
        <v-col cols="6" sm="3">
          <v-card class="streak-card" rounded="lg" elevation="0">
            <v-card-text class="text-center pa-4">
              <div class="text-h4 font-weight-bold text-primary mb-1">
                {{ streakData.learningDays }}
              </div>
              <div class="text-caption text-grey">连续学习(天)</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="3">
          <v-card class="streak-card" rounded="lg" elevation="0">
            <v-card-text class="text-center pa-4">
              <div class="text-h4 font-weight-bold text-success mb-1">
                {{ streakData.reviewDays }}
              </div>
              <div class="text-caption text-grey">连续复习(天)</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="3">
          <v-card class="streak-card" rounded="lg" elevation="0">
            <v-card-text class="text-center pa-4">
              <div class="text-h4 font-weight-bold text-grey-darken-2 mb-1">
                {{ streakData.totalLearningDays }}
              </div>
              <div class="text-caption text-grey">累计学习(天)</div>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="3">
          <v-card class="streak-card" rounded="lg" elevation="0">
            <v-card-text class="text-center pa-4">
              <div class="text-h4 font-weight-bold text-grey-darken-2 mb-1">
                {{ streakData.totalReviewDays }}
              </div>
              <div class="text-caption text-grey">累计复习(天)</div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- 去复习按钮 -->
      <div v-if="isOwnProfile" class="mt-4 text-center">
        <v-btn
          color="primary"
          variant="tonal"
          rounded="lg"
          @click="goToReview"
        >
          <v-icon icon="mdi-cards-outline" class="mr-2" />
          去复习
        </v-btn>
      </div>
    </div>

    <!-- 创作概览 -->
    <div class="section">
      <div class="section-header">
        <h3 class="section-title">创作内容</h3>
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
              <div class="text-caption text-grey">文章</div>
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
              <div class="text-caption text-grey">目录</div>
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
              <div class="text-caption text-grey">路线图</div>
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
              <div class="text-caption text-grey">卡片组</div>
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
  margin-bottom: 32px;
}

.section-header {
  margin-bottom: 16px;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: rgb(var(--v-theme-on-surface));
  margin: 0;
}

.progress-card,
.streak-card,
.creation-card {
  height: 100%;
  border: 1px solid rgb(var(--v-theme-border));
  background: rgb(var(--v-theme-surface));
}

.clickable {
  cursor: pointer;
  transition: all 0.2s ease;
}

.clickable:hover {
  border-color: rgb(var(--v-theme-primary));
  transform: translateY(-2px);
}

.streak-card {
  background: linear-gradient(135deg, rgba(var(--v-theme-surface), 1) 0%, rgba(var(--v-theme-surface-variant), 0.3) 100%);
}
</style>
