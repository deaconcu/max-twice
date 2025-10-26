<script setup lang="ts">
  import { ref, watch, inject } from 'vue'
  import draggable from 'vuedraggable'
  import { useRoute } from 'vue-router'
  import { useI18n } from 'vue-i18n'
  import { aiServiceV1, postServiceV1 } from '@/services/api/v1/apiServiceV1'
  import { PostType } from '@/types/enums'

  const route = useRoute()
  const { t } = useI18n()
  const showSnackbar = inject<(message: string) => void>('showSnackbar')

  interface Props {
    nodeId: number
    pathText: string
  }

  const props = defineProps<Props>()

  const dialog = defineModel<boolean>({
    default: false,
  })

  interface Emits {
    (e: 'loadData', data: any[]): void
  }

  const emit = defineEmits<Emits>()

  interface ContentItem {
    name: string
    description: string
  }

  const createContentsTab = ref<string | number>(0)
  const newContentsItem = ref<string>('')
  const newContentsDescription = ref<string>('')
  const newContents = ref<ContentItem[]>([])

  watch(
    () => route.query.path,
    () => {
      newContents.value = []
    }
  )

  const addContentsItem = (): void => {
    if (newContentsItem.value.trim() !== '') {
      newContents.value.push({
        name: newContentsItem.value.trim(),
        description: newContentsDescription.value.trim(),
      })
      newContentsItem.value = ''
      newContentsDescription.value = ''
    }
  }

  const removeContentsItem = (index: number): void => {
    newContents.value.splice(index, 1) // 根据索引从数组中移除
  }

  const submitAddContents = async (): Promise<void> => {
    try {
      // 验证目录数量
      if (newContents.value.length < 2) {
        showSnackbar?.('目录至少需要2个子目录')
        return
      }

      console.log('begin post')

      const contentArray = newContents.value.map((item) => ({
        [item.name]: item.description,
      }))

      const data = {
        content: JSON.stringify(contentArray),
        nodeId: props.nodeId,
        type: PostType.CONTENTS,
      }

      console.log(`request: ${JSON.stringify(data)}`)
      const response = await postServiceV1.createPost(data)
      console.log(`response: ${JSON.stringify(response)}`)

      if (response.code === 200) {
        console.log('Form submitted successfully')
        showSnackbar?.('目录添加成功')
        dialog.value = false
        emit('loadData', [])
      } else {
        showSnackbar?.(response.message || '目录添加失败')
      }
    } catch (error: any) {
      console.error('Error submitting form:', error)
      showSnackbar?.(error.response?.data?.message || error.message || '目录添加失败，请稍后重试')
    }
  }

  const addAIContents = async (): Promise<void> => {
    try {
      console.log('begin get')
      const response = await aiServiceV1.chat(
        t('addContents.aiPrompt', { pathText: props.pathText }),
        'openai/gpt-4o-mini'
      )
      console.log(`response: ${JSON.stringify(response)}`)
      if (response.code === 200) {
        newContents.value = JSON.parse(response.data)
      }
    } catch (error) {
      console.error('Error submitting form:', error)
    }
  }
</script>

<template>
  <v-dialog v-model="dialog" width="1100" height="700px">
    <v-card prepend-icon="mdi-account" :title="t('addContents.title')" rounded="xl">
      <v-row class="ma-0 border-t-sm">
        <v-col class="border-e-sm">
          <v-card-text>
            <v-tabs v-model="createContentsTab">
              <v-tab value="one">{{ t('addContents.createNode') }}</v-tab>
              <v-tab value="two">{{ t('addContents.selectExisting') }}</v-tab>
              <v-tab value="three">{{ t('addContents.myNodes') }}</v-tab>
            </v-tabs>

            <v-card-text class="px-0">
              <v-tabs-window v-model="createContentsTab">
                <v-tabs-window-item value="one">
                  <v-text-field
                    v-model="newContentsItem"
                    :label="t('addContents.nodeName')"
                    variant="outlined"
                    class="pt-5"
                  ></v-text-field>
                  <v-textarea
                    v-model="newContentsDescription"
                    :label="t('addContents.nodeDescription')"
                    variant="outlined"
                    rows="3"
                    class="pt-2"
                  ></v-textarea>
                  <v-btn variant="tonal" class="me-4" @click="addContentsItem">{{
                    t('addContents.submit')
                  }}</v-btn>
                  <v-btn variant="plain" @click="addAIContents">{{
                    t('addContents.aiGenerate')
                  }}</v-btn>
                </v-tabs-window-item>
                <v-tabs-window-item value="two"> Two </v-tabs-window-item>
                <v-tabs-window-item value="three"> Three </v-tabs-window-item>
              </v-tabs-window>
            </v-card-text>
          </v-card-text>
        </v-col>

        <v-col>
          <div class="scrollable-div px-5 py-1">
            <draggable v-model="newContents" item-key="name">
              <template #item="{ element, index }">
                <div class="d-flex justify-space-between align-start pt-3 dashed-border-bottom pb-2">
                  <div class="flex-grow-1">
                    <div class="font-weight-medium">{{ index + 1 }}. {{ element.name }}</div>
                    <div v-if="element.description" class="text-caption text-grey-darken-1 mt-1">
                      {{ element.description }}
                    </div>
                  </div>
                  <v-btn
                    v-ripple="false"
                    icon
                    flat
                    size="small"
                    variant="text"
                    elevation="0"
                    @click="removeContentsItem(index)"
                  >
                    <v-icon size="15">mdi-close</v-icon>
                  </v-btn>
                </div>
              </template>
            </draggable>
          </div>
        </v-col>
      </v-row>
      <v-card-actions class="d-flex justify-center py-5 border-t-sm">
        <v-btn class="primary-button" @click="submitAddContents">{{
          t('addContents.confirm')
        }}</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
  .dashed-border-bottom {
    border-bottom: 1px dashed #ddd;
  }

  .primary-button {
    background-color: #1976d2 !important;
    color: #fff !important;
    width: 100px;
  }
</style>