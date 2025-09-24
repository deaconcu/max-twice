<script setup>
  import { inject, onMounted, ref, computed } from 'vue'
  import { useI18n } from 'vue-i18n'
  import { postServiceV1 } from '@/services/api/v1/apiServiceV1'
  import { POST_STATE, POST_TYPE } from '@/constants/statusConstants'

  const { t } = useI18n()
  const showSnackbar = inject('showSnackbar')

  const allPostList = ref([])
  const currentTab = ref('pending')

  // 标签配置
  const tabs = [
    {
      key: 'pending',
      label: t('admin.pending'),
      state: POST_STATE.SUBMITTED,
      icon: 'mdi-clock-outline',
      color: 'orange'
    },
    {
      key: 'approved',
      label: t('admin.approved'),
      state: POST_STATE.APPROVED,
      icon: 'mdi-check-circle',
      color: 'green'
    },
    {
      key: 'rejected',
      label: t('admin.rejected'),
      state: POST_STATE.DELETED,
      icon: 'mdi-close-circle',
      color: 'red'
    }
  ]

  // 直接显示当前tab的数据，不需要过滤
  const postList = computed(() => {
    return allPostList.value
  })

  // 根据当前tab获取对应状态的帖子
  const getPostsByTab = async (tabKey) => {
    try {
      const response = await postServiceV1.getPostsByState(tabKey)

      if (response.code === 401) {
        console.log('not login')
      } else if (response.code === 200) {
        allPostList.value = response.data
        console.log('done')
      }
    } catch (error) {
      console.error('Error loading posts:', error)
    }
  }

  const getPostSensorList = async () => {
    await getPostsByTab(currentTab.value)
  }

  const approvePost = async (post, approve) => {
    try {
      const response = await postServiceV1.approvePost(post.id, approve)

      if (response.code === 401) {
        console.log('not login')
      } else if (response.code === 200) {
        console.log('done')
        console.log(`post: ${JSON.stringify(response.data)}`)

        // 审核操作后重新加载当前tab的数据
        await getPostsByTab(currentTab.value)

        showSnackbar(t('admin.operationSuccess'))
      }
    } catch (error) {
      console.error('Error verifying login status:', error)
    }
  }

  // 监听tab切换，重新加载数据
  const handleTabChange = async (newTab) => {
    await getPostsByTab(newTab)
  }

  onMounted(() => {
    getPostSensorList()
  })
</script>

<template>
  <div>
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
          <v-icon icon="mdi-note-check-outline" color="teal-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">
            {{ t('admin.articleReview') }}
          </h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">{{ t('admin.reviewUserArticles') }}</p>
        </div>
      </div>
      <v-chip variant="flat" color="blue-lighten-4" rounded="lg">
        <v-icon
          icon="mdi-file-document-multiple"
          color="blue-darken-2"
          size="16"
          class="mr-1"
        ></v-icon>
        <span class="text-blue-darken-2 text-caption"
          >{{ postList.length }} {{ tabs.find(tab => tab.key === currentTab)?.label }}</span
        >
      </v-chip>
    </div>

    <!-- 状态标签 -->
    <v-tabs
      v-model="currentTab"
      color="primary"
      class="mb-6"
      show-arrows
      @update:model-value="handleTabChange"
    >
      <v-tab
        v-for="tab in tabs"
        :key="tab.key"
        :value="tab.key"
        class="text-none"
      >
        <v-icon
          :icon="tab.icon"
          :color="`${tab.color}-darken-1`"
          size="18"
          class="mr-2"
        ></v-icon>
        {{ tab.label }}
      </v-tab>
    </v-tabs>

    <div v-if="postList.length === 0" class="text-center py-12">
      <v-icon
        icon="mdi-file-document-outline"
        size="48"
        color="grey-lighten-1"
        class="mb-4"
      ></v-icon>
      <p class="text-body-1 text-grey-darken-1">
        {{ currentTab === 'pending' ? t('admin.noArticlesToReview') : `暂无${tabs.find(tab => tab.key === currentTab)?.label}的文章` }}
      </p>
    </div>

    <div v-for="post in postList" :key="post.id" class="mb-4">
      <v-card flat class="border rounded-lg pa-5" hover>
        <div class="d-flex align-start">
          <!-- 状态和操作区域 -->
          <div class="mr-4 action-area">
            <div class="mb-3">
              <v-chip
                v-if="post.state == POST_STATE.SUBMITTED"
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                {{ t('admin.pending') }}
              </v-chip>
              <v-chip
                v-if="post.state == POST_STATE.APPROVED"
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-check-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.approved') }}
              </v-chip>
              <v-chip
                v-if="post.state == POST_STATE.DELETED"
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
              >
                <v-icon icon="mdi-close-circle" size="14" class="mr-1"></v-icon>
                {{ t('admin.rejected') }}
              </v-chip>
            </div>
            <div v-if="post.state == POST_STATE.SUBMITTED" class="d-flex flex-column ga-2">
              <v-btn
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="approvePost(post, true)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.approve') }}
              </v-btn>
              <v-btn
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="approvePost(post, false)"
              >
                <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                {{ t('admin.reject') }}
              </v-btn>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-3">
              <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                <v-icon icon="mdi-account" color="grey-darken-1" size="18"></v-icon>
              </v-avatar>
              <div>
                <div class="text-body-2 font-weight-medium text-grey-darken-2">
                  {{ t('admin.articleId') }}: {{ post.id }}
                </div>
                <div class="text-caption text-grey-darken-1">{{ post.createdAt }}</div>
              </div>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <div
                v-if="post.type == POST_TYPE.ARTICLE"
                class="tiptap post-content"
                v-html="post.content"
              ></div>
              <div v-if="post.type == POST_TYPE.CONTENTS">
                <div class="text-caption text-grey-darken-1 mb-2">{{ t('admin.directory') }}</div>
                <div class="gap-2">
                  <v-chip
                    v-for="(item, index) in post.content.split(',')"
                    :key="index"
                    variant="flat"
                    color="grey-lighten-4"
                    rounded="lg"
                    class="my-2 py-1 d-block"
                  >
                    {{ item.trim() }}
                  </v-chip>
                </div>
              </div>
            </div>
          </div>
        </div>
      </v-card>
    </div>
  </div>
</template>

<style scoped>
  .tiptap.post-content {
    max-height: 200px;
    overflow-y: auto;
  }

  .action-area {
    min-width: 200px;
  }
</style>
