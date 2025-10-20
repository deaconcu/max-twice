# 职业中心功能实现计划

## 概述
参考 web-ts 的职业中心，在 web-ts-reddit-template 中实现完整的职业中心功能。

**⚠️ 重要设计原则**：创建的所有页面和组件的设计风格必须符合 LoginView 和 ReadView 页面的设计风格。

## 设计风格要求（基于 LoginView / ReadView）

### 核心设计元素
- **背景色**: `#FAFBFC` (浅灰色背景)
- **卡片背景**: 白色 `#FFFFFF`
- **边框**: `#EDEFF1` (浅灰色边框)
- **圆角**: 大圆角设计 `border-radius: 12-24px`
- **阴影**: 最小化阴影，扁平化设计
- **间距**: 使用 Vuetify 的 spacing (pa-4, mb-4, etc.)

### 按钮样式
- 使用 `rounded="lg"` 或 `rounded="xl"`
- `variant="flat"` 或 `variant="outlined"`
- 主色按钮：`color="primary"`

### 卡片样式
- `border` 属性添加边框
- `rounded="xl"` 大圆角
- 白色背景，灰色边框

### 字体层级
- 标题：`text-h4`, `text-h5`, `text-h6`
- 正文：`text-body-1`, `text-body-2`
- 字重：`font-weight-bold`, `font-weight-medium`

### 图标使用
- 使用 `mdi-*` 图标
- 颜色：`color="primary"` 或 `color="grey"`
- 尺寸：`size="20"` 至 `size="32"`

## 目录结构
```
src/
├── views/
│   └── CareerCenter.vue                 # 职业中心主页面
├── components/
│   └── career/
│       ├── CareerFilter.vue             # ✅ 已完成 - 搜索和筛选组件
│       ├── CategoryNavigation.vue       # 分类导航组件
│       ├── CareerGrid.vue              # 职业网格列表组件
│       └── CareerCard.vue              # 职业卡片组件
├── types/
│   └── profession.ts                    # 职业相关类型定义
└── router/
    └── index.ts                         # 路由配置（需要添加）
```

## 实现步骤

### 步骤 1: 创建类型定义文件 ✅ 待完成
**文件**: `src/types/profession.ts`

**内容**:
```typescript
// 职业接口
export interface Profession {
  id: number
  name: string
  description: string
  mainCategory: number
  subCategory: number | null
  skills: string
  createdAt?: string
  updatedAt?: string
}

// 带显示属性的职业
export interface CareerWithDisplay extends Profession {
  icon?: string
  iconColor?: string
}

// 主分类
export interface ProfessionCategory {
  id: number
  title: string
  icon: string
  order: number
}

// 子分类
export interface Subcategory {
  id: number
  name: string
  order: number
}

// 分类映射
export interface CategoryMapping {
  mainCategoryId: number
  subcategories: Subcategory[]
}
```

---

### 步骤 2: 创建 CategoryNavigation 组件 ⏳ 待完成
**文件**: `src/components/career/CategoryNavigation.vue`

**功能**:
- 显示一级分类按钮（技术、商业、艺术等）
- 点击一级分类展开二级分类
- 支持分类筛选切换

**Props**:
- `categories`: 分类列表
- `categoryMapping`: 分类映射关系
- `activeFirstLvl`: 当前选中的一级分类
- `activeSecondLvl`: 当前选中的二级分类
- `searchText`: 搜索文本

**Emits**:
- `selectFirstLevel`: 选择一级分类
- `selectSecondLevel`: 选择二级分类

---

### 步骤 3: 创建 CareerCard 组件 ⏳ 待完成
**文件**: `src/components/career/CareerCard.vue`

**功能**:
- 显示职业卡片（图标、标题、描述、分类、技能）
- 点击跳转到职业详情
- Reddit 风格的卡片设计

**Props**:
- `career`: 职业数据
- `getCategoryName`: 获取分类名称的函数
- `getSubCategoryNameById`: 获取子分类名称的函数

**样式特点**:
- 白色背景，圆角卡片
- Hover 效果
- 图标使用 mdi 图标库
- 技能标签显示

---

### 步骤 4: 创建 CareerGrid 组件 ⏳ 待完成
**文件**: `src/components/career/CareerGrid.vue`

**功能**:
- 网格布局展示职业卡片
- 无限滚动加载更多
- 空状态提示
- 加载状态显示

**Props**:
- `displayedCareers`: 显示的职业列表
- `loading`: 加载状态
- `activeFirstLvl/activeSecondLvl`: 当前分类
- `searchText`: 搜索文本
- 其他辅助数据

**Emits**:
- `loadMoreCareers`: 加载更多
- `goToCareerDetail`: 跳转详情
- `goBackToSecondLevel`: 返回二级分类

---

### 步骤 5: 创建 CareerCenter 主页面 ⏳ 待完成
**文件**: `src/views/CareerCenter.vue`

**功能**:
- 集成所有子组件
- 管理状态（搜索、分类选择、职业列表）
- 处理数据加载（使用 Mock 数据）
- 职业申请对话框

**主要状态**:
```typescript
- careers: 职业列表
- filteredCareers: 筛选后的职业
- loading: 加载状态
- searchText: 搜索文本
- activeFirstLvl: 一级分类
- activeSecondLvl: 二级分类
- categories: 分类数据
- showApplicationDialog: 申请对话框
```

**Mock 数据**:
```typescript
// 示例分类数据
const categories = [
  { id: 1, title: '技术开发', icon: 'mdi-laptop', order: 1 },
  { id: 2, title: '产品设计', icon: 'mdi-palette', order: 2 },
  { id: 3, title: '商业运营', icon: 'mdi-briefcase', order: 3 },
  // ...
]

// 示例职业数据
const careers = [
  {
    id: 1,
    name: '前端工程师',
    description: '负责网站和应用的用户界面开发',
    mainCategory: 1,
    subCategory: 1,
    skills: 'Vue,React,TypeScript,CSS',
    icon: 'mdi-laptop',
    iconColor: 'primary'
  },
  // ...
]
```

---

### 步骤 6: 配置路由 ⏳ 待完成
**文件**: `src/router/index.ts`

**添加路由**:
```typescript
{
  path: '/career',
  name: 'CareerCenter',
  component: () => import('@/views/CareerCenter.vue'),
  meta: {
    title: '职业中心'
  }
}
```

---

### 步骤 7: 样式调整 ⏳ 待完成
**适配 Reddit 风格**:
- 使用圆角卡片 (border-radius: 12-24px)
- 白色背景 (#FFFFFF)
- 灰色边框 (#EDEFF1)
- 主题色按钮
- 扁平化设计，最小化阴影

---

## Mock 数据详细说明

### 分类数据
```typescript
// 主分类
const categories: ProfessionCategory[] = [
  { id: 1, title: '技术开发', icon: 'mdi-laptop', order: 1 },
  { id: 2, title: '产品设计', icon: 'mdi-palette', order: 2 },
  { id: 3, title: '商业运营', icon: 'mdi-briefcase', order: 3 },
  { id: 4, title: '市场营销', icon: 'mdi-bullhorn', order: 4 },
  { id: 5, title: '数据分析', icon: 'mdi-chart-bar', order: 5 }
]

// 分类映射
const categoryMapping: CategoryMapping[] = [
  {
    mainCategoryId: 1,
    subcategories: [
      { id: 101, name: '前端开发', order: 1 },
      { id: 102, name: '后端开发', order: 2 },
      { id: 103, name: '全栈开发', order: 3 },
      { id: 104, name: '移动开发', order: 4 }
    ]
  },
  {
    mainCategoryId: 2,
    subcategories: [
      { id: 201, name: 'UI设计', order: 1 },
      { id: 202, name: 'UX设计', order: 2 },
      { id: 203, name: '产品经理', order: 3 }
    ]
  },
  // ... 其他分类
]
```

### 职业数据（至少准备 20 条）
```typescript
const mockCareers: Profession[] = [
  {
    id: 1,
    name: '前端工程师',
    description: '负责开发网站和应用的用户界面，使用现代前端框架构建响应式、高性能的Web应用',
    mainCategory: 1,
    subCategory: 101,
    skills: 'Vue.js,React,TypeScript,HTML,CSS,JavaScript',
  },
  {
    id: 2,
    name: '后端工程师',
    description: '设计和开发服务器端应用程序，负责数据库设计、API开发和系统架构',
    mainCategory: 1,
    subCategory: 102,
    skills: 'Java,Spring Boot,MySQL,Redis,微服务',
  },
  // ... 更多职业数据
]
```

---

## 功能特性

### 核心功能
- ✅ 搜索职业（按名称、描述、技能）
- ⏳ 分类筛选（一级、二级分类）
- ⏳ 职业列表展示（网格布局）
- ⏳ 职业详情跳转
- ⏳ 职业申请表单

### 交互功能
- ⏳ 无限滚动加载更多
- ⏳ 空状态提示
- ⏳ 加载状态显示
- ⏳ 搜索结果高亮

### 视觉效果
- ⏳ 卡片 Hover 效果
- ⏳ 分类按钮激活状态
- ⏳ 平滑过渡动画
- ⏳ 响应式布局

---

## 实现顺序建议

1. **第一阶段**（基础结构）
   - ✅ 创建类型定义
   - ✅ 创建 CareerFilter 组件
   - ⏳ 准备 Mock 数据

2. **第二阶段**（展示组件）
   - ⏳ 创建 CareerCard 组件
   - ⏳ 创建 CategoryNavigation 组件
   - ⏳ 创建 CareerGrid 组件

3. **第三阶段**（主页面）
   - ⏳ 创建 CareerCenter 主页面
   - ⏳ 集成所有组件
   - ⏳ 实现状态管理和数据流

4. **第四阶段**（完善功能）
   - ⏳ 配置路由
   - ⏳ 添加职业申请对话框
   - ⏳ 测试和调整样式

---

## 注意事项

1. **类型安全**: 使用 TypeScript 确保类型正确
2. **组件复用**: 保持组件独立性，便于维护
3. **性能优化**: 大列表使用虚拟滚动或分页
4. **响应式设计**: 适配不同屏幕尺寸
5. **无障碍访问**: 添加适当的 aria 标签
6. **错误处理**: 处理空状态和加载失败

---

## 后续扩展

### API 集成
- 替换 Mock 数据为真实 API 调用
- 添加 loading 状态
- 错误处理和重试机制

### 功能增强
- 职业收藏功能
- 职业对比功能
- 职业推荐算法
- 个性化推荐

### 数据可视化
- 技能热度图
- 职业发展路径图
- 薪资分布图

---

## 当前进度

- ✅ CareerFilter 组件已完成
- ⏳ 等待创建其他组件
- ⏳ 等待创建主页面
- ⏳ 等待配置路由

---

## 下一步

请告知需要实现的具体步骤，我将按顺序完成：
1. 创建类型定义文件
2. 创建 CareerCard 组件
3. 创建 CategoryNavigation 组件
4. 创建 CareerGrid 组件
5. 创建 CareerCenter 主页面
6. 配置路由
