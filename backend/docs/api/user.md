# 用户管理接口文档

## 接口概览

| 接口 | 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|------|
| 用户注册 | POST | `/api/v1/auth/register` | 否 | 用户注册账号 |
| 用户登录 | POST | `/api/v1/auth/login` | 否 | 用户登录获取token |
| 邮箱验证 | POST | `/api/v1/auth/validate-email` | 否 | 验证邮箱并自动登录 |
| 获取当前用户信息 | GET | `/api/v1/users/current` | 是 | 获取当前登录用户的完整信息 |
| 修改当前用户信息 | PUT | `/api/v1/users/current` | 是 | 修改当前用户的名称和简介 |
| 更新用户头像 | POST | `/api/v1/users/avatar` | 是 | 上传并更新用户头像 |
| 获取用户公开信息 | GET | `/api/v1/users/{username}` | 是 | 根据用户名获取用户公开信息 |
| 搜索用户 | GET | `/api/v1/users/search` | 否 | 根据用户名模糊搜索用户 |

---

## 1. 用户注册

### 接口信息
- **路径**: `POST /api/v1/auth/register`
- **认证**: 不需要
- **限流**: 5次/分钟 (按IP)

### 请求参数

**Body (JSON)**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| email | String | 是 | @Email, @NotBlank, 长度由配置决定 | 用户邮箱 |
| password | String | 是 | @NotBlank, 长度由配置决定 | 用户密码 |

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1703001234567
}
```

**失败 - 邮箱已存在 (1102)**:
```json
{
  "code": 1102,
  "message": "用户已存在",
  "timestamp": 1703001234567
}
```

**失败 - 参数验证失败 (1002)**:
```json
{
  "code": 1002,
  "message": "邮箱格式不正确",
  "timestamp": 1703001234567
}
```

### 业务说明
- 注册成功后需要通过邮箱验证才能完全激活账号
- 邮箱必须唯一
- 密码会进行加密存储

---

## 2. 用户登录

### 接口信息
- **路径**: `POST /api/v1/auth/login`
- **认证**: 不需要
- **限流**: 10次/分钟 (按IP)

### 请求参数

**Body (JSON)**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| email | String | 是 | @Email, @NotBlank, 长度由配置决定 | 用户邮箱 |
| password | String | 是 | @NotBlank, 长度由配置决定 | 用户密码 |

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "用户名",
    "avatar": "https://example.com/avatar.jpg"
  },
  "timestamp": 1703001234567
}
```

**失败 - 用户不存在 (1116)**:
```json
{
  "code": 1116,
  "message": "用户不存在",
  "timestamp": 1703001234567
}
```

**失败 - 密码错误 (1103)**:
```json
{
  "code": 1103,
  "message": "密码错误",
  "timestamp": 1703001234567
}
```

### 业务说明
- 登录成功后会设置 Sa-Token 认证状态
- 前端需要从响应头 `satoken` 中获取 token
- 后续请求需要在请求头中携带 `token: xxx`

---

## 3. 邮箱验证

### 接口信息
- **路径**: `POST /api/v1/auth/validate-email`
- **认证**: 不需要
- **限流**: 10次/分钟 (按IP)

### 请求参数

**Body (JSON)**:
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| email | String | 是 | @Email, @NotBlank, 最大100字符 | 用户邮箱 |
| code | String | 是 | @NotBlank, 最大10字符 | 验证码 |

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "用户名",
    "email": "user@example.com",
    "avatar": "https://example.com/avatar.jpg",
    "biography": "个人简介",
    "emailValidated": true,
    "createdAt": "2024-01-01T00:00:00"
  },
  "timestamp": 1703001234567
}
```

**失败 - 验证码无效 (1108)**:
```json
{
  "code": 1108,
  "message": "验证码无效",
  "timestamp": 1703001234567
}
```

**失败 - 验证码过期 (1110)**:
```json
{
  "code": 1110,
  "message": "验证码已过期",
  "timestamp": 1703001234567
}
```

### 业务说明
- 验证成功后会自动登录
- 验证码通常通过邮件发送
- 验证码有时效性限制

---

## 4. 获取当前用户信息

### 接口信息
- **路径**: `GET /api/v1/users/current`
- **认证**: 需要登录
- **限流**: 50次/分钟 (按用户)

### 请求参数
无

### 请求头
```
token: your-auth-token
```

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "用户名",
    "email": "user@example.com",
    "avatar": "https://example.com/avatar.jpg",
    "biography": "个人简介",
    "emailValidated": true,
    "createdAt": "2024-01-01T00:00:00"
  },
  "timestamp": 1703001234567
}
```

**失败 - 未登录 (1101)**:
```json
{
  "code": 1101,
  "message": "用户未登录",
  "timestamp": 1703001234567
}
```

### 业务说明
- 返回当前登录用户的完整个人信息
- 包含邮箱等敏感信息
- 需要有效的 token

---

## 5. 修改当前用户信息

### 接口信息
- **路径**: `PUT /api/v1/users/current`
- **认证**: 需要登录
- **限流**: 50次/分钟 (按用户)

### 请求参数

**Body (JSON)**:
```json
{
  "name": "新用户名",
  "biography": "这是我的个人简介"
}
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| name | String | 是 | @NotBlank, 长度由配置决定 | 用户名 |
| biography | String | 否 | 长度由配置决定 | 个人简介 |

### 请求头
```
token: your-auth-token
```

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1703001234567
}
```

**失败 - 参数验证失败 (1002)**:
```json
{
  "code": 1002,
  "message": "用户名不能为空",
  "timestamp": 1703001234567
}
```

### 业务说明
- 只能修改当前登录用户的信息
- 不能修改邮箱和头像（头像使用专用接口）
- 用户名长度限制由系统配置决定

---

## 6. 更新用户头像

### 接口信息
- **路径**: `POST /api/v1/users/avatar`
- **认证**: 需要登录
- **限流**: 5次/分钟 (按用户)

### 请求参数

**Body (multipart/form-data)**:
```
file: (binary file data)
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| file | MultipartFile | 是 | @NotNull | 图片文件 |

### 请求头
```
Content-Type: multipart/form-data
token: your-auth-token
```

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "https://example.com/avatars/user123.jpg",
  "timestamp": 1703001234567
}
```

**失败 - 文件类型不支持 (2404)**:
```json
{
  "code": 2404,
  "message": "不支持的文件类型",
  "timestamp": 1703001234567
}
```

**失败 - 文件过大 (2403)**:
```json
{
  "code": 2403,
  "message": "文件大小超出限制",
  "timestamp": 1703001234567
}
```

### 业务说明
- 只支持图片格式 (JPG, PNG, etc.)
- 文件大小有限制（具体限制由系统配置决定）
- 上传成功后返回新头像的 URL
- 系统会自动压缩和优化图片

---

## 7. 获取用户公开信息

### 接口信息
- **路径**: `GET /api/v1/users/{username}`
- **认证**: 需要登录
- **限流**: 50次/分钟 (按用户)

### 请求参数

**路径参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |

### 请求头
```
token: your-auth-token
```

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "data": {
    "id": 2,
    "name": "其他用户",
    "avatar": "https://example.com/avatar.jpg",
    "biography": "个人简介",
    "createdAt": "2024-01-01T00:00:00",
    "isFollowing": false
  },
  "timestamp": 1703001234567
}
```

**失败 - 用户不存在 (1116)**:
```json
{
  "code": 1116,
  "message": "用户不存在",
  "timestamp": 1703001234567
}
```

### 业务说明
- 返回用户的公开信息（不含邮箱等敏感信息）
- 包含当前用户是否关注该用户的信息
- 用户名必须精确匹配

---

## 8. 搜索用户

### 接口信息
- **路径**: `GET /api/v1/users/search`
- **认证**: 不需要
- **限流**: 30次/分钟 (按IP)

### 请求参数

**Query参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 搜索关键词（用户名） |

### 请求示例
```
GET /api/v1/users/search?name=张三
```

### 响应示例

**成功 (200)**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "张三",
      "avatar": "https://example.com/avatar1.jpg"
    },
    {
      "id": 2,
      "name": "张三丰",
      "avatar": "https://example.com/avatar2.jpg"
    }
  ],
  "timestamp": 1703001234567
}
```

**失败 - 参数验证失败 (1002)**:
```json
{
  "code": 1002,
  "message": "搜索名称不能为空",
  "timestamp": 1703001234567
}
```

### 业务说明
- 支持模糊搜索，返回用户名包含搜索关键词的用户
- 返回简要信息 (id, name, avatar)
- 不需要登录即可搜索
- 搜索结果可能有数量限制

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 1002 | 参数异常 |
| 1101 | 用户未登录 |
| 1102 | 用户已存在 |
| 1103 | 密码错误 |
| 1108 | 验证码无效 |
| 1110 | 验证码已过期 |
| 1116 | 用户不存在 |
| 2301 | 访问过于频繁，请稍后再试 |
| 2403 | 文件大小超出限制 |
| 2404 | 不支持的文件类型 |

---

## 认证说明

### Sa-Token 认证流程

1. **登录**: 调用登录接口，获取 token
   ```bash
   curl -X POST http://api.example.com/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"user@example.com","password":"password123"}'
   ```

2. **响应头获取 token**:
   ```
   satoken: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...
   ```

3. **后续请求携带 token**:
   ```bash
   curl -X GET http://api.example.com/api/v1/users/current \
     -H "token: eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..."
   ```

### Token 说明
- Token 在响应头 `satoken` 中返回
- 后续请求需要在请求头 `token` 中携带
- Token 有过期时间，过期后需要重新登录
- 退出登录后 token 会失效

---

## 限流说明

本模块接口采用令牌桶算法进行限流：

### 全局限流
- **容量**: 50次
- **补充速率**: 每分钟补充50次
- **限流维度**: 按用户ID

### 特殊限流

| 接口 | 容量 | 补充周期 | 限流维度 |
|------|------|----------|----------|
| 用户注册 | 5次 | 1分钟 | 按IP |
| 用户登录 | 10次 | 1分钟 | 按IP |
| 邮箱验证 | 10次 | 1分钟 | 按IP |
| 更新头像 | 5次 | 1分钟 | 按用户 |
| 搜索用户 | 30次 | 1分钟 | 按IP |

### 限流错误响应
```json
{
  "code": 2301,
  "message": "访问过于频繁，请稍后再试",
  "timestamp": 1703001234567
}
```

---

## 注意事项

1. **邮箱验证**: 注册后需要验证邮箱才能完全激活账号
2. **密码安全**: 密码会进行加密存储，系统不保存明文密码
3. **Token 安全**: Token 应妥善保管，不要泄露给他人
4. **文件上传**: 上传头像时注意文件大小和格式限制
5. **限流控制**: 频繁调用接口可能触发限流，请合理控制请求频率
6. **用户名唯一性**: 系统可能要求用户名唯一（具体看业务规则）
