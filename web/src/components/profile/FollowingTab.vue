<template>
  <div class="pa-0 pa-sm-1">
    <!-- 顶部信息栏 -->
    <div
      class="d-flex flex-column flex-sm-row align-start align-sm-center justify-space-between mb-4 mb-md-6 ga-3"
    >
      <div></div>
      <div class="text-caption text-grey">
        <span class="font-weight-bold text-primary">{{ stats.following }}</span> 关注 ·
        <span class="font-weight-bold text-success">{{ stats.followers }}</span> 粉丝
      </div>
    </div>

    <!-- 加载状态 -->
    <LoadingSpinner v-if="loading" />

    <!-- 用户列表 -->
    <div v-else-if="formattedUsers.length > 0">
      <div class="user-grid">
        <v-card
          v-for="user in formattedUsers"
          :key="user.id"
          rounded="lg"
          border
          hover
          class="user-card"
        >
          <v-card-text class="pa-3 position-relative">
            <v-btn
              color="grey"
              variant="text"
              size="x-small"
              icon="mdi-close"
              class="close-btn"
              @click.stop="unfollow(user.id)"
            />
            <div class="d-flex align-center ga-2">
              <UserAvatar
                :name="user.name"
                :avatar-url="user.avatar"
                :size="36"
                rounded="lg"
                class="flex-shrink-0"
              />
              <div class="flex-grow-1" style="min-width: 0">
                <div class="text-body-2 font-weight-bold text-truncate">
                  {{ user.name }}
                </div>
                <div class="text-caption text-medium-emphasis text-truncate">
                  {{ user.bio }}
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="text-center py-8 py-md-12">
      <v-icon
        icon="mdi-account-multiple"
        :size="$vuetify.display.mobile ? 48 : 64"
        color="grey-lighten-2"
        class="mb-3 mb-md-4"
      />
      <p class="text-body-2 text-md-body-1 text-grey-darken-2">暂无关注的人</p>
      <p class="text-caption text-md-body-2 text-grey">关注优秀的创作者，获取精彩内容</p>
    </div>

    <ConfirmDialog
      v-model="showUnfollowDialog"
      title="确认取消关注"
      message="确定要取消关注该用户吗？"
      confirm-text="确认取消"
      @confirm="confirmUnfollow"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { followApi } from '@/api'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'

const userStore = useUserStore()

// 获取关注的用户列表
const {
  data: followingUsers,
  loading,
  execute: fetchFollowees,
} = useFetch({
  fetchFn: () => {
    const userId = userStore.userId
    if (!userId) throw new Error('User ID not found')
    return followApi.getFollowees(userId)
  },
  immediate: true,
  defaultValue: [],
})

// 取消关注
const { execute: unfollowAction } = useMutation(
  (followeeId: number) => followApi.unfollow(followeeId),
  {
    successMessage: '已取消关注该用户',
    onSuccess: () => {
      fetchFollowees()
    },
  }
)

// 统计数据
const stats = computed(() => ({
  following: followingUsers.value?.length || 0,
  followers: 0,
}))

// 转换用户数据为组件所需格式
const formattedUsers = computed(() => {
  if (!followingUsers.value) return []

  return followingUsers.value.map((user) => {
    return {
      id: user.id,
      name: user.name || '未知用户',
      bio: user.biography || '暂无简介',
      avatar: user.avatar || '',
    }
  })
})

// 取消关注确认对话框
const showUnfollowDialog = ref(false)
const userToUnfollow = ref<number | null>(null)

// 取消关注
const unfollow = (userId: number) => {
  userToUnfollow.value = userId
  showUnfollowDialog.value = true
}

// 确认取消关注
const confirmUnfollow = async () => {
  if (userToUnfollow.value !== null) {
    await unfollowAction(userToUnfollow.value)
  }
  userToUnfollow.value = null
}
</script>

<style scoped>
.user-card {
  cursor: pointer;
  transition: all 0.2s;
  background-color: rgb(var(--v-theme-surface));
}

.user-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.close-btn {
  position: absolute;
  top: 4px;
  right: 4px;
}

/* 基于容器宽度的响应式网格 */
.user-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

@container (max-width: 1200px) {
  .user-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@container (max-width: 900px) {
  .user-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@container (max-width: 600px) {
  .user-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@container (max-width: 350px) {
  .user-grid {
    grid-template-columns: 1fr;
  }
}

/* 启用 container query */
.pa-0 {
  container-type: inline-size;
}
</style>
