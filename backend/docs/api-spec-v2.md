# 新 API 接口规范 v1.0

> 本文档定义 v2 API 的设计规范。后端按此规范实现 `/v2/` 下所有接口；前端 apiClient 按此规范适配。迁移完成后 v1 下线，v2 去掉前缀回归根路径。
>
> 注：API 部署在独立域名 `api.twicemax.com`，路径上不再重复 `/api/` 前缀。
>
> **状态**：规范定稿，未开始实施
> **最近更新**：2026-04-24

---

## 1. URL 策略

### 1.1 过渡期
- 新接口全部挂在 `/v2/` 前缀下
- 老接口 `/v1/` 保留不动，直到全部迁移完成

### 1.2 完成期
- v1 接口全部下线后，将 v2 接口**移动到**根路径（去掉 v2 前缀）
- 最终 URL 示例：`/v2/roles` → `/roles`

### 1.3 前端 apiClient 策略
- 通过 URL 前缀自动识别新旧格式
- URL 以 `/v2/` 开头走新格式处理
- URL 以 `/v1/` 开头走旧格式处理（兼容期）
- 迁移完成后，只保留新格式处理逻辑

---

## 2. 成功响应格式

| 操作 | HTTP 状态 | 响应体 | 额外 header |
|------|----------|--------|------------|
| GET 单个资源 | 200 | 资源对象本身 | - |
| GET 列表（分页）| 200 | `{ items, hasMore, nextCursor }` | - |
| GET 列表（不分页，固定数量）| 200 | 数组 `[...]` | - |
| POST 创建（立即生效）| 201 | 新资源完整对象 | `Location: /v2/roles/123` |
| POST 创建（待审核/异步）| 202 | 资源对象（中间状态，如 pending）| - |
| PUT/PATCH 更新 | 200 | 更新后的完整资源 | - |
| POST 动作（点赞/打卡/订阅）| 200 | 相关状态对象 | - |
| DELETE 删除 | 204 | 空 | - |

### 2.1 GET 单个资源

```http
GET /v2/roles/123

HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": 123,
  "name": "前端工程师",
  "description": "...",
  "createdAt": "2026-04-24T10:00:00Z"
}
```

**body 就是资源本身，无 wrapper**。

### 2.2 GET 列表（分页）

```http
GET /v2/roles?cursor=100&limit=20

HTTP/1.1 200 OK

{
  "items": [
    { "id": 101, "name": "..." },
    { "id": 102, "name": "..." }
  ],
  "hasMore": true,
  "nextCursor": 120
}
```

**字段约定**：
- `items`：数据数组（固定用 items，不用 list / data）
- `hasMore`：布尔，是否有下一页
- `nextCursor`：下一页的 cursor 值，用于下次请求的 `?cursor=` 参数。如果 `hasMore = false`，此字段可省略或为 null

### 2.3 GET 列表（不分页）

用于返回固定数量且数据不多的场景（如热门前 15、Top 10 等）：

```http
GET /v2/roles/hot?limit=15

HTTP/1.1 200 OK

[
  { "id": 1, "name": "..." },
  { "id": 2, "name": "..." }
]
```

**直接返回数组，无外壳**。

### 2.4 POST 创建

```http
POST /v2/roles
Content-Type: application/json

{ "name": "前端工程师", "description": "..." }

HTTP/1.1 201 Created
Location: /v2/roles/123
Content-Type: application/json

{
  "id": 123,
  "name": "前端工程师",
  "description": "...",
  "createdAt": "2026-04-24T10:00:00Z"
}
```

**必须**：
- 状态码 201（不是 200）
- `Location` header 指向新资源
- body 是完整新资源（便于前端 `setQueryData` 缓存）

### 2.5 POST 创建（待审核 / 异步）

当创建动作并不立即产生"可用资源"，而是进入待审核、待处理、异步任务流程时，使用 **202 Accepted**。

典型场景：
- 用户申请专业 / 角色（需要管理员审核才上线）
- 用户提交课程（需要审核通过后发布）
- 发起异步任务（视频转码、批量导入等）

```http
POST /v2/roles
Content-Type: application/json

{ "name": "前端工程师", "description": "..." }

HTTP/1.1 202 Accepted
Content-Type: application/json

{
  "id": 123,
  "name": "前端工程师（申请中）",
  "status": "pending",
  "submittedAt": "2026-04-24T10:00:00Z"
}
```

**要点**：
- 状态码 202（不是 201，因为资源还不完全可用）
- 不需要 `Location` header（资源还不在"最终位置"）
- body 返回中间态的资源对象（前端可缓存、可跳转查看审核进度）
- 区分原则：**资源是否已经对用户/系统完全可用**
  - 立即可用 → 201
  - 需要后续处理（审核、异步任务）→ 202

### 2.6 PUT/PATCH 更新

```http
PUT /v2/users/me/profile
Content-Type: application/json

{ "name": "新名字" }

HTTP/1.1 200 OK

{
  "id": 1,
  "name": "新名字",
  "email": "alice@example.com",
  "avatar": "...",
  "updatedAt": "2026-04-24T10:00:00Z",
  "locale": "zh"
}
```

**返回完整资源**（非 204）。让前端能拿到 `updatedAt` 等服务端生成字段，并更新缓存。

### 2.7 POST 动作（点赞 / 打卡 / 订阅 / 关注 等）

这些"POST 但不创建资源"的动作，返回动作后的相关状态：

```http
POST /v2/posts/42/upvote

HTTP/1.1 200 OK

{
  "upvoted": true,
  "upvoteCount": 43
}
```

**用途**：前端可做乐观更新，收到响应后用真实数据覆盖。

如果该动作前端完全不需要返回数据（罕见），也可用 204。

### 2.8 DELETE 删除

```http
DELETE /v2/roles/123

HTTP/1.1 204 No Content
```

**body 必须为空**（不是 `{}`，不是 `null`，是完全没有 body）。

---

## 3. 错误响应格式

### 3.1 统一结构

```http
HTTP/1.1 4xx/5xx

{
  "error": {
    "code": "INVITE_ONLY",
    "message": "TwiceMax 正在内测中...",
    "details": { ... }
  }
}
```

**字段**：
- `code`：字符串，业务错误码（取自后端 `StatusCode` 枚举的 `name()`）
- `message`：人类可读的消息（已根据 `Accept-Language` i18n）
- `details`：可选对象，放字段级错误、traceId、retry_after 等附加信息

### 3.2 字符串错误码

后端 `StatusCode` 枚举已有 name，直接使用：

```java
// 旧：返回 1127
// 新：返回 "INVITE_ONLY"（StatusCode.INVITE_ONLY.name()）
```

**所有错误码字符串白名单**见第 5 节"前端关心的错误码"。其他错误码后端照常返回（用于日志），前端不做 if-else 分支处理。

### 3.3 HTTP 状态码映射规则

| HTTP | 含义 | 示例业务场景 | 对应后端 StatusCode |
|------|------|------------|-------------------|
| 400 | 请求参数/格式错误 | 字段缺失、类型错、格式不合法 | `INVALID_PARAMETER`、`USER_INVALID_EMAIL_FORMAT`、`USER_INVALID_USERNAME_LENGTH`、`ROLE_NAME_REQUIRED`、所有 `*_INVALID_*` |
| 401 | 未认证 | 未登录、token 过期、密码错误 | `USER_NOT_LOGIN`、`USER_PASSWORD_WRONG`、`USER_LOGIN_FAILED` |
| 403 | 已认证但禁止 | 权限不足、邮箱未验证、内测限制、资源不可见、账号被封 | `PERMISSION_DENIED`、`USER_EMAIL_NOT_VALIDATED`、`USER_BANNED`、`INVITE_ONLY`、`CONTENT_NOT_VISIBLE`、`COURSE_IS_NOT_PUBLISHED`、`NODE_STATE_INVALID`、`ROLE_BLOCKED`、`MEMORY_CARD_NOT_AVAILABLE`、`INTERACTION_CANNOT_UPVOTE_OWN_CONTENT` |
| 404 | 资源不存在 | 所有 `*_NOT_FOUND` | `ROLE_NOT_FOUND`、`USER_NOT_FOUND`、`POST_NOT_FOUND`、`COMMENT_NOT_FOUND` 等 23 个 |
| 409 | 资源状态冲突 | 已存在、已批准、状态已变 | `USER_ALREADY_EXISTS`、`USER_ALREADY_FOLLOWED`、`COURSE_ALREADY_APPROVED`、`COURSE_STATE_CONFLICT`、`NODE_ALREADY_COMPLETED` 等 19 个 |
| 410 | 会话/验证码过期 | 重置 session 失效、验证码过期 | `PENDING_SESSION_INVALID`、`PASSWORD_RESET_SESSION_INVALID`、`USER_VERIFICATION_CODE_EXPIRED`、`CAPTCHA_EXPIRED` |
| 412 | 前置条件未满足 | 需要先完成某步骤 | `PASSWORD_RESET_NOT_VERIFIED` |
| 413 | 请求体过大 | 文件过大 | `FILE_TOO_LARGE`、`IMAGE_DIMENSION_TOO_LARGE` |
| 415 | 媒体类型不支持 | 文件类型不允许 | `FILE_TYPE_NOT_ALLOWED` |
| 422 | 语义错误（格式对但业务校验失败）| 验证码无效、密码太弱、图片无效 | `USER_VERIFICATION_CODE_INVALID`、`USER_VERIFICATION_CODE_NOT_FOUND`、`USER_PASSWORD_TOO_WEAK`、`CAPTCHA_INVALID`、`INVALID_IMAGE`、`ROADMAP_CONTENT_INVALID` |
| 428 | 要求前置条件 | 需要验证码 | `CAPTCHA_REQUIRED` |
| 429 | 限流/超限 | 过于频繁、配额超限 | `RATE_LIMIT_EXCEEDED`、`USER_VERIFICATION_CODE_SEND_TOO_FREQUENT`、`USER_VERIFICATION_CODE_ATTEMPTS_EXCEEDED`、`UPLOAD_TOO_FREQUENT`、`UPLOAD_QUOTA_EXCEEDED`、`USER_SUBSCRIPTION_LIMIT_EXCEEDED`、`LEARNING_ROADMAP_LIMIT_EXCEEDED`、`NODE_CARD_LIMIT_EXCEEDED`、`USER_CARD_LIMIT_EXCEEDED` |
| 500 | 服务端内部错 | 所有内部异常、数据库错、解析错 | `SYSTEM_ERROR`、`UNKNOWN_EXCEPTION`、`DATABASE_ERROR`、`JSON_PROCESSING_ERROR`、`LEARNING_PROGRESS_SYNC_FAILED`、`LEARNING_PROGRESS_REDIS_FAILED`、所有 `*_FAILED` 等 28 个 |
| 502 | 外部服务错 | AI 服务、外部 API | `EXTERNAL_SERVICE_ERROR`、`AI_SERVICE_REQUEST_FAILED` |
| 503 | 服务不可用 | 维护中 | `SYSTEM_READONLY_MODE` |

### 3.4 错误响应示例

**参数错误（400）**：
```http
POST /v2/roles
body: {}

HTTP/1.1 400 Bad Request

{
  "error": {
    "code": "ROLE_NAME_REQUIRED",
    "message": "专业名称不能为空"
  }
}
```

**邮箱已注册（409）**：
```http
POST /v2/auth/register

HTTP/1.1 409 Conflict

{
  "error": {
    "code": "USER_ALREADY_EXISTS",
    "message": "邮箱已被注册"
  }
}
```

**文件过大（413）**：
```http
POST /v2/users/avatar

HTTP/1.1 413 Payload Too Large

{
  "error": {
    "code": "FILE_TOO_LARGE",
    "message": "文件大小超出 10MB 限制"
  }
}
```

**限流（429）**：
```http
HTTP/1.1 429 Too Many Requests

{
  "error": {
    "code": "USER_VERIFICATION_CODE_SEND_TOO_FREQUENT",
    "message": "验证码发送过于频繁，请稍后再试",
    "details": {
      "retryAfter": 60
    }
  }
}
```

**内测拒绝（403）**：
```http
HTTP/1.1 403 Forbidden

{
  "error": {
    "code": "INVITE_ONLY",
    "message": "TwiceMax 正在内测中，目前仅开放给受邀用户..."
  }
}
```

---

## 4. 分页约定

### 4.1 游标分页（默认，所有列表）

**请求参数**：
- `cursor`：上次响应的 `nextCursor`，首次请求不传
- `limit`：每页数量，可选，后端设默认值和上限

**响应体**：
- `items`：数据数组
- `hasMore`：是否有下一页
- `nextCursor`：下一页的 cursor 值

**示例**：
```http
GET /v2/roles?cursor=100&limit=20

{
  "items": [...],
  "hasMore": true,
  "nextCursor": 120
}
```

### 4.2 复合游标

如果一个游标不够（如需要 `lastScore + lastId`），`cursor` 和 `nextCursor` 使用**对象**：

```http
GET /v2/posts?nodeId=5&cursor=<base64编码的JSON对象>

{
  "items": [...],
  "hasMore": true,
  "nextCursor": { "lastScore": 100.5, "lastId": 1234 }
}
```

客户端不需要理解 cursor 内容，原样传回。推荐后端将对象 base64 编码为字符串，前端透明处理。

---

## 5. 前端关心的业务错误码白名单

前端代码**只对下列错误码**做 `if (err.code === X)` 分支处理，其他错误码统一显示 `err.message`。

```ts
// web/src/constants/errorCode.ts
export const BUSINESS_ERROR = {
  // 认证 / 会话
  USER_NOT_LOGIN: 'USER_NOT_LOGIN',
  USER_EMAIL_NOT_VALIDATED: 'USER_EMAIL_NOT_VALIDATED',
  USER_PASSWORD_WRONG: 'USER_PASSWORD_WRONG',
  PENDING_SESSION_INVALID: 'PENDING_SESSION_INVALID',
  PASSWORD_RESET_SESSION_INVALID: 'PASSWORD_RESET_SESSION_INVALID',
  PASSWORD_RESET_NOT_VERIFIED: 'PASSWORD_RESET_NOT_VERIFIED',
  INVITE_ONLY: 'INVITE_ONLY',

  // 验证码
  USER_VERIFICATION_CODE_INVALID: 'USER_VERIFICATION_CODE_INVALID',
  USER_VERIFICATION_CODE_EXPIRED: 'USER_VERIFICATION_CODE_EXPIRED',
  USER_VERIFICATION_CODE_ATTEMPTS_EXCEEDED: 'USER_VERIFICATION_CODE_ATTEMPTS_EXCEEDED',
  USER_VERIFICATION_CODE_SEND_TOO_FREQUENT: 'USER_VERIFICATION_CODE_SEND_TOO_FREQUENT',
  CAPTCHA_REQUIRED: 'CAPTCHA_REQUIRED',

  // 限流 / 配额
  RATE_LIMIT_EXCEEDED: 'RATE_LIMIT_EXCEEDED',
  UPLOAD_QUOTA_EXCEEDED: 'UPLOAD_QUOTA_EXCEEDED',

  // 上传
  FILE_TOO_LARGE: 'FILE_TOO_LARGE',
  FILE_TYPE_NOT_ALLOWED: 'FILE_TYPE_NOT_ALLOWED',

  // 状态 / 访问
  CONTENT_NOT_VISIBLE: 'CONTENT_NOT_VISIBLE',
  NODE_STATE_INVALID: 'NODE_STATE_INVALID',
  USER_ALREADY_FOLLOWED: 'USER_ALREADY_FOLLOWED',
  USER_COURSE_ALREADY_SUBSCRIBED: 'USER_COURSE_ALREADY_SUBSCRIBED',
} as const
```

其他 110+ 个业务错误码后端照常返回在 `error.code`（用于排障、日志），前端不处理分支，只展示 `message`。

---

## 6. 字段命名约定

### 6.1 camelCase
所有 JSON 字段使用 camelCase：
- ✅ `createdAt`、`hasMore`、`nextCursor`、`userId`、`likeCount`
- ❌ `created_at`、`has_more`、`user_id`

### 6.2 布尔字段前缀
以 `is` / `has` / `can` / `should` 开头：
- ✅ `isActive`、`hasChildren`、`canEdit`、`shouldNotify`
- ❌ `active`、`children`（歧义）

### 6.3 时间字段
- 统一 **ISO 8601 UTC 字符串**：`"createdAt": "2026-04-24T10:00:00Z"`
- **不使用** Unix 时间戳（`1698134400`）
- **不使用** 无时区字符串（`"2026-04-24 10:00:00"`）

### 6.4 空值
- 明确返回 `null`，不省略字段
- ✅ `{ "description": null }`
- ❌ `{}`（缺字段，客户端无法区分"没有"和"未返回"）

---

## 7. 请求头

### 7.1 认证
```
Authorization: Bearer <token>
```

### 7.2 国际化
```
Accept-Language: zh | en
X-Site-Lang: zh | en
```
- `Accept-Language`：标准 header，影响后端 i18n 的 locale 解析
- `X-Site-Lang`：自定义，用于业务路由选择（与当前 locale 机制对齐）

### 7.3 缓存
```
If-None-Match: "abc123"
```
启用 ETag 的接口使用。

---

## 8. 响应头

### 8.1 必须
```
X-Trace-Id: <uuid>
```
- 所有响应（含错误响应）都必须带
- 后端生成 UUID，存 MDC，所有日志自动带上
- 前端可在报错提示中显示，便于排障

### 8.2 按场景
```
Location: /v2/roles/123          # POST 创建时必须
ETag: "abc123"                       # 启用 ETag 的 GET
Retry-After: 60                      # 429/503 时可选
```

---

## 9. 全局约定

### 9.1 批量操作
**不鼓励**。每个操作尽量单一资源，简化幂等性和错误处理。

确实需要批量时：
- 使用 `POST /v2/{resource}/batch`
- 响应为 `{ "succeeded": [...], "failed": [{ id, error: { code, message } }] }`

### 9.2 软删除 vs 物理删除
对前端透明。DELETE 接口不暴露实现细节。

### 9.3 部分更新
推荐使用 `PATCH`，body 只包含要改的字段。完整替换用 `PUT`。

### 9.4 幂等性
- GET、PUT、DELETE 必须幂等
- POST 非幂等（除非通过 `Idempotency-Key` header 支持）

---

## 10. 前端 apiClient 适配层

### 10.1 职责
将后端的 v2 响应转换成前端业务代码期望的形式：
- 成功：剥掉 HTTP 层，返回业务数据
- 失败：转成 JS Error 对象，`err.code` 为字符串错误码，`err.message` 为人类可读消息

### 10.2 伪代码
```ts
async function apiClient<T>(config): Promise<T> {
  const res = await axios(config)

  // 2xx 成功路径
  if (res.status === 204) return undefined as T
  return res.data  // body 就是业务数据
}

axios.interceptors.response.use(undefined, (err) => {
  if (err.response?.data?.error) {
    const { code, message, details } = err.response.data.error
    throw Object.assign(new Error(message), {
      code,
      details,
      isBusinessError: true,
      httpStatus: err.response.status,
      traceId: err.response.headers['x-trace-id'],
    })
  }
  throw err
})
```

### 10.3 业务代码使用
```ts
try {
  const role = await roleApi.createRole({ name: '...' })
  // role 是完整的 Role 对象，无 wrapper
} catch (err) {
  if (err.code === BUSINESS_ERROR.INVITE_ONLY) {
    // 特殊处理
  } else {
    toast.error(err.message)
  }
}
```

---

## 11. 迁移路径

1. **后端**：搭建 v2 基础设施（路由、全局异常处理器、响应工具类）
2. **前端**：apiClient 适配层增加 v2 分支（URL 前缀识别）
3. **逐接口迁移**：
   - 后端新建 v2 Controller + DTO
   - 前端 `api/modules/*.ts` 切换到新 URL
   - 联调测试
4. **前端 TanStack 迁移**：基于已改造的 v2 接口
5. **清理**：删除 v1 后端代码；v2 接口移到 `/api/` 根路径；apiClient 移除 v1 兼容逻辑

---

## 12. 变更历史

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-04-24 | v1.0 | 规范定稿 |
