<script setup lang="ts">
import { inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { adminApi } from '@/api'
import { UserRole } from '@/enums'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

const { t } = useI18n()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 用户角色选项
const userRoles = [
  { value: UserRole.USER, text: '普通用户', color: 'blue' },
  { value: UserRole.MODERATOR, text: '版主', color: 'purple' },
  { value: UserRole.ADMIN, text: '管理员', color: 'red' },
  { value: UserRole.SUPER_ADMIN, text: '超级管理员', color: 'pink' }
]

// 获取角色配置
const getRoleConfig = (role: number) => {
  return userRoles.find(r => r.value === role) || userRoles[0]
}

// 使用 useInfiniteScroll 加载用户列表
const {
  items: userList,
  loading,
  hasMore,
  loadMore,
  reset: resetUserList
} = useInfiniteScroll({
  fetchFn: async (params) => {
    const response = await adminApi.getUsers(params.lastId)
    return response.data
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id
  }),
  initialParams: {
    lastId: undefined
  }
})

// 使用 useMutation 更新用户角色
const { execute: executeUpdateRole } = useMutation(
  (data: { userId: number; role: number }) => adminApi.updateUserRole(data.userId, data.role),
  {
    successMessage: '用户角色已更新',
    onSuccess: (_, data) => {
      const user = userList.value.find((u: any) => u.id === data.userId)
      if (user) {
        user.role = data.role
      }
    }
  }
)

const updateUserRole = async (user: any, newRole: number): Promise<void> => {
  await executeUpdateRole({ userId: user.id, role: newRole })
}

// 使用 useMutation 封禁/解封用户
const { execute: executeBanUser } = useMutation(
  (data: { userId: number; banned: boolean }) => adminApi.banUser(data.userId, data.banned),
  {
    onSuccess: (_, data) => {
      const message = data.banned ? '用户已封禁' : '用户已解封'
      showSnackbar?.(message, 'success')

      const user = userList.value.find((u: any) => u.id === data.userId)
      if (user) {
        user.banned = data.banned
      }
    }
  }
)

const toggleUserBan = async (user: any): Promise<void> => {
  await executeBanUser({ userId: user.id, banned: !user.banned })
}

// 格式化日期
const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString()
}
</script>

<template>
  <div class="user-management">
    <!-- 页面头部 -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-cyan-lighten-5 mr-3">
          <v-icon icon="mdi-account-group" color="cyan-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">用户管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">管理用户信息和权限</p>
        </div>
      </div>
      <v-chip variant="flat" color="cyan-lighten-4" rounded="lg">
        <v-icon icon="mdi-account" color="cyan-darken-2" size="16" class="mr-2"></v-icon>
        <span class="text-cyan-darken-2 text-caption">{{ userList.length }}个用户</span>
      </v-chip>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && userList.length === 0" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
      <p class="mt-3 text-grey-darken-1">加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="userList.length === 0" class="text-center py-12">
      <v-icon icon="mdi-account-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">暂无用户数据</p>
    </div>

    <!-- 用户列表 -->
    <div v-else>
      <div
        v-for="user in userList"
        :key="user.id"
        class="mb-4"
        v-intersect="{
          handler: (isIntersecting: boolean) => {
            if (isIntersecting && user === userList[userList.length - 1] && hasMore && !loading) {
              loadMore()
            }
          }
        }"
      >
        <v-card flat class="border rounded-lg pa-5" hover>
          <div class="d-flex align-start">
            <!-- 头像和基本信息区域 -->
            <div class="mr-4">
              <v-avatar size="64" color="grey-lighten-3">
                <v-img v-if="user.avatar" :src="user.avatar" />
                <v-icon v-else icon="mdi-account" color="grey-darken-1" size="32"></v-icon>
              </v-avatar>
            </div>

            <!-- 用户信息区域 -->
            <div class="flex-grow-1">
              <div class="d-flex align-center justify-space-between mb-3">
                <div>
                  <h4 class="text-h6 font-weight-bold text-grey-darken-3 mb-1">
                    {{ user.name }}
                    <v-chip
                      v-if="user.banned"
                      variant="flat"
                      color="red-lighten-4"
                      rounded="lg"
                      size="small"
                      class="ml-2"
                    >
                      <v-icon icon="mdi-cancel" size="14" class="mr-1"></v-icon>
                      已封禁
                    </v-chip>
                  </h4>
                  <p class="text-body-2 text-grey-darken-1 mb-0">{{ user.email }}</p>
                </div>
                <div class="text-caption text-grey-darken-1">
                  ID: {{ user.id }}
                </div>
              </div>

              <div class="d-flex align-center justify-space-between mb-3">
                <div class="d-flex align-center gap-4">
                  <!-- 角色显示和修改 -->
                  <div class="d-flex align-center">
                    <span class="text-body-2 text-grey-darken-2 mr-2">角色:</span>
                    <v-select
                      :model-value="user.role"
                      :items="userRoles"
                      item-title="text"
                      item-value="value"
                      variant="outlined"
                      density="compact"
                      rounded="lg"
                      bg-color="white"
                      hide-details
                      style="min-width: 120px"
                      @update:model-value="updateUserRole(user, $event)"
                    >
                      <template #selection="{ item }">
                        <v-chip
                          :color="item.raw.color"
                          variant="flat"
                          rounded="lg"
                          size="small"
                        >
                          {{ item.raw.text }}
                        </v-chip>
                      </template>
                    </v-select>
                  </div>

                  <!-- 统计信息 -->
                  <div class="d-flex align-center gap-2">
                    <v-chip variant="outlined" size="small" rounded="lg">
                      <v-icon icon="mdi-eye" size="14" class="mr-1"></v-icon>
                      {{ user.views || 0 }} 浏览
                    </v-chip>
                    <v-chip variant="outlined" size="small" rounded="lg">
                      <v-icon icon="mdi-thumb-up" size="14" class="mr-1"></v-icon>
                      {{ user.upvotes || 0 }} 点赞
                    </v-chip>
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="d-flex align-center gap-2">
                  <v-btn
                    :variant="user.banned ? 'flat' : 'outlined'"
                    :color="user.banned ? 'green' : 'red'"
                    rounded="lg"
                    size="small"
                    @click="toggleUserBan(user)"
                  >
                    <v-icon
                      :icon="user.banned ? 'mdi-lock-open' : 'mdi-cancel'"
                      size="16"
                      class="mr-1"
                    ></v-icon>
                    {{ user.banned ? '解封' : '封禁' }}
                  </v-btn>
                </div>
              </div>

              <!-- 时间信息 -->
              <div class="d-flex align-center justify-between">
                <div class="text-body-2 text-grey-darken-1">
                  注册时间：{{ formatDate(user.createdAt) }}
                </div>
                <div v-if="user.lastLoginAt" class="text-body-2 text-grey-darken-1">
                  最后登录：{{ formatDate(user.lastLoginAt) }}
                </div>
              </div>
            </div>
          </div>
        </v-card>
      </div>
    </div>

    <!-- 加载更多指示器 -->
    <div v-if="loading" class="text-center py-4">
      <v-progress-circular
        indeterminate
        color="primary"
        size="24"
      ></v-progress-circular>
      <span class="ml-2 text-grey-darken-1">加载中...</span>
    </div>

    <!-- 没有更多数据提示 -->
    <div v-if="!hasMore && userList.length > 0" class="text-center py-4">
      <span class="text-grey-darken-1">没有更多数据了</span>
    </div>
  </div>
</template>

<style scoped>
.user-management {
  max-width: 100%;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}
</style>