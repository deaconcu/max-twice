# REJECTED 状态迁移文档

## 概述
将 `ContentState` 枚举从 3 个状态扩展到 4 个状态，区分"审核不通过"和"违规封禁"。

## 状态变更

### 修改前
```java
SUBMITTED(0),  // 待审核
APPROVED(1),   // 已批准
BANNED(2)      // 封禁/拒绝（语义混乱）
```

### 修改后
```java
SUBMITTED(1),  // 待审核
APPROVED(2),   // 已批准
REJECTED(3),   // 审核不通过
BANNED(4)      // 违规封禁
```

## 语义说明
- **SUBMITTED**: 内容已提交，等待审核
- **APPROVED**: 审核通过，内容可见
- **REJECTED**: 审核不通过，用户可以修改后重新提交
- **BANNED**: 严重违规被封禁，永久不可见

## 实施计划

### ✅ 第一步：修改枚举定义
**文件**: `learn-common/src/main/java/com/prosper/learn/common/Enums.java`

**已完成内容**:
1. ✅ 修改 ContentState 枚举值从 0/1/2 改为 1/2/3/4
2. ✅ 添加 REJECTED(3) 状态
3. ✅ 添加静态常量用于 MyBatis 注解:
   ```java
   public static final byte SUBMITTED_VALUE = 1;
   public static final byte APPROVED_VALUE = 2;
   public static final byte REJECTED_VALUE = 3;
   public static final byte BANNED_VALUE = 4;
   ```
4. ✅ 为 UserProgressState 也添加了静态常量:
   ```java
   public static final byte NOT_STARTED_VALUE = 0;
   public static final byte IN_PROGRESS_VALUE = 1;
   public static final byte COMPLETED_VALUE = 2;
   ```

### ✅ 第二步：修改 Mapper 层
**目标**: 将所有硬编码的状态值改为使用枚举常量

**已完成文件**:
1. ✅ `CourseMapper.java` - 使用 APPROVED_VALUE, REJECTED_VALUE
2. ✅ `PostMapper.java` - 使用 APPROVED_VALUE
3. ✅ `CommentMapper.java` - 使用 APPROVED_VALUE
4. ✅ `ProfessionMapper.java` - 使用 APPROVED_VALUE
5. ✅ `RoadmapMapper.java` - 使用 SUBMITTED_VALUE, APPROVED_VALUE
6. ✅ `MemoryCardMapper.java` - 使用 SUBMITTED_VALUE (已在前序会话完成)
7. ✅ `UserCardSrsMapper.java` - 使用 APPROVED_VALUE
8. ✅ `UserCourseMapper.java` - 使用 APPROVED_VALUE, IN_PROGRESS_VALUE
9. ✅ `UserRoadmapMapper.java` - 使用 IN_PROGRESS_VALUE
10. ✅ `UserCardInCourseMapper.java` - 使用 APPROVED_VALUE

**关键变更**:
- 将 `APPROVED.value()` 改为 `APPROVED_VALUE`
- 将 `REJECTED.value()` 改为 `REJECTED_VALUE`
- 将 `IN_PROGRESS.value()` 改为 `IN_PROGRESS_VALUE`
- 将硬编码的 `1` 改为 `APPROVED_VALUE`

### ⏳ 第三步：修改 Service 层
**文件**: 待定

**待完成任务**:
1. ⏳ 修改审核逻辑，调用 `approve(false)` 时设置为 REJECTED
2. ⏳ 保留 ban 功能用于严重违规场景
3. ⏳ 确保所有使用 ContentState 的地方正确处理 REJECTED 状态

### ⏳ 第四步：修改 Controller 层
**文件**: 待定

**待完成任务**:
1. ⏳ 添加或修改管理员接口支持 REJECTED 状态
2. ⏳ 可能需要添加 ban 接口（如果当前没有）
3. ⏳ 更新 API 响应文档

### ⏳ 第五步：修改前端代码
**文件**: 待定

**待完成任务**:
1. ⏳ 添加 REJECTED(3) 状态常量
2. ⏳ 更新状态显示文本（如：审核不通过、违规封禁）
3. ⏳ 更新状态颜色/图标
4. ⏳ 处理 REJECTED 状态的用户交互（允许重新提交）

### ⏳ 第六步：数据库迁移
**文件**: 待创建迁移 SQL 脚本

**待完成任务**:
1. ⏳ 将现有 BANNED(2) 数据迁移到 REJECTED(3)
2. ⏳ 更新所有相关表的 state 字段
3. ⏳ 验证数据完整性

**迁移脚本示例**:
```sql
-- 将现有 state=2 的数据改为 state=3 (REJECTED)
UPDATE course SET state = 3 WHERE state = 2;
UPDATE post SET state = 3 WHERE state = 2;
UPDATE comment SET state = 3 WHERE state = 2;
UPDATE profession SET state = 3 WHERE state = 2;
UPDATE roadmap SET state = 3 WHERE state = 2;
UPDATE memory_card_deck SET state = 3 WHERE state = 2;
-- ... 其他相关表
```

## 关键决策记录

### 为什么使用静态常量而不是 .value() 方法？
**问题**: Java 注解要求属性值必须是编译时常量，而 `APPROVED.value()` 是方法调用，不是编译时常量。

**解决方案**: 在枚举中添加 `public static final` 常量：
```java
public static final byte APPROVED_VALUE = 2;
```

**原因**:
- 枚举是现代 Java 的最佳实践，提供类型安全
- MyBatis 注解的限制需要编译时常量
- 静态常量是折衷方案，兼顾类型安全和 MyBatis 兼容性

### 为什么保留 ban 功能？
虽然当前可能没有使用 ban 场景，但：
- 区分"审核不通过"和"违规封禁"在语义上更清晰
- 为将来的违规管理功能预留空间
- REJECTED 状态允许用户修改后重新提交
- BANNED 状态代表永久封禁，不可恢复

## 测试计划
待完成后补充测试用例。

## 风险评估
- **数据迁移风险**: 需要确保所有表的 state 字段都正确迁移
- **兼容性风险**: 前后端需要同步更新，避免状态不一致
- **业务逻辑风险**: 需要确认所有依赖 state 字段的业务逻辑都正确处理新状态

## 回滚计划
如果出现问题，可以：
1. 恢复数据库备份
2. 回滚代码到修改前版本
3. 将 REJECTED(3) 数据改回 BANNED(2)

---
**文档创建时间**: 2025-10-20
**最后更新时间**: 2025-10-20
**当前状态**: Mapper 层修改完成，Service/Controller/Frontend 待完成
