# DataService缓存层使用指南

## 概述

我们实现了统一的DataService缓存层，提供高效的数据访问和缓存管理功能。

## 核心特性

### 1. 统一的缓存策略
- 使用Redis MGET/MSET实现高效批量操作
- 分批Pipeline避免大批量操作的性能问题
- 自动降级处理确保系统稳定性

### 2. 智能缓存命中
```java
// 批量查询时自动优化：
// 1. 先从缓存批量获取
// 2. 只查询未命中的数据
// 3. 将新数据写入缓存
// 4. 返回完整结果
List<UserDO> users = userDataService.getByIds(Arrays.asList(1L, 2L, 3L));
```

### 3. 避免循环依赖
- DataService层独立于业务Service
- 专注于数据访问和缓存管理
- 清晰的分层架构

## DataService vs Repository vs QueryService

| 方案 | 职责 | 优势 | 劣势 |
|------|------|------|------|
| **Repository** | 数据访问层 | 传统，易理解 | 容易"变质"，加入业务逻辑 |
| **QueryService** | 查询服务 | 职责单一 | 需要单独的CommandService |
| **DataService** | 数据服务 | 查询+命令统一，避免循环依赖 | 相对较新的概念 |

我们选择**DataService**的原因：
- ✅ **统一管理**：查询和命令操作在一个类中
- ✅ **避免循环依赖**：专注数据访问，不依赖其他业务Service  
- ✅ **职责清晰**：只处理缓存相关的数据操作
- ✅ **命名直观**：DataService明确表达数据服务的含义

## 使用方法

### 1. 在Service中注入DataService

```java
@Service
public class UserService {
    
    @Autowired
    private UserDataService userDataService;
    @Autowired 
    private UserMapper userMapper;  // 用于非缓存操作
    
    public UserDTO getUserInfo(Long userId) {
        // 通过DataService获取用户（自动缓存）
        UserDO user = userDataService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        return convertToDTO(user);
    }
    
    public List<UserDTO> getUsersByIds(List<Long> userIds) {
        // 批量获取（自动优化缓存）
        List<UserDO> users = userDataService.getByIds(userIds);
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public UserDTO getUserByEmail(String email) {
        // 邮箱查询（带缓存）
        UserDO user = userDataService.getByEmail(email);
        return user != null ? convertToDTO(user) : null;
    }
    
    public UserDTO createUser(UserDO user) {
        // 创建直接用Mapper（无缓存需求）
        UserDO created = userMapper.insert(user);
        return convertToDTO(created);
    }
    
    public void updateUser(UserDO user) {
        // 更新走DataService（自动清缓存）
        userDataService.update(user);
    }
    
    public List<UserDTO> searchUsers(String name) {
        // 搜索直接用Mapper（无缓存价值）
        List<UserDO> users = userMapper.searchByName(name);
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
```

### 2. DataService vs 直接调用Mapper

| 操作类型 | 使用方式 | 原因 |
|----------|----------|------|
| **高频查询** | DataService | 有缓存价值 |
| **更新操作** | DataService | 需要清缓存 |
| **创建操作** | 直接Mapper | 无缓存需求 |
| **搜索查询** | 直接Mapper | 无缓存价值 |
| **复杂查询** | 直接Mapper | 一次性使用 |

### 3. 缓存管理

```java
// 更新数据时清除缓存（推荐使用@CacheEvict）
userDataService.update(user);  // 自动清除缓存

// 手动清除缓存
userDataService.evictCache(userId);
userDataService.evictEmailCache(email);

// 预热缓存
userDataService.warmUpCache(Arrays.asList(1L, 2L, 3L));

// 获取缓存统计
Map<String, Object> stats = userDataService.getCacheStats(userIds);
```

## 设计原则和最佳实践

### 1. 缓存清除策略

**使用@CacheEvict注解（推荐）：**
```java
@CacheEvict(value = "users", key = "#user.id")
public void update(UserDO user) {
    userMapper.update(user);
    // Spring AOP自动处理缓存清除
}
```

**优势：**
- **声明式**: 更清晰地表达缓存清除意图
- **AOP处理**: Spring自动处理，即使方法抛异常也能正确清除
- **统一管理**: 和其他缓存注解保持一致
- **支持SpEL**: 可以用表达式灵活指定key

**避免手动调用：**
```java
// ❌ 不推荐
public void update(UserDO user) {
    userMapper.update(user);
    evictCache(user.getId());  // 如果上面抛异常，缓存不会被清除
}
```

### 2. 方法命名规范

**简洁命名（推荐）：**
```java
@CacheEvict(value = "users", key = "#user.id")
public void update(UserDO user) { ... }
```

**避免冗余命名：**
```java
// ❌ 不推荐
public void updateAndEvictCache(UserDO user) { ... }
```

注解已经表达了缓存清除的行为，方法名应该简洁明了。

### 3. 批量操作优化

**为什么小批量查缓存，大批量直接查数据库？**

我们采用了更优化的策略：
```java
public List<T> getByIds(Collection<Long> ids) {
    // 1. 先从缓存批量查询（使用Redis MGET）
    Map<Long, T> cached = batchGetFromCache(ids);
    
    // 2. 找出缓存未命中的ID
    Set<Long> missedIds = findMissedIds(ids, cached);
    
    // 3. 查询未命中的数据
    if (!missedIds.isEmpty()) {
        List<T> fromDB = getByIdsFromMapper(mapper, missedIds);
        // 4. 回写缓存（使用分批Pipeline）
        batchPutToCache(fromDB);
    }
    
    // 5. 合并结果
    return mergeResults(ids, cached, fromDB);
}
```

**为什么getMapByIds要查缓存？**

之前的实现直接查数据库是错误的，正确的应该是：
```java
public Map<Long, T> getMapByIds(Collection<Long> ids) {
    // 复用getByIds的缓存逻辑
    List<T> entities = getByIds(ids);
    
    // 转换为Map
    return entities.stream()
        .collect(Collectors.toMap(this::getId, Function.identity()));
}
```

### 4. Redis批量操作

**使用Redis原生批量命令：**
- **MGET**: 批量获取多个key
- **Pipeline**: 批量执行多个命令

**分批Pipeline策略：**
```java
// 分批处理，避免单次操作过大
int batchSize = 100;
for (int i = 0; i < entries.size(); i += batchSize) {
    List<Entry> batch = entries.subList(i, Math.min(i + batchSize, entries.size()));
    
    redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
        batch.forEach(entry -> {
            connection.setEx(keyBytes, ttl.getSeconds(), valueBytes);
        });
        return null;
    });
}
```

**为什么不用multiSet？**
- `multiSet`不能设置TTL
- 需要逐个调用`expire`，网络开销大
- Pipeline能将多个`SETEX`命令打包发送，更高效

### 5. Repository方法设计原则

**Repository应该专注于缓存相关操作，不是所有Mapper方法的包装层。**

#### **应该包装的方法：**

1. **需要缓存的查询方法**
```java
// 高频查询，值得缓存
@Override
public T getById(Long id) { ... }

public List<T> getByIds(Collection<Long> ids) { ... }
```

2. **需要清除缓存的更新方法**
```java
@CacheEvict(value = "users", key = "#user.id")
public void update(UserDO user) { ... }

@CacheEvict(value = "users", key = "#id")
public void delete(Long id) { ... }
```

3. **常用的业务查询方法（如果需要缓存）**
```java
@Cacheable(value = "usersByEmail", key = "#email")
public UserDO getByEmail(String email) { ... }
```

#### **不应该包装的方法：**

1. **简单的插入操作**
```java
// ❌ 不推荐 - 简单透传，没有价值
public UserDO insert(UserDO user) {
    return userMapper.insert(user);
}

// ✅ 推荐 - 直接在Service中调用
@Service
public class UserService {
    public UserDO createUser(UserDO user) {
        return userMapper.insert(user);  // 直接调用
    }
}
```

2. **复杂查询和业务查询**
```java
// ❌ 不推荐 - 简单包装业务查询
public List<NodeDO> getByParent(long parentId) {
    return nodeMapper.getByParent(parentId);
}

// ✅ 推荐 - 直接在Service中调用
@Service
public class NodeService {
    public List<NodeDO> getNodesByParent(long parentId) {
        return nodeMapper.getByParent(parentId);  // 直接调用
    }
}
```

3. **统计和聚合查询**
```java
// ❌ 不推荐包装
public long countActiveUsers() {
    return userMapper.countActiveUsers();
}

// ✅ 推荐直接调用
```

#### **特殊情况下可以包装insert的场景：**

1. **需要缓存预热**
```java
public NodeDO insert(NodeDO node) {
    nodeMapper.insert(node);
    // 插入后立即缓存，避免后续查询穿透
    putToCache(node.getId(), node);
    return node;
}
```

2. **统一的数据验证和处理**
```java
public UserDO insert(UserDO user) {
    // 统一的业务验证
    validateUser(user);
    // 设置审计字段
    user.setCreatedAt(LocalDateTime.now());
    // 数据清洗
    user.setEmail(user.getEmail().toLowerCase());
    
    return userMapper.insert(user);
}
```

3. **需要清除相关缓存**
```java
public CourseNodeDO insert(CourseNodeDO node) {
    nodeMapper.insert(node);
    // 插入新节点后，清除课程的节点列表缓存
    evictCourseNodesCache(node.getCourseId());
    return node;
}
```

4. **统一的异常处理和日志**
```java
public UserDO insert(UserDO user) {
    try {
        userMapper.insert(user);
        log.info("User created: {}", user.getId());
        return user;
    } catch (Exception e) {
        log.error("Failed to create user: {}", user.getEmail(), e);
        throw new BusinessException("创建用户失败", e);
    }
}
```

#### **Service和Repository的协作模式：**

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;  // 缓存相关
    @Autowired 
    private UserMapper userMapper;         // 直接数据访问
    
    // 高频查询走缓存
    public UserDO getUserById(Long id) {
        return userRepository.getById(id);
    }
    
    // 复杂查询直接用Mapper
    public List<UserDO> searchUsers(String keyword, int page, int size) {
        return userMapper.searchByKeyword(keyword, page * size, size);
    }
    
    // 简单插入直接用Mapper
    public UserDO createUser(UserDO user) {
        return userMapper.insert(user);
    }
    
    // 更新走Repository（清缓存）
    public void updateUser(UserDO user) {
        userRepository.update(user);
    }
}

## 创建新的Repository

### 1. 继承AbstractCachedRepository

```java
@Repository
public class CourseRepository extends AbstractCachedRepository<CourseDO, CourseMapper> {
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Override
    protected CourseMapper getMapper() {
        return courseMapper;
    }
    
    @Override
    protected String getCacheName() {
        return "courses";
    }
    
    @Override
    protected String getEntityName() {
        return "Course";
    }
    
    @Override
    protected Long getEntityId(CourseDO entity) {
        return entity.getId();
    }
    
    @Override
    protected CourseDO getByIdFromMapper(CourseMapper mapper, Long id) {
        return mapper.getById(id);
    }
    
    @Override
    protected List<CourseDO> getByIdsFromMapper(CourseMapper mapper, Collection<Long> ids) {
        return mapper.getByIds(ids);
    }
    
    @Override
    protected Map<Long, CourseDO> getMapByIdsFromMapper(CourseMapper mapper, Collection<Long> ids) {
        // 如果Mapper没有这个方法，可以转换
        return getByIdsFromMapper(mapper, ids).stream()
                .collect(Collectors.toMap(CourseDO::getId, Function.identity()));
    }
    
    // 可以添加特定的查询方法
    @Cacheable(value = "coursesByCategory", key = "#mainCategory + '_' + #subCategory")
    public List<CourseDO> getByCategory(int mainCategory, int subCategory) {
        return courseMapper.listRootByCategory(mainCategory, subCategory);
    }
}
```

### 2. 在CacheConfig中添加配置

```java
// 已在CacheConfig.java中配置
cacheConfigurations.put("courses", defaultConfig.entryTtl(Duration.ofMinutes(15)));
cacheConfigurations.put("coursesByCategory", defaultConfig.entryTtl(Duration.ofMinutes(10)));
```

## 配置说明

### 缓存TTL设置
- **users**: 30分钟（用户信息相对稳定）
- **courses**: 15分钟（课程信息中等变化频率）
- **posts**: 5分钟（帖子信息变化较频繁）
- **usersByEmail**: 15分钟（邮箱查询结果）

### 批量操作配置
- **默认批次大小**: 100
- **用户批次大小**: 200（用户查询量大）
- **分批Pipeline**: 避免大批量操作的内存和性能问题

## 监控和调试

### 日志输出
```
DEBUG - Retrieved 150 User entities (cache:120, db:30) in 25ms
DEBUG - MGET cache hit: 120/150 for User
DEBUG - Pipeline cached batch 0-100 (100 entities) for User
```

### 缓存统计
```java
Map<String, Object> stats = userRepository.getCacheStats(userIds);
// 输出：{total=150, cached=120, hitRate=0.8, entity=User}
```

## 最佳实践

1. **优先使用Repository**: 所有数据访问都通过Repository，不要直接调用Mapper
2. **合理设置TTL**: 根据数据变化频率设置合适的缓存时间
3. **及时清除缓存**: 数据更新时记得清除相关缓存
4. **批量操作**: 尽量使用批量查询减少网络开销
5. **监控缓存命中率**: 定期检查缓存效果，优化缓存策略
6. **简化DataService实现**: 没有缓存操作需求的方法不需要在DataService中实现
7. **优先使用注解**: 使用@Cacheable、@CacheEvict等注解来操作缓存，避免手动缓存操作
8. **删除前先查询**: 删除操作需要清除缓存时，先查询获取完整信息，再执行删除和缓存清除