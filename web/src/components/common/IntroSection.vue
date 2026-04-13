<template>
  <div class="intro-content">
    <!-- 插画 -->
    <img src="@/assets/images/front.png" alt="TwiceMax" class="hero-image" />

    <!-- 文案区 -->
    <div class="text-content">
      <div class="slogan-card">
        <!-- 手绘风格 SVG 边框（含尖角） -->
        <svg class="hand-drawn-border" viewBox="0 0 200 120" preserveAspectRatio="none">
          <!-- 框体 + 尖角一体，用多段不同粗细模拟手绘 -->
          <path
            d="M10,25 Q5,23 5,33 L3,105 Q2,115 15,117"
            fill="none"
            stroke="rgba(0,0,0,0.5)"
            stroke-width="2"
            stroke-linecap="round"
          />
          <path
            d="M15,117 L185,115 Q198,114 197,102"
            fill="none"
            stroke="rgba(0,0,0,0.5)"
            stroke-width="3.2"
            stroke-linecap="round"
          />
          <path
            d="M197,102 L199,36 Q200,25 188,26 L105,24"
            fill="none"
            stroke="rgba(0,0,0,0.5)"
            stroke-width="2.4"
            stroke-linecap="round"
          />
          <path
            d="M105,24 L100,12 L95,24"
            fill="none"
            stroke="rgba(0,0,0,0.5)"
            stroke-width="2.7"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <path
            d="M95,24 L15,25 Q8,24 10,25"
            fill="none"
            stroke="rgba(0,0,0,0.5)"
            stroke-width="2.2"
            stroke-linecap="round"
          />
          <!-- 白色填充 -->
          <path
            d="M10,25 Q5,23 5,33 L3,105 Q2,115 15,117 L185,115 Q198,114 197,102 L199,36 Q200,25 188,26 L105,24 L100,12 L95,24 L15,25 Q8,24 10,25"
            fill="#fff"
            stroke="none"
          />
        </svg>
        <div class="slogan-content">
          <p class="slogan-line">{{ t('intro.greeting') }}</p>
          <p class="slogan-line">{{ t('intro.line1') }}</p>
          <p class="slogan-line">{{ t('intro.line2') }}</p>
        </div>
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
  margin-bottom: 40px;
}

/* 文案区 */
.text-content {
  width: 100%;
  display: flex;
  justify-content: center;
}

/* 文案卡片 - 漫画对话框 */
.slogan-card {
  position: relative;
  padding: 8px 14px;
  transform: rotate(-1deg);
}

/* 手绘 SVG 边框 */
.hand-drawn-border {
  position: absolute;
  top: -18px;
  left: 0;
  width: 100%;
  height: calc(100% + 18px);
  pointer-events: none;
}

/* 文案内容 */
.slogan-content {
  position: relative;
  padding: 22px 16px 8px 16px;
}

/* 对话框尾巴边框 */
.slogan-card::before {
  display: none;
}

/* 对话框尾巴填充 */
.slogan-card::after {
  display: none;
}

/* 文案行 */
.slogan-line {
  font-size: 0.9rem;
  font-weight: 500;
  line-height: 1.8;
  color: rgb(var(--v-theme-on-surface));
  margin: 0;
  padding: 6px 0;
  text-align: left;
  letter-spacing: 0.03em;
}

.slogan-line:not(:last-child) {
  border-bottom: 1px solid rgba(var(--v-theme-on-surface), 0.06);
}

/* 暗色模式适配 */
:root.dark .slogan-card {
  background: linear-gradient(135deg, rgba(30,30,30,0.9) 0%, rgba(30,30,30,0.7) 100%);
}
</style>
