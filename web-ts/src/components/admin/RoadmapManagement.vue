<template>
  <div class="roadmap-management">
    <!-- 页面头部 -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-blue-lighten-5 mr-3">
          <v-icon icon="mdi-map-marker-path" color="blue-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">路线图管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">管理和审核学习路线图</p>
        </div>
      </div>
      <v-chip variant="flat" color="blue-lighten-4" rounded="lg">
        <v-icon icon="mdi-map-marker-path" color="blue-darken-2" size="16" class="mr-2"></v-icon>
        <span class="text-blue-darken-2 text-caption">{{ roadmapList.length }}个路线图</span>
      </v-chip>
    </div>

    <!-- 筛选区域 -->
    <v-card flat class="pa-4 bg-grey-lighten-5 rounded-lg mb-6">
      <h4 class="text-subtitle-2 text-grey-darken-2 mb-3 d-flex align-center">
        <v-icon icon="mdi-filter-outline" size="16" class="mr-2"></v-icon>
        高级筛选
      </h4>
      <v-row dense>
        <v-col cols="12" sm="4">
          <v-text-field
            v-model.number="professionIdFilter"
            label="职业ID"
            type="number"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="4">
          <v-text-field
            v-model.number="creatorIdFilter"
            label="创建者ID"
            type="number"
            variant="outlined"
            density="compact"
            rounded="lg"
            bg-color="white"
            hide-details
            clearable
          ></v-text-field>
        </v-col>
        <v-col cols="12" sm="4">
          <div class="d-flex gap-2">
            <v-btn
              variant="flat"
              color="primary"
              rounded="lg"
              @click="onFilterChange"
            >
              <v-icon icon="mdi-magnify" class="mr-1"></v-icon>
              筛选
            </v-btn>
            <v-btn
              variant="outlined"
              color="grey"
              rounded="lg"
              @click="onResetFilter"
            >
              <v-icon icon="mdi-refresh" class="mr-1"></v-icon>
              重置
            </v-btn>
          </div>
        </v-col>
      </v-row>
    </v-card>

    <!-- 状态标签 -->
    <v-tabs
      v-model="selectedStateIndex"
      color="primary"
      class="mb-6"
      show-arrows
      @update:model-value="onStateChange"
    >
      <v-tab
        v-for="(state, index) in stateOptions"
        :key="state.value"
        :value="index"
        class="text-none"
      >
        <v-icon
          :icon="state.icon"
          :color="state.value === ContentState.SUBMITTED ? 'orange-darken-1' : state.value === ContentState.PUBLISHED ? 'green-darken-1' : state.value === ContentState.REJECTED ? 'red-darken-1' : 'grey-darken-1'"
          size="18"
          class="mr-2"
        ></v-icon>
        {{ state.text }}
      </v-tab>
    </v-tabs>

    <!-- 加载状态 -->
    <div v-if="loading && roadmapList.length === 0" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
      <p class="mt-3 text-grey-darken-1">加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="roadmapList.length === 0" class="text-center py-12">
      <v-icon icon="mdi-map-marker-path" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">暂无路线图</p>
    </div>

    <!-- 路线图列表 -->
    <div v-else>
      <v-card
        v-for="roadmap in roadmapList"
        :key="roadmap.id"
        flat
        class="border rounded-lg pa-5 mb-4"
        hover
      >
        <div class="d-flex align-start">
          <!-- 状态和操作区域 -->
          <div class="mr-4 status-actions-area">
            <div class="mb-3">
              <v-chip
                :color="getStateConfig(roadmap.state).color"
                variant="flat"
                rounded="lg"
                size="small"
              >
                <v-icon :icon="getStateConfig(roadmap.state).icon" size="14" class="mr-1"></v-icon>
                {{ getStateConfig(roadmap.state).text }}
              </v-chip>
            </div>

            <!-- 审核操作按钮 -->
            <div class="d-flex flex-column ga-2">
              <!-- 待审核状态：清除描述并通过、直接通过 -->
              <template v-if="roadmap.state === ContentState.SUBMITTED">
                <v-btn
                  variant="flat"
                  color="green-lighten-4"
                  rounded="lg"
                  size="small"
                  :loading="roadmap.approving"
                  @click="approveRoadmap(roadmap, 'approve_clear')"
                >
                  <v-icon icon="mdi-check-bold" color="green-darken-2" size="16" class="mr-1"></v-icon>
                  清除并通过
                </v-btn>
                <v-btn
                  variant="flat"
                  color="green-lighten-4"
                  rounded="lg"
                  size="small"
                  :loading="roadmap.approving"
                  @click="approveRoadmap(roadmap, 'approve')"
                >
                  <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                  直接通过
                </v-btn>
              </template>

              <!-- 已通过状态：修改描述 -->
              <v-btn
                v-if="roadmap.state === ContentState.PUBLISHED"
                variant="flat"
                color="blue-lighten-4"
                rounded="lg"
                size="small"
                @click="showEditModal(roadmap)"
              >
                <v-icon icon="mdi-pencil" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                修改描述
              </v-btn>

              <!-- 已拒绝状态：恢复 -->
              <v-btn
                v-if="roadmap.state === ContentState.REJECTED"
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                :loading="roadmap.restoring"
                @click="approveRoadmap(roadmap, 'approve')"
              >
                <v-icon icon="mdi-check" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                重新通过
              </v-btn>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-3">
              <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                <v-icon icon="mdi-map-marker-path" color="grey-darken-1" size="18"></v-icon>
              </v-avatar>
              <div>
                <div class="text-body-2 font-weight-medium text-grey-darken-2">
                  路线图 ID: {{ roadmap.id }}
                </div>
                <div class="text-caption text-grey-darken-1">
                  {{ roadmap.createdAt || '未知时间' }}
                </div>
              </div>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <!-- 职业信息 -->
              <div v-if="roadmap.profession" class="mb-3">
                <v-chip variant="tonal" color="purple-lighten-1" size="small" rounded="lg">
                  <v-icon icon="mdi-briefcase" size="14" class="mr-1"></v-icon>
                  {{ roadmap.profession.name }}
                </v-chip>
              </div>

              <!-- 路线图描述 -->
              <div class="mb-3">
                <div class="text-caption text-grey-darken-1 mb-1">路线图描述：</div>
                <div class="text-body-2 text-grey-darken-3">
                  {{ roadmap.description || '暂无描述' }}
                </div>
              </div>

              <!-- 创建者信息 -->
              <div class="d-flex align-center text-caption text-grey-darken-1">
                <v-icon icon="mdi-account-outline" size="14" class="mr-1"></v-icon>
                创建者: {{ roadmap.creator?.name || '未知' }}
                <span class="mx-3">|</span>
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                更新时间: {{ roadmap.updatedAt || '未知' }}
              </div>
            </div>
          </div>
        </div>
      </v-card>

      <!-- 加载更多状态 -->
      <div v-if="loading && roadmapList.length > 0" class="text-center py-4">
        <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
        <p class="mt-2 text-body-2 text-grey-darken-1">加载更多中...</p>
      </div>

      <!-- 没有更多数据提示 -->
      <div v-else-if="!hasMoreData && roadmapList.length > 0" class="text-center py-4">
        <p class="text-body-2 text-grey-darken-1">已加载全部数据</p>
      </div>
    </div>

    <!-- 编辑描述对话框 -->
    <v-dialog v-model="showEditDialog" max-width="700px" persistent>
      <v-card rounded="lg">
        <v-card-title class="text-h6 font-weight-bold pa-6 pb-4">
          <v-icon icon="mdi-pencil-outline" color="blue-darken-2" class="mr-3"></v-icon>
          编辑路线图描述
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <v-form ref="editForm" v-model="editFormValid">
            <v-textarea
              v-model="editDescription"
              label="路线图描述"
              :counter="500"
              variant="outlined"
              rounded="lg"
              bg-color="grey-lighten-5"
              rows="6"
              clearable
              placeholder="请输入路线图描述..."
            ></v-textarea>
          </v-form>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn variant="outlined" color="grey" rounded="lg" @click="closeEditDialog">
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="blue-lighten-4"
            rounded="lg"
            :loading="updating"
            @click="updateRoadmapDescription"
          >
            <v-icon icon="mdi-content-save" color="blue-darken-2" class="mr-2"></v-icon>
            保存修改
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { roadmapServiceV1 } from '@/services/api/v1/apiServiceV1'
import { ContentState } from '@/types/enums'
import type { Roadmap } from '@/types/roadmap'
import type { StateOption } from '@/types/common'

const roadmapList = ref<Roadmap[]>([])
const loading = ref<boolean>(false)
const lastId = ref<number>(0)
const hasMoreData = ref<boolean>(true)

const professionIdFilter = ref<number | null>(null)
const creatorIdFilter = ref<number | null>(null)
const selectedStateIndex = ref<number>(0)

const showEditDialog = ref<boolean>(false)
const editFormValid = ref<boolean>(false)
const currentRoadmap = ref<Roadmap | null>(null)
const editDescription = ref<string>('')
const updating = ref<boolean>(false)
const editForm = ref(null)

// 状态选项
const stateOptions: StateOption[] = [
  {
    value: ContentState.SUBMITTED,
    text: '待审核',
    color: 'orange-lighten-4',
    icon: 'mdi-clock-outline',
  },
  {
    value: ContentState.PUBLISHED,
    text: '已通过',
    color: 'green-lighten-4',
    icon: 'mdi-check-circle',
  },
  {
    value: ContentState.REJECTED,
    text: '已拒绝',
    color: 'red-lighten-4',
    icon: 'mdi-close-circle',
  },
  {
    value: ContentState.BANNED,
    text: '已封禁',
    color: 'grey-lighten-2',
    icon: 'mdi-cancel',
  },
]

// 获取当前选中的状态
const getCurrentState = (): number => stateOptions[selectedStateIndex.value]?.value || ContentState.SUBMITTED

// 根据状态获取配置
const getStateConfig = (state?: number): StateOption => {
  return stateOptions.find((option) => option.value === state) || stateOptions[0]
}

// 状态改变
const onStateChange = (): void => {
  loadRoadmapList(true)
}

// 筛选条件变化
const onFilterChange = (): void => {
  loadRoadmapList(true)
}

// 重置筛选
const onResetFilter = (): void => {
  professionIdFilter.value = null
  creatorIdFilter.value = null
  loadRoadmapList(true)
}

// 加载路线图列表
const loadRoadmapList = async (reset: boolean = true): Promise<void> => {
  if (loading.value) return

  try {
    loading.value = true

    const currentLastId = reset ? null : lastId.value
    const currentState = getCurrentState()
    const professionId = professionIdFilter.value || undefined
    const creatorId = creatorIdFilter.value || undefined

    const response = await roadmapServiceV1.getAdminRoadmaps(
      currentState,
      professionId,
      creatorId,
      currentLastId
    )

    if (response.code === 200 && response.data) {
      const newData = Array.isArray(response.data) ? response.data : []

      if (reset) {
        roadmapList.value = newData
        lastId.value = 0
      } else {
        roadmapList.value = [...roadmapList.value, ...newData]
      }

      if (newData.length > 0) {
        lastId.value = newData[newData.length - 1].id
        hasMoreData.value = newData.length >= 20
      } else {
        hasMoreData.value = false
      }
    } else {
      console.error('加载路线图列表失败:', response.message)
      if (reset) {
        roadmapList.value = []
      }
    }
  } catch (error) {
    console.error('加载路线图列表失败:', error)
    if (reset) {
      roadmapList.value = []
    }
  } finally {
    loading.value = false
  }
}

// 加载更多数据
const loadMoreData = (): void => {
  if (!loading.value && hasMoreData.value) {
    loadRoadmapList(false)
  }
}

// 滚动监听
let isScrollLoading = false
const handleScroll = (): void => {
  if (isScrollLoading || loading.value || !hasMoreData.value) {
    return
  }

  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight

  if (scrollTop + windowHeight >= documentHeight - 100) {
    isScrollLoading = true
    loadMoreData()
    setTimeout(() => {
      isScrollLoading = false
    }, 500)
  }
}

// 显示编辑对话框
const showEditModal = (roadmap: Roadmap): void => {
  currentRoadmap.value = roadmap
  editDescription.value = roadmap.description || ''
  showEditDialog.value = true
}

// 关闭编辑对话框
const closeEditDialog = (): void => {
  showEditDialog.value = false
  currentRoadmap.value = null
  editDescription.value = ''
  editFormValid.value = false
}

// 更新路线图描述
const updateRoadmapDescription = async (): Promise<void> => {
  if (!currentRoadmap.value) return

  try {
    updating.value = true

    const response = await roadmapServiceV1.updateRoadmapDescription(
      currentRoadmap.value.id,
      editDescription.value || ''
    )

    if (response.code === 200 && response.data) {
      const index = roadmapList.value.findIndex((r) => r.id === currentRoadmap.value!.id)
      if (index !== -1) {
        roadmapList.value[index] = response.data
      }

      closeEditDialog()
    } else {
      console.error('更新路线图描述失败:', response.message)
    }
  } catch (error) {
    console.error('更新路线图描述错误:', error)
  } finally {
    updating.value = false
  }
}

// 批准路线图
const approveRoadmap = async (roadmap: Roadmap, action: string): Promise<void> => {
  try {
    ;(roadmap as any).approving = true

    const response = await roadmapServiceV1.approveRoadmap(
      roadmap.id,
      action
    )

    if (response.code === 200 && response.data) {
      const index = roadmapList.value.findIndex((r) => r.id === roadmap.id)
      if (index !== -1) {
        const currentState = getCurrentState()
        if (currentState !== response.data.state) {
          roadmapList.value.splice(index, 1)
        } else {
          roadmapList.value[index] = response.data
        }
      }
    } else {
      console.error('批准路线图失败:', response.message)
    }
  } catch (error) {
    console.error('批准路线图错误:', error)
  } finally {
    ;(roadmap as any).approving = false
    ;(roadmap as any).restoring = false
  }
}

onMounted(() => {
  loadRoadmapList(true)
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.roadmap-management {
  padding: 0;
}

.status-actions-area {
  min-width: 130px;
}

:deep(.v-card[hover]:hover) {
  transform: translateY(-2px);
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}
</style>
