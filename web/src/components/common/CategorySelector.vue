<template>
  <div class="category-selector">
    <!-- 主分类选择 -->
    <v-select
      v-model="selectedMainCategory"
      :items="formMainCategories"
      item-title="title"
      item-value="value"
      :label="t('categorySelector.mainCategory')"
      variant="outlined"
      rounded="lg"
      bg-color="grey-lighten-5"
      class="mb-4"
      @update:model-value="onMainCategoryChange"
    >
      <template #prepend-item>
        <v-list-item>
          <template #prepend>
            <v-icon icon="mdi-folder-outline" class="mr-3"></v-icon>
          </template>
          <v-list-item-title class="font-weight-bold">{{
            t('categorySelector.selectMainCategory')
          }}</v-list-item-title>
        </v-list-item>
        <v-divider></v-divider>
      </template>
    </v-select>

    <!-- 子分类选择 -->
    <v-select
      v-model="selectedSubCategory"
      :items="availableSubCategories"
      item-title="name"
      item-value="id"
      :label="t('categorySelector.subCategory')"
      variant="outlined"
      rounded="lg"
      bg-color="grey-lighten-5"
      :disabled="!selectedMainCategory || selectedMainCategory === 'all'"
      @update:model-value="onSubCategoryChange"
    >
      <template #prepend-item>
        <v-list-item>
          <template #prepend>
            <v-icon icon="mdi-folder-multiple-outline" class="mr-3"></v-icon>
          </template>
          <v-list-item-title class="font-weight-bold">{{
            t('categorySelector.selectSubCategory')
          }}</v-list-item-title>
        </v-list-item>
        <v-divider></v-divider>
      </template>
    </v-select>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, onMounted } from 'vue'
import { useCategoryStore } from '@/stores'
import { useI18n } from '@/composables/useI18n'

// Props
const props = withDefaults(defineProps<Props>(), {
  modelMainCategory: null,
  modelSubCategory: null,
})

// Emits
const emit = defineEmits<Emits>()

const { t } = useI18n()
const categoryStore = useCategoryStore()

interface Props {
  modelMainCategory?: number | string | null
  modelSubCategory?: number | string | null
}

interface Emits {
  (e: 'update:modelMainCategory', value: number | string | null): void
  (e: 'update:modelSubCategory', value: number | string | null): void
}

// 本地状态 - 使用 computed 避免解构 props
const selectedMainCategory = computed({
  get: () => props.modelMainCategory,
  set: (value: number | string | null) => {
    emit('update:modelMainCategory', value)
  },
})

const selectedSubCategory = computed({
  get: () => props.modelSubCategory,
  set: (value: number | string | null) => {
    emit('update:modelSubCategory', value)
  },
})

// 加载分类数据
onMounted(async () => {
  await categoryStore.checkAndLoad()
})

// 转换为表单选项格式
const formMainCategories = computed(() =>
  categoryStore.getRoleMainCategories().map((cat) => ({
    title: cat.title,
    value: cat.id,
  }))
)

// 计算当前可用的子分类
const availableSubCategories = computed(() => {
  if (!selectedMainCategory.value || typeof selectedMainCategory.value !== 'number') return []
  return categoryStore.getRoleSubCategories(selectedMainCategory.value)
})

// 监听主分类变化
const onMainCategoryChange = (value: number | string | null): void => {
  selectedMainCategory.value = value
  selectedSubCategory.value = null
  emit('update:modelMainCategory', value)
  emit('update:modelSubCategory', null)
}

// 监听子分类变化
const onSubCategoryChange = (value: number | string | null): void => {
  selectedSubCategory.value = value
  emit('update:modelSubCategory', value)
}

// 监听外部props变化
watch(
  () => props.modelMainCategory,
  (newValue: number | string | null) => {
    selectedMainCategory.value = newValue
  }
)

watch(
  () => props.modelSubCategory,
  (newValue: number | string | null) => {
    selectedSubCategory.value = newValue
  }
)
</script>

<style scoped>
.category-selector {
  width: 100%;
}
</style>
