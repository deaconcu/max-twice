# 自动生成默认目录功能 - 设计文档 (v6 - Final Simplified)

## 1. 背景与目标

### 1.1. 背景

当前,当用户首次进入某个课程的 `read` 页面时,如果用户的 TOC (Table of Contents) 为空,页面将显示空白内容,用户需要手动构建目录。这导致用户体验不佳。

### 1.2. 目标

为了提升用户体验,我们希望实现一个"默认目录自动生成"功能:
- 当用户首次访问课程时,自动查询根节点下 **score 排序第一** 的 Post (必须是 contents 类型)
- 如果找到,将其添加为默认选中的内容
- 如果没找到或不是 contents 类型,创建空目录

## 2. TOC 机制理解

### 2.1. 现有架构

**涉及的表:**
- `user_course_toc`: 用户课程目录关系表,存储用户对每个课程的目录哈希列表
- `course_toc`: 课程目录内容表,存储实际的 TOC JSON 内容,使用引用计数

**数据结构:**
```
user_course_toc:
  id, user_id, course_id, toc (哈希列表: "hash1,hash2"), updated_at

course_toc:
  hash (SHA hash), toc (JSON内容), ref_count (引用计数)
```

**TOC JSON 格式示例:**
```json
{
  "123": {           // 根节点ID
    "456": {         // 子节点ID
      "^": [789],    // 固定帖子列表
      "+": 790       // 选中的帖子
    }
  }
}
```

### 2.2. 获取流程

1. `PageService.getToc()` 调用 `ContentsService.getToc(userId, courseId, create)`
2. 查询 `user_course_toc` 表获取用户的目录哈希列表
3. 如果不存在且 `create=true`,创建包含根节点的目录
4. 根据哈希列表从 `course_toc` 表获取实际内容
5. 返回 `ArrayNode` 格式的TOC

## 3. 核心逻辑

### 3.1. 触发时机

在 `ContentsService.getToc()` 方法中,当创建新的用户目录时 (`userCourseTocDO == null`):
1. 检查根节点下是否有 score 最高的 Post
2. 如果有且是 contents 类型,自动将其添加为选中的帖子
3. 如果没有或不是 contents 类型,创建空目录

### 3.2. 为什么不需要标识字段？

**关键点:** 创建 `user_course_toc` 记录本身就是一个天然的"已生成"标记

- **首次访问**: `userCourseTocDO == null` → 触发创建逻辑 → 自动生成
- **后续访问**: `userCourseTocDO != null` → 直接返回现有 TOC → 不会重复生成
- **用户修改**: 只更新 `toc` 字段 → 不会重新创建记录 → 不会被覆盖

因此,**无需额外的 `auto_generated` 字段**来防止重复生成。

### 3.3. 生成规则

1. 使用现有方法 `getListByNodeAndScore(nodeId, 1, PUBLISHED)` 查询
2. 取第一个 Post,检查其 `type` 字段
3. **只有 contents 类型才添加** (article 类型不能作为目录)
4. 将 Post ID 添加到根节点的 `"+"` 字段

### 3.4. 边界处理

- 如果根节点下没有任何 Post → 创建空目录 `{rootNodeId: {}}`
- 如果只有 article 类型的 Post → 创建空目录
- 用户可以随时修改或删除自动生成的内容 → 不会被覆盖

## 4. 方案实现

### 4.1. 无需数据库变更 ✅

**无需修改数据库表结构!**

现有的 `user_course_toc` 表结构完全满足需求。

### 4.2. 无需修改实体类 ✅

**无需修改 `UserCourseTocDO`!**

现有字段已足够。

### 4.3. 无需修改 Mapper ✅

**无需修改 `UserCourseTocMapper` 和 `PostMapper`!**

使用现有方法:
```java
// PostDataService 现有方法
List<PostDO> getListByNodeAndScore(long nodeId, int limit, byte state)
```

### 4.4. 业务逻辑修改 (唯一需要修改的地方)

在 `ContentsService.getToc()` 方法中修改创建逻辑:

```java
// Path: backend/learn-domain/src/main/java/com/prosper/learn/domain/service/basic/ContentsService.java

public ArrayNode getToc(long userId, long courseId, boolean create) {
    CourseDO courseDO = validateCourseExists(courseId);

    UserCourseTocDO userCourseTocDO = userCourseTocDataService.getByUserAndCourse(userId, courseId);
    String tocStr = "";

    // 如果用户目录不存在且不需要创建,直接返回null
    if (userCourseTocDO == null && !create) return null;

    if (userCourseTocDO == null) {
        // ========== 新增: 自动生成默认目录 ==========

        // 1. 创建根目录结构
        ObjectNode rootContent = objectMapper.createObjectNode();
        ObjectNode rootNodeContent = objectMapper.createObjectNode();

        // 2. 查找根节点下 score 最高的 Post
        List<PostDO> topPosts = postDataService.getListByNodeAndScore(
                courseDO.getRootNodeId(), 1, Enums.ContentState.PUBLISHED.value());

        // 3. 如果找到 contents 类型的 Post,添加为默认选中
        if (!topPosts.isEmpty()) {
            PostDO topPost = topPosts.get(0);

            // 只有 contents 类型才能作为目录
            if (topPost.getType() == Enums.PostType.contents.value()) {
                rootNodeContent.put("+", topPost.getId());

                log.info("自动生成默认TOC: userId={}, courseId={}, postId={}",
                        userId, courseId, topPost.getId());
            }
        }

        rootContent.set(Long.toString(courseDO.getRootNodeId()), rootNodeContent);
        // =========================================

        tocStr = rootContent.toString();
        String tosHash = Utils.hashSHA(tocStr);

        // 检查是否已存在相同内容的目录,避免重复存储
        CourseTocDO courseTocDO = courseTocDataService.get(tosHash);
        if (courseTocDO == null) {
            CourseTocDO newToc = new CourseTocDO();
            newToc.setHash(tosHash);
            newToc.setToc(tocStr);
            courseTocDataService.insert(newToc);
        }

        // 为用户创建目录记录,指向刚创建的目录版本
        userCourseTocDO = new UserCourseTocDO();
        userCourseTocDO.setUserId(userId);
        userCourseTocDO.setCourseId(courseId);
        userCourseTocDO.setToc(tosHash);

        userCourseTocDataService.insert(userCourseTocDO);
    } else {
        // 用户目录已存在,获取目录哈希列表
        tocStr = userCourseTocDO.getToc();
    }

    // ... 后续逻辑保持不变
    ArrayNode arrayNode = objectMapper.createArrayNode();
    String[] tocHashArr = tocStr.split(",");

    Map<String, CourseTocDO> map = courseTocDataService.getByHashes(tocHashArr);
    for(String tocHash: tocHashArr) {
        try {
            CourseTocDO courseTocDO = map.get(tocHash);
            if (courseTocDO == null) {
                throw ErrorCode.TOC_INDEX_OUT_OF_BOUNDS.exception();
            }
            arrayNode.add(objectMapper.readTree(courseTocDO.getToc()));
        } catch (JsonProcessingException e) {
            log.error("TOC解析失败", e);
            throw ErrorCode.JSON_PROCESSING_ERROR.exception(e);
        }
    }

    return arrayNode;
}
```

**修改要点:**
1. 在创建根目录结构前,先查询 score 最高的 Post
2. 检查 Post 类型,只有 contents 类型才添加
3. 使用 `rootNodeContent.put("+", topPost.getId())` 设置为选中
4. 其他逻辑完全不变

## 5. 执行流程

### 5.1. 首次访问课程

```
用户访问课程 Read 页面
    ↓
PageService.getToc(userId, courseId, true)
    ↓
ContentsService.getToc(userId, courseId, true)
    ↓
检查: userCourseTocDO == null (首次访问)
    ↓
查询根节点下 score 最高的 Post
    ↓
情况1: 找到 contents 类型的 Post
  → 创建 TOC: {rootNodeId: {"+": postId}}
  → 用户看到自动选中的内容
    ↓
情况2: 没找到或只有 article 类型
  → 创建空 TOC: {rootNodeId: {}}
  → 用户看到空目录
    ↓
保存到 user_course_toc 表
    ↓
返回 TOC 给前端
```

### 5.2. 后续访问

```
用户再次访问课程
    ↓
ContentsService.getToc(userId, courseId, true)
    ↓
检查: userCourseTocDO != null (已存在)
    ↓
直接返回现有的 TOC
    ↓
不执行创建逻辑,不会重复生成
```

### 5.3. 用户手动修改 TOC

```
用户删除或修改自动生成的内容
    ↓
只更新 user_course_toc.toc 字段
    ↓
记录仍然存在
    ↓
下次访问时 userCourseTocDO != null
    ↓
返回用户修改后的 TOC
    ↓
不会重新生成,尊重用户选择
```

## 6. TOC JSON 示例

### 6.1. 自动生成的 TOC (有默认 Post)

```json
[
  {
    "123": {        // 根节点ID
      "+": 456      // 自动选中的 Post ID (contents 类型)
    }
  }
]
```

前端效果: 自动展开并显示 Post 456 的内容

### 6.2. 空 TOC (无可用 Post)

```json
[
  {
    "123": {}       // 根节点ID,空内容
  }
]
```

前端效果: 显示空状态,等待用户手动添加内容

### 6.3. 用户自定义后的 TOC

```json
[
  {
    "123": {
      "456": {      // 用户展开的子节点
        "^": [789], // 用户添加的固定帖子
        "+": 790    // 用户选中的帖子
      },
      "+": 456      // 保留自动生成的选中
    }
  }
]
```

前端效果: 显示用户自定义的复杂目录结构

## 7. 优势

1. **✅ 零数据库变更**: 无需修改表结构
2. **✅ 零新方法**: 使用现有的 `getListByNodeAndScore` 方法
3. **✅ 单文件修改**: 仅需修改 `ContentsService.java`
4. **✅ 代码量极小**: 仅增加约 10 行代码
5. **✅ 对前端透明**: 前端无需任何修改
6. **✅ 自然防重复**: 记录本身就是标记,无需额外字段
7. **✅ 尊重用户**: 用户修改后不会被覆盖
8. **✅ 类型安全**: 只选择 contents 类型的 Post
9. **✅ 性能优良**: 只在首次创建时执行一次查询
10. **✅ 向后兼容**: 对已有用户无任何影响

## 8. 测试要点

### 8.1. 功能测试

- [ ] 测试根节点有 contents Post 时,能正确生成默认 TOC
- [ ] 测试根节点只有 article Post 时,生成空 TOC
- [ ] 测试根节点无 Post 时,生成空 TOC
- [ ] 测试首次访问后再次访问,不会重复生成
- [ ] 测试用户修改 TOC 后,下次访问保持用户的修改
- [ ] 测试用户删除自动生成的内容后,不会再次生成

### 8.2. 边界测试

- [ ] 测试根节点下有多个 contents Post,选择 score 最高的
- [ ] 测试 Post 的 score 相同时,按 id DESC 排序
- [ ] 测试课程未发布时的行为
- [ ] 测试多个用户访问同一课程,各自独立的 TOC

### 8.3. 性能测试

- [ ] 测试并发创建 TOC 时的数据一致性
- [ ] 测试 TOC 引用计数机制正常工作
- [ ] 测试哈希计算和去重逻辑

## 9. 实施清单

### 9.1. 代码修改 (1 个文件)

**文件:** `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/basic/ContentsService.java`

**修改位置:** `getToc()` 方法的创建逻辑部分 (line 194 附近)

**修改内容:** 在创建根目录结构时,添加查询和判断逻辑

**预计代码量:** +10 行

### 9.2. 测试

**单元测试:** 测试 `ContentsService.getToc()` 方法的各种场景

**集成测试:** 测试用户首次访问课程的完整流程

**手工测试:** 在开发环境验证前端显示效果

### 9.3. 发布步骤

1. 部署新代码
2. 对新用户进行功能验证
3. 监控日志,观察自动生成的触发情况
4. 收集用户反馈

**回滚方案:** 如有问题,直接回滚代码即可,无数据库变更

## 10. 代码 Diff 示例

```diff
  if (userCourseTocDO == null) {
-     // 创建根目录结构：包含课程根节点的空目录
      ObjectNode s = objectMapper.createObjectNode();
-     s.put(Long.toString(courseDO.getRootNodeId()), objectMapper.createObjectNode());
+     ObjectNode rootNodeContent = objectMapper.createObjectNode();
+
+     // 自动生成默认目录：查找 score 最高的 contents 类型 Post
+     List<PostDO> topPosts = postDataService.getListByNodeAndScore(
+             courseDO.getRootNodeId(), 1, Enums.ContentState.PUBLISHED.value());
+
+     if (!topPosts.isEmpty()) {
+         PostDO topPost = topPosts.get(0);
+         if (topPost.getType() == Enums.PostType.contents.value()) {
+             rootNodeContent.put("+", topPost.getId());
+             log.info("自动生成默认TOC: userId={}, courseId={}, postId={}",
+                     userId, courseId, topPost.getId());
+         }
+     }
+
+     s.put(Long.toString(courseDO.getRootNodeId()), rootNodeContent);

      tocStr = s.toString();
      // ... 后续逻辑不变
```

## 11. 后续优化方向

1. **智能推荐**: 根据用户历史学习数据推荐起始内容
2. **多维度排序**: 结合内容热度、完成度、评分等多维度
3. **A/B 测试**: 测试不同的默认内容选择策略
4. **用户偏好**: 在设置中允许用户配置默认行为
5. **批量迁移**: 为老用户提供"重置为推荐目录"功能

---

## 总结

这是一个**极简方案**:
- ✅ 无数据库变更
- ✅ 仅修改 1 个文件
- ✅ 仅增加约 10 行代码
- ✅ 对前端完全透明
- ✅ 自然防止重复生成

**实施成本最低,用户体验提升明显!** 🚀
