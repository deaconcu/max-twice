<template>
  <DefaultLayout>
    <div class="roadmap-create-page">
      <!-- 页面标题 -->
      <div class="mb-6 mb-md-8">
        <div class="d-flex align-center title-row">
          <!-- 图标和标题 -->
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
        </div>
      </div>

      <!-- 加载状态 -->
      <LoadingSpinner v-if="loading" />

      <!-- 草稿描述（顶部，宽屏时跟标题在一行） -->
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
        <!-- 流程图编辑器（占满页面） -->
        <div class="main-content">
          <v-card border rounded="xl" class="flow-editor-card">
            <v-card-title
              class="pa-3 pa-sm-4 d-flex flex-row align-center justify-space-between ga-2 ga-sm-3"
            >
              <div class="d-flex align-center">
                <span class="text-h6 font-weight-bold me-4">{{
                  t('roadmapCreate.editorTitle')
                }}</span>
                <v-chip v-if="nodes.length > 1" size="small" color="primary" variant="tonal">
                  {{ nodes.length - 1 }} {{ t('roadmapCreate.nodeCount') }}
                </v-chip>
              </div>
              <div class="d-flex flex-wrap align-center gap-2">
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="error"
                  rounded="lg"
                  @click="deleteSelectedNodes"
                >
                  <v-icon
                    icon="mdi-delete"
                    :size="$vuetify.display.mobile ? 16 : 18"
                    class="mr-1"
                  />
                  <span class="d-none d-sm-inline">{{ t('roadmapCreate.deleteSelected') }}</span>
                </v-btn>
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="warning"
                  rounded="lg"
                  @click="resetAll"
                >
                  <v-icon
                    icon="mdi-refresh"
                    :size="$vuetify.display.mobile ? 16 : 18"
                    class="mr-1"
                  />
                  <span class="d-none d-sm-inline">{{ t('roadmapCreate.reset') }}</span>
                </v-btn>
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="info"
                  rounded="lg"
                  @click="applyAutoLayout(true)"
                >
                  <v-icon
                    icon="mdi-auto-fix"
                    :size="$vuetify.display.mobile ? 16 : 18"
                    class="mr-1"
                  />
                  <span class="d-none d-sm-inline">{{ t('roadmapCreate.autoLayout') }}</span>
                </v-btn>
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="primary"
                  rounded="lg"
                  @click="showSave"
                >
                  <v-icon
                    icon="mdi-content-save"
                    :size="$vuetify.display.mobile ? 16 : 18"
                    class="mr-1"
                  />
                  <span class="d-none d-sm-inline">{{
                    t('roadmapCreate.saveWithDescription')
                  }}</span>
                </v-btn>
                <v-divider vertical class="mx-2 toolbar-divider" />
                <v-btn
                  :size="$vuetify.display.mobile ? 'small' : 'default'"
                  variant="tonal"
                  color="grey-darken-1"
                  rounded="lg"
                  @click="goToMyRoadmaps"
                >
                  <v-icon
                    icon="mdi-format-list-bulleted"
                    :size="$vuetify.display.mobile ? 16 : 18"
                    class="mr-1"
                  />
                  <span class="d-none d-sm-inline">{{ t('roadmapCreate.myRoadmaps') }}</span>
                </v-btn>
              </div>
            </v-card-title>
            <v-card-text class="pa-0">
              <div class="flow-editor">
                <VueFlow
                  :nodes="nodes"
                  :edges="edges"
                  :nodes-draggable="false"
                  :nodes-connectable="false"
                  :elements-selectable="false"
                  :zoom-on-scroll="false"
                  :zoom-on-pinch="false"
                  :zoom-on-double-click="false"
                  :prevent-scrolling="false"
                  :min-zoom="1.1"
                  :max-zoom="1.1"
                  @node-mouse-enter="onNodeMouseEnter"
                  @node-mouse-leave="onNodeMouseLeave"
                  @pane-click="cancelBinding"
                >
                  <Background pattern-color="#e0e0e0" :gap="20" :size="1" variant="dots" />
                  <template #node-root="{ id, data }">
                    <Handle id="top" type="target" :position="Position.Top" />
                    <div class="node-wrapper">
                      <div class="node-root">{{ data.label }}</div>
                      <div v-if="hoveredNodeId === id" class="node-actions" @mouseenter="onActionsEnter" @mouseleave="onActionsLeave">
                        <button class="node-action-btn" title="在后面插入节点" @click="insertNodeAfter(id)">
                          <v-icon icon="mdi-arrow-down-bold-outline" size="14" />
                        </button>
                      </div>
                    </div>
                    <Handle id="bottom" type="source" :position="Position.Bottom" />
                  </template>
                  <template #node-end="{ id, data }">
                    <Handle id="top" type="target" :position="Position.Top" />
                    <div class="node-wrapper">
                      <div class="node-end">{{ data.label }}</div>
                      <div v-if="hoveredNodeId === id" class="node-actions" @mouseenter="onActionsEnter" @mouseleave="onActionsLeave">
                        <button class="node-action-btn" title="在前面插入节点" @click="insertNodeBefore(id)">
                          <v-icon icon="mdi-arrow-up-bold-outline" size="14" />
                        </button>
                      </div>
                    </div>
                    <Handle id="bottom" type="source" :position="Position.Bottom" />
                  </template>
                  <template #node-topic="{ id, data }">
                    <Handle id="top" type="target" :position="Position.Top" />
                    <Handle id="bottom" type="source" :position="Position.Bottom" />
                    <Handle id="left" type="source" :position="Position.Left" />
                    <Handle id="left-in" type="target" :position="Position.Left" />
                    <Handle id="right" type="source" :position="Position.Right" />
                    <Handle id="right-in" type="target" :position="Position.Right" />
                    <div class="node-wrapper">
                      <div
                        class="node-topic"
                        :class="[
                          data.nodeType === 'course'
                            ? 'node-topic--course'
                            : data.nodeType === 'node'
                              ? 'node-topic--node'
                              : 'node-topic--group',
                          { 'node-topic--selected': selectedNodeForBinding === id },
                        ]"
                      >{{ data.label }}</div>
                      <div v-if="hoveredNodeId === id" class="node-actions" @mouseenter="onActionsEnter" @mouseleave="onActionsLeave">
                        <button class="node-action-btn" title="在前面插入节点" @click="insertNodeBefore(id)">
                          <v-icon icon="mdi-arrow-up-bold-outline" size="14" />
                        </button>
                        <button class="node-action-btn" title="在后面插入节点" @click="insertNodeAfter(id)">
                          <v-icon icon="mdi-arrow-down-bold-outline" size="14" />
                        </button>
                        <button class="node-action-btn" title="设置为课程节点" @click="setNodeAsCourse(id)">
                          <v-icon icon="mdi-book-outline" size="14" />
                        </button>
                        <button class="node-action-btn" title="设置为 node 节点" @click="setNodeAsNode(id)">
                          <v-icon icon="mdi-file-document-outline" size="14" />
                        </button>
                        <button v-if="!branchedNodes.has(id)" class="node-action-btn" title="创建新路径" @click="createBranch(id)">
                          <v-icon icon="mdi-source-branch" size="14" />
                        </button>
                        <button v-else class="node-action-btn" title="移除子路径" @click="removeBranch(id)">
                          <v-icon icon="mdi-source-branch-remove" size="14" />
                        </button>
                      </div>
                    </div>
                  </template>
                  <template #node-phantom>
                    <Handle id="bottom" type="source" :position="Position.Bottom" />
                    <Handle id="top" type="target" :position="Position.Top" />
                    <div style="width: 0; height: 0;" />
                  </template>
                </VueFlow>
              </div>
            </v-card-text>
          </v-card>
        </div>

        <!-- 右侧：课程/节点搜索面板（仅在绑定时显示） -->
        <div v-if="bindingType" class="right-sidebar">
          <v-card class="course-search-card sticky-card no-border" elevation="0">
            <v-card-text class="pa-0 ps-4">
              <!-- 课程搜索 -->
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
                      @click="cancelBinding"
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
                  @keydown.enter="handleSearch"
                  @click:clear="handleClearSearch"
                >
                  <template #append-inner>
                    <v-btn icon size="small" variant="text" @click="handleSearch">
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
                  <div v-if="filteredCourses.length > 0" class="course-list">
                    <div v-for="course in filteredCourses" :key="course.id" class="course-item">
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
                        :disabled="isNodeAdded(course.rootNodeId)"
                        @click.stop="bindCourseToSelected(course)"
                      >
                        <v-icon size="14">{{
                          isNodeAdded(course.rootNodeId) ? 'mdi-check' : 'mdi-plus'
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

              <!-- 节点搜索 -->
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
                    @click="cancelBinding"
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
                  @keydown.enter="handleNodeSearch"
                  @click:clear="handleClearNodeSearch"
                >
                  <template #append-inner>
                    <v-btn icon size="small" variant="text" @click="handleNodeSearch">
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
                  <div v-if="filteredNodes.length > 0" class="course-list">
                    <div v-for="node in filteredNodes" :key="node.id" class="course-item">
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
                        :disabled="isNodeAdded(node.id)"
                        @click.stop="bindNodeToSelected(node)"
                      >
                        <v-icon size="14">{{
                          isNodeAdded(node.id) ? 'mdi-check' : 'mdi-plus'
                        }}</v-icon>
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
        <!-- 关闭按钮 -->
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

          <!-- 孤立节点提示 -->
          <v-alert
            v-if="hasIsolatedNodes"
            type="warning"
            variant="tonal"
            density="compact"
            class="mt-4"
          >
            {{ t('roadmapCreate.isolatedNodesWarning', { count: isolatedNodesCount }) }}
          </v-alert>
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

    <!-- 统一确认对话框 -->
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
import { ref, computed, inject, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { VueFlow, useVueFlow, Handle } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { MiniMap } from '@vue-flow/minimap'
import { Controls } from '@vue-flow/controls'
import { Position, SelectionMode } from '@vue-flow/core'
import type { Node, Edge, Connection } from '@vue-flow/core'
import dagre from 'dagre'
import DefaultLayout from '@/components/layout/DefaultLayout.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { useValidationRules, useMaxLength } from '@/composables/useValidation'
import { useCategoryStore } from '@/stores'
import { courseApi } from '@/api/modules/course'
import { searchApi } from '@/api/modules/search'
import { useRoadmapDetailQuery, useCreateRoadmapMutation, useUpdateRoadmapMutation } from '@/queries/roadmap'
import { useRoleDetailQuery } from '@/queries/role'
import { getGlobalSnackbar } from '@/composables/config'
import type { Course } from '@/types/course'
import type { SearchResultItem } from '@/api/modules/search'
import { useI18n } from '@/composables/useI18n'

const { t } = useI18n()

const router = useRouter()
const route = useRoute()
const categoryStore = useCategoryStore()

// 注入全局 snackbar
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')!
const globalSnackbar = getGlobalSnackbar()

// 获取 VueFlow 实例
const { fitView, setCenter, updateNodeData } = useVueFlow()

// 验证规则
const roadmapDescriptionRules = useValidationRules('roadmap-description')
const roadmapDescriptionMaxLength = useMaxLength('roadmap-description')

// 从路由获取参数
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

// 状态管理
const saving = ref(false)
const saveType = ref<'draft' | 'publish' | ''>('') // 保存类型
const showSaveDialog = ref(false)
const roadmapDescription = ref('')
const savedDraftDescription = ref('') // 已保存的草稿描述
const draftRoadmapId = ref<number | null>(null) // 草稿路线图ID
const roadmapState = ref<number | null>(null) // 路线图状态：0=草稿，1=审核中，2=已发布
// 加载角色信息（创建模式从路由 roleId）
const { data: roleData } = useRoleDetailQuery(roleId)
const roleName = computed(() => roleData.value?.name ?? '')

// roleName 变化时同步终结节点 label
watch(roleName, (name) => {
  if (!name) return
  const endNode = nodes.value.find((n) => n.id === '__end')
  if (endNode) {
    endNode.data = { ...endNode.data, label: name }
  }
  updateNodeData('__end', { label: name })
})

// Tab 切换状态
const searchTab = ref<'course' | 'node'>('course')
const nodeSearchText = ref('')

// 是否可以保存为草稿：只有草稿状态(0)或新建时可以保存为草稿，已发布(2)不能变回草稿
const canSaveAsDraft = computed(() => {
  return roadmapState.value === null || roadmapState.value === 0
})

// 计算孤立节点
const hasIsolatedNodes = computed(() => {
  if (nodes.value.length <= 1 || edges.value.length === 0) {
    return false
  }

  const connectedNodeIds = new Set<string>()
  edges.value.forEach((e) => {
    connectedNodeIds.add(e.source)
    connectedNodeIds.add(e.target)
  })

  return nodes.value.some((n) => !connectedNodeIds.has(n.id))
})

const isolatedNodesCount = computed((): number => {
  if (!hasIsolatedNodes.value) {
    return 0
  }

  const connectedNodeIds = new Set<string>()
  edges.value.forEach((e) => {
    connectedNodeIds.add(e.source)
    connectedNodeIds.add(e.target)
  })

  return nodes.value.filter((n) => !connectedNodeIds.has(n.id)).length
})

// 确认对话框状态
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

// 可用课程列表
const availableCourses = ref<Course[]>([])
const coursesLoading = ref(false)
const searchText = ref('')

// 搜索课程
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

// 手动搜索方法（点击搜索按钮或按下回车触发）
const handleSearch = () => {
  searchCourses()
}

// 清除搜索
const handleClearSearch = () => {
  searchText.value = ''
  availableCourses.value = []
}

// 监听搜索文本变化，清空旧的搜索结果
watch(searchText, () => {
  availableCourses.value = []
})

const filteredCourses = computed(() => availableCourses.value)

// 节点搜索
const availableNodes = ref<SearchResultItem[]>([])
const nodesLoading = ref(false)

// 手动搜索节点
const handleNodeSearch = async () => {
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

// 清除节点搜索
const handleClearNodeSearch = () => {
  nodeSearchText.value = ''
  availableNodes.value = []
}

// 监听节点搜索文本变化，清空旧的搜索结果
watch(nodeSearchText, () => {
  availableNodes.value = []
})

const filteredNodes = computed(() => availableNodes.value ?? [])

// 节点样式常量
const ROOT_NODE_STYLE = {
  background: '#616161',
  color: '#ffffff',
  border: '2px solid #9e9e9e',
  borderRadius: '12px',
  padding: '10px',
  fontWeight: '600',
  fontSize: '14px',
}

const COURSE_NODE_STYLE = {
  background: '#fafafa',
  color: '#424242',
  border: '2px solid #bdbdbd',
  borderRadius: '12px',
  padding: '10px',
  fontWeight: '500',
  fontSize: '13px',
}

// 普通节点样式（绿色，区别于课程节点）
const NODE_STYLE = {
  background: '#f1f8e9',
  color: '#33691e',
  border: '2px solid #aed581',
  borderRadius: '12px',
  padding: '10px',
  fontWeight: '500',
  fontSize: '13px',
}

const EDGE_STYLE = {
  stroke: '#78909c',
  strokeWidth: 2,
}

// 布局常量（参考 RoadmapDemo）
const NODE_W = 160
const NODE_H = 36
const CENTER_X = 300
const TRUNK_GAP = 80 // 起止节点之间的初始间隔
const EXTEND = 40    // 顶部/底部虚线延伸长度

// 构建初始节点：phantom_top → 起始 → 终结 → phantom_bottom
const buildInitialNodes = (): Node[] => {
  const startY = 20
  const endY = startY + TRUNK_GAP
  return [
    {
      id: '__phantom_top',
      type: 'phantom',
      position: { x: CENTER_X + NODE_W / 2, y: startY - EXTEND },
      data: {},
    },
    {
      id: '__start',
      type: 'root',
      position: { x: CENTER_X, y: startY },
      data: { label: '从这里开始学习' },
    },
    {
      id: '__end',
      type: 'end',
      position: { x: CENTER_X, y: endY },
      data: { label: roleName.value },
    },
    {
      id: '__phantom_bottom',
      type: 'phantom',
      position: { x: CENTER_X + NODE_W / 2, y: endY + NODE_H + EXTEND },
      data: {},
    },
  ]
}

const buildInitialEdges = (): Edge[] => [
  {
    id: 'extend-top',
    source: '__phantom_top', target: '__start',
    sourceHandle: 'bottom', targetHandle: 'top',
    type: 'straight',
    style: { stroke: '#666', strokeWidth: 3, strokeDasharray: '8,5' },
  },
  {
    id: 'trunk-start-end',
    source: '__start', target: '__end',
    sourceHandle: 'bottom', targetHandle: 'top',
    type: 'straight',
    style: { stroke: '#666', strokeWidth: 3 },
  },
  {
    id: 'extend-bottom',
    source: '__end', target: '__phantom_bottom',
    sourceHandle: 'bottom', targetHandle: 'top',
    type: 'straight',
    style: { stroke: '#666', strokeWidth: 3, strokeDasharray: '8,5' },
  },
]

// 节点和边
const nodes = ref<Node[]>(buildInitialNodes())
const edges = ref<Edge[]>(buildInitialEdges())

// 当前 hover 的节点 id（用于显示插入菜单）
const hoveredNodeId = ref<string | null>(null)
let hoverLeaveTimer: ReturnType<typeof setTimeout> | null = null

const onNodeMouseEnter = (event: { node: Node }) => {
  if (hoverLeaveTimer) {
    clearTimeout(hoverLeaveTimer)
    hoverLeaveTimer = null
  }
  hoveredNodeId.value = event.node.id
}
const onNodeMouseLeave = () => {
  // 延时关闭，给鼠标移到菜单按钮的时间
  hoverLeaveTimer = setTimeout(() => {
    hoveredNodeId.value = null
    hoverLeaveTimer = null
  }, 200)
}
// 鼠标进入菜单区域时取消关闭
const onActionsEnter = () => {
  if (hoverLeaveTimer) {
    clearTimeout(hoverLeaveTimer)
    hoverLeaveTimer = null
  }
}
const onActionsLeave = () => {
  hoveredNodeId.value = null
}

// 临时节点 ID 生成
let tmpIdSeq = 0
const genTmpId = () => `__tmp_${Date.now()}_${++tmpIdSeq}`

// 课程/节点绑定浮层状态
const selectedNodeForBinding = ref<string | null>(null)
const bindingType = ref<'course' | 'node' | null>(null)

// TODO: 设置为课程节点
const setNodeAsCourse = (nodeId: string) => {
  selectedNodeForBinding.value = nodeId
  bindingType.value = 'course'
  // 重置搜索状态
  searchText.value = ''
  availableCourses.value = []
}

// TODO: 设置为 node 节点
const setNodeAsNode = (nodeId: string) => {
  selectedNodeForBinding.value = nodeId
  bindingType.value = 'node'
  nodeSearchText.value = ''
  availableNodes.value = []
}

// 取消绑定（关闭浮层）
const cancelBinding = () => {
  selectedNodeForBinding.value = null
  bindingType.value = null
}

// 替换节点 id 与所有相关 edge 的 source/target（一次性原子替换）
const replaceNodeId = (oldId: string, newId: string, dataPatch: Record<string, any>) => {
  if (oldId === newId) {
    nodes.value = nodes.value.map((n) =>
      n.id === oldId ? { ...n, data: { ...n.data, ...dataPatch } } : n
    )
    return
  }
  nodes.value = nodes.value.map((n) =>
    n.id === oldId ? { ...n, id: newId, data: { ...n.data, ...dataPatch } } : n
  )
  edges.value = edges.value.map((e) => {
    const source = e.source === oldId ? newId : e.source
    const target = e.target === oldId ? newId : e.target
    return { ...e, source, target, id: `${source}-${target}` }
  })

  // 同步 branchedNodes：key、nodeIds、edgeIds 中所有 oldId 引用替换为 newId
  const newMap = new Map<string, BranchInfo>()
  branchedNodes.value.forEach((info, key) => {
    const newKey = key === oldId ? newId : key
    const newNodeIds = info.nodeIds.map((id) => (id === oldId ? newId : id))
    const newEdgeIds = info.edgeIds.map((eid) => {
      // edge id 形如 "src-tgt"，分别替换两端
      const [s, ...rest] = eid.split('-')
      const t = rest.join('-')
      const ns = s === oldId ? newId : s
      const nt = t === oldId ? newId : t
      return `${ns}-${nt}`
    })
    newMap.set(newKey, { nodeIds: newNodeIds, edgeIds: newEdgeIds })
  })
  branchedNodes.value = newMap

  relayout()
}

// 把当前选中节点绑定为指定课程
const bindCourseToSelected = (course: Course) => {
  if (!selectedNodeForBinding.value) return
  if (!course.rootNodeId) {
    showSnackbar(t('roadmapCreate.messages.courseCannotBeAdded'), 'warning')
    return
  }
  const newId = course.rootNodeId.toString()
  if (
    newId !== selectedNodeForBinding.value &&
    nodes.value.some((n) => n.id === newId)
  ) {
    showSnackbar(t('roadmapCreate.messages.courseAlreadyAdded'), 'warning')
    return
  }
  replaceNodeId(selectedNodeForBinding.value, newId, {
    label: `${t('roadmapDetail.courseLabel')} ${course.name}`,
    nodeType: 'course',
    courseId: course.id,
  })
  // 选中节点 id 已变，更新 selectedNodeForBinding 以保持高亮和后续可重选
  selectedNodeForBinding.value = newId
}

// 把当前选中节点绑定为指定 node
const bindNodeToSelected = (n: SearchResultItem) => {
  if (!selectedNodeForBinding.value) return
  const newId = n.id.toString()
  if (
    newId !== selectedNodeForBinding.value &&
    nodes.value.some((nd) => nd.id === newId)
  ) {
    showSnackbar(t('roadmapCreate.messages.nodeAlreadyAdded'), 'warning')
    return
  }
  replaceNodeId(selectedNodeForBinding.value, newId, {
    label: `${t('roadmapDetail.nodeLabel')} ${n.name}`,
    nodeType: 'node',
  })
  selectedNodeForBinding.value = newId
}

// 列间距（参考 demo）
const COL_GAP = 40
const PATH_GAP = 60

// 记录已经创建过子路径的节点 → 子路径信息（链上的节点 id 和 edge id，按顺序）
interface BranchInfo {
  nodeIds: string[]   // 长度 ≥ 2，从入口到出口
  edgeIds: string[]   // 长度 = nodeIds.length + 1（入边、链中边、回边）
}
const branchedNodes = ref(new Map<string, BranchInfo>())

// 节点 id → 它所属分支的 source id（不属于任何分支则 undefined）
const getNodeBranchSource = (nodeId: string): string | undefined => {
  for (const [src, info] of branchedNodes.value.entries()) {
    if (info.nodeIds.includes(nodeId)) return src
  }
  return undefined
}

// 计算主干顺序中的索引（不含 phantom），决定该节点的分支侧
const getTrunkIndex = (nodeId: string): number => {
  const order: string[] = []
  let cur: string | undefined = '__start'
  const visited = new Set<string>()
  const branchNodeIds = new Set<string>()
  branchedNodes.value.forEach((info) => {
    info.nodeIds.forEach((id) => branchNodeIds.add(id))
  })
  while (cur && !visited.has(cur)) {
    visited.add(cur)
    order.push(cur)
    const next: any = edges.value.find(
      (e) => e.source === cur && !branchNodeIds.has(e.target) && e.target !== '__phantom_bottom',
    )
    cur = next?.target
  }
  return order.indexOf(nodeId)
}

// 根据 source 节点在主干中的索引决定分支侧（偶数右，奇数左）
const getBranchSide = (nodeId: string): 'left' | 'right' => {
  const idx = getTrunkIndex(nodeId)
  if (idx < 0) return 'right' // 不在主干上则默认右
  return idx % 2 === 0 ? 'right' : 'left'
}

// 从该节点创建新路径（侧由节点主干位置决定，稳定）
const createBranch = (nodeId: string) => {
  if (branchedNodes.value.has(nodeId)) {
    showSnackbar('该节点已有子路径', 'warning')
    return
  }
  if (!nodes.value.find((n) => n.id === nodeId)) return

  const side = getBranchSide(nodeId)

  const id1 = genTmpId()
  const id2 = genTmpId()

  // 位置由 relayout 计算，先放占位坐标
  nodes.value.push(
    { id: id1, type: 'topic', position: { x: 0, y: 0 }, data: { label: '新节点' } },
    { id: id2, type: 'topic', position: { x: 0, y: 0 }, data: { label: '新节点' } },
  )

  edges.value.push(
    {
      id: `${nodeId}-${id1}`,
      source: nodeId, target: id1,
      sourceHandle: side === 'right' ? 'right' : 'left',
      targetHandle: side === 'right' ? 'left-in' : 'right-in',
      type: 'default',
      style: { stroke: '#888', strokeWidth: 1.5, strokeDasharray: '8,4' },
    },
    {
      id: `${id1}-${id2}`,
      source: id1, target: id2,
      sourceHandle: 'bottom', targetHandle: 'top',
      type: 'default',
      style: { stroke: '#888', strokeWidth: 1.5 },
    },
    {
      id: `${id2}-${nodeId}`,
      source: id2, target: nodeId,
      sourceHandle: side === 'right' ? 'left' : 'right',
      targetHandle: side === 'right' ? 'right-in' : 'left-in',
      type: 'default',
      style: { stroke: '#888', strokeWidth: 1.5, strokeDasharray: '8,4' },
    },
  )

  branchedNodes.value.set(nodeId, {
    nodeIds: [id1, id2],
    edgeIds: [`${nodeId}-${id1}`, `${id1}-${id2}`, `${id2}-${nodeId}`],
  })
  relayout()
}

// 移除子路径
const removeBranch = (nodeId: string) => {
  const info = branchedNodes.value.get(nodeId)
  if (!info) return
  const nodeIdSet = new Set<string>(info.nodeIds)
  const edgeIdSet = new Set<string>(info.edgeIds)
  nodes.value = nodes.value.filter((n) => !nodeIdSet.has(n.id))
  edges.value = edges.value.filter((e) => !edgeIdSet.has(e.id))
  branchedNodes.value.delete(nodeId)
  relayout()
}

// 全局重新布局：按主干顺序从上到下排列，分支节点放在主干节点旁
const relayout = () => {
  // 1. 沿主干找顺序
  const branchNodeIds = new Set<string>()
  branchedNodes.value.forEach((info) => {
    info.nodeIds.forEach((id) => branchNodeIds.add(id))
  })
  const trunkOrder: string[] = []
  let cur: string | undefined = '__phantom_top'
  const visited = new Set<string>()
  while (cur && !visited.has(cur)) {
    visited.add(cur)
    trunkOrder.push(cur)
    const next: any = edges.value.find(
      (e) => e.source === cur && !branchNodeIds.has(e.target),
    )
    cur = next?.target
  }

  // 2. 计算每个主干节点的 y
  //    基础：主干按 TRUNK_ROW 推进；如果该 source 有分支，分支链中点对齐 source.y
  //    若分支链与同侧上一个分支链重叠，下移 source.y 直到不重叠
  const ROW = NODE_H + 8
  const TRUNK_ROW = NODE_H + TRUNK_GAP
  const yMap = new Map<string, number>()
  // 同侧分支已用 y 底部
  const sideBottom: Record<'left' | 'right', number> = { left: -Infinity, right: -Infinity }
  let trunkY = 20 - EXTEND // __phantom_top
  trunkOrder.forEach((id) => {
    if (id === '__phantom_top') {
      yMap.set(id, trunkY)
      trunkY += EXTEND
      return
    }
    if (id === '__phantom_bottom') {
      yMap.set(id, trunkY + EXTEND)
      return
    }
    const branch = branchedNodes.value.get(id)
    if (branch) {
      const len = branch.nodeIds.length
      const branchH = len * NODE_H + (len - 1) * 8
      const side = getBranchSide(id)
      // 分支链顶部 = trunkY + (NODE_H - branchH)/2
      // 要求分支链顶 >= sideBottom[side] + PATH_GAP
      const branchTop = trunkY + (NODE_H - branchH) / 2
      const minBranchTop = sideBottom[side] + PATH_GAP
      if (branchTop < minBranchTop) {
        trunkY += minBranchTop - branchTop
      }
      const finalBranchTop = trunkY + (NODE_H - branchH) / 2
      sideBottom[side] = finalBranchTop + branchH
    }
    yMap.set(id, trunkY)
    trunkY += TRUNK_ROW
  })

  // 3. 应用主干 y
  nodes.value = nodes.value.map((n) => {
    if (yMap.has(n.id)) {
      const isPhantom = n.id === '__phantom_top' || n.id === '__phantom_bottom'
      return {
        ...n,
        position: {
          x: isPhantom ? CENTER_X + NODE_W / 2 : CENTER_X,
          y: yMap.get(n.id)!,
        },
      }
    }
    return n
  })

  // 4. 应用分支节点位置 + 修正所有分支 edge 的 handle
  branchedNodes.value.forEach((info, sourceId) => {
    const sourceY = yMap.get(sourceId)
    if (sourceY === undefined) return
    const side = getBranchSide(sourceId)
    const dx = side === 'right' ? NODE_W + COL_GAP : -(NODE_W + COL_GAP)
    const branchX = CENTER_X + dx
    const sourceSide = side === 'right' ? 'right' : 'left'
    const branchSideHandle = side === 'right' ? 'left-in' : 'right-in'

    // 分支节点位置：链中点对齐 sourceY
    const len = info.nodeIds.length
    const branchTotalH = len * NODE_H + (len - 1) * 8
    const startY = sourceY + (NODE_H - branchTotalH) / 2
    info.nodeIds.forEach((bid, i) => {
      const by = startY + i * ROW
      nodes.value = nodes.value.map((n) =>
        n.id === bid ? { ...n, position: { x: branchX, y: by } } : n,
      )
    })

    // 分支 edge handle 修正
    const last = info.nodeIds.length - 1
    edges.value = edges.value.map((e) => {
      if (!info.edgeIds.includes(e.id)) return e
      // 入边：source → nodeIds[0]
      if (e.source === sourceId && e.target === info.nodeIds[0]) {
        return { ...e, sourceHandle: sourceSide, targetHandle: branchSideHandle }
      }
      // 回边：nodeIds[last] → source
      if (e.target === sourceId && e.source === info.nodeIds[last]) {
        return { ...e, sourceHandle: branchSideHandle.replace('-in', ''), targetHandle: sourceSide + '-in' }
      }
      // 链中：nodeIds[i] → nodeIds[i+1]
      return { ...e, sourceHandle: 'bottom', targetHandle: 'top' }
    })
  })
}

/**
 * 在指定节点之前插入新节点（变成它的父节点）
 * - 找到 target=nodeId 的边（必然只有一条父边，主干上的节点）
 * - 删除该边，新增 parent → newNode 和 newNode → nodeId
 */
const insertNodeBefore = (nodeId: string) => {
  const branchSourceId = getNodeBranchSource(nodeId)
  const newId = genTmpId()

  if (branchSourceId) {
    // 在分支节点前插入
    const info = branchedNodes.value.get(branchSourceId)!
    const i = info.nodeIds.indexOf(nodeId)
    const prev = i === 0 ? branchSourceId : info.nodeIds[i - 1]
    // 找到 prev → nodeId 的 edge
    const oldEdge = edges.value.find((e) => e.source === prev && e.target === nodeId)
    if (!oldEdge) return
    nodes.value.push({
      id: newId,
      type: 'topic',
      position: { x: 0, y: 0 },
      data: { label: '新节点' },
    })
    edges.value = edges.value.filter((e) => e.id !== oldEdge.id)
    const e1 = `${prev}-${newId}`
    const e2 = `${newId}-${nodeId}`
    edges.value.push(
      {
        id: e1,
        source: prev, target: newId,
        type: 'default',
        style: { stroke: '#888', strokeWidth: 1.5 },
      },
      {
        id: e2,
        source: newId, target: nodeId,
        type: 'default',
        style: { stroke: '#888', strokeWidth: 1.5 },
      },
    )
    // 更新 BranchInfo：在 i 位置插入新节点；edgeIds 对应位置替换
    const newNodeIds = [...info.nodeIds.slice(0, i), newId, ...info.nodeIds.slice(i)]
    const oldEdgeIdx = info.edgeIds.indexOf(oldEdge.id)
    const newEdgeIds = [
      ...info.edgeIds.slice(0, oldEdgeIdx),
      e1, e2,
      ...info.edgeIds.slice(oldEdgeIdx + 1),
    ]
    branchedNodes.value.set(branchSourceId, { nodeIds: newNodeIds, edgeIds: newEdgeIds })
    relayout()
    return
  }

  // 主干节点前插入
  const allBranchNodeIds = new Set<string>()
  branchedNodes.value.forEach((info) => info.nodeIds.forEach((id) => allBranchNodeIds.add(id)))
  const incoming = edges.value.find(
    (e) => e.target === nodeId && !allBranchNodeIds.has(e.source),
  )
  if (!incoming) return
  const parentId = incoming.source

  nodes.value.push({
    id: newId,
    type: 'topic',
    position: { x: CENTER_X, y: 0 },
    data: { label: '新节点' },
  })

  edges.value = edges.value.filter((e) => e.id !== incoming.id)
  edges.value.push(
    {
      id: `${parentId}-${newId}`,
      source: parentId, target: newId,
      sourceHandle: 'bottom', targetHandle: 'top',
      type: 'straight',
      style: { stroke: '#666', strokeWidth: 3 },
    },
    {
      id: `${newId}-${nodeId}`,
      source: newId, target: nodeId,
      sourceHandle: 'bottom', targetHandle: 'top',
      type: 'straight',
      style: { stroke: '#666', strokeWidth: 3 },
    },
  )
  relayout()
}

const insertNodeAfter = (nodeId: string) => {
  const branchSourceId = getNodeBranchSource(nodeId)
  const newId = genTmpId()

  if (branchSourceId) {
    const info = branchedNodes.value.get(branchSourceId)!
    const i = info.nodeIds.indexOf(nodeId)
    const next = i === info.nodeIds.length - 1 ? branchSourceId : info.nodeIds[i + 1]
    const oldEdge = edges.value.find((e) => e.source === nodeId && e.target === next)
    if (!oldEdge) return
    nodes.value.push({
      id: newId,
      type: 'topic',
      position: { x: 0, y: 0 },
      data: { label: '新节点' },
    })
    edges.value = edges.value.filter((e) => e.id !== oldEdge.id)
    const e1 = `${nodeId}-${newId}`
    const e2 = `${newId}-${next}`
    edges.value.push(
      {
        id: e1,
        source: nodeId, target: newId,
        type: 'default',
        style: { stroke: '#888', strokeWidth: 1.5 },
      },
      {
        id: e2,
        source: newId, target: next,
        type: 'default',
        style: { stroke: '#888', strokeWidth: 1.5 },
      },
    )
    const newNodeIds = [...info.nodeIds.slice(0, i + 1), newId, ...info.nodeIds.slice(i + 1)]
    const oldEdgeIdx = info.edgeIds.indexOf(oldEdge.id)
    const newEdgeIds = [
      ...info.edgeIds.slice(0, oldEdgeIdx),
      e1, e2,
      ...info.edgeIds.slice(oldEdgeIdx + 1),
    ]
    branchedNodes.value.set(branchSourceId, { nodeIds: newNodeIds, edgeIds: newEdgeIds })
    relayout()
    return
  }

  // 主干节点后插入
  const allBranchNodeIds2 = new Set<string>()
  branchedNodes.value.forEach((info) => info.nodeIds.forEach((id) => allBranchNodeIds2.add(id)))
  const outgoing = edges.value.find(
    (e) => e.source === nodeId && !allBranchNodeIds2.has(e.target),
  )
  if (!outgoing) return
  const childId = outgoing.target

  nodes.value.push({
    id: newId,
    type: 'topic',
    position: { x: CENTER_X, y: 0 },
    data: { label: '新节点' },
  })

  edges.value = edges.value.filter((e) => e.id !== outgoing.id)
  edges.value.push(
    {
      id: `${nodeId}-${newId}`,
      source: nodeId, target: newId,
      sourceHandle: 'bottom', targetHandle: 'top',
      type: 'straight',
      style: { stroke: '#666', strokeWidth: 3 },
    },
    {
      id: `${newId}-${childId}`,
      source: newId, target: childId,
      sourceHandle: 'bottom', targetHandle: 'top',
      type: 'straight',
      style: { stroke: '#666', strokeWidth: 3 },
    },
  )
  relayout()
}

// 计算新节点位置的公共方法
const calculateNodePosition = (): { x: number; y: number } => {
  let x: number
  let y: number

  if (nodes.value.length === 1) {
    // 第一个节点：放在根节点下方居中
    const rootNode = nodes.value[0]
    x = rootNode.position.x
    y = rootNode.position.y + 100
  } else {
    // 找到 y 坐标最大的节点（最下面的节点）
    const bottomNode = nodes.value.reduce((lowest, node) => {
      return node.position.y > lowest.position.y ? node : lowest
    })

    // 计算所有节点的 x 坐标中心位置
    const sumX = nodes.value.reduce((sum, node) => sum + node.position.x, 0)
    const centerX = sumX / nodes.value.length

    // 新节点位置：x 为所有节点中心，y 在最下面节点下方 60px
    x = centerX
    y = bottomNode.position.y + 60
  }

  return { x, y }
}

// 添加课程节点
const addCourseNode = (course: Course) => {
  if (!course.rootNodeId) {
    showSnackbar(t('roadmapCreate.messages.courseCannotBeAdded'), 'warning')
    return
  }
  const nodeId = course.rootNodeId.toString()

  // 检查是否已存在
  if (nodes.value.find((n) => n.id === nodeId)) {
    showSnackbar(t('roadmapCreate.messages.courseAlreadyAdded'), 'warning')
    return
  }

  // 计算位置
  const { x, y } = calculateNodePosition()

  nodes.value.push({
    id: nodeId, // 使用 rootNodeId
    type: 'default',
    data: { label: `${t('roadmapDetail.courseLabel')} ${course.name}` }, // 课程前面加文字标识
    position: { x, y },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    style: COURSE_NODE_STYLE,
  })

  // 聚焦到新节点（使用 nextTick 确保 DOM 更新后再聚焦）
  setTimeout(() => {
    setCenter(x, y, { zoom: 1, duration: 300 })
  }, 50)
}

/**
 * 检查节点是否已添加
 */
const isNodeAdded = (nodeId: number | undefined): boolean => {
  if (!nodeId) return false
  return nodes.value.some((n) => n.id === nodeId.toString())
}

/**
 * 跳转到课程详情页
 */
const goToCourseDetail = (courseId: number) => {
  window.open(`/courses/${courseId}`, '_blank')
}

/**
 * 跳转到节点详情页
 */
const goToNodeDetail = (nodeId: number) => {
  window.open(`/read?nodeId=${nodeId}`, '_blank')
}

/**
 * 添加普通节点
 */
const addNode = (node: SearchResultItem) => {
  const nodeId = node.id.toString()

  // 检查是否已存在
  if (nodes.value.find((n) => n.id === nodeId)) {
    showSnackbar(t('roadmapCreate.messages.nodeAlreadyAdded'), 'warning')
    return
  }

  // 计算位置
  const { x, y } = calculateNodePosition()

  nodes.value.push({
    id: nodeId,
    type: 'default',
    data: { label: `${t('roadmapDetail.nodeLabel')} ${node.name}` }, // 节点前面加文字标识
    position: { x, y },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    style: NODE_STYLE, // 使用绿色节点样式
  })

  // 聚焦到新节点
  setTimeout(() => {
    setCenter(x, y, { zoom: 1, duration: 300 })
  }, 50)
}

/**
 * 跳转到我的路线图页面
 */
const goToMyRoadmaps = () => {
  router.push('/users/me?mode=creator&tab=roadmaps')
}

// 删除选中的节点和边
const deleteSelectedNodes = () => {
  const selectedNodes = nodes.value.filter((n) => (n as any).selected && n.id !== '0')
  const selectedEdges = edges.value.filter((e) => (e as any).selected)

  const totalSelected = selectedNodes.length + selectedEdges.length

  if (totalSelected === 0) {
    showSnackbar(t('roadmapCreate.messages.noSelectionToDelete'), 'warning')
    return
  }

  const itemsText: string[] = []
  if (selectedNodes.length > 0)
    itemsText.push(t('roadmapCreate.messages.nodeCount', { count: selectedNodes.length }))
  if (selectedEdges.length > 0)
    itemsText.push(t('roadmapCreate.messages.edgeCount', { count: selectedEdges.length }))

  confirmDialogConfig.value = {
    title: t('roadmapCreate.messages.deleteConfirmTitle'),
    message: t('roadmapCreate.messages.deleteConfirmMsg', {
      items: itemsText.join(t('roadmapCreate.messages.and')),
    }),
    confirmText: t('common.delete'),
    cancelText: t('common.cancel'),
    confirmColor: 'error',
    icon: 'mdi-delete-outline',
    iconColor: 'error-lighten-4',
    iconForeground: 'error',
    onConfirm: () => {
      // 删除选中的节点
      const selectedNodeIds = new Set(selectedNodes.map((n) => n.id))
      nodes.value = nodes.value.filter((n) => !selectedNodeIds.has(n.id))

      // 删除选中的边，以及与被删除节点相关的边
      const selectedEdgeIds = new Set(selectedEdges.map((e) => e.id))
      edges.value = edges.value.filter(
        (e) =>
          !selectedEdgeIds.has(e.id) &&
          !selectedNodeIds.has(e.source) &&
          !selectedNodeIds.has(e.target)
      )

      // 同步清理 branchedNodes 中已被删除的条目
      const newMap = new Map<string, BranchInfo>()
      branchedNodes.value.forEach((info, key) => {
        if (selectedNodeIds.has(key)) return
        if (info.nodeIds.some((id) => selectedNodeIds.has(id))) return
        newMap.set(key, info)
      })
      branchedNodes.value = newMap

      relayout()

      showSnackbar(
        t('roadmapCreate.messages.deleted', {
          items: itemsText.join(t('roadmapCreate.messages.and')),
        }),
        'success'
      )
    },
  }
  confirmDialogVisible.value = true
}

// 处理连接
const onConnect = (connection: Connection) => {
  // 不允许从根节点出发的连接（根节点只有入口，没有出口）
  if (connection.source === '0') return

  // 检查该源节点是否已经有出口连接
  const hasSourceConnection = edges.value.find((e) => e.source === connection.source)
  if (hasSourceConnection) {
    showSnackbar(t('roadmapCreate.messages.singleConnectionOnly'), 'warning')
    return
  }

  // 检查是否已存在相同的连接
  const exists = edges.value.find(
    (e) => e.source === connection.source && e.target === connection.target
  )
  if (exists) return

  edges.value.push({
    id: `${connection.source}-${connection.target}`,
    source: connection.source ?? '',
    target: connection.target ?? '',
    type: 'default',
    animated: true,
    style: EDGE_STYLE,
  })
}

// 处理节点变化（包括选中状态和位置）
const onNodesChange = (changes: any[]) => {
  changes.forEach((change) => {
    if (change.type === 'select') {
      const node = nodes.value.find((n) => n.id === change.id)
      if (node) {
        ;(node as any).selected = change.selected
      }
    } else if (change.type === 'position') {
      // 拖动过程中和拖动结束时都更新位置
      const node = nodes.value.find((n) => n.id === change.id)
      if (node && change.position) {
        node.position = change.position
      }
    }
  })
}

// 处理边变化（包括选中状态）
const onEdgesChange = (changes: any[]) => {
  changes.forEach((change) => {
    if (change.type === 'select') {
      const edge = edges.value.find((e) => e.id === change.id)
      if (edge) {
        ;(edge as any).selected = change.selected
      }
    }
  })
}

// 显示保存对话框
const showSave = () => {
  if (nodes.value.length <= 1) {
    showSnackbar(t('roadmapCreate.messages.addNodesFirst'), 'warning')
    return
  }
  showSaveDialog.value = true
}

// 创建/更新路线图
const { mutate: createRoadmapMutate } = useCreateRoadmapMutation()
const { mutate: updateRoadmapMutate } = useUpdateRoadmapMutation()

// 保存路径
const saveRoadmap = (type: 'draft' | 'publish') => {
  if (!roadmapDescription.value.trim()) {
    showSnackbar(t('roadmapCreate.messages.enterDescription'), 'warning')
    return
  }

  // 验证至少有一个课程节点（除了根节点）
  if (nodes.value.length <= 1) {
    showSnackbar(t('roadmapCreate.messages.addNodesFirst'), 'warning')
    return
  }

  saving.value = true
  saveType.value = type

  try {
    // 序列化边数组：[[source, target], ...]
    const edgeArray = edges.value
      .map((e) => {
        const source = parseInt(e.source)
        const target = parseInt(e.target)
        if (isNaN(source) || isNaN(target)) {
          return null
        }
        return [source, target]
      })
      .filter((edge): edge is [number, number] => edge !== null)

    // 找出所有有连接的节点ID
    const connectedNodeIds = new Set<number>()
    edgeArray.forEach(([source, target]) => {
      connectedNodeIds.add(source)
      connectedNodeIds.add(target)
    })

    // 获取所有节点ID
    const allNodeIds = nodes.value
      .map((n) => {
        const id = parseInt(n.id)
        if (isNaN(id)) {
          return null
        }
        return id
      })
      .filter((id): id is number => id !== null)

    let nodeArray: number[]

    if (type === 'draft') {
      // 草稿模式：保留所有节点（包括孤立节点）
      nodeArray = allNodeIds
    } else {
      // 发布模式：只保留有连接的节点
      nodeArray = allNodeIds.filter((id) => connectedNodeIds.has(id))

      // 检查是否有有效节点
      if (nodeArray.length === 0) {
        showSnackbar(t('roadmapCreate.messages.connectCourses'), 'warning')
        return
      }

      // 验证树结构：边数 = 节点数 - 1
      if (edgeArray.length !== nodeArray.length - 1) {
        showSnackbar(
          t('roadmapCreate.messages.invalidTreeStructure', {
            nodes: nodeArray.length,
            edges: edgeArray.length,
            expectedEdges: nodeArray.length - 1,
          }),
          'error'
        )
        return
      }
    }

    // 后端期望的格式：[边数组, 节点ID数组]
    const content = JSON.stringify([edgeArray, nodeArray])

    // 调用 API
    const state = type === 'draft' ? 0 : 1 // 0-草稿，1-提交审核

    const onSaveSuccess = (result: { id?: number } | null | undefined) => {
      const message =
        type === 'draft'
          ? t('roadmapCreate.messages.draftSaved')
          : t('roadmapCreate.messages.published')
      globalSnackbar?.(message, 'success')
      showSaveDialog.value = false

      if (type === 'draft') {
        savedDraftDescription.value = roadmapDescription.value.trim()
        if (result?.id) {
          draftRoadmapId.value = result.id
        }
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
        { id: draftRoadmapId.value, content, description: roadmapDescription.value.trim(), state },
        { onSuccess: onSaveSuccess, onError: onSaveError }
      )
    } else {
      createRoadmapMutate(
        { roleId: roleId.value, content, description: roadmapDescription.value.trim(), state },
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

// 自动布局
const applyAutoLayout = (showMessage = false) => {
  if (nodes.value.length <= 1) {
    if (showMessage) {
      showSnackbar(t('roadmapCreate.messages.needNodesForLayout'), 'warning')
    }
    return
  }

  // 先找出所有有连接关系的节点ID
  const connectedNodeIds = new Set<string>()
  edges.value.forEach((edge) => {
    connectedNodeIds.add(edge.source.toString())
    connectedNodeIds.add(edge.target.toString())
  })

  // 分离有连接的节点和无连接的节点
  const connectedNodes = nodes.value.filter((node) => connectedNodeIds.has(node.id.toString()))
  const unconnectedNodes = nodes.value.filter((node) => !connectedNodeIds.has(node.id.toString()))

  // 如果有连接的节点，使用 dagre 布局
  if (connectedNodes.length > 0) {
    const dagreGraph = new dagre.graphlib.Graph()
    dagreGraph.setDefaultEdgeLabel(() => ({}))
    dagreGraph.setGraph({
      rankdir: 'LR', // Left to Right - 叶子节点在左边，根节点在右边
      nodesep: 20,
      ranksep: 150,
      marginx: 20,
      marginy: 20,
    })

    const nodeWidth = 120
    const nodeHeight = 40

    // 只添加有连接的节点到 dagre 图
    connectedNodes.forEach((node) => {
      dagreGraph.setNode(node.id.toString(), { width: nodeWidth, height: nodeHeight })
    })

    // 添加边到 dagre 图
    edges.value.forEach((edge) => {
      dagreGraph.setEdge(edge.source.toString(), edge.target.toString())
    })

    // 计算布局
    dagre.layout(dagreGraph)

    // 更新有连接节点的位置
    connectedNodes.forEach((node) => {
      const nodeWithPosition = dagreGraph.node(node.id.toString())
      node.position = {
        x: nodeWithPosition.x - nodeWidth / 2,
        y: nodeWithPosition.y - nodeHeight / 2,
      }
    })
  }

  // 处理没有连接关系的节点
  if (unconnectedNodes.length > 0) {
    // 找到有连接的节点中 x 坐标最大的（最右边的节点）
    let rightX = 0
    let centerY = 400 // 默认中心位置

    if (connectedNodes.length > 0) {
      const rightNode = connectedNodes.reduce((rightmost, node) => {
        return node.position.x > rightmost.position.x ? node : rightmost
      })
      rightX = rightNode.position.x

      // 计算有连接节点的 y 坐标中心位置
      const sumY = connectedNodes.reduce((sum, node) => sum + node.position.y, 0)
      centerY = sumY / connectedNodes.length
    }

    // 网格布局参数
    const rows = 3 // 每列3个节点
    const horizontalSpacing = 200 // 水平间距
    const verticalSpacing = 100 // 垂直间距
    const startX = rightX + 200 // 在最右边节点右侧200px开始

    // 排列无连接的节点
    unconnectedNodes.forEach((node, index) => {
      const col = Math.floor(index / rows)
      const row = index % rows

      // 计算该列的起始 y 坐标，使这一列居中对齐
      const colHeight = Math.min(unconnectedNodes.length - col * rows, rows) * verticalSpacing
      const colStartY = centerY - colHeight / 2 + verticalSpacing / 2

      node.position = {
        x: startX + col * horizontalSpacing,
        y: colStartY + row * verticalSpacing,
      }
    })
  }

  // 重新构建完整的节点数组
  nodes.value = [...connectedNodes, ...unconnectedNodes]

  // 布局完成后，调用 fitView 聚焦到所有节点
  setTimeout(() => {
    fitView({ padding: 0.2, duration: 300 })
  }, 50)

  if (showMessage) {
    showSnackbar(t('roadmapCreate.messages.autoLayoutDone'), 'success')
  }
}

// 重置
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
      nodes.value = buildInitialNodes()
      edges.value = buildInitialEdges()
      branchedNodes.value = new Map()
      roadmapDescription.value = ''
      relayout()
      showSnackbar(t('roadmapCreate.messages.resetSuccess'), 'success')
    },
  }
  confirmDialogVisible.value = true
}

// 编辑模式：加载已有路线图数据
const { data: roadmapData, isLoading: roadmapLoading } = useRoadmapDetailQuery(
  computed(() => roadmapId.value ?? 0),
  { enabled: isEditMode }
)

// 复制模式：加载要复制的路线图数据
const { data: copyRoadmapData, isLoading: copyRoadmapLoading } = useRoadmapDetailQuery(
  computed(() => copyId.value ?? 0),
  { enabled: computed(() => !!copyId.value) }
)

// 综合加载状态
const loading = computed(() => roadmapLoading.value || copyRoadmapLoading.value)

// 监听路线图数据加载完成
watch(roadmapData, (newData) => {
  if (newData && isEditMode.value) {
    console.log('加载的路线图数据:', newData)

    // 设置描述和状态
    roadmapDescription.value = newData.description || ''
    savedDraftDescription.value = newData.description || ''
    draftRoadmapId.value = newData.id
    roadmapState.value = newData.state ?? null // 保存路线图状态

    // 解析 content 并设置节点和边
    try {
      // content 是 JSON 字符串，格式为 {nodes: [], edges: []}
      const contentData =
        typeof newData.content === 'string' ? JSON.parse(newData.content) : newData.content

      console.log('解析后的 content:', contentData)

      if (contentData.nodes && contentData.edges) {
        // 设置边
        edges.value = contentData.edges.map((edge: any) => ({
          id: `${edge.source}-${edge.target}`,
          source: edge.source.toString(),
          target: edge.target.toString(),
          type: 'default',
          animated: true,
          style: EDGE_STYLE,
        }))

        // 设置节点
        nodes.value = contentData.nodes.map((node: any) => {
          if (node.id === '0' || node.id === 0) {
            return {
              id: '0',
              type: 'default',
              data: { label: roleName.value },
              position: { x: 0, y: 0 },
              sourcePosition: undefined,
              targetPosition: Position.Left,
              style: ROOT_NODE_STYLE,
            }
          } else {
            return {
              id: node.id.toString(),
              type: 'default',
              data: { label: node.name || `${t('roadmapDetail.courseLabel')} ${node.id}` },
              position: { x: 0, y: 0 },
              sourcePosition: Position.Right,
              targetPosition: Position.Left,
              style: COURSE_NODE_STYLE,
            }
          }
        })

        console.log('设置的节点:', nodes.value.length, '设置的边:', edges.value.length)

        // 使用自动布局
        setTimeout(() => {
          applyAutoLayout()
        }, 100)
      }
    } catch (parseError) {
      console.error('解析路线图内容失败:', parseError)
      showSnackbar(t('roadmapCreate.messages.loadFailed'), 'error')
    }
  }
})

// 监听复制路线图数据加载完成
watch(copyRoadmapData, (newData) => {
  if (newData && copyId.value) {
    console.log('加载的复制路线图数据:', newData)

    // 设置描述（添加"副本"标识）
    roadmapDescription.value = `${newData.description || t('roadmapCreate.unnamedRoadmap')} ${t('roadmapCreate.copySuffix')}`

    // 解析 content 并设置节点和边
    try {
      const contentData =
        typeof newData.content === 'string' ? JSON.parse(newData.content) : newData.content

      console.log('解析后的 content:', contentData)

      if (contentData.nodes && contentData.edges) {
        // 重新生成节点ID，避免ID冲突
        const idMap = new Map<string, string>()
        const newNodes = contentData.nodes.map((node: any) => {
          const newId =
            node.id === '0' || node.id === 0
              ? '0'
              : `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
          idMap.set(node.id.toString(), newId)

          if (node.id === '0' || node.id === 0) {
            return {
              id: '0',
              type: 'default',
              data: { label: roleName.value },
              position: { x: 0, y: 0 },
              sourcePosition: undefined,
              targetPosition: Position.Left,
              style: ROOT_NODE_STYLE,
            }
          } else {
            return {
              id: newId,
              type: 'default',
              data: { label: node.name || `${t('roadmapDetail.courseLabel')} ${node.id}` },
              position: { x: 0, y: 0 },
              sourcePosition: Position.Right,
              targetPosition: Position.Left,
              style: COURSE_NODE_STYLE,
            }
          }
        })

        // 更新边的ID引用
        const newEdges = contentData.edges.map((edge: any) => ({
          id: `${idMap.get(edge.source.toString())}-${idMap.get(edge.target.toString())}`,
          source: idMap.get(edge.source.toString()) || edge.source.toString(),
          target: idMap.get(edge.target.toString()) || edge.target.toString(),
          type: 'default',
          animated: true,
          style: EDGE_STYLE,
        }))

        nodes.value = newNodes
        edges.value = newEdges

        console.log('设置的节点:', nodes.value.length, '设置的边:', edges.value.length)

        // 使用自动布局
        setTimeout(() => {
          applyAutoLayout()
        }, 100)
      }
    } catch (parseError) {
      console.error('解析复制路线图内容失败:', parseError)
      showSnackbar(t('roadmapCreate.messages.loadFailed'), 'error')
    }
  }
})

// 如果是复制模式，数据加载由 useRoadmapDetailQuery 处理
</script>

<style scoped>
.roadmap-create-page {
  /* 使用 DefaultLayout 的默认 padding */
}

/* 草稿描述区域样式 */
.draft-description-section {
  padding: 0;
}

/* 草稿描述文本样式 - 最多显示5行 */
.draft-description-text {
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  line-height: 1.5;
}

/* 对话框关闭按钮 */
.dialog-close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 1;
}

/* 宽屏时向左延伸，让后退按钮露出到页面外 */
@media (min-width: 1800px) {
  .title-row {
    margin-left: -56px;
  }
}

/* 内容布局 */
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

/* 右侧课程列表 */
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

/* 右侧面板卡片样式 */
.course-search-card {
  background-color: rgb(var(--v-theme-surface));
  transition: box-shadow 0.2s ease;
}

/* 搜索框样式 */
.search-field :deep(.v-field) {
  border-radius: 12px;
  background-color: transparent !important;
}

/* 空状态图标动画 */
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

/* 课程列表样式 */
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

/* 操作指南样式 */
.tips-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgb(var(--v-theme-grey-lighten-4));
}

.tips-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  opacity: 0.7;
}

.tips-list-simple {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tip-simple {
  font-size: 12px;
  line-height: 1.5;
  color: rgb(var(--v-theme-grey-darken-1));
  padding-left: 12px;
  position: relative;
}

.tip-simple::before {
  content: '·';
  position: absolute;
  left: 0;
  font-size: 16px;
  line-height: 1.2;
  color: rgb(var(--v-theme-grey));
}

.flow-editor {
  height: 500px;
  min-height: 400px;
  background: rgb(var(--v-theme-surface));
  position: relative;
}

/* 节点样式（参考 RoadmapDemo） */
:deep(.vue-flow__node) {
  overflow: visible;
}

:deep(.node-wrapper) {
  position: relative;
  display: inline-block;
}

:deep(.node-actions) {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: row;
  gap: 4px;
  z-index: 10;
}

:deep(.node-action-btn) {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #1a1a1a;
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: background 0.15s;
}

:deep(.node-action-btn:hover) {
  background: #424242;
}

:deep(.node-root),
:deep(.node-end) {
  background: #1a1a1a;
  color: #fff;
  border-radius: 8px;
  padding: 8px 0;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
  width: 160px;
  text-align: center;
  letter-spacing: 0.5px;
}

:deep(.node-topic) {
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 13px;
  white-space: nowrap;
  width: 160px;
  text-align: center;
  position: relative;
}

:deep(.node-topic--group) {
  background: #fff;
  color: #1a1a1a;
  border: 1.5px solid #1a1a1a;
  font-weight: 600;
}

/* 课程节点：偏粉红 */
:deep(.node-topic--course) {
  background: #fee2e8;
  color: #1a1a1a;
  border: 1.5px solid #1a1a1a;
  font-weight: 500;
}

/* node 节点：偏橙粉 */
:deep(.node-topic--node) {
  background: #feeadf;
  color: #1a1a1a;
  border: 1.5px solid #1a1a1a;
  font-weight: 400;
}

/* 节点选中高亮（绑定课程/node 时） */
:deep(.node-topic--selected) {
  outline: 3px solid #ff9800;
  outline-offset: 2px;
  box-shadow: 0 0 0 6px rgba(255, 152, 0, 0.15);
}

:deep(.vue-flow__handle) {
  opacity: 0;
  pointer-events: none;
  width: 0;
  height: 0;
  min-width: 0;
  min-height: 0;
  border: none;
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

/* Vue Flow 节点样式 */
:deep(.vue-flow__node) {
  cursor: move;
}

:deep(.vue-flow__node.selected) {
  box-shadow: 0 0 0 2px rgb(var(--v-theme-primary));
}

/* Vue Flow 边（连接线）样式 */
:deep(.vue-flow__edge) {
  cursor: pointer;
}

:deep(.vue-flow__edge.selected) {
  z-index: 1000;
}

:deep(.vue-flow__edge.selected .vue-flow__edge-path) {
  stroke: rgb(var(--v-theme-primary)) !important;
  stroke-width: 3px !important;
}

/* 工具栏分隔符 */
.toolbar-divider {
  height: 24px;
  align-self: center;
}

/* 隐藏根节点的 source handle */
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle.source),
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle-right),
:deep(.vue-flow__node[data-id='0'] .vue-flow__handle-bottom) {
  display: none !important;
}
</style>
