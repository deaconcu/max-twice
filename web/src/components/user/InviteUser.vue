<script setup>
  import { inject, ref } from 'vue'
  import { messageServiceV1, userServiceV1 } from '@/services/api/v1/apiServiceV1'
  import { useI18n } from 'vue-i18n'

  const showSnackbar = inject('showSnackbar')
  const { t } = useI18n()

  const props = defineProps({
    nodeId: {
      type: Number,
      required: true,
    },
  })
  const dialog = defineModel({ type: Boolean })

  const inputUserName = ref('')
  const info = ref('')

  const users = ref([])

  const searchUser = async () => {
    try {
      const response = await userServiceV1.searchUser(inputUserName.value)
      console.log(`response: ${JSON.stringify(response)}`)

      if (response.code === 200) {
        console.log('Form submitted successfully')
        users.value = response.data
        if (users.value.length === 0) {
          info.value = t('invite.noUser')
        }
      }
    } catch (error) {
      // todo
      console.error('Error submitting form:', error)
    }
  }

  const inviteUser = async (event, user) => {
    try {
      const response = await messageServiceV1.inviteUser(user.id, props.nodeId)
      console.log(`response: ${JSON.stringify(response)}`)

      if (response.code === 200) {
        console.log('Form submitted successfully')
        //event.currentTarget.disabled = true
        user.disabled = true
        showSnackbar(t('invite.operationSuccess'))
      }
    } catch (error) {
      // todo
      console.error('Error submitting form:', error)
    }
  }

  const closeDialog = () => {
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
                  <a :href="`/user?id=${user.id}`" target="_blank">{{ user.name }}</a>
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
