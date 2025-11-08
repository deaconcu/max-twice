<template>
  <v-container>
    <v-row justify="center">
      <v-col cols="12" md="8">
        <v-card>
          <v-card-title class="text-h4"> Welcome to Max Twice </v-card-title>

          <v-card-text>
            <p class="text-h6 mb-4">项目初始化成功！</p>
            <v-chip color="success" class="mr-2"> Vue {{ vueVersion }} </v-chip>
            <v-chip color="primary" class="mr-2"> Vuetify 3 </v-chip>
            <v-chip color="secondary" class="mr-2"> TypeScript </v-chip>
          </v-card-text>

          <v-card-actions>
            <v-btn color="primary" @click="toggleTheme"> 切换主题 </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import { version as vueVersion } from 'vue'
import { useTheme } from 'vuetify'

const theme = useTheme()

const toggleTheme = () => {
  theme.global.name.value = theme.global.current.value.dark ? 'light' : 'dark'
}
</script>
