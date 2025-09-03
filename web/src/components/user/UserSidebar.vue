<script setup>
  import { computed, onMounted, ref, watch } from 'vue'
  import { userServiceV1 } from '@/services/api/v1/apiServiceV1'

  // Props
  const props = defineProps({
    selectedTab: {
      type: String,
      default: 'info',
    },
    items: {
      type: Array,
      required: true,
    },
    userId: {
      type: [String, Number],
      default: null, // null 表示当前用户
    },
  })

  // Emits
  const emit = defineEmits(['update:selectedTab', 'tab-change'])

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

  // 处理标签选择
  const handleTabSelect = (value) => {
    emit('update:selectedTab', value)
    emit('tab-change', value)
  }

  // 获取用户头像字母
  const getUserInitial = () => {
    return user.value.name ? user.value.name.charAt(0).toUpperCase() : 'U'
  }

  // 用户统计数据 (模拟数据，实际应该从 API 获取)
  const userStats = computed(() => ({
    posts: stats.value.contentCount || 12,
    followers: stats.value.followerCount || 156,
    following: stats.value.followeeCount || 89,
    subscriptions: stats.value.subscriptionCount || 25,
  }))

  onMounted(() => {
    loadUser()
  })

  // 监听 userId 变化
  watch(
    () => props.userId,
    () => {
      loadUser()
    }
  )
</script>

<template>
  <v-col cols="auto" class="pr-4 pt-0 sidebar-width">
    <v-card flat color="grey-lighten-5" rounded="xl" class="sticky-nav">
      <v-card-text class="pa-4">
        <!-- 用户信息区域 -->
        <div v-if="loading" class="text-center py-4">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <p class="text-caption text-grey-darken-1 mt-2">加载中...</p>
        </div>

        <div v-else-if="error" class="text-center py-4">
          <v-icon icon="mdi-alert-circle" color="error" size="32" class="mb-2"></v-icon>
          <p class="text-caption text-error">{{ error }}</p>
        </div>

        <div v-else class="text-center mb-4">
          <!-- 用户头像 - 使用真实头像或首字母 -->
          <v-avatar v-if="user.avatar" size="64" class="mb-3">
            <v-img :src="user.avatar" :alt="user.name"></v-img>
          </v-avatar>
          <v-avatar v-else color="primary" size="64" class="mb-3">
            <span class="text-white text-h5 font-weight-bold">{{ getUserInitial() }}</span>
          </v-avatar>

          <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-1">
            {{ user.name || '未登录用户' }}
          </h3>
          <p class="text-body-2 text-grey-darken-2 mb-2">
            {{ user.email || '请登录账户' }}
          </p>
          <p class="text-caption text-grey-darken-1">
            {{ user.biography || '这个人很懒，什么都没留下' }}
          </p>
        </div>

        <!-- 用户统计简要信息 -->
        <div v-if="!loading && !error" class="mb-4 pa-3 user-stats-container">
          <div class="d-flex justify-space-around text-center">
            <div>
              <div class="text-h6 font-weight-bold text-primary">{{ userStats.posts }}</div>
              <div class="text-caption text-grey-darken-2">内容</div>
            </div>
            <div>
              <div class="text-h6 font-weight-bold text-primary">{{ userStats.followers }}</div>
              <div class="text-caption text-grey-darken-2">关注者</div>
            </div>
            <div>
              <div class="text-h6 font-weight-bold text-primary">{{ userStats.following }}</div>
              <div class="text-caption text-grey-darken-2">关注</div>
            </div>
            <div>
              <div class="text-h6 font-weight-bold text-primary">{{ userStats.subscriptions }}</div>
              <div class="text-caption text-grey-darken-2">订阅</div>
            </div>
          </div>
        </div>

        <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-4">
          <v-icon icon="mdi-view-dashboard" color="primary" size="18" class="mr-2"></v-icon>
          功能导航
        </h3>

        <v-list bg-color="transparent" class="pa-0">
          <v-list-item
            v-for="item in items"
            :key="item.value"
            :value="item.value"
            :ripple="false"
            class="nav-item ma-1 rounded-lg"
            :class="[selectedTab === item.value ? 'nav-item-active' : 'nav-item-inactive']"
            @click="handleTabSelect(item.value)"
          >
            <template #prepend>
              <v-avatar
                :color="selectedTab === item.value ? 'primary' : 'grey-lighten-2'"
                size="32"
                class="mr-3"
              >
                <v-icon
                  :icon="item.icon"
                  :color="selectedTab === item.value ? 'white' : 'grey-darken-2'"
                  size="16"
                ></v-icon>
              </v-avatar>
            </template>

            <v-list-item-title
              class="font-weight-medium"
              :class="selectedTab === item.value ? 'text-primary' : 'text-grey-darken-3'"
            >
              {{ item.text }}
            </v-list-item-title>

            <template #append>
              <v-icon
                icon="mdi-chevron-right"
                :color="selectedTab === item.value ? 'primary' : 'grey-lighten-1'"
                size="16"
              ></v-icon>
            </template>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>
  </v-col>
</template>

<style scoped>
  .sidebar-width {
    width: 320px;
  }

  .sticky-nav {
    position: sticky;
    top: 65px;
  }

  /* 导航项样式 - 匹配 MessageSidebar */
  .nav-item {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    cursor: pointer;
    border: 1px solid transparent;
    padding: 8px 12px;
  }

  .nav-item-inactive {
    background: rgba(255, 255, 255, 0.7);
  }

  .nav-item-inactive:hover {
    background: rgba(25, 118, 210, 0.08);
    border-color: rgba(25, 118, 210, 0.2);
  }

  .nav-item-active {
    background: rgba(189, 189, 189, 0.1) !important;
    border-color: rgba(189, 189, 189, 0.2) !important;
  }

  .nav-item-active:hover {
    background: rgba(189, 189, 189, 0.15) !important;
    border-color: rgba(189, 189, 189, 0.3) !important;
  }

  .user-stats-container {
    background: rgba(25, 118, 210, 0.05);
    border-radius: 8px;
  }
</style>
