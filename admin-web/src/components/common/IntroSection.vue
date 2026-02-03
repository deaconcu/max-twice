<template>
  <div class="intro-content">
    <h2 class="section-title">{{ title }}</h2>

    <div class="rights-carousel">
      <transition name="slide-fade" mode="out-in">
        <div :key="currentIndex" class="right-item">
          <span class="right-number">{{ currentIndex + 1 }}.</span>
          <span class="right-text">{{ items[currentIndex] }}</span>
        </div>
      </transition>
      <div class="carousel-dots">
        <span
          v-for="(_, index) in items"
          :key="index"
          class="dot"
          :class="{ active: index === currentIndex }"
          @click="currentIndex = index"
        ></span>
      </div>
    </div>

    <h2 class="section-title">{{ missionTitle }}</h2>

    <p class="mission-text">
      {{ missionIntro }}<span class="mission-quote">{{ missionQuote }}</span>
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

interface Props {
  title?: string
  items: readonly string[]
  missionTitle?: string
  missionIntro?: string
  missionQuote?: string
  autoPlay?: boolean
  interval?: number
}

const props = withDefaults(defineProps<Props>(), {
  title: '我们想和您一起，争取如下权利',
  missionTitle: 'MaxTwice 的含义',
  missionIntro: '我们想和您一起，努力做到：',
  missionQuote: '"这里的任何一个知识节点，都能只看两遍就懂"',
  autoPlay: true,
  interval: 3000,
})

const currentIndex = ref(0)
let intervalId: number | null = null

onMounted(() => {
  if (props.autoPlay && props.items.length > 1) {
    intervalId = window.setInterval(() => {
      currentIndex.value = (currentIndex.value + 1) % props.items.length
    }, props.interval)
  }
})

onUnmounted(() => {
  if (intervalId) {
    clearInterval(intervalId)
  }
})
</script>

<style scoped>
.intro-content {
  max-width: 560px;
  margin: 0 auto;
}

/* Section Title */
.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: rgb(var(--v-theme-on-surface));
  margin-bottom: 24px;
}

/* Rights Carousel */
.rights-carousel {
  margin-bottom: 56px;
  min-height: 120px;
}

.right-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  line-height: 1.7;
  padding: 20px 0;
}

.right-number {
  color: rgb(var(--v-theme-primary));
  font-weight: 600;
  flex-shrink: 0;
  min-width: 28px;
  font-size: 1.1rem;
}

.right-text {
  color: rgb(var(--v-theme-on-surface));
  font-size: 1.05rem;
}

/* Carousel Dots */
.carousel-dots {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-top: 24px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: rgba(var(--v-theme-on-surface), 0.2);
  cursor: pointer;
  transition: all 0.3s ease;
}

.dot:hover {
  background-color: rgba(var(--v-theme-on-surface), 0.4);
}

.dot.active {
  width: 24px;
  border-radius: 4px;
  background-color: rgb(var(--v-theme-primary));
}

/* Slide Fade Transition */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.5s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* Mission Text */
.mission-text {
  margin-top: 0;
  color: rgb(var(--v-theme-on-surface));
  font-size: 1rem;
  line-height: 1.8;
  font-weight: 400;
}

/* Mission Quote */
.mission-quote {
  color: rgb(var(--v-theme-primary));
  font-size: 1.5rem;
  font-weight: 600;
  font-family: Georgia, serif;
}
</style>
