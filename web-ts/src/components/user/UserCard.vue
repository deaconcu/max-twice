<script setup lang="ts">
import { ref, computed } from 'vue'
import { followServiceV1, userServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useI18n } from 'vue-i18n'
import { Bool } from '@/types/enums'
import type { User } from '@/types/user'
import { getUserDisplayName } from '@/utils/common'

interface Props {
  userId: number
  userName: string
  showAtSign?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showAtSign: false
})
const { t } = useI18n()

const loadingUserInfo = ref<boolean>(false)
const userInfo = ref<User | null>(null)

const onHover = function (open: boolean): void {
  if (open && (!userInfo.value?.biography || userInfo.value.followed === undefined)) {
    loadingUserInfo.value = true
    getUser(props.userId)
  }
}

const getUser = async (id: number): Promise<void> => {
  try {
    console.log('begin post')

    const response = await userServiceV1.getUser(id)
    console.log(`response: ${JSON.stringify(response)}`)

    if (response.code === 200) {
      userInfo.value = response.data as User
      loadingUserInfo.value = false
    }
  } catch (error) {
    console.error('Error submitting form:', error)
  }
}

const displayName = computed(() => userInfo.value ? getUserDisplayName(userInfo.value) : props.userName)

const follow = async (id: number): Promise<void> => {
  try {
    console.log('begin follow')

    const response = await followServiceV1.follow(id)
    console.log(`response: ${JSON.stringify(response)}`)

    if (response.code === 200) {
      if (userInfo.value) {
        // 关注成功后，增加关注人数
        userInfo.value.followed = (userInfo.value.followed || 0) + 1
      }
    }
  } catch (error) {
    console.error('Error submitting form:', error)
  }
}

const unfollow = async (id: number): Promise<void> => {
  try {
    console.log('begin follow')

    const response = await followServiceV1.unfollow(id)
    console.log(`response: ${JSON.stringify(response)}`)

    if (response.code === 200) {
      if (userInfo.value) {
        userInfo.value.followed = 0
      }
    }
  } catch (error) {
    console.error('Error submitting form:', error)
  }
}
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
        :href="'/user?id=' + props.userId"
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
          <a :href="'/user?id=' + props.userId" target="_blank">{{ displayName }}</a>
        </v-card-title>
        <v-card-subtitle>
          {{ t('userCard.stats', { articles: 5, likes: 202 }) }}
        </v-card-subtitle>

        <v-card-actions class="justify-left">
          <v-btn
            v-if="userInfo?.followed == Bool.FALSE"
            variant="text"
            @click="follow(userInfo.id)"
            >{{ t('userCard.follow') }}</v-btn
          >
          <v-btn
            v-if="userInfo?.followed == Bool.TRUE"
            variant="text"
            @click="unfollow(userInfo.id)"
            >{{ t('userCard.unfollow') }}</v-btn
          >
        </v-card-actions>
      </div>
    </v-card>
  </v-menu>
</template>