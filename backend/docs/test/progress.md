# 学习进度接口测试用例

> 参考测试编写规范：`docs/test/TEST_GUIDE.md`

---

## 测试准备

### 测试类基础配置
- MockMvc
- ObjectMapper
- UserDataService
- NodeDataService
- CourseDataService
- CourseDomainService
- RoadmapDataService
- RoadmapDomainService
- ProfessionDataService
- UserCourseService
- UserRoadmapService
- LearningProgressService
- RedisTemplate

### 必需的测试辅助方法
- createUser() - 创建测试用户
- createPublishedCourse() - 创建已发布课程
- createNode() - 创建课程节点
- createProfession() - 创建测试专业
- createPublishedRoadmap() - 创建已发布路线图
- startLearningCourse() - 用户开始学习课程
- startLearningRoadmap() - 用户开始学习路线图
- generateToken() - 生成测试 Token

### 说明
- @Transactional 自动回滚数据库操作
- Redis 数据需要手动清理（节点完成状态存储在 Redis）
- 所有测试数据使用动态生成的ID

---

## 节点进度测试

### 1. 标记节点完成 (POST /api/v1/progress/nodes/{nodeId}/complete)

#### 1.1 成功标记节点完成
- 验证返回 200 和 NodeProgressResponseDTO
- 验证 completed = true
- 验证 Redis 中保存了完成状态
- 验证课程进度自动更新

#### 1.2 重复标记节点完成 - 抛出异常
- 验证返回错误码 1609 (NODE_ALREADY_COMPLETED)

#### 1.3 节点ID验证
- 负数ID返回 400
- 0返回 400

#### 1.4 节点不存在
- 验证返回相应错误码

#### 1.5 课程不存在
- 验证返回相应错误码

#### 1.6 请求体缺少 courseId
- 验证返回 400

---

### 2. 取消节点完成 (DELETE /api/v1/progress/nodes/{nodeId}/complete)

#### 2.1 成功取消节点完成
- 验证返回 200 和 NodeProgressResponseDTO
- 验证 completed = false
- 验证 Redis 中删除了完成状态
- 验证课程进度自动更新

#### 2.2 取消未完成的节点 - 抛出异常
- 验证返回错误码 1610 (NODE_ALREADY_NOT_COMPLETED)

#### 2.3 节点ID验证
- 负数ID和0返回 400

#### 2.4 请求体缺少 courseId
- 验证返回 400

---

### 3. 检查节点完成状态 (GET /api/v1/progress/nodes/{nodeId}/status)

#### 3.1 查询已完成节点
- 验证返回 NodeProgressResponseDTO
- 验证 completed = true

#### 3.2 查询未完成节点
- 验证 completed = false

#### 3.3 节点不存在
- 验证返回相应错误码

#### 3.4 节点ID验证
- 负数ID和0返回 400

---

## 课程进度测试

### 4. 开始学习课程 (POST /api/v1/progress/courses/{courseId}/start)

#### 4.1 成功开始学习
- 验证返回 200 和 CourseProgressResponseDTO
- 验证 learning = true
- 验证数据库创建了学习记录

#### 4.2 重复开始学习 - 抛出异常
- 验证返回错误码 1611 (USER_COURSE_ALREADY_STARTED)

#### 4.3 课程ID验证
- 负数ID和0返回 400

#### 4.4 课程不存在
- 验证返回相应错误码

---

### 5. 取消学习课程 (DELETE /api/v1/progress/courses/{courseId}/start)

#### 5.1 成功取消学习
- 验证返回 200 和 CourseProgressResponseDTO
- 验证 learning = false
- 验证数据库删除了学习记录

#### 5.2 取消未开始的学习 - 抛出异常
- 验证返回错误码 1612 (USER_COURSE_NOT_STARTED)

---

### 6. 获取课程进度 (GET /api/v1/progress/courses/{courseId})

#### 6.1 获取正在学习的课程
- 验证返回 UserCourseWithCourseDTO
- 验证包含课程简要信息 (CourseBriefDTO: id, name)
- 验证进度信息完整

#### 6.2 学习记录不存在
- 验证返回错误码 1601

#### 6.3 课程ID验证
- 负数ID和0返回 400

---

### 7. 获取所有课程进度 (GET /api/v1/progress/courses)

#### 7.1 获取多个课程进度
- 验证返回数组
- 验证每个元素包含 UserCourseWithCourseDTO 结构
- 验证 course 只包含 id 和 name

#### 7.2 没有学习任何课程
- 验证返回空数组

#### 7.3 分页功能 (lastId 参数)
- 验证 lastId = 0 返回所有记录
- 验证 lastId > 0 返回后续记录

#### 7.4 批量查询优化验证
- 验证避免 N+1 查询问题

---

### 8. 更新课程进度 (PUT /api/v1/progress/courses/{courseId})

#### 8.1 成功更新进度
- 验证返回 200 和 UserCourseWithCourseDTO
- 验证进度已更新

#### 8.2 进度达到100% - 自动标记完成
- 验证状态变为 COMPLETED
- 验证 completedAt 字段已设置

#### 8.3 学习记录不存在
- 验证返回错误码 1601

#### 8.4 课程不存在
- 验证返回相应错误码

#### 8.5 进度值验证
- 负数返回 400
- 超过100返回 400
- 缺少 progressPercent 字段返回 400

---

### 9. 删除课程进度 (DELETE /api/v1/progress/courses/{courseId})

#### 9.1 成功删除进度
- 验证返回 200 和 CourseProgressResponseDTO
- 验证 learning = false
- 验证数据库删除了学习记录

#### 9.2 课程ID验证
- 负数ID和0返回 400

---

### 10. 标记课程完成 (POST /api/v1/progress/courses/{courseId}/complete)

#### 10.1 成功标记课程完成
- 验证返回 200 和 CourseCompletionResponseDTO
- 验证 completed = true
- 验证状态变为 COMPLETED
- 验证 completedAt 已设置

#### 10.2 课程ID验证
- 负数ID和0返回 400

---

## 路线图进度测试

### 11. 开始学习路线图 (POST /api/v1/progress/roadmaps/{roadmapId}/start)

#### 11.1 成功开始学习
- 验证返回 200 和 RoadmapProgressResponseDTO
- 验证 learning = true
- 验证数据库创建了学习记录
- 验证初始进度为0，状态为 IN_PROGRESS

#### 11.2 重复开始学习 - 抛出异常
- 验证返回错误码 1613 (USER_ROADMAP_ALREADY_STARTED)

#### 11.3 路线图ID验证
- 负数ID和0返回 400

#### 11.4 路线图不存在
- 验证返回相应错误码

---

### 12. 取消学习路线图 (DELETE /api/v1/progress/roadmaps/{roadmapId}/start)

#### 12.1 成功取消学习
- 验证返回 200 和 RoadmapProgressResponseDTO
- 验证 learning = false
- 验证数据库删除了学习记录

#### 12.2 取消未开始的学习 - 抛出异常
- 验证返回错误码 1614 (USER_ROADMAP_NOT_STARTED)

---

### 13. 获取路线图进度 (GET /api/v1/progress/roadmaps/{roadmapId})

#### 13.1 获取正在学习的路线图
- 验证返回 UserRoadmapWithDetailDTO
- 验证包含路线图详细信息

#### 13.2 学习记录不存在
- 验证返回错误码 1601

#### 13.3 路线图ID验证
- 负数ID和0返回 400

---

### 14. 获取所有路线图进度 (GET /api/v1/progress/roadmaps)

#### 14.1 获取多个路线图进度
- 验证返回数组
- 验证每个元素包含 UserRoadmapWithBriefDTO 结构

#### 14.2 没有学习任何路线图
- 验证返回空数组

#### 14.3 验证返回的路线图简要信息格式
- roadmap 只包含: id, professionName, nodeCount
- roadmap 不包含: content, description, creator 等详细信息
- 验证 nodeCount 从数据库字段直接读取（不解析 JSON）

#### 14.4 批量查询优化验证
- 验证批量查询路线图
- 验证批量查询专业
- 验证避免 N+1 查询问题

---

### 15. 更新路线图进度 (PUT /api/v1/progress/roadmaps/{roadmapId})

#### 15.1 成功更新进度
- 验证返回 200 和 UserRoadmapSummaryDTO
- 验证进度已更新

#### 15.2 进度达到100% - 自动标记完成
- 验证状态变为 COMPLETED
- 验证 completedAt 字段已设置

#### 15.3 学习记录不存在
- 验证返回错误码 1601

#### 15.4 进度值验证
- 负数返回 400
- 超过100返回 400
- 缺少 progressPercent 字段返回 400

---

## 参数验证测试

### 16. 通用参数验证

#### 16.1 ID参数验证
- 所有需要 ID 的接口
- 负数、0、null 都应返回 400

#### 16.2 请求体字段验证
- 缺少必填字段返回 400
- 字段值为 null 返回 400
- 字段类型错误返回 400

#### 16.3 未登录访问
- 所有需要登录的接口返回 401

---

## 业务逻辑测试

### 17. 节点完成与课程进度联动

#### 17.1 完成节点自动更新课程进度
- 标记节点完成
- 验证课程进度自动计算并更新

#### 17.2 取消节点完成自动更新课程进度
- 取消节点完成
- 验证课程进度自动重新计算

---

### 18. 状态转换测试

#### 18.1 课程进度状态转换
- 开始学习: 进度0，状态 IN_PROGRESS
- 更新到50%: 状态保持 IN_PROGRESS
- 更新到100%: 状态变为 COMPLETED

#### 18.2 路线图进度状态转换
- 开始学习: 进度0，状态 IN_PROGRESS
- 更新到50%: 状态保持 IN_PROGRESS
- 更新到100%: 状态变为 COMPLETED

#### 18.3 completedAt 时间戳验证
- 首次完成时设置
- 再次更新不改变 completedAt

---

## 并发测试

### 19. 并发操作测试

#### 19.1 并发标记节点完成
- 多个线程同时标记同一节点
- 验证只有一个成功，其余返回 1609

#### 19.2 并发开始学习课程
- 多个线程同时开始学习同一课程
- 验证只有一个成功，其余返回 1611

#### 19.3 并发开始学习路线图
- 多个线程同时开始学习同一路线图
- 验证只有一个成功，其余返回 1613

---

## 边界测试

### 20. 边界场景

#### 20.1 进度边界值测试
- 进度 = 0: 正常更新
- 进度 = 100: 状态变为 COMPLETED
- 进度 = 1: 状态变为 IN_PROGRESS

#### 20.2 大量数据场景
- 用户完成100个节点
- 用户学习50个课程
- 用户学习20个路线图
- 验证查询性能和数据完整性

#### 20.3 空数据场景
- 查询进度列表返回空数组
- 不会抛出异常

---

## 集成测试

### 21. 跨域数据验证

#### 21.1 节点被删除后的查询
- 创建节点完成记录
- 删除节点
- 查询节点状态时应处理节点不存在的情况

#### 21.2 课程被删除后的查询
- 创建学习记录
- 删除课程
- 查询进度时应处理课程不存在的情况

#### 21.3 路线图被删除后的查询
- 创建学习记录
- 删除路线图
- 查询进度时应处理路线图不存在的情况

---

## 性能测试

### 22. 批量查询性能

#### 22.1 获取所有课程进度 - 避免 N+1
- 验证批量查询课程列表
- 验证不会对每个课程单独查询

#### 22.2 获取所有路线图进度 - 避免 N+1
- 验证批量查询路线图列表
- 验证批量查询专业列表
- 验证 nodeCount 直接从字段读取

#### 22.3 Redis 缓存性能
- 节点完成状态从 Redis 读取
- 验证响应时间

---

## Redis 数据一致性测试

### 23. Redis 与数据库一致性

#### 23.1 节点完成状态同步
- 标记节点完成后 Redis 和数据库都应更新
- 取消节点完成后 Redis 和数据库都应删除

#### 23.2 Redis 数据清理
- 测试结束后手动清理 Redis 数据
- 验证不影响其他测试

---
