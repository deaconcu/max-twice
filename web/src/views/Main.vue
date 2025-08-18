<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import Header from '../components/Header.vue';
import Footer from '../components/Footer.vue';
import router from '@/router';

const scrollY = ref(0);

const updateScroll = () => {
  scrollY.value = window.scrollY;
};

onMounted(() => {
  window.addEventListener("scroll", updateScroll);
});

onUnmounted(() => {
  window.removeEventListener("scroll", updateScroll);
});
</script>

<template>
  <div class="page-container">
    <!-- 顶部标题栏 -->
    <div app flat :density="scrollY > 20 ? 'compact' : 'comfortable'" style="border-bottom: 1px solid #eef;">
      <Header />
    </div>

    <div class="content" style="width: 1600px;height:1800px">
      <RouterView />
    </div>

    <Footer />
  </div>
</template>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  min-height: 97vh;
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
</style>