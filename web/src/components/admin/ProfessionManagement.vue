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
      <v-chip-group v-model="selectedStateIndex" color="primary" variant="flat" @update:model-value="onStateChange" mandatory>
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
          <div class="mr-4" style="min-width: 200px;">
            <div class="mb-3">
              <v-chip
                :color="getStateConfig(profession.state).color"
                variant="flat"
                rounded="lg"
                size="small"
              >
                <v-icon :icon="getStateConfig(profession.state).icon" size="14" class="mr-1"></v-icon>
                {{ getStateConfig(profession.state).text }}
              </v-chip>
            </div>
            
            <!-- 审核操作按钮 -->
            <div class="d-flex flex-column ga-2">
              <!-- 通过按钮 - 只在待审核状态显示 -->
              <v-btn
                v-if="profession.state === APPROVAL_STATE.SUBMITTED"
                variant="flat"
                color="green-lighten-4"
                rounded="lg"
                size="small"
                @click="approveProfession(profession)"
                :loading="profession.approving"
              >
                <v-icon icon="mdi-check" color="green-darken-2" size="16" class="mr-1"></v-icon>
                通过
              </v-btn>
              
              <!-- 拒绝按钮 - 待审核和已通过状态都显示 -->
              <v-btn
                v-if="profession.state === APPROVAL_STATE.SUBMITTED || profession.state === APPROVAL_STATE.APPROVED"
                variant="flat"
                color="red-lighten-4"
                rounded="lg"
                size="small"
                @click="showRejectModal(profession)"
              >
                <v-icon icon="mdi-close" color="red-darken-2" size="16" class="mr-1"></v-icon>
                {{ profession.state === APPROVAL_STATE.APPROVED ? '撤销通过' : '拒绝' }}
              </v-btn>
              
              <!-- 恢复按钮 - 只在已拒绝状态显示，实际是重新通过 -->
              <v-btn
                v-if="profession.state === APPROVAL_STATE.REJECTED"
                variant="flat"
                color="orange-lighten-4"
                rounded="lg"
                size="small"
                @click="restoreProfession(profession)"
                :loading="profession.restoring"
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
                @click="showDeleteConfirm(profession)"
                :loading="profession.deleting"
              >
                <v-icon icon="mdi-delete" color="grey-darken-2" size="16" class="mr-1"></v-icon>
                删除
              </v-btn>
            </div>

            <!-- 拒绝原因显示 -->
            <div v-if="profession.state === APPROVAL_STATE.REJECTED && profession.rejectedReason" class="mt-3">
              <div class="text-caption text-grey-darken-1 mb-1">拒绝原因：</div>
              <div class="text-body-2 text-red-darken-2 rejection-reason">{{ profession.rejectedReason }}</div>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="flex-grow-1">
            <div class="d-flex align-center mb-3">
              <v-avatar size="32" color="grey-lighten-3" class="mr-3">
                <v-icon :icon="profession.icon || 'mdi-briefcase'" color="grey-darken-1" size="18"></v-icon>
              </v-avatar>
              <div>
                <div class="text-body-2 font-weight-medium text-grey-darken-2">
                  职业ID: {{ profession.id }}
                  
                </div>
                <div class="text-caption text-grey-darken-1">{{ profession.createdAt || '未知时间' }}</div>
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
                <div class="text-body-2 text-grey-darken-1 my-6">{{ profession.description || '暂无描述' }}</div>
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
              <div v-if="profession.mainCategory !== undefined || profession.subCategory !== undefined" class="mb-3 d-flex align-center">
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
          {{ currentProfession?.state === APPROVAL_STATE.APPROVED ? '撤销职业通过' : '拒绝职业申请' }}
        </v-card-title>
        
        <v-card-text class="pa-6 pt-0">
          <div class="mb-4">
            <div class="text-body-2 text-grey-darken-1 mb-2">
              职业名称：<strong>{{ currentProfession?.name }}</strong>
            </div>
            <div v-if="currentProfession?.state === APPROVAL_STATE.APPROVED" class="text-body-2 text-orange-darken-2 mb-2">
              <v-icon icon="mdi-alert" size="16" class="mr-1"></v-icon>
              注意：此职业已通过审核，撤销后将变为拒绝状态
            </div>
          </div>

          <div class="mb-4">
            <div class="text-body-2 font-weight-medium mb-2">
              {{ currentProfession?.state === APPROVAL_STATE.APPROVED ? '选择撤销原因：' : '选择拒绝原因：' }}
            </div>
            <v-chip-group v-model="selectedRejectReason" color="red-lighten-3" variant="flat">
              <v-chip
                v-for="reason in (currentProfession?.state === APPROVAL_STATE.APPROVED ? revokeReasons : rejectReasons)"
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
            :label="currentProfession?.state === APPROVAL_STATE.APPROVED ? '撤销原因' : '拒绝原因'"
            :placeholder="currentProfession?.state === APPROVAL_STATE.APPROVED ? '请详细说明撤销通过的原因...' : '请详细说明拒绝的原因...'"
            variant="outlined"
            rows="4"
            rounded="lg"
            bg-color="grey-lighten-5"
          ></v-textarea>
        </v-card-text>

        <v-card-actions class="pa-6 pt-0">
          <v-spacer></v-spacer>
          <v-btn
            variant="outlined"
            color="grey"
            rounded="lg"
            @click="closeRejectDialog"
          >
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="red-lighten-4"
            rounded="lg"
            @click="rejectProfession"
            :disabled="!rejectReason.trim()"
            :loading="rejecting"
          >
            <v-icon icon="mdi-close" color="red-darken-2" class="mr-2"></v-icon>
            {{ currentProfession?.state === APPROVAL_STATE.APPROVED ? '确认撤销' : '确认拒绝' }}
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
                  variant="outlined"
                  rounded="lg"
                  bg-color="grey-lighten-5"
                  :rules="[v => !!v || '职业名称不能为空']"
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
              variant="outlined"
              rounded="lg"
              bg-color="grey-lighten-5"
              rows="4"
              :rules="[v => !!v || '职业描述不能为空']"
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
              v-if="editProfession.state === APPROVAL_STATE.REJECTED"
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
          <v-btn
            variant="outlined"
            color="grey"
            rounded="lg"
            @click="closeEditDialog"
          >
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="blue-lighten-4"
            rounded="lg"
            @click="updateProfession"
            :disabled="!editFormValid"
            :loading="updating"
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
          <v-btn
            variant="outlined"
            color="grey"
            rounded="lg"
            @click="closeDeleteDialog"
          >
            取消
          </v-btn>
          <v-btn
            variant="flat"
            color="red-lighten-4"
            rounded="lg"
            @click="deleteProfession"
            :loading="deleting"
          >
            <v-icon icon="mdi-delete" color="red-darken-2" class="mr-2"></v-icon>
            确认删除
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { professionServiceV1 } from '@/services/api/v1/apiServiceV1';
import { learnService } from '@/services/learnService'; // TODO: 临时保留，用于未迁移的接口
import { APPROVAL_STATE, APPROVAL_STATE_TEXT, getApprovalStateClass } from '@/constants/statusConstants';
import CategorySelector from '../CategorySelector.vue';

// 响应式数据
const professionList = ref([]);
const loading = ref(false);
const lastId = ref(0);
const hasMoreData = ref(true);
const selectedStateIndex = ref(0);
const rejectReason = ref('');
const selectedRejectReason = ref('');
const showRejectDialog = ref(false);
const currentProfession = ref(null);
const rejecting = ref(false);

// 编辑相关数据
const showEditDialog = ref(false);
const editProfession = ref({});
const editFormValid = ref(false);
const updating = ref(false);

// 删除相关数据
const showDeleteDialog = ref(false);
const deleting = ref(false);

// 动态类别数据
const mainCategories = ref([]);
const categoryMapping = ref([]);

// 动态类别功能函数
const getCategoryName = (mainCategoryId) => {
  if (!mainCategories.value || !mainCategoryId) return '未知类别';
  const category = mainCategories.value.find(cat => cat.id === mainCategoryId);
  return category ? category.name : '未知类别';
};

const getSubCategoryName = (mainCategoryId, subCategoryId) => {
  if (!categoryMapping.value || !mainCategoryId || !subCategoryId) return '未知子类别';
  const mapping = categoryMapping.value.find(m => m.mainCategoryId === mainCategoryId);
  if (!mapping) return '未知子类别';
  const subCategory = mapping.subCategories.find(sub => sub.id === subCategoryId);
  return subCategory ? subCategory.name : '未知子类别';
};

// 加载职业类别数据
const loadProfessionCategories = async () => {
  try {
    const data = await learnService.getProfessionCategories(); // TODO: 需要迁移到V1
    mainCategories.value = data.mainCategories || [];
    categoryMapping.value = data.categoryMapping || [];
  } catch (error) {
    console.error('加载职业类别失败:', error);
    // 保持空数组作为默认值
  }
};

// 状态选项
const stateOptions = [
  { value: APPROVAL_STATE.SUBMITTED, text: APPROVAL_STATE_TEXT[APPROVAL_STATE.SUBMITTED], color: 'orange-lighten-4', icon: 'mdi-clock-outline' },
  { value: APPROVAL_STATE.APPROVED, text: APPROVAL_STATE_TEXT[APPROVAL_STATE.APPROVED], color: 'green-lighten-4', icon: 'mdi-check-circle' },
  { value: APPROVAL_STATE.REJECTED, text: APPROVAL_STATE_TEXT[APPROVAL_STATE.REJECTED], color: 'red-lighten-4', icon: 'mdi-close-circle' }
];

// 预设拒绝理由
const rejectReasons = [
  '职业描述不够详细',
  '技能要求不明确',
  '重复申请',
  '不符合平台定位',
  '信息不完整'
];

// 预设撤销理由
const revokeReasons = [
  '发现信息有误',
  '用户投诉举报',
  '不符合最新政策',
  '重复职业',
  '质量不达标',
  '其他管理原因'
];

// 获取当前选中的状态
const getCurrentState = () => stateOptions[selectedStateIndex.value]?.value || APPROVAL_STATE.SUBMITTED;
const getCurrentStateText = () => stateOptions[selectedStateIndex.value]?.text || APPROVAL_STATE_TEXT[APPROVAL_STATE.SUBMITTED];

// 根据状态获取配置
const getStateConfig = (state) => {
  return stateOptions.find(option => option.value === state) || stateOptions[0];
};

// 处理技能数组
const getSkillsArray = (skills) => {
  if (!skills) return [];
  if (typeof skills === 'string') {
    return skills.split(',').map(s => s.trim()).filter(s => s);
  }
  return Array.isArray(skills) ? skills : [];
};

// 获取主分类名称
// 获取职业申请列表
const loadProfessionList = async (reset = true) => {
  if (loading.value) return;
  
  try {
    loading.value = true;
    
    const currentLastId = reset ? 0 : lastId.value;
    const state = getCurrentState();
    const response = await professionServiceV1.getProfessions(1, 20, state, currentLastId);
    
    if (response.code === 200 && response.data) {
      const newData = Array.isArray(response.data) ? response.data : [];
      
      if (reset) {
        professionList.value = newData;
        lastId.value = 0;
      } else {
        professionList.value = [...professionList.value, ...newData];
      }
      
      // 更新分页信息
      if (newData.length > 0) {
        lastId.value = newData[newData.length - 1].id;
        hasMoreData.value = newData.length >= 20; // 假设每页20条
      } else {
        hasMoreData.value = false;
      }
    } else {
      console.error('获取职业申请列表失败:', response.message);
      if (reset) {
        professionList.value = [];
      }
    }
  } catch (error) {
    console.error('获取职业申请列表错误:', error);
    if (reset) {
      professionList.value = [];
    }
  } finally {
    loading.value = false;
  }
};

// 加载更多数据
const loadMoreData = () => {
  if (!loading.value && hasMoreData.value) {
    loadProfessionList(false);
  }
};

// 滚动监听处理（带节流）
let isScrollLoading = false;
const handleScroll = () => {
  // 防止重复触发
  if (isScrollLoading || loading.value || !hasMoreData.value) {
    return;
  }
  
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
  const windowHeight = window.innerHeight;
  const documentHeight = document.documentElement.scrollHeight;
  
  // 当滚动到距离底部100px时触发加载
  if (scrollTop + windowHeight >= documentHeight - 100) {
    isScrollLoading = true;
    loadMoreData();
    // 延迟重置标志，避免过于频繁触发
    setTimeout(() => {
      isScrollLoading = false;
    }, 500);
  }
};

// 状态改变处理
const onStateChange = () => {
  loadProfessionList(true);
};

// 操作职业申请
const operateProfession = async (profession, action, reason = '') => {
  try {
    const response = await professionServiceV1.approveProfession(profession.id, action, reason);
    
    if (response.code === 200) {
      // 更新本地数据
      const index = professionList.value.findIndex(p => p.id === profession.id);
      if (index !== -1) {
        if (action === 'APPROVE') {
          professionList.value[index].state = APPROVAL_STATE.APPROVED;
          professionList.value[index].rejectedReason = ''; // 清空拒绝原因
        } else if (action === 'REJECT') {
          professionList.value[index].state = APPROVAL_STATE.REJECTED;
          professionList.value[index].rejectedReason = reason;
        }
      }
      
      // 如果当前筛选状态与操作结果不匹配，从列表中移除
      const currentState = getCurrentState();
      if ((action === 'APPROVE' && currentState !== APPROVAL_STATE.APPROVED) ||
          (action === 'REJECT' && currentState !== APPROVAL_STATE.REJECTED)) {
        professionList.value = professionList.value.filter(p => p.id !== profession.id);
      }
      
      return true;
    } else {
      console.error('操作失败:', response.message);
      return false;
    }
  } catch (error) {
    console.error('操作错误:', error);
    return false;
  }
};

// 通过申请
const approveProfession = async (profession) => {
  profession.approving = true;
  const success = await operateProfession(profession, 'APPROVE');
  profession.approving = false;
  
  if (success) {
    // 可以添加成功提示
  }
};

// 恢复申请 - 将已拒绝的职业重新通过
const restoreProfession = async (profession) => {
  // 显示确认对话框
  const confirmed = confirm(`确定要通过职业申请"${profession.name}"吗？\n通过后该申请将变为已通过状态。`);
  if (!confirmed) {
    return;
  }
  
  profession.restoring = true;
  const success = await operateProfession(profession, 'APPROVE');
  profession.restoring = false;
  
  if (success) {
    // 可以添加成功提示
  }
};

// 显示拒绝对话框
const showRejectModal = (profession) => {
  currentProfession.value = profession;
  rejectReason.value = '';
  selectedRejectReason.value = '';
  showRejectDialog.value = true;
};

// 关闭拒绝对话框
const closeRejectDialog = () => {
  showRejectDialog.value = false;
  currentProfession.value = null;
  rejectReason.value = '';
  selectedRejectReason.value = '';
};

// 拒绝申请
const rejectProfession = async () => {
  if (!rejectReason.value.trim()) {
    return;
  }
  
  try {
    rejecting.value = true;
    const success = await operateProfession(currentProfession.value, 'REJECT', rejectReason.value);
    if (success) {
      closeRejectDialog();
    }
  } finally {
    rejecting.value = false;
  }
};

// 监听选中的拒绝理由
watch(selectedRejectReason, (newValue) => {
  if (newValue) {
    rejectReason.value = newValue;
  }
});

// 显示编辑对话框
const showEditModal = (profession) => {
  currentProfession.value = profession;
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
    state: profession.state
  };
  
  showEditDialog.value = true;
};

// 关闭编辑对话框
const closeEditDialog = () => {
  showEditDialog.value = false;
  currentProfession.value = null;
  editProfession.value = {};
  editFormValid.value = false;
};

// 更新职业信息
const updateProfession = async () => {
  if (!editFormValid.value) return;
  
  try {
    updating.value = true;
    
    // 准备更新数据
    const updateData = {
      id: editProfession.value.id,
      name: editProfession.value.name,
      description: editProfession.value.description,
      price: editProfession.value.price || '',
      skills: editProfession.value.skillsText || '',
      mainCategory: editProfession.value.mainCategory || 0,
      subCategory: editProfession.value.subCategory || 0,
      icon: editProfession.value.icon || 'mdi-briefcase',
      rejectedReason: editProfession.value.rejectedReason || ''
    };
    
    const response = await professionServiceV1.updateProfession(profession.id, updateData);
    
    if (response.code === 200) {
      // 更新本地数据
      const index = professionList.value.findIndex(p => p.id === editProfession.value.id);
      if (index !== -1) {
        professionList.value[index] = {
          ...professionList.value[index],
          ...updateData
        };
      }
      
      closeEditDialog();
      // 可以添加成功提示
    } else {
      console.error('更新职业信息失败:', response.message);
    }
  } catch (error) {
    console.error('更新职业信息错误:', error);
  } finally {
    updating.value = false;
  }
};

// 显示删除确认对话框
const showDeleteConfirm = (profession) => {
  currentProfession.value = profession;
  showDeleteDialog.value = true;
};

// 关闭删除确认对话框
const closeDeleteDialog = () => {
  showDeleteDialog.value = false;
  currentProfession.value = null;
};

// 删除职业申请
const deleteProfession = async () => {
  if (!currentProfession.value) return;
  
  try {
    deleting.value = true;
    
    const response = await professionServiceV1.deleteProfession(currentProfession.value.id);
    
    if (response.code === 200) {
      // 从本地列表中移除
      professionList.value = professionList.value.filter(p => p.id !== currentProfession.value.id);
      
      closeDeleteDialog();
      // 可以添加成功提示
    } else {
      console.error('删除职业申请失败:', response.message);
    }
  } catch (error) {
    console.error('删除职业申请错误:', error);
  } finally {
    deleting.value = false;
  }
};

// 组件挂载时加载数据和添加滚动监听
onMounted(() => {
  loadProfessionCategories();
  loadProfessionList(true);
  // 添加滚动监听
  window.addEventListener('scroll', handleScroll);
});

// 组件卸载时移除滚动监听
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style scoped>
.profession-management {
  padding: 0;
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
