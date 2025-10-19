<template>
  <div class="read-page">
    <AppHeader />

    <div class="read-content">
      <!-- 左侧目录 - 靠边固定 -->
      <div class="left-sidebar">
        <div class="toc-card">
          <h3 class="text-h6 font-weight-bold mb-4">目录</h3>
          <div class="toc-tree">
            <TreeNode
              v-for="(item, index) in tocData"
              :key="index"
              :node="item"
              :active-node="activeNode"
              @node-click="handleNodeClick"
            />
          </div>
        </div>
      </div>

      <!-- 中间+右侧容器 - 居中 -->
      <div class="center-right-wrapper">
        <!-- 中间内容区 - 固定宽度居中 -->
        <div class="center-content">
        <!-- 课程头部 -->
        <div class="course-header">
          <h1 class="text-h4 font-weight-bold mb-2">{{ courseData.title }}</h1>
          <p class="text-body-1 text-medium-emphasis mb-4">{{ courseData.description }}</p>

          <div class="d-flex align-center ga-2">
            <v-btn
              :variant="isLearning ? 'flat' : 'outlined'"
              color="primary"
              class="text-none"
              @click="toggleLearning"
            >
              {{ isLearning ? '正在学习' : '开始学习' }}
            </v-btn>
            <v-btn
              variant="outlined"
              color="primary"
              class="text-none"
            >
              {{ courseData.subscribed ? '已订阅' : '订阅课程' }}
            </v-btn>
          </div>
        </div>

        <!-- 文章内容 -->
        <v-card class="post-card mb-4" border rounded="xl">
          <v-card-text class="pa-6">
            <h2 class="text-h5 font-weight-bold mb-4">{{ currentPost.title }}</h2>
            <div class="post-content" v-html="currentPost.content"></div>

            <div class="d-flex align-center mt-6 ga-4">
              <div class="d-flex align-center ga-1">
                <v-btn icon variant="text" size="small">
                  <v-icon>mdi-arrow-up-bold</v-icon>
                </v-btn>
                <span class="font-weight-bold">{{ currentPost.votes }}</span>
                <v-btn icon variant="text" size="small">
                  <v-icon>mdi-arrow-down-bold</v-icon>
                </v-btn>
              </div>

              <v-btn variant="text" class="text-none">
                <v-icon left>mdi-comment-outline</v-icon>
                {{ currentPost.commentCount }} 评论
              </v-btn>

              <v-btn variant="text" class="text-none">
                <v-icon left>mdi-share-variant</v-icon>
                分享
              </v-btn>
            </div>
          </v-card-text>
        </v-card>

        <!-- 评论区 -->
        <v-card class="comments-card" border rounded="xl">
          <v-card-title class="pa-6 pb-4">
            <span class="text-h6 font-weight-bold">评论 ({{ currentPost.commentCount }})</span>
          </v-card-title>

          <v-card-text class="pa-6 pt-0">
            <!-- 评论输入框 -->
            <v-textarea
              v-model="newComment"
              variant="outlined"
              placeholder="写下你的评论..."
              rows="3"
              class="mb-4"
            />
            <v-btn color="primary" class="text-none">发表评论</v-btn>

            <!-- 评论列表 -->
            <div class="comments-list mt-6">
              <div
                v-for="comment in comments"
                :key="comment.id"
                class="comment-item mb-4 pa-4"
              >
                <div class="d-flex align-start">
                  <v-avatar size="40" class="mr-3">
                    <v-icon>mdi-account-circle</v-icon>
                  </v-avatar>
                  <div class="flex-grow-1">
                    <div class="d-flex align-center mb-1">
                      <span class="font-weight-bold mr-2">{{ comment.author }}</span>
                      <span class="text-caption text-medium-emphasis">{{ comment.time }}</span>
                    </div>
                    <p class="mb-2">{{ comment.content }}</p>
                    <div class="d-flex align-center ga-2">
                      <v-btn icon variant="text" size="x-small">
                        <v-icon size="small">mdi-arrow-up-bold</v-icon>
                      </v-btn>
                      <span class="text-caption">{{ comment.votes }}</span>
                      <v-btn icon variant="text" size="x-small">
                        <v-icon size="small">mdi-arrow-down-bold</v-icon>
                      </v-btn>
                      <v-btn variant="text" size="small" class="text-none ml-2">
                        回复
                      </v-btn>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </div>

      <!-- 右侧工具栏 - 靠边固定 -->
      <div class="right-sidebar">
        <div class="sidebar-sticky">
          <!-- AI助手卡片 -->
          <v-card class="sidebar-card mb-4" border rounded="xl">
            <v-card-title class="pa-4">
              <v-icon left color="primary">mdi-robot</v-icon>
              AI 答疑助手
            </v-card-title>
            <v-card-text class="pa-4 pt-0">
              <p class="text-body-2 text-medium-emphasis mb-3">
                选中文本即可提问
              </p>
              <v-btn block color="primary" variant="outlined" class="text-none">
                开始提问
              </v-btn>
            </v-card-text>
          </v-card>

          <!-- 记忆卡片组 -->
          <v-card class="sidebar-card mb-4" border rounded="xl">
            <v-card-title class="pa-4">
              <v-icon left color="primary">mdi-cards</v-icon>
              记忆卡片组
            </v-card-title>
            <v-card-text class="pa-4 pt-0">
              <v-list>
                <v-list-item
                  v-for="deck in memoryDecks"
                  :key="deck.id"
                  class="px-0"
                >
                  <v-list-item-title class="text-body-2">
                    {{ deck.title }}
                  </v-list-item-title>
                  <v-list-item-subtitle class="text-caption">
                    {{ deck.cardCount }} 张卡片
                  </v-list-item-subtitle>
                </v-list-item>
              </v-list>
              <v-btn block color="primary" variant="outlined" class="text-none mt-2">
                创建卡片组
              </v-btn>
            </v-card-text>
          </v-card>

          <!-- 课程信息 -->
          <v-card class="sidebar-card" border rounded="xl">
            <v-card-title class="pa-4">
              关于本课程
            </v-card-title>
            <v-card-text class="pa-4 pt-0">
              <div class="info-item mb-3">
                <div class="text-caption text-medium-emphasis">总节数</div>
                <div class="text-body-2 font-weight-bold">{{ courseData.totalNodes }}</div>
              </div>
              <div class="info-item mb-3">
                <div class="text-caption text-medium-emphasis">已完成</div>
                <div class="text-body-2 font-weight-bold">{{ courseData.completedNodes }}</div>
              </div>
              <div class="info-item">
                <div class="text-caption text-medium-emphasis">学习进度</div>
                <v-progress-linear
                  :model-value="courseData.progress"
                  color="primary"
                  height="8"
                  rounded
                  class="mt-2"
                />
                <div class="text-caption text-right mt-1">{{ courseData.progress }}%</div>
              </div>
            </v-card-text>
          </v-card>
        </div>
      </div>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import TreeNode from '@/components/TreeNode.vue'

const isLearning = ref(false)
const currentNode = ref(0)
const newComment = ref('')
const activeNode = ref(null)

const handleNodeClick = (node: any) => {
  activeNode.value = node
  console.log('Clicked node:', node.name)
}

// Mock 数据
const courseData = ref({
  title: 'Vue 3 完整教程',
  description: '从入门到精通，掌握 Vue 3 的核心概念和最佳实践',
  subscribed: false,
  totalNodes: 20,
  completedNodes: 5,
  progress: 25
})

// 多级树形目录数据
const tocData = ref([
  {
    name: '1. Vue 3 入门',
    completed: true,
    expanded: true,
    children: [
      { name: '1.1 Vue 3 简介', completed: true },
      { name: '1.2 安装和设置', completed: true },
      {
        name: '1.3 基本概念',
        completed: true,
        expanded: true,
        children: [
          { name: '1.3.1 模板语法', completed: true },
          { name: '1.3.2 数据绑定', completed: true },
          { name: '1.3.3 条件渲染', completed: false }
        ]
      },
      { name: '1.4 开发工具', completed: false }
    ]
  },
  {
    name: '2. 响应式系统',
    completed: false,
    expanded: false,
    children: [
      {
        name: '2.1 响应式基础',
        completed: false,
        children: [
          { name: '2.1.1 reactive()', completed: false },
          { name: '2.1.2 ref()', completed: false },
          { name: '2.1.3 toRef() 和 toRefs()', completed: false }
        ]
      },
      {
        name: '2.2 计算属性和侦听器',
        completed: false,
        children: [
          { name: '2.2.1 computed()', completed: false },
          { name: '2.2.2 watch()', completed: false },
          { name: '2.2.3 watchEffect()', completed: false }
        ]
      },
      { name: '2.3 响应式原理', completed: false }
    ]
  },
  {
    name: '3. Composition API',
    completed: false,
    expanded: false,
    children: [
      { name: '3.1 setup() 函数', completed: false },
      {
        name: '3.2 生命周期钩子',
        completed: false,
        children: [
          { name: '3.2.1 onMounted', completed: false },
          { name: '3.2.2 onUpdated', completed: false },
          { name: '3.2.3 onUnmounted', completed: false }
        ]
      },
      { name: '3.3 依赖注入', completed: false },
      { name: '3.4 组合式函数', completed: false }
    ]
  },
  {
    name: '4. 组件开发',
    completed: false,
    expanded: false,
    children: [
      { name: '4.1 组件基础', completed: false },
      { name: '4.2 Props', completed: false },
      { name: '4.3 事件', completed: false },
      { name: '4.4 插槽', completed: false },
      {
        name: '4.5 组件通信',
        completed: false,
        children: [
          { name: '4.5.1 父子组件通信', completed: false },
          { name: '4.5.2 兄弟组件通信', completed: false },
          { name: '4.5.3 跨级组件通信', completed: false }
        ]
      }
    ]
  },
  {
    name: '5. 路由管理',
    completed: false,
    expanded: false,
    children: [
      { name: '5.1 Vue Router 安装', completed: false },
      { name: '5.2 路由配置', completed: false },
      { name: '5.3 动态路由', completed: false },
      { name: '5.4 导航守卫', completed: false },
      { name: '5.5 路由懒加载', completed: false }
    ]
  },
  {
    name: '6. 状态管理',
    completed: false,
    expanded: false,
    children: [
      { name: '6.1 Pinia 简介', completed: false },
      { name: '6.2 定义 Store', completed: false },
      { name: '6.3 State', completed: false },
      { name: '6.4 Getters', completed: false },
      { name: '6.5 Actions', completed: false }
    ]
  },
  {
    name: '7. 高级特性',
    completed: false,
    expanded: false,
    children: [
      { name: '7.1 指令', completed: false },
      { name: '7.2 Teleport', completed: false },
      { name: '7.3 Suspense', completed: false },
      { name: '7.4 性能优化', completed: false }
    ]
  },
  {
    name: '8. 项目实战',
    completed: false,
    expanded: false,
    children: [
      { name: '8.1 项目搭建', completed: false },
      { name: '8.2 UI 框架集成', completed: false },
      { name: '8.3 API 请求封装', completed: false },
      { name: '8.4 权限管理', completed: false },
      { name: '8.5 项目部署', completed: false }
    ]
  }
])

const currentPost = ref({
  title: 'Vue 3 响应式系统深度解析',
  content: `
    <p>Vue 3 的响应式系统是框架的核心特性之一。它基于 Proxy 实现，相比 Vue 2 的 Object.defineProperty 有更好的性能和更完整的功能支持。在本文中，我们将深入探讨 Vue 3 响应式系统的工作原理、核心 API 以及最佳实践。</p>

    <h3>为什么需要响应式系统</h3>
    <p>在现代前端开发中，我们经常需要处理动态数据。当数据发生变化时，界面应该自动更新以反映最新的状态。这就是响应式系统存在的意义——它能够自动追踪数据的变化，并触发相应的视图更新，让开发者无需手动操作 DOM。</p>
    <p>传统的前端开发需要开发者手动监听数据变化，然后更新对应的 DOM 元素。这种方式不仅繁琐，而且容易出错。Vue 的响应式系统通过自动化这个过程，极大地提升了开发效率和代码的可维护性。</p>

    <h3>Vue 2 vs Vue 3 响应式实现</h3>
    <p>Vue 2 使用 Object.defineProperty 来实现响应式。这种方式虽然可行，但存在一些局限性：无法检测对象属性的添加或删除、无法直接监听数组的索引和 length 属性变化、性能开销较大等。</p>
    <p>Vue 3 采用了 ES6 的 Proxy 来重写响应式系统。Proxy 是一个更底层、更强大的 API，它能够拦截对象的所有操作，包括属性的读取、设置、删除等。这使得 Vue 3 的响应式系统更加完善和高效。</p>
    <p>使用 Proxy 的优势包括：可以检测到对象属性的添加和删除、可以直接监听数组的变化、性能更好、代码实现更简洁等。这些改进让 Vue 3 的响应式系统更加强大和可靠。</p>

    <h3>核心 API 详解</h3>

    <h4>reactive() - 创建响应式对象</h4>
    <p>reactive() 是 Vue 3 中用于创建响应式对象的核心 API。它接收一个普通对象，返回该对象的响应式代理。当你修改响应式对象的属性时，所有依赖该属性的地方都会自动更新。</p>
    <p>使用 reactive() 时需要注意：它只能用于对象类型（对象、数组、Map、Set 等），不能用于基本类型；返回的响应式代理会深层递归，即嵌套的对象也会变成响应式的；解构会导致响应式丢失，需要使用 toRefs() 来保持响应式。</p>
    <p>在实际开发中，reactive() 适合用于组合多个相关联的状态。例如，表单数据、用户信息对象等场景都很适合使用 reactive()。</p>

    <h4>ref() - 创建响应式引用</h4>
    <p>ref() 用于创建一个响应式引用，它可以包装任何类型的值，包括基本类型。ref() 返回一个包含 value 属性的对象，通过 .value 来访问和修改实际的值。</p>
    <p>ref() 的特点是：可以用于基本类型和对象类型；在模板中使用时会自动解包，不需要 .value；在 JavaScript 代码中需要通过 .value 访问；可以方便地在不同组件之间传递响应式数据。</p>
    <p>什么时候用 ref() 而不是 reactive()？一般来说，当你需要存储基本类型的值时，必须使用 ref()。对于对象类型，两者都可以使用，但 ref() 更适合需要整体替换的场景，而 reactive() 更适合需要修改内部属性的场景。</p>

    <h4>computed() - 计算属性</h4>
    <p>computed() 用于创建计算属性，它会基于响应式依赖进行缓存。只有当依赖的响应式数据发生变化时，计算属性才会重新计算。这是计算属性相比方法的主要优势——避免不必要的重复计算。</p>
    <p>计算属性默认是只读的，但你也可以提供 getter 和 setter 来创建可写的计算属性。在大多数情况下，只读的计算属性就足够了。</p>
    <p>使用计算属性的最佳场景包括：需要对数据进行复杂的转换或格式化；需要从多个响应式数据源派生出新的值；需要进行开销较大的计算并希望利用缓存机制等。</p>

    <h4>watch() 和 watchEffect() - 侦听器</h4>
    <p>watch() 用于监听一个或多个响应式数据源，当数据变化时执行回调函数。它可以访问到数据变化前后的值，并且支持配置选项如 immediate、deep 等。</p>
    <p>watchEffect() 是一个更加自动化的侦听器。它会自动追踪回调函数中使用的所有响应式依赖，并在依赖变化时重新执行。相比 watch()，watchEffect() 不需要明确指定要监听的数据源，更加简洁。</p>
    <p>选择使用 watch() 还是 watchEffect() 取决于具体场景。如果你需要访问变化前后的值，或者需要更精确地控制监听的数据源，使用 watch()。如果只是想在某些响应式数据变化时执行副作用操作，watchEffect() 更加方便。</p>

    <h3>响应式原理深入理解</h3>
    <p>Vue 3 的响应式系统基于依赖追踪和发布订阅模式。当你访问一个响应式对象的属性时，Vue 会追踪这个依赖关系。当属性被修改时，Vue 会通知所有依赖该属性的订阅者（如组件、计算属性、侦听器等），触发它们的更新。</p>
    <p>这个过程涉及三个核心概念：Proxy 代理对象负责拦截属性的读取和设置操作；track() 函数在属性被读取时收集依赖；trigger() 函数在属性被修改时触发更新。</p>
    <p>了解这些原理可以帮助你更好地理解 Vue 的行为，避免一些常见的陷阱，并写出更高效的代码。</p>

    <h3>最佳实践与注意事项</h3>
    <p>在使用 Vue 3 响应式系统时，有一些最佳实践值得遵循。首先，避免在响应式对象中存储不必要的数据。响应式系统会为每个属性添加额外的开销，因此只应该让真正需要响应式的数据变成响应式。</p>
    <p>其次，注意响应式对象的解构问题。当你解构一个 reactive 对象时，解构出来的值会失去响应式。如果确实需要解构，可以使用 toRefs() 或 toRef() 来保持响应式。</p>
    <p>另外，在处理大型列表或频繁更新的数据时，要注意性能优化。可以考虑使用 shallowReactive() 或 shallowRef() 来创建浅层响应式对象，只监听第一层属性的变化。对于不需要响应式的数据，可以使用 markRaw() 标记为非响应式。</p>
    <p>最后，合理使用计算属性和侦听器。不要在计算属性中进行有副作用的操作，也不要在侦听器中进行过于复杂的逻辑处理。保持代码的清晰和可维护性。</p>

    <h3>总结</h3>
    <p>Vue 3 的响应式系统是一个强大而灵活的工具，它为开发者提供了简洁的 API 来处理动态数据。通过深入理解其工作原理和掌握核心 API 的使用方法，你可以更好地利用 Vue 3 构建高性能、易维护的应用程序。</p>
    <p>无论你是从 Vue 2 迁移到 Vue 3，还是刚开始学习 Vue，掌握响应式系统都是至关重要的。希望本文能帮助你更好地理解和使用 Vue 3 的响应式特性。</p>
  `,
  votes: 42,
  commentCount: 8
})

const comments = ref([
  {
    id: 1,
    author: '用户A',
    time: '2小时前',
    content: '讲解得很清楚，特别是 reactive 和 ref 的区别部分！',
    votes: 12
  },
  {
    id: 2,
    author: '用户B',
    time: '5小时前',
    content: '能否再详细说明一下 computed 的缓存机制？',
    votes: 8
  },
  {
    id: 3,
    author: '用户C',
    time: '1天前',
    content: '非常实用的教程，已经在项目中应用了这些概念。',
    votes: 15
  }
])

const memoryDecks = ref([
  { id: 1, title: 'Vue 3 核心 API', cardCount: 12 },
  { id: 2, title: '响应式原理', cardCount: 8 },
  { id: 3, title: '常见问题', cardCount: 15 }
])

const toggleLearning = () => {
  isLearning.value = !isLearning.value
}
</script>

<style scoped>
.read-page {
  min-height: 100vh;
  background-color: #FAFBFC;
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.read-page::before {
  content: '';
  position: absolute;
  top: -20%;
  right: -10%;
  width: 1000px;
  height: 1000px;
  background: radial-gradient(circle, rgba(255, 87, 34, 0.25) 0%, rgba(255, 87, 34, 0.1) 40%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
}

.read-page::after {
  content: '';
  position: absolute;
  bottom: -25%;
  left: -15%;
  width: 1100px;
  height: 1100px;
  background: radial-gradient(circle, rgba(0, 188, 212, 0.2) 0%, rgba(0, 188, 212, 0.08) 40%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
}

/* Reddit风格三栏布局 */
.read-content {
  display: flex;
  position: relative;
  z-index: 1;
}

/* 左侧边栏 - 固定靠左 */
.left-sidebar {
  width: 360px;
  flex-shrink: 0;
  padding: 20px 0 20px 20px;
}

.toc-card {
  position: sticky;
  top: 20px;
  background-color: white;
  padding: 20px;
  border: 1px solid #EDEFF1;
  border-radius: 24px;
}

.toc-tree {
  margin-top: 8px;
}

/* 中间+右侧容器 - 居中 */
.center-right-wrapper {
  display: flex;
  flex: 1;
  justify-content: center;
  max-width: calc(100% - 360px);
}

/* 中间内容区 - 固定宽度 */
.center-content {
  width: 750px;
  flex-shrink: 0;
  padding: 20px 20px 20px 20px;
}

.course-header {
  background-color: white;
  padding: 32px;
  border: 1px solid #EDEFF1;
  border-radius: 24px;
  margin-bottom: 20px;
}

.post-card,
.comments-card {
  background-color: white;
  border: 1px solid #EDEFF1;
}

/* 文章内容区域 */
.post-content {
  line-height: 1.8;
  color: #1A1A1B;
  font-size: 1rem;
}

/* 段落样式 */
.post-content p {
  margin-bottom: 1rem;
  line-height: 1.75;
}

/* 标题层级样式 - 使用 Vuetify spacing */
.post-content h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-top: 2rem;
  margin-bottom: 1rem;
  color: #1A1A1B;
  line-height: 1.3;
  letter-spacing: -0.02em;
}

.post-content h2 {
  font-size: 1.75rem;
  font-weight: 700;
  margin-top: 2.5rem;
  margin-bottom: 1rem;
  color: #1A1A1B;
  line-height: 1.3;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #EDEFF1;
  letter-spacing: -0.01em;
}

.post-content h3 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-top: 2rem;
  margin-bottom: 0.875rem;
  color: #1A1A1B;
  line-height: 1.4;
}

.post-content h4 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-top: 1.75rem;
  margin-bottom: 0.75rem;
  color: #1A1A1B;
  line-height: 1.4;
}

.post-content h5 {
  font-size: 1.1rem;
  font-weight: 600;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
  color: #1A1A1B;
  line-height: 1.4;
}

.post-content h6 {
  font-size: 1rem;
  font-weight: 600;
  margin-top: 1.25rem;
  margin-bottom: 0.625rem;
  color: #666;
  line-height: 1.4;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-size: 0.875rem;
}

/* 第一个元素不需要上边距 */
.post-content > *:first-child {
  margin-top: 0 !important;
}

/* 列表样式 */
.post-content ul,
.post-content ol {
  margin-bottom: 1rem;
  padding-left: 2rem;
  line-height: 1.75;
}

.post-content ul {
  list-style-type: disc;
}

.post-content ol {
  list-style-type: decimal;
}

.post-content li {
  margin-bottom: 0.5rem;
  padding-left: 0.5rem;
}

.post-content ul ul,
.post-content ol ul {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
  list-style-type: circle;
}

.post-content ul ol,
.post-content ol ol {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}

/* 链接样式 */
.post-content a {
  color: rgb(var(--v-theme-primary));
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all 0.2s ease;
  font-weight: 500;
}

.post-content a:hover {
  border-bottom-color: rgb(var(--v-theme-primary));
}

/* 代码块样式 */
.post-content code {
  background-color: rgba(0, 0, 0, 0.05);
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 0.875em;
  color: #E91E63;
  font-weight: 500;
}

.post-content pre {
  background-color: #F6F7F8;
  padding: 1rem;
  border-radius: 0.5rem;
  overflow-x: auto;
  margin-top: 1rem;
  margin-bottom: 1rem;
  border: 1px solid #EDEFF1;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.post-content pre code {
  background-color: transparent;
  padding: 0;
  color: #1A1A1B;
  font-size: 0.875rem;
  font-weight: 400;
}

/* 引用块样式 */
.post-content blockquote {
  margin: 1.5rem 0;
  padding: 0.75rem 1.25rem;
  border-left: 4px solid rgb(var(--v-theme-primary));
  background-color: rgba(var(--v-theme-primary), 0.05);
  color: #666;
  font-style: italic;
  border-radius: 0 0.25rem 0.25rem 0;
}

.post-content blockquote p {
  margin-bottom: 0;
}

.post-content blockquote p:not(:last-child) {
  margin-bottom: 0.5rem;
}

/* 分隔线样式 */
.post-content hr {
  margin: 2rem 0;
  border: none;
  border-top: 1px solid #EDEFF1;
  opacity: 0.7;
}

/* 图片样式 */
.post-content img {
  max-width: 100%;
  height: auto;
  border-radius: 0.5rem;
  margin: 1.5rem 0;
  display: block;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 表格样式 */
.post-content table {
  width: 100%;
  border-collapse: collapse;
  margin: 1.5rem 0;
  font-size: 0.9rem;
  border-radius: 0.5rem;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.post-content th,
.post-content td {
  padding: 0.75rem 1rem;
  text-align: left;
  border-bottom: 1px solid #EDEFF1;
}

.post-content th {
  background-color: #F6F7F8;
  font-weight: 600;
  color: #1A1A1B;
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.post-content tr:last-child td {
  border-bottom: none;
}

.post-content tbody tr:hover {
  background-color: #FAFBFC;
  transition: background-color 0.2s ease;
}

/* 强调样式 */
.post-content strong {
  font-weight: 600;
  color: #1A1A1B;
}

.post-content em {
  font-style: italic;
  color: #666;
}

/* 删除线 */
.post-content del {
  text-decoration: line-through;
  color: #999;
  opacity: 0.7;
}

/* 标记文本 */
.post-content mark {
  background-color: rgba(255, 235, 59, 0.4);
  padding: 0.125rem 0.25rem;
  border-radius: 0.125rem;
}

/* 键盘按键样式 */
.post-content kbd {
  background-color: #F6F7F8;
  border: 1px solid #EDEFF1;
  border-radius: 0.25rem;
  padding: 0.125rem 0.375rem;
  font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
  font-size: 0.875em;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.comment-item {
  background-color: #F6F7F8;
  border-radius: 24px;
}

/* 右侧边栏 - 紧贴中间内容 */
.right-sidebar {
  width: 320px;
  flex-shrink: 0;
  padding: 20px 0 20px 20px;
}

.sidebar-sticky {
  position: sticky;
  top: 20px;
}

.sidebar-card {
  background-color: white;
  border: 1px solid #EDEFF1;
}

.info-item {
  padding: 8px 0;
}

/* 按钮样式 */
.v-btn {
  border-radius: 24px;
}

/* 响应式 */
@media (max-width: 1280px) {
  .left-sidebar {
    display: none;
  }

  .center-content {
    width: 100%;
    max-width: 640px;
  }

  .right-sidebar {
    display: none;
  }
}

@media (min-width: 1281px) and (max-width: 1600px) {
  .left-sidebar {
    width: 320px;
  }

  .right-sidebar {
    width: 280px;
  }
}
</style>
