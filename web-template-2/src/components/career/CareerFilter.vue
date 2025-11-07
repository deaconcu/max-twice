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
  <div class="filter-section">
    <!-- 搜索和操作区域 -->
    <div class="search-container">
      <v-text-field
        v-model="searchTextModel"
        hide-details="auto"
        density="compact"
        class="search-input"
        placeholder="搜索职业名称、技能或描述..."
        variant="outlined"
        color="primary"
        clearable
        @keyup.enter="handleEnterSearch"
      >
        <template #prepend-inner>
          <v-icon icon="mdi-magnify" color="#666666" size="20"></v-icon>
        </template>
      </v-text-field>
      <v-btn
        color="primary"
        variant="flat"
        class="search-btn"
        rounded="lg"
        size="default"
        @click="handleSearch"
      >
        <v-icon icon="mdi-magnify" class="mr-2"></v-icon>
        搜索
      </v-btn>
      <v-btn
        color="grey-darken-2"
        variant="outlined"
        rounded="lg"
        size="default"
        prepend-icon="mdi-plus-circle"
        class="apply-btn"
        @click="handleOpenCareerApplication"
      >
        申请职业
      </v-btn>
    </div>
  </div>
</template>

<style scoped>
.filter-section {
  margin-bottom: 32px;
}

.search-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  flex: 1;
}

.search-btn,
.apply-btn {
  flex-shrink: 0;
}
</style>
