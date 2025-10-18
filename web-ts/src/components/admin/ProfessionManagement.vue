<template>
  <div class="profession-management">
    <!-- 页面头部 -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-blue-lighten-5 mr-3">
          <v-icon icon="mdi-briefcase-check-outline" color="blue-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">职业申请管理</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">审核和管理用户提交的职业申请</p>
        </div>
      </div>
      <v-chip variant="flat" color="blue-lighten-4" rounded="lg">
        <v-icon icon="mdi-briefcase" color="blue-darken-2" size="16" class="mr-2"></v-icon>
        <span class="text-blue-darken-2 text-caption">{{ professionList.length }}个职业</span>
      </v-chip>
    </div>

    <!-- 状态筛选 -->
    <div class="mb-6">
      <v-chip-group
        v-model="selectedStateIndex"
        color="primary"
        variant="flat"
        mandatory
        @update:model-value="onStateChange"
      >
        <v-chip
          v-for="(state, index) in stateOptions"
          :key="state.value"
          :value="index"
          :color="state.color"
          rounded="lg"
          class="me-2"
        >
          <v-icon :icon="state.icon" size="16" class="mr-1"></v-icon>
          {{ state.text }}
        </v-chip>
      </v-chip-group>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && professionList.length === 0" class="text-center py-8">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
      <p class="mt-3 text-grey-darken-1">加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="professionList.length === 0" class="text-center py-12">
      <v-icon icon="mdi-briefcase-outline" size="48" color="grey-lighten-1" class="mb-4"></v-icon>
      <p class="text-body-1 text-grey-darken-1">暂无{{ getCurrentStateText() }}的职业申请</p>
    </div>

    <!-- 职业申请列表 -->
    <div v-else>
      <v-card
        v-for="profession in professionList"
        :key="profession.id"
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
              <!-- 通过按钮 - 只在待审核状态显示 -->
              <v-btn
                v-if="profession.state === ProfessionState.SUBMITTED"
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                :loading="profession.approving"
                @click="approveProfession(profession)"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                通过
              </v-btn>

              <!-- 拒绝按钮 - 待审核和已通过状态都显示 -->
              <v-btn
                v-if="
                  profession.state === ProfessionState.SUBMITTED ||
                  profession.state === ProfessionState.APPROVED
                "
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="showRejectModal(profession)"
              >
                <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                {{ profession.state === ProfessionState.APPROVED ? '撤销通过' : '拒绝' }}
              </v-btn>

              <!-- 恢复按钮 - 只在已拒绝状态显示，实际是重新通过 -->
              <v-btn
                v-if="profession.state === ProfessionState.REJECTED"
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                :loading="profession.restoring"
                @click="restoreProfession(profession)"
              >
                <v-icon icon="mdi-check" color="orange-darken-2" size="16" class="mr-1"></v-icon>
                重新通过
              </v-btn>
            </div>

            <!-- 通用操作按钮 - 所有状态都显示 -->
            <div class="d-flex flex-column ga-2 mt-3">
              <v-btn
                variant="flat"
                color="blue-lighten-4"
                rounded="lg"
                size="small"
                @click="showEditModal(profession)"
              >
                <v-icon icon="mdi-pencil" color="blue-darken-2" size="16" class="mr-1"></v-icon>
                编辑
              </v-btn>
              <v-btn
                variant="flat"
                color="grey-lighten-3"
                rounded="lg"
                size="small"
                :loading="profession.deleting"
                @click="showDeleteConfirm(profession)"
              >
                <v-icon icon="mdi-delete" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                删除
              </v-btn>
            </div>

            <!-- 拒绝原因显示 -->
            <div
              v-if="profession.state === ProfessionState.REJECTED && profession.rejectedReason"
              class="mt-3"
            >
              <div class="text-caption text-grey-darken-1 mb-1">拒绝原因：</div>
              <div class="text-body-2 text-red-darken-2 rejection-reason">
                {{ profession.rejectedReason }}
              </div>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-3">
              <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                <v-icon
                  :icon="profession.icon || 'mdi-briefcase'"
                  color="grey-darken-1"
                  size="18"
                ></v-icon>
              </v-avatar>
              <div>
                <div class="text-body-2 font-weight-medium text-grey-darken-2">
                  职业ID: {{ profession.id }}
                </div>
                <div class="text-caption text-grey-darken-1">
                  {{ profession.createdAt || '未知时间' }}
                </div>
              </div>
            </div>

            <div class="bg-grey-lighten-5 rounded-lg pa-4">
              <div class="mb-3">
                <div class="text-h6 font-weight-bold text-grey-darken-3 mb-2">
                  {{ profession.name || '职业名称' }}
                  <v-chip
                    v-if="profession.price"
                    variant="flat"
                    color="green-lighten-4"
                    size="small"
                    rounded="lg"
                    class="ml-2"
                  >
                    $ {{ profession.price }}
                  </v-chip>
                </div>
                <div class="text-body-2 text-grey-darken-1 my-6">
                  {{ profession.description || '暂无描述' }}
                </div>
              </div>

              <!-- 技能要求 -->
              <div v-if="profession.skills" class="mb-3 d-flex align-center">
                <div class="text-caption text-grey-darken-1">技能：</div>
                <div class="d-flex flex-wrap ga-2">
                  <v-chip
                    v-for="skill in getSkillsArray(profession.skills)"
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
                v-if="profession.mainCategory !== undefined || profession.subCategory !== undefined"
                class="mb-3 d-flex align-center"
              >
                <div class="text-caption text-grey-darken-1">分类：</div>
                <div class="d-flex ga-2">
                  <v-chip
                    v-if="profession.mainCategory !== undefined"
                    variant="tonal"
                    color="purple-lighten-1"
                    size="small"
                    rounded="lg"
                  >
                    <v-icon icon="mdi-folder" size="14" class="mr-1"></v-icon>
                    {{ getCategoryName(profession.mainCategory) }}
                  </v-chip>
                  <v-chip
                    v-if="profession.subCategory !== undefined"
                    variant="tonal"
                    color="orange-darken-4"
                    size="small"
                    rounded="lg"
                  >
                    <v-icon icon="mdi-folder-outline" size="14" class="mr-1"></v-icon>
                    {{ getSubCategoryName(profession.mainCategory, profession.subCategory) }}
                  </v-chip>
                </div>
              </div>

              <!-- 创建者信息 -->
              <div class="d-flex align-center text-caption text-grey-darken-1">
                <v-icon icon="mdi-account-outline" size="14" class="mr-1"></v-icon>
                创建者ID: {{ profession.creator || '未知' }}
                <span class="mx-3">|</span>
                <v-icon icon="mdi-clock-outline" size="14" class="mr-1"></v-icon>
                更新时间: {{ profession.updatedAt || '未知' }}
              </div>
            </div>
          </div>
        </div>
      </v-card>

      <!-- 加载状态提示 -->
      <div v-if="loading && professionList.length > 0" class="text-center py-4">
        <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
        <p class="mt-2 text-body-2 text-grey-darken-1">加载更多中...</p>
      </div>

      <!-- 没有更多数据提示 -->
      <div v-else-if="!hasMoreData && professionList.length > 0" class="text-center py-4">
        <p class="text-body-2 text-grey-darken-1">已加载全部数据</p>
      </div>
    </div>

    <!-- 拒绝申请对话框 -->
    <v-dialog v-model="showRejectDialog" max-width="500px" persistent>
      <v-card rounded="lg">
        <v-card-title class="text-h6 font-weight-bold pa-6 pb-4">
          <v-icon icon="mdi-close-circle-outline" color="red-darken-2" class="mr-3"></v-icon>
          {{
            currentProfession?.state === ProfessionState.APPROVED ? '撤销职业通过' : '拒绝职业申请'
          }}
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <div class="mb-4">
            <div class="text-body-2 text-grey-darken-1 mb-2">
              职业名称：<strong>{{ currentProfession?.name }}</strong>
            </div>
            <div
              v-if="currentProfession?.state === ProfessionState.APPROVED"
              class="text-body-2 text-orange-darken-2 mb-2"
            >
              <v-icon icon="mdi-alert" size="16" class="mr-1"></v-icon>
              注意：此职业已通过审核，撤销后将变为拒绝状态
            </div>
          </div>

          <div class="mb-4">
            <div class="text-body-2 font-weight-medium mb-2">
              {{
                currentProfession?.state === ProfessionState.APPROVED
                  ? '选择撤销原因：'
                  : '选择拒绝原因：'
              }}
            </div>
            <v-chip-group v-model="selectedRejectReason" color="red-lighten-3" variant="flat">
              <v-chip
                v-for="reason in currentProfession?.state === ProfessionState.APPROVED
                  ? revokeReasons
                  : rejectReasons"
                :key="reason"
                :value="reason"
                rounded="lg"
                size="small"
                class="mb-2"
              >
                {{ reason }}
              </v-chip>
            </v-chip-group>
          </div>

          <v-textarea
            v-model="rejectReason"
            :label="
              currentProfession?.state === ProfessionState.APPROVED ? '撤销原因' : '拒绝原因'
            "
            :placeholder="
              currentProfession?.state === ProfessionState.APPROVED
                ? '请详细说明撤销通过的原因...'
                : '请详细说明拒绝的原因...'
            "
            variant="outlined"
            rows="4"
            rounded="lg"
            bg-color="grey-lighten-5"
          ></v-textarea>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn variant="outlined" color="grey" rounded="lg" @click="closeRejectDialog">
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="red-lighten-4"
            rounded="lg"
            :disabled="!rejectReason.trim()"
            :loading="rejecting"
            @click="rejectProfession"
          >
            <v-icon icon="mdi-close" color="red-darken-2" class="mr-2"></v-icon>
            {{ currentProfession?.state === ProfessionState.APPROVED ? '确认撤销' : '确认拒绝' }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 编辑职业对话框 -->
    <v-dialog v-model="showEditDialog" max-width="700px" persistent>
      <v-card rounded="lg">
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
              v-if="editProfession.state === ProfessionState.REJECTED"
              v-model="editProfession.rejectedReason"
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
            @click="updateProfession"
          >
            <v-icon icon="mdi-content-save" color="blue-darken-2" class="mr-2"></v-icon>
            保存修改
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 删除确认对话框 -->
    <v-dialog v-model="showDeleteDialog" max-width="400px" persistent>
      <v-card rounded="lg">
        <v-card-title class="text-h6 font-weight-bold pa-6 pb-4">
          <v-icon icon="mdi-delete-outline" color="red-darken-2" class="mr-3"></v-icon>
          确认删除
        </v-card-title>

        <v-card-text class="pa-6 pt-0">
          <div class="text-body-1 mb-4">
            确定要删除职业 <strong>"{{ currentProfession?.name }}"</strong> 吗？
          </div>
          <div class="text-body-2 text-red-darken-2">
            <v-icon icon="mdi-alert" size="16" class="mr-1"></v-icon>
            此操作不可撤销，请谨慎操作。
          </div>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn variant="outlined" color="grey" rounded="lg" @click="closeDeleteDialog">
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="red-lighten-4"
            rounded="lg"
            :loading="deleting"
            @click="deleteProfession"
          >
            <v-icon icon="mdi-delete" color="red-darken-2" class="mr-2"></v-icon>
            确认删除
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { professionServiceV1, systemServiceV1 } from '@/services/api/v1/apiServiceV1'
import { ProfessionState } from '@/types/enums'
import type { Profession, ProfessionCategory, CategoryMapping } from '@/types/profession'
import type { StateOption } from '@/types/common'
import CategorySelector from '../common/CategorySelector.vue'
import { professionNameRules, professionDescriptionRules, categoryRules } from '@/utils/validationRules'
import { PROFESSION_VALIDATION } from '@/types/validation'

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
  rejectedReason?: string
  state?: number
}

// 响应式数据
const professionList = ref<ProfessionWithUIState[]>([])
const loading = ref<boolean>(false)
const lastId = ref<number>(0)
const hasMoreData = ref<boolean>(true)
const selectedStateIndex = ref<number>(0)
const rejectReason = ref<string>('')
const selectedRejectReason = ref<string>('')
const showRejectDialog = ref<boolean>(false)
const currentProfession = ref<ProfessionWithUIState | null>(null)
const rejecting = ref<boolean>(false)

// 编辑相关数据
const showEditDialog = ref<boolean>(false)
const editProfession = ref<EditProfessionForm>({})
const editFormValid = ref<boolean>(false)
const updating = ref<boolean>(false)
const editForm = ref(null)

// 删除相关数据
const showDeleteDialog = ref<boolean>(false)
const deleting = ref<boolean>(false)

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
    const response = await systemServiceV1.getProfessionCategories()
    if (response.code === 200 && response.data) {
      mainCategories.value = response.data.mainCategories || []
      categoryMapping.value = response.data.categoryMapping || []
    }
  } catch (error) {
    console.error('加载职业类别失败:', error)
    // 保持空数组作为默认值
  }
}

// 状态选项
const stateOptions: StateOption[] = [
  {
    value: ProfessionState.SUBMITTED,
    text: '待审核',
    color: 'orange-lighten-4',
    icon: 'mdi-clock-outline',
  },
  {
    value: ProfessionState.APPROVED,
    text: '已批准',
    color: 'green-lighten-4',
    icon: 'mdi-check-circle',
  },
  {
    value: ProfessionState.REJECTED,
    text: '已拒绝',
    color: 'red-lighten-4',
    icon: 'mdi-close-circle',
  },
]

// 预设拒绝理由
const rejectReasons: string[] = [
  '职业描述不够详细',
  '技能要求不明确',
  '重复申请',
  '不符合平台定位',
  '信息不完整',
]

// 预设撤销理由
const revokeReasons: string[] = [
  '发现信息有误',
  '用户投诉举报',
  '不符合最新政策',
  '重复职业',
  '质量不达标',
  '其他管理原因',
]

// 获取当前选中的状态
const getCurrentState = (): number =>
  stateOptions[selectedStateIndex.value]?.value || ProfessionState.SUBMITTED
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

// 获取职业申请列表
const loadProfessionList = async (reset: boolean = true): Promise<void> => {
  if (loading.value) return

  try {
    loading.value = true

    const currentLastId = reset ? null : lastId.value
    const state = getCurrentState()
    const response = await professionServiceV1.getAdminProfessions(state, currentLastId)

    if (response.code === 200 && response.data) {
      const newData = Array.isArray(response.data) ? response.data : []

      if (reset) {
        professionList.value = newData
        lastId.value = 0
      } else {
        professionList.value = [...professionList.value, ...newData]
      }

      // 更新分页信息
      if (newData.length > 0) {
        lastId.value = newData[newData.length - 1].id
        hasMoreData.value = newData.length >= 20 // 假设每页20条
      } else {
        hasMoreData.value = false
      }
    } else {
      console.error('获取职业申请列表失败:', response.message)
      if (reset) {
        professionList.value = []
      }
    }
  } catch (error) {
    console.error('获取职业申请列表错误:', error)
    if (reset) {
      professionList.value = []
    }
  } finally {
    loading.value = false
  }
}

// 加载更多数据
const loadMoreData = (): void => {
  if (!loading.value && hasMoreData.value) {
    loadProfessionList(false)
  }
}

// 滚动监听处理（带节流）
let isScrollLoading = false
const handleScroll = (): void => {
  // 防止重复触发
  if (isScrollLoading || loading.value || !hasMoreData.value) {
    return
  }

  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight

  // 当滚动到距离底部100px时触发加载
  if (scrollTop + windowHeight >= documentHeight - 100) {
    isScrollLoading = true
    loadMoreData()
    // 延迟重置标志，避免过于频繁触发
    setTimeout(() => {
      isScrollLoading = false
    }, 500)
  }
}

// 状态改变处理
const onStateChange = (): void => {
  loadProfessionList(true)
}

// 操作职业申请
const operateProfession = async (
  profession: ProfessionWithUIState,
  action: string,
  reason: string = ''
): Promise<boolean> => {
  try {
    const response = await professionServiceV1.approveProfession(profession.id, action, reason)

    if (response.code === 200) {
      // 更新本地数据
      const index = professionList.value.findIndex((p) => p.id === profession.id)
      if (index !== -1) {
        if (action === 'APPROVE') {
          professionList.value[index].state = ProfessionState.APPROVED
          professionList.value[index].rejectedReason = '' // 清空拒绝原因
        } else if (action === 'REJECT') {
          professionList.value[index].state = ProfessionState.REJECTED
          professionList.value[index].rejectedReason = reason
        }
      }

      // 如果当前筛选状态与操作结果不匹配，从列表中移除
      const currentState = getCurrentState()
      if (
        (action === 'APPROVE' && currentState !== ProfessionState.APPROVED) ||
        (action === 'REJECT' && currentState !== ProfessionState.REJECTED)
      ) {
        professionList.value = professionList.value.filter((p) => p.id !== profession.id)
      }

      return true
    } else {
      console.error('操作失败:', response.message)
      return false
    }
  } catch (error) {
    console.error('操作错误:', error)
    return false
  }
}

// 通过申请
const approveProfession = async (profession: ProfessionWithUIState): Promise<void> => {
  profession.approving = true
  const success = await operateProfession(profession, 'APPROVE')
  profession.approving = false

  if (success) {
    // 可以添加成功提示
  }
}

// 恢复申请 - 将已拒绝的职业重新通过
const restoreProfession = async (profession: ProfessionWithUIState): Promise<void> => {
  profession.restoring = true
  const success = await operateProfession(profession, 'APPROVE')
  profession.restoring = false

  if (success) {
    // 可以添加成功提示
  }
}

// 显示拒绝对话框
const showRejectModal = (profession: ProfessionWithUIState): void => {
  currentProfession.value = profession
  rejectReason.value = ''
  selectedRejectReason.value = ''
  showRejectDialog.value = true
}

// 关闭拒绝对话框
const closeRejectDialog = (): void => {
  showRejectDialog.value = false
  currentProfession.value = null
  rejectReason.value = ''
  selectedRejectReason.value = ''
}

// 拒绝申请
const rejectProfession = async (): Promise<void> => {
  if (!rejectReason.value.trim()) {
    return
  }

  try {
    rejecting.value = true
    const success = await operateProfession(currentProfession.value!, 'REJECT', rejectReason.value)
    if (success) {
      closeRejectDialog()
    }
  } finally {
    rejecting.value = false
  }
}

// 监听选中的拒绝理由
watch(selectedRejectReason, (newValue: string) => {
  if (newValue) {
    rejectReason.value = newValue
  }
})

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
    icon: profession.icon || 'mdi-briefcase',
    rejectedReason: profession.rejectedReason || '',
    state: profession.state,
  }

  showEditDialog.value = true
}

// 关闭编辑对话框
const closeEditDialog = (): void => {
  showEditDialog.value = false
  currentProfession.value = null
  editProfession.value = {}
  editFormValid.value = false
}

// 更新职业信息
const updateProfession = async (): Promise<void> => {
  if (!editFormValid.value) return

  try {
    updating.value = true

    // 准备更新数据
    const updateData = {
      id: editProfession.value.id!,
      name: editProfession.value.name!,
      description: editProfession.value.description!,
      price: editProfession.value.price || '',
      skills: editProfession.value.skillsText || '',
      mainCategory: editProfession.value.mainCategory || 0,
      subCategory: editProfession.value.subCategory || 0,
      icon: editProfession.value.icon || 'mdi-briefcase',
      rejectedReason: editProfession.value.rejectedReason || '',
    }

    const response = await professionServiceV1.updateProfession(
      editProfession.value.id!,
      updateData
    )

    if (response.code === 200) {
      // 更新本地数据
      const index = professionList.value.findIndex((p) => p.id === editProfession.value.id)
      if (index !== -1) {
        professionList.value[index] = {
          ...professionList.value[index],
          ...updateData,
        }
      }

      closeEditDialog()
      // 可以添加成功提示
    } else {
      console.error('更新职业信息失败:', response.message)
    }
  } catch (error) {
    console.error('更新职业信息错误:', error)
  } finally {
    updating.value = false
  }
}

// 显示删除确认对话框
const showDeleteConfirm = (profession: ProfessionWithUIState): void => {
  currentProfession.value = profession
  showDeleteDialog.value = true
}

// 关闭删除确认对话框
const closeDeleteDialog = (): void => {
  showDeleteDialog.value = false
  currentProfession.value = null
}

// 删除职业申请
const deleteProfession = async (): Promise<void> => {
  if (!currentProfession.value) return

  try {
    deleting.value = true

    const response = await professionServiceV1.deleteProfession(currentProfession.value.id)

    if (response.code === 200) {
      // 从本地列表中移除
      professionList.value = professionList.value.filter(
        (p) => p.id !== currentProfession.value!.id
      )

      closeDeleteDialog()
      // 可以添加成功提示
    } else {
      console.error('删除职业申请失败:', response.message)
    }
  } catch (error) {
    console.error('删除职业申请错误:', error)
  } finally {
    deleting.value = false
  }
}

// 组件挂载时加载数据和添加滚动监听
onMounted(() => {
  loadProfessionCategories()
  loadProfessionList(true)
  // 添加滚动监听
  window.addEventListener('scroll', handleScroll)
})

// 组件卸载时移除滚动监听
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

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