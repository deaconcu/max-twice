# 图片上传接口测试用例

> 参考测试编写规范：`docs/test/TEST_GUIDE.md`

---

## 测试准备

### 测试类基础配置

**测试类名**: `ImageUploadControllerTest`

**继承**: `extends BaseControllerTest`

**注解**:
- `@Transactional` - 自动回滚，测试之间互不影响

**必需依赖**:
- MockMvc - HTTP 请求测试
- ObjectMapper - JSON 解析
- UserDomainService - 创建测试用户
- ImageUploadService - 图片上传业务逻辑
- PostService - 创建帖子（测试自动标记）

### 测试辅助方法

需要创建以下辅助方法：

1. **createUser(String email)** - 创建测试用户
2. **createAdminUser(String email)** - 创建管理员用户
3. **createMockImageFile(String filename, String contentType, long size)** - 创建模拟图片文件
4. **uploadImage(Long userId, String refType)** - 上传图片并返回URL
5. **createPostWithImages(Long userId, String content)** - 创建包含图片的帖子

### 测试数据说明

1. **@Transactional - 自动清理数据**:
   - 每个 `@Test` 方法执行完后，所有数据库操作都会自动回滚
   - 包括用户、图片记录、帖子等所有操作
   - 无需手动清理数据，测试之间互不影响

2. **动态ID**:
   - 所有测试数据使用数据库自动生成的ID
   - 避免测试之间的ID冲突

3. **Sa-Token 登录**:
   - 需要登录的接口使用 `StpUtil.login(userId)` 模拟登录
   - 请求时添加 header: `"token", StpUtil.getTokenValue()`
   - 测试结束后使用 `StpUtil.logout()` 清理登录状态

4. **MockMultipartFile**:
   - 使用 Spring 提供的 MockMultipartFile 模拟文件上传
   - 不需要真实的文件，使用字节数组即可
   - 可以模拟不同的文件类型、大小、内容

5. **图片状态说明**:
   - status = 0: 未使用（上传后默认状态）
   - status = 1: 使用中（被帖子/评论引用后）

---

## 接口1: 上传图片 (POST /api/v1/images/upload)

### 测试场景

#### 1.1 成功上传图片 - JPEG格式
- **准备**：创建用户A
- **登录**：用户A登录
- **请求**：POST /images/upload
  - 参数：file (MockMultipartFile, image/jpeg, 1MB)
  - 参数：refType = "post"
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data.fileUrl` 不为空，是有效的URL格式
  - 数据库中存在该图片记录，status = 0

#### 1.2 成功上传图片 - PNG格式
- **准备**：创建用户A
- **登录**：用户A登录
- **请求**：POST /images/upload
  - 参数：file (MockMultipartFile, image/png, 2MB)
  - 参数：refType = "comment"
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data.fileUrl` 不为空

#### 1.3 成功上传图片 - WebP格式
- **准备**：创建用户A
- **登录**：用户A登录
- **请求**：POST /images/upload
  - 参数：file (MockMultipartFile, image/webp, 500KB)
  - 参数：refType = "avatar"
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data.fileUrl` 不为空

#### 1.4 字段验证 - refType为空
- **准备**：创建用户A
- **登录**：用户A登录
- **请求**：POST /images/upload
  - 参数：file (MockMultipartFile)
  - 参数：refType = "" (空字符串)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 1.5 字段验证 - 文件为空
- **准备**：创建用户A
- **登录**：用户A登录
- **请求**：POST /images/upload
  - 参数：file = null
  - 参数：refType = "post"
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 1.6 文件类型验证 - 不支持的格式
- **准备**：创建用户A
- **登录**：用户A登录
- **请求**：POST /images/upload
  - 参数：file (MockMultipartFile, application/pdf)
  - 参数：refType = "post"
- **验证**：
  - 返回 200 状态码
  - `$.code` = 2404 (StatusCode.FILE_TYPE_NOT_ALLOWED)

#### 1.7 文件大小验证 - 文件过大
- **准备**：创建用户A
- **登录**：用户A登录
- **请求**：POST /images/upload
  - 参数：file (MockMultipartFile, image/jpeg, 15MB)
  - 参数：refType = "post"
- **验证**：
  - 返回 200 状态码
  - `$.code` = 2403 (StatusCode.FILE_TOO_LARGE)

#### 1.8 权限验证 - 未登录
- **请求**：POST /images/upload (不传token)
  - 参数：file (MockMultipartFile)
  - 参数：refType = "post"
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

#### 1.9 配额限制 - 超出分钟级限制
- **准备**：创建用户A，用户A在1分钟内已上传5张图片
- **登录**：用户A登录
- **请求**：POST /images/upload (第6次上传)
  - 参数：file (MockMultipartFile)
  - 参数：refType = "post"
- **验证**：
  - 返回 200 状态码
  - `$.code` = 2407 (StatusCode.UPLOAD_TOO_FREQUENT)

---

## 接口2: 标记图片为使用中 (POST /api/v1/images/mark-used)

### 测试场景

#### 2.1 成功标记单张图片 - 管理员操作
- **准备**：
  - 创建管理员用户A
  - 用户A上传1张图片，获取URL
  - 创建帖子，获取帖子ID
- **登录**：管理员A登录
- **请求**：POST /images/mark-used
  - Body: {"fileUrls": ["图片URL"], "refType": "post", "refId": 帖子ID}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - 数据库中图片状态更新为 status = 1
  - refId 字段已设置
  - usedAt 字段已设置

#### 2.2 成功标记多张图片 - 管理员操作
- **准备**：
  - 创建管理员用户A
  - 用户A上传3张图片，获取URL列表
  - 创建帖子，获取帖子ID
- **登录**：管理员A登录
- **请求**：POST /images/mark-used
  - Body: {"fileUrls": [URL1, URL2, URL3], "refType": "post", "refId": 帖子ID}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - 数据库中3张图片状态都更新为 status = 1

#### 2.3 字段验证 - fileUrls为空
- **准备**：创建管理员用户A
- **登录**：管理员A登录
- **请求**：POST /images/mark-used
  - Body: {"fileUrls": [], "refType": "post", "refId": 123}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 2.4 字段验证 - refType为空
- **准备**：创建管理员用户A
- **登录**：管理员A登录
- **请求**：POST /images/mark-used
  - Body: {"fileUrls": ["url"], "refType": null, "refId": 123}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 2.5 字段验证 - refId为空
- **准备**：创建管理员用户A
- **登录**：管理员A登录
- **请求**：POST /images/mark-used
  - Body: {"fileUrls": ["url"], "refType": "post", "refId": null}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 2.6 权限验证 - 普通用户无权限
- **准备**：创建普通用户A（非管理员）
- **登录**：用户A登录
- **请求**：POST /images/mark-used
  - Body: {"fileUrls": ["url"], "refType": "post", "refId": 123}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1103 (StatusCode.PERMISSION_DENIED)

#### 2.7 权限验证 - 未登录
- **请求**：POST /images/mark-used (不传token)
  - Body: {"fileUrls": ["url"], "refType": "post", "refId": 123}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

---

## 接口3: 删除图片 (DELETE /api/v1/images)

### 测试场景

#### 3.1 成功删除图片 - 管理员操作
- **准备**：
  - 创建管理员用户A
  - 用户A上传1张图片，获取URL
- **登录**：管理员A登录
- **请求**：DELETE /images?fileUrl={图片URL}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - 数据库中图片记录已删除（软删除或物理删除）

#### 3.2 字段验证 - fileUrl为空
- **准备**：创建管理员用户A
- **登录**：管理员A登录
- **请求**：DELETE /images?fileUrl=
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1002 (StatusCode.INVALID_PARAMETER)

#### 3.3 业务验证 - 图片不存在
- **准备**：创建管理员用户A
- **登录**：管理员A登录
- **请求**：DELETE /images?fileUrl=http://example.com/not-exist.jpg
- **验证**：
  - 返回 200 状态码
  - `$.code` = 2401 (StatusCode.FILE_UPLOAD_FAILED) 或其他合适的错误码

#### 3.4 权限验证 - 普通用户无权限
- **准备**：创建普通用户A（非管理员），上传1张图片
- **登录**：用户A登录
- **请求**：DELETE /images?fileUrl={图片URL}
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1103 (StatusCode.PERMISSION_DENIED)

#### 3.5 权限验证 - 未登录
- **请求**：DELETE /images?fileUrl=http://example.com/test.jpg (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

---

## 接口4: 获取配额使用情况 (GET /api/v1/images/quota)

### 测试场景

#### 4.1 成功获取配额 - 未使用
- **准备**：创建用户A
- **登录**：用户A登录
- **请求**：GET /images/quota
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data.minuteUsed` = 0
  - `$.data.minuteLimit` > 0
  - `$.data.hourUsed` = 0
  - `$.data.hourLimit` > 0
  - `$.data.dailyUsed` = 0
  - `$.data.dailyLimit` > 0

#### 4.2 成功获取配额 - 已部分使用
- **准备**：
  - 创建用户A
  - 用户A上传2张图片
- **登录**：用户A登录
- **请求**：GET /images/quota
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data.minuteUsed` = 2
  - `$.data.hourUsed` = 2
  - `$.data.dailyUsed` = 2

#### 4.3 权限验证 - 未登录
- **请求**：GET /images/quota (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

---

## 接口5: 获取上传历史 (GET /api/v1/images/history)

### 测试场景

#### 5.1 成功获取上传历史 - 有记录
- **准备**：
  - 创建用户A
  - 用户A上传3张图片
- **登录**：用户A登录
- **请求**：GET /images/history?limit=20
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是数组，长度为 3
  - 每个记录包含字段：id, fileUrl, fileName, fileSize, refType, refId, status, createdAt, usedAt
  - 按上传时间降序排序

#### 5.2 成功获取上传历史 - 无记录
- **准备**：创建用户A（未上传任何图片）
- **登录**：用户A登录
- **请求**：GET /images/history?limit=20
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是空数组

#### 5.3 参数验证 - 限制数量
- **准备**：
  - 创建用户A
  - 用户A上传10张图片
- **登录**：用户A登录
- **请求**：GET /images/history?limit=5
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是数组，长度为 5

#### 5.4 参数验证 - 默认限制
- **准备**：
  - 创建用户A
  - 用户A上传5张图片
- **登录**：用户A登录
- **请求**：GET /images/history (不传limit参数)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是数组，长度为 5

#### 5.5 权限验证 - 只能看到自己的记录
- **准备**：
  - 创建用户A，上传2张图片
  - 创建用户B，上传3张图片
- **登录**：用户A登录
- **请求**：GET /images/history
- **验证**：
  - 返回 200 状态码
  - `$.code` = 200
  - `$.data` 是数组，长度为 2（只返回用户A的记录）

#### 5.6 权限验证 - 未登录
- **请求**：GET /images/history (不传token)
- **验证**：
  - 返回 200 状态码
  - `$.code` = 1101 (StatusCode.USER_NOT_LOGIN)

---

## 集成测试：自动标记图片

### 测试场景

#### 6.1 创建帖子 - 自动标记图片为使用中
- **准备**：
  - 创建用户A
  - 用户A上传2张图片，获取URL1和URL2
  - 创建课程和节点
- **登录**：用户A登录
- **请求**：POST /posts
  - Body: {"nodeId": 节点ID, "content": "<p>文本<img src='URL1'/><img src='URL2'/></p>", "type": 1}
- **验证**：
  - 帖子创建成功
  - 数据库中URL1对应的图片：status = 1, refId = 帖子ID, refType = "post"
  - 数据库中URL2对应的图片：status = 1, refId = 帖子ID, refType = "post"

#### 6.2 创建帖子 - 内容无图片
- **准备**：
  - 创建用户A
  - 创建课程和节点
- **登录**：用户A登录
- **请求**：POST /posts
  - Body: {"nodeId": 节点ID, "content": "<p>纯文本内容</p>", "type": 1}
- **验证**：
  - 帖子创建成功
  - 不会调用图片标记逻辑（没有异常）

#### 6.3 更新帖子 - 添加新图片
- **准备**：
  - 创建用户A
  - 创建帖子A（无图片）
  - 上传1张图片，获取URL
- **登录**：用户A登录
- **请求**：PUT /posts/{帖子ID}
  - Body: {"content": "<p>文本<img src='URL'/></p>"}
- **验证**：
  - 帖子更新成功
  - 数据库中URL对应的图片：status = 1, refId = 帖子ID, refType = "post"

#### 6.4 更新帖子 - 删除图片
- **准备**：
  - 创建用户A
  - 创建帖子A（包含1张图片）
- **登录**：用户A登录
- **请求**：PUT /posts/{帖子ID}
  - Body: {"content": "<p>纯文本</p>"}
- **验证**：
  - 帖子更新成功
  - 旧图片状态不受影响（仍为使用中）
  - 说明：删除引用后，图片仍保留，需要定时任务或手动清理

#### 6.5 图片提取 - 多种HTML格式
- **准备**：
  - 创建用户A
  - 上传3张图片，获取URL1, URL2, URL3
  - 创建课程和节点
- **登录**：用户A登录
- **请求**：POST /posts
  - Body: {"nodeId": 节点ID, "content": "<div><img src=\"URL1\"><p><img src='URL2'/></p><img src=URL3></div>", "type": 1}
- **验证**：
  - 帖子创建成功
  - 3张图片都被正确标记为使用中

#### 6.6 标记失败不影响主流程
- **准备**：
  - 创建用户A
  - 上传1张图片，获取URL
  - 手动删除该图片记录（模拟图片不存在）
  - 创建课程和节点
- **登录**：用户A登录
- **请求**：POST /posts
  - Body: {"nodeId": 节点ID, "content": "<p><img src='URL'/></p>", "type": 1}
- **验证**：
  - 帖子创建成功（标记图片失败不影响帖子创建）
  - 日志中记录了标记失败的错误信息

---

## 测试执行顺序建议

1. **先测试基础功能**：上传图片（接口1）
2. **再测试查询功能**：配额查询（接口4）、上传历史（接口5）
3. **然后测试管理功能**：标记使用（接口2）、删除图片（接口3）
4. **最后测试集成功能**：自动标记图片（集成测试）

---

## 注意事项

1. **文件上传测试**：
   - 使用 MockMultipartFile 模拟文件上传
   - 不需要真实的图片文件，字节数组即可
   - 重点测试文件类型、大小验证

2. **配额测试**：
   - 使用 Redis 存储配额信息
   - 测试前需要确保 Redis 可用
   - 测试后清理 Redis 中的测试数据

3. **权限测试**：
   - 标记和删除接口需要管理员权限
   - 测试时区分普通用户和管理员用户

4. **自动标记测试**：
   - 需要集成 PostService 进行测试
   - 验证从创建帖子到标记图片的完整流程
   - 确保标记失败不影响主业务流程

5. **数据清理**：
   - 所有测试使用 @Transactional 自动回滚
   - 测试结束后清理 Sa-Token 登录状态
   - 测试结束后清理 Redis 配额数据

---

*此文档用于指导 ImageUploadController 的测试用例编写，确保接口功能的正确性和健壮性*
