<template>
  <v-app-bar :elevation="0" class="app-header" height="56">
    <v-container fluid class="px-4">
      <v-row align="center" no-gutters>
        <!-- Logo -->
        <v-col cols="auto">
          <div class="d-flex align-center" style="cursor: pointer" @click="$router.push('/home')">
            <div class="logo-icon">
              <v-icon size="28" color="primary">mdi-reddit</v-icon>
            </div>
            <h2 class="logo-text ml-2">MaxTwice</h2>
          </div>
        </v-col>

        <v-spacer />

        <!-- Right side actions -->
        <v-col cols="auto">
          <div class="d-flex align-center ga-2">
            <button class="icon-btn" title="搜索">
              <v-icon size="18">mdi-magnify</v-icon>
            </button>
            <button class="icon-btn" title="通知">
              <v-icon size="18">mdi-bell-outline</v-icon>
            </button>
            <button class="icon-btn" title="个人中心">
              <v-icon size="18">mdi-account-circle-outline</v-icon>
            </button>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </v-app-bar>
</template>

<script setup lang="ts">
</script>

<style scoped>
.app-header {
  background-color: rgb(var(--v-theme-surface)) !important;
}

.logo-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgb(var(--v-theme-surface-variant));
  border: 2px solid rgb(var(--v-theme-border));
  border-radius: 50%;
}

.logo-text {
  font-size: 1.25rem;
  font-weight: 700;
}

.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #656D76;
  cursor: pointer;
  transition: all 0.15s ease;
}

.icon-btn:hover {
  background-color: rgb(var(--v-theme-surface-variant));
  color: rgb(var(--v-theme-on-surface));
}
</style>
