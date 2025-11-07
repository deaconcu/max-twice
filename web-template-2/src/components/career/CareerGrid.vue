<script setup lang="ts">
import { computed } from 'vue'
import CareerCard from './CareerCard.vue'
import type { CareerWithDisplay, ProfessionCategory, CategoryMapping } from '@/types/profession'

interface Props {
  displayedCareers: CareerWithDisplay[]
  loading: boolean
  activeFirstLvl: number
  activeSecondLvl: number
  categories: ProfessionCategory[]
  categoryMapping: CategoryMapping[]
  searchText: string
}

interface Emits {
  (e: 'goToCareerDetail', career: CareerWithDisplay): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 获取分类名称
const getCategoryName = (categoryId: number): string => {
  const category = props.categories.find((cat) => cat.id === categoryId)
  return category?.title || ''
}

// 获取子分类名称
const getSubCategoryNameById = (subCategoryId: number): string => {
  for (const mapping of props.categoryMapping) {
    const subcategory = mapping.subcategories.find((sub) => sub.id === subCategoryId)
    if (subcategory) {
      return subcategory.name
    }
  }
  return ''
}

const handleGoToCareerDetail = (career: CareerWithDisplay): void => {
  emit('goToCareerDetail', career)
}

// 显示标题
const listingTitle = computed(() => {
  if (props.searchText.trim()) {
    return `搜索结果：找到 ${props.displayedCareers.length} 个职业`
  } else if (props.activeFirstLvl !== -1) {
    const categoryName = getCategoryName(props.activeFirstLvl)
    return `${categoryName} - 共 ${props.displayedCareers.length} 个职业`
  }
  return `全部职业 - 共 ${props.displayedCareers.length} 个`
})
</script>

<template>
  <div>
    <!-- 列表标题 -->
    <div class="d-flex align-center justify-space-between mb-5">
      <h4 class="text-h5 font-weight-semibold">
        {{ listingTitle }}
      </h4>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-16">
      <v-progress-circular
        indeterminate
        color="primary"
        size="64"
      ></v-progress-circular>
      <p class="text-body-2 text-grey mt-4">加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="displayedCareers.length === 0" class="text-center py-16">
      <div class="empty-state pa-16">
        <v-icon
          icon="mdi-briefcase-search-outline"
          size="80"
          color="grey-lighten-1"
          class="mb-4"
        ></v-icon>
        <h3 class="text-h6 mb-2">
          {{ searchText.trim() ? '未找到相关职业' : '暂无职业数据' }}
        </h3>
        <p class="text-body-2 text-grey">
          {{
            searchText.trim()
              ? '请尝试其他搜索关键词或浏览分类'
              : '请选择分类查看职业或使用搜索功能'
          }}
        </p>
      </div>
    </div>

    <!-- 职业列表 -->
    <v-row v-else>
      <v-col
        v-for="career in displayedCareers"
        :key="career.id"
        cols="12"
        md="6"
        lg="4"
      >
        <CareerCard
          :career="career"
          :get-category-name="getCategoryName"
          :get-sub-category-name-by-id="getSubCategoryNameById"
          @click="handleGoToCareerDetail(career)"
        />
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>
.empty-state {
  background-color: #FFFFFF;
  border: 1px solid #E5E5E5;
  border-radius: 16px;
}
</style>
