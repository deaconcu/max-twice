# JPA + MyBatis 混合方案迁移检查清单

**项目**: max-twice 后端架构改造
**创建日期**: 2025-11-26
**负责人**: _____________
**预计完成**: _____________

---

## 📋 使用说明

- 使用 `[ ]` 表示未完成
- 使用 `[x]` 表示已完成
- 使用 `[!]` 表示遇到问题，需要关注
- 使用 `[-]` 表示不适用或跳过

---

## 🎯 准备阶段

### 架构理解
- [ ] 团队成员已理解本项目的 **DataService 模式**
- [ ] 理解 DataService 模式 vs 标准 JPA 做法的区别
- [ ] 理解职责分离：BusinessService（业务逻辑）vs DataService（数据+缓存）

### 团队准备
- [ ] 团队成员已了解 JPA 基础知识
- [ ] 团队成员已阅读实施文档 `jpa-mybatis-migration-guide.md`
- [ ] 已召开项目启动会议，明确分工

### 环境准备
- [ ] 确认当前代码已提交到 Git
- [ ] 创建开发分支 `feature/jpa-mybatis-migration`
- [ ] 数据库已备份
- [ ] 准备好测试环境

---

## 🔧 阶段 1: 环境配置（预计 1-2 天）

### 1.1 依赖配置
- [ ] 在 `backend/pom.xml` 中添加 `spring-boot-starter-data-jpa` 依赖
- [ ] 运行 `mvn clean install` 验证依赖无冲突
- [ ] 确认 JPA 和 MyBatis 依赖版本兼容

### 1.2 配置文件
- [ ] 在 `application.yml` 中添加 JPA 配置
- [ ] 配置 `ddl-auto: validate`（只验证，不自动修改表）
- [ ] 配置 `show-sql: true`（开发环境）
- [ ] 配置 Hibernate 命名策略（保持列名原样）
- [ ] 验证 MyBatis 配置未被影响

### 1.3 JPA Auditing 配置
- [ ] 创建 `JpaConfig.java` 配置类
- [ ] 添加 `@EnableJpaAuditing` 注解
- [ ] 配置 `@EnableJpaRepositories` 扫描路径

### 1.4 基础实体类
- [ ] 创建 `BaseEntity.java` 抽象类
- [ ] 添加 `@MappedSuperclass` 注解
- [ ] 添加 `createdAt` 字段（@CreatedDate）
- [ ] 添加 `updatedAt` 字段（@LastModifiedDate）
- [ ] 添加 `@EntityListeners(AuditingEntityListener.class)`

### 1.5 验证配置
- [ ] 运行 `mvn spring-boot:run` 验证启动成功
- [ ] 检查日志，确认 JPA 和 MyBatis 都正常初始化
- [ ] 确认无错误信息

**阶段 1 完成日期**: _____________

---

## 🚀 阶段 2: 试点迁移 - User 模块（预计 3-5 天）

### 2.1 改造 UserDO
- [ ] 为 `UserDO` 添加 `@Entity` 注解
- [ ] 添加 `@Table(name = "user")` 注解
- [ ] 让 `UserDO` 继承 `BaseEntity`
- [ ] 为 `id` 字段添加 `@Id` 和 `@GeneratedValue` 注解
- [ ] 为所有字段添加 `@Column` 注解（指定列名）
- [ ] 删除 `createdAt` 和 `updatedAt` 字段定义（继承自 BaseEntity）
- [ ] 验证字段类型与数据库一致

### 2.2 创建 UserRepository
- [ ] 创建 `UserRepository` 接口
- [ ] 继承 `JpaRepository<UserDO, Long>`
- [ ] 添加 `@Repository` 注解
- [ ] 添加 `Optional<UserDO> findByEmail(String email)` 方法
- [ ] 添加 `Optional<UserDO> findByName(String name)` 方法
- [ ] 添加 `List<UserDO> findByIdIn(Collection<Long> ids)` 方法（批量查询）
- [ ] **重要：不要在 Repository 添加缓存注解**（缓存在 DataService 管理）
- [ ] 验证方法命名符合 JPA 规范

### 2.3 精简 UserMapper
- [ ] **保留**：`getByIds(Collection<Long> ids)` - 动态 SQL
- [ ] **保留**：`getMapByIds(Collection<Long> ids)` - 返回 Map 的查询
- [ ] **保留**：`searchByName(String name)` - INSTR 模糊查询
- [ ] **保留**：`getList(int count)` - 分页查询
- [ ] **保留**：`getListPaginated(long offsetId, int count)` - 游标分页
- [ ] **删除**：`getById(long id)` → 改用 JPA Repository
- [ ] **删除**：`getByEmail(String email)` → 改用 JPA Repository
- [ ] **删除**：`getByName(String name)` → 改用 JPA Repository
- [ ] **删除**：`insert(UserDO user)` → 改用 JPA Repository
- [ ] **删除**：`update(UserDO user)` → 改用 JPA Repository

### 2.4 重构 UserDataService（保持现有架构）
- [ ] **重要**：确认 UserDataService 已存在
- [ ] 注入 `UserRepository`（新增）
- [ ] 保留 `UserMapper` 注入
- [ ] 修改 `getById()` 使用 `userRepository.findById()` 替换 `userMapper.getById()`
- [ ] 修改 `getByEmail()` 使用 `userRepository.findByEmail()`
- [ ] 修改 `getByName()` 使用 `userRepository.findByName()`
- [ ] 修改 `getByIds()` 中的数据库查询部分，使用 `userRepository.findByIdIn()`
- [ ] 修改 `insert()` 使用 `userRepository.save()`
- [ ] 修改 `update()` 使用 `userRepository.save()`
- [ ] **保留**：`getMapByIds()` 继续使用 `userMapper`
- [ ] **保留**：`searchByName()` 继续使用 `userMapper`
- [ ] **保留**：所有缓存逻辑（@Cacheable, @CacheEvict）
- [ ] 删除手动设置 `createdAt` 和 `updatedAt` 的代码（JPA Auditing 自动填充）

### 2.5 验证 BusinessService 不需要修改
- [ ] 确认 `UserService`（BusinessService）调用的是 `UserDataService`
- [ ] 确认 BusinessService 不直接依赖 Repository/Mapper
- [ ] **不需要修改 BusinessService 代码**（因为 DataService 接口不变）

### 2.6 编写测试
- [ ] 创建 `UserDataServiceTest` 测试类（测试数据访问层）
- [ ] 测试 JPA 简单查询：`getById()`, `getByEmail()`, `getByName()`
- [ ] 测试 JPA 批量查询：`getByIds()`
- [ ] 测试 JPA 保存：`save()` - 验证时间自动填充
- [ ] 测试 JPA 更新：`update()` - 验证 `updatedAt` 自动更新
- [ ] 测试 MyBatis 复杂查询：`getMapByIds()`, `searchByName()`
- [ ] 测试缓存功能：验证 `@Cacheable` 和 `@CacheEvict` 生效
- [ ] 运行所有测试，确保通过

### 2.7 回归测试
- [ ] 测试 `getUserById()` - 验证 JPA 查询
- [ ] 测试 `updateUser()` - 验证 `updatedAt` 自动更新
- [ ] 测试 `getUsersByIds()` - 验证 MyBatis 复杂查询
- [ ] 测试 `searchByName()` - 验证 MyBatis 搜索功能
- [ ] 运行所有测试，确保通过

### 2.6 回归测试
- [ ] 运行 `UserController` 相关的所有 API 测试
- [ ] 手动测试用户注册功能
- [ ] 手动测试用户登录功能
- [ ] 手动测试用户信息更新功能
- [ ] 验证时间字段显示正确

### 2.7 性能测试
- [ ] 对比 JPA 和 MyBatis 的简单查询性能
- [ ] 记录性能数据
- [ ] 确认性能差异在可接受范围内（±10%）

**阶段 2 完成日期**: _____________

---

## 📦 阶段 3: 批量迁移（预计 2-3 周）

### 3.1 Course 模块（P0 优先级）

#### CourseDO 改造
- [ ] 添加 JPA 注解
- [ ] 继承 `BaseEntity`
- [ ] 配置字段映射

#### CourseRepository
- [ ] 创建 `CourseRepository` 接口
- [ ] 定义基础查询方法

#### CourseMapper 精简
- [ ] 删除简单 CRUD 方法
- [ ] 保留复杂查询（如多表联查）

#### CourseService 重构
- [ ] 简单操作使用 `courseRepository`
- [ ] 复杂查询使用 `courseMapper`

#### 测试
- [ ] 单元测试
- [ ] API 集成测试
- [ ] 回归测试

**Course 模块完成日期**: _____________

---

### 3.2 Node 模块（P0 优先级）

#### NodeDO 改造
- [ ] 添加 JPA 注解
- [ ] 继承 `BaseEntity`
- [ ] 配置字段映射

#### NodeRepository
- [ ] 创建 `NodeRepository` 接口
- [ ] 定义基础查询方法

#### NodeMapper 精简
- [ ] 删除简单 CRUD 方法
- [ ] 保留树形结构查询

#### NodeService 重构
- [ ] 简单操作使用 `nodeRepository`
- [ ] 复杂查询使用 `nodeMapper`

#### 测试
- [ ] 单元测试
- [ ] API 集成测试
- [ ] 回归测试

**Node 模块完成日期**: _____________

---

### 3.3 Post 模块（P1 优先级）

#### PostDO 改造
- [ ] 添加 JPA 注解
- [ ] 继承 `BaseEntity`
- [ ] 配置字段映射
- [ ] 处理 `scoreCalculatedAt` 等特殊字段

#### PostRepository
- [ ] 创建 `PostRepository` 接口
- [ ] 定义基础查询方法

#### PostMapper 精简
- [ ] 删除简单 CRUD 方法
- [ ] 保留复杂评分查询
- [ ] 保留多表联查

#### PostService 重构
- [ ] 简单操作使用 `postRepository`
- [ ] 复杂查询使用 `postMapper`

#### 测试
- [ ] 单元测试
- [ ] API 集成测试
- [ ] 回归测试

**Post 模块完成日期**: _____________

---

### 3.4 Comment 模块（P1 优先级）

#### CommentDO 改造
- [ ] 添加 JPA 注解
- [ ] 继承 `BaseEntity`
- [ ] 配置字段映射

#### CommentRepository
- [ ] 创建 `CommentRepository` 接口
- [ ] 定义基础查询方法

#### CommentMapper 精简
- [ ] 删除简单 CRUD 方法
- [ ] 保留树形评论查询
- [ ] 保留统计查询

#### CommentService 重构
- [ ] 简单操作使用 `commentRepository`
- [ ] 复杂查询使用 `commentMapper`

#### 测试
- [ ] 单元测试
- [ ] API 集成测试
- [ ] 回归测试

**Comment 模块完成日期**: _____________

---

### 3.5 MemoryCardDeck 模块（P2 优先级）

#### MemoryCardDeckDO 改造
- [ ] 添加 JPA 注解
- [ ] 继承 `BaseEntity`
- [ ] 配置字段映射

#### MemoryCardDeckRepository
- [ ] 创建 Repository 接口
- [ ] 定义基础查询方法

#### MemoryCardDeckMapper 精简
- [ ] 删除简单 CRUD 方法
- [ ] 保留复杂统计查询

#### MemoryCardDeckService 重构
- [ ] 简单操作使用 Repository
- [ ] 复杂查询使用 Mapper

#### 测试
- [ ] 单元测试
- [ ] API 集成测试
- [ ] 回归测试

**MemoryCardDeck 模块完成日期**: _____________

---

### 3.6 MemoryCard 模块（P2 优先级）

#### MemoryCardDO 改造
- [ ] 添加 JPA 注解
- [ ] 继承 `BaseEntity`
- [ ] 配置字段映射

#### MemoryCardRepository
- [ ] 创建 Repository 接口
- [ ] 定义基础查询方法

#### MemoryCardMapper 精简
- [ ] 删除简单 CRUD 方法
- [ ] 保留批量操作
- [ ] 保留复杂查询

#### MemoryCardService 重构
- [ ] 简单操作使用 Repository
- [ ] 批量操作使用 Mapper
- [ ] 复杂查询使用 Mapper

#### 测试
- [ ] 单元测试
- [ ] API 集成测试
- [ ] 回归测试

**MemoryCard 模块完成日期**: _____________

---

### 3.7 UserCardSrs 模块（P2 优先级）

#### UserCardSrsDO 改造
- [ ] 添加 JPA 注解
- [ ] 继承 `BaseEntity`
- [ ] 配置字段映射

#### UserCardSrsRepository
- [ ] 创建 Repository 接口
- [ ] 定义基础查询方法

#### UserCardSrsMapper 精简
- [ ] 删除简单 CRUD 方法
- [ ] 保留复杂的 UNION 查询
- [ ] 保留 SRS 算法相关查询

#### UserCardSrsService 重构
- [ ] 简单操作使用 Repository
- [ ] 复杂 SRS 查询使用 Mapper

#### 测试
- [ ] 单元测试
- [ ] SRS 算法测试
- [ ] API 集成测试
- [ ] 回归测试

**UserCardSrs 模块完成日期**: _____________

---

### 3.8 其他模块（P3 优先级）

按照以下模块依次迁移：

#### Profession 模块
- [ ] ProfessionDO 改造
- [ ] 创建 ProfessionRepository
- [ ] 精简 ProfessionMapper
- [ ] 重构 ProfessionService
- [ ] 测试

#### Roadmap 模块
- [ ] RoadmapDO 改造
- [ ] 创建 RoadmapRepository
- [ ] 精简 RoadmapMapper
- [ ] 重构 RoadmapService
- [ ] 测试

#### Curriculum 模块
- [ ] CurriculumDO 改造
- [ ] 创建 CurriculumRepository
- [ ] 精简 CurriculumMapper
- [ ] 重构 CurriculumService
- [ ] 测试

#### 其他小模块
- [ ] Follow 模块
- [ ] Like 模块
- [ ] Bookmark 模块
- [ ] Subscription 模块
- [ ] Notification 模块
- [ ] 等等...

**其他模块完成日期**: _____________

---

## ✅ 阶段 4: 测试与优化（预计 1 周）

### 4.1 全量功能测试
- [ ] 运行所有单元测试
- [ ] 运行所有集成测试
- [ ] 手动测试核心功能流程
- [ ] 验证时间字段在所有模块中正常工作

### 4.2 性能测试
- [ ] 测试简单 CRUD 性能（JPA vs MyBatis）
- [ ] 测试复杂查询性能
- [ ] 测试批量操作性能
- [ ] 记录性能数据并分析

### 4.3 问题排查

#### N+1 查询问题
- [ ] 检查日志中的 SQL 数量
- [ ] 识别 N+1 查询场景
- [ ] 使用 `@EntityGraph` 优化
- [ ] 验证优化效果

#### 事务问题
- [ ] 验证 JPA 和 MyBatis 事务一致性
- [ ] 测试事务回滚场景
- [ ] 确认分布式事务（如有）正常

#### 字段映射问题
- [ ] 检查所有实体类字段映射
- [ ] 验证特殊类型字段（如枚举、JSON）
- [ ] 确认时间类型正确映射

### 4.4 代码质量
- [ ] 运行 SonarQube 分析
- [ ] 修复所有严重问题
- [ ] 代码覆盖率达到 80% 以上
- [ ] 通过代码审查

### 4.5 文档更新
- [ ] 更新技术文档
- [ ] 更新 API 文档（如有变化）
- [ ] 记录已知问题和解决方案
- [ ] 编写迁移总结报告

**阶段 4 完成日期**: _____________

---

## 🚢 阶段 5: 部署上线（预计 2-3 天）

### 5.1 部署前检查
- [ ] 所有测试通过
- [ ] 代码已合并到主分支
- [ ] 生产环境配置已准备
- [ ] 数据库无需变更（`ddl-auto: validate`）
- [ ] 回滚方案已准备

### 5.2 灰度发布（推荐）
- [ ] 部署到预生产环境
- [ ] 运行冒烟测试
- [ ] 观察日志和性能指标
- [ ] 逐步放量到生产环境

### 5.3 监控
- [ ] 监控应用启动状态
- [ ] 监控 SQL 执行日志
- [ ] 监控性能指标（响应时间、QPS）
- [ ] 监控错误日志

### 5.4 验证
- [ ] 验证核心功能正常
- [ ] 验证时间字段显示正确
- [ ] 验证性能无明显下降
- [ ] 收集用户反馈

**上线日期**: _____________

---

## 📈 度量指标

### 代码度量
- **原 Mapper 方法总数**: 335 个
- **迁移到 JPA 的方法数**: _____ 个（目标 ~150 个）
- **保留 MyBatis 的方法数**: _____ 个（目标 ~185 个）
- **代码减少行数**: _____ 行

### 性能度量
- **简单查询性能对比**: JPA _____ ms vs MyBatis _____ ms
- **复杂查询性能对比**: JPA _____ ms vs MyBatis _____ ms
- **批量操作性能对比**: JPA _____ ms vs MyBatis _____ ms

### 质量度量
- **单元测试覆盖率**: _____%（目标 > 80%）
- **集成测试通过率**: _____%（目标 100%）
- **SonarQube 严重问题数**: _____ 个（目标 0 个）

---

## ⚠️ 风险和问题追踪

### 已识别风险

| 风险 | 等级 | 缓解措施 | 状态 |
|------|------|---------|------|
| JPA 和 MyBatis 事务冲突 | 中 | 统一使用 @Transactional | [ ] |
| N+1 查询性能问题 | 中 | 使用 @EntityGraph 优化 | [ ] |
| 字段映射错误 | 高 | 充分测试，逐步迁移 | [ ] |
| 团队学习成本 | 低 | 提供培训和文档 | [ ] |

### 问题日志

| 日期 | 问题描述 | 影响 | 解决方案 | 状态 |
|------|---------|------|---------|------|
| | | | | |
| | | | | |
| | | | | |

---

## 📝 备注

### 重要提醒
1. **每完成一个模块都要进行充分测试**
2. **遇到问题及时记录在"问题日志"中**
3. **保持代码提交频率，方便回滚**
4. **复杂查询优先保留 MyBatis 实现**
5. **性能测试结果记录在"性能度量"中**

### 下一步计划
- [ ] 总结迁移经验
- [ ] 制定后续优化计划
- [ ] 考虑引入 QueryDSL（可选）
- [ ] 考虑引入 Redis 缓存优化（可选）

---

**检查清单完成日期**: _____________
**最终验收人**: _____________
**签字**: _____________
