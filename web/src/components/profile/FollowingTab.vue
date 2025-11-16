<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">关注的人</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">管理您关注的用户，查看他们的最新动态。</p>
          <v-divider class="my-3" />
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-rss" size="14" class="mr-1" />
              订阅用户更新
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-account-check" size="14" class="mr-1" />
              互相关注提醒
            </div>
            <div>
              <v-icon icon="mdi-message" size="14" class="mr-1" />
              私信交流
            </div>
          </div>
        </div>
      </div>
    </v-col>

    <!-- 右侧主内容 -->
    <v-col cols="12" md="10">
      <div class="pa-2">
        <div class="d-flex align-center justify-space-between mb-4">
          <v-icon icon="mdi-menu" size="18" color="grey-lighten-1" />

          <div class="text-body-2 text-grey">
            <span class="font-weight-bold text-primary">{{ stats.following }}</span> 关注 ·
            <span class="font-weight-bold text-success">{{ stats.followers }}</span> 粉丝
          </div>
        </div>

        <!-- 用户列表 -->
        <div v-if="formattedUsers.length > 0">
          <v-row>
            <v-col v-for="user in formattedUsers" :key="user.id" cols="12" sm="6" md="4">
              <v-card border rounded="lg" hover class="hoverable">
                <v-card-text class="pa-4">
                  <div class="d-flex align-start">
                    <!-- 用户头像 -->
                    <v-avatar :color="user.avatarColor" size="48" class="mr-3">
                      <span class="text-h6 font-weight-bold text-white">{{ user.initial }}</span>
                    </v-avatar>

                    <!-- 用户信息 -->
                    <div class="flex-grow-1">
                      <h4 class="text-body-2 font-weight-bold mb-1">{{ user.name }}</h4>
                      <p class="text-caption text-grey mb-0">{{ user.bio }}</p>
                    </div>

                    <!-- 操作按钮 -->
                    <v-btn
                      color="grey"
                      variant="tonal"
                      size="x-small"
                      icon="mdi-close"
                      @click.stop="unfollow(user.id)"
                    />
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-account-multiple" size="64" color="grey-lighten-2" class="mb-4" />
          <p class="text-body-1 text-grey-darken-2">暂无关注的人</p>
          <p class="text-body-2 text-grey">关注优秀的创作者，获取精彩内容</p>
        </div>

        <!-- 取消关注确认对话框 -->
        <ConfirmDialog
          v-model="showUnfollowDialog"
          title="确认取消关注"
          message="确定要取消关注该用户吗？"
          confirm-text="确认取消"
          @confirm="confirmUnfollow"
        />
      </div>
    </v-col>
  </v-row>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import { followApi } from '@/api'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

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
  followers: 0, // TODO: 需要添加获取粉丝数量的 API
}))

// 转换用户数据为组件所需格式
const formattedUsers = computed(() => {
  if (!followingUsers.value) return []

  return followingUsers.value.map((user) => {
    // 获取用户名首字母
    const initial = user.name ? user.name.charAt(0).toUpperCase() : '?'

    // 生成头像颜色
    const colors = ['blue', 'pink', 'green', 'purple', 'orange', 'teal', 'indigo', 'red']
    const colorIndex = user.id % colors.length
    const avatarColor = colors[colorIndex]

    return {
      id: user.id,
      name: user.name || '未知用户',
      bio: user.biography || '暂无简介',
      posts: 0, // TODO: 如果需要显示帖子数，需要额外的 API
      followers: 0, // TODO: 如果需要显示粉丝数，需要额外的 API
      initial,
      avatarColor,
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
/* 左侧边栏固定 */
.sticky-sidebar {
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

/* 移动端取消 sticky */
@media (max-width: 960px) {
  .sticky-sidebar {
    position: relative;
    top: 0;
    max-height: none;
    margin-bottom: 16px;
  }
}
</style>
