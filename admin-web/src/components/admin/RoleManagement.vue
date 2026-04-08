<script setup lang="ts">
import { inject, ref, onMounted, nextTick, computed } from 'vue'
import { adminApi, systemApi, roleApi, imageApi } from '@/api'
import { ContentState } from '@/enums'
import type { Role, RoleCategory, CategoryMapping } from '@/types/role.d'
import type { StateOption } from '@/types/common'
import CategorySelector from '../common/CategorySelector.vue'
import RejectBanDialog from './RejectBanDialog.vue'
import { roleNameRules, roleDescriptionRules } from '@/utils/validationRules'
import { ROLE_VALIDATION } from '@/types/validation'
import { useFetchForScroll } from '@/composables/useFetchForScroll'
import { useMutation } from '@/composables/useMutation'
import { useSystemConfigStore } from '@/stores'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')
const systemConfigStore = useSystemConfigStore()

// 扩展 Role 接口以包含UI状态
interface RoleWithUIState extends Role {
  approving?: boolean
  restoring?: boolean
  deleting?: boolean
}

// 编辑表单数据
interface EditRoleForm {
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
const filterRoleId = ref<number | null>(null)
const filterRoleName = ref<string>('')
const isFilterMode = ref<boolean>(false)
const filterResult = ref<RoleWithUIState | null>(null)

// 按名称搜索（使用 useFetchForScroll）
const {
  items: searchedRoleList,
  loading: searchByNameLoading,
  hasMore: searchByNameHasMore,
  loadMore: loadMoreSearchResults,
  reset: resetSearchResults,
} = useFetchForScroll<RoleWithUIState>({
  fetchFn: (params) => adminApi.searchRolesByName(filterRoleName.value.trim(), params.lastId ?? undefined),
  initialParams: { lastId: null },
  immediate: false,
})

// 显示的职业列表
const displayList = computed<RoleWithUIState[]>(() => {
  if (isFilterMode.value && filterResult.value) {
    return [filterResult.value]
  }
  if (searchedRoleList.value && searchedRoleList.value.length > 0) {
    return searchedRoleList.value
  }
  return roleList.value
})

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const currentRole = ref<RoleWithUIState | null>(null)
const dialogType = ref<'reject' | 'ban'>('reject')

// 编辑相关数据
const showEditDialog = ref<boolean>(false)
const editRole = ref<EditRoleForm>({})
const editFormValid = ref<boolean>(false)
const editForm = ref(null)

// 图标上传相关
const iconFileInput = ref<HTMLInputElement | null>(null)

// 使用 useMutation 上传图标
const { execute: uploadIcon, loading: iconUploading } = useMutation(
  (file: File) => imageApi.upload(file, 'role'),
  { showToast: false }
)

// 判断当前图标类型
const currentIconType = computed(() => {
  const icon = editRole.value.icon
  if (!icon) return 'none'
  if (icon.startsWith('http')) return 'image'
  return 'mdi'
})

// 触发图标文件选择
const triggerIconUpload = (): void => {
  iconFileInput.value?.click()
}

// 处理图标上传
const handleIconUpload = async (event: Event): Promise<void> => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    showSnackbar?.('请上传图片文件', 'error')
    return
  }

  // 验证文件大小 (最大 2MB)
  if (file.size > 2 * 1024 * 1024) {
    showSnackbar?.('图片大小不能超过 2MB', 'error')
    return
  }

  try {
    const response = await uploadIcon(file)
    if (response?.fileUrl) {
      editRole.value.icon = response.fileUrl
      showSnackbar?.('图标上传成功', 'success')
    }
  } catch {
    showSnackbar?.('上传失败', 'error')
  } finally {
    if (target) target.value = ''
  }
}

// 清除图标
const clearIcon = (): void => {
  editRole.value.icon = ''
}

// 动态类别数据
const mainCategories = ref<RoleCategory[]>([])
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
const loadRoleCategories = async (): Promise<void> => {
  try {
    const response = await systemApi.getRoleCategories()
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

// 使用 useFetchForScroll 加载职业列表
const {
  items: roleList,
  loading,
  hasMore: hasMoreData,
  loadMore: loadMoreData,
  reset: resetRoleList,
} = useFetchForScroll<RoleWithUIState>({
  fetchFn: (params) => {
    const state = getCurrentState()
    return adminApi.getRolesByFilter(state, params.lastId ?? undefined)
  },
  initialParams: {
    lastId: null,
  },
  immediate: true,
})

// 状态改变处理
const onStateChange = (): void => {
  resetRoleList()
  loadMoreData()
}

// 无限滚动回调接口
type InfiniteScrollCallback = (status: 'ok' | 'empty' | 'error') => void

// 名称搜索加载更多回调
const onSearchLoadMore = async ({ done }: { done: InfiniteScrollCallback }): Promise<void> => {
  if (!searchByNameHasMore.value || searchByNameLoading.value) {
    done('empty')
    return
  }

  try {
    await loadMoreSearchResults()
    if (searchByNameHasMore.value) {
      done('ok')
    } else {
      done('empty')
    }
  } catch {
    done('error')
  }
}

// 按ID筛选职业
const searchById = async (): Promise<void> => {
  if (!filterRoleId.value) {
    showSnackbar?.('请输入职业ID', 'warning')
    return
  }

  // 清除名称搜索
  filterRoleName.value = ''
  resetSearchResults()

  try {
    const response = await adminApi.getRoleById(filterRoleId.value)
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

// 按名称搜索职业
const searchByName = async (): Promise<void> => {
  if (!filterRoleName.value || filterRoleName.value.trim() === '') {
    showSnackbar?.('请输入职业名称', 'error')
    return
  }

  // 清除ID搜索
  filterRoleId.value = null
  filterResult.value = null
  isFilterMode.value = false

  // 重置并加载
  resetSearchResults()
  await loadMoreSearchResults()

  if (searchedRoleList.value && searchedRoleList.value.length > 0) {
    showSnackbar?.(`找到 ${searchedRoleList.value.length} 个职业`, 'success')
  } else {
    showSnackbar?.('未找到相关职业', 'warning')
  }
}

// 清除筛选
const clearFilter = (): void => {
  filterRoleId.value = null
  filterRoleName.value = ''
  filterResult.value = null
  isFilterMode.value = false
  resetSearchResults()
}

// 使用 useMutation 操作职业申请
const { execute: executeOperateRole } = useMutation(
  (data: { roleId: number; action: string; reason?: string }) =>
    roleApi.approveRole(data.roleId, data.action, data.reason || ''),
  {
    onSuccess: (_, data) => {
      const index = roleList.value.findIndex((p) => p.id === data.roleId)
      if (index !== -1) {
        if (data.action === 'APPROVE') {
          roleList.value[index].state = ContentState.PUBLISHED
          roleList.value[index].reason = ''
        } else if (data.action === 'REJECT') {
          roleList.value[index].state = ContentState.REJECTED
          roleList.value[index].reason = data.reason || ''
        } else if (data.action === 'BAN') {
          roleList.value[index].state = ContentState.BANNED
          roleList.value[index].reason = data.reason || ''
        }

        // 如果当前筛选状态与操作结果不匹配，从列表中移除
        const currentState = getCurrentState()
        const shouldRemove =
          (data.action === 'APPROVE' && currentState !== ContentState.PUBLISHED) ||
          (data.action === 'REJECT' && currentState !== ContentState.REJECTED) ||
          (data.action === 'BAN' && currentState !== ContentState.BANNED)

        if (shouldRemove) {
          roleList.value.splice(index, 1)
        }
      }
    },
  }
)

const operateRole = async (
  role: RoleWithUIState,
  action: string,
  reason = ''
): Promise<boolean> => {
  try {
    await executeOperateRole({ roleId: role.id, action, reason })
    return true
  } catch (error) {
    return false
  }
}

// 通过申请
const approveRole = async (role: RoleWithUIState): Promise<void> => {
  role.approving = true
  const success = await operateRole(role, 'APPROVE')
  role.approving = false

  if (success) {
    showSnackbar?.('操作成功')
  }
}

// 恢复申请 - 将已拒绝的职业重新通过
const restoreRole = async (role: RoleWithUIState): Promise<void> => {
  role.restoring = true
  const success = await operateRole(role, 'APPROVE')
  role.restoring = false

  if (success) {
    showSnackbar?.('操作成功')
  }
}

// 显示拒绝对话框
const showRejectModal = (role: RoleWithUIState): void => {
  currentRole.value = role
  dialogType.value = 'reject'
  showReasonDialog.value = true
}

// 显示屏蔽对话框
const showBanModal = (role: RoleWithUIState): void => {
  currentRole.value = role
  dialogType.value = 'ban'
  showReasonDialog.value = true
}

// 使用 useMutation 处理拒绝/屏蔽
const { execute: executeRejectOrBan, loading: submitting } = useMutation(
  (data: { roleId: number; action: string; reason: string }) =>
    roleApi.approveRole(data.roleId, data.action, data.reason),
  {
    successMessage: '操作成功',
    onSuccess: (_, data) => {
      const index = roleList.value.findIndex((p) => p.id === data.roleId)
      if (index !== -1) {
        const targetState = data.action === 'REJECT' ? ContentState.REJECTED : ContentState.BANNED
        roleList.value[index].state = targetState
        roleList.value[index].reason = data.reason

        const currentState = getCurrentState()
        if (currentState !== targetState) {
          roleList.value.splice(index, 1)
        }
      }

      showReasonDialog.value = false
      currentRole.value = null
    },
  }
)

// 处理对话框确认
const handleConfirmAction = async (reason: string): Promise<void> => {
  if (!currentRole.value) return

  const action = dialogType.value === 'reject' ? 'REJECT' : 'BAN'
  await executeRejectOrBan({
    roleId: currentRole.value.id,
    action,
    reason,
  })
}

// 使用 useMutation 取消屏蔽职业
const { execute: executeUnbanRole } = useMutation(
  (roleId: number) => roleApi.approveRole(roleId, 'APPROVE'),
  {
    successMessage: '操作成功',
    onSuccess: (_, roleId) => {
      const index = roleList.value.findIndex((p) => p.id === roleId)
      if (index !== -1) {
        roleList.value[index].state = ContentState.PUBLISHED
        roleList.value[index].reason = ''

        const currentState = getCurrentState()
        if (currentState !== ContentState.PUBLISHED) {
          roleList.value.splice(index, 1)
        }
      }
    },
  }
)

const unbanRole = async (role: RoleWithUIState): Promise<void> => {
  await executeUnbanRole(role.id)
}

// 显示编辑对话框
const showEditModal = (role: RoleWithUIState): void => {
  currentRole.value = role
  editRole.value = {
    id: role.id,
    name: role.name || '',
    description: role.description || '',
    price: role.price || '',
    skillsText: role.skills || '',
    mainCategory: role.mainCategory || null,
    subCategory: role.subCategory || null,
    icon: role.icon || '',
    reason: role.reason || '',
    state: role.state,
  }

  showEditDialog.value = true

  nextTick(() => {
    ;(editForm.value as any)?.validate()
  })
}

// 关闭编辑对话框
const closeEditDialog = (): void => {
  showEditDialog.value = false
  currentRole.value = null
  editRole.value = {}
  editFormValid.value = false
}

// 使用 useMutation 更新职业信息
const { execute: executeUpdateRole, loading: updating } = useMutation(
  (data: { id: number; updateData: any }) =>
    roleApi.updateRole(data.id, data.updateData),
  {
    successMessage: '操作成功',
    onSuccess: (_, data) => {
      const index = roleList.value.findIndex((p) => p.id === data.id)
      if (index !== -1) {
        roleList.value[index] = {
          ...roleList.value[index],
          ...data.updateData,
        }
      }
      closeEditDialog()
    },
  }
)

// 更新职业信息
const updateRole = async (): Promise<void> => {
  if (!editFormValid.value) return

  const updateData = {
    name: editRole.value.name!,
    description: editRole.value.description!,
    price: editRole.value.price || '',
    skills: editRole.value.skillsText || '',
    mainCategory: editRole.value.mainCategory || null,
    subCategory: editRole.value.subCategory || null,
    icon: editRole.value.icon || '',
    reason: editRole.value.reason || '',
  }

  await executeUpdateRole({
    id: editRole.value.id!,
    updateData,
  })
}

// 组件挂载时加载数据
onMounted(() => {
  loadRoleCategories()
})
</script>

<template>
  <div>
    <h2 class="text-h5 font-weight-bold mb-4">职业管理</h2>

    <!-- 查询 -->
    <v-card flat class="border mb-4">
      <v-card-text>
        <v-row align="center">
          <v-col cols="2">
            <v-text-field
              v-model.number="filterRoleId"
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
            <v-btn
              variant="tonal"
              size="default"
              :disabled="!filterRoleId"
              @click="searchById"
            >
              <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
              查询
            </v-btn>
          </v-col>
          <v-col cols="auto" class="text-body-2 text-grey-darken-1">
            或
          </v-col>
          <v-col cols="3">
            <v-text-field
              v-model="filterRoleName"
              label="职业名称"
              variant="outlined"
              density="compact"
              hide-details
              clearable
              @keyup.enter="searchByName"
            ></v-text-field>
          </v-col>
          <v-col cols="auto">
            <v-btn
              variant="tonal"
              size="default"
              :loading="searchByNameLoading"
              :disabled="!filterRoleName"
              @click="searchByName"
            >
              <v-icon icon="mdi-magnify" size="16" class="mr-1"></v-icon>
              查询
            </v-btn>
          </v-col>
          <v-col cols="auto">
            <v-btn
              v-if="isFilterMode || (searchedRoleList && searchedRoleList.length > 0)"
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
          v-if="!isFilterMode && (!searchedRoleList || searchedRoleList.length === 0)"
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

        <!-- 搜索加载状态（仅首次加载时显示） -->
        <div v-if="searchByNameLoading && searchedRoleList.length === 0" class="text-center py-8">
          <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
          <span class="ml-2 text-grey-darken-1">查询中...</span>
        </div>

        <!-- 名称搜索结果 -->
        <template v-else-if="searchedRoleList && searchedRoleList.length > 0">
          <div class="text-body-2 font-weight-medium text-grey-darken-2 mb-3">
            搜索结果 ({{ searchedRoleList.length }}个)
          </div>
          <v-infinite-scroll
            :empty="!searchByNameHasMore"
            @load="onSearchLoadMore"
          >
            <div
              v-for="role in searchedRoleList"
              :key="role.id"
              class="list-item mb-3"
            >
              <div class="d-flex align-start">
                <!-- 操作区 -->
                <div class="action-area mr-4">
                  <!-- 待审核 -->
                  <div v-if="role.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                    <v-btn variant="tonal" color="success" size="small" block :loading="role.approving" @click="approveRole(role)">
                      批准
                    </v-btn>
                    <v-btn variant="tonal" color="error" size="small" block @click="showRejectModal(role)">
                      拒绝
                    </v-btn>
                    <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(role)">
                      屏蔽
                    </v-btn>
                  </div>

                  <!-- 已通过 -->
                  <div v-if="role.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                    <v-btn variant="tonal" color="warning" size="small" block @click="showRejectModal(role)">
                      撤回
                    </v-btn>
                    <v-btn variant="tonal" color="info" size="small" block @click="showEditModal(role)">
                      编辑
                    </v-btn>
                    <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(role)">
                      屏蔽
                    </v-btn>
                  </div>

                  <!-- 已拒绝 -->
                  <div v-if="role.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                    <v-btn variant="tonal" color="success" size="small" block :loading="role.restoring" @click="restoreRole(role)">
                      通过
                    </v-btn>
                    <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(role)">
                      屏蔽
                    </v-btn>
                  </div>

                  <!-- 已屏蔽 -->
                  <div v-if="role.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                    <v-btn variant="tonal" color="info" size="small" block @click="unbanRole(role)">
                      解封
                    </v-btn>
                  </div>

                  <!-- 编辑按钮（非已通过状态） -->
                  <div v-if="role.state !== ContentState.PUBLISHED" class="mt-2">
                    <v-btn variant="tonal" color="info" size="small" block @click="showEditModal(role)">
                      编辑
                    </v-btn>
                  </div>
                </div>

                <!-- 内容区 -->
                <div class="flex-grow-1">
                  <!-- 标题行 -->
                  <div class="d-flex align-center justify-space-between mb-2">
                    <div class="d-flex align-center">
                      <v-icon v-if="role.icon" :icon="role.icon" size="18" class="mr-2 text-grey-darken-2"></v-icon>
                      <div class="text-body-1 font-weight-medium text-grey-darken-3">
                        {{ role.name || '职业名称' }}
                      </div>
                      <a :href="systemConfigStore.getRoleUrl(role.id)" target="_blank" class="ml-1">
                        <v-icon icon="mdi-open-in-new" size="14" color="grey"></v-icon>
                      </a>
                      <v-chip variant="flat" :color="getStateConfig(role.state).color" size="x-small" class="ml-2">
                        {{ getStateConfig(role.state).text }}
                      </v-chip>
                      <v-chip v-if="role.price" variant="flat" color="green-lighten-4" size="x-small" class="ml-1">
                        $ {{ role.price }}
                      </v-chip>
                    </div>
                    <div class="d-flex align-center text-caption text-grey-darken-1">
                      <a v-if="role.creator" :href="`/user/${role.creator.id}`" target="_blank" class="text-grey-darken-1">{{ role.creator.name }}</a>
                      <span v-else>未知</span>
                      <span class="mx-1">·</span>
                      <span>{{ role.createdAt }}</span>
                      <span class="mx-1">·</span>
                      <span>ID: {{ role.id }}</span>
                    </div>
                  </div>

                  <!-- 内容 -->
                  <div class="content-wrapper">
                    <div class="text-body-2 text-grey-darken-1 mb-3">
                      {{ role.description || '暂无描述' }}
                    </div>

                    <!-- 技能要求 -->
                    <div v-if="role.skills" class="mb-2 d-flex align-center flex-wrap">
                      <span class="text-caption text-grey-darken-1 mr-2 mb-1">技能：</span>
                      <v-chip
                        v-for="skill in getSkillsArray(role.skills)"
                        :key="skill"
                        variant="flat"
                        color="grey-lighten-4"
                        size="x-small"
                        class="mr-1 mb-1"
                      >
                        {{ skill }}
                      </v-chip>
                    </div>

                    <!-- 分类信息和统计 -->
                    <div class="d-flex align-center text-caption text-grey-darken-1">
                      <template v-if="role.mainCategory || role.subCategory">
                        <span class="d-inline-flex align-center">
                          <v-icon icon="mdi-tag-outline" size="12" class="mr-1"></v-icon>
                          <v-tooltip activator="parent" location="top">分类</v-tooltip>
                        </span>
                        <span v-if="role.mainCategory">{{ getCategoryName(role.mainCategory) }}</span>
                        <span v-if="role.mainCategory && role.subCategory" class="mx-1">|</span>
                        <span v-if="role.subCategory">{{ getSubCategoryName(role.mainCategory, role.subCategory) }}</span>
                        <span class="mx-2"></span>
                      </template>
                      <span class="d-inline-flex align-center">
                        <v-icon icon="mdi-map-outline" size="12" class="mr-1"></v-icon>
                        <v-tooltip activator="parent" location="top">路线图数量</v-tooltip>
                      </span>
                      {{ role.roadmapCount ?? 0 }}
                      <span class="mx-2"></span>
                      <span class="d-inline-flex align-center">
                        <v-icon icon="mdi-bookmark-outline" size="12" class="mr-1"></v-icon>
                        <v-tooltip activator="parent" location="top">收藏数</v-tooltip>
                      </span>
                      {{ role.bookmarkCount ?? 0 }}
                    </div>

                    <!-- 拒绝/屏蔽原因 -->
                    <div
                      v-if="role.reason && (role.state === ContentState.REJECTED || role.state === ContentState.BANNED)"
                      class="text-caption text-error mt-2"
                    >
                      原因: {{ role.reason }}
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <template #empty>
              <div class="text-center py-4 text-caption text-grey">
                没有更多了
              </div>
            </template>

            <template #loading>
              <div class="text-center py-4">
                <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
                <span class="ml-2 text-grey-darken-1">加载中...</span>
              </div>
            </template>
          </v-infinite-scroll>
        </template>

        <!-- 首次加载状态 -->
        <div v-else-if="loading && displayList.length === 0" class="text-center py-8">
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
            v-for="role in displayList"
            :key="role.id"
            v-intersect="{
              handler: (isIntersecting: boolean) => {
                if (!isFilterMode && isIntersecting && role === displayList[displayList.length - 1] && hasMoreData && !loading) {
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
                <div v-if="role.state === ContentState.SUBMITTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block :loading="role.approving" @click="approveRole(role)">
                    批准
                  </v-btn>
                  <v-btn variant="tonal" color="error" size="small" block @click="showRejectModal(role)">
                    拒绝
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(role)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已通过 -->
                <div v-if="role.state === ContentState.PUBLISHED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="warning" size="small" block @click="showRejectModal(role)">
                    撤回
                  </v-btn>
                  <v-btn variant="tonal" color="info" size="small" block @click="showEditModal(role)">
                    编辑
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(role)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已拒绝 -->
                <div v-if="role.state === ContentState.REJECTED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="success" size="small" block :loading="role.restoring" @click="restoreRole(role)">
                    通过
                  </v-btn>
                  <v-btn variant="tonal" color="grey" size="small" block @click="showBanModal(role)">
                    屏蔽
                  </v-btn>
                </div>

                <!-- 已屏蔽 -->
                <div v-if="role.state === ContentState.BANNED" class="d-flex flex-column ga-2">
                  <v-btn variant="tonal" color="info" size="small" block @click="unbanRole(role)">
                    解封
                  </v-btn>
                </div>

                <!-- 编辑按钮（非已通过状态） -->
                <div v-if="role.state !== ContentState.PUBLISHED" class="mt-2">
                  <v-btn variant="tonal" color="info" size="small" block @click="showEditModal(role)">
                    编辑
                  </v-btn>
                </div>
              </div>

              <!-- 内容区 -->
              <div class="flex-grow-1">
                <!-- 标题行 -->
                <div class="d-flex align-center justify-space-between mb-2">
                  <div class="d-flex align-center">
                    <v-icon v-if="role.icon" :icon="role.icon" size="18" class="mr-2 text-grey-darken-2"></v-icon>
                    <div class="text-body-1 font-weight-medium text-grey-darken-3">
                      {{ role.name || '职业名称' }}
                    </div>
                    <a :href="systemConfigStore.getRoleUrl(role.id)" target="_blank" class="ml-1">
                      <v-icon icon="mdi-open-in-new" size="14" color="grey"></v-icon>
                    </a>
                    <v-chip variant="flat" :color="getStateConfig(role.state).color" size="x-small" class="ml-2">
                      {{ getStateConfig(role.state).text }}
                    </v-chip>
                    <v-chip v-if="role.price" variant="flat" color="green-lighten-4" size="x-small" class="ml-1">
                      $ {{ role.price }}
                    </v-chip>
                  </div>
                  <div class="d-flex align-center text-caption text-grey-darken-1">
                    <a v-if="role.creator" :href="`/user/${role.creator.id}`" target="_blank" class="text-grey-darken-1">{{ role.creator.name }}</a>
                    <span v-else>未知</span>
                    <span class="mx-1">·</span>
                    <span>{{ role.createdAt }}</span>
                    <span class="mx-1">·</span>
                    <span>ID: {{ role.id }}</span>
                  </div>
                </div>

                <!-- 内容 -->
                <div class="content-wrapper">
                  <div class="text-body-2 text-grey-darken-1 mb-3">
                    {{ role.description || '暂无描述' }}
                  </div>

                  <!-- 技能要求 -->
                  <div v-if="role.skills" class="mb-2 d-flex align-center flex-wrap">
                    <span class="text-caption text-grey-darken-1 mr-2 mb-1">技能：</span>
                    <v-chip
                      v-for="skill in getSkillsArray(role.skills)"
                      :key="skill"
                      variant="flat"
                      color="grey-lighten-4"
                      size="x-small"
                      class="mr-1 mb-1"
                    >
                      {{ skill }}
                    </v-chip>
                  </div>

                  <!-- 分类信息和统计 -->
                  <div class="d-flex align-center text-caption text-grey-darken-1">
                    <template v-if="role.mainCategory || role.subCategory">
                      <span class="d-inline-flex align-center">
                        <v-icon icon="mdi-tag-outline" size="12" class="mr-1"></v-icon>
                        <v-tooltip activator="parent" location="top">分类</v-tooltip>
                      </span>
                      <span v-if="role.mainCategory">{{ getCategoryName(role.mainCategory) }}</span>
                      <span v-if="role.mainCategory && role.subCategory" class="mx-1">|</span>
                      <span v-if="role.subCategory">{{ getSubCategoryName(role.mainCategory, role.subCategory) }}</span>
                      <span class="mx-2"></span>
                    </template>
                    <span class="d-inline-flex align-center">
                      <v-icon icon="mdi-map-outline" size="12" class="mr-1"></v-icon>
                      <v-tooltip activator="parent" location="top">路线图数量</v-tooltip>
                    </span>
                    {{ role.roadmapCount ?? 0 }}
                    <span class="mx-2"></span>
                    <span class="d-inline-flex align-center">
                      <v-icon icon="mdi-bookmark-outline" size="12" class="mr-1"></v-icon>
                      <v-tooltip activator="parent" location="top">收藏数</v-tooltip>
                    </span>
                    {{ role.bookmarkCount ?? 0 }}
                  </div>

                  <!-- 拒绝/封禁原因 -->
                  <div v-if="(role.state === ContentState.REJECTED || role.state === ContentState.BANNED) && role.reason" class="mt-2">
                    <span class="text-caption text-red-darken-2">{{ role.state === ContentState.BANNED ? '封禁' : '拒绝' }}原因：{{ role.reason }}</span>
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
      :item-name="currentRole?.name || ''"
      :item-state="currentRole?.state"
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
                  v-model="editRole.name"
                  label="职业名称"
                  :rules="roleNameRules"
                  :counter="ROLE_VALIDATION.NAME_MAX_LENGTH"
                  variant="outlined"
                  rounded="lg"
                  bg-color="grey-lighten-5"
                  clearable
                ></v-text-field>
              </v-col>

              <v-col cols="12" md="6">
                <v-text-field
                  v-model="editRole.price"
                  label="价格"
                  variant="outlined"
                  rounded="lg"
                  bg-color="grey-lighten-5"
                  placeholder="例如：免费 或 ¥99"
                ></v-text-field>
              </v-col>
            </v-row>

            <v-textarea
              v-model="editRole.description"
              label="职业描述"
              :rules="roleDescriptionRules"
              :counter="ROLE_VALIDATION.DESCRIPTION_MAX_LENGTH"
              variant="outlined"
              rounded="lg"
              bg-color="grey-lighten-5"
              rows="4"
              clearable
              class="mb-4"
            ></v-textarea>

            <v-text-field
              v-model="editRole.skillsText"
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
                v-model:model-main-category="editRole.mainCategory"
                v-model:model-sub-category="editRole.subCategory"
              />
            </div>

            <!-- 图标设置 -->
            <div class="mb-4">
              <div class="text-body-2 font-weight-medium mb-2">职业图标</div>

              <!-- 隐藏的文件输入 -->
              <input
                ref="iconFileInput"
                type="file"
                accept="image/jpeg,image/png,image/webp,image/svg+xml"
                style="display: none"
                @change="handleIconUpload"
              />

              <!-- MDI 图标输入 -->
              <v-text-field
                v-model="editRole.icon"
                label="MDI 图标名称或图片链接"
                variant="outlined"
                rounded="lg"
                bg-color="grey-lighten-5"
                placeholder="例如：mdi-briefcase, mdi-laptop"
                density="compact"
                hide-details
                class="mb-2"
              >
                <!-- 左侧：图标预览 -->
                <template #prepend-inner>
                  <v-icon
                    v-if="!editRole.icon || currentIconType === 'mdi'"
                    :icon="editRole.icon || 'mdi-palette'"
                    size="20"
                    :color="editRole.icon ? 'grey-darken-2' : 'grey'"
                    class="mr-1"
                  />
                  <v-img
                    v-else
                    :src="editRole.icon"
                    width="24"
                    height="24"
                    cover
                    class="rounded mr-1"
                  />
                </template>
                <!-- 右侧：清除按钮 -->
                <template v-if="editRole.icon" #append-inner>
                  <v-icon
                    icon="mdi-close-circle"
                    size="18"
                    color="grey"
                    class="cursor-pointer"
                    @click="clearIcon"
                  />
                </template>
              </v-text-field>

              <div class="d-flex align-center my-2">
                <v-divider class="flex-grow-1" />
                <span class="text-caption text-grey mx-3">或</span>
                <v-divider class="flex-grow-1" />
              </div>

              <!-- 上传图片按钮 -->
              <v-btn
                variant="tonal"
                color="primary"
                size="small"
                :loading="iconUploading"
                @click="triggerIconUpload"
              >
                <v-icon icon="mdi-upload" size="16" class="mr-1" />
                上传图片
              </v-btn>
              <div class="text-caption text-grey mt-1">
                支持 JPG、PNG、WebP、SVG 格式，最大 2MB
              </div>
            </div>

            <!-- 拒绝原因（仅在拒绝状态时显示） -->
            <v-textarea
              v-if="editRole.state === ContentState.REJECTED"
              v-model="editRole.reason"
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
            @click="updateRole"
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
