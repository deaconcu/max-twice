<script setup>
// 左侧欢迎区域 - 静态展示组件，无需props和emits
</script>

<template>
  <v-col cols="6" class="text-center pa-8">
    <div class="mb-6">
      <h1 class="text-h3 font-weight-bold text-grey-darken-4 mb-4">欢迎来到学习平台</h1>
      <p class="text-h6 text-grey-darken-2 mb-8">高效学习，一次掌握 • 智能互动，共同进步</p>
    </div>
    
    <v-card rounded="xl" elevation="0" color="grey-lighten-5" class="pa-6 mx-auto" style="max-width: 600px; border: 1px solid rgba(0, 0, 0, 0.06);">
      <v-img 
        src="/images/big2.png" 
        width="0" 
        class="mx-auto rounded-lg mb-4">
      </v-img>
      
      <!-- 特色展示 -->
      <div class="d-flex justify-space-around mt-6">
        <div class="text-center">
          <v-avatar color="primary" size="48" class="mb-2">
            <v-icon icon="mdi-lightning-bolt" color="white" size="24"></v-icon>
          </v-avatar>
          <h4 class="text-body-1 font-weight-bold text-grey-darken-4">高效学习</h4>
          <p class="text-body-2 text-grey-darken-2 mb-0">最多两次掌握</p>
        </div>
        
        <div class="text-center">
          <v-avatar color="success" size="48" class="mb-2">
            <v-icon icon="mdi-account-group" color="white" size="24"></v-icon>
          </v-avatar>
          <h4 class="text-body-1 font-weight-bold text-grey-darken-4">互动社区</h4>
          <p class="text-body-2 text-grey-darken-2 mb-0">共同学习进步</p>
        </div>
        
        <div class="text-center">
          <v-avatar color="warning" size="48" class="mb-2">
            <v-icon icon="mdi-chart-line" color="white" size="24"></v-icon>
          </v-avatar>
          <h4 class="text-body-1 font-weight-bold text-grey-darken-4">进度跟踪</h4>
          <p class="text-body-2 text-grey-darken-2 mb-0">实时掌握成果</p>
        </div>
      </div>
      
      <!-- 网站简介内容 -->
      <div class="mt-6 pa-4 bg-blue-lighten-5 rounded-lg">
        <h3 class="text-h6 font-weight-bold text-primary mb-2">为什么选择我们？</h3>
        <p class="text-body-2 text-grey-darken-2 mb-2">• 个性化学习路径，根据您的进度调整</p>
        <p class="text-body-2 text-grey-darken-2 mb-2">• 专业导师在线指导，随时答疑解惑</p>
        <p class="text-body-2 text-grey-darken-2 mb-2">• 学习社区互动，与同学共同进步</p>
        <p class="text-body-2 text-grey-darken-2 mb-0">• 多媒体教学资源，让学习更生动</p>
      </div>
    </v-card>
  </v-col>
</template>