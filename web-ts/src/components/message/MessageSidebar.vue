<script setup lang="ts">
  import { computed } from 'vue'
  import { useI18n } from 'vue-i18n'

  const { t } = useI18n()

  type MessageType = 'system' | 'courseApply' | 'private'

  interface MenuItem {
    text: string
    icon: string
    value: MessageType
    subtitle: string
  }

  interface Props {
    selectedMessageType?: MessageType
    unreadCount?: number
    totalMessages?: number
  }

  // Props
  defineProps<Props>()

  interface Emits {
    (e: 'update:selectedMessageType', value: MessageType): void
  }

  // Emits
  const emit = defineEmits<Emits>()

  // 导航菜单项
  const menuItems = computed((): MenuItem[] => [
    {
      text: t('message.systemNotification'),
      icon: 'mdi-bell-outline',
      value: 'system',
      subtitle: '系统通知消息',
    },
    {
      text: t('message.courseApplication'),
      icon: 'mdi-file-document-outline',
      value: 'courseApply',
      subtitle: '课程申请消息',
    },
    {
      text: '私信消息',
      icon: 'mdi-message-text-outline',
      value: 'private',
      subtitle: '用户私信消息',
    },
  ])

  // 处理菜单选择
  const handleMenuSelect = (value: MessageType): void => {
    emit('update:selectedMessageType', value)
  }
</script>

<template>
  <v-col class="pr-6 pt-0 message-sidebar-container">
    <v-card flat color="grey-lighten-5" rounded="xl" class="sticky-nav">
      <v-card-text class="pa-4">
        <h3 class="text-h6 font-weight-bold text-grey-darken-4 mb-4">
          <v-icon icon="mdi-format-list-bulleted" color="primary" size="18" class="mr-2"></v-icon>
          消息分类
        </h3>

        <v-list bg-color="transparent" class="pa-0">
          <v-list-item
            v-for="item in menuItems"
            :key="item.value"
            :value="item.value"
            class="nav-item ma-1 rounded-lg"
            :class="[selectedMessageType === item.value ? 'nav-item-active' : 'nav-item-inactive']"
            @click="handleMenuSelect(item.value)"
          >
            <template #prepend>
              <v-avatar
                :color="selectedMessageType === item.value ? 'primary' : 'grey-lighten-2'"
                size="32"
                class="mr-3"
              >
                <v-icon
                  :icon="item.icon"
                  :color="selectedMessageType === item.value ? 'white' : 'grey-darken-2'"
                  size="16"
                ></v-icon>
              </v-avatar>
            </template>

            <v-list-item-title
              class="font-weight-medium"
              :class="selectedMessageType === item.value ? 'text-primary' : 'text-grey-darken-3'"
            >
              {{ item.text }}
            </v-list-item-title>

            <v-list-item-subtitle class="text-caption">
              {{ item.subtitle }}
            </v-list-item-subtitle>

            <template #append>
              <v-icon
                icon="mdi-chevron-right"
                :color="selectedMessageType === item.value ? 'primary' : 'grey-lighten-1'"
                size="16"
              ></v-icon>
            </template>
          </v-list-item>
        </v-list>

        <!-- 消息统计 -->
        <div class="mt-4 pt-4 border-t">
          <div class="d-flex align-center justify-space-between mb-2">
            <span class="text-body-2 text-grey-darken-3">未读消息</span>
            <v-chip color="warning" variant="flat" size="x-small">
              {{ unreadCount || 0 }}
            </v-chip>
          </div>
          <div class="d-flex align-center justify-space-between">
            <span class="text-body-2 text-grey-darken-3">总消息</span>
            <v-chip color="grey-lighten-1" variant="flat" size="x-small">
              {{ totalMessages || 0 }}
            </v-chip>
          </div>
        </div>
      </v-card-text>
    </v-card>
  </v-col>
</template>

<style scoped>
  .sticky-nav {
    position: sticky;
    top: 65px;
  }

  /* 导航项样式 - 匹配 HotRanking.vue */
  .nav-item {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    cursor: pointer;
    border: 1px solid transparent;
    padding: 8px 12px;
  }

  .nav-item-inactive {
    background: rgba(255, 255, 255, 0.7);
  }

  .nav-item-inactive:hover {
    background: rgba(25, 118, 210, 0.08);
    border-color: rgba(25, 118, 210, 0.2);
  }

  .nav-item-active {
    background: rgba(25, 118, 210, 0.1);
    border-color: rgba(25, 118, 210, 0.3);
  }

  .nav-item-active:hover {
    background: rgba(25, 118, 210, 0.15);
    border-color: rgba(25, 118, 210, 0.4);
  }

  /* 边框分隔线 */
  .border-t {
    border-top: 1px solid rgba(0, 0, 0, 0.08);
  }

  .message-sidebar-container {
    max-width: 320px;
  }
</style>