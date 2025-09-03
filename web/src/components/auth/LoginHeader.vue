<script setup>
  // 无需props和emits，这是一个静态展示组件
</script>

<template>
  <v-app-bar app flat>
    <v-container class="ma-0 pa-0" fluid>
      <div class="d-flex align-center">
        <v-avatar color="primary" size="40" class="mr-3">
          <v-icon size="24" color="white">mdi-alpha-m-box</v-icon>
        </v-avatar>
        <span class="text-h6 font-weight-bold text-grey-darken-4">最多两次，最好一次</span>
        <v-spacer></v-spacer>
        <router-link to="/about" class="text-primary font-weight-medium text-decoration-none">
          <v-btn variant="text" color="primary" prepend-icon="mdi-help-circle">
            为什么有这个网站？
          </v-btn>
        </router-link>
      </div>
    </v-container>
  </v-app-bar>
</template>
