<template>
  <!-- 保存确认对话框 -->
  <v-dialog v-model="showSave" max-width="800px">
    <v-card rounded="lg">
      <v-card-title class="text-teal-darken-3 d-flex align-center">
        <v-icon class="mr-2" size="small">mdi-content-save</v-icon>
        保存课程表
      </v-card-title>
      
      <v-card-text class="pt-4">
        <v-alert color="grey" type="info" variant="tonal" class="mb-6" density="compact" rounded="lg">
          请确认课程表描述信息。描述将帮助其他用户了解这个课程表的内容和学习路径。
        </v-alert>
        
        <v-textarea
          v-model="description"
          label="课程表描述（必填）"
          placeholder="例如：这是一个完整的Java后端开发学习路径，涵盖从基础语法到企业级框架的全部内容，包括Spring Boot、MyBatis、Redis等核心技术栈..."
          variant="outlined"
          rows="6"
          class="flat-input"
          color="teal-darken-1"
          :rules="[rules.required]"
          counter="500"
          maxlength="500">
        </v-textarea>
      </v-card-text>

      <v-card-actions class="px-6 py-3">
        <v-spacer></v-spacer>
        <v-btn 
          variant="text" 
          color="grey" 
          @click="$emit('cancel-save')"
          class="flat-button">
          取消
        </v-btn>   
        <v-btn 
          color="teal-darken-1" 
          variant="flat" 
          @click="$emit('confirm-save', description)"
          :disabled="!description || description.trim().length === 0"
          class="flat-button ml-2">
          <v-icon class="me-1" left>mdi-content-save</v-icon>
          确认保存
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>

  <!-- 重置确认对话框 -->
  <v-dialog v-model="showReset" max-width="500px">
    <v-card rounded="lg">
      <v-card-title class="text-orange-darken-2 d-flex align-center">
        <v-icon class="mr-2" size="small">mdi-alert-circle-outline</v-icon>
        确认重置
      </v-card-title>
      
      <v-card-text class="pt-4">
        确定要重置编辑区域吗？这将清空所有节点、边和描述信息。
      </v-card-text>

      <v-card-actions class="px-6 py-3">
        <v-spacer></v-spacer>
        <v-btn 
          variant="text" 
          color="grey" 
          @click="$emit('cancel-reset')"
          class="flat-button">
          取消
        </v-btn>
        <v-btn 
          color="orange-darken-2" 
          variant="flat" 
          @click="$emit('confirm-reset')"
          class="flat-button ml-2">
          <v-icon class="me-1" left>mdi-refresh</v-icon>
          确认重置
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  showSaveDialog: {
    type: Boolean,
    default: false
  },
  showResetDialog: {
    type: Boolean,
    default: false
  },
  initialDescription: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['confirm-save', 'cancel-save', 'confirm-reset', 'cancel-reset'])

const description = ref(props.initialDescription)

const showSave = computed({
  get: () => props.showSaveDialog,
  set: () => {} // 由父组件控制
})

const showReset = computed({
  get: () => props.showResetDialog,
  set: () => {} // 由父组件控制
})

const rules = {
  required: value => !!value || '请输入课程表描述',
}

// 监听初始描述变化
watch(() => props.initialDescription, (newVal) => {
  description.value = newVal
})
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
