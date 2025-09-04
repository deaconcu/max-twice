<script setup lang="ts">
  import { computed } from 'vue'

  type CardSize = 'small' | 'medium' | 'large'

  interface Props {
    title: string
    value: string | number
    subtitle?: string
    icon?: string
    color?: string
    size?: CardSize
    clickable?: boolean
  }

  const props = withDefaults(defineProps<Props>(), {
    subtitle: '',
    icon: '',
    color: 'primary',
    size: 'medium',
    clickable: false,
  })

  interface Emits {
    (e: 'click'): void
  }

  const emit = defineEmits<Emits>()

  const handleClick = (): void => {
    if (props.clickable) {
      emit('click')
    }
  }

  const cardClasses = computed(() => {
    const classes = ['data-card']
    if (props.clickable) classes.push('clickable')
    if (props.size) classes.push(`size-${props.size}`)
    return classes
  })
</script>

<template>
  <v-card
    :class="cardClasses"
    rounded="lg"
    elevation="0"
    color="grey-lighten-5"
    @click="handleClick"
  >
    <v-card-text class="pa-4">
      <div class="d-flex align-center">
        <!-- 图标 -->
        <div v-if="icon" class="icon-container mr-3">
          <v-avatar :color="color" size="40" class="elevation-1">
            <v-icon :icon="icon" color="white" size="20"></v-icon>
          </v-avatar>
        </div>

        <!-- 内容 -->
        <div class="flex-grow-1">
          <div class="data-value text-h6 font-weight-bold text-grey-darken-4 mb-1">
            {{ value }}
          </div>
          <div class="data-title text-body-2 text-grey-darken-2 mb-0">
            {{ title }}
          </div>
          <div v-if="subtitle" class="data-subtitle text-caption text-grey-darken-1">
            {{ subtitle }}
          </div>
        </div>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
  .data-card {
    transition: all 0.2s ease;
  }

  .data-card.clickable {
    cursor: pointer;
  }

  .data-card.clickable:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  }

  .data-card.size-small .data-value {
    font-size: 1.1rem !important;
  }

  .data-card.size-medium .data-value {
    font-size: 1.3rem !important;
  }

  .data-card.size-large .data-value {
    font-size: 1.6rem !important;
  }

  .icon-container {
    flex-shrink: 0;
  }
</style>