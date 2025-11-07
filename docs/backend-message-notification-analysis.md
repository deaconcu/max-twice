# 后台操作消息通知完整分析报告

> 文档创建时间: 2025-11-01
> 分析范围: 所有后台业务操作的用户消息发送情况
> 代码版本: 当前主分支

---

## 📊 整体概览

本文档详细分析了后台系统中所有业务操作的消息通知机制。经过全面检查，系统共有 **26 个发送用户消息的操作**，涵盖互动通知和审核通知两大类。

### 消息分类体系

系统使用 `category` 字段将消息分为三类：

| Category | 类型 | 说明 |
|----------|------|------|
| 1 | 互动消息 | 评论、点赞、关注等用户互动行为 |
| 2 | 系统消息 | 审核结果通知（批准、拒绝、封禁） |
| 3 | 私信 | 用户之间的私信 |

---

## ✅ 会发送用户消息的操作 (26个)

### 1️⃣ 互动消息 (7个操作)

#### 评论通知 (1个操作，多种场景)

**操作**: `CommentService.createComment()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/CommentService.java:154`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 用户评论帖子 | postComment | 帖子作者 | commenterId, nodeId, commentId |
| 用户评论节点 | nodeComment | 节点作者 | commenterId, nodeId, commentId |
| 用户评论路线图 | roadmapComment | 路线图作者 | commenterId, nodeId, commentId |
| 回复帖子评论 | replyPostingComment | 被回复者 | commenterId, nodeId, commentId |
| 回复节点评论 | replyNodeComment | 被回复者 | commenterId, nodeId, commentId |
| 回复路线图评论 | replyRoadmapComment | 被回复者 | commenterId, nodeId, commentId |

**实现方法**:
- `messageService.createCommentMessage()` (行224-232)
- `messageService.createCommentMessage()` (行241-249，回复场景)

---

#### 关注通知 (2个操作)

**操作1**: `FollowService.follow()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/FollowService.java:154`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 用户关注另一用户 | follow | 被关注者 | followerId |

**实现方法**: `messageService.createFollowMessage()` (行167)

---

**操作2**: `UserService.follow()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/UserService.java:405`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 用户关注另一用户 | follow | 被关注者 | followerId |

**实现方法**: `messageService.createFollowMessage()` (行405)

**说明**: 系统有两个关注入口，都会发送通知

---

#### 点赞通知 (2个操作)

**操作1**: `UpvoteService.upvotePost()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/UpvoteService.java:217`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 用户点赞帖子 (twice) | upvote | 帖子作者 | voterId, nodeId, postingId, type=2 |
| 用户点赞帖子 (helpful) | upvote | 帖子作者 | voterId, nodeId, postingId, type=3 |

**实现方法**: `messageService.createUpvoteMessage()` (行200)

**注意**:
- 只在新增点赞和切换点赞类型时发送
- 取消点赞不发送消息

---

**操作2**: `UpvoteService.upvoteComment()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/UpvoteService.java:307`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 用户点赞评论 | upvote | 评论作者 | voterId, nodeId, commentId, type=1 |

**实现方法**: `messageService.createUpvoteMessage()` (行337)

**注意**: 取消点赞不发送消息

---

### 2️⃣ 审核通知 - 批准 (2个操作)

#### 课程批准

**操作**: `CourseService.approve()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/CourseService.java:301`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员批准课程 | courseApproved | 课程创建者 | courseId, courseName, linkUrl |

**实现方法**: `messageService.sendCourseModeration(..., APPROVED, null)` (行309)

---

#### 职业批准

**操作**: `ProfessionService.approve()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/ProfessionService.java:150`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员批准职业 | professionApproved | 职业创建者 | professionId, professionName, linkUrl |

**实现方法**: `messageService.sendProfessionModeration(..., APPROVED, null)` (行150)

---

### 3️⃣ 审核通知 - 拒绝 (7个操作)

#### 课程拒绝

**操作**: `CourseService.reject()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/CourseService.java:318`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员拒绝课程 | courseRejected | 课程创建者 | courseId, courseName, reason |

**实现方法**: `messageService.sendCourseModeration(..., REJECTED, reason)` (行326)

---

#### 帖子拒绝

**操作**: `PostService.reject()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/PostService.java:586`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员拒绝帖子 | postRejected | 帖子作者 | postId, postPreview, nodeId, nodeName, courseName, reason |

**实现方法**: `messageService.sendPostModeration(..., REJECTED, reason)` (行605)

---

#### 评论拒绝

**操作**: `CommentService.reject()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/CommentService.java:456`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员拒绝评论 | commentRejected | 评论作者 | commentId, commentPreview, objectType, objectId, objectTitle, reason |

**实现方法**: `messageService.sendCommentModeration(..., REJECTED, reason)` (行500)

---

#### 职业拒绝

**操作**: `ProfessionService.reject()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/ProfessionService.java:178`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员拒绝职业 | professionRejected | 职业创建者 | professionId, professionName, reason |

**实现方法**: `messageService.sendProfessionModeration(..., REJECTED, reason)` (行178)

---

#### 路线图拒绝

**操作**: `RoadmapService.reject()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/RoadmapService.java:796`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员拒绝路线图 | roadmapRejected | 路线图创建者 | roadmapId, professionId, professionName, reason |

**实现方法**: `messageService.sendRoadmapModeration(..., REJECTED, reason)` (行796)

---

#### 节点拒绝

**操作**: `NodeService.reject()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/NodeService.java:160`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员拒绝节点 | nodeRejected | 节点创建者 | nodeId, nodeName, courseId, courseName, reason |

**实现方法**: `messageService.sendNodeModeration(..., REJECTED, reason)` (行160)

---

#### 记忆卡片组拒绝

**操作**: `MemoryCardDeckService.reject()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/MemoryCardDeckService.java:605`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员拒绝卡片组 | memoryDeckRejected | 卡片组创建者 | deckId, deckTitle, postId, postTitle, reason |

**实现方法**: `messageService.sendMemoryDeckModeration(..., REJECTED, reason)` (行605)

---

### 4️⃣ 审核通知 - 封禁 (7个操作)

#### 课程封禁

**操作**: `CourseService.ban()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/CourseService.java:335`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员封禁课程 | courseBanned | 课程创建者 | courseId, courseName, reason |

**实现方法**: `messageService.sendCourseModeration(..., BANNED, reason)` (行343)

---

#### 帖子封禁

**操作**: `PostService.ban()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/PostService.java:631`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员封禁帖子 | postBanned | 帖子作者 | postId, postPreview, nodeId, nodeName, courseName, reason |

**实现方法**: `messageService.sendPostModeration(..., BANNED, reason)` (行650)

---

#### 评论封禁

**操作**: `CommentService.ban()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/CommentService.java:516`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员封禁评论 | commentBanned | 评论作者 | commentId, commentPreview, objectType, objectId, objectTitle, reason |

**实现方法**: `messageService.sendCommentModeration(..., BANNED, reason)` (行560)

---

#### 职业封禁

**操作**: `ProfessionService.ban()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/ProfessionService.java:206`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员封禁职业 | professionBanned | 职业创建者 | professionId, professionName, reason |

**实现方法**: `messageService.sendProfessionModeration(..., BANNED, reason)` (行206)

---

#### 路线图封禁

**操作**: `RoadmapService.ban()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/RoadmapService.java:828`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员封禁路线图 | roadmapBanned | 路线图创建者 | roadmapId, professionId, professionName, reason |

**实现方法**: `messageService.sendRoadmapModeration(..., BANNED, reason)` (行828)

---

#### 节点封禁

**操作**: `NodeService.ban()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/NodeService.java:189`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员封禁节点 | nodeBanned | 节点创建者 | nodeId, nodeName, courseId, courseName, reason |

**实现方法**: `messageService.sendNodeModeration(..., BANNED, reason)` (行189)

---

#### 记忆卡片组封禁

**操作**: `MemoryCardDeckService.ban()`
**代码位置**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/MemoryCardDeckService.java:643`

| 触发场景 | 消息类型 | 接收者 | 消息内容 |
|---------|---------|-------|---------|
| 管理员封禁卡片组 | memoryDeckBanned | 卡片组创建者 | deckId, deckTitle, postId, postTitle, reason |

**实现方法**: `messageService.sendMemoryDeckModeration(..., BANNED, reason)` (行643)

---

## ❌ 不发送用户消息的操作

### 1️⃣ 审核批准(部分内容类型)

以下内容类型的批准操作**不发送**通知消息：

| 操作 | 代码位置 | 说明 |
|------|---------|------|
| **批准帖子** | `PostService.approve()` (行569) | 无通知 |
| **批准评论** | `CommentService.approve()` (行437) | 无通知 |
| **批准节点** | 无对应操作 | 节点创建后直接发布 |
| **批准路线图** | 无对应操作 | 只有拒绝/封禁 |
| **批准卡片组** | 无对应操作 | 只有拒绝/封禁 |

**对比**: 课程和职业的批准**会**发送通知，但帖子、评论等不发送。

---

### 2️⃣ 取消操作

所有取消操作都**不发送**通知消息，符合常规产品设计：

| 操作 | 代码位置 | 说明 |
|------|---------|------|
| 取消点赞帖子 | `UpvoteService.upvotePost()` (行227-242) | 检测到相同类型点赞时取消 |
| 取消点赞评论 | `UpvoteService.upvoteComment()` (行314-322) | 检测到已点赞时取消 |
| 取消路线图投票 | `UpvoteService.upvoteRoadmap()` (行361-370) | 检测到已投票时取消 |
| 取消卡片组点赞 | `UpvoteService.upvoteMemoryCardDeck()` (行438-449) | 检测到已点赞时取消 |
| 取消关注 | `FollowService.unfollow()` (行181) | 删除关注关系 |

**设计原则**: 取消操作通常不需要通知对方，避免过度打扰。

---

### 3️⃣ 内容创建操作

用户创建内容时**不发送**通知消息：

| 操作 | 代码位置 | 说明 |
|------|---------|------|
| 创建帖子 | `PostService.createPost()` (行392) | 创建后进入待审核状态 |
| 创建课程 | `CourseService.createCourse()` (行361) | 创建后进入待审核状态 |
| 创建子课程 | `CourseService.createSubcourse()` (行386) | 创建后进入待审核状态 |
| 创建节点 | 各Service中 | 创建时不通知 |
| 创建路线图 | `RoadmapService` | 创建时不通知 |
| 创建卡片组 | `MemoryCardDeckService` | 创建时不通知 |

**设计原则**: 内容创建是用户主动操作，不需要通知自己。审核结果会通过审核通知告知用户。

---

### 4️⃣ 内容更新/删除操作

用户更新或删除自己的内容时**不发送**通知消息：

| 操作 | 代码位置 | 说明 |
|------|---------|------|
| 更新帖子 | `PostService.updatePost()` (行479) | 更新后重新进入待审核状态 |
| 更新课程 | `CourseService.updateCourse()` (行224) | 仅更新基本信息 |
| 删除帖子 | `PostService.deletePost()` (行528) | 软删除 |
| 删除课程 | `CourseService.delete()` (行352) | 软删除 |

**设计原则**: 用户自己的操作不需要通知自己。

---

## 🔍 发现的问题与不一致性

### ⚠️ 问题1: 审核批准通知不一致

不同内容类型的审核批准通知存在不一致：

| 内容类型 | 批准通知 | 拒绝通知 | 封禁通知 | 一致性评分 |
|---------|---------|---------|---------|-----------|
| **课程** Course | ✅ 有 | ✅ 有 | ✅ 有 | ⭐⭐⭐ 完整 |
| **职业** Profession | ✅ 有 | ✅ 有 | ✅ 有 | ⭐⭐⭐ 完整 |
| **帖子** Post | ❌ **缺失** | ✅ 有 | ✅ 有 | ⭐⭐ 不完整 |
| **评论** Comment | ❌ **缺失** | ✅ 有 | ✅ 有 | ⭐⭐ 不完整 |
| **路线图** Roadmap | ❌ 无操作 | ✅ 有 | ✅ 有 | ⭐⭐ 不完整 |
| **节点** Node | ❌ 无操作 | ✅ 有 | ✅ 有 | ⭐⭐ 不完整 |
| **卡片组** Deck | ❌ 无操作 | ✅ 有 | ✅ 有 | ⭐⭐ 不完整 |

**影响**:
1. 用户体验不一致：用户提交帖子/评论后，只能在被拒绝/封禁时收到通知，批准时无通知
2. 用户需要主动刷新页面才能发现内容已批准
3. 降低用户对审核结果的感知

---

### ⚠️ 问题2: 路线图/节点/卡片组无批准操作

部分内容类型只有拒绝和封禁操作，没有显式的批准操作：

- **路线图** (Roadmap): 创建后直接发布，无待审核流程
- **节点** (Node): 创建后直接发布，无待审核流程
- **卡片组** (MemoryCardDeck): 创建后直接发布，无待审核流程

**可能的原因**:
- 这些内容类型可能采用"先发布后审核"的模式
- 或者审核流程未完全实现

---

## 💡 改进建议

### 建议1: 统一审核通知策略 ⭐⭐⭐

**目标**: 为所有内容类型提供完整的审核通知（批准、拒绝、封禁）

#### 需要修改的位置

1️⃣ **PostService.approve()** - 添加批准通知

```java
// 文件: backend/learn-domain/.../PostService.java
// 行号: 569

@Transactional
public void approve(Long id, UserDO currentUser) {
    PostDO postDO = validateAndGetPost(id);
    postDO.setState(Enums.ContentState.PUBLISHED.value());
    postDO.setReason(null);
    postDataService.update(postDO);

    // 🔴 添加: 发送批准通知
    NodeDO nodeDO = nodeDataService.getById(postDO.getNodeId());
    CourseDO courseDO = nodeDO != null ? courseDataService.getById(nodeDO.getCourseId()) : null;

    if (nodeDO != null && courseDO != null) {
        String contentPreview = com.prosper.learn.domain.util.Util.stripFormatting(postDO.getContent());
        if (contentPreview != null && contentPreview.length() > 50) {
            contentPreview = contentPreview.substring(0, 50) + "...";
        }

        messageService.sendPostModeration(
            postDO.getCreatorId(),
            postDO.getId(),
            contentPreview,
            nodeDO.getId(),
            nodeDO.getName(),
            courseDO.getName(),
            Enums.ModerationAction.APPROVED,  // 🔴 使用 APPROVED
            null
        );
    }

    log.info("审核员 {} 批准了帖子 {}", currentUser.getId(), id);
}
```

---

2️⃣ **CommentService.approve()** - 添加批准通知

```java
// 文件: backend/learn-domain/.../CommentService.java
// 行号: 437

@Transactional
public void approve(Long id, UserDO operator) {
    validateCommentId(id);
    CommentDO commentDO = validateAndGetComment(id);
    int oldState = commentDO.getState();

    if (oldState != Enums.ContentState.PUBLISHED.value()) {
        commentDO.setState(Enums.ContentState.PUBLISHED.value());
        commentDO.setReason(null);
        commentDataService.update(commentDO);

        // 批准评论，评论数+1
        updateObjectCommentCount(commentDO, 1);

        // 🔴 添加: 发送批准通知
        String objectType = "node";
        if (commentDO.getObjectType() == Enums.ContentType.post.value()) {
            objectType = "post";
        } else if (commentDO.getObjectType() == Enums.ContentType.roadmap.value()) {
            objectType = "roadmap";
        }

        String objectTitle = "";
        if (commentDO.getObjectType() == Enums.ContentType.post.value()) {
            PostDO postDO = postDataService.getById(commentDO.getObjectId());
            if (postDO != null && postDO.getContent() != null) {
                objectTitle = com.prosper.learn.domain.util.Util.stripFormatting(postDO.getContent());
                if (objectTitle.length() > 50) {
                    objectTitle = objectTitle.substring(0, 50) + "...";
                }
            }
        } else if (commentDO.getObjectType() == Enums.ContentType.node.value()) {
            NodeDO nodeDO = nodeDataService.getById(commentDO.getObjectId());
            if (nodeDO != null) objectTitle = nodeDO.getName();
        } else if (commentDO.getObjectType() == Enums.ContentType.roadmap.value()) {
            objectTitle = "路线图";
        }

        String preview = commentDO.getContent();
        if (preview != null && preview.length() > 50) {
            preview = preview.substring(0, 50) + "...";
        }

        messageService.sendCommentModeration(
            commentDO.getCreatorId(),
            commentDO.getId(),
            preview,
            objectType,
            commentDO.getObjectId(),
            objectTitle,
            Enums.ModerationAction.APPROVED,  // 🔴 使用 APPROVED
            null
        );
    }
}
```

---

3️⃣ **MessageService** - 确认支持 APPROVED 枚举

检查 `MessageService.sendPostModeration()` 和 `MessageService.sendCommentModeration()` 是否已经支持 `ModerationAction.APPROVED`。

根据之前的代码分析：
- `sendPostModeration()` 目前只处理 REJECTED 和 BANNED
- `sendCommentModeration()` 目前只处理 REJECTED 和 BANNED

**需要修改**: 添加对 APPROVED 的支持

```java
// 文件: backend/learn-domain/.../MessageService.java
// 方法: sendPostModeration (行568)

public void sendPostModeration(long userId, long postId, String postPreview,
                               long nodeId, String nodeName, String courseName,
                               Enums.ModerationAction action, String reason) {
    Map<String, Object> data = new HashMap<>();
    data.put("postId", postId);
    data.put("postPreview", Util.stripFormatting(postPreview));
    data.put("nodeId", nodeId);
    data.put("nodeName", nodeName);
    data.put("courseName", courseName);
    data.put("reason", reason != null ? reason : "");

    // 🔴 修改: 添加 APPROVED 分支
    int type = switch (action) {
        case APPROVED -> {
            data.put("linkUrl", "/node/" + nodeId);  // 🔴 添加跳转链接
            yield MessageType.postApproved.value();   // 🔴 需要新增此消息类型
        }
        case REJECTED -> {
            data.put("linkUrl", "/self?tab=posts");
            yield MessageType.postRejected.value();
        }
        case BANNED -> {
            data.put("linkUrl", "/self?tab=posts");
            yield MessageType.postBanned.value();
        }
        default -> throw ErrorCode.INVALID_PARAMETER.exception("无效的帖子审核操作");
    };

    createSystemMessage(type, userId, Util.toJson(data));
}
```

类似地修改 `sendCommentModeration()` 方法。

---

4️⃣ **Enums.MessageType** - 添加新的消息类型

需要在枚举中添加：
- `postApproved`
- `commentApproved`

```java
// 文件: backend/learn-common/.../Enums.java

public enum MessageType {
    // ... 现有类型 ...

    // 🔴 新增: 帖子批准
    postApproved(21, 2),        // category=2 系统消息

    // 🔴 新增: 评论批准
    commentApproved(22, 2),     // category=2 系统消息

    // ... 其他类型 ...
}
```

---

### 建议2: 完善路线图/节点/卡片组的审核流程 ⭐⭐

**目标**: 统一所有UGC内容的审核流程

#### 分析当前状态

| 内容类型 | 创建后状态 | 是否需要审核 | 建议 |
|---------|-----------|------------|------|
| 路线图 | 直接发布? | ❓ 待确认 | 如需审核，添加批准操作 |
| 节点 | 直接发布? | ❓ 待确认 | 如需审核，添加批准操作 |
| 卡片组 | 直接发布? | ❓ 待确认 | 如需审核，添加批准操作 |

**行动项**:
1. 确认产品设计：这些内容是否需要审核
2. 如果需要审核，参考课程/职业的实现方式添加完整的审核流程
3. 如果不需要审核，在代码注释中明确说明原因

---

### 建议3: 添加代码文档和注释 ⭐

**目标**: 让开发者清楚了解每个操作的通知行为

#### 建议的注释格式

```java
/**
 * 批准帖子
 *
 * 审核流程:
 * 1. 验证帖子存在且为待审核状态
 * 2. 更新状态为已发布
 * 3. 发送批准通知给帖子作者 ✉️
 *
 * @param id 帖子ID
 * @param currentUser 当前审核员
 * @throws BusinessException 当帖子不存在或状态不正确时
 */
@Transactional
public void approve(Long id, UserDO currentUser) {
    // ...
}
```

在每个Service类的类级别注释中，添加通知行为说明：

```java
/**
 * 帖子服务
 *
 * 消息通知:
 * - ✅ 拒绝帖子 → 通知作者 (postRejected)
 * - ✅ 封禁帖子 → 通知作者 (postBanned)
 * - ❌ 批准帖子 → 无通知 (TODO: 需要添加)
 * - ❌ 创建帖子 → 无通知
 * - ❌ 更新帖子 → 无通知
 * - ❌ 删除帖子 → 无通知
 *
 * @since 2024-01-20
 */
@Service
public class PostService {
    // ...
}
```

---

### 建议4: 添加单元测试 ⭐⭐

**目标**: 确保消息通知的正确性

#### 测试覆盖范围

为每个发送消息的操作添加测试：

```java
@Test
public void testApprovePost_ShouldSendNotification() {
    // Given
    PostDO post = createTestPost(ContentState.SUBMITTED);
    UserDO admin = createAdminUser();

    // When
    postService.approve(post.getId(), admin);

    // Then
    // 验证消息是否发送
    verify(messageService, times(1)).sendPostModeration(
        eq(post.getCreatorId()),
        eq(post.getId()),
        anyString(),
        anyLong(),
        anyString(),
        anyString(),
        eq(ModerationAction.APPROVED),
        isNull()
    );

    // 验证帖子状态已更新
    PostDO updatedPost = postDataService.getById(post.getId());
    assertEquals(ContentState.PUBLISHED.value(), updatedPost.getState());
}
```

---

## 📈 优先级建议

根据用户体验影响和实现难度，建议按以下优先级进行改进：

| 优先级 | 任务 | 影响 | 难度 | 工作量 |
|-------|------|------|------|--------|
| 🔴 P0 | 帖子批准添加通知 | 高 | 中 | 2h |
| 🔴 P0 | 评论批准添加通知 | 高 | 中 | 2h |
| 🟡 P1 | 添加单元测试 | 中 | 低 | 4h |
| 🟡 P1 | 添加代码注释 | 中 | 低 | 2h |
| 🟢 P2 | 确认路线图/节点/卡片组审核流程 | 低 | 中 | 调研1h |
| 🟢 P2 | 完善其他内容类型审核流程 | 低 | 高 | 8h |

**总工作量估算**: P0任务 4小时，P1任务 6小时，P2任务 9小时

---

## 🎯 实施计划

### 阶段1: 快速修复 (P0任务, 1天)

**目标**: 修复帖子和评论批准通知缺失的问题

1. ✅ 在 `MessageType` 枚举中添加 `postApproved` 和 `commentApproved`
2. ✅ 修改 `MessageService.sendPostModeration()` 支持 APPROVED
3. ✅ 修改 `MessageService.sendCommentModeration()` 支持 APPROVED
4. ✅ 在 `PostService.approve()` 中调用通知方法
5. ✅ 在 `CommentService.approve()` 中调用通知方法
6. ✅ 手动测试验证

---

### 阶段2: 质量保障 (P1任务, 1天)

**目标**: 添加测试和文档

1. ✅ 为新增的批准通知添加单元测试
2. ✅ 为所有Service添加通知行为注释
3. ✅ 更新本文档，记录修改后的状态
4. ✅ Code Review

---

### 阶段3: 长期优化 (P2任务, 2天)

**目标**: 统一审核流程

1. ✅ 与产品确认路线图/节点/卡片组的审核需求
2. ✅ 根据需求实现完整审核流程
3. ✅ 添加相应的通知逻辑
4. ✅ 更新前端适配新的消息类型

---

## 📝 附录

### 附录A: MessageType 完整列表

基于代码分析，系统支持以下消息类型（部分）：

| 类型值 | 消息类型 | Category | 用途 |
|-------|---------|----------|------|
| 1 | applyCourse | 2 | 申请课程 |
| 2 | system | 2 | 系统消息 |
| 3 | follow | 1 | 关注通知 |
| 4 | upvote | 1 | 点赞通知 |
| 5 | postComment | 1 | 帖子评论通知 |
| 6 | nodeComment | 1 | 节点评论通知 |
| 7 | replyPostingComment | 1 | 回复帖子评论通知 |
| 8 | replyNodeComment | 1 | 回复节点评论通知 |
| 9 | invite | 1 | 邀请通知 |
| 10 | courseApproved | 2 | 课程批准 |
| 11 | courseRejected | 2 | 课程拒绝 |
| 12 | courseBanned | 2 | 课程封禁 |
| 13 | postRejected | 2 | 帖子拒绝 |
| 14 | postBanned | 2 | 帖子封禁 |
| 15 | commentRejected | 2 | 评论拒绝 |
| 16 | commentBanned | 2 | 评论封禁 |
| 17 | professionApproved | 2 | 职业批准 |
| 18 | professionRejected | 2 | 职业拒绝 |
| 19 | professionBanned | 2 | 职业封禁 |
| 20 | roadmapRejected | 2 | 路线图拒绝 |
| 21 | roadmapBanned | 2 | 路线图封禁 |
| 22 | nodeRejected | 2 | 节点拒绝 |
| 23 | nodeBanned | 2 | 节点封禁 |
| 24 | memoryDeckRejected | 2 | 卡片组拒绝 |
| 25 | memoryDeckBanned | 2 | 卡片组封禁 |
| 26 | roadmapComment | 1 | 路线图评论通知 |
| 27 | replyRoadmapComment | 1 | 回复路线图评论通知 |

**注**: 需要新增的类型:
- `postApproved` (帖子批准)
- `commentApproved` (评论批准)

---

### 附录B: 相关代码文件清单

| 文件路径 | 说明 |
|---------|------|
| `backend/learn-domain/.../MessageService.java` | 消息服务核心类 |
| `backend/learn-domain/.../PostService.java` | 帖子业务逻辑 |
| `backend/learn-domain/.../CommentService.java` | 评论业务逻辑 |
| `backend/learn-domain/.../CourseService.java` | 课程业务逻辑 |
| `backend/learn-domain/.../ProfessionService.java` | 职业业务逻辑 |
| `backend/learn-domain/.../RoadmapService.java` | 路线图业务逻辑 |
| `backend/learn-domain/.../NodeService.java` | 节点业务逻辑 |
| `backend/learn-domain/.../MemoryCardDeckService.java` | 卡片组业务逻辑 |
| `backend/learn-domain/.../FollowService.java` | 关注业务逻辑 |
| `backend/learn-domain/.../UpvoteService.java` | 点赞业务逻辑 |
| `backend/learn-common/.../Enums.java` | 枚举定义 |
| `backend/learn-api/.../MessagesController.java` | 消息API接口 |

---

### 附录C: 前端消息展示

消息在前端的展示位置（需要前端开发确认）：

1. **消息中心**: `/messages` 或 `/notifications`
2. **分类展示**:
   - 互动消息 (category=1): 评论、点赞、关注
   - 系统消息 (category=2): 审核通知
   - 私信 (category=3): 用户私信
3. **实时通知**: 页面顶部的通知图标
4. **邮件通知**: 可选的邮件提醒

**前端需要适配的新消息类型**:
- `postApproved`: 显示"您的帖子已通过审核"
- `commentApproved`: 显示"您的评论已通过审核"

---

## 📚 参考资料

- [审核通知系统实现方案](/docs/审核通知系统实现方案.md)
- [消息系统设计文档](/docs/message-system-design.md) (如果存在)
- [后端架构改进建议](/backend/docs/后端架构改进建议.md)

---

## 📋 文档变更记录

| 版本 | 日期 | 作者 | 变更说明 |
|-----|------|------|---------|
| v1.0 | 2025-11-01 | Claude | 初始版本，完成全面分析 |

---

**文档结束**
