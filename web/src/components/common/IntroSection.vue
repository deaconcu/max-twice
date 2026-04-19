<template>
  <div class="intro-content" ref="scrollContainer">
    <!-- 第一屏：核心价值主张 -->
    <section class="intro-screen screen-1">
      <div class="screen-inner">
        <div class="tagline">学习平台</div>
        <h1 class="main-title">
          任何知识点<br />
          <span class="highlight">最多看两遍就懂</span>
        </h1>
        <p class="sub-text">同一个知识点，多种讲解方式，最易懂的排最前</p>
        <div class="scroll-indicator">
          <span>滑动探索</span>
          <div class="scroll-line"></div>
        </div>
      </div>
    </section>

    <!-- 第二屏：角色驱动 -->
    <section class="intro-screen screen-2">
      <div class="screen-inner">
        <div class="section-label">01 · 角色驱动</div>
        <h2 class="section-title">你在承担哪些角色？</h2>
        <div class="roles-cloud">
          <span class="role-chip">程序员</span>
          <span class="role-chip">父亲</span>
          <span class="role-chip">投资者</span>
          <span class="role-chip">儿子</span>
          <span class="role-chip">跑者</span>
          <span class="role-chip">丈夫</span>
          <span class="role-chip">厨师</span>
          <span class="role-chip">读书人</span>
          <span class="role-chip">团队领导</span>
          <span class="role-chip">摄影爱好者</span>
          <span class="role-chip">...</span>
        </div>
        <p class="section-hint">每个角色，都值得好好学习</p>
      </div>
    </section>

    <!-- 第三屏：学习路径 -->
    <section class="intro-screen screen-3">
      <div class="screen-inner screen-inner-flow">
        <div class="section-label">02 · 清晰路径</div>
        <h2 class="section-title">知道先学什么，再学什么</h2>

        <!-- 角色 Tab 切换 -->
        <div class="role-tabs">
          <button
            v-for="(role, index) in roles"
            :key="role.id"
            :class="['role-tab', { active: activeRoleIndex === index }]"
            @click="activeRoleIndex = index"
          >
            {{ role.name }}
          </button>
        </div>

        <div class="flow-container">
          <VueFlow
            :key="activeRoleIndex"
            :nodes="currentFlowNodes"
            :edges="currentFlowEdges"
            :nodes-draggable="false"
            :nodes-connectable="false"
            :elements-selectable="false"
            :zoom-on-scroll="false"
            :pan-on-scroll="false"
            :pan-on-drag="false"
            :prevent-scrolling="false"
            fit-view-on-init
            :fit-view-options="{ padding: 0.3 }"
          >
            <template #node-custom="{ data }">
              <div :class="['custom-node', data.type, data.status]">
                <Handle type="target" :position="Position.Left" />
                {{ data.label }}
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
          </VueFlow>
        </div>
        <div class="tree-legend">
          <span class="legend-item"><span class="legend-dot done"></span>已完成</span>
          <span class="legend-item"><span class="legend-dot active"></span>正在学</span>
          <span class="legend-item"><span class="legend-dot locked"></span>待解锁</span>
        </div>
        <p class="section-hint">社区贡献路径，持续优化</p>
      </div>
    </section>

    <!-- 第四屏：课程阅读 -->
    <section class="intro-screen screen-4">
      <div class="screen-inner screen-inner-full">
        <div class="section-label">03 · 课程阅读</div>
        <h2 class="section-title">同一知识点，最易懂的排最前</h2>

        <div class="sketch-container">
          <svg class="sketch-svg" viewBox="-80 0 500 300" preserveAspectRatio="xMidYMid meet">
            <!-- 手绘风格滤镜 -->
            <defs>
              <filter id="sketchy" x="-5%" y="-5%" width="110%" height="110%">
                <feTurbulence type="fractalNoise" baseFrequency="0.03" numOctaves="3" result="noise"/>
                <feDisplacementMap in="SourceGraphic" in2="noise" scale="1.5" xChannelSelector="R" yChannelSelector="G"/>
              </filter>
            </defs>

            <!-- 左侧目录框 - 堆叠效果表示多个目录组 -->
            <!-- 底层目录（第三层） -->
            <path d="M 30,40 Q 32,38 126,39 Q 130,40 128,41 L 129,216 Q 128,220 126,218 L 31,219 Q 27,218 28,215 Z"
                  fill="#f5f5f5" stroke="#ccc" stroke-width="1" filter="url(#sketchy)"/>
            <!-- 中层目录（第二层） -->
            <path d="M 26,36 Q 28,34 122,35 Q 126,36 124,37 L 125,212 Q 124,216 122,214 L 27,215 Q 23,214 24,211 Z"
                  fill="#fafafa" stroke="#bbb" stroke-width="1" filter="url(#sketchy)"/>
            <!-- 顶层目录（当前选中） -->
            <path d="M 22,32 Q 24,30 118,31 Q 122,32 120,33 L 121,208 Q 120,212 118,210 L 23,211 Q 19,210 20,207 Z"
                  fill="#fff" stroke="#555" stroke-width="1.5" filter="url(#sketchy)"/>
            <text x="70" y="22" text-anchor="middle" class="sketch-label-hand">目录</text>

            <!-- 目录项 - 手绘高亮 -->
            <path d="M 28,46 Q 30,44 108,45 Q 112,46 110,48 L 111,66 Q 110,70 108,68 L 29,69 Q 25,68 27,65 Z"
                  fill="rgba(var(--v-theme-primary), 0.12)" stroke="rgb(var(--v-theme-primary))" stroke-width="1.2" filter="url(#sketchy)"/>
            <text x="70" y="61" text-anchor="middle" class="sketch-item-hand">决策树原理</text>

            <!-- 其他目录项 - 手绘线条 -->
            <path d="M 32,82 Q 50,81 80,83 Q 100,82 106,83" fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy)"/>
            <path d="M 32,100 Q 55,99 75,101 Q 90,100 98,101" fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy)"/>
            <path d="M 32,118 Q 48,117 68,119 Q 82,118 92,119" fill="none" stroke="#ccc" stroke-width="1.2" filter="url(#sketchy)"/>
            <path d="M 32,136 Q 45,135 60,137 Q 75,136 85,137" fill="none" stroke="#ddd" stroke-width="1.2" filter="url(#sketchy)"/>

            <!-- 右侧内容框 - 手绘风格 -->
            <path d="M 142,32 Q 145,29 338,31 Q 342,33 340,35 L 341,208 Q 340,212 337,210 L 143,211 Q 139,209 141,206 Z"
                  fill="none" stroke="#555" stroke-width="1.5" filter="url(#sketchy)"/>
            <text x="240" y="22" text-anchor="middle" class="sketch-label-hand">文章列表</text>

            <!-- 文章排名 - 手绘卡片 -->
            <g class="article-row">
              <text x="155" y="60" class="sketch-medal">🥇</text>
              <path d="M 176,44 Q 180,42 322,43 Q 326,45 324,47 L 325,70 Q 324,74 321,72 L 177,73 Q 173,71 175,68 Z"
                    fill="rgba(255,215,0,0.12)" stroke="#e6c200" stroke-width="1.2" filter="url(#sketchy)"/>
              <text x="250" y="62" text-anchor="middle" class="sketch-article-hand">3分钟搞懂决策树</text>
            </g>

            <g class="article-row">
              <text x="155" y="100" class="sketch-medal">🥈</text>
              <path d="M 176,84 Q 179,82 322,83 Q 326,85 324,87 L 325,110 Q 324,114 321,112 L 177,113 Q 173,111 175,108 Z"
                    fill="#fafafa" stroke="#ccc" stroke-width="1.2" filter="url(#sketchy)"/>
              <text x="250" y="102" text-anchor="middle" class="sketch-article-hand">决策树图解教程</text>
            </g>

            <g class="article-row">
              <text x="155" y="140" class="sketch-medal">🥉</text>
              <path d="M 176,124 Q 180,122 322,123 Q 326,125 324,127 L 325,150 Q 324,154 321,152 L 177,153 Q 173,151 175,148 Z"
                    fill="#fafafa" stroke="#ccc" stroke-width="1.2" filter="url(#sketchy)"/>
              <text x="250" y="142" text-anchor="middle" class="sketch-article-hand">从零实现决策树</text>
            </g>

            <!-- 省略号 -->
            <text x="250" y="178" text-anchor="middle" fill="#999" class="sketch-dots">· · ·</text>

            <!-- 左侧标注 - 手绘曲线箭头 -->
            <path d="M 18,100 C 0,100 -10,80 -20,65" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" filter="url(#sketchy)"/>
            <path d="M -25,72 L -20,65 L -13,70" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" filter="url(#sketchy)"/>
            <text x="-30" y="50" text-anchor="middle" class="sketch-annotation-hand">
              <tspan x="-30" dy="0">社区贡献节点</tspan>
              <tspan x="-30" dy="16">自选组成目录</tspan>
            </text>

            <!-- 堆叠目录标注 - 指向目录框右上角的堆叠部分 -->
            <path d="M 18,180 C -5,180 -15,200 -25,215" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" filter="url(#sketchy)"/>
            <path d="M -30,208 L -25,215 L -18,210" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" filter="url(#sketchy)"/>
            <text x="-35" y="235" text-anchor="middle" class="sketch-annotation-hand">
              <tspan x="-35" dy="0">多个目录组</tspan>
              <tspan x="-35" dy="16">不同学习路径</tspan>
            </text>

            <!-- 右侧标注 - 手绘曲线箭头 -->
            <path d="M 343,95 C 360,95 370,115 380,135" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" filter="url(#sketchy)"/>
            <path d="M 373,128 L 380,135 L 385,127" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" filter="url(#sketchy)"/>
            <text x="390" y="155" text-anchor="middle" class="sketch-annotation-hand">
              <tspan x="390" dy="0">用户投票</tspan>
              <tspan x="390" dy="16">易懂优先</tspan>
            </text>

            <!-- 底部标注 - 从省略号指向文字（箭头朝下） -->
            <path d="M 240,190 C 238,200 242,210 240,230" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" filter="url(#sketchy)"/>
            <path d="M 234,223 L 240,230 L 246,223" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" filter="url(#sketchy)"/>
            <text x="240" y="255" text-anchor="middle" class="sketch-annotation-hand">
              第一篇不懂？往下看，总有适合你的
            </text>
          </svg>
        </div>

        <p class="section-hint">好内容自然上浮，学习更高效</p>
      </div>
    </section>

    <!-- 第五屏：科学复习 -->
    <section class="intro-screen screen-5">
      <div class="screen-inner">
        <div class="section-label">04 · 科学复习</div>
        <h2 class="section-title">学完不忘，真正掌握</h2>
        <div class="review-demo">
          <div class="flashcard">
            <div class="card-face front">
              <div class="card-label">Q</div>
              <div class="card-text">决策树如何选择最佳分裂点？</div>
            </div>
          </div>
          <div class="review-stats">
            <div class="stat-item today">
              <div class="stat-num">12</div>
              <div class="stat-label">今日待复习</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">89%</div>
              <div class="stat-label">记忆保持率</div>
            </div>
            <div class="stat-item">
              <div class="stat-num">7</div>
              <div class="stat-label">连续打卡</div>
            </div>
          </div>
        </div>
        <p class="section-hint">艾宾浩斯曲线，智能安排复习</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { VueFlow, Handle, Position } from '@vue-flow/core'
import type { Node, Edge } from '@vue-flow/core'

const scrollContainer = ref<HTMLElement | null>(null)
const activeRoleIndex = ref(0)

// 角色数据
const roles = [
  { id: 'programmer', name: '程序员' },
  { id: 'investor', name: '投资者' },
  { id: 'father', name: '父亲' },
]

// 各角色的学习路径数据
const rolesData = {
  programmer: {
    nodes: [
      { id: '1', position: { x: 0, y: -5 }, data: { label: '决策树', type: 'node', status: 'done' } },
      { id: '2', position: { x: 0, y: 135 }, data: { label: 'Flask基础', type: 'node', status: 'done' } },
      { id: '3', position: { x: 160, y: -40 }, data: { label: '机器学习', type: 'course', status: 'done' } },
      { id: '4', position: { x: 160, y: 30 }, data: { label: '深度学习', type: 'course', status: 'active' } },
      { id: '5', position: { x: 160, y: 100 }, data: { label: 'Web开发', type: 'course', status: 'done' } },
      { id: '6', position: { x: 160, y: 170 }, data: { label: '系统设计', type: 'course', status: 'active' } },
      { id: '7', position: { x: 320, y: -5 }, data: { label: 'AI路线', type: 'roadmap', status: 'locked' } },
      { id: '8', position: { x: 320, y: 135 }, data: { label: '后端路线', type: 'roadmap', status: 'locked' } },
      { id: '9', position: { x: 480, y: 65 }, data: { label: '程序员', type: 'role', status: '' } },
    ],
    edges: [
      { id: 'e1-3', source: '1', target: '3', status: 'done' },
      { id: 'e1-4', source: '1', target: '4', status: 'active' },
      { id: 'e2-5', source: '2', target: '5', status: 'done' },
      { id: 'e2-6', source: '2', target: '6', status: 'active' },
      { id: 'e3-7', source: '3', target: '7', status: 'locked' },
      { id: 'e4-7', source: '4', target: '7', status: 'locked' },
      { id: 'e5-8', source: '5', target: '8', status: 'locked' },
      { id: 'e6-8', source: '6', target: '8', status: 'locked' },
      { id: 'e7-9', source: '7', target: '9', status: 'locked' },
      { id: 'e8-9', source: '8', target: '9', status: 'locked' },
    ],
  },
  investor: {
    nodes: [
      { id: '1', position: { x: 0, y: 0 }, data: { label: '财务报表', type: 'node', status: 'done' } },
      { id: '2', position: { x: 0, y: 80 }, data: { label: '估值方法', type: 'node', status: 'active' } },
      { id: '3', position: { x: 160, y: -20 }, data: { label: '价值投资', type: 'course', status: 'done' } },
      { id: '4', position: { x: 160, y: 60 }, data: { label: '行业分析', type: 'course', status: 'active' } },
      { id: '5', position: { x: 160, y: 140 }, data: { label: '风险管理', type: 'course', status: 'locked' } },
      { id: '6', position: { x: 320, y: 60 }, data: { label: '长期投资路线', type: 'roadmap', status: 'locked' } },
      { id: '7', position: { x: 480, y: 60 }, data: { label: '投资者', type: 'role', status: '' } },
    ],
    edges: [
      { id: 'e1-3', source: '1', target: '3', status: 'done' },
      { id: 'e1-4', source: '1', target: '4', status: 'active' },
      { id: 'e2-4', source: '2', target: '4', status: 'active' },
      { id: 'e2-5', source: '2', target: '5', status: 'locked' },
      { id: 'e3-6', source: '3', target: '6', status: 'locked' },
      { id: 'e4-6', source: '4', target: '6', status: 'locked' },
      { id: 'e5-6', source: '5', target: '6', status: 'locked' },
      { id: 'e6-7', source: '6', target: '7', status: 'locked' },
    ],
  },
  father: {
    nodes: [
      { id: '1', position: { x: 0, y: 0 }, data: { label: '儿童心理', type: 'node', status: 'done' } },
      { id: '2', position: { x: 0, y: 80 }, data: { label: '沟通技巧', type: 'node', status: 'done' } },
      { id: '3', position: { x: 160, y: -20 }, data: { label: '亲子教育', type: 'course', status: 'done' } },
      { id: '4', position: { x: 160, y: 60 }, data: { label: '情绪管理', type: 'course', status: 'active' } },
      { id: '5', position: { x: 160, y: 140 }, data: { label: '学习辅导', type: 'course', status: 'locked' } },
      { id: '6', position: { x: 320, y: 60 }, data: { label: '好爸爸路线', type: 'roadmap', status: 'locked' } },
      { id: '7', position: { x: 480, y: 60 }, data: { label: '父亲', type: 'role', status: '' } },
    ],
    edges: [
      { id: 'e1-3', source: '1', target: '3', status: 'done' },
      { id: 'e1-4', source: '1', target: '4', status: 'active' },
      { id: 'e2-3', source: '2', target: '3', status: 'done' },
      { id: 'e2-4', source: '2', target: '4', status: 'active' },
      { id: 'e2-5', source: '2', target: '5', status: 'locked' },
      { id: 'e3-6', source: '3', target: '6', status: 'locked' },
      { id: 'e4-6', source: '4', target: '6', status: 'locked' },
      { id: 'e5-6', source: '5', target: '6', status: 'locked' },
      { id: 'e6-7', source: '6', target: '7', status: 'locked' },
    ],
  },
}

// 根据状态获取边的样式
const getEdgeStyle = (status: string) => {
  switch (status) {
    case 'done':
      return { animated: false, style: { stroke: '#66bb6a', strokeWidth: 2 } }
    case 'active':
      return { animated: true, style: { stroke: '#42a5f5', strokeWidth: 2 } }
    default:
      return { animated: false, style: { stroke: '#bdbdbd', strokeWidth: 2 } }
  }
}

// 当前角色的节点
const currentFlowNodes = computed<Node[]>(() => {
  const roleId = roles[activeRoleIndex.value].id
  const data = rolesData[roleId as keyof typeof rolesData]
  return data.nodes.map((node) => ({
    id: node.id,
    type: 'custom',
    position: node.position,
    data: node.data,
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
  }))
})

// 当前角色的边
const currentFlowEdges = computed<Edge[]>(() => {
  const roleId = roles[activeRoleIndex.value].id
  const data = rolesData[roleId as keyof typeof rolesData]
  return data.edges.map((edge) => ({
    id: edge.id,
    source: edge.source,
    target: edge.target,
    ...getEdgeStyle(edge.status),
  }))
})
</script>

<style scoped>
.intro-content {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  scroll-snap-type: y proximity;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.intro-content::-webkit-scrollbar {
  display: none;
}

.intro-screen {
  height: 100%;
  min-height: 100%;
  scroll-snap-align: start;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
}

.screen-inner {
  width: 100%;
  max-width: 400px;
  text-align: center;
}

/* ========== 第一屏：核心价值 ========== */

.tagline {
  display: inline-block;
  font-size: 0.85rem;
  font-weight: 600;
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.1);
  padding: 6px 16px;
  border-radius: 20px;
  margin-bottom: 24px;
  letter-spacing: 2px;
}

.main-title {
  font-size: 2.2rem;
  font-weight: 800;
  line-height: 1.3;
  color: rgb(var(--v-theme-on-surface));
  margin: 0 0 20px 0;
}

.main-title .highlight {
  color: rgb(var(--v-theme-primary));
}

.sub-text {
  font-size: 1.1rem;
  color: rgba(var(--v-theme-on-surface), 0.6);
  margin: 0 0 48px 0;
  line-height: 1.5;
}

.scroll-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: rgba(var(--v-theme-on-surface), 0.35);
  font-size: 0.9rem;
}

.scroll-line {
  width: 1px;
  height: 40px;
  background: linear-gradient(180deg, rgba(var(--v-theme-on-surface), 0.3) 0%, transparent 100%);
  animation: scrollPulse 2s ease-in-out infinite;
}

@keyframes scrollPulse {
  0%, 100% { opacity: 0.3; transform: scaleY(1); }
  50% { opacity: 0.6; transform: scaleY(1.1); }
}

/* ========== 通用区块样式 ========== */
.section-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: rgb(var(--v-theme-primary));
  letter-spacing: 1px;
  margin-bottom: 10px;
  text-transform: uppercase;
}

.section-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: rgb(var(--v-theme-on-surface));
  margin: 0 0 28px 0;
}

.section-hint {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-on-surface), 0.5);
  margin: 24px 0 0 0;
}

/* ========== 第二屏：角色 ========== */
.roles-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.role-chip {
  font-size: 1rem;
  padding: 10px 20px;
  border-radius: 24px;
  font-weight: 500;
  background: rgba(var(--v-theme-surface-variant), 0.5);
  color: rgba(var(--v-theme-on-surface), 0.85);
}

/* ========== 第三屏：Vue Flow 路径 ========== */
.screen-inner-flow {
  max-width: 600px;
}

/* 角色 Tab 切换 */
.role-tabs {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}

.role-tab {
  padding: 8px 20px;
  border: none;
  border-radius: 20px;
  background: rgba(var(--v-theme-surface-variant), 0.5);
  color: rgba(var(--v-theme-on-surface), 0.7);
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.role-tab:hover {
  background: rgba(var(--v-theme-surface-variant), 0.8);
}

.role-tab.active {
  background: rgb(var(--v-theme-primary));
  color: white;
}

.flow-container {
  width: 100%;
  height: 280px;
  border-radius: 16px;
  overflow: hidden;
  background: transparent;
  margin-bottom: 12px;
}

/* 自定义节点样式 */
.custom-node {
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  border: 2px solid;
  transition: all 0.2s ease;
}

/* 节点类型样式 */
.custom-node.node {
  background: #fff;
  border-color: #bdbdbd;
  color: #616161;
}

.custom-node.course {
  background: #fff;
  border-color: #90caf9;
  color: #1565c0;
}

.custom-node.roadmap {
  background: #fff;
  border-color: #a5d6a7;
  color: #2e7d32;
}

.custom-node.role {
  background: linear-gradient(135deg, rgb(var(--v-theme-primary)), rgba(var(--v-theme-primary), 0.8));
  border-color: rgb(var(--v-theme-primary));
  color: white;
  font-size: 16px;
  padding: 12px 24px;
}

/* 节点状态样式 */
.custom-node.done {
  background: #e8f5e9;
  border-color: #66bb6a;
  color: #2e7d32;
}

.custom-node.active {
  background: #e3f2fd;
  border-color: #42a5f5;
  color: #1565c0;
  box-shadow: 0 0 0 3px rgba(66, 165, 245, 0.3);
}

.custom-node.locked {
  background: #f5f5f5;
  border-color: #e0e0e0;
  color: #9e9e9e;
}

.custom-node.role.active {
  box-shadow: 0 0 0 4px rgba(var(--v-theme-primary), 0.3);
}

/* Vue Flow 覆盖样式 */
.flow-container :deep(.vue-flow__node) {
  cursor: default;
}

.flow-container :deep(.vue-flow__edge-path) {
  stroke-width: 2;
}

.flow-container :deep(.vue-flow__controls) {
  display: none;
}

.flow-container :deep(.vue-flow__handle) {
  width: 8px;
  height: 8px;
  background: transparent;
  border: none;
}

/* ========== 第四屏：手绘风格课程图示 ========== */
.screen-inner-full {
  max-width: 800px;
}

.sketch-container {
  width: 100%;
}

.sketch-svg {
  width: 100%;
  height: auto;
}

.sketch-label-hand {
  font-size: 12px;
  fill: #666;
  font-weight: 500;
}

.sketch-item-hand {
  font-size: 11px;
  fill: rgb(var(--v-theme-primary));
  font-weight: 500;
}

.sketch-medal {
  font-size: 14px;
}

.sketch-article-hand {
  font-size: 11px;
  fill: #444;
}

.sketch-dots {
  font-size: 16px;
  letter-spacing: 4px;
}

.sketch-annotation-hand {
  font-size: 12px;
  fill: rgba(var(--v-theme-on-surface), 0.75);
  font-weight: 500;
}

/* 图例 */
.tree-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-on-surface), 0.6);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(var(--v-theme-surface-variant), 0.6);
  border: 2px solid rgba(var(--v-theme-on-surface), 0.2);
}

.legend-dot.done {
  background: #e8f5e9;
  border-color: #66bb6a;
}

.legend-dot.active {
  background: #e3f2fd;
  border-color: #42a5f5;
}

.legend-dot.locked {
  background: #f5f5f5;
  border-color: #e0e0e0;
}

/* ========== 第五屏：复习 ========== */
.review-demo {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.flashcard {
  perspective: 1000px;
}

.card-face {
  background: linear-gradient(135deg, rgb(var(--v-theme-primary)) 0%, rgba(var(--v-theme-primary), 0.8) 100%);
  border-radius: 16px;
  padding: 24px 20px;
  color: white;
  text-align: left;
}

.card-label {
  font-size: 0.85rem;
  font-weight: 700;
  opacity: 0.7;
  margin-bottom: 10px;
}

.card-text {
  font-size: 1.1rem;
  font-weight: 500;
  line-height: 1.5;
}

.review-stats {
  display: flex;
  gap: 8px;
}

.stat-item {
  flex: 1;
  background: rgba(var(--v-theme-surface-variant), 0.4);
  border-radius: 12px;
  padding: 14px 8px;
  text-align: center;
}

.stat-item.today {
  background: rgba(var(--v-theme-primary), 0.1);
}

.stat-num {
  font-size: 1.5rem;
  font-weight: 700;
  color: rgb(var(--v-theme-on-surface));
  margin-bottom: 4px;
}

.stat-item.today .stat-num {
  color: rgb(var(--v-theme-primary));
}

.stat-label {
  font-size: 0.8rem;
  color: rgba(var(--v-theme-on-surface), 0.5);
}

/* ========== 响应式 ========== */
@media (max-height: 700px) {
  .intro-screen {
    padding: 16px;
  }

  .main-title {
    font-size: 1.5rem;
  }

  .section-title {
    font-size: 1.2rem;
    margin-bottom: 16px;
  }

  .sub-text {
    margin-bottom: 24px;
  }

  .scroll-indicator {
    display: none;
  }
}

@media (max-width: 1400px) {
  .main-title {
    font-size: 1.5rem;
  }

  .section-title {
    font-size: 1.2rem;
  }
}
</style>
