# 内容管理接口文档

## 接口概览

| 接口 | 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|------|
| 内容操作 | POST | `/api/v1/contents` | 是 | 执行内容相关操作（选择目录、固定帖子等） |

---

## 1. 内容操作

### 接口信息
- **路径**: `POST /api/v1/contents`
- **认证**: 需要登录
- **限流**: 80次/分钟 (按用户)

### 请求参数

**Body (JSON)**:
```json
{
  "path": "1-123",
  "courseId": 456,
  "postingId": 789,
  "action": 1
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| path | String | 是 | @NotBlank | 目录路径，格式：`tocIndex-nodeId`，例如 "1-123" |
| courseId | Long | 是 | @NotNull, @Positive | 课程ID，必须大于0 |
| postingId | Long | 是 | @NotNull, @Positive | 帖子ID，必须大于0 |
| action | Integer | 是 | @NotNull, @Min(1), @Max(4) | 操作类型：1-选择目录, 2-取消选择, 3-固定帖子, 4-取消固定 |

### 请求头
```
token: your-auth-token
```

### 操作类型说明

#### action = 1: 选择目录（Choose）
- **用途**: 将某个内容帖子（目录型帖子）设置为课程目录的一部分
- **业务逻辑**:
  1. 验证帖子是否为内容型帖子（包含目录结构）
  2. 验证课程是否存在
  3. 解析帖子内容为目录结构（JSON格式）
  4. 在用户的课程目录中标记该帖子为已选择
  5. 更新用户的课程目录版本

#### action = 2: 取消选择（Unchoose）
- **用途**: 取消已选择的目录内容
- **业务逻辑**:
  1. 验证课程是否存在
  2. 查找用户的课程目录
  3. 移除指定路径的目录选择标记
  4. 更新用户的课程目录版本

#### action = 3: 固定帖子（Pin）
- **用途**: 将某个帖子固定到目录节点上，使其在该节点下置顶显示
- **业务逻辑**:
  1. 验证课程是否存在
  2. 查找用户的课程目录
  3. 在指定路径下添加帖子ID到固定列表
  4. 更新用户的课程目录版本

#### action = 4: 取消固定（Unpin）
- **用途**: 取消帖子的固定状态
- **业务逻辑**:
  1. 验证课程是否存在
  2. 查找用户的课程目录
  3. 从指定路径下移除帖子ID
  4. 更新用户的课程目录版本

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1703001234567
}
```

**失败 - 参数验证失败 (1002)**:
```json
{
  "code": 1002,
  "message": "路径不能为空",
  "timestamp": 1703001234567
}
```

**失败 - 用户未登录 (1101)**:
```json
{
  "code": 1101,
  "message": "用户未登录",
  "timestamp": 1703001234567
}
```

**失败 - 课程不存在 (1007)**:
```json
{
  "code": 1007,
  "message": "课程不存在",
  "timestamp": 1703001234567
}
```

**失败 - 帖子不存在 (1007)**:
```json
{
  "code": 1007,
  "message": "帖子不存在",
  "timestamp": 1703001234567
}
```

**失败 - 用户目录不存在 (1007)**:
```json
{
  "code": 1007,
  "message": "用户课程目录不存在",
  "timestamp": 1703001234567
}
```

**失败 - 操作类型不支持 (1008)**:
```json
{
  "code": 1008,
  "message": "不支持的操作",
  "timestamp": 1703001234567
}
```

**失败 - 访问过于频繁 (2301)**:
```json
{
  "code": 2301,
  "message": "访问过于频繁，请稍后再试",
  "timestamp": 1703001234567
}
```

### 业务说明

1. **路径格式**:
   - 格式：`tocIndex-nodeId`
   - `tocIndex`: 目录版本索引（1-9），表示用户使用的目录版本
   - `nodeId`: 节点ID，表示操作的目标节点
   - 示例：`"1-123"` 表示使用第1版目录，操作节点123

2. **目录版本管理**:
   - 每个用户对每个课程可以有独立的目录结构
   - 目录使用哈希去重，相同结构的目录共享同一份数据
   - 用户目录可以有多个版本（最多9个），通过 tocIndex 区分

3. **选择目录 (action=1)**:
   - 只能选择内容型帖子（PostType.CONTENTS）
   - 帖子内容必须是有效的目录结构 JSON
   - 选择后会在目录中添加特殊标记字段（`+`）

4. **固定帖子 (action=3/4)**:
   - 可以将任意帖子固定到目录节点
   - 固定的帖子会在节点下置顶显示
   - 使用特殊标记字段（`^`）存储固定帖子列表
   - 固定数量有上限（由配置决定）

5. **权限要求**:
   - 所有操作都需要登录
   - 只能操作自己的课程目录
   - 不能操作他人的目录结构

6. **幂等性**:
   - 重复执行相同操作不会报错
   - 例如：重复固定同一个帖子，返回成功
   - 取消不存在的固定，返回成功

### 使用示例

#### 1. 选择目录内容

```bash
curl -X POST http://localhost:9202/api/v1/contents \
  -H "Content-Type: application/json" \
  -H "token: your-auth-token" \
  -d '{
    "path": "1-123",
    "courseId": 456,
    "postingId": 789,
    "action": 1
  }'
```

**场景**: 用户在学习课程456，想要使用帖子789作为节点123的目录内容。

#### 2. 取消选择目录

```bash
curl -X POST http://localhost:9202/api/v1/contents \
  -H "Content-Type: application/json" \
  -H "token: your-auth-token" \
  -d '{
    "path": "1-123",
    "courseId": 456,
    "postingId": 789,
    "action": 2
  }'
```

**场景**: 取消之前选择的目录内容，恢复默认目录。

#### 3. 固定帖子

```bash
curl -X POST http://localhost:9202/api/v1/contents \
  -H "Content-Type: application/json" \
  -H "token: your-auth-token" \
  -d '{
    "path": "1-123",
    "courseId": 456,
    "postingId": 789,
    "action": 3
  }'
```

**场景**: 将帖子789固定到节点123下，使其在该节点下置顶显示。

#### 4. 取消固定

```bash
curl -X POST http://localhost:9202/api/v1/contents \
  -H "Content-Type: application/json" \
  -H "token: your-auth-token" \
  -d '{
    "path": "1-123",
    "courseId": 456,
    "postingId": 789,
    "action": 4
  }'
```

**场景**: 取消帖子789的固定状态。

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 1002 | 参数异常 |
| 1007 | 资源不存在 |
| 1008 | 不支持的操作 |
| 1101 | 用户未登录 |
| 2301 | 访问过于频繁，请稍后再试 |

---

## 限流说明

### 全局限流
- **容量**: 80次
- **补充速率**: 每分钟补充80次
- **限流维度**: 按用户ID

### 限流错误响应
```json
{
  "code": 2301,
  "message": "访问过于频繁，请稍后再试",
  "timestamp": 1703001234567
}
```

---

## 业务流程说明

### 使用场景

#### 场景1: 自定义课程目录
1. 用户学习某个课程
2. 发现社区有人发布了更好的目录结构（内容型帖子）
3. 使用 `action=1` 选择该帖子作为自己的目录
4. 系统会根据帖子内容重新组织课程目录

#### 场景2: 固定重要帖子
1. 用户在学习某个节点时发现一个特别有用的帖子
2. 使用 `action=3` 将该帖子固定到节点上
3. 下次访问该节点时，固定的帖子会出现在最前面
4. 方便快速访问重要内容

#### 场景3: 恢复默认目录
1. 用户之前选择了自定义目录，但不满意
2. 使用 `action=2` 取消选择
3. 系统恢复使用默认的课程目录结构

### 目录数据结构

目录内容使用 JSON 格式存储，结构如下：

```json
{
  "123": {
    "456": {},
    "789": {}
  },
  "+": 999,
  "^": "111,222,333"
}
```

**字段说明**:
- 数字键（如 "123"）: 节点ID，值为子节点的嵌套结构
- `"+"`: 选中字段，值为被选择的内容帖子ID
- `"^"`: 固定字段，值为固定帖子ID列表（逗号分隔）
- 空对象 `{}`: 叶子节点，没有子节点

---

## 技术实现细节

### 1. 目录版本管理
- 使用哈希值（SHA）标识目录内容
- 相同内容的目录共享同一个哈希
- 用户可以有多个目录版本（最多9个）
- 通过 `tocIndex` 选择使用哪个版本

### 2. 引用计数
- 每个目录哈希都有引用计数
- 当用户选择某个目录时，引用计数+1
- 当用户放弃某个目录时，引用计数-1
- 引用计数为0的目录可以被清理

### 3. 并发安全
- 目录更新操作使用事务保证一致性
- 哈希计算保证目录版本的唯一性

---

## 注意事项

1. **路径格式严格**:
   - 必须是 `tocIndex-nodeId` 格式
   - tocIndex 范围：1-9
   - nodeId 必须是有效的节点ID

2. **内容帖子要求**:
   - action=1 时，postingId 必须指向内容型帖子（PostType.CONTENTS）
   - 帖子内容必须是有效的目录 JSON 结构
   - 不能选择普通帖子或文章作为目录

3. **固定帖子限制**:
   - 每个节点的固定帖子数量有上限（配置决定）
   - 超过上限后无法继续固定
   - 固定列表使用逗号分隔存储

4. **用户目录隔离**:
   - 每个用户都有独立的目录结构
   - 操作只影响当前用户的目录
   - 不会影响其他用户或课程的默认目录

5. **目录初始化**:
   - 用户首次访问课程时，系统会自动创建默认目录
   - 如果用户目录不存在，操作会失败
   - 需要先通过页面访问接口初始化目录

---

## 前端集成

### API 调用

```typescript
// 定义在 web/src/api/modules/page.ts
import { post } from '@/api'

// 选择目录
export const chooseContents = (path: string, courseId: number, postingId: number) => {
  return post<void>('/api/v1/contents', {
    path,
    courseId,
    postingId,
    action: 1
  })
}

// 取消选择
export const unchooseContents = (path: string, courseId: number, postingId: number) => {
  return post<void>('/api/v1/contents', {
    path,
    courseId,
    postingId,
    action: 2
  })
}

// 固定帖子
export const pinPost = (path: string, courseId: number, postingId: number) => {
  return post<void>('/api/v1/contents', {
    path,
    courseId,
    postingId,
    action: 3
  })
}

// 取消固定
export const unpinPost = (path: string, courseId: number, postingId: number) => {
  return post<void>('/api/v1/contents', {
    path,
    courseId,
    postingId,
    action: 4
  })
}
```

### 使用示例

```vue
<script setup lang="ts">
import { chooseContents, pinPost } from '@/api/modules/page'

// 选择目录
const handleChooseContents = async (postId: number) => {
  try {
    await chooseContents('1-123', courseId.value, postId)
    // 刷新页面数据
    await refreshPage()
  } catch (error) {
    console.error('选择目录失败', error)
  }
}

// 固定帖子
const handlePinPost = async (postId: number) => {
  try {
    await pinPost('1-123', courseId.value, postId)
    // 刷新页面数据
    await refreshPage()
  } catch (error) {
    console.error('固定帖子失败', error)
  }
}
</script>
```

---

## 相关接口

- **获取课程目录**: `GET /api/v1/toc?courseId={courseId}`
- **页面聚合数据**: `GET /api/v1/pages/read?courseId={courseId}&path={path}`
- **创建内容帖**: `POST /api/v1/posts` (type=1)

---

## 常见问题

### Q1: 为什么需要传 postingId？
A: action=2 (取消选择) 时理论上不需要 postingId，但为了保持接口一致性，所有操作都要求传入。取消选择时会忽略该参数。

### Q2: tocIndex 是什么？
A: 目录版本索引。用户可以为同一课程维护多个不同的目录版本（例如：默认版、自定义版1、自定义版2），通过 tocIndex 切换。

### Q3: 如何获取当前的目录结构？
A: 使用页面聚合接口 `GET /api/v1/pages/read`，返回的数据中包含 `toc` 字段。

### Q4: 固定帖子的数量上限是多少？
A: 由配置 `app.contents.max-pinned-items` 决定，默认为10个。

### Q5: 选择目录后其他用户能看到吗？
A: 不能。目录选择是用户级别的，每个用户都有独立的目录结构，互不影响。

---

*此文档用于指导前后端开发和接口对接，确保数据结构和业务逻辑的一致性*
