# MyBatis Plus 迁移指南

## 📚 目录

1. [环境准备](#1-环境准备)
2. [特性1：自动填充时间字段](#2-特性1自动填充时间字段)
3. [特性2：BaseMapper 消除基础 CRUD](#3-特性2basemapper-消除基础-crud)
4. [特性3：LambdaQueryWrapper 条件构造器](#4-特性3lambdaquerywrapper-条件构造器)
5. [特性4：逻辑删除](#5-特性4逻辑删除)
6. [特性5：分页插件](#6-特性5分页插件)
7. [迁移检查清单](#7-迁移检查清单)

---

## 1. 环境准备

### 1.1 添加依赖

**修改文件**: `backend/pom.xml`

**替换前**:
```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>
```

**替换后**:
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.7</version>
</dependency>
```

**说明**:
- MyBatis Plus 会自动引入 MyBatis 依赖，无需保留原 mybatis-spring-boot-starter
- 版本 3.5.7 支持 Spring Boot 3.x

### 1.2 配置文件调整

**修改文件**: `backend/learn-api/src/main/resources/application.yml` (或 application.properties)

**添加配置**:
```yaml
mybatis-plus:
  # 指定 Mapper XML 文件位置（如果有）
  mapper-locations: classpath*:/mapper/**/*.xml

  # 全局配置
  global-config:
    db-config:
      # 主键类型：AUTO 表示数据库自增
      id-type: AUTO
      # 逻辑删除字段名
      logic-delete-field: deletedAt
      # 逻辑删除值（删除后的值）
      logic-delete-value: NOW()
      # 逻辑未删除值
      logic-not-delete-value: "NULL"

  # 配置日志（开发环境）
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # 驼峰转下划线
    map-underscore-to-camel-case: true
```

### 1.3 验证安装

**运行测试**:
```bash
cd backend
mvn clean install
```

**预期结果**:
- ✅ 编译成功
- ✅ 无依赖冲突
- ✅ 现有测试通过

---

## 2. 特性1：自动填充时间字段

### 2.1 功能说明

**问题**:
- 当前 INSERT 时 `created_at` 和 `updated_at` 依赖数据库 DEFAULT CURRENT_TIMESTAMP
- UPDATE 时 `updated_at` 依赖数据库 ON UPDATE CURRENT_TIMESTAMP
- 无法在程序中控制时间，不利于测试和时区管理

**解决方案**:
- 使用 MyBatis Plus 的 `MetaObjectHandler` 自动填充
- 在 Java 代码中设置时间字段，完全控制

### 2.2 实现步骤

#### 步骤 1: 创建自动填充处理器

**创建文件**: `backend/learn-persistence/src/main/java/com/prosper/learn/persistence/handler/MyMetaObjectHandler.java`

```java
package com.prosper.learn.persistence.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 自动填充处理器
 * 自动填充 created_at 和 updated_at 字段
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");

        // 严格模式填充：只在字段为 null 时填充
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");

        // 严格模式填充：只在字段为 null 时填充
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

**关键点**:
- `@Component`: 注册为 Spring Bean，自动生效
- `strictInsertFill`: 只在字段为 null 时填充（不会覆盖已设置的值）
- `LocalDateTime.now()`: 使用服务器时间

#### 步骤 2: 标注实体字段

**修改文件**: `backend/learn-persistence/src/main/java/com/prosper/learn/persistence/dataobject/PostDO.java`

**修改前**:
```java
@Data
public class PostDO {
    private Long id;
    private Long nodeId;
    // ...
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**修改后**:
```java
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

@Data
public class PostDO {
    private Long id;
    private Long nodeId;
    // ...

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

**注解说明**:
- `FieldFill.INSERT`: 仅在插入时填充（createdAt）
- `FieldFill.INSERT_UPDATE`: 插入和更新时都填充（updatedAt）

#### 步骤 3: 批量修改所有 DO

**需要修改的文件列表**:
```
backend/learn-persistence/src/main/java/com/prosper/learn/persistence/dataobject/
├── UserDO.java
├── PostDO.java
├── CommentDO.java
├── CourseDO.java
├── NodeDO.java
├── RoadmapDO.java
├── ProfessionDO.java
├── MessageDO.java
├── OperationLogDO.java
└── ... (所有包含 createdAt/updatedAt 的 DO)
```

**批量修改模板**:
```java
// 在每个 DO 文件头部添加 import
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

// 在 createdAt 字段上添加
@TableField(fill = FieldFill.INSERT)
private LocalDateTime createdAt;

// 在 updatedAt 字段上添加
@TableField(fill = FieldFill.INSERT_UPDATE)
private LocalDateTime updatedAt;
```

### 2.3 测试验证

#### 测试用例 1: 插入数据

```java
@SpringBootTest
class AutoFillTest {

    @Autowired
    private PostMapper postMapper;

    @Test
    void testInsertAutoFill() {
        PostDO post = new PostDO();
        post.setNodeId(1L);
        post.setCreatorId(1L);
        post.setContent("测试内容");

        // 插入前 createdAt 和 updatedAt 为 null
        assertNull(post.getCreatedAt());
        assertNull(post.getUpdatedAt());

        postMapper.insert(post);

        // 插入后自动填充
        assertNotNull(post.getCreatedAt());
        assertNotNull(post.getUpdatedAt());
        assertEquals(post.getCreatedAt(), post.getUpdatedAt());
    }
}
```

#### 测试用例 2: 更新数据

```java
@Test
void testUpdateAutoFill() throws InterruptedException {
    // 先插入
    PostDO post = new PostDO();
    post.setNodeId(1L);
    post.setCreatorId(1L);
    post.setContent("原始内容");
    postMapper.insert(post);

    LocalDateTime createdAt = post.getCreatedAt();
    LocalDateTime updatedAt = post.getUpdatedAt();

    // 等待 1 秒确保时间不同
    Thread.sleep(1000);

    // 更新
    post.setContent("更新后的内容");
    postMapper.update(post);

    // 验证：createdAt 不变，updatedAt 更新
    assertEquals(createdAt, post.getCreatedAt());
    assertTrue(post.getUpdatedAt().isAfter(updatedAt));
}
```

### 2.4 注意事项

**⚠️ 重要**:
1. **Mapper 无需修改**: 现有的 `@Insert` 和 `@Update` 注解继续使用，自动填充会自动生效
2. **手动设置优先**: 如果代码中手动设置了时间，自动填充不会覆盖（strictFill 模式）
3. **数据库兼容**: 保留数据库的 DEFAULT CURRENT_TIMESTAMP 作为兜底（防止直接 SQL 插入）

**✅ 改造完成标志**:
- 所有 DO 添加了 `@TableField` 注解
- 插入/更新操作自动设置时间字段
- 测试通过

---

## 3. 特性2：BaseMapper 消除基础 CRUD

### 3.1 功能说明

**问题**:
- 27 个 Mapper 都手写了相同的基础方法：
  - `getById(Long id)`
  - `getByIds(List<Long> ids)`
  - `insert(DO entity)`
  - `update(DO entity)`
  - `delete(Long id)`

**解决方案**:
- 继承 `BaseMapper<T>` 接口，自动获得 17 个常用方法
- 删除重复代码，只保留业务特定查询

### 3.2 BaseMapper 提供的方法

| 方法 | 说明 | 替代现有方法 |
|------|------|--------------|
| `selectById(Serializable id)` | 根据 ID 查询 | `getById(long id)` |
| `selectBatchIds(Collection<? extends Serializable> ids)` | 批量查询 | `getByIds(List<Long> ids)` |
| `selectList(Wrapper<T> wrapper)` | 条件查询 | 自定义 @Select |
| `selectOne(Wrapper<T> wrapper)` | 单个条件查询 | 自定义 @Select |
| `selectCount(Wrapper<T> wrapper)` | 统计查询 | 自定义 @Select COUNT |
| `insert(T entity)` | 插入 | `insert(DO entity)` |
| `updateById(T entity)` | 根据 ID 更新 | `update(DO entity)` |
| `update(T entity, Wrapper<T> wrapper)` | 条件更新 | 自定义 @Update |
| `deleteById(Serializable id)` | 根据 ID 删除 | `delete(long id)` |
| `deleteBatchIds(Collection<? extends Serializable> ids)` | 批量删除 | 无 |

### 3.3 实现步骤

#### 步骤 1: 配置实体表映射

**修改文件**: `backend/learn-persistence/src/main/java/com/prosper/learn/persistence/dataobject/PostDO.java`

**添加注解**:
```java
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

@Data
@TableName("post")  // 指定表名
public class PostDO {

    @TableId(value = "id", type = IdType.AUTO)  // 主键自增
    private Long id;

    private Long nodeId;
    private Long creatorId;
    // ...
}
```

**注解说明**:
- `@TableName("post")`: 指定数据库表名（如果类名和表名一致可省略）
- `@TableId`: 标注主键字段
- `IdType.AUTO`: 使用数据库自增主键

#### 步骤 2: 改造 Mapper 接口

**修改文件**: `backend/learn-persistence/src/main/java/com/prosper/learn/persistence/mapper/PostMapper.java`

**改造前**:
```java
public interface PostMapper {

    @Select("SELECT * FROM post WHERE id = #{id} AND deleted_at IS NULL")
    PostDO get(long id);

    @Select({"<script>SELECT * FROM post where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            " AND deleted_at IS NULL</script>"})
    List<PostDO> getByIds(List<Long> ids);

    @Insert("INSERT INTO post (node_id, creator_id, type, content, state) " +
            "VALUES (#{nodeId}, #{creatorId}, #{type}, #{content}, #{state})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PostDO posting);

    @Update("UPDATE post SET " +
            "node_id = #{nodeId}, content = #{content}, twice = #{twice}, helpful = #{helpful}, " +
            "comment_count=#{commentCount}, view_count=#{viewCount}, state=#{state} where id = #{id}")
    void update(PostDO posting);

    // 保留业务特定查询
    @Select("SELECT * FROM post WHERE node_id = #{nodeId} ...")
    List<PostDO> getListByNode(long nodeId, int limit, byte state);

    // ... 其他业务方法
}
```

**改造后**:
```java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface PostMapper extends BaseMapper<PostDO> {
    // ✅ 删除以下方法，BaseMapper 已提供：
    // - get(long id) → selectById(Long id)
    // - getByIds(List<Long> ids) → selectBatchIds(Collection ids)
    // - insert(PostDO) → insert(T entity)
    // - update(PostDO) → updateById(T entity)

    // ✅ 保留业务特定查询
    @Select("SELECT * FROM post WHERE node_id = #{nodeId} and state = #{state} AND deleted_at IS NULL " +
            "order by created_at desc limit #{limit}")
    List<PostDO> getListByNode(long nodeId, int limit, byte state);

    @Select("SELECT * FROM post WHERE node_id = #{nodeId} and id < #{lastId} and state = #{state} AND deleted_at IS NULL " +
            "order by id desc limit #{limit}")
    List<PostDO> getListByLastId(long nodeId, long lastId, int limit, byte state);

    // ... 其他复杂业务查询
}
```

#### 步骤 3: 修改 DataService 调用

**修改文件**: `backend/learn-domain/src/main/java/com/prosper/learn/domain/service/data/PostDataService.java`

**改造前**:
```java
@Override
protected PostDO getByIdFromMapper(PostMapper mapper, Long id) {
    return mapper.get(id);  // 老方法名
}

@Override
protected List<PostDO> getByIdsFromMapper(PostMapper mapper, Collection<Long> ids) {
    return mapper.getByIds(ids.stream().collect(Collectors.toList()));
}

public void insert(PostDO post) {
    postMapper.insert(post);
}
```

**改造后**:
```java
@Override
protected PostDO getByIdFromMapper(PostMapper mapper, Long id) {
    return mapper.selectById(id);  // ✅ BaseMapper 方法
}

@Override
protected List<PostDO> getByIdsFromMapper(PostMapper mapper, Collection<Long> ids) {
    return mapper.selectBatchIds(ids);  // ✅ BaseMapper 方法
}

public void insert(PostDO post) {
    postMapper.insert(post);  // ✅ 方法名不变，但实际调用 BaseMapper
}
```

### 3.4 迁移检查清单

**每个 Mapper 需要检查**:

- [ ] 添加 `extends BaseMapper<DO>`
- [ ] 删除 `getById` / `get` 方法 → 改用 `selectById`
- [ ] 删除 `getByIds` 方法 → 改用 `selectBatchIds`
- [ ] 删除基础 `insert` 方法（保留复杂插入）
- [ ] 删除基础 `update` 方法（保留复杂更新）
- [ ] 删除基础 `delete` 方法 → 改用 `deleteById`
- [ ] 保留所有业务特定查询（如 `getListByNode`）

**每个 DO 需要检查**:

- [ ] 添加 `@TableName` 注解
- [ ] 添加 `@TableId(type = IdType.AUTO)` 注解
- [ ] 如果字段名与数据库列名不一致，添加 `@TableField("column_name")`

### 3.5 测试验证

```java
@Test
void testBaseMapperMethods() {
    // 测试 selectById
    PostDO post = postMapper.selectById(1L);
    assertNotNull(post);

    // 测试 selectBatchIds
    List<PostDO> posts = postMapper.selectBatchIds(Arrays.asList(1L, 2L, 3L));
    assertEquals(3, posts.size());

    // 测试 insert
    PostDO newPost = new PostDO();
    newPost.setContent("测试");
    postMapper.insert(newPost);
    assertNotNull(newPost.getId());

    // 测试 updateById
    newPost.setContent("更新");
    postMapper.updateById(newPost);

    // 测试 deleteById
    postMapper.deleteById(newPost.getId());
}
```

### 3.6 注意事项

**⚠️ 兼容性**:
- 保留现有的 `@Select/@Insert/@Update` 方法，与 BaseMapper 方法共存
- 逐步迁移，不要一次性删除所有自定义方法

**✅ 改造完成标志**:
- 所有 Mapper 继承 `BaseMapper`
- 基础 CRUD 方法已删除
- DataService 调用改为 BaseMapper 方法
- 测试通过

---

## 4. 特性3：LambdaQueryWrapper 条件构造器

### 4.1 功能说明

**问题**:
- 动态 SQL 使用 `<script>` 和 `<if>` 标签，XML 风格冗长
- 字符串硬编码字段名，重构时容易遗漏
- 不支持 IDE 自动补全和类型检查

**解决方案**:
- 使用 `LambdaQueryWrapper` 构建查询条件
- 类型安全，支持 Lambda 表达式
- 支持链式调用，代码简洁

### 4.2 常用方法对照表

| SQL 条件 | MyBatis 写法 | LambdaQueryWrapper 写法 |
|----------|--------------|-------------------------|
| `WHERE id = ?` | `<if test='id != null'> AND id = #{id}</if>` | `.eq(id != null, Post::getId, id)` |
| `WHERE id < ?` | `AND id < #{id}` | `.lt(Post::getId, id)` |
| `WHERE id IN (?)` | `<foreach>...</foreach>` | `.in(Post::getId, ids)` |
| `WHERE name LIKE ?` | `LIKE CONCAT('%', #{name}, '%')` | `.like(Post::getName, name)` |
| `ORDER BY id DESC` | `ORDER BY id DESC` | `.orderByDesc(Post::getId)` |
| `LIMIT ?` | `LIMIT #{limit}` | `.last("LIMIT " + limit)` |

### 4.3 实现步骤

#### 示例 1: 简单条件查询

**改造前**: `CommentMapper.java`
```java
@Select({"<script>",
    "SELECT * FROM comment",
    "<where>",
    "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
    "<if test='objectType != null'> AND object_type = #{objectType}</if>",
    "<if test='objectId != null'> AND object_id = #{objectId}</if>",
    "<if test='creatorId != null'> AND creator_id = #{creatorId}</if>",
    "<if test='state != null'> AND state = #{state}</if>",
    "</where>",
    "ORDER BY id DESC LIMIT #{limit}",
    "</script>"})
List<CommentDO> getListByFilter(@Param("objectType") Integer objectType,
                                @Param("objectId") Long objectId,
                                @Param("creatorId") Long creatorId,
                                @Param("lastId") Long lastId,
                                @Param("state") Byte state,
                                @Param("limit") int limit);
```

**改造后**: 删除 Mapper 方法，在 Service 中使用
```java
// CommentDataService.java
public List<CommentDO> getListByFilter(Integer objectType, Long objectId,
                                       Long creatorId, Long lastId,
                                       Byte state, int limit) {
    LambdaQueryWrapper<CommentDO> wrapper = new LambdaQueryWrapper<>();
    wrapper.lt(lastId != null, CommentDO::getId, lastId)
           .eq(objectType != null, CommentDO::getObjectType, objectType)
           .eq(objectId != null, CommentDO::getObjectId, objectId)
           .eq(creatorId != null, CommentDO::getCreatorId, creatorId)
           .eq(state != null, CommentDO::getState, state)
           .orderByDesc(CommentDO::getId)
           .last("LIMIT " + limit);

    return commentMapper.selectList(wrapper);
}
```

**优点**:
- ✅ 类型安全：`CommentDO::getId` 是方法引用，重构时自动更新
- ✅ 条件判断：第一个参数是条件，false 时不加入 WHERE
- ✅ 链式调用：代码简洁易读

#### 示例 2: 复杂排序和分页

**改造前**: `PostMapper.java`
```java
@Select("SELECT * FROM post WHERE node_id = #{nodeId} AND state = #{state} AND deleted_at IS NULL AND " +
        "(score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
        "ORDER BY score DESC, id DESC LIMIT #{limit}")
List<PostDO> getListByNodeAndScoreAndPaginated(long nodeId, double lastScore, long lastId, int limit, byte state);
```

**改造后**:
```java
// PostDataService.java
public List<PostDO> getListByNodeAndScoreAndPaginated(Long nodeId, double lastScore,
                                                       Long lastId, int limit, Byte state) {
    LambdaQueryWrapper<PostDO> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(PostDO::getNodeId, nodeId)
           .eq(PostDO::getState, state)
           .isNull(PostDO::getDeletedAt)
           .and(w -> w.lt(PostDO::getScore, lastScore)
                      .or()
                      .nested(w2 -> w2.eq(PostDO::getScore, lastScore)
                                      .lt(PostDO::getId, lastId)))
           .orderByDesc(PostDO::getScore, PostDO::getId)
           .last("LIMIT " + limit);

    return postMapper.selectList(wrapper);
}
```

#### 示例 3: IN 查询

**改造前**: `PostMapper.java`
```java
@Select({"<script>SELECT * FROM post where id in " +
        "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
        " AND deleted_at IS NULL</script>"})
List<PostDO> getByIds(List<Long> ids);
```

**改造后**:
```java
// 直接使用 BaseMapper 的 selectBatchIds
List<PostDO> posts = postMapper.selectBatchIds(ids);

// 如果需要额外条件
LambdaQueryWrapper<PostDO> wrapper = new LambdaQueryWrapper<>();
wrapper.in(PostDO::getId, ids)
       .isNull(PostDO::getDeletedAt);
List<PostDO> posts = postMapper.selectList(wrapper);
```

### 4.4 迁移策略

**建议顺序**:
1. ✅ **保留复杂业务查询**: 如多表关联、子查询、使用 `@Select` 保持不变
2. ✅ **改造简单动态查询**: 如多条件筛选、简单分页
3. ✅ **逐步优化**: 不要一次性改造所有查询

**需要改造的场景**:
- [ ] 使用 `<script>` 和 `<if>` 的动态 SQL
- [ ] 简单的 WHERE 条件组合
- [ ] 需要类型安全的查询

**保留原有写法的场景**:
- [ ] 复杂子查询
- [ ] 多表 JOIN
- [ ] 性能敏感的查询（可先保留，后续优化）

### 4.5 测试验证

```java
@Test
void testLambdaQueryWrapper() {
    // 测试单条件
    LambdaQueryWrapper<PostDO> wrapper1 = new LambdaQueryWrapper<>();
    wrapper1.eq(PostDO::getNodeId, 1L);
    List<PostDO> posts = postMapper.selectList(wrapper1);

    // 测试多条件
    LambdaQueryWrapper<PostDO> wrapper2 = new LambdaQueryWrapper<>();
    wrapper2.eq(PostDO::getNodeId, 1L)
            .eq(PostDO::getState, (byte) 1)
            .orderByDesc(PostDO::getCreatedAt)
            .last("LIMIT 10");
    List<PostDO> posts2 = postMapper.selectList(wrapper2);

    // 测试条件判断
    Long lastId = null;
    LambdaQueryWrapper<PostDO> wrapper3 = new LambdaQueryWrapper<>();
    wrapper3.lt(lastId != null, PostDO::getId, lastId)  // lastId=null，此条件不生效
            .eq(PostDO::getState, (byte) 1);
    List<PostDO> posts3 = postMapper.selectList(wrapper3);
}
```

### 4.6 注意事项

**⚠️ 性能**:
- 简单查询性能与原生 SQL 一致
- 复杂查询建议先用 `@Select` 保留，确认性能后再改造

**⚠️ 可读性**:
- 嵌套条件过多时，使用 `.and()` 和 `.or()` 分组
- 可以提取为方法，提高复用性

**✅ 改造完成标志**:
- 简单动态查询已改为 LambdaQueryWrapper
- 代码通过 IDE 类型检查
- 测试通过

---

## 5. 特性4：逻辑删除

### 5.1 功能说明

**问题**:
- 当前使用 `deleted_at` 字段软删除
- 每个 SELECT 都需要手动加 `AND deleted_at IS NULL`
- 容易遗漏，导致查询到已删除数据

**解决方案**:
- 使用 `@TableLogic` 注解标注逻辑删除字段
- 所有查询自动过滤 `deleted_at IS NOT NULL` 的数据
- `deleteById` 自动执行 `UPDATE ... SET deleted_at = NOW()`

### 5.2 实现步骤

#### 步骤 1: 标注逻辑删除字段

**修改文件**: `backend/learn-persistence/src/main/java/com/prosper/learn/persistence/dataobject/PostDO.java`

**添加注解**:
```java
import com.baomidou.mybatisplus.annotation.TableLogic;

@Data
@TableName("post")
public class PostDO {
    private Long id;
    // ...

    @TableLogic(value = "NULL", delval = "NOW()")  // 未删除=NULL，已删除=NOW()
    private LocalDateTime deletedAt;
}
```

**注解说明**:
- `value = "NULL"`: 逻辑未删除的值（数据库中为 NULL）
- `delval = "NOW()"`: 逻辑删除时设置的值（当前时间）

#### 步骤 2: 配置全局逻辑删除

**修改文件**: `backend/learn-api/src/main/resources/application.yml`

```yaml
mybatis-plus:
  global-config:
    db-config:
      # 全局逻辑删除字段名
      logic-delete-field: deletedAt
      # 逻辑删除值
      logic-delete-value: NOW()
      # 逻辑未删除值
      logic-not-delete-value: "NULL"
```

#### 步骤 3: 改造 Mapper 查询

**改造前**: `PostMapper.java`
```java
@Select("SELECT * FROM post WHERE id = #{id} AND deleted_at IS NULL")
PostDO get(long id);

@Select("SELECT * FROM post WHERE node_id = #{nodeId} AND deleted_at IS NULL " +
        "ORDER BY created_at DESC LIMIT #{limit}")
List<PostDO> getListByNode(long nodeId, int limit, byte state);

@Update("UPDATE post SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
int softDelete(long id);
```

**改造后**:
```java
// ✅ 删除所有 "AND deleted_at IS NULL" 条件
@Select("SELECT * FROM post WHERE id = #{id}")  // 自动过滤已删除
PostDO get(long id);

@Select("SELECT * FROM post WHERE node_id = #{nodeId} " +
        "ORDER BY created_at DESC LIMIT #{limit}")  // 自动过滤已删除
List<PostDO> getListByNode(long nodeId, int limit, byte state);

// ✅ 删除 softDelete 方法，使用 BaseMapper.deleteById
// postMapper.deleteById(id) 会自动执行 UPDATE ... SET deleted_at = NOW()
```

#### 步骤 4: 改造 DataService

**改造前**: `PostDataService.java`
```java
@CacheEvict(value = "posts", key = "#id")
public int softDelete(long id) {
    return postMapper.softDelete(id);
}
```

**改造后**:
```java
@CacheEvict(value = "posts", key = "#id")
public int softDelete(long id) {
    return postMapper.deleteById(id);  // ✅ 自动执行软删除
}
```

### 5.3 批量改造清单

**需要修改的 DO**:
- [ ] PostDO
- [ ] CommentDO
- [ ] CourseDO
- [ ] NodeDO
- [ ] ... (所有包含 deletedAt 字段的 DO)

**需要删除的代码**:
- [ ] 所有 SELECT 中的 `AND deleted_at IS NULL`
- [ ] 所有自定义的 `softDelete` 方法
- [ ] 所有 `UPDATE ... SET deleted_at = NOW()` 语句

### 5.4 特殊场景处理

#### 场景 1: 查询包含已删除数据

```java
// 使用 LambdaQueryWrapper，不自动过滤逻辑删除
LambdaQueryWrapper<PostDO> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(PostDO::getId, id)
       .apply("1=1");  // 强制不过滤逻辑删除

// 或者手动查询所有数据（包括已删除）
@Select("SELECT * FROM post WHERE id = #{id}")  // 不使用 BaseMapper，不会自动过滤
PostDO getIncludingDeleted(long id);
```

#### 场景 2: 物理删除

```java
// 使用原生 SQL 执行物理删除
@Delete("DELETE FROM post WHERE id = #{id}")
int physicalDelete(long id);
```

### 5.5 测试验证

```java
@Test
void testLogicDelete() {
    // 插入数据
    PostDO post = new PostDO();
    post.setContent("测试");
    postMapper.insert(post);
    Long id = post.getId();

    // 验证可以查询到
    assertNotNull(postMapper.selectById(id));

    // 执行逻辑删除
    postMapper.deleteById(id);

    // 验证查询不到（自动过滤）
    assertNull(postMapper.selectById(id));

    // 验证数据库中数据仍存在，只是 deleted_at 不为 NULL
    PostDO deletedPost = postMapper.selectOne(
        new LambdaQueryWrapper<PostDO>()
            .eq(PostDO::getId, id)
            .apply("1=1")  // 强制查询已删除数据
    );
    assertNotNull(deletedPost);
    assertNotNull(deletedPost.getDeletedAt());
}
```

### 5.6 注意事项

**⚠️ 重要**:
- 逻辑删除只对 MyBatis Plus 方法生效（`selectById`, `selectList` 等）
- 自定义 `@Select` 仍需手动处理（如果不想自动过滤，保留 `@Select`）
- 关联查询需要在 JOIN 中手动加 `AND deleted_at IS NULL`

**✅ 改造完成标志**:
- 所有 DO 添加了 `@TableLogic` 注解
- Mapper 中删除了所有 `AND deleted_at IS NULL`
- 测试通过

---

## 6. 特性5：分页插件

### 6.1 功能说明

**问题**:
- 当前使用 `LIMIT offset, size` 手动分页
- 需要单独查询总数 `COUNT(*)`
- 代码重复，容易出错

**解决方案**:
- 使用 MyBatis Plus 分页插件
- 自动统计总数，自动处理分页参数
- 支持多数据库方言

### 6.2 实现步骤

#### 步骤 1: 配置分页插件

**创建文件**: `backend/learn-persistence/src/main/java/com/prosper/learn/persistence/config/MybatisPlusConfig.java`

```java
package com.prosper.learn.persistence.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);

        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInterceptor.setMaxLimit(500L);

        // 溢出总页数后是否进行处理（默认不处理）
        paginationInterceptor.setOverflow(false);

        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}
```

#### 步骤 2: 使用分页查询

**改造前**: `CourseMapper.java`
```java
@Select("SELECT * FROM course WHERE state = #{state.value} AND creator_id = #{creatorId} " +
        "ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
List<CourseDO> list(ContentState state, long creatorId, int limit, int offset);

// 需要单独查询总数
@Select("SELECT COUNT(*) FROM course WHERE state = #{state.value} AND creator_id = #{creatorId}")
Long countByStateAndCreator(ContentState state, long creatorId);
```

**改造后**:
```java
// Mapper 中定义分页方法（返回 Page 对象）
@Select("SELECT * FROM course WHERE state = #{state.value} AND creator_id = #{creatorId} " +
        "ORDER BY created_at DESC")
Page<CourseDO> listPage(Page<CourseDO> page, @Param("state") ContentState state, @Param("creatorId") long creatorId);

// 或者使用 LambdaQueryWrapper
// Service 中调用
public Page<CourseDO> listPage(ContentState state, long creatorId, int pageNum, int pageSize) {
    Page<CourseDO> page = new Page<>(pageNum, pageSize);

    LambdaQueryWrapper<CourseDO> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(CourseDO::getState, state.getValue())
           .eq(CourseDO::getCreatorId, creatorId)
           .orderByDesc(CourseDO::getCreatedAt);

    Page<CourseDO> result = courseMapper.selectPage(page, wrapper);

    // 返回结果包含：
    // result.getRecords()  - 当前页数据
    // result.getTotal()    - 总记录数（自动查询）
    // result.getPages()    - 总页数
    // result.getCurrent()  - 当前页码
    // result.getSize()     - 每页大小

    return result;
}
```

#### 步骤 3: 改造 Controller 返回分页结果

**创建分页 DTO**: `backend/learn-dto/src/main/java/com/prosper/learn/dto/PageResult.java`

```java
package com.prosper.learn.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;      // 当前页数据
    private Long total;           // 总记录数
    private Long pages;           // 总页数
    private Long current;         // 当前页码
    private Long size;            // 每页大小

    public static <T> PageResult<T> from(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }
}
```

**Controller 使用**:
```java
@GetMapping("/courses")
public ResponseData<PageResult<CourseDTO>> listCourses(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {

    Page<CourseDO> coursePage = courseService.listPage(ContentState.PUBLISHED, userId, page, size);

    // 转换为 DTO
    PageResult<CourseDTO> result = PageResult.from(coursePage)
        .map(courseConverter::toDTO);  // 需要自己实现 map 方法

    return ResponseData.success(result);
}
```

### 6.3 特殊场景：游标分页（保留）

**说明**:
- 游标分页（如 `id < lastId`）性能更好，适合移动端无限滚动
- 不需要改造，保留现有实现

**保留场景**:
```java
// 保留此类查询，性能优于 offset 分页
@Select("SELECT * FROM post WHERE id < #{lastId} ORDER BY id DESC LIMIT #{limit}")
List<PostDO> getListByLastId(long lastId, int limit);
```

### 6.4 测试验证

```java
@Test
void testPagination() {
    // 准备测试数据：插入 100 条数据
    for (int i = 0; i < 100; i++) {
        PostDO post = new PostDO();
        post.setContent("测试" + i);
        postMapper.insert(post);
    }

    // 测试第1页
    Page<PostDO> page1 = new Page<>(1, 20);
    Page<PostDO> result1 = postMapper.selectPage(page1, null);
    assertEquals(20, result1.getRecords().size());
    assertEquals(100, result1.getTotal());
    assertEquals(5, result1.getPages());

    // 测试第2页
    Page<PostDO> page2 = new Page<>(2, 20);
    Page<PostDO> result2 = postMapper.selectPage(page2, null);
    assertEquals(20, result2.getRecords().size());

    // 验证数据不重复
    Long firstIdOfPage2 = result2.getRecords().get(0).getId();
    Long lastIdOfPage1 = result1.getRecords().get(19).getId();
    assertNotEquals(firstIdOfPage2, lastIdOfPage1);
}
```

### 6.5 注意事项

**⚠️ 性能**:
- `COUNT(*)` 查询会增加开销，如果不需要总数，使用游标分页
- 大表分页建议使用游标分页（`id < lastId`）

**✅ 改造完成标志**:
- 分页插件配置完成
- 需要总数的分页场景已改造
- 游标分页场景保留
- 测试通过

---

## 7. 迁移检查清单

### 7.1 环境配置

- [ ] 替换 `mybatis-spring-boot-starter` 为 `mybatis-plus-boot-starter`
- [ ] 添加 `application.yml` 配置
- [ ] 创建 `MybatisPlusConfig` 配置类
- [ ] 项目编译通过

### 7.2 特性1：自动填充时间字段

- [ ] 创建 `MyMetaObjectHandler` 处理器
- [ ] 所有 DO 添加 `@TableField(fill = FieldFill.INSERT/INSERT_UPDATE)` 注解
- [ ] 测试插入和更新操作
- [ ] 验证时间字段自动填充

### 7.3 特性2：BaseMapper

- [ ] 所有 DO 添加 `@TableName` 和 `@TableId` 注解
- [ ] 所有 Mapper 继承 `BaseMapper<DO>`
- [ ] 删除重复的基础 CRUD 方法
- [ ] 修改 DataService 调用为 BaseMapper 方法
- [ ] 测试基础 CRUD 功能

### 7.4 特性3：LambdaQueryWrapper

- [ ] 识别可改造的动态查询
- [ ] 改造简单条件查询为 LambdaQueryWrapper
- [ ] 保留复杂查询的 @Select 注解
- [ ] 测试查询功能

### 7.5 特性4：逻辑删除

- [ ] 所有 DO 添加 `@TableLogic` 注解
- [ ] 删除所有 `AND deleted_at IS NULL` 条件
- [ ] 删除自定义 `softDelete` 方法
- [ ] 测试逻辑删除和查询过滤

### 7.6 特性5：分页插件

- [ ] 配置 `PaginationInnerInterceptor`
- [ ] 改造需要总数的分页查询
- [ ] 保留游标分页查询
- [ ] 创建 `PageResult` DTO
- [ ] 测试分页功能

### 7.7 最终验证

- [ ] 运行所有单元测试
- [ ] 运行集成测试
- [ ] 手动测试核心功能
- [ ] 检查日志输出的 SQL 语句
- [ ] 性能测试（可选）

---

## 8. 常见问题

### Q1: MyBatis Plus 会影响现有的 @Select/@Insert 注解吗？

**A**: 不会。MyBatis Plus 完全兼容原生 MyBatis 注解，可以共存。

### Q2: 是否需要一次性改造所有 Mapper？

**A**: 不需要。建议渐进式迁移：
1. 先添加依赖和配置
2. 新功能使用 MyBatis Plus
3. 逐步重构现有代码

### Q3: LambdaQueryWrapper 性能如何？

**A**: 性能与原生 SQL 一致，MyBatis Plus 只是在编译时生成 SQL，运行时无额外开销。

### Q4: 逻辑删除会影响性能吗？

**A**: 会在每个查询自动加 `WHERE deleted_at IS NULL`，对性能影响极小。建议在 `deleted_at` 字段上建索引。

### Q5: 如何禁用某个查询的逻辑删除过滤？

**A**: 使用 `.apply("1=1")` 或手动写 `@Select` 注解。

---

## 9. 参考资料

- [MyBatis Plus 官方文档](https://baomidou.com/)
- [自动填充功能](https://baomidou.com/pages/4c6bcf/)
- [条件构造器](https://baomidou.com/pages/10c804/)
- [分页插件](https://baomidou.com/pages/97710a/)
- [逻辑删除](https://baomidou.com/pages/6b03c5/)

---

## 10. 下一步行动

推荐按以下顺序执行：

1. **第1步**（1小时）：环境准备 + 自动填充时间字段
2. **第2步**（2小时）：BaseMapper 改造（选择1-2个 Mapper 试点）
3. **第3步**（按需）：LambdaQueryWrapper 改造动态查询
4. **第4步**（1小时）：逻辑删除配置
5. **第5步**（可选）：分页插件

**现在开始第1步？** 我可以帮你：
1. 修改 pom.xml 添加依赖
2. 创建 MyMetaObjectHandler
3. 修改 PostDO 作为示例
4. 编写测试用例验证

要开始吗？
