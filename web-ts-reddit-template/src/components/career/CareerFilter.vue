<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  searchText: string
}

interface Emits {
  (e: 'update:searchText', value: string): void
  (e: 'performSearch'): void
  (e: 'openCareerApplication'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const searchTextModel = computed({
  get: () => props.searchText,
  set: (value: string) => emit('update:searchText', value),
})

const handleSearch = (): void => {
  emit('performSearch')
}

const handleEnterSearch = (): void => {
  emit('performSearch')
}

const handleOpenCareerApplication = (): void => {
  emit('openCareerApplication')
}
</script>

<template>
  <div class="mb-6">
    <!-- 页面头部 -->
    <div class="d-flex align-center justify-space-between mb-4">
      <div class="d-flex align-center">
        <v-avatar color="orange-lighten-4" size="56" class="mr-3">
          <v-icon icon="mdi-briefcase-variant" color="orange" size="28"></v-icon>
        </v-avatar>
        <div>
          <h1 class="text-h4 font-weight-bold mb-1">职业中心</h1>
          <p class="text-body-2 text-grey mb-0">探索不同职业道路，规划你的职业发展</p>
        </div>
      </div>
    </div>

    <!-- 搜索和操作区域 -->
    <div class="d-flex align-center search-container">
      <v-text-field
        v-model="searchTextModel"
        hide-details="auto"
        density="comfortable"
        class="search-input flex-grow-1"
        placeholder="搜索职业名称、技能或描述..."
        variant="outlined"
        color="primary"
        clearable
        @keyup.enter="handleEnterSearch"
      >
        <template #prepend-inner>
          <v-icon icon="mdi-magnify" color="grey" size="20"></v-icon>
        </template>
      </v-text-field>
      <v-btn
        color="primary"
        variant="flat"
        class="ml-3"
        rounded="lg"
        @click="handleSearch"
      >
        <v-icon icon="mdi-magnify" class="mr-2"></v-icon>
        搜索
      </v-btn>
      <v-btn
        color="grey-darken-2"
        variant="tonal"
        rounded="lg"
        prepend-icon="mdi-plus-circle"
        class="ml-3"
        @click="handleOpenCareerApplication"
      >
        申请职业
      </v-btn>
    </div>
  </div>
</template>

<style scoped>
.search-container {
  gap: 0;
}

.search-input {
  border-radius: 12px;
}
</style>
