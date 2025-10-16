<script setup lang="ts">
import { computed } from 'vue'
import { getErrorIcon } from '@/types/error'

interface Props {
  errorCode: number
  errorMessage: string
  showRetry?: boolean
  showBackHome?: boolean
}

interface Emits {
  (e: 'retry'): void
  (e: 'back-home'): void
}

const props = withDefaults(defineProps<Props>(), {
  showRetry: true,
  showBackHome: true
})

const emit = defineEmits<Emits>()

const icon = computed(() => getErrorIcon(props.errorCode))
</script>

<template>
  <v-container fluid class="error-page">
    <v-row justify="center" align="center" class="fill-height">
      <v-col cols="12" md="6" class="text-center">
        <v-icon :icon="icon" size="120" color="grey-lighten-1" class="mb-6"></v-icon>

        <h2 class="text-h4 font-weight-bold text-grey-darken-3 mb-4">
          {{ errorMessage }}
        </h2>

        <p class="text-body-1 text-grey-darken-1 mb-8">
          错误码: {{ errorCode }}
        </p>

        <div class="d-flex justify-center gap-4">
          <v-btn
            v-if="showBackHome"
            color="primary"
            variant="flat"
            size="large"
            prepend-icon="mdi-home"
            @click="emit('back-home')"
          >
            返回首页
          </v-btn>

          <v-btn
            v-if="showRetry"
            color="primary"
            variant="outlined"
            size="large"
            prepend-icon="mdi-refresh"
            @click="emit('retry')"
          >
            重试
          </v-btn>
        </div>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.error-page {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gap-4 {
  gap: 1rem;
}
</style>
