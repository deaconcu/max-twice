<script setup lang="ts">
import { computed, inject } from 'vue'
import { followServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useUserStore } from '@/stores/user'
import type { User } from '@/types/user'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

// Props
const props = defineProps<{
  userId?: number | null
  editable?: boolean
}>()

const userStore = useUserStore()
const showSnackbar = inject('showSnackbar') as (message: string, type?: string) => void

// 当前操作的用户ID
const targetUserId = computed(() => props.userId || userStore.currentUser?.id)

// 是否为当前用户查看自己的信息
const isSelf = computed(() => !props.userId || props.userId === userStore.currentUser?.id)

// 实际的可编辑状态：必须是自己的信息且明确允许编辑
const canEdit = computed(() => props.editable && isSelf.value)

// 使用 useInfiniteScroll 加载关注列表
const {
  items: followeeList,
  loading,
  loadMore: loadFollowee
} = useInfiniteScroll<User>({
  fetchFn: (params) =>
    followServiceV1.getFollowees(targetUserId.value, params.lastCreatedAt),
  getNextParams: (lastItem) => ({
    lastCreatedAt: lastItem.createdAt
  }),
  initialParams: {
    lastCreatedAt: '2100-01-01 00:00:01'
  }
})

// 使用 useMutation 处理取消关注
const { execute: executeUnfollow } = useMutation(
  (userId: string | number) => followServiceV1.unfollow(userId),
  {
    successMessage: '已取消关注',
    onSuccess: (_, userId) => {
      const index = followeeList.value.findIndex(item => item.id === userId)
      if (index !== -1) {
        followeeList.value.splice(index, 1)
      }
    }
  }
)

const handleUnfollow = async (userId: string | number): Promise<void> => {
  if (!canEdit.value) return
  await executeUnfollow(userId)
}

</script>

<template>
  <div>
    <div class="mb-5 py-3 rounded text-grey d-flex align-center">
      <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
      <span class="text-body-2">{{ canEdit ? '查看和管理您关注的用户' : '查看关注的用户' }}</span>
    </div>

    <v-infinite-scroll
      :items="followeeList"
      @load="loadFollowee"
      no-more-text="已经到底了"
      class="infinite-scroll-offset"
    >
      <v-list>
        <v-list-item v-for="(item, i) in followeeList" :key="i" :value="item" class="mb-5 py-2">
          <template #prepend>
            <v-avatar icon="mdi-account" size="34" color="red">
              <span class="text-body-1">{{
                item.name ? item.name.charAt(0).toUpperCase() : 'U'
              }}</span>
            </v-avatar>
          </template>

          <v-list-item-title>{{ item.name }}</v-list-item-title>
          <v-list-item-subtitle>{{ item.biography }}</v-list-item-subtitle>

          <template #append>
            <v-btn v-if="canEdit" variant="text" @click="handleUnfollow(item.id)"> 取消关注 </v-btn>
          </template>
        </v-list-item>
      </v-list>

      <template #empty>
        <div class="text-body-2 text-grey py-5">已经到底了</div>
      </template>
    </v-infinite-scroll>
  </div>
</template>

<style scoped>
.v-infinite-scroll__side {
  display: none !important;
}

.infinite-scroll-offset {
  position: relative;
  top: -12px;
}
</style>