# Claude Code 编程规范

## 核心原则
- 只做被要求的事，不多不少
- 优先修改现有文件，避免创建新文件
- 不主动创建文档文件，除非明确要求

## 一些要求
- 禁止覆盖式的修补bug，禁止未经验证的假设，要找到问题的根本原因，从根本上解决问题
- 你可以说不，对于你认为不合理的需求可以进行拒绝，并给出原因和你认为更好的建议
- 对于我提出的需求，请先进行复述，并列出解决方案和你的疑问，在我确认之前，不要修改代码
- 代码修改完毕请客观评价，并给出后续建议

## 代码规范

### Java 后端
- 遵循 Spring Boot 最佳实践
- 使用 @Service/@Repository/@Controller 正确分层
- 异常处理使用 @ControllerAdvice
- 数据校验使用 Bean Validation
- 避免在 Controller 写业务逻辑
- Service 层处理事务
- 使用 Optional 处理可能为空的返回值

### Vue 3 前端
- 使用 Composition API (setup)
- 优先使用 TypeScript
- 组件名使用 PascalCase
- 避免直接操作 DOM
- 使用 Pinia 进行状态管理
- 异步操作使用 async/await
- 组件通信优先使用 props/emit

## 命名规范
- Java: camelCase (方法/变量), PascalCase (类)
- JavaScript/Vue: camelCase (变量/方法), PascalCase (组件)
- CSS: kebab-case
- 文件名: 组件用 PascalCase.vue, 其他用 camelCase.js
- 数据库: snake_case

## Git 提交
- 只在明确要求时才提交
- 提交前运行测试和 lint
- 使用清晰的提交信息
- 不提交 .env 或敏感信息

## 测试要求
- 修改后运行相关测试
- Java: mvn test
- Vue: npm run test
- 检查类型: npm run typecheck
- 代码检查: npm run lint

## 项目结构

### 后端结构
```
backend/
├── learn-api/          # REST API 层
├── learn-domain/       # 业务逻辑层
├── learn-persistence/  # 数据持久层
└── learn-dto/         # 数据传输对象
```

### 前端结构
```
web/
├── src/
│   ├── components/    # 可复用组件
│   ├── views/        # 页面视图
│   ├── services/     # API 服务
│   ├── composables/  # 组合式函数
│   └── stores/       # Pinia 状态管理
```

## 安全规范
- 不在代码中硬编码密钥
- 使用环境变量管理配置
- 输入验证和清理
- SQL 使用参数化查询
- XSS 防护：对用户输入进行转义

## 性能优化
- Vue: 使用 v-memo, computed, watchEffect 优化
- 懒加载大型组件
- 图片使用适当格式和大小
- API 调用添加缓存策略
- 数据库查询使用索引

## 错误处理
- 始终捕获异步错误
- 提供用户友好的错误信息
- 记录详细错误日志
- 使用全局错误处理器

## 注释规范
- 只在必要时添加注释
- 代码应该自解释
- 复杂逻辑需要说明意图
- TODO 注释需包含任务说明

## 重要提醒
- 修改代码前先理解现有代码风格
- 使用项目已有的库和工具
- 保持代码一致性
- 不要引入不必要的依赖

## 常用命令
```bash
# 后端
cd backend
mvn clean install
mvn spring-boot:run

# 前端
cd web
npm install
npm run dev
npm run build
npm run lint
npm run typecheck
```

## 禁止事项
- ❌ 不要修改 .gitignore
- ❌ 不要提交 node_modules 或 target 目录
- ❌ 不要在生产代码中使用 console.log
- ❌ 不要禁用 ESLint 规则
- ❌ 不要使用 any 类型（TypeScript）
- ❌ 不要直接操作 DOM（Vue）

## API 规范
- RESTful 设计原则
- 使用合适的 HTTP 状态码
- 统一的错误响应格式
- API 版本管理 (/api/v1/)
- 分页使用统一参数 (page, size, sort)

---
*此规范用于指导 Claude Code 的编程行为，确保代码质量和一致性*