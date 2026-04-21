# JPA + MyBatis 混合方案实施指南

## 📋 文档信息

- **创建日期**: 2025-11-26
- **方案类型**: 架构改造
- **预计工期**: 3-4 周
- **风险等级**: 中等

---

## 🎯 方案目标

### 核心诉求
1. **减少样板代码**：消除 45% 的简单 CRUD 重复代码
2. **自动时间字段管理**：通过 JPA Auditing 自动填充 `createdAt` / `updatedAt`
3. **保持灵活性**：复杂查询继续使用 MyBatis
4. **渐进式迁移**：降低风险，可随时回滚

### 方案原则
- ✅ **简单 CRUD** → 使用 JPA Repository（约 150 个方法）
- ✅ **复杂查询** → 保留 MyBatis Mapper（约 185 个方法）
- ✅ **事务管理** → 统一在 Service 层使用 `@Transactional`
- ✅ **向后兼容** → 不破坏现有功能

---

## 📊 现状分析

### 项目统计
- **Mapper 数量**: 27 个
- **SQL 方法总数**: 约 335 个
- **简单 CRUD**: 45% (150 个方法) → **迁移到 JPA**
- **中等复杂**: 40% (134 个方法) → **保留 MyBatis**
- **高复杂**: 15% (51 个方法) → **保留 MyBatis**

### 技术栈
- **当前**: Spring Boot 3.3.3 + MyBatis 3.0.3
- **目标**: Spring Boot 3.3.3 + JPA 3.x + MyBatis 3.0.3（共存）

---

## 🏗️ 架构设计

### 分层架构（保留现有 DataService 模式）

```
┌─────────────────────────────────────────┐
│           Controller 层                  │
│  (不变，继续调用 BusinessService)         │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│       BusinessService 层                 │
│  - 纯业务逻辑                            │
│  - 业务流程编排                          │
│  - 调用 DataService 进行数据访问          │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│         DataService 层                   │
│  - 数据访问（JpaRepository + Mapper）    │
│  - 缓存管理（@Cacheable, @CacheEvict）   │
│  - 简单操作用 JpaRepository              │
│  - 复杂查询用 MyBatis Mapper             │
└─────────────────────────────────────────┘
                   ↓
        ┌──────────┴──────────┐
        ↓                     ↓
┌──────────────┐      ┌──────────────┐
│JpaRepository │      │MyBatis Mapper│
│ (简单 CRUD)   │      │ (复杂查询)    │
└──────────────┘      └──────────────┘
        ↓                     ↓
┌─────────────────────────────────────────┐
│         实体类 (统一)                     │
│  - 添加 JPA 注解 (@Entity, @Table 等)   │
│  - 保持 MyBatis 兼容性                   │
│  - 使用 JPA Auditing 自动填充时间         │
└─────────────────────────────────────────┘
```

### 架构说明

**重要：本项目采用 DataService 模式，而非标准 JPA 做法**

#### 我们的模式 vs 标准 JPA 做法

| 特性 | 我们的 DataService 模式 | 标准 JPA 做法 |
|------|----------------------|--------------|
| **分层** | Controller → BusinessService → DataService → Repository/Mapper | Controller → Service → Repository |
| **职责分离** | ✅ 业务逻辑和数据访问分离 | ⚠️ 混在 Service 层 |
| **缓存位置** | DataService 层 | Service 层 |
| **符合 SRP** | ✅ 是 | ❌ 否 |
| **适合场景** | 中大型项目，复杂业务逻辑 | 中小型项目，简单 CRUD |

#### 为什么保留 DataService 模式？

1. **✅ 职责清晰**
   - DataService：专注数据访问 + 缓存
   - BusinessService：专注业务逻辑

2. **✅ 易于测试**
   - 可以单独测试数据访问逻辑
   - 可以 Mock DataService 测试业务逻辑

3. **✅ 易于维护**
   - 缓存策略变化只影响 DataService
   - 业务规则变化只影响BusinessService

4. **✅ 符合架构原则**
   - 遵循单一职责原则（SRP）
   - 符合 DDD 和 Clean Architecture

5. **✅ 适合大型项目**
   - 本项目有 27 个模块，335 个数据访问方法
   - 复杂的缓存逻辑（批量查询、多级缓存）

**注意：这不是标准 JPA 做法，但更适合本项目！**

### 实体类设计

所有实体类（`*DO`）将同时支持 JPA 和 MyBatis：

```java
@Entity
@Table(name = "user")
@EntityListeners(AuditingEntityListener.class)
@Data
public class UserDO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "email", unique = true)
    private String email;

    // JPA Auditing 自动管理时间字段
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

### 数据访问层设计（DataService 模式）

#### 1. JpaRepository (新增)

```java
package com.prosper.learn.persistence.repository;

import profile.com.twicemax.user.UserDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserDO, Long> {
    // 简单查询方法（JPA 自动实现）
    Optional<UserDO> findByEmail(String email);
    Optional<UserDO> findByName(String name);
    List<UserDO> findByIdIn(Collection<Long> ids);

    // 注意：不要在 Repository 加缓存注解！
    // 缓存由 DataService 统一管理
}
```

#### 2. MyBatis Mapper (保留复杂查询)

```java
package com.prosper.learn.persistence.mapper;

@Mapper
public interface UserMapper {

    // 保留：复杂的动态 SQL
    @Select({"<script>SELECT * FROM user where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<UserDO> getByIds(Collection<Long> ids);

    @Select({"<script>SELECT * FROM user where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, UserDO> getMapByIds(Collection<Long> ids);

    // 保留：复杂的多表联查
    @Select("SELECT u.*, COUNT(c.id) as course_count " +
            "FROM user u LEFT JOIN course c ON u.id = c.creator_id " +
            "GROUP BY u.id")
    List<UserWithCourseCount> getUsersWithCourseCount();

    // 保留：模糊搜索
    @Select("SELECT * FROM user WHERE INSTR(name, #{name}) > 0 limit 20")
    List<UserDO> searchByName(String name);
}
```

#### 3. DataService 层（数据访问 + 缓存）

**重要：这是本项目的核心设计！**

```java
package com.prosper.learn.business.service.data;

/**
 * 用户数据服务
 * 职责：数据访问 + 缓存管理
 *
 * 注意：这一层不包含业务逻辑！
 */
@Service
@Slf4j
public class UserDataService {

    @Autowired
    private UserRepository userRepository;  // JPA（简单 CRUD）

    @Autowired
    private UserMapper userMapper;          // MyBatis（复杂查询）

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ========== 简单查询用 JPA（带缓存） ==========

    @Cacheable(value = "users", key = "#id")
    public UserDO getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Cacheable(value = "usersByEmail", key = "#email")
    public UserDO getByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Cacheable(value = "usersByName", key = "#name")
    public UserDO getByName(String name) {
        return userRepository.findByName(name).orElse(null);
    }

    // ========== 批量查询（带缓存优化） ==========

    /**
     * 批量查询用户（MGET 优化）
     * 1. 先从 Redis 批量获取
     * 2. 缓存未命中的从 JPA 查询
     * 3. 写回缓存
     */
    public List<UserDO> getByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 批量从缓存获取
        Map<Long, UserDO> cachedUsers = batchGetFromCache(ids);

        // 2. 找出缓存未命中的 ID
        List<Long> missedIds = ids.stream()
                .filter(id -> !cachedUsers.containsKey(id))
                .collect(Collectors.toList());

        // 3. 从 JPA 查询未命中的（简单查询用 JPA）
        if (!missedIds.isEmpty()) {
            List<UserDO> fromDB = userRepository.findByIdIn(missedIds);
            batchPutToCache(fromDB);
            fromDB.forEach(user -> cachedUsers.put(user.getId(), user));
        }

        return new ArrayList<>(cachedUsers.values());
    }

    // ========== 复杂查询用 MyBatis（不缓存） ==========

    /**
     * 批量查询（返回 Map）
     * 使用 MyBatis 的 @MapKey 功能
     */
    public Map<Long, UserDO> getMapByIds(Collection<Long> ids) {
        return userMapper.getMapByIds(ids);
    }

    /**
     * 搜索用户（模糊查询）
     * 使用 MyBatis 的 INSTR 函数
     */
    public List<UserDO> searchByName(String name) {
        return userMapper.searchByName(name);
    }

    /**
     * 复杂的统计查询
     * 使用 MyBatis 的多表联查
     */
    public List<UserWithCourseCount> getUsersWithCourseCount() {
        return userMapper.getUsersWithCourseCount();
    }

    // ========== 写操作（清除缓存） ==========

    @CacheEvict(value = "users", key = "#user.id")
    @Transactional
    public UserDO save(UserDO user) {
        return userRepository.save(user);  // JPA 自动填充时间
    }

    @CacheEvict(value = "users", key = "#user.id")
    @Transactional
    public void update(UserDO user) {
        userRepository.save(user);  // updatedAt 自动更新
    }

    // ========== 私有方法：缓存工具 ==========

    private Map<Long, UserDO> batchGetFromCache(Collection<Long> ids) {
        // 使用 MGET 批量获取
        List<String> keys = ids.stream()
                .map(id -> "users::" + id)
                .collect(Collectors.toList());

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, UserDO> result = new HashMap<>();
        // ... 省略实现细节
        return result;
    }

    private void batchPutToCache(List<UserDO> users) {
        users.forEach(user -> {
            String key = "users::" + user.getId();
            redisTemplate.opsForValue().set(key, user, Duration.ofMinutes(30));
        });
    }
}
```

#### 4. BusinessService 层（纯业务逻辑）

```java
package com.prosper.learn.business.service.application;

/**
 * 用户业务服务
 * 职责：业务逻辑 + 业务流程编排
 *
 * 注意：这一层不直接访问数据库，通过 DataService 访问！
 */
@Service
@RequiredArgsConstructor
public class UserService {

   private final UserDataService userDataService;  // ← 只依赖 DataService
   private final EmailService emailService;
   private final ValidationService validationService;

   /**
    * 注册用户（业务流程）
    */
   @Transactional
   public void registerUser(RegisterRequest request) {
      // 1. 业务验证
      validationService.validateRegistration(request);

      // 2. 检查用户是否已存在
      UserDO existingUser = userDataService.getByEmail(request.getEmail());
      if (existingUser != null) {
         throw ErrorCode.USER_ALREADY_EXISTS.exception();
      }

      // 3. 创建用户实体
      UserDO user = new UserDO();
      user.setName(request.getName());
      user.setEmail(request.getEmail());
      user.setPassword(hashPassword(request.getPassword()));

      // 4. 保存用户（时间字段自动填充）
      userDataService.save(user);

      // 5. 发送欢迎邮件
      emailService.sendWelcomeEmail(user);

      // 6. 创建默认设置
      createDefaultSettings(user);
   }

   /**
    * 获取用户信息（简单查询）
    */
   public UserDO getUserById(Long userId) {
      UserDO user = userDataService.getById(userId);
      if (user == null) {
         throw ErrorCode.USER_NOT_FOUND.exception();
      }
      return user;
   }

   /**
    * 批量获取用户（带缓存优化）
    */
   public List<UserDO> getUsersByIds(List<Long> userIds) {
      return userDataService.getByIds(userIds);
   }

   /**
    * 搜索用户（复杂查询）
    */
   public List<UserDO> searchUsers(String keyword) {
      if (keyword == null || keyword.trim().isEmpty()) {
         throw ErrorCode.INVALID_PARAMETER.exception();
      }
      return userDataService.searchByName(keyword);
   }

   /**
    * 更新用户信息（业务逻辑）
    */
   @Transactional
   public void updateUserProfile(Long userId, UpdateProfileRequest request) {
      // 1. 获取用户
      UserDO user = getUserById(userId);

      // 2. 业务验证
      validationService.validateProfile(request);

      // 3. 更新字段
      user.setName(request.getName());
      user.setBiography(request.getBiography());

      // 4. 保存（updatedAt 自动更新）
      userDataService.update(user);

      // 5. 通知相关系统
      notifyProfileUpdated(user);
   }

   // ========== 私有方法：业务逻辑 ==========

   private String hashPassword(String password) {
      // 密码加密逻辑
      return BCrypt.hashpw(password, BCrypt.gensalt());
   }

   private void createDefaultSettings(UserDO user) {
      // 创建默认设置
   }

   private void notifyProfileUpdated(UserDO user) {
      // 通知相关系统
   }
}
```

### 关键要点

1. **✅ DataService 只负责数据访问和缓存**
   - 不包含业务逻辑
   - 不抛出业务异常（如 USER_NOT_FOUND）
   - 只返回数据或 null

2. **✅ BusinessService 只负责业务逻辑**
   - 不直接访问 Repository/Mapper
   - 通过 DataService 访问数据
   - 负责业务验证和异常处理

3. **✅ 简单查询用 JPA，复杂查询用 MyBatis**
   - 单表简单查询 → JPA Repository
   - 批量查询优化 → JPA + Redis MGET
   - 动态 SQL、多表联查 → MyBatis Mapper

4. **✅ 缓存统一在 DataService 管理**
   - 不在 Repository 加缓存
   - 不在 BusinessService 加缓存
   - 只在 DataService 层使用 @Cacheable/@CacheEvict

---

## 🚀 实施步骤

### 阶段 0: 准备工作（开始前）

#### ✅ 确认事项
- [ ] 团队成员了解 JPA 基础
- [ ] 确认当前代码已提交 Git
- [ ] 创建独立的开发分支 `feature/jpa-mybatis-migration`
- [ ] 备份数据库

---

### 阶段 1: 环境配置（1-2 天）

#### 1.1 添加 JPA 依赖

**文件**: `backend/pom.xml`

```xml
<!-- 在 dependencies 中添加 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

#### 1.2 配置 JPA 和 MyBatis 共存

**文件**: `backend/learn-api/src/main/resources/application.yml`

```yaml
spring:
  # JPA 配置
  jpa:
    show-sql: true  # 开发环境显示 SQL
    hibernate:
      ddl-auto: validate  # 只验证，不自动修改表结构
      naming:
        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl  # 保持列名原样
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQLDialect

  # MyBatis 配置（保持不变）
  mybatis:
    configuration:
      map-underscore-to-camel-case: true
```

#### 1.3 启用 JPA Auditing

**文件**: 创建 `backend/learn-persistence/src/main/java/com/prosper/learn/persistence/config/JpaConfig.java`

```java
package com.prosper.learn.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.prosper.learn.persistence.repository")
public class JpaConfig {
}
```

#### 1.4 创建 BaseEntity

**文件**: 创建 `backend/learn-persistence/src/main/java/com/prosper/learn/persistence/dataobject/BaseEntity.java`

```java
package com.prosper.learn.persistence.dataobject;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

#### 1.5 验证配置

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

**预期结果**: 启动成功，无报错

---

### 阶段 2: 试点迁移 - User 模块（3-5 天）

#### 2.1 改造 UserDO

**文件**: `backend/learn-persistence/src/main/java/com/prosper/learn/persistence/dataobject/UserDO.java`

**改造前**:
```java
@Data
public class UserDO {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**改造后**:
```java
@Entity
@Table(name = "user")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDO extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email_validated")
    private Boolean emailValidated;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    @Column(name = "msg_read_time")
    private LocalDateTime msgReadTime;

    @Column(name = "state")
    private Byte state;

    @Column(name = "role")
    private Integer role;

    // 继承自 BaseEntity:
    // - createdAt
    // - updatedAt

    // 业务方法保持不变
    public Enums.UserRole getRoleEnum() {
        return Enums.UserRole.fromCode(this.role);
    }

    public void setRoleEnum(Enums.UserRole role) {
        this.role = role != null ? role.getCode() : Enums.UserRole.USER.getCode();
    }

    public boolean hasRole(Enums.UserRole role) {
        return getRoleEnum().equalOrHigher(role);
    }
}
```

#### 2.2 创建 UserRepository

**文件**: 创建 `backend/learn-persistence/src/main/java/com/prosper/learn/persistence/repository/UserRepository.java`

```java
package com.prosper.learn.persistence.repository;

import profile.com.twicemax.user.UserDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserDO, Long> {

    // 简单查询方法（JPA 自动实现）
    Optional<UserDO> findByEmail(String email);

    Optional<UserDO> findByName(String name);

    // 其他基础 CRUD 已自动生成：
    // - save(UserDO)
    // - findById(Long)
    // - findAll()
    // - deleteById(Long)
    // 等等...
}
```

#### 2.3 保留 UserMapper 的复杂查询

**文件**: `backend/learn-persistence/src/main/java/com/prosper/learn/persistence/mapper/UserMapper.java`

```java
@Mapper
public interface UserMapper {

    // 删除简单的查询方法，保留复杂查询

    // 保留：批量查询（动态 SQL）
    @Select({"<script>SELECT * FROM user where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<UserDO> getByIds(Collection<Long> ids);

    @Select({"<script>SELECT * FROM user where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, UserDO> getMapByIds(Collection<Long> ids);

    // 保留：搜索功能
    @Select("SELECT * FROM user WHERE INSTR(name, #{name}) > 0 limit 20")
    List<UserDO> searchByName(String name);

    // 保留：分页查询
    @Select("SELECT * FROM user ORDER BY id DESC LIMIT #{count}")
    List<UserDO> getList(int count);

    @Select("SELECT * FROM user WHERE id < #{offsetId} ORDER BY id DESC LIMIT #{count}")
    List<UserDO> getListPaginated(long offsetId, int count);

    // 删除以下方法（迁移到 JPA）：
    // ❌ getById(long id) → userRepository.findById(id)
    // ❌ getByEmail(String email) → userRepository.findByEmail(email)
    // ❌ getByName(String name) → userRepository.findByName(name)
    // ❌ insert(UserDO) → userRepository.save(user)
    // ❌ update(UserDO) → userRepository.save(user)
}
```

#### 2.4 重构 UserService

**文件**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/business/UserService.java`

```java
@Service
@RequiredArgsConstructor
public class UserService {

    // 注入 JPA Repository
    private final UserRepository userRepository;

    // 注入 MyBatis Mapper（复杂查询）
    private final UserMapper userMapper;

    // 简单 CRUD 改用 JPA
    @Transactional
    public UserDO createUser(UserDO user) {
        // 时间字段自动填充，无需手动设置
        return userRepository.save(user);
    }

    public UserDO getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public UserDO getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public void updateUser(UserDO user) {
        // updatedAt 自动更新
        userRepository.save(user);
    }

    // 复杂查询继续用 MyBatis
    public List<UserDO> getUsersByIds(List<Long> ids) {
        return userMapper.getByIds(ids);
    }

    public Map<Long, UserDO> getUserMapByIds(Collection<Long> ids) {
        return userMapper.getMapByIds(ids);
    }

    public List<UserDO> searchUsersByName(String name) {
        return userMapper.searchByName(name);
    }

    public List<UserDO> getLatestUsers(int count) {
        return userMapper.getList(count);
    }
}
```

#### 2.5 测试验证

**创建测试类**: `backend/learn-domain/src/test/java/com/prosper/learn/domain/service/UserServiceTest.java`

```java
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    @Transactional
    @Rollback
    void testCreateUser_autoFillTimestamp() {
        // 创建用户
        UserDO user = new UserDO();
        user.setName("测试用户");
        user.setEmail("test@example.com");
        user.setPassword("password");

        UserDO saved = userService.createUser(user);

        // 验证时间字段自动填充
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertNotNull(saved.getId());
    }

    @Test
    void testGetUserById() {
        UserDO user = userService.getUserById(1L);
        assertNotNull(user);
    }

    @Test
    void testComplexQuery_mybatis() {
        List<UserDO> users = userService.getUsersByIds(List.of(1L, 2L, 3L));
        assertFalse(users.isEmpty());
    }
}
```

**运行测试**:
```bash
cd backend
mvn test -Dtest=UserServiceTest
```

**预期结果**: 所有测试通过

---

### 阶段 3: 批量迁移（2-3 周）

#### 迁移优先级

| 优先级 | 模块 | 复杂度 | 预计时间 |
|-------|------|-------|---------|
| P0 | User | 简单 | 已完成 |
| P0 | Course | 简单 | 1 天 |
| P1 | Post | 中等 | 1-2 天 |
| P1 | Comment | 中等 | 1-2 天 |
| P1 | Node | 简单 | 1 天 |
| P2 | MemoryCard | 复杂 | 2-3 天 |
| P2 | UserCardSrs | 复杂 | 2-3 天 |
| P3 | 其他模块 | 混合 | 1 周 |

#### 迁移步骤（标准流程）

对每个模块重复以下步骤：

1. **改造实体类**
   - 添加 JPA 注解
   - 继承 `BaseEntity`
   - 配置字段映射

2. **创建 Repository**
   - 继承 `JpaRepository`
   - 定义简单查询方法

3. **精简 Mapper**
   - 删除简单 CRUD 方法
   - 保留复杂查询

4. **重构 Service**
   - 注入 Repository 和 Mapper
   - 简单操作用 Repository
   - 复杂查询用 Mapper

5. **编写测试**
   - 测试 CRUD 功能
   - 测试时间自动填充
   - 测试复杂查询

6. **运行回归测试**
   - 确保功能不变
   - 检查性能

---

### 阶段 4: 测试与优化（1 周）

#### 4.1 功能测试

- [ ] 所有 API 接口功能正常
- [ ] 时间字段自动填充正确
- [ ] 复杂查询结果正确
- [ ] 事务管理正常

#### 4.2 性能测试

**对比测试**:
```java
// 测试简单 CRUD 性能
@Test
void performanceTest() {
    long start = System.currentTimeMillis();
    for (int i = 0; i < 1000; i++) {
        userRepository.findById(1L);
    }
    long jpaTime = System.currentTimeMillis() - start;

    start = System.currentTimeMillis();
    for (int i = 0; i < 1000; i++) {
        userMapper.getById(1L);
    }
    long mybatisTime = System.currentTimeMillis() - start;

    System.out.println("JPA: " + jpaTime + "ms");
    System.out.println("MyBatis: " + mybatisTime + "ms");
}
```

**性能指标**:
- 简单查询：JPA 和 MyBatis 性能相近（±10%）
- 复杂查询：MyBatis 通常更快（手动优化）

#### 4.3 常见问题解决

##### 问题 1: N+1 查询

**症状**: 日志中出现大量 SQL 查询

**解决方案**: 使用 `@EntityGraph` 或 `JOIN FETCH`

```java
@EntityGraph(attributePaths = {"courses", "posts"})
UserDO findByIdWithRelations(Long id);
```

##### 问题 2: 事务边界

**症状**: JPA 和 MyBatis 事务不一致

**解决方案**: 统一使用 `@Transactional` 在 Service 层

```java
@Transactional
public void complexOperation() {
    userRepository.save(user);      // JPA
    userMapper.updateStats(userId);  // MyBatis
    // 统一在同一个事务中
}
```

##### 问题 3: 字段映射不一致

**症状**: 部分字段查询结果为 null

**解决方案**: 检查 `@Column(name = "...")` 配置

---

## ✅ 验收标准

### 功能验收
- [ ] 所有原有功能正常工作
- [ ] 新增用户时，`createdAt` 和 `updatedAt` 自动填充
- [ ] 更新用户时，`updatedAt` 自动更新
- [ ] 复杂查询结果与之前一致

### 性能验收
- [ ] 简单 CRUD 性能无明显下降（±10%）
- [ ] 复杂查询性能保持不变

### 代码质量
- [ ] 所有单元测试通过
- [ ] 代码覆盖率 > 80%
- [ ] 无 SonarQube 严重问题

---

## 🔄 回滚方案

如果迁移出现严重问题，可以快速回滚：

### 回滚步骤

1. **代码回滚**
   ```bash
   git checkout main
   git branch -D feature/jpa-mybatis-migration
   ```

2. **依赖回滚**
   - 移除 `spring-boot-starter-data-jpa` 依赖
   - 恢复原有配置文件

3. **数据库无需回滚**
   - 因为使用 `ddl-auto: validate`，未修改表结构

---

## 📚 参考资料

### JPA 文档
- [Spring Data JPA 官方文档](https://spring.io/projects/spring-data-jpa)
- [JPA Auditing](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#auditing)

### MyBatis 文档
- [MyBatis 官方文档](https://mybatis.org/mybatis-3/)
- [MyBatis-Spring-Boot-Starter](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)

### 最佳实践
- [JPA + MyBatis 共存方案](https://www.baeldung.com/spring-boot-jpa-mybatis)
- [Spring Boot 多数据源配置](https://www.baeldung.com/spring-data-jpa-multiple-databases)

---

## 🤝 团队协作

### 分工建议
- **后端负责人**: 配置环境、制定标准
- **开发人员 A**: User、Course 模块迁移
- **开发人员 B**: Post、Comment 模块迁移
- **测试人员**: 编写测试用例、回归测试

### 沟通机制
- **每日站会**: 同步进度、解决问题
- **代码审查**: 所有迁移代码需经过 Review
- **问题追踪**: 使用 Issue 跟踪迁移问题

---

## 📊 进度跟踪

参见 `backend/docs/jpa-mybatis-migration-checklist.md`
