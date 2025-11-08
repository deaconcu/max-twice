<template>
  <v-row dense align="start">
    <!-- 左侧简介栏 -->
    <v-col cols="12" md="2">
      <div class="sticky-sidebar">
        <div class="pa-2 pr-10 pt-4">
          <div class="mb-3">
            <h4 class="text-body-1 font-weight-bold">关注的人</h4>
          </div>
          <p class="text-body-2 text-grey-darken-2 mb-3">
            管理您关注的用户，查看他们的最新动态。
          </p>
          <v-divider class="my-3"></v-divider>
          <div class="text-caption text-grey">
            <div class="mb-2">
              <v-icon icon="mdi-rss" size="14" class="mr-1"></v-icon>
              订阅用户更新
            </div>
            <div class="mb-2">
              <v-icon icon="mdi-account-check" size="14" class="mr-1"></v-icon>
              互相关注提醒
            </div>
            <div>
              <v-icon icon="mdi-message" size="14" class="mr-1"></v-icon>
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
          <v-icon icon="mdi-menu" size="18" color="grey-lighten-1"></v-icon>

          <div class="text-body-2 text-grey">
            <span class="font-weight-bold text-primary">{{ stats.following }}</span> 关注 ·
            <span class="font-weight-bold text-success">{{ stats.followers }}</span> 粉丝
          </div>
        </div>

        <!-- 用户列表 -->
        <div v-if="followingUsers.length > 0">
          <v-row>
            <v-col
              v-for="user in followingUsers"
              :key="user.id"
              cols="12"
              sm="6"
              md="4"
            >
              <v-card
                border
                rounded="lg"
                hover
                class="hoverable"
              >
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
                    ></v-btn>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>
        </div>

        <!-- 空状态 -->
        <div v-else class="text-center py-12">
          <v-icon icon="mdi-account-multiple" size="64" color="grey-lighten-2" class="mb-4"></v-icon>
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
import { ref } from 'vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

// 统计数据
const stats = ref({
  following: 8,
  followers: 24
})

// 取消关注确认对话框
const showUnfollowDialog = ref(false)
const userToUnfollow = ref<number | null>(null)

// Mock 关注的用户数据
const followingUsers = ref([
  {
    id: 1,
    name: '李明',
    bio: '前端工程师 | Vue.js 爱好者',
    posts: 45,
    followers: 1234,
    initial: '李',
    avatarColor: 'blue'
  },
  {
    id: 2,
    name: '王芳',
    bio: 'UI/UX 设计师 | 热爱创作',
    posts: 32,
    followers: 892,
    initial: '王',
    avatarColor: 'pink'
  },
  {
    id: 3,
    name: '陈浩',
    bio: '全栈开发 | 技术分享',
    posts: 67,
    followers: 2145,
    initial: '陈',
    avatarColor: 'green'
  },
  {
    id: 4,
    name: '赵薇',
    bio: '产品经理 | 用户体验专家',
    posts: 28,
    followers: 756,
    initial: '赵',
    avatarColor: 'purple'
  },
  {
    id: 5,
    name: '刘强',
    bio: '后端架构师 | 分布式系统',
    posts: 52,
    followers: 1567,
    initial: '刘',
    avatarColor: 'orange'
  },
  {
    id: 6,
    name: '杨静',
    bio: '数据分析师 | Python 爱好者',
    posts: 38,
    followers: 934,
    initial: '杨',
    avatarColor: 'teal'
  }
])

// 取消关注
const unfollow = (userId: number) => {
  userToUnfollow.value = userId
  showUnfollowDialog.value = true
}

// 确认取消关注
const confirmUnfollow = () => {
  if (userToUnfollow.value !== null) {
    followingUsers.value = followingUsers.value.filter(u => u.id !== userToUnfollow.value)
    stats.value.following -= 1
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
