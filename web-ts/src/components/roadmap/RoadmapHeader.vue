<script setup lang="ts">
  import { computed } from 'vue'
  import { useI18n } from 'vue-i18n'
  import type { Roadmap } from '@/types/roadmap'

  interface Skill {
    icon: string
    label: string
    color: string
  }

  interface Stat {
    icon: string
    label: string
    color: string
  }

  interface Props {
    professionName?: string
    roadmaps?: Roadmap[]
    totalLearners?: number
  }

  interface Emits {
    (e: 'create-roadmap'): void
  }

  const props = withDefaults(defineProps<Props>(), {
    professionName: 'JAVA初级程序员',
    roadmaps: () => [],
    totalLearners: 0,
  })

  const emit = defineEmits<Emits>()

  const { t } = useI18n()

  // 技能列表
  const skills = computed((): Skill[] => [
    {
      icon: 'mdi-language-java',
      label: t('roadmap.skills.javaBasics'),
      color: 'grey-darken-2',
    },
    {
      icon: 'mdi-cube-outline',
      label: t('roadmap.skills.oop'),
      color: 'grey-darken-2',
    },
    {
      icon: 'mdi-database-outline',
      label: t('roadmap.skills.dataStructures'),
      color: 'grey-darken-2',
    },
    {
      icon: 'mdi-web',
      label: t('roadmap.skills.webDevelopment'),
      color: 'grey-darken-2',
    },
  ])

  // 统计信息
  const stats = computed((): Stat[] => [
    {
      icon: 'mdi-chart-line',
      label: `总学习人数: ${props.totalLearners}`,
      color: 'grey-darken-2',
    },
    {
      icon: 'mdi-star',
      label: '平均评分: 4.8',
      color: 'grey-darken-2',
    },
    {
      icon: 'mdi-clock-outline',
      label: '最近更新: 2小时前',
      color: 'primary',
    },
  ])

  const handleCreateClick = (): void => {
    emit('create-roadmap')
  }
</script>

<template>
  <div class="roadmap-header mb-8">
    <!-- 页面头部 -->
    <v-row justify="start" class="mb-4">
      <v-col cols="12">
        <div class="d-flex align-center mb-3">
          <v-avatar color="primary" size="40" class="mr-3">
            <v-icon icon="mdi-school-outline" color="white" size="20"></v-icon>
          </v-avatar>
          <div>
            <h1 class="text-h4 font-weight-bold text-grey-darken-4 mb-1">{{ professionName }}</h1>
            <p class="text-body-2 text-grey-darken-2 mb-0">{{ t('roadmap.systematicLearning') }}</p>
          </div>
        </div>
      </v-col>
    </v-row>

    <!-- 技能标签和操作按钮 -->
    <v-row justify="start" align="center" class="mb-4">
      <v-col cols="12" md="8">
        <div class="d-flex flex-wrap align-center">
          <v-chip
            v-for="skill in skills"
            :key="skill.label"
            color="grey-lighten-3"
            variant="flat"
            class="mr-2 mb-2 skill-chip"
          >
            <v-icon :icon="skill.icon" size="14" class="mr-1" :color="skill.color"></v-icon>
            <span class="text-grey-darken-3">{{ skill.label }}</span>
          </v-chip>
        </div>
      </v-col>
      <v-col cols="12" md="4" class="d-flex justify-end">
        <v-btn
          color="primary"
          variant="flat"
          rounded="lg"
          class="create-btn"
          @click="handleCreateClick"
        >
          <v-icon icon="mdi-plus" class="mr-2" size="16"></v-icon>
          {{ t('roadmap.createNew') }}
        </v-btn>
      </v-col>
    </v-row>

    <!-- 统计信息卡片 -->
    <v-row class="mb-6">
      <v-col cols="12">
        <v-card flat color="grey-lighten-5" rounded="lg" class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <div class="d-flex align-center text-grey-darken-3">
              <v-icon icon="mdi-information-outline" size="16" class="mr-2"></v-icon>
              <span class="text-body-2"
                >已有 <strong class="text-primary">{{ roadmaps.length }}</strong> 个学习路径</span
              >
            </div>
            <div class="d-flex align-center text-grey-darken-3 text-body-2">
              <div
                v-for="(stat, index) in stats"
                :key="stat.label"
                class="d-flex align-center"
                :class="{ 'mr-6': index < stats.length - 1 }"
              >
                <v-icon :icon="stat.icon" class="mr-1" size="16" :color="stat.color"></v-icon>
                <span
                  >{{ stat.label.includes(':') ? stat.label.split(':')[0] + ': ' : ''
                  }}<strong :class="`text-${stat.color}`">{{
                    stat.label.includes(':') ? stat.label.split(':')[1] : stat.label
                  }}</strong></span
                >
              </div>
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>
  .skill-chip {
    transition: all 0.2s ease;
  }

  .skill-chip:hover {
    background-color: rgba(0, 0, 0, 0.08) !important;
    transform: translateY(-1px);
  }

  .create-btn {
    text-transform: none;
    font-weight: 600;
    min-width: 120px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .create-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 16px rgba(25, 118, 210, 0.2);
  }
</style>