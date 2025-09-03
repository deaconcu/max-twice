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

        <v-textarea
          v-model="description"
          :label="t('confirmDialog.descriptionLabel')"
          :placeholder="t('confirmDialog.descriptionPlaceholder')"
          variant="outlined"
          rows="6"
          class="flat-input"
          color="teal-darken-1"
          :rules="[rules.required]"
          counter="500"
          maxlength="500"
        >
        </v-textarea>
      </v-card-text>

      <v-card-actions class="px-6 py-3">
        <v-spacer></v-spacer>
        <v-btn variant="text" color="grey" class="flat-button" @click="$emit('cancel-save')">
          {{ t('confirmDialog.cancel') }}
        </v-btn>
        <v-btn
          color="teal-darken-1"
          variant="flat"
          :disabled="!description || description.trim().length === 0"
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

<script setup>
  import { computed, ref, watch } from 'vue'
  import { useI18n } from 'vue-i18n'

  const { t } = useI18n()

  const props = defineProps({
    showSaveDialog: {
      type: Boolean,
      default: false,
    },
    showResetDialog: {
      type: Boolean,
      default: false,
    },
    initialDescription: {
      type: String,
      default: '',
    },
  })

  defineEmits(['confirm-save', 'cancel-save', 'confirm-reset', 'cancel-reset'])

  const description = ref()

  // 监听 props 变化来更新本地 ref
  watch(
    () => props.initialDescription,
    (newValue) => {
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

  const rules = {
    required: (value) => Boolean(value) || t('confirmDialog.descriptionRequired'),
  }

  // 监听初始描述变化
  watch(
    () => props.initialDescription,
    (newVal) => {
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
