<script setup lang="ts">
import { ref, watch, inject } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import draggable from 'vuedraggable'
import { useI18n } from '@/composables/useI18n'
import { useUpdateNodeTocMutation } from '@/queries/toc'

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

interface Props {
  nodeId: number
  contents: any[]
}

type Emits = (e: 'load-data') => void

const dialog = defineModel<boolean>({ default: false })
const list = ref<number[]>([])

// 初始化 list
watch(
  () => props.contents,
  (newContents: any[]) => {
    list.value = Array.from({ length: newContents.length }, (_, i) => i + 1)
  },
  { immediate: true }
)

const updateNodeTocMutation = useUpdateNodeTocMutation()
const submitting = updateNodeTocMutation.isPending

const submit = () => {
  updateNodeTocMutation.mutate(
    { nodeId: props.nodeId, indexArray: list.value.join(',') },
    {
      onSuccess: () => {
        showSnackbar?.(t('posting.operationSuccess'), 'success')

        if (!route.query.path) {
          emit('load-data')
          dialog.value = false
          return
        }

        const pathParts = (route.query.path as string).split('-')
        const currIndex = Number(pathParts[0])
        const index = list.value.indexOf(currIndex) + 1

        let nextPath: string
        if (index === 0) {
          nextPath = `1-${pathParts[1]}`
        } else {
          nextPath = `${index}-${pathParts.slice(1).join('-')}`
        }

        if (nextPath === route.query.path) {
          emit('load-data')
        } else {
          router.replace({
            path: '/read',
            query: { nodeId: String(props.nodeId), path: nextPath },
          })
        }

        dialog.value = false
      },
    }
  )
}

const addItem = () => {
  list.value.push(0)
}

const copyItem = (index: number, val: number) => {
  list.value.splice(index + 1, 0, -val)
}

const removeItem = (index: number) => {
  list.value.splice(index, 1)
}

const resetList = () => {
  list.value = Array.from({ length: props.contents.length }, (_, i) => i + 1)
}
</script>

<template>
  <v-dialog v-model="dialog" width="800" persistent>
    <v-card rounded="xl">
      <v-card-title class="pa-4 d-flex align-center justify-space-between">
        <div class="d-flex align-center">
          <v-icon icon="mdi-file-cog-outline" color="primary" class="mr-2"></v-icon>
          <span class="text-h6 font-weight-bold">{{ t('configContents.modifyContents') }}</span>
        </div>
        <v-btn icon="mdi-close" variant="text" size="small" @click="dialog = false"></v-btn>
      </v-card-title>

      <v-card-text class="pa-0">
        <v-sheet height="400px" class="overflow-auto">
          <div class="px-6 py-3 bg-grey-lighten-5">
            <div class="text-body-2 text-grey-darken-2">{{ t('configContents.subtitle') }}</div>
          </div>

          <div class="px-6 py-4">
            <draggable v-model="list" item-key="id" handle=".drag-handle">
              <template #item="{ element, index }">
                <div class="toc-item d-flex align-center mb-3 pa-3">
                  <div class="drag-handle mr-3">
                    <v-icon icon="mdi-drag" size="20" color="grey"></v-icon>
                  </div>

                  <div class="flex-grow-1">
                    <span v-if="element > 0" class="text-body-1">{{
                      t('configContents.contentItem', { number: element })
                    }}</span>
                    <span v-if="element === 0" class="text-body-1 text-error">{{
                      t('configContents.newContent')
                    }}</span>
                    <span v-if="element < 0" class="text-body-1 text-error">
                      {{ t('configContents.copyOfContent', { number: -element }) }}
                    </span>
                  </div>

                  <div class="d-flex" style="gap: 8px">
                    <v-btn
                      v-if="element > 0"
                      variant="text"
                      size="small"
                      color="grey-darken-2"
                      prepend-icon="mdi-content-copy"
                      @click="copyItem(index, element)"
                    >
                      {{ t('common.copy') }}
                    </v-btn>
                    <v-btn
                      variant="text"
                      size="small"
                      color="error"
                      prepend-icon="mdi-close"
                      @click="removeItem(index)"
                    >
                      {{ t('common.delete') }}
                    </v-btn>
                  </div>
                </div>
              </template>
            </draggable>
          </div>
        </v-sheet>
      </v-card-text>

      <v-card-actions class="pa-4 d-flex justify-space-between">
        <div class="d-flex" style="gap: 8px">
          <v-btn variant="text" color="primary" prepend-icon="mdi-plus" @click="addItem">
            {{ t('configContents.addContent') }}
          </v-btn>
          <v-btn variant="text" color="primary" prepend-icon="mdi-refresh" @click="resetList">
            {{ t('common.reset') }}
          </v-btn>
        </div>
        <div class="d-flex" style="gap: 8px">
          <v-btn variant="text" color="grey-darken-2" @click="dialog = false">{{
            t('common.cancel')
          }}</v-btn>
          <v-btn color="primary" variant="flat" :loading="submitting" @click="submit">
            {{ t('common.confirm') }}
          </v-btn>
        </div>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.toc-item {
  background-color: white;
  border: 1px solid rgb(var(--v-theme-border));
  border-radius: 8px;
  transition: all 0.2s ease;
}

.toc-item:hover {
  background-color: #f6f7f8;
}

.drag-handle {
  cursor: move;
  padding: 4px;
  border-radius: 4px;
}

.drag-handle:hover {
  background-color: rgba(0, 0, 0, 0.05);
}
</style>
