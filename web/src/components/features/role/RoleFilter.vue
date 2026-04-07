<template>
  <v-card flat class="category-nav-card" rounded="xl">
    <div class="bg-surface-variant pa-3 rounded-xl">
      <!-- 一级分类按钮组 -->
      <div class="d-flex flex-wrap category-buttons-gap">
        <!-- 全部分类 -->
        <v-btn
          :color="!selectedMainCategory ? 'grey-darken-2' : 'white'"
          variant="tonal"
          rounded="xl"
          class="font-weight-medium category-btn"
          @click="selectMainCategory(undefined)"
        >
          <v-icon icon="mdi-view-grid" size="18" class="mr-2" :color="'grey-darken-2'" />
          <span class="text-grey-darken-3">
            {{ t('roleCenter.difficulty.all') }}
          </span>
        </v-btn>

        <!-- 具体分类 -->
        <v-btn
          v-for="category in categories"
          :key="category.id"
          :color="selectedMainCategory === category.id ? 'grey-darken-2' : 'white'"
          variant="tonal"
          rounded="xl"
          class="font-weight-medium category-btn"
          @click="selectMainCategory(category.id)"
        >
          <v-icon
            :icon="category.icon || 'mdi-folder'"
            size="18"
            class="mr-2"
            color="grey-darken-2"
          />
          <span class="text-grey-darken-3">
            {{ category.title }}
          </span>
        </v-btn>
      </div>

      <!-- 二级分类 -->
      <div v-if="selectedMainCategory && subCategories.length > 0" class="mt-4">
        <div class="pa-4 rounded-xl bg-white">
          <!-- 二级分类按钮组 -->
          <div class="d-flex flex-wrap subcategory-buttons-gap">
            <!-- 二级全部 -->
            <v-btn
              :color="!selectedSubCategory ? 'grey' : 'white'"
              variant="tonal"
              rounded="xl"
              class="font-weight-medium subcategory-btn"
              @click="selectSubCategory(undefined)"
            >
              <span class="text-grey-darken-3">
                {{ t('roleCenter.difficulty.allCategory', { category: getCategoryName(selectedMainCategory) }) }}
              </span>
            </v-btn>

            <!-- 具体二级分类 -->
            <v-btn
              v-for="sub in subCategories"
              :key="sub.id"
              :color="selectedSubCategory === sub.id ? 'grey' : 'white'"
              variant="tonal"
              rounded="xl"
                            class="font-weight-medium subcategory-btn"
              @click="selectSubCategory(sub.id)"
            >
              <span class="text-grey-darken-3">
                {{ sub.name }}
              </span>
            </v-btn>
          </div>
        </div>
      </div>
    </div>
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
/* 一级分类按钮样式 */
.category-btn {
  transition: all 0.2s ease-in-out;
  text-transform: none;
  letter-spacing: normal;
  min-height: 40px;
  padding: 8px 16px;
}

.category-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 二级分类按钮样式 */
.subcategory-btn {
  transition: all 0.15s ease-in-out;
  text-transform: none;
  letter-spacing: normal;
  min-height: 36px;
  padding: 6px 12px;
}

.subcategory-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 按钮间距 */
.category-buttons-gap {
  gap: 12px;
}

.subcategory-buttons-gap {
  gap: 10px;
}
</style>
