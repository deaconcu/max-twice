# API Composables 重构 Checklist

> 基于 `api-composables-design.md` 的组件重构清单
>
> 创建日期：2025-10-31
> 最后更新：2025-10-31

---

## 📊 总体进度

- **总组件数：** 28 个
- **已完成：** 7 个 ✅
- **部分完成：** 1 个 ⚠️
- **待重构：** 20 个 ⬜

**完成度：** 25% (7/28)

---

## ✅ 已完成的组件（7个）

### 管理后台（1个）
- [x] **RoadmapManagement.vue** - 路线图管理
  - ✅ 使用 `useInfiniteScroll` 替代分页逻辑
  - ✅ 使用 `useMutation` 处理更新/批准/拒绝/屏蔽
  - ✅ 减少代码 85%
  - 📍 `web-ts/src/components/admin/RoadmapManagement.vue`

### 内容模块（4个）
- [x] **CommentArea.vue** - 评论区域
  - ✅ 使用 `useInfiniteScroll` 加载评论
  - ✅ 使用 `useMutation` 发送评论/子评论
  - ✅ 使用 `useMutation` 点赞（带节流）
  - ✅ 减少代码 75%
  - 📍 `web-ts/src/components/read/CommentArea.vue`

- [x] **SubcommentArea.vue** - 子评论区域
  - ✅ 使用 `useMutation` 点赞（带节流）
  - ✅ 使用 `useMutation` 发送回复
  - ✅ 减少代码 60%
  - 📍 `web-ts/src/components/read/SubcommentArea.vue`

- [x] **AddArticle.vue** - 添加文章
  - ✅ 使用 `useMutation` 提交文章
  - ✅ 添加 loading 状态
  - ✅ 减少代码 60%
  - 📍 `web-ts/src/components/read/AddArticle.vue`

- [x] **ConfigContents.vue** - 配置目录
  - ✅ 使用 `useMutation` 更新目录
  - ✅ 统一错误处理
  - ✅ 减少代码 50%
  - 📍 `web-ts/src/components/read/ConfigContents.vue`

- [x] **AddContents.vue** - 添加目录
  - ✅ 使用 `useMutation` 提交目录
  - ✅ 使用 `useMutation` AI 生成
  - ✅ 减少代码 65%
  - 📍 `web-ts/src/components/read/AddContents.vue`

### 公共组件（1个）
- [x] **RightSidebar.vue** - 右侧边栏
  - ✅ 使用 `useFetch` 加载热门职业
  - ✅ 使用 `useFetch` 加载热门课程
  - ✅ 减少代码 60%
  - 📍 `web-ts/src/components/common/RightSidebar.vue`

---

## ⚠️ 部分完成的组件（1个）

### 管理后台（1个）
- [ ] **CourseManagement.vue** - 课程管理 ⚠️
  - ✅ 已导入 `useInfiniteScroll`, `useMutation`, `useFetch`
  - ⬜ 重构列表加载逻辑（复杂分类筛选）
  - ⬜ 重构批准/拒绝/屏蔽操作
  - ⬜ 重构编辑功能
  - ⬜ 重构 ID 查询功能
  - **复杂度：** 🔴 高（1000+ 行）
  - 📍 `web-ts/src/components/admin/CourseManagement.vue`

---

## 🔴 高优先级 - 管理后台组件（9个）

### 第一批（审核相关）
- [ ] **PostReview.vue** - 帖子审核
  - ⬜ 使用 `useInfiniteScroll` 加载帖子列表
  - ⬜ 使用 `useMutation` 审核操作（通过/拒绝/屏蔽）
  - **文件大小：** 754 行
  - **API 调用：** 5 处
  - **预计减少：** 70%
  - 📍 `web-ts/src/components/admin/PostReview.vue`

- [ ] **CommentReview.vue** - 评论审核
  - ⬜ 使用 `useInfiniteScroll` 加载评论列表
  - ⬜ 使用 `useMutation` 审核操作
  - **文件大小：** 686 行
  - **API 调用：** 5 处
  - **预计减少：** 70%
  - 📍 `web-ts/src/components/admin/CommentReview.vue`

- [ ] **MemoryCardReview.vue** - 记忆卡片审核
  - ⬜ 使用 `useInfiniteScroll` 加载卡片列表
  - ⬜ 使用 `useMutation` 审核操作
  - **API 调用：** 多处
  - 📍 `web-ts/src/components/admin/MemoryCardReview.vue`

### 第二批（实体管理）
- [ ] **ProfessionManagement.vue** - 职业管理
  - ⬜ 使用 `useInfiniteScroll` 加载职业列表
  - ⬜ 使用 `useMutation` 批准/拒绝/屏蔽操作
  - ⬜ 使用 `useMutation` 编辑职业信息
  - **文件大小：** 934 行
  - **API 调用：** 5 处
  - **预计减少：** 75%
  - 📍 `web-ts/src/components/admin/ProfessionManagement.vue`

- [ ] **UserManagement.vue** - 用户管理
  - ⬜ 使用 `useInfiniteScroll` 加载用户列表
  - ⬜ 使用 `useMutation` 用户操作（封禁/解封）
  - **文件大小：** 367 行
  - **API 调用：** 4 处
  - **预计减少：** 65%
  - 📍 `web-ts/src/components/admin/UserManagement.vue`

- [ ] **NodeManagement.vue** - 节点管理
  - ⬜ 使用 `useInfiniteScroll` 或 `useFetch` 加载节点
  - ⬜ 使用 `useMutation` 节点操作
  - **API 调用：** 4 处
  - 📍 `web-ts/src/components/admin/NodeManagement.vue`

### 第三批（系统功能）
- [ ] **DeckQuery.vue** - 卡片组查询
  - ⬜ 使用 `useFetch` 查询功能
  - ⬜ 使用 `useInfiniteScroll` 结果列表
  - 📍 `web-ts/src/components/admin/DeckQuery.vue`

- [ ] **OperationLogManagement.vue** - 操作日志管理
  - ⬜ 使用 `useInfiniteScroll` 加载日志列表
  - 📍 `web-ts/src/components/admin/OperationLogManagement.vue`

- [ ] **SystemConfiguration.vue** - 系统配置
  - ⬜ 使用 `useFetch` 加载配置
  - ⬜ 使用 `useMutation` 保存配置
  - 📍 `web-ts/src/components/admin/SystemConfiguration.vue`

- [ ] **SystemOperations.vue** - 系统操作
  - ⬜ 使用 `useMutation` 系统操作
  - 📍 `web-ts/src/components/admin/SystemOperations.vue`

---

## 🟡 中优先级 - 内容组件（3个）

- [ ] **PostingList.vue** - 帖子列表
  - ⬜ 使用 `useInfiniteScroll` 无限滚动加载
  - ⬜ 使用 `useMutation` 点赞操作（带节流）
  - **文件大小：** 828 行
  - **预计减少：** 65%
  - 📍 `web-ts/src/components/read/PostingList.vue`

- [ ] **SinglePost.vue** - 单个帖子
  - ⬜ 使用 `useFetch` 加载帖子数据
  - ⬜ 使用 `useMutation` 点赞/收藏操作
  - 📍 `web-ts/src/components/read/SinglePost.vue`

- [ ] **TiptapInput.vue** - 富文本编辑器
  - ⬜ 使用 `useMutation` 自动保存（带防抖）
  - ⬜ 使用 `useMutation` 图片上传
  - 📍 `web-ts/src/components/read/TiptapInput.vue`

---

## 🟡 中优先级 - 记忆卡片组件（4个）

- [ ] **CreateDeckDialog.vue** - 创建卡片组
  - ⬜ 使用 `useMutation` 创建操作
  - 📍 `web-ts/src/components/memory/CreateDeckDialog.vue`

- [ ] **DeckDetailDialog.vue** - 卡片组详情
  - ⬜ 使用 `useFetch` 加载详情
  - ⬜ 使用 `useMutation` 更新操作
  - 📍 `web-ts/src/components/memory/DeckDetailDialog.vue`

- [ ] **MemoryCardList.vue** - 记忆卡片列表
  - ⬜ 使用 `useInfiniteScroll` 加载卡片
  - **文件大小：** 299 行
  - 📍 `web-ts/src/components/memory/MemoryCardList.vue`

- [ ] **MemoryCardSidebar.vue** - 记忆卡片侧边栏
  - ⬜ 使用 `useFetch` 加载侧边栏数据
  - 📍 `web-ts/src/components/memory/MemoryCardSidebar.vue`

---

## 🟡 中优先级 - 学习组件（2个）

- [ ] **CourseLearningContainer.vue** - 课程学习容器
  - ⬜ 使用 `useFetch` 加载学习进度
  - ⬜ 使用 `useMutation` 更新进度
  - 📍 `web-ts/src/components/learning/CourseLearningContainer.vue`

- [ ] **RoadmapLearningContainer.vue** - 路线图学习容器
  - ⬜ 使用 `useFetch` 加载路线图数据
  - ⬜ 使用 `useMutation` 更新学习状态
  - 📍 `web-ts/src/components/learning/RoadmapLearningContainer.vue`

---

## 🟢 低优先级 - 其他组件（1个）

- [ ] **CategorySelector.vue** - 分类选择器
  - ⬜ 使用 `useFetch` 加载分类数据
  - 📍 `web-ts/src/components/common/CategorySelector.vue`

---

## 📋 重构标准检查项

每个组件重构时需确认以下项目：

### 代码质量
- [ ] 移除手动的 `loading.value = true/false`
- [ ] 移除手动的 `try-catch` 错误处理
- [ ] 移除手动的 `if (response.code === 200)` 判断
- [ ] 使用相应的 composable（useFetch/useInfiniteScroll/useMutation）

### 功能完整性
- [ ] 保持原有功能不变
- [ ] 添加 loading 状态到 UI（如按钮）
- [ ] 统一成功/失败提示
- [ ] 适当添加防抖/节流（如点赞、搜索）

### 代码规范
- [ ] 通过 `npm run typecheck`
- [ ] 通过 `npm run lint`
- [ ] 符合 ESLint 和 Prettier 规则
- [ ] 代码注释简洁明了

### 测试验证
- [ ] 功能测试通过
- [ ] Loading 状态正确显示
- [ ] 错误处理正确
- [ ] 无控制台错误

---

## 🎯 批次执行计划

### 批次 1：管理后台审核功能（预计 2-3 小时）
1. PostReview.vue
2. CommentReview.vue
3. MemoryCardReview.vue

**目标：** 统一审核流程，减少 70% 重复代码

### 批次 2：管理后台实体管理（预计 2-3 小时）
4. ProfessionManagement.vue
5. UserManagement.vue
6. NodeManagement.vue
7. CourseManagement.vue（完成剩余部分）

**目标：** 统一管理界面模式

### 批次 3：内容和列表功能（预计 1-2 小时）
8. PostingList.vue
9. SinglePost.vue
10. TiptapInput.vue

**目标：** 优化内容展示和编辑体验

### 批次 4：记忆卡片功能（预计 1-2 小时）
11. CreateDeckDialog.vue
12. DeckDetailDialog.vue
13. MemoryCardList.vue
14. MemoryCardSidebar.vue

**目标：** 统一卡片管理逻辑

### 批次 5：学习和其他功能（预计 1 小时）
15. CourseLearningContainer.vue
16. RoadmapLearningContainer.vue
17. CategorySelector.vue
18. DeckQuery.vue
19. OperationLogManagement.vue
20. SystemConfiguration.vue
21. SystemOperations.vue

**目标：** 完成所有剩余组件

---

## 📊 预期总体收益

| 指标 | 当前状态 | 重构后 | 改善幅度 |
|------|---------|--------|---------|
| **总代码行数** | ~15,000 行 | ~6,000 行 | **-60%** |
| **重复代码** | ~5,000 行 | ~500 行 | **-90%** |
| **API 调用代码** | ~150 处 × 30行 | ~150 处 × 8行 | **-73%** |
| **错误处理统一性** | 分散 | 100% 统一 | **+100%** |
| **类型安全覆盖** | 部分 | 100% 完整 | **+50%** |
| **代码可维护性** | 中等 | 优秀 | **+80%** |

---

## 🚀 快速开始

### 重构一个新组件的步骤：

1. **阅读原组件代码**
   ```bash
   # 查看组件大小和复杂度
   wc -l path/to/component.vue
   grep -c "const response = await" path/to/component.vue
   ```

2. **确定使用的 composable**
   - 列表加载/分页 → `useInfiniteScroll`
   - 单次数据获取 → `useFetch`
   - 创建/更新/删除 → `useMutation`

3. **导入 composables**
   ```typescript
   import { useFetch } from '@/composables/useFetch'
   import { useInfiniteScroll } from '@/composables/useInfiniteScroll'
   import { useMutation } from '@/composables/useMutation'
   ```

4. **重构 API 调用**
   - 替换手动逻辑为 composable 调用
   - 移除 loading/error 状态管理
   - 移除 try-catch 错误处理

5. **更新模板**
   - 添加 `:loading` 到按钮
   - 使用 composable 返回的状态

6. **测试验证**
   ```bash
   npm run typecheck
   npm run lint
   # 手动功能测试
   ```

---

## 📝 注意事项

1. **CourseManagement.vue 优先级**
   - 虽然已导入 composables，但实际重构工作量大
   - 建议在熟悉重构模式后再处理

2. **测试要求**
   - 每个组件重构后必须功能测试
   - 确保 loading 状态正确显示
   - 验证错误处理是否正常

3. **代码提交**
   - 建议每完成 2-3 个组件提交一次
   - 提交信息清晰说明重构的组件

4. **文档更新**
   - 重构完成后更新本 checklist
   - 标记完成日期和负责人

---

## 🔗 相关文档

- [API Composables 设计文档](./api-composables-design.md)
- [代码规范](../web-ts/CODE_STANDARDS.md)
- [项目说明](../CLAUDE.md)

---

**最后更新：** 2025-10-31
**维护者：** Claude Code
**版本：** v1.0
