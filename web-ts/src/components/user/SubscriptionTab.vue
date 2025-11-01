<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import type { Ref } from 'vue'
import { subscriptionServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useUserStore } from '@/stores/user'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'
import type { UserCourse } from '@/types/userCourse'
import draggable from 'vuedraggable'

// Props
const props = defineProps<{
  userId?: number
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

const subscriptions: Ref<UserCourse[]> = ref([])
const subscriptionsCopy: Ref<UserCourse[]> = ref([])
const courseDescription: Ref<string> = ref('')
const courseHoveringIndex: Ref<number> = ref(-1)

// 使用 useFetch 加载订阅列表
const { execute: loadSubscription } = useFetch<UserCourse[]>({
  fetchFn: () => subscriptionServiceV1.getUserSubscriptions(targetUserId.value),
  immediate: true,
  onSuccess: (data) => {
    console.log('load subscription success')
    console.log(`get data:${JSON.stringify(data)}`)
    subscriptions.value = data
    subscriptionsCopy.value = JSON.parse(JSON.stringify(data))
  },
  onError: (error) => {
    console.error('Error get subscription:', error)
  }
})

// 使用 useMutation 保存订阅
const { execute: saveSubscription, loading: saving } = useMutation(
  (ids: string) => subscriptionServiceV1.updateSubscriptions(ids),
  {
    successMessage: '修改成功！',
    onSuccess: (result) => {
      // 更新 currentUser 中的 subscriptions
      if (userStore.currentUser) {
        userStore.setUser({
          ...userStore.currentUser,
          subscriptions: result
        })
      }
      loadSubscription()
    },
    onError: (error) => {
      console.error('Error saving subscription:', error)
    }
  }
)

// 保存订阅修改（仅在可编辑时可用）
const handleSaveSubscription = async (): Promise<void> => {
  if (!canEdit.value) return
  console.log('save subscription')
  const ids = subscriptions.value.map((item) => item.id).join(',')
  await saveSubscription(ids)
}

// 恢复订阅修改（仅在可编辑时可用）
const recoverSubscription = (): void => {
  if (!canEdit.value) return
  subscriptions.value = JSON.parse(JSON.stringify(subscriptionsCopy.value))
}

// 处理鼠标悬停
const handleCourseHover = (element: UserCourse, index: number): void => {
  courseDescription.value = element.course?.description || ''
  courseHoveringIndex.value = index
}

// 暴露函数给父组件调用
defineExpose({
  loadSubscription,
})
</script>

<template>
  <div>
    <div class="mb-5 py-3 rounded text-grey d-flex align-center">
      <v-icon icon="mdi-information-outline" start size="16" class="mr-2"></v-icon>
      <span class="text-body-2">{{
        canEdit
          ? '将鼠标放置在课程上可以查看简介，操作删除，拖动图标可以切换顺序'
          : '查看用户关注的课程'
      }}</span>
    </div>

    <v-item-group class="mt-3 mb-5" column>
      <v-item>
        <!-- 可编辑模式：使用draggable -->
        <draggable v-if="canEdit" v-model="subscriptions" item-key="id" class="pt-5">
          <template #item="{ element, index }">
            <span>
              <v-hover>
                <template #default="{ isHovering, props: hoverProps }">
                  <v-chip
                    variant="flat"
                    v-bind="hoverProps"
                    class="mr-4 mb-4 px-4 py-4 text-body-1"
                    :class="courseHoveringIndex == index ? 'bg-red-lighten-1' : 'bg-grey-lighten-4'"
                    @mouseenter="handleCourseHover(element, index)"
                  >
                    <span class="font-weight-medium">{{ element.course?.name || '未知课程' }}</span>
                    <v-slide-x-transition hide-on-leave>
                      <v-icon
                        v-if="isHovering"
                        icon="mdi-close-circle-outline"
                        class="ms-2"
                        @click="subscriptions.splice(index, 1)"
                      ></v-icon>
                    </v-slide-x-transition>
                  </v-chip>
                </template>
              </v-hover>
            </span>
          </template>
        </draggable>

        <!-- 只读模式：普通显示 -->
        <div v-else class="pt-5">
          <v-chip
            v-for="(element, index) in subscriptions"
            :key="element.id"
            variant="flat"
            class="mr-4 mb-4 px-4 py-4 text-body-1 bg-grey-lighten-4"
            @mouseenter="handleCourseHover(element, index)"
          >
            <span class="font-weight-medium">{{ element.course?.name || '未知课程' }}</span>
          </v-chip>
        </div>

        <!-- 调试信息 -->
        <div v-if="subscriptions.length === 0" class="text-center py-4">
          <p class="text-grey">{{ canEdit ? '暂无关注的课程' : '该用户暂未关注任何课程' }}</p>
          <p class="text-caption text-grey">
            调试：canEdit={{ canEdit }}, 数据长度={{ subscriptions.length }}
          </p>
        </div>
      </v-item>
    </v-item-group>

    <div v-if="canEdit && JSON.stringify(subscriptions) != JSON.stringify(subscriptionsCopy)">
      <v-btn
        variant="flat"
        color="teal"
        density="comfortable"
        class="mr-4 mt-2"
        :loading="saving"
        @click="handleSaveSubscription"
      >
        保存
      </v-btn>
      <v-btn
        variant="text"
        color=""
        density="comfortable"
        class="mr-4 mt-2"
        @click="recoverSubscription"
      >
        恢复
      </v-btn>
    </div>

    <v-divider class="mb-9 mt-9"></v-divider>

    <div class="">
      {{ courseDescription }}
    </div>
  </div>
</template>

<style scoped>
/* 继承原有样式 */
</style>