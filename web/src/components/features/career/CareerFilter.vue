<template>
  <v-card border rounded="lg" class="category-nav-card mb-6">
    <v-card-text class="pa-6">
      <!-- 一级分类按钮 -->
      <div class="d-flex flex-wrap" style="gap: 12px">
        <!-- 全部分类 -->
        <v-btn
          :color="!selectedMainCategory ? 'primary' : 'grey-lighten-3'"
          variant="flat"
          rounded="lg"
          class="category-btn"
          @click="selectMainCategory(undefined)"
        >
          <v-icon
            icon="mdi-view-grid"
            size="18"
            class="mr-2"
            :color="!selectedMainCategory ? 'white' : 'grey-darken-2'"
          />
          <span :class="!selectedMainCategory ? 'text-white' : 'text-grey-darken-3'">
            {{ t('careerCenter.difficulty.all') }}
          </span>
        </v-btn>

        <!-- 具体分类 -->
        <v-btn
          v-for="category in categories"
          :key="category.id"
          :color="selectedMainCategory === category.id ? 'primary' : 'grey-lighten-3'"
          variant="flat"
          rounded="lg"
          class="category-btn"
          @click="selectMainCategory(category.id)"
        >
          <v-icon
            :icon="category.icon || 'mdi-folder'"
            size="18"
            class="mr-2"
            :color="selectedMainCategory === category.id ? 'white' : 'grey-darken-2'"
          />
          <span :class="selectedMainCategory === category.id ? 'text-white' : 'text-grey-darken-3'">
            {{ category.title }}
          </span>
        </v-btn>
      </div>

      <!-- 二级分类 -->
      <div v-if="selectedMainCategory && subCategories.length > 0" class="mt-6">
        <v-divider class="mb-5" />

        <!-- 二级分类标题 -->
        <div class="d-flex align-center mb-4">
          <v-icon icon="mdi-chevron-right" color="primary" size="20" class="mr-2" />
          <h4 class="text-body-1 font-weight-bold">
            {{ getCategoryName(selectedMainCategory) }} -
            {{ t('careerCenter.category.specificDirection') }}
          </h4>
        </div>

        <!-- 二级分类chips -->
        <div class="d-flex flex-wrap" style="gap: 8px">
          <!-- 二级全部 -->
          <v-chip
            :color="!selectedSubCategory ? 'primary' : 'grey-lighten-2'"
            variant="flat"
            size="default"
            class="subcategory-chip"
            @click="selectSubCategory(undefined)"
          >
            <v-icon
              :icon="!selectedSubCategory ? 'mdi-check-circle' : 'mdi-circle-outline'"
              size="14"
              class="mr-1"
            />
            {{ t('careerCenter.difficulty.all') }}
          </v-chip>

          <!-- 具体二级分类 -->
          <v-chip
            v-for="sub in subCategories"
            :key="sub.id"
            :color="selectedSubCategory === sub.id ? 'primary' : 'grey-lighten-3'"
            variant="flat"
            size="default"
            class="subcategory-chip"
            @click="selectSubCategory(sub.id)"
          >
            <v-icon
              :icon="selectedSubCategory === sub.id ? 'mdi-check-circle' : 'mdi-circle-outline'"
              size="14"
              class="mr-1"
            />
            {{ sub.name }}
          </v-chip>
        </div>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from '@/composables/useI18n'

/**
 * 分类接口
 */
interface Category {
  id: number
  title: string // 注意：使用 title 而非 name
  icon?: string
}

interface SubCategory {
  id: number
  name: string
  mainCategoryId: number
}

interface Props {
  categories: Category[]
  subCategories: SubCategory[]
  mainCategory?: number
  subCategory?: number
}

interface Emits {
  (e: 'update:mainCategory' | 'update:subCategory', value: number | undefined): void
  (e: 'change', filters: { mainCategory?: number; subCategory?: number }): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const { t } = useI18n()

// 本地状态
const selectedMainCategory = ref<number | undefined>(props.mainCategory)
const selectedSubCategory = ref<number | undefined>(props.subCategory)

// 当前二级分类列表
const subCategories = computed(() => {
  if (!selectedMainCategory.value) return []
  return props.subCategories.filter((sub) => sub.mainCategoryId === selectedMainCategory.value)
})

/**
 * 监听外部变化
 */
watch(
  () => props.mainCategory,
  (value) => {
    selectedMainCategory.value = value
  }
)

watch(
  () => props.subCategory,
  (value) => {
    selectedSubCategory.value = value
  }
)

/**
 * 获取分类名称
 */
const getCategoryName = (categoryId: number) => {
  return props.categories.find((c) => c.id === categoryId)?.title ?? ''
}

/**
 * 选择主分类
 */
const selectMainCategory = (categoryId: number | undefined) => {
  selectedMainCategory.value = categoryId
  // 切换主分类时清空子分类
  selectedSubCategory.value = undefined

  emit('update:mainCategory', categoryId)
  emit('update:subCategory', undefined)
  emitChange()
}

/**
 * 选择子分类
 */
const selectSubCategory = (subCategoryId: number | undefined) => {
  selectedSubCategory.value = subCategoryId

  emit('update:subCategory', subCategoryId)
  emitChange()
}

/**
 * 触发筛选变化事件
 */
const emitChange = () => {
  emit('change', {
    mainCategory: selectedMainCategory.value,
    subCategory: selectedSubCategory.value,
  })
}
</script>

<style scoped>
.category-nav-card {
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-outline));
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
