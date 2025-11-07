<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  searchText?: string
}

interface Emits {
  (e: 'update:searchText', value: string): void
  (e: 'performSearch'): void
  (e: 'openCourseApplication'): void
}

const props = withDefaults(defineProps<Props>(), {
  searchText: ''
})

const emit = defineEmits<Emits>()

const searchTextModel = computed({
  get: () => props.searchText,
  set: (value: string) => emit('update:searchText', value)
})

const handleSearch = (): void => {
  emit('performSearch')
}

const handleOpenApplication = (): void => {
  emit('openCourseApplication')
}
</script>

<template>
  <div class="mb-6">
    <div class="d-flex align-center" style="gap: 12px">
      <!-- 搜索输入框 -->
      <v-text-field
        v-model="searchTextModel"
        placeholder="搜索课程名称或描述..."
        variant="outlined"
        color="primary"
        density="compact"
        hide-details
        clearable
        class="search-field"
        @keyup.enter="handleSearch"
      >
        <template #prepend-inner>
          <v-icon icon="mdi-magnify" color="grey-darken-1" size="20"></v-icon>
        </template>
      </v-text-field>

      <!-- 搜索按钮 -->
      <v-btn
        color="primary"
        variant="flat"
        rounded="lg"
        size="default"
        class="px-6 flex-shrink-0"
        @click="handleSearch"
      >
        <v-icon icon="mdi-magnify" size="20" class="mr-1"></v-icon>
        搜索
      </v-btn>

      <!-- 申请课程按钮 -->
      <v-btn
        color="grey-darken-2"
        variant="outlined"
        rounded="lg"
        size="default"
        class="px-6 flex-shrink-0"
        @click="handleOpenApplication"
      >
        <v-icon icon="mdi-plus-circle" size="20" class="mr-1"></v-icon>
        申请课程
      </v-btn>
    </div>
  </div>
</template>

<style scoped>
.search-field {
  flex: 1;
}
</style>
