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
    rounded="xl"
    hover
    @click="$emit('click')"
  >
    <v-card-text class="pa-6">
      <!-- 图标和标题 -->
      <div class="d-flex align-center mb-3">
        <v-avatar
          :color="career.iconColor || 'primary'"
          size="48"
          class="mr-3"
        >
          <v-icon
            :icon="career.icon || 'mdi-briefcase'"
            color="white"
            size="24"
          ></v-icon>
        </v-avatar>
        <div class="flex-grow-1">
          <h3 class="text-h6 font-weight-bold mb-1">{{ career.name }}</h3>
          <div class="d-flex align-center text-caption text-grey">
            <v-icon icon="mdi-folder-outline" size="14" class="mr-1"></v-icon>
            {{ getCategoryName(career.mainCategory) }}
            <span v-if="career.subCategory" class="mx-1">·</span>
            <span v-if="career.subCategory">{{ getSubCategoryNameById(career.subCategory) }}</span>
          </div>
        </div>
      </div>

      <!-- 描述 -->
      <p class="text-body-2 text-grey-darken-2 mb-4 description-text">
        {{ career.description }}
      </p>

      <!-- 技能标签 -->
      <div v-if="skillsList.length > 0" class="d-flex flex-wrap" style="gap: 8px;">
        <v-chip
          v-for="(skill, index) in skillsList.slice(0, 6)"
          :key="index"
          size="small"
          variant="tonal"
          color="primary"
          class="skill-chip"
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
  background-color: #FFFFFF;
  border: 1px solid #EDEFF1;
}

.career-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  border-color: rgb(var(--v-theme-primary));
}

.description-text {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
}

.skill-chip {
  font-size: 0.75rem;
  height: 24px;
}
</style>
