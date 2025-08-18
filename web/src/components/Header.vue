<script setup>
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
</script>

<template>
  <v-container max-width="1600">
    <div class="d-flex justify-space-between align-end">
      <div class="d-flex align-end">
        <v-icon class="custom-icon mb-1" color="#333" size="24" >mdi-github</v-icon>
        <span class="text-h6 ml-3 font-weight-black text-align-bottom">最多看两遍！</span>
        <router-link to="/about" class="text-grey ml-4 font-weight-bold text-body-2">争取本站大部分内容最多看两次就会，最好看一次就会 | 为什么有这个网站？</router-link>
      </div>
      <div class="d-flex align-center">
        <v-icon size="24" color="#666" class="mr-3">mdi-github</v-icon>
        <v-icon size="24" color="#666" class="mr-3">mdi-twitter</v-icon>
        <v-icon size="24" color="#666" class="mr-3">mdi-slack</v-icon>
        <v-icon size="24" color="#666" class="mr-3">mdi-rhombus-split</v-icon>
        <v-icon size="24" color="#666" class="mr-3">mdi-message</v-icon>
        <v-icon @click="router.push('/user')" size="32" color="#333" class="ml-3">mdi-account</v-icon>
      </div>
    </div>
  </v-container>
</template>


<style scoped>
.text-align-bottom {
  vertical-align: bottom;
}
</style>