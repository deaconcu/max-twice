<script setup>
import { ref, onMounted, nextTick, watch, toRef, inject } from 'vue';
import { learnService, userService } from '@/services/learnService';
import { useI18n } from 'vue-i18n'

const ps = defineProps(['id', 'name']);
const { t } = useI18n()

const loadingUserInfo = ref(false)
const userInfo = ref(null);

function onHover(open, id) {
  if (open && !userInfo.value) {
    loadingUserInfo.value = true
    getUser(id);
  }
}

const getUser = async (id) => {
  try {
    console.log("begin post");

    const response = await userService.getUser(id);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      userInfo.value = response.data;
      loadingUserInfo.value = false
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

const follow = async (id) => {
  try {
    console.log("begin follow");

    const response = await userService.follow(id);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      userInfo.value.followed = 1;
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}

const unfollow = async (id) => {
  try {
    console.log("begin follow");

    const response = await userService.unfollow(id);
    console.log('response: ' + JSON.stringify(response));

    if (response.code === 200) {
      userInfo.value.followed = 0;
    }
  } catch (error) {
    // todo
    console.error('Error submitting form:', error);
  }
}
</script>

<template>
  <v-menu open-on-hover :close-on-content-click="false" @update:modelValue="val => onHover(val, ps.id)"
    origin="top center">
    <template #activator="{ props }">
      <span v-bind="props" class="text-body-2 text-grey-darken-4 font-weight-bold">{{ ps.name }}</span>
    </template>
    <v-card width="300" elevation="1" class="mt-3 ms-9 mx-auto">
      <div v-if="loadingUserInfo">{{ t('userCard.loading') }}</div>
      <div v-else>

        <v-card-title class="d-flex align-center text-body-1">
          <a :href="'/user?id=' + ps.id" target="_blank">{{ ps.name }}</a>
        </v-card-title>
        <v-card-subtitle>
          {{ t('userCard.stats', { articles: 5, likes: 202 }) }}
        </v-card-subtitle>

        <v-card-actions class="justify-left">
          <v-btn v-if="userInfo.followed == 0" variant="text" @click="follow(userInfo.id)">{{ t('userCard.follow') }}</v-btn>
          <v-btn v-if="userInfo.followed == 1" variant="text" @click="unfollow(userInfo.id)">{{ t('userCard.unfollow') }}</v-btn>
        </v-card-actions>
        
      </div>
    </v-card>
  </v-menu>
</template>