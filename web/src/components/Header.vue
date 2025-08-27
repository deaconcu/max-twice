<script setup>
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import LanguageSwitcher from './LanguageSwitcher.vue';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
</script>

<template>
  <v-container max-width="1600">
    <div class="d-flex justify-space-between align-end">
      <div class="d-flex align-end">
        <v-icon class="custom-icon mb-1" color="#333" size="24" >mdi-github</v-icon>
        <span class="text-h6 ml-3 font-weight-black text-align-bottom">{{ t('site.name') }}</span>
        <router-link to="/about" class="text-grey ml-4 font-weight-bold text-body-2">{{ t('site.slogan') }} | {{ t('site.about') }}</router-link>
      </div>
      <div class="d-flex align-center">
        <LanguageSwitcher class="mr-3" />
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