<template>
  <div class="intro-content">
    <!-- 主标语 -->
    <h1 class="main-slogan">
      人生没有说明书，<br />
      我们一起写一本。
    </h1>

    <!-- 品牌解释 -->
    <div class="brand-explain">
      <span class="brand-name">TwiceMax</span>
      <span class="brand-meaning">= Twice（两遍）+ Max（最多）</span>
    </div>

    <p class="brand-desc">
      我们的目标：让每个知识点，最多看两遍就能搞懂。
    </p>

    <!-- 特点列表 -->
    <div class="features">
      <div class="feature-item">
        <v-icon size="20" color="primary" class="feature-icon">mdi-account-group</v-icon>
        <span>从角色出发，系统化学习路径</span>
      </div>
      <div class="feature-item">
        <v-icon size="20" color="primary" class="feature-icon">mdi-file-document-outline</v-icon>
        <span>社区精选文章，知识点精炼易懂</span>
      </div>
      <div class="feature-item">
        <v-icon size="20" color="primary" class="feature-icon">mdi-card-multiple-outline</v-icon>
        <span>间隔复习卡片，科学记忆不遗忘</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 简化版，不需要 props 和轮播逻辑
</script>

<style scoped>
.intro-content {
  max-width: 480px;
  margin: 0 auto;
}

/* 主标语 */
.main-slogan {
  font-size: 2rem;
  font-weight: 700;
  line-height: 1.4;
  color: rgb(var(--v-theme-on-surface));
  margin-bottom: 32px;
}

/* 品牌解释 */
.brand-explain {
  margin-bottom: 8px;
}

.brand-name {
  font-size: 1.25rem;
  font-weight: 700;
  color: rgb(var(--v-theme-primary));
}

.brand-meaning {
  font-size: 1rem;
  color: rgb(var(--v-theme-on-surface-variant));
  margin-left: 8px;
}

.brand-desc {
  font-size: 1rem;
  color: rgb(var(--v-theme-on-surface));
  line-height: 1.6;
  margin-bottom: 40px;
}

/* 特点列表 */
.features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 0.95rem;
  color: rgb(var(--v-theme-on-surface-variant));
}

.feature-icon {
  flex-shrink: 0;
}
</style>
