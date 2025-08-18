<template>
  <div class="system-configuration">
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <div class="pa-3 rounded-lg bg-teal-lighten-5 mr-3">
          <v-icon icon="mdi-cog-outline" color="teal-darken-1" size="20"></v-icon>
        </div>
        <div>
          <h3 class="text-h6 font-weight-bold text-grey-darken-3">系统配置</h3>
          <p class="text-body-2 text-grey-darken-1 mb-0">管理系统的核心配置参数</p>
        </div>
      </div>
      <v-chip variant="flat" color="green-lighten-4" rounded="lg">
        <v-icon icon="mdi-check-circle" color="green-darken-2" size="16" class="mr-1"></v-icon>
        <span class="text-green-darken-2 text-caption">配置有效</span>
      </v-chip>
    </div>

    <v-row>
      <!-- 课程类别配置 -->
      <v-col cols="12" md="6">
        <v-card flat class="pa-4" rounded="lg" outlined>
          <div class="d-flex align-center mb-4">
            <v-icon icon="mdi-book-outline" color="blue-darken-1" class="mr-2"></v-icon>
            <h4 class="text-h6 font-weight-bold text-grey-darken-3">课程类别配置</h4>
          </div>
          <v-textarea
            v-model="courseCategories"
            label="课程类别 JSON"
            variant="outlined"
            rows="16"
            rounded="lg"
            bg-color="grey-lighten-5"
            placeholder='请输入课程类别的JSON配置...'
            hint="配置课程的分类结构和相关参数"
            persistent-hint
          ></v-textarea>
        </v-card>
      </v-col>

      <!-- 职业类别配置 -->
      <v-col cols="12" md="6">
        <v-card flat class="pa-4" rounded="lg" outlined>
          <div class="d-flex align-center mb-4">
            <v-icon icon="mdi-briefcase-outline" color="orange-darken-1" class="mr-2"></v-icon>
            <h4 class="text-h6 font-weight-bold text-grey-darken-3">职业类别配置</h4>
          </div>
          <v-textarea
            v-model="professionCategories"
            label="职业类别 JSON"
            variant="outlined"
            rows="16"
            rounded="lg"
            bg-color="grey-lighten-5"
            placeholder='请输入职业类别的JSON配置...'
            hint="配置职业的分类结构和相关参数"
            persistent-hint
          ></v-textarea>
        </v-card>
      </v-col>
    </v-row>

    <!-- 合并后的完整配置预览 -->
    <v-card flat class="pa-4 mt-4" rounded="lg" outlined>
      <div class="d-flex align-center mb-4">
        <v-icon icon="mdi-eye-outline" color="purple-darken-1" class="mr-2"></v-icon>
        <h4 class="text-h6 font-weight-bold text-grey-darken-3">完整配置预览</h4>
      </div>
      <v-textarea
        :model-value="mergedConfig"
        label="合并后的完整配置"
        variant="outlined"
        rows="8"
        rounded="lg"
        bg-color="grey-lighten-5"
        readonly
        hint="这是将课程类别和职业类别合并后的完整系统配置"
        persistent-hint
      ></v-textarea>
    </v-card>

    <!-- 操作按钮 -->
    <div class="d-flex align-center justify-space-between mt-6">
      <div>
        <v-btn 
          variant="flat" 
          color="teal" 
          class="mr-3" 
          rounded="lg" 
          @click="saveConfiguration"
          :loading="saving"
          :disabled="!isValidConfig"
        >
          <v-icon icon="mdi-content-save" class="mr-2"></v-icon>
          保存配置
        </v-btn>
        <v-btn 
          variant="flat" 
          color="grey-lighten-3" 
          rounded="lg" 
          @click="formatConfig"
        >
          <v-icon icon="mdi-code-json" class="mr-2"></v-icon>
          格式化
        </v-btn>
      </div>
      <div class="text-caption text-grey-darken-1">
        最后更新：{{ lastUpdateTime }}
      </div>
    </div>

    <!-- 错误提示 -->
    <v-alert
      v-if="configError"
      type="error"
      variant="tonal"
      class="mt-4"
      rounded="lg"
    >
      <div class="font-weight-bold">配置格式错误</div>
      <div class="text-body-2 mt-1">{{ configError }}</div>
    </v-alert>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject, watch } from 'vue';
import { learnService } from '@/services/learnService';

// 响应式数据
const courseCategories = ref('');
const professionCategories = ref('');
const saving = ref(false);
const configError = ref('');
const lastUpdateTime = ref('');

const showSnackbar = inject('showSnackbar');

// 计算属性：合并后的配置
const mergedConfig = computed(() => {
  try {
    const config = {};
    
    // 解析课程类别
    if (courseCategories.value.trim()) {
      const courseData = JSON.parse(courseCategories.value);
      config.courseCategories = courseData;
    }
    
    // 解析职业类别
    if (professionCategories.value.trim()) {
      const professionData = JSON.parse(professionCategories.value);
      config.professionCategories = professionData;
    }
    
    return JSON.stringify(config, null, 2);
  } catch (error) {
    return '{}';
  }
});

// 计算属性：配置是否有效
const isValidConfig = computed(() => {
  try {
    if (courseCategories.value.trim()) {
      JSON.parse(courseCategories.value);
    }
    if (professionCategories.value.trim()) {
      JSON.parse(professionCategories.value);
    }
    configError.value = '';
    return true;
  } catch (error) {
    configError.value = `JSON 格式错误: ${error.message}`;
    return false;
  }
});

// 格式化配置
const formatConfig = () => {
  try {
    if (courseCategories.value.trim()) {
      const parsedCourse = JSON.parse(courseCategories.value);
      courseCategories.value = JSON.stringify(parsedCourse, null, 2);
    }
    
    if (professionCategories.value.trim()) {
      const parsedProfession = JSON.parse(professionCategories.value);
      professionCategories.value = JSON.stringify(parsedProfession, null, 2);
    }
  } catch (error) {
    configError.value = `格式化失败: ${error.message}`;
  }
};

// 保存配置
const saveConfiguration = async () => {
  if (!isValidConfig.value) {
    return;
  }
  
  try {
    saving.value = true;
    
    // 合并配置
    const config = {};
    
    if (courseCategories.value.trim()) {
      config.courseCategories = JSON.parse(courseCategories.value);
    }
    
    if (professionCategories.value.trim()) {
      config.professionCategories = JSON.parse(professionCategories.value);
    }
    
    const response = await learnService.postSystem(JSON.stringify(config));
    
    if (response.code === 200) {
      showSnackbar("配置保存成功！")
      lastUpdateTime.value = new Date().toLocaleString('zh-CN');
      configError.value = '';
    } else {
      configError.value = `保存失败: ${response.message || '未知错误'}`;
    }
  } catch (error) {
    configError.value = `保存失败: ${error.message}`;
  } finally {
    saving.value = false;
  }
};

// 加载配置
const loadConfiguration = async () => {
  try {
    const response = await learnService.getSystem();
    
    if (response.code === 200 && response.data) {
      const data = response.data;
      
      // 分离课程类别和职业类别
      if (data.courseCategories) {
        courseCategories.value = JSON.stringify(data.courseCategories, null, 2);
      }
      
      if (data.professionCategories) {
        professionCategories.value = JSON.stringify(data.professionCategories, null, 2);
      }
      
      // 如果数据结构不是分离的，尝试解析整体配置
      if (!data.courseCategories && !data.professionCategories) {
        // 如果是旧的整体配置格式，可以在这里做兼容处理
        const fullConfig = JSON.stringify(data, null, 2);
        courseCategories.value = fullConfig;
      }
      
      lastUpdateTime.value = new Date().toLocaleString('zh-CN');
    }
  } catch (error) {
    configError.value = `加载配置失败: ${error.message}`;
  }
};

// 监听配置变化，实时验证
watch([courseCategories, professionCategories], () => {
  // 触发计算属性重新计算
  isValidConfig.value;
}, { immediate: true });

// 组件挂载时加载配置
onMounted(() => {
  loadConfiguration();
});
</script>

<style scoped>
.system-configuration {
  padding: 0;
}

/* 卡片边框样式 */
.v-card[outlined] {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

/* 文本域样式优化 */
:deep(.v-textarea .v-field__input) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.4;
}

/* 配置预览区域样式 */
:deep(.v-textarea[readonly] .v-field__input) {
  color: rgba(0, 0, 0, 0.6) !important;
}
</style>
