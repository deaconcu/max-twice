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

<script setup>
  import { computed, onMounted, ref, watch } from 'vue'
  import { learnService } from '@/services/learnService'
  import { useI18n } from 'vue-i18n'

  const { t } = useI18n()

  // Props
  const props = defineProps({
    modelMainCategory: {
      type: [Number, String],
      default: null,
    },
    modelSubCategory: {
      type: [Number, String],
      default: null,
    },
  })

  // Emits
  const emit = defineEmits(['update:modelMainCategory', 'update:modelSubCategory'])

  // 本地状态 - 使用 computed 避免解构 props
  const selectedMainCategory = computed({
    get: () => props.modelMainCategory,
    set: (value) => {
      emit('update:modelMainCategory', value)
    },
  })

  const selectedSubCategory = computed({
    get: () => props.modelSubCategory,
    set: (value) => {
      emit('update:modelSubCategory', value)
    },
  })

  // 动态数据
  const mainCategories = ref([])
  const categoryMapping = ref([])
  const loading = ref(true)

  // 使用动态的主分类选项（过滤掉'all'选项，因为CategorySelector主要用于表单）
  const formMainCategories = computed(() => {
    return mainCategories.value.filter((cat) => cat.value !== 'all')
  })

  // 计算当前可用的子分类
  const availableSubCategories = computed(() => {
    if (!selectedMainCategory.value) return []

    const mapping = categoryMapping.value.find(
      (item) => item.mainCategoryId === selectedMainCategory.value
    )

    return mapping?.subcategories || []
  })

  // 加载职业类别数据
  const loadProfessionCategories = async () => {
    try {
      loading.value = true
      const response = await learnService.getProfessionCategories()
      console.log('Loaded profession categories:', response.data)

      if (response.data) {
        mainCategories.value = response.data.mainCategories || []
        categoryMapping.value = response.data.categoryMapping || []
      }
    } catch (error) {
      console.error('Failed to load profession categories:', error)
      // 如果API调用失败，可以提供默认值或错误处理
    } finally {
      loading.value = false
    }
  }

  // 监听主分类变化
  const onMainCategoryChange = (value) => {
    selectedMainCategory.value = value
    // 重置子分类
    selectedSubCategory.value = null

    emit('update:modelMainCategory', value)
    emit('update:modelSubCategory', null)
  }

  // 监听子分类变化
  const onSubCategoryChange = (value) => {
    selectedSubCategory.value = value
    emit('update:modelSubCategory', value)
  }

  // 监听外部props变化
  watch(
    () => props.modelMainCategory,
    (newValue) => {
      selectedMainCategory.value = newValue
    }
  )

  watch(
    () => props.modelSubCategory,
    (newValue) => {
      selectedSubCategory.value = newValue
    }
  )

  // 组件挂载时加载数据
  onMounted(() => {
    loadProfessionCategories()
  })
</script>

<style scoped>
  .category-selector {
    width: 100%;
  }
</style>
