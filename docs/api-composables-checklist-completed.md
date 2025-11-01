# API Composables 实施 Checklist - 已完成部分

## ✅ 阶段 1：创建基础设施（已完成）

- [x] `web-ts/src/composables/config.ts` - 全局配置
- [x] `web-ts/src/composables/types.ts` - 类型定义
- [x] `web-ts/src/composables/utils.ts` - 工具函数

## ✅ 阶段 2：实现 Composables（已完成）

- [x] `useFetch` - 统一支持单个对象和列表
- [x] `useInfiniteScroll` - 无限滚动
- [x] `useCreate` - 创建资源
- [x] `useUpdate` - 更新资源
- [x] `useDelete` - 删除资源

**注意：** `useList` 已合并到 `useFetch`，不需要单独实现。

## ⏳ 下一步：试点重构

建议试点重构以下组件验证 Composables：

1. **useFetch（对象）**：`SelfView.vue` - 获取用户信息
2. **useFetch（列表）**：`CourseList.vue` - 加载热门课程
3. **useInfiniteScroll**：帖子列表组件
4. **useCreate**：`CareerCenter.vue` - 创建职业
5. **useUpdate**：`UserInfoTab.vue` - 更新用户信息
6. **useDelete**：`UserPosting.vue` - 删除帖子
