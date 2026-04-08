<script setup lang="ts">
import { inject, ref, onMounted } from 'vue'
import { adminApi, systemApi, roleApi } from '@/api'
import { ContentState } from '@/enums'
import type { Role, RoleCategory, CategoryMapping } from '@/types/role.d'
import type { StateOption } from '@/types/common'
import CategorySelector from '../common/CategorySelector.vue'
import RejectBanDialog from './RejectBanDialog.vue'
import { roleNameRules, roleDescriptionRules } from '@/utils/validationRules'
import { ROLE_VALIDATION } from '@/types/validation'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
import { useMutation } from '@/composables/useMutation'

const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

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

// 拒绝/屏蔽对话框
const showReasonDialog = ref<boolean>(false)
const currentRole = ref<RoleWithUIState | null>(null)
const dialogType = ref<'reject' | 'ban'>('reject')

// 编辑相关数据
const showEditDialog = ref<boolean>(false)
const editRole = ref<EditRoleForm>({})
const editFormValid = ref<boolean>(false)
const editForm = ref(null)

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

// 加载角色类别数据
const loadRoleCategories = async (): Promise<void> => {
  try {
    const response = await systemApi.getRoleCategories()
    if (response.code === 200 && response.data) {
      mainCategories.value = response.data.mainCategories || []
      categoryMapping.value = response.data.categoryMapping || []
    }
  } catch (error) {
    console.error('加载角色类别失败:', error)
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
const getCurrentStateText = (): string => stateOptions[selectedStateIndex.value]?.text || '待审核'

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

// 使用 useInfiniteScroll 加载角色列表
const {
  items: roleList,
  loading,
  hasMore: hasMoreData,
  loadMore: loadMoreData,
  reset: resetRoleList,
} = useInfiniteScroll({
  fetchFn: (params) => {
    const state = getCurrentState()
    return adminApi.getRolesByFilter(state, params.lastId)
  },
  getNextParams: (lastItem) => ({
    lastId: lastItem.id,
  }),
  initialParams: {
    lastId: null,
  },
  immediate: true, // 自动初始加载
})

// 状态改变处理
const onStateChange = (): void => {
  resetRoleList()
  loadMoreData() // 重新加载数据
}

// 使用 useMutation 操作角色申请
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

// 恢复申请 - 将已拒绝的角色重新通过
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

// 使用 useMutation 取消屏蔽角色
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
    icon: role.icon || 'mdi-briefcase',
    reason: role.reason || '',
    state: role.state,
  }

  showEditDialog.value = true
}

// 关闭编辑对话框
const closeEditDialog = (): void => {
  showEditDialog.value = false
  currentRole.value = null
  editRole.value = {}
  editFormValid.value = false
}

// 使用 useMutation 更新角色信息
const { execute: executeUpdateRole, loading: updating } = useMutation(
  (data: { id: number; updateData: any }) => roleApi.updateRole(data.id, data.updateData),
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

// 更新角色信息
const updateRole = async (): Promise<void> => {
  if (!editFormValid.value) return

  const updateData = {
    id: editRole.value.id!,
    name: editRole.value.name!,
    description: editRole.value.description!,
    price: editRole.value.price || '',
    skills: editRole.value.skillsText || '',
    mainCategory: editRole.value.mainCategory || 0,
    subCategory: editRole.value.subCategory || 0,
    icon: editRole.value.icon || 'mdi-briefcase',
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
  <div class="role-management">
    <!-- 页面头部 -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-blue-lighten-5 mr-3">
          <v-icon icon="mdi-briefcase-check-outline" color="blue-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">角色申请管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">审核和管理用户提交的角色申请</p>
        </div>
      </div>
      <v-chip variant="flat" color="blue-lighten-4" rounded="lg">
        <v-icon icon="mdi-briefcase" color="blue-darken-2" size="16" class="mr-2"></v-icon>
        <span class="text-blue-darken-2 text-caption">{{ roleList.length }}个角色</span>
      </v-chip>
    </div>

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
          :color="
            state.value === ContentState.SUBMITTED
              ? 'orange-darken-1'
              : state.value === ContentState.PUBLISHED
                ? 'green-darken-1'
                : state.value === ContentState.REJECTED
                  ? 'red-darken-1'
                  : 'grey-darken-1'
          "
          size="18"
          class="mr-2"
        ></v-icon>
        {{ state.text }}
      </v-tab>
    </v-tabs>

    <!-- 加载状态 -->
    <div v-if="loading && roleList.length === 0" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
      <p class="mt-3 text-grey-darken-1">加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="roleList.length === 0" class="text-center py-12">
      <v-icon icon="mdi-briefcase-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">暂无{{ getCurrentStateText() }}的角色申请</p>
    </div>

    <!-- 角色申请列表 -->
    <div v-else>
      <v-card
        v-for="role in roleList"
        :key="role.id"
        v-intersect="{
          handler: (isIntersecting) => {
            if (
              isIntersecting &&
              role === roleList[roleList.length - 1] &&
              hasMoreData &&
              !loading
            ) {
              loadMoreData()
            }
          },
        }"
        flat
        class="border rounded-lg pa-5 mb-4"
        hover
      >
        <div class="d-flex align-start">
          <!-- 状态和操作区域 -->
          <div class="mr-4 status-actions-area">
            <div class="mb-3">
              <v-chip
                :color="getStateConfig(profession.state).color"
                variant="flat"
                rounded="lg"
                size="small"
              >
                <v-icon
                  :icon="getStateConfig(profession.state).icon"
                  size="14"
                  class="mr-1"
                ></v-icon>
                {{ getStateConfig(profession.state).text }}
              </v-chip>
            </div>

            <!-- 审核操作按钮 -->
            <div class="d-flex flex-column ga-2">
              <!-- 待审核状态：通过、拒绝、屏蔽 -->
              <template v-if="role.state === ContentState.SUBMITTED">
                <v-btn
                  variant="flat"
                  color="green-lighten-4"
                  rounded="lg"
                  size="small"
                  :loading="role.approving"
                  @click="approveRole(role)"
                >
                  <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                  批准
                </v-btn>
                <v-btn
                  variant="flat"
                  color="red-lighten-4"
                  rounded="lg"
                  size="small"
                  @click="showRejectModal(role)"
                >
                  <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                  拒绝
                </v-btn>
                <v-btn
                  variant="flat"
                  color="grey-lighten-2"
                  rounded="lg"
                  size="small"
                  @click="showBanModal(role)"
                >
                  <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                  屏蔽
                </v-btn>
              </template>

              <!-- 已发布状态：撤销通过、屏蔽 -->
              <template v-if="role.state === ContentState.PUBLISHED">
                <v-btn
                  variant="flat"
                  color="orange-lighten-4"
                  rounded="lg"
                  size="small"
                  @click="showRejectModal(role)"
                >
                  <v-icon icon="mdi-undo" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                  撤销通过
                </v-btn>
                <v-btn
                  variant="flat"
                  color="grey-lighten-2"
                  rounded="lg"
                  size="small"
                  @click="showBanModal(role)"
                >
                  <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                  屏蔽
                </v-btn>
              </template>

              <!-- 已拒绝状态：通过、屏蔽 -->
              <template v-if="role.state === ContentState.REJECTED">
                <v-btn
                  variant="flat"
                  color="green-lighten-4"
                  rounded="lg"
                  size="small"
                  :loading="role.restoring"
                  @click="restoreRole(role)"
                >
                  <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                  通过
                </v-btn>
                <v-btn
                  variant="flat"
                  color="grey-lighten-2"
                  rounded="lg"
                  size="small"
                  @click="showBanModal(role)"
                >
                  <v-icon icon="mdi-cancel" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                  屏蔽
                </v-btn>
              </template>

              <!-- 已屏蔽状态：取消屏蔽、降级为拒绝 -->
              <template v-if="role.state === ContentState.BANNED">
                <v-btn
                  variant="flat"
                  color="blue-lighten-4"
                  rounded="lg"
                  size="small"
                  @click="unbanRole(role)"
                >
                  <v-icon
                    icon="mdi-lock-open"
                    color="blue-darken-2"
                    size="16"
                    class="mr-1"
                  ></v-icon>
                  取消屏蔽
                </v-btn>
                <v-btn
                  variant="flat"
                  color="orange-lighten-4"
                  rounded="lg"
                  size="small"
                  @click="showRejectModal(role)"
                >
                  <v-icon
                    icon="mdi-arrow-down"
                    color="orange-darken-2"
                    size="16"
                    class="mr-1"
                  ></v-icon>
                  降级为拒绝
                </v-btn>
              </template>
            </div>

            <!-- 通用操作按钮 - 所有状态都显示编辑 -->
            <div class="d-flex flex-column ga-2 mt-3">
              <v-btn
                variant="flat"
                color="blue-lighten-4"
                rounded="lg"
                size="small"
                @click="showEditModal(role)"
              >
                <v-icon icon="mdi-pencil" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                编辑
              </v-btn>
            </div>

            <!-- 拒绝原因显示 -->
            <div
              v-if="role.state === ContentState.REJECTED && role.reason"
              class="mt-3"
            >
              <div class="text-caption text-grey-darken-1 mb-1">拒绝原因：</div>
              <div class="text-body-2 text-red-darken-2 rejection-reason">
                {{ role.reason }}
              </div>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-3">
              <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                <v-icon
                  :icon="role.icon || 'mdi-briefcase'"
                  color="grey-darken-1"
                  size="18"
                ></v-icon>
              </v-avatar>
              <div>
                <div class="text-body-2 font-weight-medium text-grey-darken-2">
                  角色ID: {{ role.id }}
                </div>
                <div class="text-caption text-grey-darken-1">
                  {{ role.createdAt || '未知时间' }}
                </div>
              </div>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <div class="mb-3">
                <div class="text-h6 font-weight-bold text-grey-darken-3 mb-2">
                  {{ role.name || '角色名称' }}
                  <v-chip
                    v-if="role.price"
                    variant="flat"
                    color="green-lighten-4"
                    size="small"
                    rounded="lg"
                    class="ml-2"
                  >
                    $ {{ role.price }}
                  </v-chip>
                </div>
                <div class="text-body-2 text-grey-darken-1 my-6">
                  {{ role.description || '暂无描述' }}
                </div>
              </div>

              <!-- 技能要求 -->
              <div v-if="role.skills" class="mb-3 d-flex align-center">
                <div class="text-caption text-grey-darken-1 mr-2">技能：</div>
                <div class="d-flex flex-wrap ga-2">
                  <v-chip
                    v-for="skill in getSkillsArray(role.skills)"
                    :key="skill"
                    variant="flat"
                    color="grey-lighten-3"
                    size="small"
                    rounded="lg"
                  >
                    {{ skill }}
                  </v-chip>
                </div>
              </div>

              <!-- 分类信息 -->
              <div
                v-if="role.mainCategory !== undefined || role.subCategory !== undefined"
                class="mb-3 d-flex align-center"
              >
                <div class="text-caption text-grey-darken-1 mr-2">分类：</div>
                <div class="d-flex ga-2">
                  <v-chip
                    v-if="role.mainCategory !== undefined"
                    variant="tonal"
                    color="purple-lighten-1"
                    size="small"
                    rounded="lg"
                  >
                    <v-icon icon="mdi-folder" size="14" class="mr-1"></v-icon>
                    {{ getCategoryName(role.mainCategory) }}
                  </v-chip>
                  <v-chip
                    v-if="role.subCategory !== undefined"
                    variant="tonal"
                    color="orange-darken-4"
                    size="small"
                    rounded="lg"
                  >
                    <v-icon icon="mdi-folder-outline" size="14" class="mr-1"></v-icon>
                    {{ getSubCategoryName(role.mainCategory, role.subCategory) }}
                  </v-chip>
                </div>
              </div>

              <!-- 创建者信息 -->
              <div class="d-flex align-center text-caption text-grey-darken-1">
                <v-icon icon="mdi-account-outline" size="14" class="mr-1"></v-icon>
                创建者ID: {{ role.creator || '未知' }}
                <span class="mx-3">|</span>
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                更新时间: {{ role.updatedAt || '未知' }}
              </div>
            </div>
          </div>
        </div>
      </v-card>

      <!-- 加载状态提示 -->
      <div v-if="loading && roleList.length > 0" class="text-center py-4">
        <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
        <p class="mt-2 text-body-2 text-grey-darken-1">加载更多中...</p>
      </div>

      <!-- 没有更多数据提示 -->
      <div v-else-if="!hasMoreData && roleList.length > 0" class="text-center py-4">
        <p class="text-body-2 text-grey-darken-1">已加载全部数据</p>
      </div>
    </div>

    <!-- 拒绝/屏蔽对话框 -->
    <RejectBanDialog
      v-model="showReasonDialog"
      :type="dialogType"
      :item-name="currentRole?.name || ''"
      :item-state="currentRole?.state"
      item-type="角色"
      :loading="submitting"
      @confirm="handleConfirmAction"
    />

    <!-- 编辑角色对话框 -->
    <v-dialog v-model="showEditDialog" max-width="700px" persistent>
      <v-card rounded="lg">
        <v-card-title class="text-h6 font-weight-bold pa-6 pb-4">
          <v-icon icon="mdi-pencil-outline" color="blue-darken-2" class="mr-3"></v-icon>
          编辑角色信息
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <v-form ref="editForm" v-model="editFormValid">
            <v-row>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model="editRole.name"
                  label="角色名称"
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
              label="角色描述"
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
              <div class="text-body-2 font-weight-medium mb-2">角色分类</div>
              <CategorySelector
                v-model:model-main-category="editRole.mainCategory"
                v-model:model-sub-category="editRole.subCategory"
              />
            </div>

            <!-- 图标输入 -->
            <v-text-field
              v-model="editRole.icon"
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
            color="blue-lighten-4"
            rounded="lg"
            :disabled="!editFormValid"
            :loading="updating"
            @click="updateRole"
          >
            <v-icon icon="mdi-content-save" color="blue-darken-2" class="mr-2"></v-icon>
            保存修改
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.profession-management {
  padding: 0;
}

/* 状态和操作区域最小宽度 */
.status-actions-area {
  min-width: 200px;
}

/* 卡片悬停效果 */
:deep(.v-card[hover]:hover) {
  transform: translateY(-2px);
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
}

/* 边框样式 */
.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

/* 芯片组样式优化 */
:deep(.v-chip-group) {
  column-gap: 8px;
  row-gap: 8px;
}

/* 拒绝原因文本换行 */
.rejection-reason {
  word-wrap: break-word;
  word-break: break-word;
  white-space: normal;
  line-height: 1.4;
  max-width: 180px;
}
</style>
