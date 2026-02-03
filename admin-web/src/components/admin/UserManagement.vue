<script setup lang="ts">
import { inject, ref } from 'vue'
import { adminApi } from '@/api'
import type { User } from '@/types/user.d'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'
import UserAvatar from '@/components/common/UserAvatar.vue'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

const searchId = ref<string>('')
const searchName = ref<string>('')
const isSearchMode = ref<boolean>(false)

// 使用 useInfiniteScroll 加载用户列表
const {
  items: userList,
  loading,
  hasMore,
  loadMore,
  reset: resetUserList,
} = useInfiniteScroll({
  fetchFn: (params) => {
    return adminApi.getUsers(params.lastId)
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: {
    lastId: null,
  },
  enabled: !isSearchMode.value,
})

const searchById = async (): Promise<void> => {
  if (!searchId.value.trim()) {
    return
  }

  const userId = parseInt(searchId.value.trim())
  if (isNaN(userId)) {
    showSnackbar?.('请输入有效的用户ID', 'error')
    return
  }

  isSearchMode.value = true
  loading.value = true

  try {
    const response = await adminApi.getUserById(userId)
    if (response.code === 200) {
      userList.value = [response.data]
    } else {
      userList.value = []
      showSnackbar?.(response.message || '未找到用户', 'error')
    }
  } catch (error) {
    console.error('Error searching user by id:', error)
    showSnackbar?.('搜索失败', 'error')
    userList.value = []
  } finally {
    loading.value = false
  }
}

const searchByName = async (): Promise<void> => {
  if (!searchName.value.trim()) {
    return
  }

  isSearchMode.value = true
  loading.value = true

  try {
    const response = await adminApi.adminSearchUser(searchName.value.trim())
    if (response.code === 200) {
      userList.value = response.data
    } else {
      userList.value = []
      showSnackbar?.(response.message || '搜索失败', 'error')
    }
  } catch (error) {
    console.error('Error searching users by name:', error)
    showSnackbar?.('搜索失败', 'error')
    userList.value = []
  } finally {
    loading.value = false
  }
}

const clearSearch = (): void => {
  searchId.value = ''
  searchName.value = ''
  isSearchMode.value = false
  resetUserList()
}

// 使用 useMutation 更新用户状态
const { execute: executeUpdateUserState } = useMutation(
  (data: { userId: number; ban: boolean }) => adminApi.updateUserState(data.userId, data.ban),
  {
    successMessage: '操作成功',
    onSuccess: (response, data) => {
      const index = userList.value.findIndex((u) => u.id === data.userId)
      if (index !== -1) {
        userList.value[index] = response
      }
    },
  }
)

const updateUserState = async (user: User, ban: boolean): Promise<void> => {
  await executeUpdateUserState({ userId: user.id, ban })
}
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
          <v-icon icon="mdi-account-multiple" color="teal-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">用户管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">查看和管理平台用户</p>
        </div>
      </div>
    </div>

    <div class="mb-6">
      <div class="d-flex ga-3">
        <v-text-field
          v-model="searchId"
          placeholder="按用户ID搜索"
          prepend-inner-icon="mdi-numeric"
          variant="outlined"
          density="compact"
          hide-details
          clearable
          style="max-width: 300px"
          @keydown.enter="searchById"
        >
          <template #append-inner>
            <v-btn color="teal" variant="flat" size="small" @click="searchById"> 搜索 </v-btn>
          </template>
        </v-text-field>

        <v-text-field
          v-model="searchName"
          placeholder="按用户名搜索"
          prepend-inner-icon="mdi-account-search"
          variant="outlined"
          density="compact"
          hide-details
          clearable
          style="max-width: 300px"
          @keydown.enter="searchByName"
        >
          <template #append-inner>
            <v-btn color="teal" variant="flat" size="small" @click="searchByName"> 搜索 </v-btn>
          </template>
        </v-text-field>

        <v-btn variant="outlined" color="grey" size="large" @click="clearSearch">
          <v-icon icon="mdi-refresh" class="mr-1"></v-icon>
          重置
        </v-btn>
      </div>
    </div>

    <div v-if="userList.length === 0 && !loading" class="text-center py-12">
      <v-icon icon="mdi-account-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">暂无用户</p>
    </div>

    <div
      v-for="user in userList"
      :key="user.id"
      v-intersect="{
        handler: (isIntersecting) => {
          if (isIntersecting && user === userList[userList.length - 1] && hasMore && !loading) {
            loadMore()
          }
        },
      }"
      class="mb-4"
    >
      <v-card flat class="border rounded-lg pa-5" hover>
        <div class="d-flex align-start">
          <UserAvatar
            :name="user.name"
            :avatar-url="user.avatar"
            size="48"
            rounded="lg"
            class="mr-4"
          />

          <div class="flex-grow-1">
            <div class="d-flex align-center justify-space-between mb-2">
              <div class="d-flex align-center">
                <div class="text-h6 font-weight-medium text-grey-darken-3 mr-2">
                  {{ user.name }}
                </div>
                <v-chip
                  v-if="user.state === 1"
                  variant="flat"
                  color="green-lighten-4"
                  size="x-small"
                >
                  <v-icon
                    icon="mdi-check-circle"
                    size="12"
                    class="mr-1"
                    color="green-darken-2"
                  ></v-icon>
                  <span class="text-green-darken-2">正常</span>
                </v-chip>
                <v-chip
                  v-else-if="user.state === 2"
                  variant="flat"
                  color="red-lighten-4"
                  size="x-small"
                >
                  <v-icon
                    icon="mdi-block-helper"
                    size="12"
                    class="mr-1"
                    color="red-darken-2"
                  ></v-icon>
                  <span class="text-red-darken-2">已屏蔽</span>
                </v-chip>
              </div>
              <div class="text-caption text-grey-darken-1">ID: {{ user.id }}</div>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-3 mb-3">
              <div class="d-flex align-center mb-2">
                <v-icon icon="mdi-email" size="16" color="grey-darken-1" class="mr-2"></v-icon>
                <span class="text-body-2 text-grey-darken-2">{{ user.email || '未设置邮箱' }}</span>
                <v-chip
                  v-if="user.emailValidated"
                  variant="flat"
                  color="green-lighten-4"
                  size="x-small"
                  class="ml-2"
                >
                  <v-icon
                    icon="mdi-check-circle"
                    size="12"
                    class="mr-1"
                    color="green-darken-2"
                  ></v-icon>
                  <span class="text-green-darken-2">已验证</span>
                </v-chip>
                <v-chip v-else variant="flat" color="orange-lighten-4" size="x-small" class="ml-2">
                  <v-icon
                    icon="mdi-alert-circle"
                    size="12"
                    class="mr-1"
                    color="orange-darken-2"
                  ></v-icon>
                  <span class="text-orange-darken-2">未验证</span>
                </v-chip>
              </div>
              <div v-if="user.phone" class="d-flex align-center mb-2">
                <v-icon icon="mdi-phone" size="16" color="grey-darken-1" class="mr-2"></v-icon>
                <span class="text-body-2 text-grey-darken-2">{{ user.phone }}</span>
              </div>
              <div v-if="user.biography" class="d-flex align-start">
                <v-icon icon="mdi-text" size="16" color="grey-darken-1" class="mr-2 mt-1"></v-icon>
                <span class="text-body-2 text-grey-darken-2">{{ user.biography }}</span>
              </div>
            </div>

            <div class="d-flex align-center text-caption text-grey-darken-1">
              <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
              注册时间: {{ user.createdAt }}
            </div>

            <div class="d-flex align-center mt-3 ga-2">
              <v-btn
                v-if="user.state === 1"
                variant="flat"
                color="red-lighten-4"
                size="small"
                @click="updateUserState(user, true)"
              >
                <v-icon
                  icon="mdi-block-helper"
                  size="16"
                  class="mr-1"
                  color="red-darken-2"
                ></v-icon>
                <span class="text-red-darken-2">屏蔽用户</span>
              </v-btn>
              <v-btn
                v-else-if="user.state === 2"
                variant="flat"
                color="green-lighten-4"
                size="small"
                @click="updateUserState(user, false)"
              >
                <v-icon
                  icon="mdi-check-circle"
                  size="16"
                  class="mr-1"
                  color="green-darken-2"
                ></v-icon>
                <span class="text-green-darken-2">恢复用户</span>
              </v-btn>
            </div>
          </div>
        </div>
      </v-card>
    </div>

    <div v-if="loading" class="text-center py-4">
      <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
      <span class="ml-2 text-grey-darken-1">加载中...</span>
    </div>

    <div v-if="!hasMore && userList.length > 0" class="text-center py-4">
      <span class="text-grey-darken-1">没有更多数据了</span>
    </div>
  </div>
</template>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}
</style>
