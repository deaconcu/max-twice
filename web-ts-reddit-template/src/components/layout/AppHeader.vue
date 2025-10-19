<template>
  <v-app-bar :elevation="0" class="app-header" height="56" border>
    <v-container fluid class="px-4">
      <v-row align="center" no-gutters>
        <!-- Logo -->
        <v-col cols="auto">
          <div class="d-flex align-center">
            <div class="logo-icon">
              <v-icon size="32" color="primary">mdi-reddit</v-icon>
            </div>
            <h2 class="text-h5 font-weight-bold ml-2">MaxTwice</h2>
          </div>
        </v-col>

        <v-spacer />

        <!-- Right side buttons -->
        <v-col cols="auto">
          <div class="d-flex align-center ga-2">
            <v-btn
              :variant="isLoginPage ? 'flat' : 'outlined'"
              color="primary"
              class="text-none font-weight-bold"
              size="default"
              @click="$router.push('/login')"
            >
              登录
            </v-btn>
            <v-btn
              :variant="isRegisterPage ? 'flat' : 'outlined'"
              color="primary"
              class="text-none font-weight-bold"
              size="default"
              @click="$router.push('/register')"
            >
              注册
            </v-btn>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </v-app-bar>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const isLoginPage = computed(() => route.path === '/login')
const isRegisterPage = computed(() => route.path === '/register')
</script>

<style scoped>
.app-header {
  background-color: rgb(var(--v-theme-surface)) !important;
  border-bottom: 1px solid rgb(var(--v-theme-border));
}

.logo-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(var(--v-theme-surface-variant));
  border: 2px solid rgb(var(--v-theme-border));
  border-radius: 50%;
}

.v-btn {
  border-radius: 20px;
}
</style>
