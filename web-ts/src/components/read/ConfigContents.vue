<script setup lang="ts">
  import { ref, watch } from 'vue'
  import { useRoute, useRouter } from 'vue-router'

  import draggable from 'vuedraggable'
  import { courseServiceV1 } from '@/services/api/v1/apiServiceV1'
  import { useI18n } from 'vue-i18n'
  import { useMutation } from '@/composables/useMutation'

  const route = useRoute()
  const router = useRouter()
  const { t } = useI18n()

  interface Emits {
    (e: 'loadData', data: any[]): void
  }

  const emit = defineEmits<Emits>()

  interface Props {
    courseId: number
    contents: any[]
  }

  const props = defineProps<Props>()

  const list = ref<number[]>([])
  const dialog = defineModel<boolean>({
    default: false,
  })

  // 初始化 list
  watch(
    () => props.contents,
    (newContents: any[]) => {
      list.value = Array.from({ length: newContents.length }, (_, i) => i + 1)
    },
    { immediate: true }
  )

  // 使用 useMutation 提交目录更新
  const { execute: submitUpdate, loading: submitting } = useMutation(
    () => courseServiceV1.updateUserCourseToc(
      props.courseId,
      list.value.join(',')
    ),
    {
      successMessage: '目录更新成功',
      onSuccess: () => {
        const pathParts = (route.query.path as string).split('-')
        const currIndex = Number(pathParts[0])
        const index = list.value.indexOf(currIndex) + 1

        let nextPath: string
        if (index === 0) {
          nextPath = `1-${pathParts[1]}`
        } else {
          nextPath = `${index}-${pathParts.slice(1).join('-')}`
        }

        dialog.value = false

        if (nextPath === route.query.path) {
          emit('loadData', [])
        } else {
          router.replace({
            name: 'read',
            query: {
              courseId: props.courseId,
              path: nextPath,
            },
          })
        }
      }
    }
  )

  const submit = async (): Promise<void> => {
    await submitUpdate()
  }

  const addItem = (): void => {
    list.value.push(0)
    console.log(`list: ${JSON.stringify(list.value)}`)
  }

  const copyItem = (index: number, val: number): void => {
    list.value.splice(index + 1, 0, -val)
    console.log(`list: ${JSON.stringify(list.value)}`)
  }

  const removeItem = (index: number): void => {
    list.value.splice(index, 1)
    console.log(`list: ${JSON.stringify(list.value)}`)
  }
</script>

<template>
  <v-dialog v-model="dialog" width="800" height="600" content-class="fix-dialog">
    <v-card class="px-1 py-2" rounded="lg">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-file-cog-outline" size="small" class=""></v-icon>
        <span class="ps-2">{{ t('configContents.modifyContents') }}</span>
      </v-card-title>
      <v-card-subtitle>
        {{ t('configContents.subtitle') }}
      </v-card-subtitle>
      <v-sheet height="500px" class="overflow-auto">
        <v-card-text>
          <draggable v-model="list" item-key="id" class="pt-3">
            <template #item="{ element, index }">
              <div
                class="d-flex justify-space-between align-center mb-5 pb-1 text-body-1 dashed-border-bottom"
              >
                <span v-if="element > 0">{{
                  t('configContents.contentItem', { number: element })
                }}</span>
                <span v-if="element === 0" class="text-red-darken-3">{{
                  t('configContents.newContent')
                }}</span>
                <span v-if="element < 0" class="text-red-darken-3">{{
                  t('configContents.copyOfContent', { number: -element })
                }}</span>
                <v-spacer />
                <div>
                  <v-btn
                    v-if="element > 0"
                    v-ripple="false"
                    flat
                    size="small"
                    variant="text"
                    elevation="0"
                    prepend-icon="mdi-content-copy"
                    class="me-1"
                    @click="copyItem(index, element)"
                  >
                    {{ t('configContents.copy') }}
                  </v-btn>
                  <v-btn
                    v-ripple="false"
                    flat
                    size="small"
                    variant="text"
                    elevation="0"
                    prepend-icon="mdi-close"
                    @click="removeItem(index)"
                  >
                    {{ t('configContents.delete') }}
                  </v-btn>
                </div>
              </div>
            </template>
          </draggable>
        </v-card-text>
      </v-sheet>
      <v-card-actions class="d-flex justify-end">
        <v-btn
          color="teal-lighten-1"
          variant="text"
          density="comfortable"
          size="large"
          class="rounded-lg"
          @click="addItem()"
          ><span class="font-weight-medium">{{ t('configContents.addContent') }}</span></v-btn
        >
        <v-btn
          color="teal-lighten-1"
          variant="text"
          density="comfortable"
          size="large"
          class="rounded-lg"
          :loading="submitting"
          @click="submit()"
          ><span class="font-weight-medium">{{ t('configContents.done') }}</span></v-btn
        >
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
  :deep(.fix-dialog) {
    top: 150px !important;
    position: absolute !important;
  }

  .dashed-border-bottom {
    border-bottom: 1px dashed #ddd;
  }
</style>