<script setup lang="ts">
import { computed } from 'vue'
import type { ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

// 类型定义
interface Props {
  searchText: string
  selectedNavTab: string
}

interface Emits {
  (e: 'update:searchText', value: string): void
  (e: 'update:selectedNavTab', value: string): void
  (e: 'performSearch'): void
  (e: 'openCareerApplication'): void
}

const { t } = useI18n()
const router = useRouter()

// Props
const props = defineProps<Props>()

// Emits
const emit = defineEmits<Emits>()

// Computed properties for v-model
const searchTextModel: ComputedRef<string> = computed({
  get: () => props.searchText,
  set: (value: string) => emit('update:searchText', value),
})

const selectedNavTabModel: ComputedRef<string> = computed({
  get: () => props.selectedNavTab,
  set: (value: string) => emit('update:selectedNavTab', value),
})

// 处理搜索
const handleSearch = (): void => {
  emit('performSearch')
}

// 处理回车搜索
const handleEnterSearch = (): void => {
  emit('performSearch')
}

// 打开职业申请对话框
const handleOpenCareerApplication = (): void => {
  emit('openCareerApplication')
}

// 处理导航切换
const handleNavigation = (route: string): void => {
  router.push(route)
}
</script>

<template>
  <div class="mb-8">
    <!-- 页面头部 -->
    <v-row justify="start" class="mb-4">
      <v-col cols="12">
        <div class="d-flex align-center justify-space-between mb-3">
          <div class="d-flex align-center">
            <v-avatar color="teal-lighten-4" size="40" class="mr-3">
              <v-icon icon="mdi-briefcase-variant" color="teal-darken-2" size="20"></v-icon>
            </v-avatar>
            <div>
              <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">
                {{ t('careerCenter.title') }}
              </h1>
              <p class="text-body-2 text-grey-darken-2 mb-0">{{ t('careerCenter.subtitle') }}</p>
            </div>
          </div>

          <!-- 导航栏 -->
          <div class="d-flex align-center">
            <v-btn-toggle
              v-model="selectedNavTabModel"
              variant="text"
              color="primary"
              class="nav-toggle"
            >
              <v-btn value="learning" class="nav-btn" @click="handleNavigation('/learning')">
                {{ t('careerCenter.navigation.learning') }}
              </v-btn>
              <v-btn
                value="career"
                class="nav-btn"
                :class="{ 'nav-btn-active': selectedNavTab === 'career' }"
              >
                {{ t('careerCenter.navigation.career') }}
              </v-btn>
              <v-btn value="courses" class="nav-btn" @click="handleNavigation('/course/list')">
                {{ t('careerCenter.navigation.courses') }}
              </v-btn>
            </v-btn-toggle>
          </div>
        </div>
      </v-col>
    </v-row>

    <!-- 搜索和筛选区域 -->
    <v-row align="center" class="mb-6">
      <v-col cols="12">
        <div class="d-flex align-center search-container">
          <v-text-field
            v-model="searchTextModel"
            hide-details="auto"
            density="compact"
            class="search-input flex-grow-1"
            :placeholder="t('careerCenter.search.placeholder')"
            variant="outlined"
            color="primary"
            clearable
            @keyup.enter="handleEnterSearch"
          >
            <template #prepend-inner>
              <v-icon icon="mdi-magnify" color="primary" size="20"></v-icon>
            </template>
          </v-text-field>
          <v-btn
            color="primary"
            variant="flat"
            class="ml-3 search-btn"
            rounded="lg"
            size="default"
            @click="handleSearch"
          >
            <v-icon icon="mdi-magnify" class="mr-2"></v-icon>
            {{ t('careerCenter.search.button') }}
          </v-btn>
          <v-btn
            color="grey-darken-2"
            variant="tonal"
            rounded="lg"
            size="default"
            prepend-icon="mdi-plus-circle"
            class="ml-3"
            @click="handleOpenCareerApplication"
          >
            {{ t('careerCenter.search.applyJob') }}
          </v-btn>
        </div>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>
/* 导航栏样式 */
.nav-toggle {
  background: rgba(255, 255, 255, 0.8) !important;
  border-radius: 8px !important;
  border: 1px solid #e0e0e0 !important;
  overflow: hidden;
}

.nav-btn {
  padding: 8px 16px !important;
  font-size: 0.875rem !important;
  font-weight: 500 !important;
  text-transform: none !important;
  border-radius: 6px !important;
  margin: 2px !important;
  transition: all 0.2s ease !important;
  color: #666 !important;
}

.nav-btn:hover {
  background: rgba(25, 118, 210, 0.08) !important;
  color: #1976d2 !important;
}

.nav-btn-active {
  background: #1976d2 !important;
  color: white !important;
}

.nav-btn-active:hover {
  background: #1565c0 !important;
  color: white !important;
}

/* 搜索容器样式 */
.search-container {
  gap: 0;
}

.search-input {
  border-radius: 12px;
}

.search-btn {
  min-width: 120px;
  height: 40px;
  font-weight: 500;
  text-transform: none;
  letter-spacing: normal;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-container {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .search-btn {
    width: 100%;
  }
}
</style>