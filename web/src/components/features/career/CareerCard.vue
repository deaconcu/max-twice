<template>
  <v-card rounded="lg" class="career-card hoverable" border hover @click="handleClick">
    <v-card-text class="pa-6">
      <div class="d-flex align-center mb-4">
        <div class="icon-container flex-shrink-0 mr-4">
          <v-icon :icon="getRandomIcon()" :color="getRandomColor()" size="28" />
        </div>
        <div class="flex-grow-1">
          <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-1">
            {{ career.name }}
          </h3>
          <div class="d-flex align-center">
            <v-icon icon="mdi-account-group" size="14" color="grey" class="mr-1" />
            <span class="text-caption text-grey-darken-2">
              {{ formatNumber(career.learnerCount) }} {{ t('careerCard.learnersCount') }}
            </span>
          </div>
        </div>
      </div>

      <p v-if="career.description" class="text-body-2 text-grey-darken-2 career-description mb-5">
        {{ career.description }}
      </p>

      <!-- 核心技能标签 -->
      <div v-if="career.skills" class="skills-section">
        <div class="skills-chips">
          <v-chip
            v-for="(skill, index) in getSkillsList"
            :key="index"
            size="small"
            variant="tonal"
            color="primary"
            class="mr-2"
          >
            {{ skill }}
          </v-chip>
        </div>
      </div>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '@/composables/useI18n'
import type { Profession } from '@/types/profession'

interface Props {
  career: Profession
}

type Emits = (e: 'click', career: Profession) => void

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const { t } = useI18n()

// 随机图标池 - 职业相关
const icons = [
  'mdi-briefcase-variant',
  'mdi-laptop',
  'mdi-palette',
  'mdi-bullhorn',
  'mdi-chart-bar',
  'mdi-code-braces',
  'mdi-database',
  'mdi-cloud',
  'mdi-server',
  'mdi-account-tie',
  'mdi-lightbulb',
  'mdi-rocket',
]

// 随机颜色池
const colors = [
  'primary',
  'success',
  'warning',
  'error',
  'info',
  'purple',
  'indigo',
  'blue',
  'cyan',
  'teal',
  'green',
  'orange',
]

/**
 * 根据职业 ID 获取一致的随机图标
 */
const getRandomIcon = () => {
  const index = props.career.id % icons.length
  return icons[index]
}

/**
 * 根据职业 ID 获取一致的随机颜色
 */
const getRandomColor = () => {
  const index = props.career.id % colors.length
  return colors[index]
}

/**
 * 格式化数字（千位分隔）
 */
const formatNumber = (num?: number) => {
  if (!num) return '0'
  return num.toLocaleString()
}

/**
 * 获取技能列表（最多显示4个）
 */
const getSkillsList = computed(() => {
  if (!props.career.skills) return []
  const skills = props.career.skills.split(',').map((s) => s.trim())
  return skills.slice(0, 4)
})

/**
 * 处理卡片点击
 */
const handleClick = () => {
  emit('click', props.career)
}
</script>

<style scoped>
.career-card {
  background-color: rgb(var(--v-theme-surface));
}

.icon-container {
  width: 56px;
  height: 56px;
  border: 1px solid rgb(var(--v-theme-outline));
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

.skills-section {
  padding-top: 0;
}

.skills-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
