<script setup lang="ts">
import { ref } from 'vue'
import { adminApi } from '@/api'
import { useMutation } from '@/composables'

// 根节点内容生成
const rootIdType = ref<'course' | 'node'>('course')
const rootNodeId = ref<string>('')
const rootContentType = ref<'auto' | 'index' | 'article'>('auto')
const rootEnqueueChildren = ref<boolean>(true)

// 单节点生成
const nodeId = ref<string>('')
const nodeContentType = ref<'auto' | 'index' | 'article'>('auto')

// 生成根节点内容
const { execute: generateRootContent, loading: generatingRoot } = useMutation(
  async () => {
    // TODO: 调用根节点内容生成接口
    // 目前使用 enqueue 接口作为占位
    const id = parseInt(rootNodeId.value, 10)
    return adminApi.enqueueAutoAuthorNode(id)
  },
  {
    successMessage: '根节点内容生成已启动',
    onSuccess: () => {
      rootNodeId.value = ''
    },
  }
)

const startGenerateRoot = () => {
  if (!rootNodeId.value) {
    return
  }
  generateRootContent()
}

// 生成单节点内容
const { execute: generateNodeContent, loading: generatingNode } = useMutation(
  async () => {
    // TODO: 调用单节点生成接口
    // 目前使用 enqueue 接口作为占位
    const id = parseInt(nodeId.value, 10)
    return adminApi.enqueueAutoAuthorNode(id)
  },
  {
    successMessage: '节点内容生成已启动',
    onSuccess: () => {
      nodeId.value = ''
    },
  }
)

const startGenerateNode = () => {
  if (!nodeId.value) {
    return
  }
  generateNodeContent()
}

// 查漏补缺
const { execute: scanNodes, loading: scanningNodes } = useMutation(adminApi.scanAutoAuthorNodes, {
  successMessage: '扫描已开始，正在查找需要生成内容的节点',
})
</script>

<template>
  <div class="content-generator">
    <h2 class="text-h5 font-weight-bold mb-6">节点内容生成</h2>

    <!-- 根节点内容生成 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-file-tree" class="mr-2"></v-icon>
        根节点内容生成
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">
          为指定的课程或节点生成内容，可选择让 AI 自动判断生成目录或文章，生成目录时会递归创建子节点
        </p>


        <v-row class="mt-2">
          <v-col cols="12" md="6">
            <v-text-field
              v-model="rootNodeId"
              :label="rootIdType === 'course' ? '课程 ID' : '节点 ID'"
              type="number"
              variant="outlined"
              density="compact"
              hide-details
              :placeholder="`输入${rootIdType === 'course' ? '课程' : '节点'} ID`"
            ></v-text-field>
          </v-col>
          <v-col cols="12" md="6">
            <v-radio-group v-model="rootIdType" inline hide-details>
              <v-radio label="课程" value="course"></v-radio>
              <v-radio label="节点" value="node"></v-radio>
            </v-radio-group>
          </v-col>
        </v-row>

        <v-row class="mt-2">
          <v-col cols="12" md="6">
            <v-radio-group v-model="rootContentType" inline label="内容类型" hide-details>
              <v-radio label="自动判断" value="auto"></v-radio>
              <v-radio label="目录" value="index"></v-radio>
              <v-radio label="文章" value="article"></v-radio>
            </v-radio-group>
          </v-col>
        </v-row>

        <v-row class="mt-2">
          <v-col cols="12">
            <v-checkbox
              v-model="rootEnqueueChildren"
              label="生成目录时，将子节点加入队列"
              hide-details
              density="compact"
            ></v-checkbox>
          </v-col>
        </v-row>

        <div class="mt-4">
          <v-btn
            variant="tonal"
            :loading="generatingRoot"
            :disabled="!rootNodeId"
            @click="startGenerateRoot"
          >
            <v-icon icon="mdi-play" class="mr-2"></v-icon>
            提交节点
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <!-- 单节点生成 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-file-document-outline" class="mr-2"></v-icon>
        单节点内容生成
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">为指定节点生成内容，不会递归创建子节点</p>

        <v-row>
          <v-col cols="12" md="6">
            <v-text-field
              v-model="nodeId"
              label="节点 ID"
              type="number"
              variant="outlined"
              density="compact"
              hide-details
              placeholder="输入节点 ID"
            ></v-text-field>
          </v-col>
        </v-row>

        <v-row class="mt-2">
          <v-col cols="12" md="6">
            <v-radio-group v-model="nodeContentType" inline label="内容类型" hide-details>
              <v-radio label="自动判断" value="auto"></v-radio>
              <v-radio label="目录" value="index"></v-radio>
              <v-radio label="文章" value="article"></v-radio>
            </v-radio-group>
          </v-col>
        </v-row>

        <div class="mt-4">
          <v-btn
            variant="tonal"
            :loading="generatingNode"
            :disabled="!nodeId"
            @click="startGenerateNode"
          >
            <v-icon icon="mdi-play" class="mr-2"></v-icon>
            提交节点
          </v-btn>
        </div>
      </v-card-text>
    </v-card>

    <!-- 查漏补缺 -->
    <v-card flat class="border mb-4">
      <v-card-title class="d-flex align-center">
        <v-icon icon="mdi-radar" class="mr-2"></v-icon>
        查漏补缺
      </v-card-title>
      <v-card-text>
        <p class="text-body-2 text-grey mb-4">扫描所有没有内容的节点，批量加入生成队列</p>
        <v-btn variant="tonal" :loading="scanningNodes" @click="scanNodes">
          <v-icon icon="mdi-radar" class="mr-2"></v-icon>
          开始扫描
        </v-btn>
      </v-card-text>
    </v-card>
  </div>
</template>

<style scoped>
.content-generator {
  max-width: 100%;
  padding: 0;
}

.border {
  border: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.border-b {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}
</style>

