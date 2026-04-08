<template>
  <div class="intro-content">
    <!-- 插画 -->
    <img src="@/assets/images/front.png" alt="TwiceMax" class="hero-image" />

    <!-- 文案区 -->
    <div class="text-content">
      <h1 class="main-slogan" v-html="t('intro.mainSlogan')"></h1>

      <p class="sub-slogan">{{ t('intro.subSlogan') }}</p>

      <!-- 品牌解释 -->
      <div class="brand-line">
        <span class="brand-name">TwiceMax</span>
        <span class="brand-meaning">{{ t('intro.brandMeaning') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from '@/composables/useI18n'

const { t } = useI18n()
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
