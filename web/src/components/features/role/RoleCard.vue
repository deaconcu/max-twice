<template>
  <v-card rounded="xl" class="role-card hoverable" border hover @click="handleClick">
    <v-card-text class="pa-5">
      <div class="d-flex align-center mb-4">
        <div class="icon-container flex-shrink-0 mr-4">
          <v-icon :icon="getRoleIcon()" :color="getIconColor()" size="28" />
        </div>
        <div class="flex-grow-1">
          <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-1">
            {{ role.name }}
          </h3>
          <div class="d-flex align-center">
            <v-icon icon="mdi-account-group" size="14" color="grey" class="mr-1" />
            <span class="text-caption text-grey-darken-2">
              {{ formatNumber(role.learnerCount) }} {{ t('roleCard.learnersCount') }}
            </span>
          </div>
        </div>
      </div>

      <p v-if="role.description" class="text-body-2 text-grey-darken-2 role-description mb-5">
        {{ role.description }}
      </p>

      <!-- 核心技能标签 -->
      <div v-if="role.skills" class="skills-section">
        <div class="skills-chips">
          <v-chip
            v-for="(skill, index) in getSkillsList"
            :key="index"
            size="small"
            variant="tonal"
            color="grey"
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
import { getColorByString } from '@/utils/color'
import type { Profession } from '@/types/profession'

interface Props {
  role: Profession
}

type Emits = (e: 'click', role: Profession) => void

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const { t } = useI18n()

/**
 * 获取职业图标
 */
const getRoleIcon = () => {
  return props.role.icon || 'mdi-briefcase-outline'
}

/**
 * 获取图标颜色
 */
const getIconColor = () => {
  return getColorByString(props.role.name)
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
  if (!props.role.skills) return []
  const skills = props.role.skills.split(',').map((s) => s.trim())
  return skills.slice(0, 4)
})

/**
 * 处理卡片点击
 */
const handleClick = () => {
  emit('click', props.role)
}
</script>

<style scoped>
.role-card {
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

.role-description {
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
