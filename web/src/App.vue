<script setup>
  import { RouterView } from 'vue-router'
  import { provide, ref } from 'vue'
  import { useToastMessage } from '@/composables/useToastMessage'

  const snackbars = ref([])
  const { getMessage } = useToastMessage()

  // 增强的showSnackbar函数，支持国际化
  const showSnackbar = (message, type = 'info', params = []) => {
    let translatedMessage

    if (typeof message === 'string') {
      translatedMessage = getMessage(message, type, params)
    } else {
      // 处理错误对象
      translatedMessage = getMessage(message.message || 'message.systemError', 'error')
    }

    const newSnackbar = {
      text: translatedMessage,
      visible: true,
      type,
    }
    snackbars.value.push(newSnackbar)

    setTimeout(() => {
      snackbars.value = snackbars.value.filter((snack) => snack !== newSnackbar)
    }, 4000)
  }

  provide('showSnackbar', showSnackbar)
</script>

<template>
  <v-app>
    <RouterView />
    <v-container>
      <div>
        <v-snackbar
          v-for="(item, index) in snackbars"
          :key="index"
          v-model="item.visible"
          :timeout="1000"
          color="green-lighten-5"
          variant="flat"
          rounded="lg"
          location="top center"
          min-width="200"
        >
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
