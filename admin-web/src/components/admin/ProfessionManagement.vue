<script setup lang="ts">
import { inject, ref, onMounted, nextTick, computed } from 'vue'
import { adminApi, systemApi, professionApi } from '@/api'
import { ContentState } from '@/enums'
import type { Profession, ProfessionCategory, CategoryMapping } from '@/types/profession.d'
import type { StateOption } from '@/types/common'
import CategorySelector from '../common/CategorySelector.vue'
import RejectBanDialog from './RejectBanDialog.vue'
import { professionNameRules, professionDescriptionRules } from '@/utils/validationRules'
import { PROFESSION_VALIDATION } from '@/types/validation'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 扩展 Profession 接口以包含UI状态
interface ProfessionWithUIState extends Profession {
  approving?: boolean
  restoring?: boolean
  deleting?: boolean
}

// 编辑表单数据
interface EditProfessionForm {
  id?: number
  name?: string
  description?: string
  price?: string
  skillsText?: string
  mainCategory?: number | null
  subCategory?: number | null
  icon?: string
  reason?: string
  state?: number
}

const selectedStateIndex = ref<number>(0)

// 筛选条件
const filterProfessionId = ref<number | null>(null)
const isFilterMode = ref<boolean>(false)
const filterResult = ref<ProfessionWithUIState | null>(null)

// 显示的职业列表
const displayList = computed<ProfessionWithUIState[]>(() => {
  if (isFilterMode.value && filterResult.value) {
    return [filterResult.value]
  }
  return professionList.value
})

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const currentProfession = ref<ProfessionWithUIState | null>(null)
const dialogType = ref<'reject' | 'ban'>('reject')

// 编辑相关数据
const showEditDialog = ref<boolean>(false)
const editProfession = ref<EditProfessionForm>({})
const editFormValid = ref<boolean>(false)
const editForm = ref(null)

// 动态类别数据
const mainCategories = ref<ProfessionCategory[]>([])
const categoryMapping = ref<CategoryMapping[]>([])

// 动态类别功能函数
const getCategoryName = (mainCategoryId?: number): string => {
  if (!mainCategories.value || !mainCategoryId) return '未知类别'
  const category = mainCategories.value.find((cat) => cat.id === mainCategoryId)
  return category ? category.title : '未知类别'
}

const getSubCategoryName = (mainCategoryId?: number, subCategoryId?: number): string => {
  if (!categoryMapping.value || !mainCategoryId || !subCategoryId) return '未知子类别'
  const mapping = categoryMapping.value.find((m) => m.mainCategoryId === mainCategoryId)
  if (!mapping) return '未知子类别'
  const subCategory = mapping.subcategories.find((sub) => sub.id === subCategoryId)
  return subCategory ? subCategory.name : '未知子类别'
}

// 加载职业类别数据
const loadProfessionCategories = async (): Promise<void> => {
  try {
    const response = await systemApi.getProfessionCategories()
    if (response.code === 200 && response.data) {
      mainCategories.value = response.data.mainCategories || []
      categoryMapping.value = response.data.categoryMapping || []
    }
  } catch (error) {
    console.error('加载职业类别失败:', error)
  }
}

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
const getCurrentState = (): number =>
  stateOptions[selectedStateIndex.value]?.value || ContentState.SUBMITTED

// 根据状态获取配置
const getStateConfig = (state?: number): StateOption => {
  return stateOptions.find((option) => option.value === state) || stateOptions[0]
}

// 处理技能数组
const getSkillsArray = (skills?: string): string[] => {
  if (!skills) return []
  if (typeof skills === 'string') {
    return skills
      .split(',')
      .map((s) => s.trim())
      .filter((s) => s)
  }
  return Array.isArray(skills) ? skills : []
}

// 使用 useInfiniteScroll 加载职业列表
const {
  items: professionList,
  loading,
  hasMore: hasMoreData,
  loadMore: loadMoreData,
  reset: resetProfessionList,
} = useInfiniteScroll({
  fetchFn: async (params) => {
    const state = getCurrentState()
    const response = await adminApi.getProfessionsByFilter(state, params.lastId)
    const pageData = response.data
    return {
      code: response.code,
      data: pageData?.items || [],
      message: response.message || '',
      hasMore: pageData?.hasMore ?? false,
    }
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: {
    lastId: null,
  },
  immediate: true,
})

// 状态改变处理
const onStateChange = (): void => {
  resetProfessionList()
  loadMoreData()
}

// 按ID筛选职业
const searchById = async (): Promise<void> => {
  if (!filterProfessionId.value) {
    showSnackbar?.('请输入职业ID', 'warning')
    return
  }
  try {
    const response = await adminApi.getProfessionById(filterProfessionId.value)
    if (response.data) {
      filterResult.value = response.data
      isFilterMode.value = true
    } else {
      showSnackbar?.('未找到该职业', 'warning')
    }
  } catch {
    showSnackbar?.('查询失败', 'error')
  }
}

// 清除筛选
const clearFilter = (): void => {
  filterProfessionId.value = null
  filterResult.value = null
  isFilterMode.value = false
}

// 使用 useMutation 操作职业申请
const { execute: executeOperateProfession } = useMutation(
  (data: { professionId: number; action: string; reason?: string }) =>
    professionApi.approveProfession(data.professionId, data.action, data.reason || ''),
  {
    onSuccess: (_, data) => {
      const index = professionList.value.findIndex((p) => p.id === data.professionId)
      if (index !== -1) {
        if (data.action === 'APPROVE') {
          professionList.value[index].state = ContentState.PUBLISHED
          professionList.value[index].reason = ''
        } else if (data.action === 'REJECT') {
          professionList.value[index].state = ContentState.REJECTED
          professionList.value[index].reason = data.reason || ''
        } else if (data.action === 'BAN') {
          professionList.value[index].state = ContentState.BANNED
          professionList.value[index].reason = data.reason || ''
        }

        // 如果当前筛选状态与操作结果不匹配，从列表中移除
        const currentState = getCurrentState()
        const shouldRemove =
          (data.action === 'APPROVE' && currentState !== ContentState.PUBLISHED) ||
          (data.action === 'REJECT' && currentState !== ContentState.REJECTED) ||
          (data.action === 'BAN' && currentState !== ContentState.BANNED)

        if (shouldRemove) {
          professionList.value.splice(index, 1)
        }
      }
    },
  }
)

const operateProfession = async (
  profession: ProfessionWithUIState,
  action: string,
  reason = ''
): Promise<boolean> => {
  try {
    await executeOperateProfession({ professionId: profession.id, action, reason })
    return true
  } catch (error) {
    return false
  }
}

// 通过申请
const approveProfession = async (profession: ProfessionWithUIState): Promise<void> => {
  profession.approving = true
  const success = await operateProfession(profession, 'APPROVE')
  profession.approving = false

  if (success) {
    showSnackbar?.('操作成功')
  }
}

// 恢复申请 - 将已拒绝的职业重新通过
const restoreProfession = async (profession: ProfessionWithUIState): Promise<void> => {
  profession.restoring = true
  const success = await operateProfession(profession, 'APPROVE')
  profession.restoring = false

  if (success) {
    showSnackbar?.('操作成功')
  }
}

// 显示拒绝对话框
const showRejectModal = (profession: ProfessionWithUIState): void => {
  currentProfession.value = profession
  dialogType.value = 'reject'
  showReasonDialog.value = true
}

// 显示屏蔽对话框
const showBanModal = (profession: ProfessionWithUIState): void => {
  currentProfession.value = profession
  dialogType.value = 'ban'
  showReasonDialog.value = true
}

// 使用 useMutation 处理拒绝/屏蔽
const { execute: executeRejectOrBan, loading: submitting } = useMutation(
  (data: { professionId: number; action: string; reason: string }) =>
    professionApi.approveProfession(data.professionId, data.action, data.reason),
  {
    successMessage: '操作成功',
    onSuccess: (_, data) => {
      const index = professionList.value.findIndex((p) => p.id === data.professionId)
      if (index !== -1) {
        const targetState = data.action === 'REJECT' ? ContentState.REJECTED : ContentState.BANNED
        professionList.value[index].state = targetState
        professionList.value[index].reason = data.reason

        const currentState = getCurrentState()
        if (currentState !== targetState) {
          professionList.value.splice(index, 1)
        }
      }

      showReasonDialog.value = false
      currentProfession.value = null
    },
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string): Promise<void> => {
  if (!currentProfession.value) return

  const action = dialogType.value === 'reject' ? 'REJECT' : 'BAN'
  await executeRejectOrBan({
    professionId: currentProfession.value.id,
    action,
    reason,
  })
}

// 使用 useMutation 取消屏蔽职业
const { execute: executeUnbanProfession } = useMutation(
  (professionId: number) => professionApi.approveProfession(professionId, 'APPROVE'),
  {
    successMessage: '操作成功',
    onSuccess: (_, professionId) => {
      const index = professionList.value.findIndex((p) => p.id === professionId)
      if (index !== -1) {
        professionList.value[index].state = ContentState.PUBLISHED
        professionList.value[index].reason = ''

        const currentState = getCurrentState()
        if (currentState !== ContentState.PUBLISHED) {
          professionList.value.splice(index, 1)
        }
      }
    },
  }
)

const unbanProfession = async (profession: ProfessionWithUIState): Promise<void> => {
  await executeUnbanProfession(profession.id)
}

// 显示编辑对话框
const showEditModal = (profession: ProfessionWithUIState): void => {
  currentProfession.value = profession
  editProfession.value = {
    id: profession.id,
    name: profession.name || '',
    description: profession.description || '',
    price: profession.price || '',
    skillsText: profession.skills || '',
    mainCategory: profession.mainCategory || null,
    subCategory: profession.subCategory || null,
    icon: profession.icon || '',
    reason: profession.reason || '',
    state: profession.state,
  }

  showEditDialog.value = true

  nextTick(() => {
    ;(editForm.value as any)?.validate()
  })
}

// 关闭编辑对话框
const closeEditDialog = (): void => {
  showEditDialog.value = false
  currentProfession.value = null
  editProfession.value = {}
  editFormValid.value = false
}

// 使用 useMutation 更新职业信息
const { execute: executeUpdateProfession, loading: updating } = useMutation(
  (data: { id: number; updateData: any }) =>
    professionApi.updateProfession(data.id, data.updateData),
  {
    successMessage: '操作成功',
    onSuccess: (_, data) => {
      const index = professionList.value.findIndex((p) => p.id === data.id)
      if (index !== -1) {
        professionList.value[index] = {
          ...professionList.value[index],
          ...data.updateData,
        }
      }
      closeEditDialog()
    },
  }
)

// 更新职业信息
const updateProfession = async (): Promise<void> => {
  if (!editFormValid.value) return

  const updateData = {
    name: editProfession.value.name!,
    description: editProfession.value.description!,
    price: editProfession.value.price || '',
    skills: editProfession.value.skillsText || '',
    mainCategory: editProfession.value.mainCategory || null,
    subCategory: editProfession.value.subCategory || null,
    icon: editProfession.value.icon || '',
    reason: editProfession.value.reason || '',
  }

  await executeUpdateProfession({
    id: editProfession.value.id!,
    updateData,
  })
}

// 组件挂载时加载数据
onMounted(() => {
  loadProfessionCategories()
})
</script>

<template>
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">职业管理</h2>

    <!-- ID查询 -->
    <v-card flat class="border mb-4">
      <v-card-text>
        <v-row align="center">
          <v-col cols="3">
            <v-text-field
              v-model.number="filterProfessionId"
              type="number"
              label="职业 ID"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              @keyup.enter="searchById"
            ></v-text-field>
          </v-col>
          <v-col cols="auto">
            <v-btn variant="tonal" size="default" @click="searchById">
              <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
              查询
            </v-btn>
            <v-btn
              v-if="isFilterMode"
              variant="text"
              size="default"
              @click="clearFilter"
            >
              清除
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- 职业列表 -->
    <v-card flat class="border">
      <v-card-text>
        <!-- 状态标签 -->
        <v-tabs
          v-if="!isFilterMode"
          v-model="selectedStateIndex"
          color="primary"
          density="compact"
          @update:model-value="onStateChange"
          class="mb-4"
        >
          <v-tab
            v-for="(state, index) in stateOptions"
            :key="state.value"
            :value="index"
            class="text-none"
            size="small"
          >
            <v-icon :icon="state.icon" size="14" class="mr-1"></v-icon>
            {{ state.text }}
          </v-tab>
        </v-tabs>

        <!-- 首次加载状态 -->
        <div v-if="loading && displayList.length === 0" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!loading && displayList.length === 0" class="text-center py-12">
          <v-icon icon="mdi-briefcase-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
          <p class="text-body-1 text-grey-darken-1">
            {{ isFilterMode ? '未找到该职业' : `暂无${stateOptions[selectedStateIndex]?.text}的职业` }}
          </p>
        </div>

        <!-- 列表 -->
        <div v-else>
          <div
            v-for="profession in displayList"
            :key="profession.id"
            v-intersect="{
              handler: (isIntersecting: boolean) => {
                if (!isFilterMode && isIntersecting && profession === displayList[displayList.length - 1] && hasMoreData && !loading) {
                  loadMoreData()
                }
              },
            }"
            class="list-item mb-3"
          >
            <div class="d-flex align-start">
              <!-- 操作区 -->
              <div class="action-area mr-4">
                <!-- 待审核 -->
                <div v-if="profession.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block :loading="profession.approving" @click="approveProfession(profession)">
                    批准
                  </v-btn>
                  <v-btn variant="tonal" color="error" size="small" block @click="showRejectModal(profession)">
                    拒绝
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(profession)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已通过 -->
                <div v-if="profession.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="warning" size="small" block @click="showRejectModal(profession)">
                    撤回
                  </v-btn>
                  <v-btn variant="tonal" color="info" size="small" block @click="showEditModal(profession)">
                    编辑
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(profession)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已拒绝 -->
                <div v-if="profession.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block :loading="profession.restoring" @click="restoreProfession(profession)">
                    通过
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(profession)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已屏蔽 -->
                <div v-if="profession.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="info" size="small" block @click="unbanProfession(profession)">
                    解封
                  </v-btn>
                  <v-btn variant="tonal" color="warning" size="small" block @click="showRejectModal(profession)">
                    降级
                  </v-btn>
                </div>

                <!-- 编辑按钮（非已通过状态） -->
                <div v-if="profession.state !== ContentState.PUBLISHED" class="mt-2">
                  <v-btn variant="tonal" color="info" size="small" block @click="showEditModal(profession)">
                    编辑
                  </v-btn>
                </div>
              </div>

              <!-- 内容区 -->
              <div class="flex-grow-1">
                <!-- 标题行 -->
                <div class="d-flex align-center justify-space-between mb-2">
                  <div class="d-flex align-center">
                    <v-icon v-if="profession.icon" :icon="profession.icon" size="18" class="mr-2 text-grey-darken-2"></v-icon>
                    <div class="text-body-1 font-weight-medium text-grey-darken-3">
                      {{ profession.name || '职业名称' }}
                    </div>
                    <v-chip variant="flat" :color="getStateConfig(profession.state).color" size="x-small" class="ml-2">
                      {{ getStateConfig(profession.state).text }}
                    </v-chip>
                    <v-chip v-if="profession.price" variant="flat" color="green-lighten-4" size="x-small" class="ml-1">
                      $ {{ profession.price }}
                    </v-chip>
                  </div>
                  <div class="d-flex align-center text-caption text-grey-darken-1">
                    <a v-if="profession.creator" :href="`/user/${profession.creator.id}`" target="_blank" class="text-grey-darken-1">{{ profession.creator.name }}</a>
                    <span v-else>未知</span>
                    <span class="mx-1">·</span>
                    <span>{{ profession.createdAt }}</span>
                    <span class="mx-1">·</span>
                    <span>ID: {{ profession.id }}</span>
                  </div>
                </div>

                <!-- 内容 -->
                <div class="content-wrapper">
                  <div class="text-body-2 text-grey-darken-1 mb-3">
                    {{ profession.description || '暂无描述' }}
                  </div>

                  <!-- 技能要求 -->
                  <div v-if="profession.skills" class="mb-2 d-flex align-center flex-wrap">
                    <span class="text-caption text-grey-darken-1 mr-2 mb-1">技能：</span>
                    <v-chip
                      v-for="skill in getSkillsArray(profession.skills)"
                      :key="skill"
                      variant="flat"
                      color="grey-lighten-4"
                      size="x-small"
                      class="mr-1 mb-1"
                    >
                      {{ skill }}
                    </v-chip>
                  </div>

                  <!-- 分类信息 -->
                  <div v-if="profession.mainCategory || profession.subCategory" class="text-caption text-grey-darken-1">
                    <span>分类：</span>
                    <span v-if="profession.mainCategory">{{ getCategoryName(profession.mainCategory) }}</span>
                    <span v-if="profession.mainCategory && profession.subCategory"> | </span>
                    <span v-if="profession.subCategory">{{ getSubCategoryName(profession.mainCategory, profession.subCategory) }}</span>
                  </div>

                  <!-- 拒绝/封禁原因 -->
                  <div v-if="(profession.state === ContentState.REJECTED || profession.state === ContentState.BANNED) && profession.reason" class="mt-2">
                    <span class="text-caption text-red-darken-2">{{ profession.state === ContentState.BANNED ? '封禁' : '拒绝' }}原因：{{ profession.reason }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载指示器（加载更多时显示） -->
        <div v-if="loading && displayList.length > 0" class="text-center py-4">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">加载中...</span>
        </div>

        <!-- 没有更多 -->
        <div v-if="!isFilterMode && !hasMoreData && displayList.length > 0" class="text-center py-4 text-caption text-grey">
          没有更多了
        </div>
      </v-card-text>
    </v-card>

    <!-- 拒绝/屏蔽对话框 -->
    <RejectBanDialog
      v-model="showReasonDialog"
      :type="dialogType"
      :item-name="currentProfession?.name || ''"
      :item-state="currentProfession?.state"
      item-type="职业"
      :loading="submitting"
      @confirm="handleConfirmAction"
    />

    <!-- 编辑职业对话框 -->
    <v-dialog v-model="showEditDialog" max-width="700px" persistent>
      <v-card rounded="lg" variant="flat">
        <v-card-title class="text-h6 font-weight-bold pa-6 pb-4">
          <v-icon icon="mdi-pencil-outline" color="blue-darken-2" class="mr-3"></v-icon>
          编辑职业信息
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <v-form ref="editForm" v-model="editFormValid">
            <v-row>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model="editProfession.name"
                  label="职业名称"
                  :rules="professionNameRules"
                  :counter="PROFESSION_VALIDATION.NAME_MAX_LENGTH"
                  variant="outlined"
                  rounded="lg"
                  bg-color="grey-lighten-5"
                  clearable
                ></v-text-field>
              </v-col>

              <v-col cols="12" md="6">
                <v-text-field
                  v-model="editProfession.price"
                  label="价格"
                  variant="outlined"
                  rounded="lg"
                  bg-color="grey-lighten-5"
                  placeholder="例如：免费 或 ¥99"
                ></v-text-field>
              </v-col>
            </v-row>

            <v-textarea
              v-model="editProfession.description"
              label="职业描述"
              :rules="professionDescriptionRules"
              :counter="PROFESSION_VALIDATION.DESCRIPTION_MAX_LENGTH"
              variant="outlined"
              rounded="lg"
              bg-color="grey-lighten-5"
              rows="4"
              clearable
              class="mb-4"
            ></v-textarea>

            <v-text-field
              v-model="editProfession.skillsText"
              label="技能要求（用逗号分隔）"
              variant="outlined"
              rounded="lg"
              bg-color="grey-lighten-5"
              placeholder="例如：Java编程,数据库设计,项目管理"
              class="mb-4"
            ></v-text-field>

            <!-- 分类选择 -->
            <div class="mb-4">
              <div class="text-body-2 font-weight-medium mb-2">职业分类</div>
              <CategorySelector
                v-model:model-main-category="editProfession.mainCategory"
                v-model:model-sub-category="editProfession.subCategory"
              />
            </div>

            <!-- 图标输入 -->
            <v-text-field
              v-model="editProfession.icon"
              label="图标（Material Design Icons）"
              variant="outlined"
              rounded="lg"
              bg-color="grey-lighten-5"
              placeholder="例如：mdi-briefcase, mdi-laptop, mdi-palette"
              class="mb-4"
              hint="请输入Material Design Icons图标名称，如：mdi-briefcase"
              persistent-hint
            ></v-text-field>

            <!-- 拒绝原因（仅在拒绝状态时显示） -->
            <v-textarea
              v-if="editProfession.state === ContentState.REJECTED"
              v-model="editProfession.reason"
              label="拒绝原因"
              variant="outlined"
              rounded="lg"
              bg-color="red-lighten-5"
              rows="3"
              class="mb-4"
              placeholder="请输入拒绝原因..."
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
            color="primary"
            rounded="lg"
            :disabled="!editFormValid"
            :loading="updating"
            @click="updateProfession"
          >
            <v-icon icon="mdi-content-save" class="mr-2"></v-icon>
            保存修改
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.list-item {
  padding: 16px;
  border-radius: 8px;
  background-color: #fafafa;
}

.action-area {
  width: 70px;
  flex-shrink: 0;
}

.content-wrapper {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  background-color: white;
}
</style>
