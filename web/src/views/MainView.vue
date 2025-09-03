<script setup>
  import { onMounted, onUnmounted, ref } from 'vue'
  import PageHeader from '../components/common/PageHeader.vue'
  import PageFooter from '../components/common/PageFooter.vue'

  const scrollY = ref(0)

  const updateScroll = () => {
    scrollY.value = window.scrollY
  }

  onMounted(() => {
    window.addEventListener('scroll', updateScroll)
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', updateScroll)
  })
</script>

<template>
  <div class="page-container">
    <!-- 顶部标题栏 -->
    <div class="header-container" :class="{ compact: scrollY > 20 }">
      <PageHeader />
    </div>

    <div class="content main-content-size">
      <RouterView />
    </div>

    <PageFooter />
  </div>
</template>

<style scoped>
  .page-container {
    display: flex;
    flex-direction: column;
    min-height: 97vh;
  }

  .header-container {
    position: sticky;
    top: 0;
    z-index: 1000;
    background-color: white;
    padding: 16px 0;
    transition: all 0.3s ease;
  }

  .header-container::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 100vw;
    height: 1px;
    background-color: #eef;
  }

  .header-container.compact {
    padding: 8px 0;
  }

  .content {
    flex: 1;
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  :deep(.v-main__wrap) {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .main-content-size {
    width: 1600px;
    height: 1800px;
  }
</style>
