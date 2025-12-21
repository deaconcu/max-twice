# 职业管理接口文档 (ProfessionsController)

## 基本信息

- **Controller**: `ProfessionsController.java`
- **基础路径**: `/api/v1`
- **Rate Limit**: 40 requests/minute (per user)
- **前端 API**: `web/src/api/modules/profession.ts`

## DTO 类型说明

### ProfessionDTO (职业信息)
用途：职业完整信息，包含所有字段

```json
{
  "id": 1,
  "name": "Java开发工程师",
  "description": "负责Java后端开发工作",
  "price": "15k-25k",
  "skills": "Java, Spring Boot, MySQL, Redis",
  "mainCategory": 1,
  "subCategory": 2,
  "icon": "mdi-coffee",
  "learnerCount": 500,
  "createdAt": "2025-01-15T10:30:00"
}
```

**字段说明**：
- `id` (Long): 职业ID
- `name` (String): 职业名称
- `description` (String): 职业描述
- `price` (String): 薪资范围（如：15k-25k）
- `skills` (String): 所需技能，逗号分隔
- `mainCategory` (Integer): 主分类ID（如：技术、设计、运营等）
- `subCategory` (Integer): 子分类ID（如：Java、前端、产品等）
- `icon` (String): 职业图标（Material Design Icons）
- `learnerCount` (Integer): 学习人数，当前学习该职业相关内容的用户数量
- `createdAt` (LocalDateTime): 创建时间

**使用场景**：
- 职业列表页面
- 职业详情页面
- 热门职业排行榜

---

## 接口列表

## 1. 获取职业详情

**接口路径**: `GET /api/v1/professions/{id}`

**是否需要登录**: 否

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 职业ID，必须大于0 |

**返回类型**: `ProfessionDTO`

**返回示例**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "Java开发工程师",
    "description": "负责Java后端开发工作",
    "price": "15k-25k",
    "skills": "Java, Spring Boot, MySQL, Redis",
    "mainCategory": 1,
    "subCategory": 2,
    "icon": "mdi-coffee",
    "learnerCount": 500,
    "createdAt": "2025-01-15T10:30:00"
  }
}
```

**前端调用**:
```typescript
// API 调用
professionApi.getProfession(id)

// 实际使用 (CareerDetailPage.vue:453)
const { data: profession } = useFetch<Profession>({
  fetchFn: () => professionApi.getProfession(careerId.value),
  immediate: true,
  defaultValue: null
})
```

**使用场景**:
- `CareerDetailPage.vue` - 职业详情页面

---

## 2. 获取职业列表

**接口路径**: `GET /api/v1/professions`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| lastId | Long | 否 | 分页游标，上一页最后一条数据的ID |
| mainCategory | Integer | 否 | 主分类ID，必须大于0 |
| subCategory | Integer | 否 | 子分类ID，必须大于0（需要与mainCategory一起使用） |

**参数组合规则**:
1. **按主分类筛选**: 传 `mainCategory`（可选 `lastId`）
   - 返回该主分类下的所有已发布职业
   - 支持游标分页
2. **按主分类+子分类筛选**: 传 `mainCategory` + `subCategory`（可选 `lastId`）
   - 返回该分类下的所有已发布职业
   - 支持游标分页
3. **获取所有已发布职业**: 不传分类参数（可选 `lastId`）
   - 返回所有已发布职业
   - 支持游标分页

**返回类型**: `List<ProfessionDTO>`

**返回示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "Java开发工程师",
      "description": "负责Java后端开发工作",
      "price": "15k-25k",
      "skills": "Java, Spring Boot, MySQL, Redis",
      "mainCategory": 1,
      "subCategory": 2,
      "icon": "mdi-coffee",
      "learnerCount": 500,
      "createdAt": "2025-01-15T10:30:00"
    }
  ]
}
```

**前端调用**:
```typescript
// API 调用
professionApi.getProfessionsByCategory(lastId?, mainCategory?, subCategory?)

// 实际使用 (CareerListPage.vue:477-482)
const fetchFn = currentCategory.value
  ? () => professionApi.getProfessionsByCategory(
      lastId.value,
      currentCategory.value!.mainCategory,
      currentCategory.value!.subCategory
    )
  : () => professionApi.getProfessionsByCategory(lastId.value)
```

**使用场景**:
- `CareerListPage.vue` - 职业列表页，支持分类筛选和分页

---

## 3. 获取热门职业

**接口路径**: `GET /api/v1/professions/hot`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| limit | Integer | 否 | 10 | 返回数量，必须大于0 |

**返回类型**: `List<ProfessionDTO>`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "Java开发工程师",
      "description": "负责Java后端开发工作",
      "price": "15k-25k",
      "skills": "Java, Spring Boot, MySQL, Redis",
      "mainCategory": 1,
      "subCategory": 2,
      "icon": "mdi-coffee",
      "learnerCount": 1000,
      "createdAt": "2025-01-15T10:30:00"
    }
  ]
}
```

**前端调用**:
```typescript
// API 调用
professionApi.getHotProfessions(limit?)

// 实际使用 (CareerListPage.vue:405)
const { data: hotCareersData } = useFetch<Profession[]>({
  fetchFn: () => professionApi.getHotProfessions(15),
  immediate: true,
  defaultValue: []
})
```

**使用场景**:
- `CareerListPage.vue` - 职业列表页热门推荐
- 首页热门职业展示

---

## 5. 创建职业

**接口路径**: `POST /api/v1/professions`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体** (`CreateProfessionRequest`):
```json
{
  "name": "Java开发工程师",
  "description": "负责Java后端开发工作，参与系统架构设计与优化",
  "mainCategory": 1,
  "subCategory": 2,
  "skills": "Java, Spring Boot, MySQL, Redis, 微服务架构"
}
```

**请求参数说明**:
- `name` (String, 必填): 职业名称
  - 验证规则: 不能为空，最大长度100字符
- `description` (String, 必填): 职业描述
  - 验证规则: 不能为空，最大长度500字符
- `mainCategory` (Integer, 必填): 主分类ID
- `subCategory` (Integer, 必填): 子分类ID
- `skills` (String, 可选): 技能要求
  - 验证规则: 最大长度1000字符

**返回类型**: `String`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

**前端调用**:
```typescript
// API 调用
professionApi.createProfession(professionData)

// 实际使用 (CareerListPage.vue:751)
const { execute: executeCreateProfession } = useMutation(
  (data: {
    name: string
    description: string
    mainCategory: number
    subCategory: number
    skills: string
  }) => professionApi.createProfession(data),
  {
    successMessage: t('careerCenter.application.submittedSuccess'),
    onSuccess: () => {
      closeApplicationDialog()
      // 刷新职业列表
    }
  }
)

// 调用方式
await executeCreateProfession({
  name: 'Java开发工程师',
  description: '负责Java后端开发工作',
  mainCategory: 1,
  subCategory: 2,
  skills: 'Java, Spring Boot'
})
```

**使用场景**:
- `CareerListPage.vue` - 申请创建新职业对话框

**注意**:
- 创建的职业状态默认为待审核（state=0）
- 需要管理员审核后才会发布

---

## 6. 搜索职业

**接口路径**: `GET /api/v1/professions/search`

**是否需要登录**: 否

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 是 | 搜索关键词，不能为空 |

**返回类型**: `List<ProfessionDTO>`

**返回示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "Java开发工程师",
      "description": "负责Java后端开发工作",
      "price": "15k-25k",
      "skills": "Java, Spring Boot, MySQL, Redis",
      "mainCategory": 1,
      "subCategory": 2,
      "icon": "mdi-coffee",
      "learnerCount": 500,
      "createdAt": "2025-01-15T10:30:00"
    }
  ]
}
```

**前端调用**:
```typescript
// API 调用
professionApi.searchProfessions(keyword)
```

**使用场景**:
- 职业搜索功能

---

## 7. 更新职业（管理接口）

**接口路径**: `PUT /api/v1/admin/contents/profession/{id}`

**是否需要登录**: 是（需要管理员权限）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 职业ID，必须大于0 |

**请求体** (`UpdateProfessionRequest`):
```json
{
  "name": "Java高级开发工程师",
  "description": "负责Java后端高级开发工作",
  "price": "20k-35k",
  "skills": "Java, Spring Boot, MySQL, Redis, 微服务, 分布式",
  "mainCategory": 1,
  "subCategory": 2,
  "icon": "mdi-coffee",
  "reason": "更新职业信息"
}
```

**请求参数说明**:
- `name` (String, 必填): 职业名称，最大长度100字符
- `description` (String, 可选): 职业描述，最大长度500字符
- `price` (String, 可选): 薪资范围，最大长度50字符
- `skills` (String, 可选): 技能要求，最大长度1000字符
- `mainCategory` (Integer, 可选): 主分类ID
- `subCategory` (Integer, 可选): 子分类ID
- `icon` (String, 可选): 图标，最大长度100字符
- `reason` (String, 可选): 操作原因，最大长度500字符

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

**前端调用**:
```typescript
// API 调用
professionApi.updateProfession(id, updateData)

// 实际使用 (ProfessionManagement.vue:331)
const { execute: executeUpdate } = useMutation(
  (data: { id: number; updateData: Partial<Profession> }) =>
    professionApi.updateProfession(data.id, data.updateData),
  {
    successMessage: '操作成功',
    onSuccess: () => {
      // 刷新列表
    }
  }
)
```

**使用场景**:
- `ProfessionManagement.vue` - 管理后台职业管理
- 管理员更新职业信息

---

## 8. 审核/管理职业（管理接口）

**接口路径**: `POST /api/v1/admin/contents/profession/{id}/operate`

**是否需要登录**: 是（需要管理员权限）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 职业ID，必须大于0 |

**请求体** (`OperateRequest`):
```json
{
  "action": "APPROVE",
  "reason": "符合要求"
}
```

**请求参数说明**:
- `action` (String, 必填): 操作类型
  - `APPROVE`: 审核通过
  - `REJECT`: 审核拒绝
  - `BAN`: 封禁
  - `DELETE`: 删除（软删除）
- `reason` (String, 可选): 操作原因（拒绝、封禁、删除时建议填写）

**DELETE 操作说明（软删除）**:
- **软删除机制**: 不会真正删除数据库记录，而是设置 `deleted_at` 字段为当前时间
- 已删除的职业不会在任何查询接口中返回
- 删除的数据会保留在数据库中，用于审计和可能的数据恢复
- SQL实现: `UPDATE profession SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL`
- 所有查询接口自动过滤已删除记录（WHERE deleted_at IS NULL）
- 重复删除同一条记录不会报错，但不会有实际影响

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

**前端调用**:
```typescript
// API 调用
professionApi.approveProfession(id, action, reason)

// 实际使用 (ProfessionManagement.vue)
// 方式1: 通过审核
const { execute: executeApprove } = useMutation(
  (professionId: number) => professionApi.approveProfession(professionId, 'APPROVE'),
  {
    successMessage: '操作成功',
    onSuccess: () => {
      // 刷新列表
    }
  }
)

// 方式2: 拒绝/封禁/删除
const { execute: executeOperation } = useMutation(
  (data: { professionId: number; action: string; reason: string }) =>
    professionApi.approveProfession(data.professionId, data.action, data.reason),
  {
    successMessage: '操作成功'
  }
)

// 删除示例
await executeOperation({
  professionId: 123,
  action: 'DELETE',
  reason: '违规内容'
})
```

**使用场景**:
- `ProfessionManagement.vue` - 管理后台职业管理
- 管理员审核、拒绝、封禁、删除职业

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数验证失败 |
| 401 | 未登录（需要登录的接口） |
| 403 | 无权限（如：删除非自己创建的职业） |
| 404 | 资源不存在 |
| 429 | 请求频率超限 |
| 500 | 服务器内部错误 |

---

## 测试用例建议

### 1. 获取职业详情
- ✅ 正常获取已发布职业
- ✅ 职业ID不存在返回404
- ✅ 职业ID为0或负数返回400

### 2. 获取职业列表
- ✅ 分页查询（page参数）
- ✅ 按主分类筛选
- ✅ 按主分类+子分类筛选
- ✅ 游标分页功能（使用lastId）
- ✅ 无参数抛出异常
- ✅ 参数冲突（page与lastId同时存在）

### 3. 获取已批准职业
- ✅ 不传lastId获取所有已发布职业
- ✅ 传lastId进行分页
- ✅ lastId为负数返回400

### 4. 热门职业
- ✅ 默认获取10条
- ✅ 自定义limit参数
- ✅ 按热度排序（learnerCount）

### 6. 搜索职业
- ✅ 正常搜索
- ✅ 关键词为空返回400
- ✅ 搜索不存在的职业返回空列表

### 7. 删除职业
- ✅ 未登录返回401
- ✅ 删除自己创建的职业成功
- ✅ 删除他人职业返回403（非管理员）
- ✅ 职业不存在返回404
- ✅ 管理员可以删除任何职业

### 8. 更新职业（管理）
- ✅ 未登录返回401
- ✅ 非管理员返回403
- ✅ 管理员成功更新
- ✅ 字段验证失败返回400

### 9. 审核职业（管理）
- ✅ 未登录返回401
- ✅ 非管理员返回403
- ✅ 审核通过成功
- ✅ 审核拒绝成功（需要reason）
- ✅ 封禁成功（需要reason）
- ✅ 删除成功
- ✅ 不支持的操作返回400
