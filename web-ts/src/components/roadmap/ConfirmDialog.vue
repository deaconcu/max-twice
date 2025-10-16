<template>
  <!-- 保存确认对话框 -->
  <v-dialog v-model="showSave" max-width="800px">
    <v-card rounded="lg">
      <v-card-title class="text-teal-darken-3 d-flex align-center">
        <v-icon class="mr-2" size="small">mdi-content-save</v-icon>
        {{ t('confirmDialog.saveRoadmap') }}
      </v-card-title>

      <v-card-text class="pt-4">
        <v-alert
          color="grey"
          type="info"
          variant="tonal"
          class="mb-6"
          density="compact"
          rounded="lg"
        >
          {{ t('confirmDialog.saveDescription') }}
        </v-alert>

        <v-form v-model="descriptionFormValid">
          <v-textarea
            v-model="description"
            :label="t('confirmDialog.descriptionLabel')"
            :placeholder="t('confirmDialog.descriptionPlaceholder')"
            variant="outlined"
            rows="6"
            class="flat-input"
            color="teal-darken-1"
            :rules="roadmapDescriptionRules"
            :counter="ROADMAP_VALIDATION.DESCRIPTION_MAX_LENGTH"
          >
          </v-textarea>
        </v-form>
      </v-card-text>

      <v-card-actions class="px-6 py-3">
        <v-spacer></v-spacer>
        <v-btn variant="text" color="grey" class="flat-button" @click="$emit('cancel-save')">
          {{ t('confirmDialog.cancel') }}
        </v-btn>
        <v-btn
          color="teal-darken-1"
          variant="flat"
          :disabled="!descriptionFormValid || !description || description.trim().length === 0"
          class="flat-button ml-2"
          @click="$emit('confirm-save', description)"
        >
          <v-icon class="me-1" left>mdi-content-save</v-icon>
          {{ t('confirmDialog.confirmSave') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <!-- 重置确认对话框 -->
  <v-dialog v-model="showReset" max-width="500px">
    <v-card rounded="lg">
      <v-card-title class="text-orange-darken-2 d-flex align-center">
        <v-icon class="mr-2" size="small">mdi-alert-circle-outline</v-icon>
        {{ t('confirmDialog.confirmReset') }}
      </v-card-title>

      <v-card-text class="pt-4">
        {{ t('confirmDialog.resetDescription') }}
      </v-card-text>

      <v-card-actions class="px-6 py-3">
        <v-spacer></v-spacer>
        <v-btn variant="text" color="grey" class="flat-button" @click="$emit('cancel-reset')">
          {{ t('confirmDialog.cancel') }}
        </v-btn>
        <v-btn
          color="orange-darken-2"
          variant="flat"
          class="flat-button ml-2"
          @click="$emit('confirm-reset')"
        >
          <v-icon class="me-1" left>mdi-refresh</v-icon>
          {{ t('confirmDialog.confirmResetButton') }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
  import { computed, ref, watch } from 'vue'
  import { useI18n } from 'vue-i18n'
  import { roadmapDescriptionRules } from '@/utils/validationRules'
  import { ROADMAP_VALIDATION } from '@/types/validation'

  const { t } = useI18n()

  interface Props {
    showSaveDialog?: boolean
    showResetDialog?: boolean
    initialDescription?: string
  }

  const props = withDefaults(defineProps<Props>(), {
    showSaveDialog: false,
    showResetDialog: false,
    initialDescription: '',
  })

  interface Emits {
    (e: 'confirm-save', description: string): void
    (e: 'cancel-save'): void
    (e: 'confirm-reset'): void
    (e: 'cancel-reset'): void
  }

  defineEmits<Emits>()

  const description = ref<string>('')
  const descriptionFormValid = ref(true)

  // 监听 props 变化来更新本地 ref
  watch(
    () => props.initialDescription,
    (newValue: string) => {
      description.value = newValue
    },
    { immediate: true }
  )

  const showSave = computed({
    get: () => props.showSaveDialog,
    set: () => {}, // 由父组件控制
  })

  const showReset = computed({
    get: () => props.showResetDialog,
    set: () => {}, // 由父组件控制
  })

  // 监听初始描述变化
  watch(
    () => props.initialDescription,
    (newVal: string) => {
      description.value = newVal
    }
  )
</script>

<style scoped>
  .flat-button {
    border-radius: 8px !important;
    box-shadow: none !important;
  }

  .flat-input :deep(.v-field) {
    border-radius: 8px !important;
  }
</style>