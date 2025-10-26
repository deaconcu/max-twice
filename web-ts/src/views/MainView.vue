<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import type { Ref } from 'vue'
import PageHeader from '../components/common/PageHeader.vue'
import PageFooter from '../components/common/PageFooter.vue'
import { systemServiceV1 } from '@/services/api/v1/apiServiceV1'

const scrollY: Ref<number> = ref(0)
const isReadOnlyMode = ref<boolean>(false)

const updateScroll = (): void => {
  scrollY.value = window.scrollY
}

// 加载只读模式状态
const loadReadOnlyMode = async (): Promise<void> => {
  try {
    const response = await systemServiceV1.getReadonlyMode()
    if (response.code === 200 && response.data) {
      isReadOnlyMode.value = response.data.enabled
    }
  } catch (error) {
    console.error('Failed to load readonly mode status:', error)
  }
}

// 定期检查只读模式状态（每分钟）
const startReadOnlyModePolling = (): void => {
  setInterval(() => {
    loadReadOnlyMode()
  }, 60000) // 每60秒检查一次
}

onMounted(() => {
  window.addEventListener('scroll', updateScroll)
  loadReadOnlyMode()
  startReadOnlyModePolling()
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

    <!-- 维护模式横幅 -->
    <v-container
      v-if="isReadOnlyMode"
      max-width="1600"
      class="maintenance-container"
      :class="{ 'compact-header': scrollY > 20 }"
    >
      <v-alert
        type="warning"
        density="compact"
        variant="tonal"
        class="maintenance-alert"
      >
        <div class="d-flex align-center">
          <v-icon icon="mdi-wrench" size="small" class="mr-2"></v-icon>
          <span class="text-body-2 font-weight-medium">系统维护中</span>
          <span class="text-body-2 ml-2 text-grey-darken-2">当前处于只读模式，暂时无法进行创建、修改、删除操作</span>
        </div>
      </v-alert>
    </v-container>

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

.maintenance-container {
  position: sticky;
  top: 45px;
  z-index: 999;
  padding-top: 8px;
  padding-bottom: 0px;
  background-color: white;
}

.maintenance-container.compact-header {
  top: 40px;
}

.maintenance-alert {
  margin: 0 !important;
}
</style>