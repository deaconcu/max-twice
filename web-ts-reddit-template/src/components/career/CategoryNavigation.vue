<script setup lang="ts">
import { computed } from 'vue'
import type { ProfessionCategory, CategoryMapping } from '@/types/profession'

interface Props {
  categories: ProfessionCategory[]
  categoryMapping: CategoryMapping[]
  activeFirstLvl: number
  activeSecondLvl: number
  searchText: string
}

interface Emits {
  (e: 'selectFirstLevel', categoryId: number): void
  (e: 'selectSecondLevel', subcategoryIndex: number): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 根据主分类ID获取子分类列表
const getSubcategoriesByMainCategory = (mainCategoryId: number) => {
  const mapping = props.categoryMapping.find((item) => item.mainCategoryId === mainCategoryId)
  return mapping?.subcategories || []
}

const handleFirstLevelSelect = (categoryId: number): void => {
  emit('selectFirstLevel', categoryId)
}

const handleSecondLevelSelect = (subcategoryIndex: number): void => {
  emit('selectSecondLevel', subcategoryIndex)
}

const currentFirstCategory = computed(() => {
  return props.categories.find((c) => c.id === props.activeFirstLvl)
})
</script>

<template>
  <!-- 分类导航 -->
  <div v-if="!searchText.trim()" class="mb-6">
    <v-card border rounded="xl" class="pa-6 category-card">
      <!-- 标题 -->
      <div class="d-flex align-center mb-4">
        <v-icon icon="mdi-filter-variant" color="primary" size="24" class="mr-3"></v-icon>
        <div>
          <h3 class="text-h6 font-weight-bold mb-1">职业分类</h3>
          <p class="text-body-2 text-grey mb-0">按领域筛选职业</p>
        </div>
      </div>

      <!-- 一级分类按钮 -->
      <div class="d-flex flex-wrap mb-4" style="gap: 12px;">
        <v-btn
          v-for="category in categories"
          :key="category.id"
          :color="activeFirstLvl === category.id ? 'primary' : 'grey-lighten-3'"
          :variant="activeFirstLvl === category.id ? 'flat' : 'flat'"
          rounded="lg"
          class="category-btn"
          @click="handleFirstLevelSelect(category.id)"
        >
          <v-icon
            :icon="category.icon"
            size="18"
            class="mr-2"
            :color="activeFirstLvl === category.id ? 'white' : 'grey-darken-2'"
          ></v-icon>
          <span :class="activeFirstLvl === category.id ? 'text-white' : 'text-grey-darken-3'">
            {{ category.title }}
          </span>
        </v-btn>
      </div>

      <!-- 二级分类 -->
      <div v-if="activeFirstLvl !== -1 && activeFirstLvl !== 0" class="mt-6">
        <v-divider class="mb-4"></v-divider>

        <!-- 二级分类标题 -->
        <div class="d-flex align-center mb-3">
          <v-icon icon="mdi-chevron-right" color="primary" size="20" class="mr-2"></v-icon>
          <h4 class="text-subtitle-1 font-weight-bold">
            {{ currentFirstCategory?.title }} - 具体方向
          </h4>
        </div>

        <!-- 二级分类按钮 -->
        <div class="d-flex flex-wrap" style="gap: 8px;">
          <v-chip
            v-for="(subcategory, subcategoryIndex) in getSubcategoriesByMainCategory(activeFirstLvl)"
            :key="subcategoryIndex"
            :color="activeSecondLvl === subcategoryIndex ? 'primary' : 'grey-lighten-2'"
            :variant="activeSecondLvl === subcategoryIndex ? 'flat' : 'flat'"
            size="default"
            class="subcategory-chip"
            @click="handleSecondLevelSelect(subcategoryIndex)"
          >
            <v-icon
              :icon="activeSecondLvl === subcategoryIndex ? 'mdi-check-circle' : 'mdi-circle-outline'"
              size="14"
              class="mr-1"
            ></v-icon>
            {{ subcategory.name }}
          </v-chip>
        </div>
      </div>
    </v-card>
  </div>
</template>

<style scoped>
.category-card {
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
}

.category-btn {
  text-transform: none;
  letter-spacing: normal;
  font-weight: 500;
  transition: all 0.2s ease;
}

.category-btn:hover {
  transform: translateY(-2px);
}

.subcategory-chip {
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s ease;
}

.subcategory-chip:hover {
  transform: translateY(-1px);
}
</style>
