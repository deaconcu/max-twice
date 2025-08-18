<script setup>

import { ref, watch, inject } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { learnService, userService } from '@/services/learnService';

const showSnackbar = inject('showSnackbar');

const props = defineProps(['nodeId']);
const dialog = defineModel();
const emit = defineEmits(['loadData']);

const inputUserName = ref('');
const info = ref('');

const users = ref([])

const searchUser = async () => {
  try {
    const response = await userService.searchUser(inputUserName.value);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      users.value = response.data;
      if (users.value.length == 0) {
        info.value = "没有这个用户";
      }
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

const inviteUser = async (event, user) => {
  try {
    const response = await learnService.inviteUser(user.id, props.nodeId);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      console.log('Form submitted successfully');
      //event.currentTarget.disabled = true
      user.disabled = true;
      showSnackbar('操作成功');
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

const closeDialog = () => {
  dialog.value = false;
}

</script>


<template>
  <v-dialog v-model="dialog" width="800" height="800px" >
    <v-card prepend-icon="mdi-account" title="邀请回答" rounded="lg">
      <template v-slot:append>
        <v-btn icon="mdi-close" variant="text" size=""  :ripple="false" @click="closeDialog"></v-btn>
      </template>
      <v-card-text class="pa-0">
        <v-row class="ma-0 border-t-sm">
          <v-col class="px-7">
            <v-text-field v-model="inputUserName" label="请输入邀请的用户名称" density="compact" variant="outlined" class="pt-5"
              append-inner-icon="mdi-magnify" @click:append-inner="searchUser"
              @keyup.enter="searchUser"></v-text-field>
            <div v-if="users.length > 0" class="py-1">
              <div v-for="(user, index) in users" :key="index">
                <div class="d-flex justify-space-between align-center py-4" style="border-bottom: 1px dashed #ddd;">
                  <a :href="`/user?id=${user.id}`" target="_blank">{{ user.name }}</a>
                  <v-btn density="comfortable" variant="flat" v-ripple="false" color="grey-darken-2" class="text-white" :disabled="user.disabled"
                    @click="inviteUser($event, user)">邀请</v-btn>
                </div>
              </div>
            </div>
            <div v-if="users.length == 0" class="mt-9 d-flex justify-center align-center text-grey">{{ info }}</div>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
  </v-dialog>
</template>
