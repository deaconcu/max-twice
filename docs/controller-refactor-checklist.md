# Controller 重构 Checklist

## 重构目标

1. 移除所有 Controller 中的 `StpUtil` 调用，改用 `@CurrentUser` 注解
2. 移除所有 `@RequireOwner` 注解，将所有权验证逻辑移到 Service 层
3. 优化 Service 方法，避免重复查询用户

## 重构原则

### ✅ 正确的写法
```java
// Controller
@PostMapping("/posts/{id}")
@SaCheckLogin
public ApiResponse<Void> updatePost(
        @PathVariable Long id,
        @RequestBody UpdateRequest request,
        @CurrentUser UserDO currentUser) {  // ✅ 使用 @CurrentUser
    postService.updatePost(id, request, currentUser);  // ✅ 传递 currentUser
    return ApiResponse.success();
}

// Service
public void updatePost(Long id, UpdateRequest request, UserDO operator) {
    PostDO post = postDataService.getById(id);

    // ✅ Service 层验证所有权
    if (!post.getCreatorId().equals(operator.getId()) && !operator.isAdmin()) {
        throw ErrorCode.PERMISSION_DENIED.exception();
    }

    // 业务逻辑...
}
```

### ❌ 错误的写法
```java
// ❌ 使用 StpUtil
Long userId = StpUtil.getLoginIdAsLong();
postService.updatePost(id, request, userId);

// ❌ 使用 @RequireOwner 注解
@RequireOwner(resourceType = "POST", idParam = "id")
public ApiResponse<Void> updatePost(...) { }

// ❌ Service 再次查询用户
public void updatePost(Long userId, ...) {
    UserDO user = userDataService.getById(userId);  // 重复查询！
}
```

---

## 第一部分：移除 @RequireOwner 注解

### PostsController

- [x] **删除 @RequireOwner 注解**
  - [x] 第75行：`updatePost()` 方法
  - [x] 第91行：`deletePost()` 方法

- [x] **检查 PostService 是否已有所有权验证**
  - [x] `updatePost()` 方法
  - [x] `deletePost()` 方法

- [x] **如果 Service 没有验证，添加验证逻辑**
  ```java
  if (!post.getCreatorId().equals(operator.getId()) && !operator.isAdmin()) {
      throw ErrorCode.PERMISSION_DENIED.exception();
  }
  ```

### CoursesController

- [x] **删除 @RequireOwner 注解**
  - [x] 第158行：`updateCourse()` 方法

- [x] **检查 CourseService 是否已有所有权验证**
  - [x] `updateCourse()` 方法

- [x] **如果 Service 没有验证，添加验证逻辑**

---

## 第二部分：移除 StpUtil 调用

### ✅ 已完成
1. **FollowsController** - 已修改完成 ✅
   - ✅ 移除 StpUtil
   - ✅ 添加 @CurrentUser
   - ✅ FollowService.follow() 改为接收 UserDO 参数

---

### 📋 待修改清单

#### 高优先级（核心业务功能）

2. **PostsController** ✅
   - [x] 检查所有使用 StpUtil 的方法
   - [x] 添加 `@CurrentUser UserDO currentUser` 参数
   - [x] 检查对应 Service 方法是否会查询用户
   - [x] 如果 Service 会查询用户，修改 Service 接收 UserDO 参数
   - [x] 更新 Controller 调用方式

3. **UpvotesController** ✅
   - [x] 检查所有使用 StpUtil 的方法
   - [x] 添加 `@CurrentUser UserDO currentUser` 参数
   - [x] 检查 UpvoteService 是否需要修改
   - [x] 更新调用方式

4. **SubscriptionsController** ✅
   - [x] 检查所有使用 StpUtil 的方法
   - [x] 添加 `@CurrentUser UserDO currentUser` 参数
   - [x] 检查 SubscriptionService 是否需要修改
   - [x] 更新调用方式

5. **MessagesController** ✅
   - [x] 检查所有使用 StpUtil 的方法
   - [x] 添加 `@CurrentUser UserDO currentUser` 参数
   - [x] 检查 MessageService 是否需要修改
   - [x] 更新调用方式

#### 中优先级（内容管理）

6. **ContentsController** ✅
   - [x] 检查所有使用 StpUtil 的方法
   - [x] 添加 `@CurrentUser UserDO currentUser` 参数
   - [x] 检查 ContentService 是否需要修改
   - [x] 更新调用方式

7. **PagesController** ✅
   - [x] 检查所有使用 StpUtil 的方法
   - [x] 添加 `@CurrentUser UserDO currentUser` 参数
   - [x] 检查 PageService 是否需要修改
   - [x] 更新调用方式

8. **MemoryCardController** ✅
   - [x] 检查所有使用 StpUtil 的方法
   - [x] 添加 `@CurrentUser UserDO currentUser` 参数
   - [x] 检查 MemoryCardService 是否需要修改
   - [x] 更新调用方式

9. **MemoryCardDeckController** ✅
   - [x] 检查所有使用 StpUtil 的方法
   - [x] 添加 `@CurrentUser UserDO currentUser` 参数
   - [x] 检查 MemoryCardDeckService 是否需要修改
   - [x] 更新调用方式

10. **MemoryBankController** ✅
    - [x] 检查所有使用 StpUtil 的方法
    - [x] 添加 `@CurrentUser UserDO currentUser` 参数
    - [x] 检查 MemoryBankService 是否需要修改
    - [x] 更新调用方式

#### 低优先级（辅助功能）

11. **TocController** ✅
    - [x] 检查所有使用 StpUtil 的方法
    - [x] 添加 `@CurrentUser UserDO currentUser` 参数
    - [x] 检查 TocService 是否需要修改
    - [x] 更新调用方式

12. **ProgressController** ✅
    - [x] 检查所有使用 StpUtil 的方法
    - [x] 添加 `@CurrentUser UserDO currentUser` 参数
    - [x] 检查 ProgressService 是否需要修改
    - [x] 更新调用方式

13. **ReviewController** ✅
    - [x] 检查所有使用 StpUtil 的方法
    - [x] 添加 `@CurrentUser UserDO currentUser` 参数
    - [x] 检查 ReviewService 是否需要修改
    - [x] 更新调用方式

14. **StatsController** ✅
    - [x] 检查所有使用 StpUtil 的方法
    - [x] 添加 `@CurrentUser UserDO currentUser` 参数
    - [x] 检查 StatsService 是否需要修改
    - [x] 更新调用方式

15. **UsersController** ✅
    - [x] 检查所有使用 StpUtil 的方法（除了登录相关）
    - [x] 添加 `@CurrentUser UserDO currentUser` 参数
    - [x] 注意：登录、注册等方法可以保留 StpUtil（用于设置登录状态）
    - [x] 更新其他方法的调用方式

---

## 修改步骤模板

对于每个 Controller，按以下步骤操作：

### Step 1: 分析 Controller
```bash
# 查找 StpUtil 使用位置
grep -n "StpUtil" Controller文件.java
```

### Step 2: 检查每个方法
对于每个使用 StpUtil 的方法：

1. **查看方法签名**
   - 是否已有 `@SaCheckLogin` 注解？
   - 是否已有 `@CurrentUser` 参数？

2. **查看 Service 调用**
   ```java
   // 找到类似这样的代码
   Long userId = StpUtil.getLoginIdAsLong();
   someService.someMethod(userId, ...);
   ```

3. **检查 Service 方法**
   - 打开对应的 Service 类
   - 找到被调用的方法
   - 查看方法体内是否有 `userDataService.getById(userId)`
   - 如果有，说明需要修改 Service 接收 UserDO

### Step 3: 修改 Service（如需要）
```java
// 修改前
public void someMethod(Long userId, ...) {
    UserDO user = userDataService.getById(userId);  // 重复查询
    // 业务逻辑...
}

// 修改后
public void someMethod(UserDO user, ...) {
    // 直接使用 user
    // 业务逻辑...
}
```

### Step 4: 修改 Controller
```java
// 修改前
@PostMapping("/xxx")
public ApiResponse<Void> someMethod(...) {
    Long userId = StpUtil.getLoginIdAsLong();
    someService.someMethod(userId, ...);
    return ApiResponse.success();
}

// 修改后
@PostMapping("/xxx")
@SaCheckLogin  // 确保有登录检查
public ApiResponse<Void> someMethod(
        ...,
        @CurrentUser UserDO currentUser) {  // 添加参数
    // 如果 Service 接收 UserDO
    someService.someMethod(currentUser, ...);
    // 如果 Service 只接收 userId
    // someService.someMethod(currentUser.getId(), ...);
    return ApiResponse.success();
}
```

### Step 5: 删除导入
```java
// 删除这一行
import cn.dev33.satoken.stp.StpUtil;

// 确保添加了这些
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.persistence.dataobject.UserDO;
```

---

## 验证清单

每修改完一个 Controller，执行以下检查：

- [ ] Controller 不再有 `StpUtil.getLoginIdAsLong()` 调用
- [ ] Controller 不再有 `@RequireOwner` 注解
- [ ] 所有需要登录的方法都有 `@SaCheckLogin` 注解
- [ ] 所有需要用户信息的方法都有 `@CurrentUser UserDO currentUser` 参数
- [ ] Service 层有适当的所有权验证逻辑
- [ ] Service 不会重复查询已在 Controller 获取的用户信息
- [ ] 代码能够编译通过
- [ ] 相关接口测试通过

---

## 特殊情况处理

### 登录/注册接口
```java
// ✅ 这些接口可以保留 StpUtil（用于设置登录状态）
@PostMapping("/auth/login")
public ApiResponse<UserDTO> login(...) {
    UserDTO user = userService.validateLogin(...);
    StpUtil.login(user.getId());  // ✅ 可以保留
    return ApiResponse.success(user);
}
```

### 公开接口（不需要登录）
```java
// ✅ 公开接口不需要添加 @CurrentUser
@GetMapping("/posts/{id}")
public ApiResponse<PostDTO> getPost(@PathVariable Long id) {
    // 公开接口，不需要登录
    return ApiResponse.success(postService.getPost(id));
}
```

### 可选登录接口
```java
// 如果接口支持登录和未登录两种情况
// 建议拆分为两个接口，或者在 Service 层处理
```

---

## 进度追踪

- 总计：15 个 Controller 需要检查修改
- 已完成：15 个（FollowsController, PostsController, CoursesController, UpvotesController, SubscriptionsController, MessagesController, ContentsController, PagesController, MemoryCardController, MemoryCardDeckController, MemoryBankController, TocController, ProgressController, ReviewController, StatsController, UsersController）
- 待完成：0 个
- 完成率：100% ✅

**预计工作量：** 每个 Controller 约需要 15-30 分钟，总计约 4-7 小时

---

## 注意事项

1. **不要一次性修改所有文件**，逐个修改并测试
2. **先修改 Service，再修改 Controller**，避免编译错误
3. **每个修改都提交一次 git**，方便回滚
4. **优先修改高优先级的核心功能**
5. **保持代码风格一致**
6. **添加必要的注释说明**
7. **测试修改后的接口**，确保功能正常

---

## 参考示例

### 示例1：FollowsController（已完成）
- 移除了 StpUtil
- 添加了 @CurrentUser 参数
- FollowService.follow() 改为接收 UserDO，避免重复查询

### 示例2：PostService
- 已有所有权验证逻辑
- updatePost() 和 deletePost() 方法已经检查所有权

### 示例3：RoadmapService
- 已有所有权验证逻辑
- updateRoadmap() 和 deleteRoadmap() 方法已经检查所有权

---

**最后更新：** 2025-10-28
**负责人：** Claude Code
**状态：** ✅ 已完成
