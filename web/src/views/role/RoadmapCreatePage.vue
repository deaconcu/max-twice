<template>
  <DefaultLayout>
    <div class="roadmap-create-page">
      <!-- 页面标题 -->
      <div class="mb-6 mb-md-8">
        <div class="d-flex align-center justify-space-between">
          <div class="d-flex align-center" style="min-width: 0">
            <v-avatar
              color="primary"
              :size="$vuetify.display.mobile ? 40 : 48"
              class="mr-3 flex-shrink-0"
            >
              <v-icon
                icon="mdi-briefcase-outline"
                color="white"
                :size="$vuetify.display.mobile ? 20 : 24"
              />
            </v-avatar>
            <div style="min-width: 0">
              <h1 class="text-h6 text-md-h5 font-weight-bold text-grey-darken-4 text-truncate">
                {{
                  isEditMode
                    ? t('roadmapCreate.editTitle')
                    : copyId
                      ? t('roadmapCreate.copyTitle')
                      : t('roadmapCreate.createTitle')
                }}
              </h1>
              <p class="text-caption text-sm-body-2 text-grey-darken-2 text-truncate">
                {{
                  isEditMode
                    ? t('roadmapCreate.subtitleEdit', { name: roleName })
                    : t('roadmapCreate.subtitleCreate', { name: roleName })
                }}
              </p>
            </div>
          </div>

          <v-btn
            :size="$vuetify.display.mobile ? 'small' : 'default'"
            variant="tonal"
            color="grey-darken-1"
            rounded="lg"
            @click="goToMyRoadmaps"
          >
            <v-icon icon="mdi-format-list-bulleted" size="18" class="mr-1" />
            <span class="d-none d-sm-inline">{{ t('roadmapCreate.myRoadmaps') }}</span>
          </v-btn>
        </div>
      </div>

      <!-- 加载状态 -->
      <LoadingSpinner v-if="loading" />

      <!-- 草稿描述 -->
      <div v-if="!loading && savedDraftDescription" class="draft-description-top mb-4">
        <div class="d-flex align-start">
          <div class="flex-1" style="min-width: 0">
            <div class="text-caption text-grey-darken-1 mb-1">
              {{ t('roadmapCreate.draftDescription') }}
            </div>
            <div class="text-body-2 font-weight-medium text-grey-darken-3 draft-description-text">
              {{ savedDraftDescription }}
            </div>
          </div>
          <v-btn icon size="small" variant="text" @click="showSaveDialog = true">
            <v-icon icon="mdi-file-document-edit-outline" color="grey-darken-1" size="20" />
          </v-btn>
        </div>
      </div>

      <div v-if="!loading" class="content-layout">
        <!-- 编辑器主区 -->
        <div class="main-content">
          <v-card border rounded="xl" class="flow-editor-card">
            <v-card-title
              class="pa-3 pa-sm-4 d-flex flex-row align-center justify-space-between ga-2 ga-sm-3"
            >
              <span class="text-h6 font-weight-bold">{{ t('roadmapCreate.editorTitle') }}</span>
              <div class="d-flex flex-wrap align-center gap-2">
                <v-tooltip :text="t('roadmapCreate.undo')" location="top" :open-delay="100">
                  <template #activator="{ props: tipProps }">
                    <v-btn
                      v-bind="tipProps"
                      :size="$vuetify.display.mobile ? 'small' : 'default'"
                      variant="tonal"
                      color="grey-darken-1"
                      rounded="lg"
                      icon
                      :disabled="!canUndo"
                      @click="undo"
                    >
                      <v-icon icon="mdi-undo" size="18" />
                    </v-btn>
                  </template>
                </v-tooltip>
                <v-tooltip :text="t('roadmapCreate.redo')" location="top" :open-delay="100">
                  <template #activator="{ props: tipProps }">
                    <v-btn
                      v-bind="tipProps"
                      :size="$vuetify.display.mobile ? 'small' : 'default'"
                      variant="tonal"
                      color="grey-darken-1"
                      rounded="lg"
                      icon
                      :disabled="!canRedo"
                      @click="redo"
                    >
                      <v-icon icon="mdi-redo" size="18" />
                    </v-btn>
                  </template>
                </v-tooltip>
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="warning"
                  rounded="lg"
                  @click="resetAll"
                >
                  <v-icon icon="mdi-refresh" size="18" class="mr-1" />
                  <span class="d-none d-sm-inline">{{ t('roadmapCreate.reset') }}</span>
                </v-btn>
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="primary"
                  rounded="lg"
                  @click="showSave"
                >
                  <v-icon icon="mdi-content-save" size="18" class="mr-1" />
                  <span class="d-none d-sm-inline">{{
                    t('roadmapCreate.saveWithDescription')
                  }}</span>
                </v-btn>
              </div>
            </v-card-title>
            <v-card-text class="pa-0">
              <div class="flow-editor">
                <RoadmapEditor
                  ref="editorRef"
                  v-model="trunk"
                  :role-name="roleName"
                  @request-bind="onRequestBind"
                  @cancel-bind="onCancelBind"
                  @show-message="(msg, type) => showSnackbar(msg, type ?? 'info')"
                />
              </div>
            </v-card-text>
          </v-card>
        </div>

        <!-- 右侧：课程/节点搜索面板 -->
        <div v-if="bindingType" class="right-sidebar">
          <v-card class="course-search-card sticky-card no-border" elevation="0">
            <v-card-text class="pa-0 ps-4">
              <div v-if="bindingType === 'course'">
                <div class="d-flex align-center justify-space-between mb-3">
                  <span class="text-subtitle-2 font-weight-bold text-grey-darken-4">{{
                    t('roadmapCreate.searchCourses')
                  }}</span>
                  <div class="d-flex align-center ga-2">
                    <a
                      href="/courses"
                      target="_blank"
                      class="text-caption text-primary text-decoration-none"
                    >
                      {{ t('common.viewAll') }}
                    </a>
                    <v-btn
                      icon="mdi-close"
                      variant="text"
                      size="small"
                      density="comfortable"
                      @click="closeBinding"
                    />
                  </div>
                </div>

                <v-text-field
                  v-model="searchText"
                  :placeholder="t('roadmapCreate.searchCoursesPlaceholder')"
                  variant="outlined"
                  density="comfortable"
                  hide-details
                  class="mb-3"
                  rounded="lg"
                  autocomplete="off"
                  @keydown.enter="searchCourses"
                  @click:clear="
                    () => {
                      searchText = ''
                      availableCourses = []
                    }
                  "
                >
                  <template #append-inner>
                    <v-btn icon size="small" variant="text" @click="searchCourses">
                      <v-icon icon="mdi-magnify" size="20" />
                    </v-btn>
                  </template>
                </v-text-field>

                <div v-if="coursesLoading" class="text-center py-8">
                  <v-progress-circular indeterminate color="primary" size="40" width="3" />
                  <p class="text-body-2 text-grey-darken-1 mt-3">{{ t('common.loading') }}</p>
                </div>

                <div v-else-if="!searchText.trim()" class="empty-state text-center py-8">
                  <div class="empty-icon-wrapper mb-3">
                    <v-icon icon="mdi-magnify" size="56" color="grey-lighten-1" />
                  </div>
                  <p class="text-body-2 text-grey-darken-1 mb-1">
                    {{ t('roadmapCreate.searchCoursesHint') }}
                  </p>
                  <p class="text-caption text-grey">
                    {{ t('roadmapCreate.searchCoursesSubHint') }}
                  </p>
                </div>

                <div v-else class="course-list-wrapper">
                  <div v-if="availableCourses.length > 0" class="course-list">
                    <div v-for="course in availableCourses" :key="course.id" class="course-item">
                      <v-tooltip location="left" max-width="300" content-class="rounded-lg">
                        <template #activator="{ props }">
                          <div
                            class="course-name"
                            v-bind="props"
                            @click="goToCourseDetail(course.id)"
                          >
                            <v-icon icon="mdi-book-outline" size="16" class="mr-1" />
                            {{ course.name }}
                          </div>
                        </template>
                        <div class="tooltip-content pa-1">
                          <div class="text-subtitle-2 mb-1">{{ course.name }}</div>
                          <div class="text-caption text-grey-lighten-1 mb-2">
                            {{
                              categoryStore.getCourseFullCategoryText(
                                course.mainCategory,
                                course.subCategory
                              )
                            }}
                          </div>
                          <div class="text-caption">
                            {{ course.description || t('common.noDescription') }}
                          </div>
                        </div>
                      </v-tooltip>
                      <v-btn
                        icon
                        size="x-small"
                        color="primary"
                        variant="flat"
                        :disabled="isCourseAdded(course)"
                        @click.stop="bindCourse(course)"
                      >
                        <v-icon size="14">{{
                          isCourseAdded(course) ? 'mdi-check' : 'mdi-plus'
                        }}</v-icon>
                      </v-btn>
                    </div>
                  </div>
                  <div v-else class="text-center py-6">
                    <v-icon
                      icon="mdi-book-off-outline"
                      size="48"
                      color="grey-lighten-1"
                      class="mb-2"
                    />
                    <p class="text-body-2 text-grey">{{ t('roadmapCreate.noCoursesFound') }}</p>
                  </div>
                </div>
              </div>

              <div v-else-if="bindingType === 'node'">
                <div class="d-flex align-center justify-space-between mb-3">
                  <span class="text-subtitle-2 font-weight-bold text-grey-darken-4">{{
                    t('roadmapCreate.searchNodes')
                  }}</span>
                  <v-btn
                    icon="mdi-close"
                    variant="text"
                    size="small"
                    density="comfortable"
                    @click="closeBinding"
                  />
                </div>

                <v-text-field
                  v-model="nodeSearchText"
                  :placeholder="t('roadmapCreate.searchNodesPlaceholder')"
                  variant="outlined"
                  density="comfortable"
                  hide-details
                  class="mb-3"
                  rounded="lg"
                  autocomplete="off"
                  @keydown.enter="searchNodes"
                  @click:clear="
                    () => {
                      nodeSearchText = ''
                      availableNodes = []
                    }
                  "
                >
                  <template #append-inner>
                    <v-btn icon size="small" variant="text" @click="searchNodes">
                      <v-icon icon="mdi-magnify" size="20" />
                    </v-btn>
                  </template>
                </v-text-field>

                <div v-if="nodesLoading" class="text-center py-8">
                  <v-progress-circular indeterminate color="success" size="40" width="3" />
                  <p class="text-body-2 text-grey-darken-1 mt-3">{{ t('common.loading') }}</p>
                </div>

                <div v-else-if="!nodeSearchText.trim()" class="empty-state text-center py-8">
                  <div class="empty-icon-wrapper mb-3">
                    <v-icon icon="mdi-magnify" size="56" color="grey-lighten-1" />
                  </div>
                  <p class="text-body-2 text-grey-darken-1 mb-1">
                    {{ t('roadmapCreate.searchNodesHint') }}
                  </p>
                  <p class="text-caption text-grey">{{ t('roadmapCreate.searchNodesSubHint') }}</p>
                </div>

                <div v-else class="course-list-wrapper">
                  <div v-if="availableNodes.length > 0" class="course-list">
                    <div v-for="node in availableNodes" :key="node.id" class="course-item">
                      <v-tooltip location="left" max-width="300" content-class="rounded-lg">
                        <template #activator="{ props }">
                          <div class="node-content" v-bind="props">
                            <div class="course-name" @click="goToNodeDetail(node.id)">
                              <v-icon
                                icon="mdi-file-document-outline"
                                size="16"
                                class="mr-1"
                                color="success"
                              />
                              {{ node.name }}
                            </div>
                          </div>
                        </template>
                        <div class="tooltip-content pa-1">
                          <div class="text-subtitle-2 mb-1">{{ node.name }}</div>
                          <div class="text-caption">
                            {{ node.description || t('common.noDescription') }}
                          </div>
                        </div>
                      </v-tooltip>
                      <v-btn
                        icon
                        size="x-small"
                        color="success"
                        variant="flat"
                        :disabled="isNodeAdded(node)"
                        @click.stop="bindNode(node)"
                      >
                        <v-icon size="14">{{ isNodeAdded(node) ? 'mdi-check' : 'mdi-plus' }}</v-icon>
                      </v-btn>
                    </div>
                  </div>
                  <div v-else class="text-center py-6">
                    <v-icon
                      icon="mdi-file-document-off-outline"
                      size="48"
                      color="grey-lighten-1"
                      class="mb-2"
                    />
                    <p class="text-body-2 text-grey">{{ t('roadmapCreate.noNodesFound') }}</p>
                  </div>
                </div>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </div>
    </div>

    <!-- 保存对话框 -->
    <v-dialog v-model="showSaveDialog" max-width="600px">
      <v-card rounded="xl" border>
        <v-btn
          icon="mdi-close"
          variant="text"
          size="small"
          class="dialog-close-btn"
          @click="showSaveDialog = false"
        />
        <v-card-title class="pa-6">
          <div class="d-flex align-center">
            <v-icon icon="mdi-content-save" color="primary" size="32" class="mr-3" />
            <span class="text-h6 font-weight-bold">{{ t('roadmapCreate.saveTitle') }}</span>
          </div>
        </v-card-title>
        <v-card-text class="px-6 pb-0">
          <v-textarea
            v-model="roadmapDescription"
            :label="t('roadmapCreate.descriptionLabel')"
            :placeholder="t('roadmapCreate.descriptionPlaceholder')"
            :rules="roadmapDescriptionRules"
            :counter="roadmapDescriptionMaxLength"
            variant="outlined"
            clearable
            required
            rows="4"
            auto-grow
            :hint="t('roadmapCreate.descriptionHint')"
            persistent-hint
          />
        </v-card-text>
        <v-card-actions class="px-6 pb-6 pt-4">
          <v-spacer />
          <v-btn
            v-if="canSaveAsDraft"
            color="grey-darken-1"
            variant="flat"
            rounded="lg"
            :disabled="!roadmapDescription.trim() || saving"
            :loading="saving && saveType === 'draft'"
            @click="saveRoadmap('draft')"
          >
            {{ t('roadmapCreate.saveAsDraft') }}
          </v-btn>
          <v-btn
            color="primary"
            variant="flat"
            rounded="lg"
            :disabled="!roadmapDescription.trim() || saving"
            :loading="saving && saveType === 'publish'"
            @click="saveRoadmap('publish')"
          >
            {{ t('roadmapCreate.saveAndPublish') }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <ConfirmDialog
      v-model="confirmDialogVisible"
      :title="confirmDialogConfig.title"
      :message="confirmDialogConfig.message"
      :confirm-text="confirmDialogConfig.confirmText"
      :cancel-text="confirmDialogConfig.cancelText"
      :confirm-color="confirmDialogConfig.confirmColor"
      :icon="confirmDialogConfig.icon"
      :icon-color="confirmDialogConfig.iconColor"
      :icon-foreground="confirmDialogConfig.iconForeground"
      @confirm="confirmDialogConfig.onConfirm"
    />
  </DefaultLayout>
</template>

<script setup lang="ts">
import { ref, computed, inject, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import RoadmapEditor from '@/components/features/role/RoadmapEditor.vue'
import type { RoadmapNode } from '@/components/features/role/RoadmapEditor.vue'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'
import { useCategoryStore } from '@/stores'
import { courseApi } from '@/api/modules/course'
import { searchApi } from '@/api/modules/search'
import {
  useRoadmapDetailQuery,
  useCreateRoadmapMutation,
  useUpdateRoadmapMutation,
} from '@/queries/roadmap'
import { useRoleDetailQuery } from '@/queries/role'
import { getGlobalSnackbar } from '@/composables/config'
import type { Course } from '@/types/course'
import type { SearchResultItem } from '@/api/modules/search'
import { useI18n } from '@/composables/useI18n'

const { t } = useI18n()

const router = useRouter()
const route = useRoute()
const categoryStore = useCategoryStore()

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')!
const globalSnackbar = getGlobalSnackbar()

const roadmapDescriptionRules = useValidationRules('roadmap-description')
const roadmapDescriptionMaxLength = useMaxLength('roadmap-description')

/* ========== 路由参数 ========== */
const roleId = computed(() => {
  const id = route.params.id
  return typeof id === 'string' ? parseInt(id, 10) : 0
})
const roadmapId = computed(() => {
  const id = route.params.roadmapId
  return id ? (typeof id === 'string' ? parseInt(id, 10) : Number(id)) : null
})
const isEditMode = computed(() => roadmapId.value !== null)
const copyId = ref(route.query.copy ? Number(route.query.copy) : null)

/* ========== 状态 ========== */
const trunk = ref<RoadmapNode[]>([])
const editorRef = ref<InstanceType<typeof RoadmapEditor> | null>(null)

/* ========== 历史栈（撤销/重做） ========== */
const HISTORY_LIMIT = 50
const history = ref<RoadmapNode[][]>([[]])
const historyIndex = ref(0)
let isTimeTraveling = false

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < history.value.length - 1)

const resetHistory = (initial: RoadmapNode[]) => {
  history.value = [JSON.parse(JSON.stringify(initial))]
  historyIndex.value = 0
}

watch(
  trunk,
  (newVal) => {
    if (isTimeTraveling) return
    const snapshot = JSON.parse(JSON.stringify(newVal)) as RoadmapNode[]
    // 截断 redo 部分
    history.value = history.value.slice(0, historyIndex.value + 1)
    history.value.push(snapshot)
    if (history.value.length > HISTORY_LIMIT) {
      history.value.shift()
    }
    historyIndex.value = history.value.length - 1
  },
  { deep: true }
)

const undo = () => {
  if (!canUndo.value) return
  isTimeTraveling = true
  historyIndex.value--
  trunk.value = JSON.parse(JSON.stringify(history.value[historyIndex.value]))
  nextTick(() => {
    isTimeTraveling = false
  })
}

const redo = () => {
  if (!canRedo.value) return
  isTimeTraveling = true
  historyIndex.value++
  trunk.value = JSON.parse(JSON.stringify(history.value[historyIndex.value]))
  nextTick(() => {
    isTimeTraveling = false
  })
}

const saving = ref(false)
const saveType = ref<'draft' | 'publish' | ''>('')
const showSaveDialog = ref(false)
const roadmapDescription = ref('')
const savedDraftDescription = ref('')
const draftRoadmapId = ref<number | null>(null)
const roadmapState = ref<number | null>(null)

const { data: roleData } = useRoleDetailQuery(roleId)
const roleName = computed(() => roleData.value?.name ?? '')

const canSaveAsDraft = computed(
  () => roadmapState.value === null || roadmapState.value === 0
)

/* ========== 确认对话框 ========== */
const confirmDialogVisible = ref(false)
const confirmDialogConfig = ref({
  title: '',
  message: '',
  confirmText: t('common.confirm'),
  cancelText: t('common.cancel'),
  confirmColor: 'error',
  icon: 'mdi-alert-circle-outline',
  iconColor: 'error-lighten-4',
  iconForeground: 'error',
  onConfirm: () => {},
})

/* ========== 绑定面板 ========== */
const bindingType = ref<'course' | 'node' | null>(null)
const pendingNodeId = ref<string | null>(null)

const onRequestBind = (payload: { nodeId: string; type: 'course' | 'node' }) => {
  pendingNodeId.value = payload.nodeId
  bindingType.value = payload.type
  if (payload.type === 'course') {
    searchText.value = ''
    availableCourses.value = []
  } else {
    nodeSearchText.value = ''
    availableNodes.value = []
  }
}

const onCancelBind = () => {
  bindingType.value = null
  pendingNodeId.value = null
}

const closeBinding = () => {
  bindingType.value = null
  pendingNodeId.value = null
  editorRef.value?.cancelSelection()
}

/* ========== 课程搜索 ========== */
const searchText = ref('')
const availableCourses = ref<Course[]>([])
const coursesLoading = ref(false)

const searchCourses = async () => {
  if (!searchText.value.trim()) {
    availableCourses.value = []
    return
  }
  coursesLoading.value = true
  try {
    const data = await courseApi.searchCourses(searchText.value.trim())
    availableCourses.value = data ?? []
  } catch (error) {
    console.error('搜索课程失败:', error)
    showSnackbar(t('roadmapCreate.messages.searchCoursesFailed'), 'error')
  } finally {
    coursesLoading.value = false
  }
}

watch(searchText, () => {
  availableCourses.value = []
})

const isCourseAdded = (course: Course) =>
  editorRef.value?.isNodeAddedById(course.rootNodeId ?? 0, 'course') ?? false

const bindCourse = (course: Course) => {
  if (!course.rootNodeId) {
    showSnackbar(t('roadmapCreate.messages.courseCannotBeAdded'), 'warning')
    return
  }
  if (!pendingNodeId.value) return
  editorRef.value?.applyBinding(pendingNodeId.value, {
    type: 'course',
    id: course.id,
    rootNodeId: course.rootNodeId,
    label: `${t('roadmapDetail.courseLabel')} ${course.name}`,
  })
  // 绑定后 selectedNodeForBinding 已变为新 id
  const newId = editorRef.value?.getSelectedNodeId()
  if (newId) pendingNodeId.value = newId
}

/* ========== 节点搜索 ========== */
const nodeSearchText = ref('')
const availableNodes = ref<SearchResultItem[]>([])
const nodesLoading = ref(false)

const searchNodes = async () => {
  if (!nodeSearchText.value.trim()) {
    availableNodes.value = []
    return
  }
  nodesLoading.value = true
  try {
    const data = await searchApi.searchNodes(nodeSearchText.value.trim())
    availableNodes.value = data ?? []
  } catch (error) {
    console.error('搜索节点失败:', error)
    showSnackbar(t('roadmapCreate.messages.searchNodesFailed'), 'error')
  } finally {
    nodesLoading.value = false
  }
}

watch(nodeSearchText, () => {
  availableNodes.value = []
})

const isNodeAdded = (node: SearchResultItem) =>
  editorRef.value?.isNodeAddedById(node.id, 'node') ?? false

const bindNode = (node: SearchResultItem) => {
  if (!pendingNodeId.value) return
  editorRef.value?.applyBinding(pendingNodeId.value, {
    type: 'node',
    id: node.id,
    label: `${t('roadmapDetail.nodeLabel')} ${node.name}`,
  })
  const newId = editorRef.value?.getSelectedNodeId()
  if (newId) pendingNodeId.value = newId
}

/* ========== 跳转 ========== */
const goToCourseDetail = (id: number) => window.open(`/courses/${id}`, '_blank')
const goToNodeDetail = (id: number) => window.open(`/read?nodeId=${id}`, '_blank')
const goToMyRoadmaps = () => router.push('/users/me?mode=creator&tab=roadmaps')

/* ========== 保存对话框 ========== */
const showSave = () => {
  if (trunk.value.length === 0) {
    showSnackbar(t('roadmapCreate.messages.addNodesFirst'), 'warning')
    return
  }
  showSaveDialog.value = true
}

/* ========== 校验 + 序列化 ========== */
// 序列化为后端格式（去 group 的 tmp id；只保留 t/id/label/children）
interface SerializedNode {
  t: 'c' | 'n' | 'g'
  id?: number
  label: string
  children?: SerializedNode[]
}

const serialize = (nodes: RoadmapNode[]): SerializedNode[] => {
  return nodes.map((n) => {
    let t: 'c' | 'n' | 'g' = 'g'
    let id: number | undefined
    if (n.nodeType === 'course') {
      t = 'c'
      id = n.courseId
    } else if (n.nodeType === 'node') {
      t = 'n'
      // node 的 id 形如 "n123"
      const num = parseInt(n.id.replace(/^n/, ''), 10)
      if (!isNaN(num)) id = num
    }
    const out: SerializedNode = { t, label: n.label }
    if (id !== undefined) out.id = id
    if (n.children?.length) out.children = serialize(n.children)
    return out
  })
}

// 反序列化
let loadGroupSeq = 0
const deserialize = (nodes: SerializedNode[]): RoadmapNode[] => {
  return nodes.map((n) => {
    let id: string
    let nodeType: 'course' | 'node' | 'group' | undefined
    let courseId: number | undefined
    if (n.t === 'c' && n.id != null) {
      id = `c${n.id}`
      nodeType = 'course'
      courseId = n.id
    } else if (n.t === 'n' && n.id != null) {
      id = `n${n.id}`
      nodeType = 'node'
    } else {
      id = `g_load_${++loadGroupSeq}`
      nodeType = n.children?.length ? 'group' : undefined
    }
    const out: RoadmapNode = { id, label: n.label, nodeType }
    if (courseId !== undefined) out.courseId = courseId
    if (n.children?.length) out.children = deserialize(n.children)
    return out
  })
}

// 校验：所有非 group 必须已绑定（有 nodeType=course/node）
const validateAllBound = (nodes: RoadmapNode[]): boolean => {
  for (const n of nodes) {
    if (!n.nodeType) return false
    if (n.children?.length && !validateAllBound(n.children)) return false
  }
  return true
}

/* ========== 保存 ========== */
const { mutate: createRoadmapMutate } = useCreateRoadmapMutation()
const { mutate: updateRoadmapMutate } = useUpdateRoadmapMutation()

const saveRoadmap = (type: 'draft' | 'publish') => {
  if (!roadmapDescription.value.trim()) {
    showSnackbar(t('roadmapCreate.messages.enterDescription'), 'warning')
    return
  }
  if (trunk.value.length === 0) {
    showSnackbar(t('roadmapCreate.messages.addNodesFirst'), 'warning')
    return
  }
  // 发布模式要求所有节点已绑定
  if (type === 'publish' && !validateAllBound(trunk.value)) {
    showSnackbar(t('roadmapCreate.messages.connectCourses'), 'warning')
    return
  }

  saving.value = true
  saveType.value = type

  try {
    const content = JSON.stringify({ v: 2, trunk: serialize(trunk.value) })
    const state = type === 'draft' ? 0 : 1

    const onSaveSuccess = (result: { id?: number } | null | undefined) => {
      const message =
        type === 'draft'
          ? t('roadmapCreate.messages.draftSaved')
          : t('roadmapCreate.messages.published')
      globalSnackbar?.(message, 'success')
      showSaveDialog.value = false
      if (type === 'draft') {
        savedDraftDescription.value = roadmapDescription.value.trim()
        if (result?.id) draftRoadmapId.value = result.id
      } else {
        router.back()
      }
      saving.value = false
      saveType.value = ''
    }
    const onSaveError = () => {
      showSnackbar(t('roadmapCreate.messages.saveFailedRetry'), 'error')
      saving.value = false
      saveType.value = ''
    }

    if (draftRoadmapId.value) {
      updateRoadmapMutate(
        {
          id: draftRoadmapId.value,
          content,
          description: roadmapDescription.value.trim(),
          state,
        },
        { onSuccess: onSaveSuccess, onError: onSaveError }
      )
    } else {
      createRoadmapMutate(
        {
          roleId: roleId.value,
          content,
          description: roadmapDescription.value.trim(),
          state,
        },
        { onSuccess: onSaveSuccess, onError: onSaveError }
      )
    }
  } catch (error) {
    console.error('保存路径失败:', error)
    showSnackbar(t('roadmapCreate.messages.saveFailedRetry'), 'error')
    saving.value = false
    saveType.value = ''
  }
}

/* ========== 重置 ========== */
const resetAll = () => {
  confirmDialogConfig.value = {
    title: t('roadmapCreate.messages.resetTitle'),
    message: t('roadmapCreate.messages.resetConfirm'),
    confirmText: t('common.reset'),
    cancelText: t('common.cancel'),
    confirmColor: 'warning',
    icon: 'mdi-refresh',
    iconColor: 'warning-lighten-4',
    iconForeground: 'warning',
    onConfirm: () => {
      trunk.value = []
      roadmapDescription.value = ''
      bindingType.value = null
      pendingNodeId.value = null
      showSnackbar(t('roadmapCreate.messages.resetSuccess'), 'success')
    },
  }
  confirmDialogVisible.value = true
}

/* ========== 加载 ========== */
const { data: roadmapData, isLoading: roadmapLoading } = useRoadmapDetailQuery(
  computed(() => roadmapId.value ?? 0),
  { enabled: isEditMode }
)

const { data: copyRoadmapData, isLoading: copyRoadmapLoading } = useRoadmapDetailQuery(
  computed(() => copyId.value ?? 0),
  { enabled: computed(() => !!copyId.value) }
)

const loading = computed(() => roadmapLoading.value || copyRoadmapLoading.value)

const loadFromContent = (raw: string | object | undefined): RoadmapNode[] => {
  if (!raw) return []
  try {
    const data = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (data && data.v === 2 && Array.isArray(data.trunk)) {
      return deserialize(data.trunk)
    }
    // 旧格式不兼容
    showSnackbar(t('roadmapCreate.messages.loadFailed'), 'warning')
    return []
  } catch (e) {
    console.error('解析路线图内容失败:', e)
    showSnackbar(t('roadmapCreate.messages.loadFailed'), 'error')
    return []
  }
}

watch(roadmapData, (newData) => {
  if (newData && isEditMode.value) {
    roadmapDescription.value = newData.description || ''
    savedDraftDescription.value = newData.description || ''
    draftRoadmapId.value = newData.id
    roadmapState.value = newData.state ?? null
    isTimeTraveling = true
    trunk.value = loadFromContent(newData.content)
    resetHistory(trunk.value)
    nextTick(() => {
      isTimeTraveling = false
    })
  }
})

watch(copyRoadmapData, (newData) => {
  if (newData && copyId.value) {
    roadmapDescription.value = `${newData.description || t('roadmapCreate.unnamedRoadmap')} ${t('roadmapCreate.copySuffix')}`
    isTimeTraveling = true
    trunk.value = loadFromContent(newData.content)
    resetHistory(trunk.value)
    nextTick(() => {
      isTimeTraveling = false
    })
  }
})
</script>

<style scoped>
.roadmap-create-page {
  /* 使用 DefaultLayout 的默认 padding */
}

.draft-description-text {
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  line-height: 1.5;
}

.dialog-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 1;
}

.content-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (min-width: 1280px) {
  .content-layout {
    flex-direction: row;
    gap: 24px;
    height: calc(100vh - 56px - 40px - 80px);
  }
}

.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

@media (min-width: 1280px) {
  .flow-editor-card {
    display: flex;
    flex-direction: column;
    flex: 1;
  }

  .flow-editor-card .v-card-text {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
}

.right-sidebar {
  width: 100%;
}

@media (min-width: 1280px) {
  .right-sidebar {
    width: 320px;
    flex-shrink: 0;
  }
}

.flow-editor-card {
  background-color: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-outline));
}

.course-search-card {
  background-color: rgb(var(--v-theme-surface));
  transition: box-shadow 0.2s ease;
}

.empty-icon-wrapper {
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-8px);
  }
}

.course-list-wrapper {
  max-height: 350px;
  overflow-y: auto;
}

@media (min-width: 1280px) {
  .course-list-wrapper {
    max-height: calc(100vh - 520px);
  }
}

.course-list-wrapper::-webkit-scrollbar {
  width: 4px;
}

.course-list-wrapper::-webkit-scrollbar-track {
  background: transparent;
}

.course-list-wrapper::-webkit-scrollbar-thumb {
  background-color: rgba(var(--v-theme-on-surface), 0.1);
  border-radius: 2px;
}

.course-list-wrapper::-webkit-scrollbar-thumb:hover {
  background-color: rgba(var(--v-theme-on-surface), 0.2);
}

.course-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.course-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px 8px 0;
  background: transparent;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  gap: 12px;
}

.course-item:hover {
  transform: translateY(-2px);
}

.course-name {
  flex: 1;
  display: flex;
  align-items: center;
  cursor: pointer;
  color: rgb(var(--v-theme-grey-darken-3));
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s ease;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-name:hover {
  color: rgb(var(--v-theme-primary));
}

.course-name .v-icon {
  flex-shrink: 0;
  color: rgb(var(--v-theme-grey-darken-1));
}

.course-item:hover .course-name .v-icon {
  color: rgb(var(--v-theme-primary));
}

.flow-editor {
  height: 500px;
  min-height: 400px;
  background: rgb(var(--v-theme-surface));
  position: relative;
}

@media (min-width: 1280px) {
  .flow-editor {
    height: 100%;
    min-height: 400px;
  }
}

.sticky-card {
  display: flex;
  flex-direction: column;
}

@media (min-width: 1280px) {
  .sticky-card {
    position: sticky;
    top: 75px;
    max-height: calc(100vh - 95px);
    overflow: hidden;
  }
}

.gap-2 {
  gap: 8px;
}
</style>
