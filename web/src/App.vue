<script setup>
import { RouterLink, RouterView } from 'vue-router'
import { ref, provide } from 'vue';

const snackbars = ref([]);

const showSnackbar = (message) => {
  const newSnackbar = { text: message, visible: true };
  snackbars.value.push(newSnackbar);

  setTimeout(() => {
    snackbars.value = snackbars.value.filter((snack) => snack !== newSnackbar);
  }, 4000);
};

provide('showSnackbar', showSnackbar);

async function loadPostingList() {
  try {
    if (tab.value == 'option-6') {
      let response = '';
      response = await learnService.getApplyCourseMessage(applyCourseCurrPage.value, 10);

      if (response.code === 401) {
        console.log('not login');
      } else if (response.code === 200) {
        console.log('get data:' + JSON.stringify(response.data));
        messageList.value = response.data.messages;
        applyCourseCurrPage.value = response.data.pagination.currentPage;
        applyCourseTotalPage.value = response.data.pagination.totalPages;
      }
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

async function loadContentsList() {
  try {
    if (tab.value == 'option-6') {
      let response = '';
      response = await learnService.getApplyCourseMessage(applyCourseCurrPage.value, 10);

      if (response.code === 401) {
        console.log('not login');
      } else if (response.code === 200) {
        console.log('get data:' + JSON.stringify(response.data));
        messageList.value = response.data.messages;
        applyCourseCurrPage.value = response.data.pagination.currentPage;
        applyCourseTotalPage.value = response.data.pagination.totalPages;
      }
    }
  } catch (error) {
    console.error('Error get message:', error);
  }
};

</script>

<template>
  <v-app>
    <RouterView />
    <v-container>
      <div>
        <v-snackbar v-for="(item, index) in snackbars" :key="index" v-model="item.visible" :timeout="1000"
          color="green-lighten-5" variant="flat" rounded="lg" location="top center" min-width="200">
          {{ item.text }}
        </v-snackbar>
      </div>
    </v-container>
  </v-app>
</template>


<style>
.v-snackbar__content {
  text-align: center;
}

.flipped {
  transform: rotate(180deg);
}

.slow {
  transition: transform 0.3s ease-in-out;
}
</style>
