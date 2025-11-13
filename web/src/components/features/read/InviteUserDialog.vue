<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  nodeId?: number
}

const props = withDefaults(defineProps<Props>(), {
  nodeId: 0,
})

const dialog = defineModel<boolean>({ default: false })

const searchKeyword = ref('')
const searchResultInfo = ref('')

// Mock 用户数据
const mockUsers = ref<any[]>([])

// 搜索用户
const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    mockUsers.value = []
    searchResultInfo.value = ''
    return
  }

  // Mock 搜索结果
  mockUsers.value = [
    {
      id: 1,
      name: '张三',
      avatar: null,
      disabled: false,
    },
    {
      id: 2,
      name: '李四',
      avatar: null,
      disabled: false,
    },
    {
      id: 3,
      name: '王五',
      avatar: null,
      disabled: false,
    },
  ]

  if (mockUsers.value.length === 0) {
    searchResultInfo.value = '未找到用户'
  } else {
    searchResultInfo.value = ''
  }
}

// 邀请用户
const handleInvite = (user: any) => {
  console.log('邀请用户:', user.id, '回答节点:', props.nodeId)
  user.disabled = true
  // TODO: 调用后端 API
}

// 关闭对话框
const closeDialog = () => {
  dialog.value = false
  searchKeyword.value = ''
  mockUsers.value = []
  searchResultInfo.value = ''
}
</script>

<template>
  <v-dialog v-model="dialog" width="600" persistent>
    <v-card rounded="xl">
      <!-- 头部 -->
      <v-card-title class="pa-4 d-flex align-center justify-space-between">
        <div class="d-flex align-center">
          <v-icon icon="mdi-account-plus-outline" color="primary" class="mr-2"></v-icon>
          <span class="text-h6 font-weight-bold">邀请回答</span>
        </div>
        <v-btn icon="mdi-close" variant="text" size="small" @click="closeDialog"></v-btn>
      </v-card-title>

      <!-- 内容区 -->
      <v-card-text class="pa-6 pt-1">
        <!-- 搜索框 -->
        <v-text-field
          v-model="searchKeyword"
          label="搜索用户名"
          variant="outlined"
          density="comfortable"
          hide-details
          append-inner-icon="mdi-magnify"
          @click:append-inner="handleSearch"
          @keyup.enter="handleSearch"
        ></v-text-field>

        <!-- 搜索结果 -->
        <div v-if="mockUsers.length > 0" class="user-list mt-4">
          <div
            v-for="user in mockUsers"
            :key="user.id"
            class="user-item d-flex justify-space-between align-center"
          >
            <!-- 用户信息 -->
            <div class="d-flex align-center">
              <v-avatar size="40" color="grey-lighten-2" class="mr-3">
                <v-icon icon="mdi-account" color="grey" size="24"></v-icon>
              </v-avatar>
              <div>
                <div class="text-body-2 font-weight-medium text-grey-darken-3">
                  {{ user.name }}
                </div>
              </div>
            </div>

            <!-- 邀请按钮 -->
            <v-btn
              variant="flat"
              color="primary"
              size="small"
              :disabled="user.disabled"
              @click="handleInvite(user)"
            >
              {{ user.disabled ? '已邀请' : '邀请' }}
            </v-btn>
          </div>
        </div>

        <!-- 空状态 -->
        <div
          v-if="mockUsers.length === 0 && searchResultInfo"
          class="text-body-2 text-grey text-center py-8"
        >
          {{ searchResultInfo }}
        </div>

        <!-- 初始提示 -->
        <div
          v-if="mockUsers.length === 0 && !searchResultInfo"
          class="text-body-2 text-grey-darken-1 text-center py-8"
        >
          请输入用户名进行搜索
        </div>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.border-b {
  border-bottom: 1px solid rgb(var(--v-theme-border));
}

.user-list {
  display: flex;
  flex-direction: column;
}

.user-item {
  padding: 14px;
  border-bottom: 1px solid rgb(var(--v-theme-border));
  background-color: white;
  transition: all 0.2s ease;
}

.user-item:hover {
  background-color: #f6f7f8;
}
</style>
