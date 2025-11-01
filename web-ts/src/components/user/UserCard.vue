<script setup lang="ts">
import { ref, computed } from 'vue'
import { followServiceV1, userServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useI18n } from 'vue-i18n'
import { Bool } from '@/types/enums'
import type { User } from '@/types/user'
import { getUserDisplayName } from '@/utils/common'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'

interface Props {
  userId: number
  userName: string
  showAtSign?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showAtSign: false
})
const { t } = useI18n()

// 使用 useFetch 加载用户信息
const {
  data: userInfo,
  loading: loadingUserInfo,
  execute: loadUser,
} = useFetch<User>({
  fetchFn: () => userServiceV1.getUser(props.userName),
  immediate: false,
})

const onHover = function (open: boolean): void {
  if (open && (!userInfo.value?.biography || userInfo.value.followed === undefined)) {
    loadUser()
  }
}

const displayName = computed(() => userInfo.value ? getUserDisplayName(userInfo.value) : props.userName)

// 使用 useMutation 处理关注
const { execute: followUser } = useMutation(
  (id: number) => followServiceV1.follow(id),
  {
    onSuccess: () => {
      if (userInfo.value) {
        userInfo.value.followed = (userInfo.value.followed || 0) + 1
      }
    },
  },
)

// 使用 useMutation 处理取消关注
const { execute: unfollowUser } = useMutation(
  (id: number) => followServiceV1.unfollow(id),
  {
    onSuccess: () => {
      if (userInfo.value) {
        userInfo.value.followed = 0
      }
    },
  },
)

</script>

<template>
  <v-menu
    open-on-hover
    :close-on-content-click="false"
    origin="top center"
    @update:model-value="(val) => onHover(val)"
  >
    <template #activator="{ props: menuProps }">
      <a
        v-bind="menuProps"
        :href="'/user/' + props.userName"
        target="_blank"
        class="text-decoration-none"
        :class="showAtSign ? 'text-primary' : 'text-grey-darken-4'"
      >
        <span class="text-body-2 font-weight-bold">
          <span v-if="showAtSign">@</span>{{ displayName }}
        </span>
      </a>
    </template>
    <v-card width="300" elevation="1" class="mt-3 ms-9 mx-auto">
      <div v-if="loadingUserInfo">{{ t('userCard.loading') }}</div>
      <div v-else>
        <v-card-title class="d-flex align-center text-body-1">
          <a :href="'/user/' + props.userName" target="_blank">{{ displayName }}</a>
        </v-card-title>
        <v-card-subtitle>
          {{ t('userCard.stats', { articles: 5, likes: 202 }) }}
        </v-card-subtitle>

        <v-card-actions class="justify-left">
          <v-btn
            v-if="userInfo?.followed == Bool.FALSE"
            variant="text"
            @click="followUser(userInfo.id)"
            >{{ t('userCard.follow') }}</v-btn
          >
          <v-btn
            v-if="userInfo?.followed == Bool.TRUE"
            variant="text"
            @click="unfollowUser(userInfo.id)"
            >{{ t('userCard.unfollow') }}</v-btn
          >
        </v-card-actions>
      </div>
    </v-card>
  </v-menu>
</template>