<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { REJECT_REASONS, BAN_REASONS } from '@/constants/adminReasons'
import { ContentState } from '@/types/enums'

// Props
interface Props {
  modelValue: boolean
  type: 'reject' | 'ban'
  itemName?: string
  itemType?: string // '节点', '帖子', '评论', '课程' 等
  itemState?: number // 当前项目状态，用于判断显示不同的提示文案
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false,
  type: 'reject',
  itemName: '',
  itemType: '内容',
  itemState: ContentState.SUBMITTED,
  loading: false
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'confirm': [reason: string]
}>()

// 响应式数据
const reason = ref<string>('')
const selectedReason = ref<string>('')

// 计算属性
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 预设理由列表
const reasonOptions = computed(() => {
  return props.type === 'reject' ? REJECT_REASONS : BAN_REASONS
})

// 对话框标题
const dialogTitle = computed(() => {
  if (props.type === 'ban') {
    return `屏蔽${props.itemType}`
  }
  // 拒绝类型根据当前状态显示不同文案
  if (props.itemState === ContentState.PUBLISHED) {
    return `撤销${props.itemType}通过`
  }
  if (props.itemState === ContentState.BANNED) {
    return '降级为拒绝'
  }
  return `拒绝${props.itemType}`
})

// 图标
const dialogIcon = computed(() => {
  return props.type === 'ban' ? 'mdi-cancel' : 'mdi-close-circle-outline'
})

// 图标颜色
const dialogIconColor = computed(() => {
  return props.type === 'ban' ? 'grey-darken-2' : 'red-darken-2'
})

// 操作动词
const actionText = computed(() => {
  if (props.type === 'ban') return '屏蔽'
  if (props.itemState === ContentState.PUBLISHED) return '撤销'
  if (props.itemState === ContentState.BANNED) return '降级'
  return '拒绝'
})

// 理由标签
const reasonLabel = computed(() => {
  return `选择${actionText.value}原因：`
})

// 输入框标签
const textareaLabel = computed(() => {
  return `${actionText.value}原因`
})

// 输入框占位符
const textareaPlaceholder = computed(() => {
  return `请详细说明${actionText.value}的原因...`
})

// 警告提示文本
const warningText = computed(() => {
  if (props.itemState === ContentState.PUBLISHED) {
    return `注意：此${props.itemType}已通过审核，撤销后将变为拒绝状态`
  }
  if (props.itemState === ContentState.BANNED) {
    return `注意：此${props.itemType}当前为屏蔽状态，降级后将变为拒绝状态`
  }
  return null
})

// 警告提示颜色
const warningColor = computed(() => {
  if (props.type === 'ban') return 'text-grey-darken-2'
  return 'text-orange-darken-2'
})

// 按钮颜色
const buttonColor = computed(() => {
  return props.type === 'ban' ? 'grey-lighten-2' : 'red-lighten-4'
})

// 按钮图标颜色
const buttonIconColor = computed(() => {
  return props.type === 'ban' ? 'grey-darken-2' : 'red-darken-2'
})

// 确认按钮文案
const confirmButtonText = computed(() => {
  return `确认${actionText.value}`
})

// 监听对话框关闭，重置数据
watch(dialogVisible, (newVal) => {
  if (!newVal) {
    reason.value = ''
    selectedReason.value = ''
  }
})

// 监听选中的预设理由
watch(selectedReason, (newValue: string) => {
  if (newValue) {
    reason.value = newValue
  }
})

// 方法
const handleClose = () => {
  dialogVisible.value = false
}

const handleConfirm = () => {
  if (!reason.value.trim()) {
    return
  }
  emit('confirm', reason.value.trim())
}
</script>

<template>
  <v-dialog v-model="dialogVisible" max-width="500px" persistent>
    <v-card rounded="lg">
      <v-card-title class="text-h6 font-weight-bold pa-6 pb-4">
        <v-icon :icon="dialogIcon" :color="dialogIconColor" class="mr-3"></v-icon>
        {{ dialogTitle }}
      </v-card-title>

      <v-card-text class="pa-6 pt-0">
        <div class="mb-4">
          <div class="text-body-2 text-grey-darken-1 mb-2">
            {{ itemType }}名称：<strong>{{ itemName }}</strong>
          </div>
          <div v-if="warningText" class="text-body-2 mb-2" :class="warningColor">
            <v-icon icon="mdi-alert" size="16" class="mr-1"></v-icon>
            {{ warningText }}
          </div>
        </div>

        <div class="mb-4">
          <div class="text-body-2 font-weight-medium mb-2">{{ reasonLabel }}</div>
          <v-chip-group
            v-model="selectedReason"
            :color="type === 'ban' ? 'grey-darken-1' : 'red-lighten-3'"
            variant="flat"
          >
            <v-chip
              v-for="presetReason in reasonOptions"
              :key="presetReason"
              :value="presetReason"
              rounded="lg"
              size="small"
              class="mb-2"
            >
              {{ presetReason }}
            </v-chip>
          </v-chip-group>
        </div>

        <v-textarea
          v-model="reason"
          :label="textareaLabel"
          :placeholder="textareaPlaceholder"
          variant="outlined"
          rows="4"
          rounded="lg"
          bg-color="grey-lighten-5"
        ></v-textarea>
      </v-card-text>

      <v-card-actions class="pa-6 pt-0">
        <v-spacer></v-spacer>
        <v-btn variant="outlined" color="grey" rounded="lg" @click="handleClose"> 取消 </v-btn>
        <v-btn
          variant="flat"
          :color="buttonColor"
          rounded="lg"
          :disabled="!reason.trim()"
          :loading="loading"
          @click="handleConfirm"
        >
          <v-icon :icon="dialogIcon" :color="buttonIconColor" class="mr-2"></v-icon>
          {{ confirmButtonText }}
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
/* 与职业管理对话框样式一致 */
</style>
