<script setup lang="ts">
import type { CareerWithDisplay } from '@/types/profession'

interface Props {
  career: CareerWithDisplay
  getCategoryName: (id: number) => string
  getSubCategoryNameById: (id: number) => string
}

const props = defineProps<Props>()

const skillsList = props.career.skills?.split(',').filter(s => s.trim()) || []
</script>

<template>
  <v-card
    class="career-card"
    border
    rounded="lg"
    hover
    @click="$emit('click')"
  >
    <v-card-text class="pa-6">
      <!-- 图标和标题 -->
      <div class="d-flex align-center mb-4">
        <div class="icon-container flex-shrink-0 mr-4">
          <v-icon
            :icon="career.icon || 'mdi-briefcase'"
            :color="career.iconColor || 'primary'"
            size="28"
          ></v-icon>
        </div>
        <div class="flex-grow-1">
          <h3 class="text-h6 font-weight-bold mb-1">{{ career.name }}</h3>
          <div class="d-flex align-center text-caption text-grey-darken-1">
            <v-icon icon="mdi-folder-outline" size="14" class="mr-1"></v-icon>
            {{ getCategoryName(career.mainCategory) }}
            <span v-if="career.subCategory" class="mx-1">·</span>
            <span v-if="career.subCategory">{{ getSubCategoryNameById(career.subCategory) }}</span>
          </div>
        </div>
      </div>

      <!-- 描述 -->
      <p class="text-body-2 text-grey-darken-2 mb-5 career-description">
        {{ career.description }}
      </p>

      <!-- 技能标签 -->
      <div v-if="skillsList.length > 0" class="d-flex flex-wrap" style="gap: 8px">
        <v-chip
          v-for="(skill, index) in skillsList.slice(0, 6)"
          :key="index"
          size="small"
          variant="tonal"
          color="primary"
        >
          {{ skill.trim() }}
        </v-chip>
        <v-chip
          v-if="skillsList.length > 6"
          size="small"
          variant="text"
          color="grey"
        >
          +{{ skillsList.length - 6 }}
        </v-chip>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
.career-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.career-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  border-color: #000000 !important;
}

.icon-container {
  width: 56px;
  height: 56px;
  border: 1px solid #E5E5E5;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.career-description {
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
