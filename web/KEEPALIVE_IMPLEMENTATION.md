# KeepAlive 实现文档

## 已实现的功能

### 1. ProfilePage 缓存

**文件：`src/App.vue`**

```vue
<router-view v-slot="{ Component }">
  <keep-alive :include="['ProfilePage']">
    <component :is="Component" />
  </keep-alive>
</router-view>
```

- ProfilePage 组件会被缓存
- 从文章详情页返回时，保留所有状态（滚动位置、已加载数据、Tab选择等）

### 2. ProfilePage 配置

**文件：`src/views/profile/ProfilePage.vue`**

```ts
// 定义组件名称（KeepAlive 必需）
export default {
  name: 'ProfilePage',
}

// 添加 onActivated 钩子
onActivated(() => {
  console.log('ProfilePage activated')
  // 从其他页面返回时触发
})
```

## 工作原理

### 用户操作流程

1. **进入 ProfilePage（第一次）**
   ```
   创建组件 → setup() → onMounted() → 显示
   ```

2. **切换 Tab（ArticlesTab → CommentsTab）**
   ```
   每个 Tab 首次显示时会加载数据
   已显示过的 Tab 从缓存恢复
   ```

3. **点击文章进入详情页**
   ```
   ProfilePage: onDeactivated() → 缓存（不销毁）
   详情页: 正常创建和显示
   ```

4. **点击后退返回 ProfilePage**
   ```
   ProfilePage: onActivated() → 从缓存恢复
   ✅ 滚动位置保留
   ✅ 当前 Tab 保留
   ✅ 已加载的数据保留
   ✅ 瞬间恢复
   ```

## Tab 数据加载策略

### 当前实现

各个 Tab 组件使用 `useInfiniteScroll` 加载数据：

```ts
const { data, loading, loadMore } = useInfiniteScroll({
  fetchFn: async (params) => {
    // 加载数据
  }
})
```

**行为：**
- ✅ 首次进入 Tab：加载数据
- ✅ 切换到其他 Tab：新 Tab 首次加载数据
- ✅ 切换回之前的 Tab：从缓存恢复（不重新加载）
- ✅ 从详情页返回：数据保留，滚动位置保留

### 如果需要强制刷新

可以在特定情况下触发刷新：

```vue
<script setup>
import { onActivated } from 'vue'

onActivated(() => {
  // 选项1: 总是刷新
  loadArticles({ reset: true })

  // 选项2: 超过5分钟才刷新
  const now = Date.now()
  if (now - lastRefreshTime.value > 5 * 60 * 1000) {
    loadArticles({ reset: true })
  }
})
</script>
```

## 生命周期对比

### 不使用 KeepAlive

```
ProfilePage:
  进入 → setup() → onMounted()
  离开 → onBeforeUnmount() → onUnmounted() ❌ 销毁
  返回 → setup() → onMounted() ❌ 重新创建
```

### 使用 KeepAlive

```
ProfilePage:
  进入 → setup() → onMounted()
  离开 → onDeactivated() ✅ 缓存
  返回 → onActivated() ✅ 恢复
```

## 内存占用

```
典型的 ProfilePage 缓存：
- 组件实例: ~10KB
- 文章列表数据(100条): ~200KB
- DOM 树: ~2-3MB
- 总计: ~2.5MB（可接受）
```

## 调试

打开浏览器控制台，可以看到：

```
首次进入:
（无输出）

离开页面:
（无输出，静默缓存）

返回页面:
ProfilePage activated
```

## 优缺点

### 优点
- ✅ 用户体验极佳：瞬间恢复，滚动位置完美保留
- ✅ 实现简单：只需几行代码
- ✅ 性能好：避免重复渲染和数据请求
- ✅ 自动保持状态：Tab 选择、表单输入、滚动位置等

### 缺点
- ❌ 占用内存：缓存组件会占用内存
- ❌ 数据可能过期：需要手动刷新机制
- ❌ 调试复杂：组件不会重新创建，可能遗留状态

## 最佳实践

1. **只缓存必要的页面**
   - ✅ 列表页（ProfilePage）
   - ❌ 详情页（通常不需要）
   - ❌ 表单页（可能有问题）

2. **提供手动刷新**
   - 下拉刷新
   - 刷新按钮
   - 顶部"有新内容"提示

3. **监控内存**
   - 使用 `max` 限制缓存数量
   - 清理不必要的缓存

4. **考虑数据时效性**
   - 在 `onActivated` 中检查数据是否过期
   - 超过一定时间自动刷新

## 未来优化建议

如果需要更精细的控制，可以：

1. **添加刷新按钮**
   ```vue
   <v-btn @click="refreshCurrentTab">
     <v-icon>mdi-refresh</v-icon>
     刷新
   </v-btn>
   ```

2. **智能刷新策略**
   ```ts
   const lastRefreshTime = ref<Record<string, number>>({})

   onActivated(() => {
     const tabKey = activeTab.value
     const now = Date.now()
     const lastTime = lastRefreshTime.value[tabKey] || 0

     // 超过5分钟，静默刷新
     if (now - lastTime > 5 * 60 * 1000) {
       refreshTab(tabKey, { silent: true })
     }
   })
   ```

3. **显示数据更新时间**
   ```vue
   <div class="text-caption text-grey">
     最后更新: {{ lastUpdateTime }}
   </div>
   ```

## 总结

当前实现已经满足基本需求：
- ✅ 从文章详情返回时保留滚动位置
- ✅ Tab 切换正常工作
- ✅ 数据加载逻辑不变

如果未来需要更复杂的刷新策略，可以参考上述"未来优化建议"部分。
