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

const getStateText = (state: number): string => {
  switch (state) {
    case 1:
      return '正常'
    case 2:
      return '已屏蔽'
    default:
      return '未知'
  }
}

const getStateColor = (state: number): string => {
  switch (state) {
    case 1:
      return 'green-lighten-4'
    case 2:
      return 'red-lighten-4'
    default:
      return 'grey-lighten-3'
  }
}
</script>

<template>
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">用户管理</h2>

    <!-- 筛选区域 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-filter-variant" size="18" class="mr-2"></v-icon>
        搜索用户
      </v-card-title>
      <v-card-text>
        <div class="d-flex align-center ga-3">
          <v-text-field
            v-model="searchId"
            placeholder="按用户ID搜索"
            variant="outlined"
            density="compact"
            hide-details
            clearable
            style="max-width: 200px"
            @keydown.enter="searchById"
          ></v-text-field>
          <v-btn variant="tonal" size="default" @click="searchById">
            <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
            搜索
          </v-btn>

          <v-text-field
            v-model="searchName"
            placeholder="按用户名搜索"
            variant="outlined"
            density="compact"
            hide-details
            clearable
            style="max-width: 200px"
            @keydown.enter="searchByName"
          ></v-text-field>
          <v-btn variant="tonal" size="default" @click="searchByName">
            <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
            搜索
          </v-btn>

          <v-btn variant="text" size="default" @click="clearSearch">
            重置
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <!-- 用户列表 -->
    <v-card flat class="border">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-account-multiple" size="18" class="mr-2"></v-icon>
        用户列表
      </v-card-title>
      <v-card-text>
        <!-- 空状态 -->
        <div v-if="!loading && userList.length === 0" class="text-center py-12">
          <v-icon icon="mdi-account-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">暂无用户</p>
        </div>

        <!-- 列表 -->
        <div v-if="userList.length > 0">
          <div
            v-for="user in userList"
            :key="user.id"
            v-intersect="{
              handler: (isIntersecting: boolean) => {
                if (isIntersecting && user === userList[userList.length - 1] && hasMore && !loading) {
                  loadMore()
                }
              },
            }"
            class="list-item mb-3"
          >
            <div class="d-flex align-start">
              <!-- 操作区 -->
              <div class="action-area mr-4">
                <v-chip variant="flat" :color="getStateColor(user.state)" size="small" class="mb-4 d-flex justify-center">
                  {{ getStateText(user.state) }}
                </v-chip>

                <div class="d-flex flex-column ga-3">
                  <v-btn
                    v-if="user.state === 1"
                    variant="tonal"
                    color="error"
                    size="small"
                    block
                    @click="updateUserState(user, true)"
                  >
                    屏蔽
                  </v-btn>
                  <v-btn
                    v-else-if="user.state === 2"
                    variant="tonal"
                    color="success"
                    size="small"
                    block
                    @click="updateUserState(user, false)"
                  >
                    恢复
                  </v-btn>
                </div>
              </div>

              <!-- 内容区 -->
              <div class="flex-grow-1">
                <!-- 标题行 -->
                <div class="d-flex align-center justify-space-between mb-2">
                  <div class="d-flex align-center">
                    <UserAvatar
                      :name="user.name"
                      :avatar-url="user.avatar"
                      size="32"
                      rounded="lg"
                      class="mr-2"
                    />
                    <div class="text-body-1 font-weight-medium text-grey-darken-3">
                      {{ user.name }}
                    </div>
                  </div>
                  <div class="text-caption text-grey-darken-1">ID: {{ user.id }}</div>
                </div>

                <!-- 用户信息 -->
                <div class="content-wrapper">
                  <div class="d-flex align-center mb-2">
                    <v-icon icon="mdi-email" size="14" color="grey-darken-1" class="mr-2"></v-icon>
                    <span class="text-body-2 text-grey-darken-2">{{ user.email || '未设置邮箱' }}</span>
                    <v-chip
                      v-if="user.emailValidated"
                      variant="flat"
                      color="green-lighten-4"
                      size="x-small"
                      class="ml-2"
                    >
                      已验证
                    </v-chip>
                    <v-chip v-else variant="flat" color="orange-lighten-4" size="x-small" class="ml-2">
                      未验证
                    </v-chip>
                  </div>
                  <div v-if="user.phone" class="d-flex align-center mb-2">
                    <v-icon icon="mdi-phone" size="14" color="grey-darken-1" class="mr-2"></v-icon>
                    <span class="text-body-2 text-grey-darken-2">{{ user.phone }}</span>
                  </div>
                  <div v-if="user.biography" class="d-flex align-start mb-2">
                    <v-icon icon="mdi-text" size="14" color="grey-darken-1" class="mr-2 mt-1"></v-icon>
                    <span class="text-body-2 text-grey-darken-2">{{ user.biography }}</span>
                  </div>
                  <div class="d-flex align-center text-caption text-grey-darken-1">
                    <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                    注册时间: {{ user.createdAt }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载指示器 -->
        <div v-if="loading" class="text-center py-4">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 没有更多 -->
        <div v-if="!hasMore && userList.length > 0" class="text-center py-4 text-caption text-grey">
          没有更多了
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.list-item {
  padding: 16px;
  border-radius: 8px;
  background-color: #fafafa;
}

.action-area {
  width: 70px;
  flex-shrink: 0;
}

.content-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  background-color: white;
}
</style>
