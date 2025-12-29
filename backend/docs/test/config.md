# 配置接口测试文档

> 测试类: `ConfigControllerTest`
> 测试目标: 验证验证规则配置接口的正确性和 ETag 缓存机制

---

## 测试环境准备

### 依赖组件
- MockMvc（用于接口测试）
- ValidationConfigService（验证规则配置服务）
- SystemProperties（系统配置属性）

### 测试数据准备
- 无需准备额外数据
- 配置从 application.yml 自动加载

---

## 一、获取验证规则接口测试

### 1.1 成功获取所有验证规则
- 发送 GET 请求，不带任何 header
- 验证返回 200 状态码
- 验证返回的 code 为 200
- 验证返回的 data 不为空
- 验证 data 是 Map 结构
- 验证包含所有预期的字段（至少包含核心字段）

### 1.2 验证返回的字段完整性
- 验证包含卡片相关字段：card-front, card-back, deck-title, deck-description
- 验证包含评论相关字段：comment-content
- 验证包含用户相关字段：username, password, biography, email
- 验证包含课程相关字段：course-name, course-description
- 验证包含帖子相关字段：post-content
- 验证包含职业相关字段：profession-name, profession-description
- 验证包含消息相关字段：message-content
- 验证包含路线图相关字段：roadmap-content, roadmap-description
- 验证总共至少有 17 个字段

### 1.3 验证每个规则的字段结构
- 验证每个规则包含 minLength 字段（Integer 类型）
- 验证每个规则包含 maxLength 字段（Integer 类型）
- 验证每个规则包含 label 字段（String 类型）
- 验证 minLength 和 maxLength 的值都 >= 0
- 验证 minLength <= maxLength
- 验证 label 不为空

### 1.4 验证具体字段的规则值
- 验证 username 的 minLength >= 2
- 验证 username 的 maxLength <= 50
- 验证 password 的 minLength >= 6
- 验证 email 的 minLength = 0（允许为空）
- 验证 card-front 的 minLength >= 1
- 验证 course-name 的 maxLength > 0

### 1.5 验证响应包含 ETag header
- 发送 GET 请求
- 验证响应 header 包含 ETag
- 验证 ETag 值格式正确（引号包裹的字符串）
- 验证 ETag 值不为空

### 1.6 验证响应包含正确的 Cache-Control header
- 发送 GET 请求
- 验证响应 header 包含 Cache-Control
- 验证 Cache-Control 包含 "no-cache"
- 验证 Cache-Control 包含 "must-revalidate"

---

## 二、ETag 缓存机制测试

### 2.1 首次请求返回完整数据和 ETag
- 发送 GET 请求，不带 If-None-Match header
- 验证返回 200 状态码
- 验证返回完整的验证规则数据
- 验证响应包含 ETag header
- 保存 ETag 值供后续测试使用

### 2.2 使用相同 ETag 请求返回 304
- 先发送请求获取 ETag
- 使用获取的 ETag 值设置 If-None-Match header
- 再次发送 GET 请求
- 验证返回 304 Not Modified 状态码
- 验证响应不包含 body（或 body 为空）
- 验证响应包含 ETag header（与请求的 ETag 相同）

### 2.3 使用错误的 ETag 请求返回 200
- 发送 GET 请求，设置错误的 If-None-Match 值（如 "wrong-etag"）
- 验证返回 200 状态码
- 验证返回完整的验证规则数据
- 验证响应包含新的 ETag header
- 验证新的 ETag 与请求中的 ETag 不同

### 2.4 ETag 值的一致性
- 连续发送两次 GET 请求（不带 If-None-Match）
- 验证两次响应的 ETag 值相同（配置未变时）
- 验证返回的数据内容完全一致

### 2.5 空 ETag 的处理
- 发送 GET 请求，设置 If-None-Match 为空字符串
- 验证返回 200 状态码（视为首次请求）
- 验证返回完整数据

### 2.6 多次 304 响应的一致性
- 获取一次 ETag 值
- 使用该 ETag 连续发送 3 次请求
- 验证 3 次都返回 304 状态码
- 验证 3 次响应的 ETag 值都相同

---

## 三、响应格式测试

### 3.1 响应符合 ApiResponse 格式
- 验证响应包含 code 字段
- 验证响应包含 data 字段
- 验证响应包含 timestamp 字段
- 验证 timestamp 为有效的时间戳

### 3.2 成功响应的 code 值
- 验证成功响应的 code = 200
- 验证 304 响应没有 code 字段（因为无响应体）

### 3.3 data 字段的类型
- 验证 data 是 Map<String, ValidationRuleDTO> 类型
- 验证 Map 的 key 是字符串
- 验证 Map 的 value 是对象（包含 minLength, maxLength, label）

---

## 四、边界条件测试

### 4.1 不带任何 header 的请求
- 发送最简单的 GET 请求
- 验证返回 200 状态码
- 验证正常返回数据

### 4.2 带多余 header 的请求
- 发送带额外无关 header 的请求
- 验证不影响正常响应
- 验证返回 200 状态码

### 4.3 If-None-Match header 大小写
- 测试 "if-none-match" 小写
- 测试 "IF-NONE-MATCH" 大写
- 测试 "If-None-Match" 标准格式
- 验证都能正确处理（HTTP header 不区分大小写）

### 4.4 ETag 值包含特殊字符
- ETag 值的引号处理
- 验证服务器返回的 ETag 格式正确（带引号）
- 验证客户端传入 ETag 时可以带或不带引号

---

## 五、数据完整性测试

### 5.1 所有字段都有验证规则
- 验证系统中所有需要验证的字段都在返回结果中
- 验证没有遗漏的重要字段
- 验证字段数量符合预期（17 个）

### 5.2 验证规则的合理性
- 验证 minLength 不会大于 maxLength
- 验证 maxLength 的值合理（不会过大或过小）
- 验证密码字段的 minLength >= 6（安全性要求）
- 验证邮箱字段的 maxLength 足够长
- 验证内容类字段的 maxLength 足够大（如 post-content, roadmap-content）

### 5.3 标签的中文显示
- 验证所有 label 都是中文
- 验证 label 描述准确清晰
- 验证没有空的或错误的 label

---

## 六、性能测试

### 6.1 首次请求响应时间
- 发送 GET 请求（不带 If-None-Match）
- 验证响应时间 < 100ms
- 验证内存缓存有效（从内存读取）

### 6.2 304 响应的响应时间
- 发送带正确 ETag 的请求
- 验证 304 响应时间 < 50ms（比首次请求更快）
- 验证不需要序列化数据

### 6.3 并发请求测试
- 并发发送 10 个请求（不带 If-None-Match）
- 验证所有请求都返回 200
- 验证所有响应的 ETag 相同
- 验证所有响应的数据一致

### 6.4 并发 304 请求测试
- 并发发送 10 个请求（带相同的 If-None-Match）
- 验证所有请求都返回 304
- 验证响应时间都很快

---

## 七、缓存一致性测试

### 7.1 ETag 不变时数据一致
- 发送首次请求，保存 ETag 和数据
- 等待 1 秒
- 发送第二次请求（不带 If-None-Match）
- 验证 ETag 相同
- 验证数据完全一致

### 7.2 内存缓存的有效性
- 验证多次请求返回相同的对象引用（使用内存缓存）
- 验证不会每次都重新构建 Map

---

## 八、异常场景测试

### 8.1 ValidationConfigService 为 null
- 模拟 ValidationConfigService 未注入
- 验证系统抛出合适的异常（在启动时）

### 8.2 配置缺失的处理
- 如果某些配置项缺失（在 application.yml 中）
- 验证系统能正常启动（使用默认值）
- 或验证系统启动失败并给出明确错误信息

### 8.3 配置值非法
- 如果 minLength > maxLength（配置错误）
- 验证系统能检测到并报错
- 或验证系统能自动修正

---

## 九、HTTP 标准兼容性测试

### 9.1 HTTP 方法验证
- 使用 POST 方法访问（应该不支持）
- 使用 PUT 方法访问（应该不支持）
- 使用 DELETE 方法访问（应该不支持）
- 验证返回 405 Method Not Allowed

### 9.2 Accept header 处理
- 不带 Accept header
- Accept: application/json
- Accept: */*
- 验证都能正常返回 JSON 数据

### 9.3 Content-Type 验证
- 验证响应 Content-Type 为 application/json
- 验证响应编码为 UTF-8

---

## 十、集成测试

### 10.1 与前端集成
- 模拟前端首次加载场景
- 模拟前端刷新页面场景
- 模拟前端 localStorage 存储 ETag 场景
- 验证整个流程符合预期

### 10.2 与配置系统集成
- 验证配置从 application.yml 正确读取
- 验证配置在启动时正确加载
- 验证配置在运行时不会重新读取（内存缓存）

---

## 测试注意事项

1. **启动时加载**
   - 配置在应用启动时加载到内存
   - 测试时确保 ValidationConfigService 已初始化

2. **ETag 格式**
   - 服务器返回的 ETag 格式为 "quoted-string"
   - 客户端发送时可以带或不带引号

3. **304 响应**
   - 304 响应没有 body
   - 304 响应仍然包含 ETag 和 Cache-Control header

4. **no-cache 策略**
   - 每次请求都应该到达服务器（不是浏览器直接返回缓存）
   - 服务器通过 ETag 判断是否返回 304

5. **并发测试**
   - 使用线程池或异步方式并发发送请求
   - 验证线程安全性

6. **内存缓存**
   - ValidationConfigService 使用内存缓存
   - 确保测试不会影响缓存状态

---

## 测试覆盖目标

- **接口覆盖率**: 100%（只有 1 个接口）
- **分支覆盖率**: > 90%（覆盖所有 ETag 判断分支）
- **异常场景**: 覆盖所有可能的异常情况
- **边界条件**: 覆盖所有边界值和特殊输入

---

## 测试数据验证

### 预期的字段列表
1. card-front - 记忆卡片问题
2. card-back - 记忆卡片答案
3. deck-title - 卡片组标题
4. deck-description - 卡片组描述
5. comment-content - 评论内容
6. username - 用户名
7. password - 密码
8. biography - 个人简介
9. email - 邮箱
10. course-name - 课程名称
11. course-description - 课程描述
12. post-content - 帖子内容
13. profession-name - 职业名称
14. profession-description - 职业描述
15. message-content - 消息内容
16. roadmap-content - 路线图内容
17. roadmap-description - 路线图描述

### 验证规则的合理范围
- username: minLength 2-5, maxLength 20-100
- password: minLength 6-10, maxLength 20-100
- email: minLength 0, maxLength 50-200
- 内容类字段: maxLength >= 1000
- 标题类字段: maxLength 50-200

---

## Mock 策略

### 需要 Mock 的组件
- 无（使用真实的 ValidationConfigService）

### 不需要 Mock 的组件
- ValidationConfigService（使用真实实例）
- SystemProperties（从配置文件加载）

### Mock 建议
- 建议使用真实配置进行测试
- 如需测试配置缺失场景，可以使用专门的测试配置文件

---

## 测试执行顺序

1. 先执行基础功能测试（一、三、五）
2. 再执行 ETag 缓存测试（二）
3. 然后执行边界条件测试（四）
4. 最后执行性能和并发测试（六、七）
5. 异常场景测试可以独立执行（八）
