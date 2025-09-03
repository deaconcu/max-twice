<script setup>
  import { useI18n } from 'vue-i18n'

  const { t } = useI18n()

  // Props
  defineProps({
    messageList: {
      type: Array,
      default: () => [],
    },
  })

  // Emits
  const emit = defineEmits(['loadData'])

  // 处理数据加载
  const handleLoadData = (loadOptions) => {
    emit('loadData', loadOptions)
  }
</script>

<template>
  <v-slide-y-reverse-transition hide-on-leave>
    <div class="text-body-1 d-flex justify-start">
      <v-row class="max-width-row">
        <v-col cols="" class="mt-0">
          <div class="px-0 mx-0 pt-0">
            <div class="px-8">
              <v-infinite-scroll
                :items="messageList"
                :on-load="handleLoadData"
                :no-more-text="t('message.noMoreMessages')"
                class="infinite-scroll-offset"
              >
                <div v-for="message in messageList" :key="message.id" class="mb-4">
                  <div class="pb-5 border-b d-flex justify-start">
                    <div
                      class="px-3 py-2 rounded-lg d-inline-block flex-1-1 text-start text-body-1"
                    >
                      <p class="text-subtitle-1 font-weight-bold pb-3 text-grey-darken-2">
                        {{ t('message.courseApplicationTitle') }}
                        <span v-if="message.content.parentId != '0'">(子课程)</span>
                      </p>
                      <p class="pb-2">
                        <span class="text-grey">名称：</span>{{ message.content.title }}
                      </p>
                      <p class="pb-2">
                        <span class="text-grey">简介</span>：{{ message.content.summary }}
                      </p>
                      <p class="pb-2">
                        <span class="text-grey">理由</span>：{{ message.content.explanation }}
                      </p>
                      <p v-if="message.content.parentId != '0'" class="pb-2">
                        <span class="text-grey">父课程</span>：{{ message.content.parentName }}
                      </p>
                      <p><span class="text-grey">状态</span>：{{ message.content.reply }}</p>
                    </div>
                    <span class="ms-5 mt-2 text-right text-caption text-grey-lighten-1"
                      >5小时前</span
                    >
                  </div>
                </div>
                <template #empty>
                  <div class="text-body-2 text-grey py-5">{{ t('message.noMoreMessages') }}</div>
                </template>
              </v-infinite-scroll>
            </div>
          </div>
        </v-col>
      </v-row>
    </div>
  </v-slide-y-reverse-transition>
</template>

<style scoped>
  /* 课程申请消息样式 */
  .max-width-row {
    max-width: 1150px;
  }

  .border-b {
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  }

  .flex-1-1 {
    flex: 1 1 auto;
  }

  /* 消息卡片样式 */
  .rounded-lg {
    border-radius: 12px;
  }

  /* 文字样式优化 */
  .text-grey {
    color: rgba(0, 0, 0, 0.6) !important;
  }

  .text-grey-darken-2 {
    color: rgba(0, 0, 0, 0.8) !important;
  }

  /* 无限滚动偏移 */
  .infinite-scroll-offset {
    position: relative;
    top: -12px;
  }

  /* 悬浮效果 */
  .pb-5:hover {
    background-color: rgba(0, 0, 0, 0.02);
    border-radius: 8px;
    transition: all 0.2s ease;
  }
</style>
