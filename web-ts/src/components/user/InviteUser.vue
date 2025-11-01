<script setup lang="ts">
import { inject, ref } from 'vue'
import { messageServiceV1, userServiceV1 } from '@/services/api/v1/apiServiceV1'
import { useI18n } from 'vue-i18n'
import type { User } from '@/types/user'
import UserCard from '@/components/user/UserCard.vue'
import { useFetch } from '@/composables/useFetch'
import { useMutation } from '@/composables/useMutation'

// 扩展 User 类型以支持 UI 状态
interface SearchUser extends User {
  disabled?: boolean
}

interface Props {
  nodeId: number
}

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')
const { t } = useI18n()

const props = defineProps<Props>()
const dialog = defineModel<boolean>({ type: Boolean })

const inputUserName = ref<string>('')
const info = ref<string>('')

// 使用 useFetch 搜索用户
const {
  data: users,
  execute: searchUser,
} = useFetch<SearchUser[]>({
  fetchFn: () => userServiceV1.searchUser(inputUserName.value),
  immediate: false,
  defaultValue: [],
  onSuccess: (data) => {
    if (data.length === 0) {
      info.value = t('invite.noUser')
    } else {
      info.value = ''
    }
  },
})

// 使用 useMutation 邀请用户
const { execute: sendInvite } = useMutation(
  (userId: number) => messageServiceV1.inviteUser(userId, props.nodeId),
  {
    successMessage: t('invite.operationSuccess'),
  },
)

const inviteUser = async (event: Event, user: SearchUser): Promise<void> => {
  await sendInvite(user.id)
  user.disabled = true
}

const closeDialog = (): void => {
  dialog.value = false
}
</script>

<template>
  <v-dialog v-model="dialog" width="800" height="800px">
    <v-card prepend-icon="mdi-account" :title="t('invite.inviteToAnswer')" rounded="lg">
      <template #append>
        <v-btn icon="mdi-close" variant="text" size="" :ripple="false" @click="closeDialog"></v-btn>
      </template>
      <v-card-text class="pa-0">
        <v-row class="ma-0 border-t-sm">
          <v-col class="px-7">
            <v-text-field
              v-model="inputUserName"
              :label="t('invite.inputUsername')"
              density="compact"
              variant="outlined"
              class="pt-5"
              append-inner-icon="mdi-magnify"
              @click:append-inner="searchUser"
              @keyup.enter="searchUser"
            ></v-text-field>
            <div v-if="users.length > 0" class="py-1">
              <div v-for="(user, index) in users" :key="index">
                <div
                  class="d-flex justify-space-between align-center py-4 border-b-sm border-opacity-25"
                >
                  <UserCard :user-id="user.id" :user-name="user.name" />
                  <v-btn
                    v-ripple="false"
                    density="comfortable"
                    variant="flat"
                    color="grey-darken-2"
                    class="text-white"
                    :disabled="user.disabled"
                    @click="inviteUser($event, user)"
                    >{{ t('invite.invite') }}</v-btn
                  >
                </div>
              </div>
            </div>
            <div v-if="users.length == 0" class="mt-9 d-flex justify-center align-center text-grey">
              {{ info }}
            </div>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>