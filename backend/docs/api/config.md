# 配置接口文档

> 控制器: `ConfigController`
> 基础路径: `/api/v1/config`

---

## 接口列表

1. [获取验证规则配置](#1-获取验证规则配置)

---

## 1. 获取验证规则配置

### 接口信息
- **路径**: `GET /api/v1/config/validation`
- **描述**: 获取所有字段的前端验证规则配置，包括最小长度、最大长度和字段标签
- **认证**: 不需要
- **权限**: 无

### 功能说明
- 返回系统中所有字段的验证规则，用于前端表单验证
- 支持 HTTP ETag 缓存机制，优化性能
- 配置从 `application.yml` 中读取，服务启动时加载到内存

### 缓存机制

#### ETag 工作流程
1. **首次请求**：
   - 客户端发送请求，不带 `If-None-Match` header
   - 服务器返回 200 + 完整数据 + `ETag` header
   - 浏览器缓存数据

2. **后续每次请求**：
   - 客户端每次都发送 ETag 验证请求，带上 `If-None-Match: "{etag值}"` header
   - 如果配置未变化，服务器返回 `304 Not Modified`（只有响应头，约100字节）
   - 如果配置已变化，服务器返回 200 + 新数据 + 新 ETag
   - 浏览器使用缓存数据（当收到 304 时）或使用新数据（当收到 200 时）

#### ETag 计算方式
- ETag 值 = MD5(所有配置规则的 toString 值)
- 配置改变时，ETag 自动更新

### 请求参数

#### Headers
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| If-None-Match | String | 否 | 客户端缓存的 ETag 值，用于验证缓存是否有效 |

### 响应参数

#### 成功响应 - 200 OK
```json
{
  "code": 200,
  "data": {
    "card-front": {
      "minLength": 5,
      "maxLength": 500,
      "label": "问题"
    },
    "card-back": {
      "minLength": 1,
      "maxLength": 500,
      "label": "答案"
    },
    "deck-title": {
      "minLength": 1,
      "maxLength": 100,
      "label": "卡片组标题"
    },
    "deck-description": {
      "minLength": 0,
      "maxLength": 500,
      "label": "卡片组描述"
    },
    "comment-content": {
      "minLength": 1,
      "maxLength": 1000,
      "label": "评论内容"
    },
    "username": {
      "minLength": 2,
      "maxLength": 50,
      "label": "用户名"
    },
    "password": {
      "minLength": 6,
      "maxLength": 50,
      "label": "密码"
    },
    "biography": {
      "minLength": 0,
      "maxLength": 200,
      "label": "个人简介"
    },
    "email": {
      "minLength": 0,
      "maxLength": 100,
      "label": "邮箱"
    },
    "course-name": {
      "minLength": 1,
      "maxLength": 100,
      "label": "课程名称"
    },
    "course-description": {
      "minLength": 1,
      "maxLength": 500,
      "label": "课程描述"
    },
    "post-content": {
      "minLength": 1,
      "maxLength": 10000,
      "label": "帖子内容"
    },
    "profession-name": {
      "minLength": 1,
      "maxLength": 50,
      "label": "职业名称"
    },
    "profession-description": {
      "minLength": 1,
      "maxLength": 500,
      "label": "职业描述"
    },
    "message-content": {
      "minLength": 1,
      "maxLength": 1000,
      "label": "消息内容"
    },
    "roadmap-content": {
      "minLength": 10,
      "maxLength": 50000,
      "label": "路线图内容"
    },
    "roadmap-description": {
      "minLength": 1,
      "maxLength": 500,
      "label": "路线图描述"
    }
  },
  "timestamp": 1735459200000
}
```

**Response Headers**:
- `ETag`: `"d41d8cd98f00b204e9800998ecf8427e"`
- `Cache-Control`: `no-cache, must-revalidate`

#### 缓存未变化 - 304 Not Modified
当客户端提供的 ETag 与服务器当前配置的 ETag 一致时，返回 304 状态码，不返回响应体。

**Response Headers**:
- `ETag`: `"d41d8cd98f00b204e9800998ecf8427e"`
- `Cache-Control`: `no-cache, must-revalidate`

### 字段说明

#### ValidationRuleDTO
| 字段 | 类型 | 说明 |
|------|------|------|
| minLength | Integer | 最小长度限制 |
| maxLength | Integer | 最大长度限制 |
| label | String | 字段的中文显示名称 |

### 配置字段列表

| 字段名 | 说明 | 用途场景 |
|--------|------|----------|
| card-front | 记忆卡片问题 | 创建/编辑记忆卡片 |
| card-back | 记忆卡片答案 | 创建/编辑记忆卡片 |
| deck-title | 卡片组标题 | 创建/编辑卡片组 |
| deck-description | 卡片组描述 | 创建/编辑卡片组 |
| comment-content | 评论内容 | 发表评论 |
| username | 用户名 | 用户注册/修改资料 |
| password | 密码 | 用户注册/修改密码 |
| biography | 个人简介 | 用户修改资料 |
| email | 邮箱 | 用户注册/修改邮箱 |
| course-name | 课程名称 | 创建/编辑课程 |
| course-description | 课程描述 | 创建/编辑课程 |
| post-content | 帖子内容 | 发布/编辑帖子 |
| profession-name | 职业名称 | 创建/编辑职业 |
| profession-description | 职业描述 | 创建/编辑职业 |
| message-content | 消息内容 | 发送站内消息 |
| roadmap-content | 路线图内容 | 创建/编辑路线图 |
| roadmap-description | 路线图描述 | 创建/编辑路线图 |

### 使用示例

#### 示例 1: 首次请求（无缓存）
```bash
curl -X GET "http://localhost:8080/api/v1/config/validation"
```

**响应**:
```
HTTP/1.1 200 OK
ETag: "abc123..."
Cache-Control: no-cache, must-revalidate

{
  "code": 200,
  "data": {
    "username": {
      "minLength": 2,
      "maxLength": 50,
      "label": "用户名"
    },
    ...
  }
}
```

#### 示例 2: 第二次请求（配置未变化）
```bash
curl -X GET "http://localhost:8080/api/v1/config/validation" \
  -H "If-None-Match: \"abc123...\""
```

**响应**:
```
HTTP/1.1 304 Not Modified
ETag: "abc123..."
Cache-Control: no-cache, must-revalidate

(无响应体，浏览器直接使用缓存数据)
```

#### 示例 3: 配置已更新
```bash
curl -X GET "http://localhost:8080/api/v1/config/validation" \
  -H "If-None-Match: \"abc123...\""
```

**响应**:
```
HTTP/1.1 200 OK
ETag: "xyz789..."
Cache-Control: no-cache, must-revalidate

{
  "code": 200,
  "data": {
    "username": {
      "minLength": 3,  // 配置已更新
      "maxLength": 50,
      "label": "用户名"
    },
    ...
  }
}
```

### 前端使用建议

```javascript
// 使用 axios 自动处理 ETag
const response = await axios.get('/api/v1/config/validation', {
  headers: {
    'If-None-Match': localStorage.getItem('validation-etag')
  }
});

// 保存 ETag 供下次使用
const etag = response.headers.etag;
if (etag) {
  localStorage.setItem('validation-etag', etag);
}

// 使用验证规则
const rules = response.data.data;
console.log(rules['username'].minLength); // 2
console.log(rules['username'].maxLength); // 50
```

### 注意事项

1. **配置来源**：所有验证规则从 `application.yml` 的 `system.validation` 配置项读取
2. **修改配置**：修改配置后需要重启应用才能生效（配置在启动时加载到内存）
3. **no-cache 策略**：浏览器每次都会发送 ETag 验证请求，但响应极小（约100字节），对性能影响可忽略
4. **配置变化立即生效**：由于使用 no-cache，配置更新后用户刷新页面即可获取最新配置
5. **前后端一致性**：前端验证规则与后端 Bean Validation 规则保持一致
6. **最小长度为 0**：表示该字段可以为空字符串（但不能为 null）

### 错误处理

此接口不会返回业务错误，即使系统内部错误也会返回空的规则集合。

---

## 技术实现

### 缓存策略
- **服务端缓存**：配置在应用启动时加载到内存，运行期间不重新读取
- **HTTP 缓存**：使用 `no-cache` + ETag 策略
  - 浏览器每次都发送验证请求（带 ETag）
  - 配置未变时返回 304（约100字节）
  - 配置变化时返回 200 + 完整数据

### 性能优化
1. 内存缓存避免重复读取配置文件
2. ETag 机制在配置未变时只返回响应头（约100字节）
3. 配置变化时立即生效，无延迟

### 配置变更流程
1. 修改 `application.yml` 中的 `system.validation.*` 配置
2. 重启应用，配置自动加载到内存
3. ETag 值自动更新（基于新配置计算）
4. 客户端下次请求时发现 ETag 变化，获取新配置
