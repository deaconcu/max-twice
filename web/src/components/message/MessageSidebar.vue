<script setup>
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

// Props
const props = defineProps({
  selectedMessageType: {
    type: String,
    default: 'system'
  },
  unreadCount: {
    type: Number,
    default: 0
  },
  totalMessages: {
    type: Number,
    default: 0
  }
});

// Emits
const emit = defineEmits(['update:selectedMessageType']);

// 导航菜单项
const menuItems = computed(() => [
  { text: t('message.systemNotification'), icon: 'mdi-chat-outline', value: "system" },
  { text: t('message.courseApplication'), icon: 'mdi-chat-outline', value: "courseApply" },
]);

// 处理菜单选择
const handleMenuSelect = (value) => {
  emit('update:selectedMessageType', value);
};
</script>

<template>
  <v-col class="pr-8" style="max-width: 320px;">
    <v-card flat color="grey-lighten-5" rounded="lg" class="sticky-left px-2" style="position: sticky; top: 90px;">
      <!-- 头部标题区域 -->
      <v-card-text class="pa-4">
        <div class="d-flex align-center mb-3">
          <v-avatar color="grey-darken-2" size="32" class="mr-3">
            <v-icon icon="mdi-message-reply-text" color="white" size="16"></v-icon>
          </v-avatar>
          <div>
            <h3 class="text-h6 font-weight-bold text-grey-darken-4">{{ t('message.center') }}</h3>
            <p class="text-body-2 text-grey-darken-2 mb-0">{{ t('message.subtitle') }}</p>
          </div>
        </div>
      </v-card-text>

      <!-- 导航菜单列表 -->
      <v-list class="pa-2 bg-transparent" density="compact">
        <v-list-item 
          v-for="(item, i) in menuItems" 
          :key="i" 
          :value="item.value" 
          :active="selectedMessageType === item.value"
          @click="handleMenuSelect(item.value)" 
          density="comfortable" 
          rounded="lg" 
          class="mb-1 nav-item"
          :class="{ 'nav-item-active': selectedMessageType === item.value }">
          
          <template v-slot:prepend>
            <v-icon :icon="item.icon" size="18" class="nav-icon"></v-icon>
          </template>
          <v-list-item-title class="text-body-1 nav-title">
            {{ item.text }}
          </v-list-item-title>
        </v-list-item>
      </v-list>

      <!-- 底部统计信息 -->
      <v-card-text class="pa-4 border-t">
        <div class="text-body-2 text-grey-darken-3 mb-2">
          <div class="d-flex justify-space-between align-center mb-1">
            <span>{{ t('message.unreadMessages') }}</span>
            <span class="text-primary font-weight-bold">{{ unreadCount || 0 }}</span>
          </div>
          <div class="d-flex justify-space-between align-center">
            <span>{{ t('message.totalMessages') }}</span>
            <span class="text-grey-darken-2 font-weight-medium">{{ totalMessages || 0 }}</span>
          </div>
        </div>
      </v-card-text>
    </v-card>
  </v-col>
</template>

<style scoped>
/* 导航项样式 */
.nav-item {
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.nav-item:hover {
  background: rgba(178, 223, 219, 0.15) !important;
}

.nav-item-active {
  background: #e3f2fd !important;
  color: #1976d2 !important;
}

.nav-item-active .nav-icon {
  color: #1976d2 !important;
}

.nav-item-active .nav-title {
  color: #1976d2 !important;
  font-weight: 600 !important;
}
</style>