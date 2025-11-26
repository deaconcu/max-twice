<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import draggable from 'vuedraggable'
import { courseApi } from '@/api'
import { useMutation } from '@/composables'

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const route = useRoute()
const router = useRouter()

interface Props {
  courseId: number
  contents: any[]
}

type Emits = (e: 'load-data', data: any[]) => void

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

// 使用 useMutation 提交目录更新
const { execute: submitUpdate, loading: submitting } = useMutation(
  () => courseApi.updateUserCourseToc(props.courseId, list.value.join(',')),
  {
    successMessage: '目录更新成功',
    onSuccess: () => {
      // 首先关闭对话框
      dialog.value = false

      // 然后处理路由逻辑
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
        emit('load-data', [])
      } else {
        router.replace({
          path: '/read',
          query: { courseId: String(props.courseId), path: nextPath },
        })
      }
    },
  }
)

const submit = async () => {
  await submitUpdate()
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
          <span class="text-h6 font-weight-bold">修改目录</span>
        </div>
        <v-btn icon="mdi-close" variant="text" size="small" @click="dialog = false"></v-btn>
      </v-card-title>

      <v-card-text class="pa-0">
        <v-sheet height="400px" class="overflow-auto">
          <div class="px-6 py-3 bg-grey-lighten-5">
            <div class="text-body-2 text-grey-darken-2">拖动调整顺序，添加新目录或复制现有目录</div>
          </div>

          <div class="px-6 py-4">
            <draggable v-model="list" item-key="id" handle=".drag-handle">
              <template #item="{ element, index }">
                <div class="toc-item d-flex align-center mb-3 pa-3">
                  <div class="drag-handle mr-3">
                    <v-icon icon="mdi-drag" size="20" color="grey"></v-icon>
                  </div>

                  <div class="flex-grow-1">
                    <span v-if="element > 0" class="text-body-1">目录 {{ element }}</span>
                    <span v-if="element === 0" class="text-body-1 text-error">新目录</span>
                    <span v-if="element < 0" class="text-body-1 text-error">
                      目录 {{ -element }} 的副本
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
                      复制
                    </v-btn>
                    <v-btn
                      variant="text"
                      size="small"
                      color="error"
                      prepend-icon="mdi-close"
                      @click="removeItem(index)"
                    >
                      删除
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
            添加新目录
          </v-btn>
          <v-btn variant="text" color="primary" prepend-icon="mdi-refresh" @click="resetList">
            重置
          </v-btn>
        </div>
        <div class="d-flex" style="gap: 8px">
          <v-btn variant="text" color="grey-darken-2" @click="dialog = false">取消</v-btn>
          <v-btn color="primary" variant="flat" :loading="submitting" @click="submit"> 确定 </v-btn>
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
