# 全站只读模式功能 TODO

## 项目背景
实现全站只读模式，方便代码升级和系统维护期间，禁止用户进行写操作，保证数据一致性。

## 技术方案

### 架构设计
```
┌─────────────────────────────────────────┐
│            前端 (Vue3 + TypeScript)     │
├─────────────────────────────────────────┤
│ • 页面加载查询只读状态                  │
│ • 显示维护横幅                          │
│ • 禁用所有提交按钮                      │
│ • Axios 拦截器兜底                      │
└─────────────────────────────────────────┘
                    ↓ HTTP
┌─────────────────────────────────────────┐
│            后端 (Spring Boot)           │
├─────────────────────────────────────────┤
│ • ReadOnlyModeInterceptor 拦截写请求   │
│ • SystemConfigService                   │
│ • Redis 缓存（多实例同步）              │
│ • system_config 表（持久化）            │
└─────────────────────────────────────────┘
```

---

## 后端实现

### 1. 数据库设计

#### 1.1 ✅ 已有系统配置表
**现有表结构：`system`**
```sql
CREATE TABLE `system` (
  `key` varchar(50) NOT NULL,
  `value` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

- [ ] 插入只读模式初始数据
  ```sql
  INSERT INTO `system` (`key`, `value`)
  VALUES ('readonly_mode', '0')
  ON DUPLICATE KEY UPDATE `value` = '0';
  ```
  **说明：** 0 = 关闭，1 = 开启

#### 1.2 ✅ 已有实体类和 Mapper
**现有代码：**
- `SystemDO.java`：`learn-persistence/src/main/java/com/prosper/learn/persistence/dataobject/SystemDO.java`
- `SystemMapper.java`：自动映射
- `SystemDataService.java`：`learn-domain/src/main/java/com/prosper/learn/domain/service/data/SystemDataService.java`

**现有方法：**
- ✅ `getValue(String key)` - 已有 @Cacheable
- ✅ `setValue(String key, String value)` - 已有 @CacheEvict
- ✅ `exists(String key)` - 已有

---

### 2. ✅ Redis 配置已完成
**现有配置：**
- `AppConfiguration.java` 已配置 `RedisTemplate`
- `SystemDataService` 已使用 Spring Cache（`@Cacheable`）
- 缓存名称：`system`

---

### 3. Service 层 - 扩展只读模式功能

- [ ] 在 `SystemDataService.java` 中添加只读模式相关方法
  - 位置：`learn-domain/src/main/java/com/prosper/learn/domain/service/data/SystemDataService.java`
  - 新增方法：
    ```java
    /**
     * 判断是否只读模式
     * @return true=只读模式，false=正常模式
     */
    public boolean isReadOnlyMode() {
        String value = getValue("readonly_mode");
        return "1".equals(value);
    }

    /**
     * 开启只读模式
     */
    public void enableReadOnlyMode() {
        setValue("readonly_mode", "1");
    }

    /**
     * 关闭只读模式
     */
    public void disableReadOnlyMode() {
        setValue("readonly_mode", "0");
    }
    ```
    **说明：** 0 = 关闭，1 = 开启

**说明：**
- ✅ 已有 `getValue()` 和 `setValue()` 方法
- ✅ 已有 Spring Cache（`@Cacheable` 和 `@CacheEvict`）
- ✅ Redis 已集成，缓存自动生效
- ✅ 多实例环境下，Redis 作为缓存共享

---

### 4. 拦截器

- [ ] 创建 `ReadOnlyModeInterceptor.java`
  - 位置：`learn-api/src/main/java/com/prosper/learn/api/interceptor/ReadOnlyModeInterceptor.java`
  - 功能：
    ```java
    @Component
    public class ReadOnlyModeInterceptor implements HandlerInterceptor {

        @Autowired
        private SystemConfigService configService;

        @Override
        public boolean preHandle(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler) {
            // 拦截写操作：POST, PUT, DELETE, PATCH
            String method = request.getMethod();
            if (isWriteMethod(method)) {
                if (configService.isReadOnlyMode()) {
                    throw new ReadOnlyModeException("系统维护中，暂时无法进行此操作");
                }
            }
            return true;
        }

        private boolean isWriteMethod(String method) {
            return "POST".equals(method) || "PUT".equals(method)
                || "DELETE".equals(method) || "PATCH".equals(method);
        }
    }
    ```

- [ ] 注册拦截器
  - 位置：`WebMvcConfig.java` 或类似配置类
  - 配置拦截路径：`/api/**`
  - 排除路径：登录、查询只读状态等

---

### 5. 异常处理

- [ ] 创建自定义异常 `ReadOnlyModeException.java`
  - 位置：`learn-domain/src/main/java/com/prosper/learn/domain/exception/ReadOnlyModeException.java`

- [ ] 全局异常处理
  - 在 `GlobalExceptionHandler` 中添加处理
  - 返回统一错误格式：
    ```json
    {
      "code": "READONLY_MODE",
      "message": "系统维护中，暂时无法进行此操作",
      "data": null
    }
    ```

---

### 6. API 接口

#### 6.1 管理员接口
- [ ] 在现有 `SystemController.java` 中添加只读模式接口
  - 位置：`learn-api/src/main/java/com/prosper/learn/api/v1/controller/SystemController.java`
  - 新增接口：
    ```java
    // 开启只读模式（管理员）
    POST /api/v1/system/readonly-mode/enable

    // 关闭只读模式（管理员）
    POST /api/v1/system/readonly-mode/disable
    ```

**说明：**
- ✅ 现有 Controller 已有 `@SaCheckLogin` 注解（需要登录）
- 注意：现有代码没有明确的管理员角色检查，需要确认权限体系
- 可选：添加 `@SaCheckRole("admin")` 或自定义权限检查

#### 6.2 公开接口
- [ ] 添加公开查询接口（无需登录）
  ```java
  // 前端查询只读状态
  GET /api/v1/public/readonly-mode

  // 返回：
  {
    "code": 200,
    "data": {
      "enabled": true
    }
  }
  ```

- [ ] 创建 `PublicController.java` 或在 `SystemController` 中添加
  - 该接口不能有 `@SaCheckLogin` 限制
  - 需要单独的 Controller 或排除路径

---

### 7. 测试

- [ ] 单元测试
  - SystemConfigService 测试
  - ReadOnlyModeInterceptor 测试

- [ ] 集成测试
  - 开启只读模式后，写操作被拦截
  - 关闭只读模式后，写操作正常
  - Redis 缓存同步测试

---

## 前端实现

### 1. API 服务

- [ ] 创建 `systemService.ts`
  - 位置：`web-ts/src/services/api/v1/systemService.ts`
  - 方法：
    ```typescript
    // 查询只读模式状态
    getReadOnlyMode(): Promise<{enabled: boolean, message?: string}>

    // 管理员开启只读模式
    enableReadOnlyMode(): Promise<void>

    // 管理员关闭只读模式
    disableReadOnlyMode(): Promise<void>
    ```

---

### 2. 状态管理

- [ ] 创建 `systemStore.ts`
  - 位置：`web-ts/src/stores/systemStore.ts`
  - 状态：
    ```typescript
    interface SystemState {
      readOnlyMode: boolean
      readOnlyMessage: string
    }

    // Actions
    fetchReadOnlyMode()  // 查询状态
    setReadOnlyMode(enabled: boolean, message?: string)
    ```

---

### 3. 全局组件

- [ ] 创建维护横幅组件 `MaintenanceBanner.vue`
  - 位置：`web-ts/src/components/common/MaintenanceBanner.vue`
  - 功能：
    - 显示维护提示信息
    - 支持自定义消息
    - 醒目的样式（黄色/橙色背景）

- [ ] 在主布局中集成
  - 位置：`App.vue` 或主布局组件
  - 条件显示：`v-if="systemStore.readOnlyMode"`

---

### 4. Axios 拦截器

- [ ] 响应拦截器处理只读错误
  - 位置：`web-ts/src/services/axios.ts` 或类似文件
  - 代码：
    ```typescript
    axios.interceptors.response.use(
      response => response,
      error => {
        if (error.response?.data?.code === 'READONLY_MODE') {
          // 显示友好提示
          showSnackbar('系统维护中，暂时无法进行此操作')
        }
        return Promise.reject(error)
      }
    )
    ```

---

### 5. 按钮禁用

- [ ] 全局混入或组合函数
  - 创建 `useReadOnlyMode.ts`
    ```typescript
    export function useReadOnlyMode() {
      const systemStore = useSystemStore()

      const isReadOnly = computed(() => systemStore.readOnlyMode)

      return { isReadOnly }
    }
    ```

- [ ] 关键组件集成
  - `AddContents.vue`：禁用提交按钮
  - 其他写操作组件：禁用相应按钮
  - 示例：`:disabled="isReadOnly || 其他条件"`

---

### 6. 页面初始化

- [ ] App.vue 加载时查询状态
  ```typescript
  onMounted(async () => {
    await systemStore.fetchReadOnlyMode()
  })
  ```

---

### 7. 管理员界面（可选）

- [ ] 创建管理员配置页面
  - 位置：`web-ts/src/views/admin/SystemConfig.vue`
  - 功能：
    - 一键开关只读模式
    - 显示当前状态
    - 操作日志

---

## 优化项（第二阶段）

### 后端优化

- [ ] Redis Pub/Sub 实现
  - 配置变更时发布消息
  - 所有实例订阅消息，实时刷新缓存

- [ ] 审计日志
  - 记录谁开启/关闭了只读模式
  - 记录操作时间

- [ ] 白名单机制（可选）
  - 某些接口不受只读限制
  - 配置化管理

---

### 前端优化

- [ ] 定期轮询
  - 每 1-5 分钟查询一次状态
  - 自动更新 UI

- [ ] WebSocket 推送（可选）
  - 管理员切换状态时实时推送
  - 所有在线用户立即收到通知

- [ ] 优化用户体验
  - 显示预计恢复时间
  - 维护倒计时

---

## 部署检查清单

### 数据库
- [ ] 执行 SQL 脚本创建表
- [ ] 验证初始数据

### Redis
- [ ] 确认 Redis 连接正常
- [ ] 验证多实例同步

### 配置
- [ ] 检查拦截器是否生效
- [ ] 检查权限配置

### 测试
- [ ] 开启只读模式，验证写操作被拦截
- [ ] 前端显示维护横幅
- [ ] 关闭只读模式，验证功能恢复
- [ ] 多实例环境测试缓存同步

---

## 现有基础设施确认 ✅

### 已确认的现有代码
1. **✅ Redis 配置**
   - 已配置 `RedisTemplate` 在 `AppConfiguration.java:62-86`
   - 已使用 Spring Cache 注解

2. **⚠️ 权限体系**
   - 使用 Sa-Token（`@SaCheckLogin`）
   - 现有代码中管理员接口路径为 `/api/v1/admin/*`
   - 但没有看到明确的角色检查（`@SaCheckRole`）
   - **需要确认**：管理员权限如何判断？是否需要添加？

3. **✅ 现有配置表**
   - 表名：`system` (key-value 结构)
   - Service：`SystemDataService.java`（已有缓存）
   - Controller：`SystemController.java`（需要登录）

4. **⚠️ 异常处理**
   - **需要查找**：GlobalExceptionHandler 的位置
   - **需要确认**：统一返回格式（ApiResponse）

5. **⚠️ 前端结构**
   - **需要查找**：Axios 配置文件
   - **需要确认**：Snackbar/Toast 调用方式
   - **需要确认**：主布局文件

### 待确认问题
- [ ] 管理员权限验证机制
- [ ] GlobalExceptionHandler 位置
- [ ] 前端 Axios 拦截器配置
- [ ] 前端状态管理（Pinia stores）

### 技术决策待确认
- [ ] Redis Pub/Sub 是否需要？（第一阶段可不做）
- [ ] 前端轮询频率？（建议 2-5 分钟，或不做）
- [ ] 是否需要维护消息配置？（建议支持）
- [ ] 是否需要管理员 UI？（建议后做）

---

## 开发排期建议

### 第一阶段（核心功能，2-3 天）
- Day 1：后端基础（数据库、Service、拦截器）
- Day 2：后端 API + 前端基础（API、状态管理）
- Day 3：前端 UI（横幅、按钮禁用）+ 测试

### 第二阶段（优化，1-2 天）
- 管理员 UI
- 轮询或推送
- Redis Pub/Sub

---

## 备注
- 本方案基于多实例部署环境
- 只读模式下管理员也不能操作
- 采用拦截器方式，零代码侵入
- Redis 作为多实例状态同步方案
