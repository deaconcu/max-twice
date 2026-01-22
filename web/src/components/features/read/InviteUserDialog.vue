<script setup lang="ts">
import { ref, inject } from 'vue'
import { userApi } from '@/api/modules/user'
import { messageApi } from '@/api/modules/message'
import { useMutation } from '@/composables/useMutation'

interface Props {
  nodeId?: number
}

const props = withDefaults(defineProps<Props>(), {
  nodeId: 0,
})

const dialog = defineModel<boolean>({ default: false })
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

const searchKeyword = ref('')
const searchResults = ref<any[]>([])
const searchResultInfo = ref('')
const loading = ref(false)
const invitedUserIds = ref<Set<number>>(new Set())

// 搜索用户
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    searchResultInfo.value = ''
    return
  }

  loading.value = true
  try {
    const response = await userApi.searchUser(searchKeyword.value)
    if (response.data && response.data.length > 0) {
      searchResults.value = response.data
      searchResultInfo.value = ''
    } else {
      searchResults.value = []
      searchResultInfo.value = '未找到用户'
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
    searchResults.value = []
    searchResultInfo.value = '搜索失败，请重试'
  } finally {
    loading.value = false
  }
}

// 邀请用户回答
const { execute: executeInvite } = useMutation(
  (userId: number) => messageApi.inviteUser(userId, props.nodeId),
  {
    successMessage: '邀请成功',
    onSuccess: (_, userId) => {
      invitedUserIds.value.add(userId)
    },
  }
)

// 邀请用户
const handleInvite = (user: any) => {
  executeInvite(user.id)
}

// 检查用户是否已邀请
const isInvited = (userId: number) => {
  return invitedUserIds.value.has(userId)
}

// 关闭对话框
const closeDialog = () => {
  dialog.value = false
  searchKeyword.value = ''
  searchResults.value = []
  searchResultInfo.value = ''
  invitedUserIds.value.clear()
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
          :loading="loading"
          append-inner-icon="mdi-magnify"
          @click:append-inner="handleSearch"
          @keyup.enter="handleSearch"
        ></v-text-field>

        <!-- 搜索结果 -->
        <div v-if="searchResults.length > 0" class="user-list mt-4">
          <div
            v-for="user in searchResults"
            :key="user.id"
            class="user-item d-flex justify-space-between align-center"
          >
            <!-- 用户信息 -->
            <div class="d-flex align-center">
              <v-avatar size="40" color="grey-lighten-2" class="mr-3">
                <v-img v-if="user.avatar" :src="user.avatar" />
                <v-icon v-else icon="mdi-account" color="grey" size="24"></v-icon>
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
              :disabled="isInvited(user.id)"
              @click="handleInvite(user)"
            >
              {{ isInvited(user.id) ? '已邀请' : '邀请' }}
            </v-btn>
          </div>
        </div>

        <!-- 空状态 -->
        <div
          v-if="searchResults.length === 0 && searchResultInfo"
          class="text-body-2 text-grey text-center py-8"
        >
          {{ searchResultInfo }}
        </div>

        <!-- 初始提示 -->
        <div
          v-if="searchResults.length === 0 && !searchResultInfo"
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
