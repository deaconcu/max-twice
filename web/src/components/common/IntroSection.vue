<template>
  <div class="intro-content">
    <!-- 插画 -->
    <img src="@/assets/images/front.png" alt="TwiceMax" class="hero-image" />

    <!-- 文案区 -->
    <div class="text-content">
      <h1 class="main-slogan">
        人生没有说明书，<br />
        我们一起写一本。
      </h1>

      <p class="sub-slogan">想成为谁，就学什么。</p>

      <!-- 品牌解释 -->
      <div class="brand-line">
        <span class="brand-name">TwiceMax</span>
        <span class="brand-meaning">— 每个知识点，最多看两遍就懂</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 简化版，不需要 props 和轮播逻辑
</script>

<style scoped>
.intro-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
}

/* 插画 */
.hero-image {
  width: 100%;
  max-width: 1200px;
  height: auto;
  margin-bottom: 32px;
}

/* 文案区 */
.text-content {
  text-align: center;
}

/* 主标语 */
.main-slogan {
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1.5;
  color: rgb(var(--v-theme-on-surface));
  margin-bottom: 12px;
}

/* 副标语 */
.sub-slogan {
  font-size: 1.1rem;
  font-weight: 500;
  color: rgb(var(--v-theme-primary));
  margin-bottom: 24px;
}

/* 品牌解释 */
.brand-line {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.brand-name {
  font-size: 1rem;
  font-weight: 700;
  color: rgb(var(--v-theme-on-surface));
}

.brand-meaning {
  font-size: 0.95rem;
  color: rgb(var(--v-theme-on-surface-variant));
}
</style>
