# Node 和 ToC 重构任务进度

## 📋 任务目标

支持 Roadmap 包含任意 Node（不仅限于 Course），实现按 Node 阅读时显示左侧目录。

### 核心需求场景
用户需要学习跨课程的知识组合，例如：
- Java编程基础（完整课程）
- 概率论 - 第3章：随机变量（单个节点）
- 概率论 - 第5章：概率分布（单个节点）
- 线性代数 - 第2章：向量运算（单个节点）

不需要学完整门课程，只学习需要的章节。

## ✅ 已完成

### 1. 数据库迁移脚本
**文件**：`docs/migrations/001_node_and_toc_refactor.sql`

**内容**：
- ✅ `user_course_toc` 表重命名为 `user_node_toc`
- ✅ `course_id` 字段改为 `node_id`
- ✅ 索引调整
- ✅ `node` 表增加 `is_course_root` 字段
- ✅ 数据迁移：设置现有课程根节点的 `is_course_root=1`
- ✅ 4个数据一致性检查
- ✅ 完整的回滚脚本

**执行方式**：
```bash
mysql -u用户名 -p learn_database < docs/migrations/001_node_and_toc_refactor.sql
```

### 2. Roadmap Content 数据迁移代码
**文件**：`backend/learn-application/src/main/java/com/prosper/learn/application/migration/RoadmapContentMigration.java`

**功能**：
- ✅ 将 `roadmap.content` 中的 `course_id` 转换为 `root_node_id`
- ✅ 分批查询所有 Roadmap
- ✅ 自动跳过已迁移的
- ✅ 验证迁移结果

### 3. 通用 Spring 方法执行器
**文件**：`backend/learn-application/src/main/java/com/prosper/learn/application/runner/SpringMethodRunner.java`

**功能**：
- ✅ 启动 Spring 容器（非 Web 模式）
- ✅ 执行任意 Bean 的任意方法
- ✅ 只扫描必要的包，避免加载不需要的配置
- ✅ 使用和 Web 启动类相同的配置

**执行方式**：
```bash
mvn exec:java -pl learn-application \
  -Dexec.mainClass="com.prosper.learn.application.runner.SpringMethodRunner" \
  -Dexec.args="com.prosper.learn.application.migration.RoadmapContentMigration migrateAllRoadmaps"
```

## 🚧 进行中

### 调试 SpringMethodRunner
**当前状态**：正在解决 Bean 依赖问题
- ❌ 缺少 RedisTemplate Bean
- 🔧 正在添加必要的包扫描路径

## 📝 待办事项

### 任务 2：重命名 UserCourseToc 相关类
**文件列表**：
- [ ] `UserCourseTocDO.java` → `UserNodeTocDO.java`
- [ ] `UserCourseTocMapper.java` → `UserNodeTocMapper.java`
- [ ] `UserCourseTocDataService.java` → `UserNodeTocDataService.java`
- [ ] 所有字段 `courseId` → `nodeId`
- [ ] Mapper XML SQL 修改

### 任务 3：修改 NodeDO 和 Mapper
**文件列表**：
- [ ] `NodeDO.java`：增加 `isCourseRoot` 字段
- [ ] `NodeMapper.java`：
  - [ ] SELECT 语句包含 `is_course_root`
  - [ ] INSERT 语句包含 `is_course_root`

### 任务 4：修改 TocDomainService
**文件**：`TocDomainService.java`

**改动**：
- [ ] `getToc(userId, courseId)` → `getToc(userId, nodeId)`
- [ ] `getToc(userId, courseId, tocIndex)` → `getToc(userId, nodeId, tocIndex)`
- [ ] 修改验证逻辑：`validateCourseExists` → `validateNodeExists`
- [ ] 修改默认 ToC 生成逻辑：使用 `nodeId` 而非 `courseDO.getRootNodeId()`
- [ ] 所有内部方法参数 `courseId` → `nodeId`

### 任务 5：修改 RoadmapDomainService
**文件**：`RoadmapDomainService.java`

**改动**：
- [ ] `parseContentToGraphFormat()` 方法
  - [ ] 参数：`Map<Long, String> courseNames` → `Map<Long, NodeInfo>`
  - [ ] 根据 `is_course_root` 决定显示 Course 名称还是 Node 名称
- [ ] `parseNodes()` 方法
  - [ ] 输入改为 `nodeIds`
  - [ ] 查询 Node 信息，根据 `is_course_root` 决定如何显示
- [ ] 验证 Roadmap 创建/更新
  - [ ] 检查所有 `nodeId` 存在且有效
  - [ ] 检查所有 Node 都有 `course_id`

### 任务 6：修改 CourseDomainService
**文件**：`CourseDomainService.java`

**改动**：
- [ ] 创建 Course 时，设置对应 Node 的 `is_course_root=1`
- [ ] 确保数据一致性

### 任务 7：修改 Controller 和测试
**文件列表**：
- [ ] `TocController.java`
  - [ ] API 路径：`/courses/{courseId}/toc` → `/nodes/{nodeId}/toc`
  - [ ] 请求参数：`courseId` → `nodeId`
- [ ] `RoadmapController.java`
  - [ ] 响应数据：nodes 列表包含 `is_course_root` 标识
- [ ] Request/Response VO 修改
- [ ] 所有相关测试文件更新

## 📊 整体进度

```
总进度: 15%
├─ [100%] 数据库迁移脚本
├─ [100%] Roadmap 数据迁移代码
├─ [100%] 通用 Spring 执行器
├─ [ 50%] SpringMethodRunner 调试
├─ [  0%] 重命名 UserCourseToc 类
├─ [  0%] 修改 NodeDO 和 Mapper
├─ [  0%] 修改 TocDomainService
├─ [  0%] 修改 RoadmapDomainService
├─ [  0%] 修改 CourseDomainService
└─ [  0%] 修改 Controller 和测试
```

## 🎯 下一步行动

1. **立即**：解决 SpringMethodRunner 的依赖问题
2. **然后**：执行 SQL 迁移
3. **然后**：执行 Roadmap 数据迁移
4. **然后**：开始后端代码重构（按任务 2-7 顺序）

## ⚠️ 注意事项

1. **数据备份**：执行迁移前必须备份数据库
2. **测试环境优先**：先在测试环境完整测试
3. **分阶段上线**：可以先完成数据迁移，后端代码分批上线
4. **向后兼容**：Roadmap 数据迁移支持新旧格式共存

## 📚 相关文档

- 架构文档：`backend/docs/ARCHITECTURE.md`
- 数据库文档：`docs/learn_database`
- 编程规范：`CLAUDE.md`

---

**最后更新**：2026-01-26
**预计完成**：2周（10-14个工作日）
