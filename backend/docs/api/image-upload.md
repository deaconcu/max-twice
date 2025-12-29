# 图片上传接口文档 (ImageUploadController)

## 基本信息

- **Controller**: `ImageUploadController.java`
- **基础路径**: `/api/v1/images`
- **Rate Limit**: 50 requests/minute (per user)
- **前端 API**: `web/src/api/modules/image.ts` (imageApi)

## 接口列表

## 1. 上传图片

**接口路径**: `POST /api/v1/images/upload`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求方式**: `multipart/form-data`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | 图片文件 |
| refType | String | 是 | 引用类型：post/comment/avatar/course/roadmap |

**支持的引用类型**:
- `post` - 帖子配图
- `comment` - 评论配图
- `avatar` - 用户头像
- `course` - 课程配图
- `roadmap` - 路线图配图

**返回类型**: `ImageUploadResponse`

**ImageUploadResponse 字段说明**:
- `fileUrl` (String): 图片URL

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "fileUrl": "https://cdn.example.com/images/2025/01/15/abc123.jpg"
  }
}
```

**业务逻辑**:
1. 验证文件类型（支持jpg、png、gif、webp等）
2. 验证文件大小（默认最大10MB）
3. 检查用户上传配额（分钟/小时/天级别限制）
4. 图片压缩和优化
5. 上传到文件存储服务（阿里云OSS等）
6. 记录上传历史，状态为"未使用"
7. 返回图片URL

**前端调用**:
```typescript
// API 调用
imageApi.upload(file, refType)

// 实际使用
const file = document.querySelector('input[type="file"]').files[0]
const { data } = await imageApi.upload(file, 'post')
console.log('图片URL:', data.fileUrl)
```

**使用场景**:
- 用户发布帖子时上传图片
- 用户评论时上传图片
- 用户修改头像
- 课程编辑时上传封面图

**错误情况**:
- 文件类型不支持: 返回 2404 (FILE_TYPE_NOT_ALLOWED)
- 文件过大: 返回 2403 (FILE_TOO_LARGE)
- 配额超限: 返回 2408 (UPLOAD_QUOTA_EXCEEDED)
- 上传频率过高: 返回 2407 (UPLOAD_TOO_FREQUENT)
- 未登录: 返回 1101 (USER_NOT_LOGIN)

---

## 2. 标记图片为使用中

**接口路径**: `POST /api/v1/images/mark-used`

**是否需要登录**: 是 (`@SaCheckLogin`)

**请求体**:
```json
{
  "fileUrls": [
    "https://cdn.example.com/images/2025/01/15/abc123.jpg",
    "https://cdn.example.com/images/2025/01/15/def456.jpg"
  ],
  "refId": 12345
}
```

**请求参数说明**:
- `fileUrls` (List<String>, 必填): 图片URL列表
- `refId` (Long, 必填): 引用的资源ID（文章ID、评论ID等）

**返回类型**: `void`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功"
}
```

**业务逻辑**:
1. 验证图片URL列表不为空
2. 验证refId有效
3. 查找图片记录（根据fileUrl和当前用户）
4. 更新图片状态为"使用中"
5. 关联refId（记录被哪个资源引用）
6. 记录usedAt时间

**使用场景**:
- 用户发布帖子后，标记帖子中使用的图片
- 用户提交评论后，标记评论中使用的图片
- 防止未使用的图片占用存储空间

**工作流程**:
```
1. 用户上传图片 → 状态：未使用
2. 用户编辑内容（可能插入或删除图片）
3. 用户提交内容 → 调用markAsUsed → 状态：使用中
4. 定时任务清理长期未使用的图片
```

**前端调用**:
```typescript
// API 调用
imageApi.markAsUsed(fileUrls, refId)

// 实际使用
// 用户提交帖子后
const imageUrls = ['https://cdn.example.com/images/abc.jpg']
const postId = 12345
await imageApi.markAsUsed(imageUrls, postId)
```

**错误情况**:
- 图片URL列表为空: 返回 1002 (INVALID_PARAMETER)
- refId为空: 返回 1002 (INVALID_PARAMETER)
- 未登录: 返回 1101 (USER_NOT_LOGIN)

---

## 3. 删除图片

**接口路径**: `DELETE /api/v1/images`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fileUrl | String | 是 | 图片URL |

**返回类型**: `void`

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功"
}
```

**业务逻辑**:
1. 验证fileUrl不为空
2. 查找图片记录（根据fileUrl和当前用户）
3. 验证权限（只能删除自己上传的图片）
4. 从文件存储服务删除文件
5. 删除数据库记录
6. 如果图片已被引用，解除引用关系

**前端调用**:
```typescript
// API 调用
imageApi.delete(fileUrl)

// 实际使用
const fileUrl = 'https://cdn.example.com/images/abc.jpg'
await imageApi.delete(fileUrl)
```

**使用场景**:
- 用户在编辑器中删除图片
- 用户取消发布内容，清理上传的图片
- 用户管理上传历史，删除不需要的图片

**权限验证**:
- 只能删除自己上传的图片
- 管理员可以删除任何图片（需要额外权限）

**错误情况**:
- fileUrl为空: 返回 1002 (INVALID_PARAMETER)
- 图片不存在: 返回 2401 (FILE_UPLOAD_FAILED)
- 权限不足: 返回 1103 (PERMISSION_DENIED)
- 删除失败: 返回 2402 (FILE_DELETE_FAILED)
- 未登录: 返回 1101 (USER_NOT_LOGIN)

---

## 4. 获取配额使用情况

**接口路径**: `GET /api/v1/images/quota`

**是否需要登录**: 是 (`@SaCheckLogin`)

**返回类型**: `QuotaUsageDTO`

**QuotaUsageDTO 字段说明**:
- `minuteUsed` (int): 每分钟已使用次数
- `minuteLimit` (int): 每分钟上传限制
- `hourUsed` (int): 每小时已使用次数
- `hourLimit` (int): 每小时上传限制
- `dailyUsed` (int): 每天已使用次数
- `dailyLimit` (int): 每天上传限制

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "minuteUsed": 2,
    "minuteLimit": 5,
    "hourUsed": 15,
    "hourLimit": 50,
    "dailyUsed": 120,
    "dailyLimit": 500
  }
}
```

**业务逻辑**:
1. 从Redis查询用户在不同时间窗口的上传次数
2. 获取系统配置的上传限制
3. 计算剩余配额
4. 返回详细的配额使用情况

**前端调用**:
```typescript
// API 调用
imageApi.getQuota()

// 实际使用
const { data: quota } = await imageApi.getQuota()
console.log(`今日已上传 ${quota.dailyUsed}/${quota.dailyLimit} 张`)
```

**使用场景**:
- 上传页面显示剩余配额
- 上传前检查是否超限
- 用户个人中心显示上传统计

**配额说明**:
- **普通用户**: 5张/分钟, 50张/小时, 500张/天
- **VIP用户**: 10张/分钟, 100张/小时, 1000张/天
- **限制目的**: 防止滥用、节省存储成本、确保服务质量

---

## 5. 获取上传历史

**接口路径**: `GET /api/v1/images/history`

**是否需要登录**: 是 (`@SaCheckLogin`)

**查询参数**:
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| limit | int | 否 | 20 | 返回数量 |

**返回类型**: `List<ImageUploadHistoryDTO>`

**ImageUploadHistoryDTO 字段说明**:
- `id` (Long): 图片ID
- `fileUrl` (String): 文件URL
- `fileName` (String): 文件名
- `fileSize` (Long): 文件大小（字节）
- `refType` (String): 引用类型（post/comment/avatar等）
- `refId` (Long): 引用ID（可能为null）
- `status` (Integer): 状态（0-未使用，1-使用中）
- `createdAt` (LocalDateTime): 创建时间
- `usedAt` (LocalDateTime): 首次被引用时间（可能为null）

**返回示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 12345,
      "fileUrl": "https://cdn.example.com/images/2025/01/15/abc123.jpg",
      "fileName": "screenshot.png",
      "fileSize": 524288,
      "refType": "post",
      "refId": 6789,
      "status": 1,
      "createdAt": "2025-01-15T10:30:00",
      "usedAt": "2025-01-15T10:35:00"
    },
    {
      "id": 12344,
      "fileUrl": "https://cdn.example.com/images/2025/01/15/def456.jpg",
      "fileName": "photo.jpg",
      "fileSize": 1048576,
      "refType": "comment",
      "refId": null,
      "status": 0,
      "createdAt": "2025-01-15T09:20:00",
      "usedAt": null
    }
  ]
}
```

**业务逻辑**:
1. 查询当前用户的上传历史
2. 按上传时间降序排序
3. 限制返回数量
4. 包含图片状态和引用信息

**前端调用**:
```typescript
// API 调用
imageApi.getHistory(limit)

// 实际使用
const { data: history } = await imageApi.getHistory(20)
history.forEach(item => {
  console.log(`${item.fileName} - ${item.status === 1 ? '使用中' : '未使用'}`)
})
```

**使用场景**:
- 用户个人中心查看上传历史
- 管理未使用的图片
- 查看图片引用情况
- 清理临时上传的图片

**状态说明**:
- **0 (未使用)**: 图片已上传但未被任何内容引用，可能被定时任务清理
- **1 (使用中)**: 图片已被文章、评论等内容引用，不会被自动清理

---

## 数据存储说明

### image_upload 表字段

**主要字段**:
- `id` (BIGINT): 主键
- `user_id` (BIGINT): 上传用户ID
- `file_url` (VARCHAR): 图片URL
- `file_name` (VARCHAR): 原始文件名
- `file_size` (BIGINT): 文件大小（字节）
- `file_type` (VARCHAR): 文件类型（image/jpeg等）
- `ref_type` (VARCHAR): 引用类型（post/comment/avatar等）
- `ref_id` (BIGINT): 引用的资源ID，NULL表示未使用
- `status` (TINYINT): 状态（0-未使用，1-使用中）
- `created_at` (TIMESTAMP): 上传时间
- `used_at` (TIMESTAMP): 首次被引用时间
- `deleted_at` (TIMESTAMP): 删除时间（软删除）

**索引**:
- PRIMARY KEY: `id`
- INDEX: `idx_user_id` - 用户历史查询
- INDEX: `idx_file_url` - URL查找
- INDEX: `idx_status_created_at` - 清理未使用图片

### Redis 配额管理

**Key格式**:
- `upload:quota:minute:{userId}` - 分钟级配额，TTL 60秒
- `upload:quota:hour:{userId}` - 小时级配额，TTL 3600秒
- `upload:quota:day:{userId}` - 天级配额，TTL 86400秒

**Value**: 上传次数（整数）

---

## 图片处理流程

### 上传流程

```
1. 前端选择图片
   ↓
2. 调用 /images/upload
   ↓
3. 后端验证：
   - 文件类型
   - 文件大小
   - 用户配额
   ↓
4. 图片处理：
   - 压缩优化
   - 生成缩略图（可选）
   - 添加水印（可选）
   ↓
5. 上传到OSS/CDN
   ↓
6. 保存记录到数据库（状态：未使用）
   ↓
7. 返回图片URL
   ↓
8. 前端插入到编辑器
```

### 引用流程

```
1. 用户编辑内容，插入图片URL
   ↓
2. 用户提交内容（发布帖子/评论）
   ↓
3. 后端保存内容，获取content中的图片URL
   ↓
4. 调用 /images/mark-used
   ↓
5. 更新图片状态为"使用中"
   ↓
6. 关联 refId（帖子ID/评论ID）
   ↓
7. 记录 usedAt 时间
```

### 清理流程

```
定时任务（每天凌晨执行）：
1. 查询 status=0 且 created_at < 24小时前 的图片
   ↓
2. 从OSS删除文件
   ↓
3. 软删除数据库记录（设置 deleted_at）
   ↓
4. 记录清理日志
```

---

## 安全限制

### 1. 文件类型限制

**允许的MIME类型**:
- `image/jpeg` - JPEG图片
- `image/png` - PNG图片
- `image/gif` - GIF动图
- `image/webp` - WebP图片
- `image/svg+xml` - SVG矢量图（可能受限）

**不允许的类型**:
- 可执行文件（.exe, .sh等）
- 脚本文件（.js, .php等）
- 其他非图片格式

### 2. 文件大小限制

**普通用户**:
- 单文件最大：10MB
- 图片尺寸最大：8000x8000像素

**VIP用户**:
- 单文件最大：20MB
- 图片尺寸最大：10000x10000像素

### 3. 上传频率限制

**Rate Limit**:
- 50 requests/minute (Controller级别)
- 5张图片/分钟（普通用户）
- 50张图片/小时（普通用户）
- 500张图片/天（普通用户）

### 4. 存储空间限制

**用户配额**:
- 普通用户：1GB
- VIP用户：10GB
- 超出配额后无法上传

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 1002 | 参数验证失败 |
| 1101 | 未登录 |
| 1103 | 权限不足 |
| 2401 | 文件上传失败 |
| 2402 | 文件删除失败 |
| 2403 | 文件大小超出限制 |
| 2404 | 不支持的文件类型 |
| 2405 | 无效的图片文件 |
| 2406 | 图片压缩失败 |
| 2407 | 上传过于频繁 |
| 2408 | 上传配额已用尽 |
| 2409 | 图片尺寸超出限制 |

---

## 注意事项

1. **上传前检查配额**:
   - 建议前端在上传前调用 `/images/quota` 检查剩余配额
   - 避免用户上传后被拒绝的糟糕体验

2. **图片压缩**:
   - 后端会自动压缩图片，优化存储和加载速度
   - 前端也可以预先压缩，提升上传速度

3. **标记为使用中**:
   - 必须在内容提交成功后调用 `markAsUsed`
   - 否则图片会在24小时后被清理

4. **未使用图片清理**:
   - 24小时后自动清理未使用的图片
   - 用户取消发布时应主动调用 `delete` 接口

5. **CDN缓存**:
   - 图片URL返回后立即可用
   - CDN可能有缓存延迟（通常<1分钟）
   - 删除图片后CDN缓存可能仍存在（需要时间刷新）

6. **并发上传**:
   - 前端可以并发上传多张图片
   - 但要注意配额限制（分钟级别）
   - 建议使用队列依次上传

7. **图片URL格式**:
   - 返回的URL是完整的HTTPS地址
   - 直接可用于 `<img>` 标签或CSS
   - 不要手动拼接或修改URL

8. **权限控制**:
   - 用户只能删除自己上传的图片
   - 图片URL公开可访问（CDN）
   - 敏感图片需要额外的访问控制

---

*此文档用于指导前后端开发和接口对接，确保图片上传功能的正确使用*
