<script setup>
  import { computed, onMounted, ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { userServiceV1 } from '@/services/api/v1/apiServiceV1'

  const props = defineProps({
    userId: {
      type: [String, Number],
      default: null, // null 表示当前用户
    },
    showStats: {
      type: Boolean,
      default: true,
    },
    size: {
      type: String,
      default: 'medium', // small, medium, large
      validator: (value) => ['small', 'medium', 'large'].includes(value),
    },
    editable: {
      type: Boolean,
      default: false,
    },
  })

  const emit = defineEmits(['edit', 'follow', 'unfollow'])

  const router = useRouter()

  // 用户数据
  const user = ref({})
  const loading = ref(false)
  const error = ref(null)

  // 统计数据
  const stats = ref({
    followeeCount: 0,
    followerCount: 0,
    subscriptionCount: 0,
    contentCount: 0,
  })

  const avatarSize = computed(() => {
    switch (props.size) {
      case 'small':
        return 48
      case 'large':
        return 80
      default:
        return 64
    }
  })

  // 加载用户信息
  const loadUser = async () => {
    try {
      loading.value = true
      error.value = null

      let response
      if (props.userId) {
        // 加载指定用户信息
        response = await userServiceV1.getUser(props.userId)
      } else {
        // 加载当前用户信息
        response = await userServiceV1.getCurrentUser()
      }

      if (response.code === 200) {
        user.value = response.data
        // 可以在这里加载统计数据
        // await loadUserStats();
      } else {
        error.value = '加载用户信息失败'
      }
    } catch (err) {
      console.error('Error loading user:', err)
      error.value = '加载用户信息失败'
    } finally {
      loading.value = false
    }
  }

  const handleEdit = () => {
    if (props.editable) {
      emit('edit', user.value)
    }
  }

  const handleFollow = () => {
    emit('follow', user.value)
  }

  const handleUnfollow = () => {
    emit('unfollow', user.value)
  }

  const handleStatClick = (statType) => {
    // 内部处理统计数据点击，直接进行路由导航
    switch (statType) {
      case 'subscriptions':
        router.push({ query: { tab: 'subscription' } })
        break
      case 'contents':
        router.push({ query: { tab: 'contents' } })
        break
      case 'followees':
        router.push({ query: { tab: 'follow' } })
        break
      case 'followers':
        // 可以后续添加粉丝页面
        console.log('Navigate to followers')
        break
      default:
        break
    }
  }

  onMounted(() => {
    loadUser()
  })

  // 暴露刷新方法供父组件调用
  defineExpose({
    refresh: loadUser,
  })
</script>

<template>
  <v-card rounded="xl" elevation="0" color="grey-lighten-5">
    <v-card-text class="pa-5">
      <!-- 加载状态 -->
      <div v-if="loading" class="text-center py-4">
        <v-progress-circular indeterminate color="primary"></v-progress-circular>
        <p class="text-body-2 text-grey-darken-1 mt-2">加载中...</p>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="text-center py-4">
        <v-icon icon="mdi-alert-circle" color="error" size="48" class="mb-2"></v-icon>
        <p class="text-body-2 text-error">{{ error }}</p>
        <v-btn size="small" variant="outlined" @click="loadUser">重试</v-btn>
      </div>

      <!-- 用户信息 -->
      <div v-else class="text-center">
        <!-- 用户头像 -->
        <v-avatar :size="avatarSize" class="mb-3 profile-avatar">
          <v-img
            :src="
              user.avatar || 'https://pica.zhimg.com/v2-b12a03a32cf776765897927720acb3bf_xll.jpg'
            "
            :alt="user.name"
          ></v-img>
        </v-avatar>

        <!-- 用户信息 -->
        <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-1">
          {{ user.name || '用户' }}
        </h3>
        <p class="text-body-2 text-grey-darken-2 mb-3">
          {{ user.biography || '暂无简介' }}
        </p>

        <!-- 操作按钮 -->
        <div v-if="editable || user.canFollow" class="mb-4">
          <v-btn
            v-if="editable"
            variant="outlined"
            color="primary"
            size="small"
            rounded="lg"
            @click="handleEdit"
          >
            <v-icon icon="mdi-pencil" size="16" class="mr-1"></v-icon>
            编辑资料
          </v-btn>

          <v-btn
            v-else-if="user.canFollow && !user.isFollowing"
            variant="flat"
            color="primary"
            size="small"
            rounded="lg"
            @click="handleFollow"
          >
            <v-icon icon="mdi-plus" size="16" class="mr-1"></v-icon>
            关注
          </v-btn>

          <v-btn
            v-else-if="user.canFollow && user.isFollowing"
            variant="outlined"
            color="grey"
            size="small"
            rounded="lg"
            @click="handleUnfollow"
          >
            <v-icon icon="mdi-check" size="16" class="mr-1"></v-icon>
            已关注
          </v-btn>
        </div>

        <!-- 统计数据 -->
        <div v-if="showStats" class="stats-container">
          <v-row class="ma-0" no-gutters>
            <v-col cols="3" class="text-center px-1">
              <div class="stat-item clickable" @click="handleStatClick('followees')">
                <div class="text-subtitle-1 font-weight-bold text-primary">
                  {{ stats.followeeCount }}
                </div>
                <div class="text-caption text-grey-darken-1 text-nowrap">关注</div>
              </div>
            </v-col>
            <v-col cols="3" class="text-center px-1">
              <div class="stat-item clickable" @click="handleStatClick('followers')">
                <div class="text-subtitle-1 font-weight-bold text-success">
                  {{ stats.followerCount }}
                </div>
                <div class="text-caption text-grey-darken-1 text-nowrap">粉丝</div>
              </div>
            </v-col>
            <v-col cols="3" class="text-center px-1">
              <div class="stat-item clickable" @click="handleStatClick('subscriptions')">
                <div class="text-subtitle-1 font-weight-bold text-info">
                  {{ stats.subscriptionCount }}
                </div>
                <div class="text-caption text-grey-darken-1 text-nowrap">关注课程</div>
              </div>
            </v-col>
            <v-col cols="3" class="text-center px-1">
              <div class="stat-item clickable" @click="handleStatClick('contents')">
                <div class="text-subtitle-1 font-weight-bold text-warning">
                  {{ stats.contentCount }}
                </div>
                <div class="text-caption text-grey-darken-1 text-nowrap">创建内容</div>
              </div>
            </v-col>
          </v-row>
        </div>
      </div>
    </v-card-text>
  </v-card>
</template>

<style scoped>
  .profile-avatar {
    border: 3px solid white;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .stat-item {
    padding: 4px;
    border-radius: 8px;
    transition: background-color 0.2s ease;
  }

  .stat-item.clickable {
    cursor: pointer;
  }

  .stat-item.clickable:hover {
    background-color: rgba(0, 0, 0, 0.04);
  }

  .stats-container {
    border-top: 1px solid rgba(0, 0, 0, 0.12);
    padding-top: 16px;
    margin-top: 8px;
  }
</style>
