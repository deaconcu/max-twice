# Redis缓存层设计方案

## 1. 方案概述

为学习平台添加Redis缓存层，通过多级缓存策略显著提升系统性能，减少数据库压力，改善用户体验。

## 2. 缓存策略设计

### 2.1 缓存分层架构

```
┌─────────────────┐
│   Web前端       │
├─────────────────┤
│   应用层缓存     │  ← 本地缓存(Caffeine)
├─────────────────┤
│   Redis缓存层   │  ← 分布式缓存
├─────────────────┤
│   数据库层      │  ← MySQL持久化
└─────────────────┘
```

### 2.2 缓存数据分类

#### 热点数据 (高频访问)
- **课程信息**: 课程详情、目录结构
- **用户信息**: 用户基本信息、学习进度
- **统计数据**: 平台统计、用户统计

#### 会话数据 (短期存储)
- **用户会话**: 登录状态、临时权限
- **学习状态**: 当前学习节点、临时进度

#### 计算结果 (复杂查询)
- **排行榜**: 用户排名、课程排名
- **推荐数据**: 课程推荐、内容推荐
- **聚合统计**: 实时统计数据

## 3. 缓存键设计规范

### 3.1 命名规范
```
{业务模块}:{数据类型}:{业务标识}:{版本号}

示例:
- course:info:123:v1          # 课程123的详细信息
- user:profile:456:v1         # 用户456的个人资料
- stats:platform:daily:v1     # 平台每日统计
- toc:course:123:v1           # 课程123的目录结构
- progress:user:456:course:123:v1  # 用户456在课程123的进度
```

### 3.2 键空间设计
```
learn:
├── course:
│   ├── info:{courseId}           # 课程基本信息
│   ├── toc:{courseId}            # 课程目录结构
│   ├── list:category:{categoryId}  # 分类课程列表
│   └── ranking:popular           # 热门课程排行
├── user:
│   ├── info:{userId}             # 用户基本信息
│   ├── profile:{userId}          # 用户详细资料
│   ├── progress:{userId}         # 用户学习进度
│   └── subscription:{userId}     # 用户订阅信息
├── content:
│   ├── post:{postId}             # 帖子详情
│   ├── comment:{postId}          # 帖子评论
│   └── node:{nodeId}             # 节点内容
├── stats:
│   ├── platform:realtime         # 平台实时统计
│   ├── user:daily:{date}         # 用户每日统计
│   └── course:popular            # 课程热度统计
└── session:
    ├── user:{userId}             # 用户会话
    └── learning:{userId}         # 学习会话
```

## 4. 缓存配置管理

### 4.1 Redis配置类
```java
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class RedisConfig {
    
    @Value("${spring.redis.host}")
    private String host;
    
    @Value("${spring.redis.port}")
    private int port;
    
    @Value("${spring.redis.password}")
    private String password;
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        if (StringUtils.hasText(password)) {
            factory.setPassword(password);
        }
        factory.setValidateConnection(true);
        return factory;
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        // 使用Jackson序列化
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LazyWhitelistTypeMapper.validatedDefaultTyping(objectMapper), 
            ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(objectMapper);
        
        // 设置序列化器
        template.setDefaultSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
            
        return builder.build();
    }
    
    private RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1)) // 默认1小时过期
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
    }
}
```

### 4.2 缓存策略配置
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 200
        max-idle: 20
        min-idle: 5
        max-wait: 1000ms
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1小时
      cache-null-values: false

# 自定义缓存配置
cache:
  config:
    # 课程相关缓存
    course:
      info:
        ttl: 3600        # 1小时
        max-idle: 1800   # 30分钟无访问清除
      toc:
        ttl: 7200        # 2小时（目录变化较少）
      list:
        ttl: 600         # 10分钟（列表更新较频繁）
    
    # 用户相关缓存
    user:
      info:
        ttl: 1800        # 30分钟
      progress:
        ttl: 300         # 5分钟（进度更新频繁）
      subscription:
        ttl: 3600        # 1小时
    
    # 内容相关缓存
    content:
      post:
        ttl: 3600        # 1小时
      comment:
        ttl: 600         # 10分钟
    
    # 统计数据缓存
    stats:
      realtime:
        ttl: 60          # 1分钟
      daily:
        ttl: 86400       # 24小时
```

## 5. 缓存服务实现

### 5.1 通用缓存服务
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheConfig cacheConfig;
    
    /**
     * 获取缓存数据
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return clazz.isInstance(value) ? clazz.cast(value) : null;
        } catch (Exception e) {
            log.error("获取缓存失败, key: {}", key, e);
            return null;
        }
    }
    
    /**
     * 设置缓存数据
     */
    public void set(String key, Object value, Duration ttl) {
        try {
            if (ttl != null && ttl.getSeconds() > 0) {
                redisTemplate.opsForValue().set(key, value, ttl);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
        } catch (Exception e) {
            log.error("设置缓存失败, key: {}", key, e);
        }
    }
    
    /**
     * 删除缓存
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("删除缓存失败, key: {}", key, e);
        }
    }
    
    /**
     * 批量删除缓存
     */
    public void deletePattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("批量删除缓存失败, pattern: {}", pattern, e);
        }
    }
    
    /**
     * 获取或设置缓存（Cache-Aside模式）
     */
    public <T> T getOrSet(String key, Class<T> clazz, Supplier<T> dataSupplier, Duration ttl) {
        T cachedData = get(key, clazz);
        if (cachedData != null) {
            return cachedData;
        }
        
        T freshData = dataSupplier.get();
        if (freshData != null) {
            set(key, freshData, ttl);
        }
        return freshData;
    }
}
```

### 5.2 业务缓存服务
```java
@Service
@RequiredArgsConstructor
public class CourseCacheService {
    
    private final CacheService cacheService;
    private final CourseMapper courseMapper;
    private final CacheConfig cacheConfig;
    
    private static final String COURSE_INFO_KEY = "learn:course:info:%d";
    private static final String COURSE_TOC_KEY = "learn:course:toc:%d";
    private static final String COURSE_LIST_KEY = "learn:course:list:category:%d";
    
    /**
     * 获取课程信息（带缓存）
     */
    public CourseDO getCourseInfo(int courseId) {
        String key = String.format(COURSE_INFO_KEY, courseId);
        
        return cacheService.getOrSet(key, CourseDO.class, 
            () -> courseMapper.getById(courseId),
            Duration.ofSeconds(cacheConfig.getCourse().getInfo().getTtl())
        );
    }
    
    /**
     * 获取课程目录（带缓存）
     */
    public String getCourseToc(int courseId) {
        String key = String.format(COURSE_TOC_KEY, courseId);
        
        return cacheService.getOrSet(key, String.class,
            () -> {
                CourseDO course = courseMapper.getById(courseId);
                return course != null ? course.getContents() : null;
            },
            Duration.ofSeconds(cacheConfig.getCourse().getToc().getTtl())
        );
    }
    
    /**
     * 获取分类课程列表（带缓存）
     */
    public List<CourseDO> getCoursesByCategory(int categoryId) {
        String key = String.format(COURSE_LIST_KEY, categoryId);
        
        return cacheService.getOrSet(key, 
            new TypeReference<List<CourseDO>>() {}.getType(),
            () -> courseMapper.listByCategory(categoryId),
            Duration.ofSeconds(cacheConfig.getCourse().getList().getTtl())
        );
    }
    
    /**
     * 更新课程信息（清除相关缓存）
     */
    @CacheEvict(value = "course", key = "#courseId")
    public void updateCourse(int courseId, CourseDO course) {
        courseMapper.update(course);
        
        // 清除相关缓存
        cacheService.delete(String.format(COURSE_INFO_KEY, courseId));
        cacheService.delete(String.format(COURSE_TOC_KEY, courseId));
        cacheService.deletePattern("learn:course:list:*");
    }
}
```

### 5.3 用户缓存服务
```java
@Service
@RequiredArgsConstructor
public class UserCacheService {
    
    private final CacheService cacheService;
    private final UserMapper userMapper;
    private final UserProgressService userProgressService;
    
    private static final String USER_INFO_KEY = "learn:user:info:%d";
    private static final String USER_PROGRESS_KEY = "learn:user:progress:%d";
    private static final String USER_SUBSCRIPTION_KEY = "learn:user:subscription:%d";
    
    /**
     * 获取用户信息（带缓存）
     */
    @Cacheable(value = "user", key = "#userId")
    public UserDO getUserInfo(int userId) {
        String key = String.format(USER_INFO_KEY, userId);
        
        return cacheService.getOrSet(key, UserDO.class,
            () -> userMapper.getById(userId),
            Duration.ofMinutes(30)
        );
    }
    
    /**
     * 获取用户学习进度（带缓存）
     */
    public Map<String, Object> getUserProgress(int userId) {
        String key = String.format(USER_PROGRESS_KEY, userId);
        
        return cacheService.getOrSet(key, Map.class,
            () -> userProgressService.getUserProgress(userId),
            Duration.ofMinutes(5) // 进度更新频繁，缓存时间较短
        );
    }
    
    /**
     * 更新用户进度（更新缓存）
     */
    public void updateUserProgress(int userId, Map<String, Object> progress) {
        userProgressService.updateProgress(userId, progress);
        
        // 更新缓存
        String key = String.format(USER_PROGRESS_KEY, userId);
        cacheService.set(key, progress, Duration.ofMinutes(5));
    }
    
    /**
     * 清除用户相关缓存
     */
    public void clearUserCache(int userId) {
        cacheService.delete(String.format(USER_INFO_KEY, userId));
        cacheService.delete(String.format(USER_PROGRESS_KEY, userId));
        cacheService.delete(String.format(USER_SUBSCRIPTION_KEY, userId));
    }
}
```

## 6. 缓存注解使用

### 6.1 Spring Cache注解
```java
@Service
public class PostService {
    
    /**
     * 缓存帖子信息
     */
    @Cacheable(value = "post", key = "#postId", unless = "#result == null")
    public PostDO getPost(int postId) {
        return postMapper.getById(postId);
    }
    
    /**
     * 更新帖子时清除缓存
     */
    @CacheEvict(value = "post", key = "#postId")
    public void updatePost(int postId, PostDO post) {
        postMapper.update(post);
    }
    
    /**
     * 删除帖子时清除缓存
     */
    @CacheEvict(value = "post", key = "#postId")
    public void deletePost(int postId) {
        postMapper.delete(postId);
    }
    
    /**
     * 缓存帖子列表
     */
    @Cacheable(value = "postList", key = "#nodeId + '_' + #lastId")
    public List<PostDO> getPostList(int nodeId, int lastId) {
        return postMapper.getListByNode(nodeId, lastId);
    }
    
    /**
     * 发布新帖子时清除列表缓存
     */
    @CacheEvict(value = "postList", allEntries = true)
    public void createPost(PostDO post) {
        postMapper.insert(post);
    }
}
```

### 6.2 自定义缓存注解
```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheableWithTTL {
    String value() default "";
    String key() default "";
    int ttl() default 3600; // 默认1小时
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}

@Aspect
@Component
@RequiredArgsConstructor
public class CacheableWithTTLAspect {
    
    private final CacheService cacheService;
    
    @Around("@annotation(cacheableWithTTL)")
    public Object around(ProceedingJoinPoint joinPoint, CacheableWithTTL cacheableWithTTL) throws Throwable {
        String key = generateKey(joinPoint, cacheableWithTTL.key());
        
        // 尝试从缓存获取
        Object cachedResult = cacheService.get(key, Object.class);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // 执行原方法
        Object result = joinPoint.proceed();
        
        // 缓存结果
        if (result != null) {
            Duration ttl = Duration.of(cacheableWithTTL.ttl(), cacheableWithTTL.timeUnit().toChronoUnit());
            cacheService.set(key, result, ttl);
        }
        
        return result;
    }
}
```

## 7. 缓存预热和刷新

### 7.1 缓存预热策略
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService {
    
    private final CourseCacheService courseCacheService;
    private final UserCacheService userCacheService;
    private final StatsService statsService;
    
    /**
     * 应用启动时预热热点数据
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmupOnStartup() {
        log.info("开始缓存预热...");
        
        // 预热热门课程
        warmupPopularCourses();
        
        // 预热平台统计
        warmupPlatformStats();
        
        log.info("缓存预热完成");
    }
    
    /**
     * 预热热门课程数据
     */
    private void warmupPopularCourses() {
        try {
            List<Integer> popularCourseIds = statsService.getPopularCourseIds(20);
            
            popularCourseIds.parallelStream().forEach(courseId -> {
                try {
                    courseCacheService.getCourseInfo(courseId);
                    courseCacheService.getCourseToc(courseId);
                } catch (Exception e) {
                    log.error("预热课程缓存失败, courseId: {}", courseId, e);
                }
            });
            
            log.info("预热了 {} 个热门课程缓存", popularCourseIds.size());
        } catch (Exception e) {
            log.error("预热热门课程失败", e);
        }
    }
    
    /**
     * 定时刷新缓存
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void refreshCache() {
        // 刷新实时统计
        refreshRealtimeStats();
        
        // 刷新热门内容排行
        refreshPopularRankings();
    }
}
```

### 7.2 缓存更新策略
```java
@Service
@RequiredArgsConstructor
public class CacheUpdateService {
    
    private final CacheService cacheService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 更新缓存（Write-Through模式）
     */
    public void updateCacheAndDatabase(String key, Object data, Runnable dbUpdate) {
        try {
            // 先更新数据库
            dbUpdate.run();
            
            // 再更新缓存
            cacheService.set(key, data, Duration.ofHours(1));
        } catch (Exception e) {
            log.error("缓存和数据库更新失败", e);
            // 删除可能不一致的缓存
            cacheService.delete(key);
        }
    }
    
    /**
     * 延迟双删策略
     */
    public void delayedDoubleDelete(String key, Runnable dbUpdate) {
        // 第一次删除缓存
        cacheService.delete(key);
        
        try {
            // 更新数据库
            dbUpdate.run();
        } catch (Exception e) {
            log.error("数据库更新失败", e);
            throw e;
        }
        
        // 延迟第二次删除缓存
        CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)
            .execute(() -> cacheService.delete(key));
    }
}
```

## 8. 缓存监控和指标

### 8.1 缓存指标收集
```java
@Component
@RequiredArgsConstructor
public class CacheMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
    @PostConstruct
    public void initMetrics() {
        Gauge.builder("cache.hit.ratio")
            .description("缓存命中率")
            .register(meterRegistry, this, this::getHitRatio);
            
        Gauge.builder("cache.size")
            .description("缓存大小")
            .register(meterRegistry, this, this::getCacheSize);
    }
    
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }
    
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }
    
    private double getHitRatio() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }
    
    private long getCacheSize() {
        try {
            return redisTemplate.getConnectionFactory()
                .getConnection()
                .dbSize();
        } catch (Exception e) {
            return 0;
        }
    }
}
```

### 8.2 缓存健康检查
```java
@Component
@RequiredArgsConstructor
public class CacheHealthIndicator implements HealthIndicator {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public Health health() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
                
            if ("PONG".equals(pong)) {
                return Health.up()
                    .withDetail("redis", "连接正常")
                    .withDetail("ping", pong)
                    .build();
            } else {
                return Health.down()
                    .withDetail("redis", "连接异常")
                    .withDetail("ping", pong)
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("redis", "连接失败")
                .withException(e)
                .build();
        }
    }
}
```

## 9. 缓存最佳实践

### 9.1 缓存穿透防护
```java
@Service
@RequiredArgsConstructor
public class CachePenetrationProtectionService {
    
    private final CacheService cacheService;
    private final BloomFilter<String> bloomFilter;
    
    /**
     * 防止缓存穿透的查询
     */
    public <T> T getWithBloomFilter(String key, Class<T> clazz, Supplier<T> dataSupplier) {
        // 1. 布隆过滤器检查
        if (!bloomFilter.mightContain(key)) {
            return null; // 一定不存在
        }
        
        // 2. 查询缓存
        T cachedData = cacheService.get(key, clazz);
        if (cachedData != null) {
            return cachedData;
        }
        
        // 3. 查询数据库
        T dbData = dataSupplier.get();
        
        // 4. 缓存结果（包括null值）
        if (dbData != null) {
            cacheService.set(key, dbData, Duration.ofHours(1));
        } else {
            // 缓存空值，防止缓存穿透
            cacheService.set(key, "NULL", Duration.ofMinutes(5));
        }
        
        return dbData;
    }
}
```

### 9.2 缓存雪崩防护
```java
@Service
@RequiredArgsConstructor
public class CacheAvalancheProtectionService {
    
    private final CacheService cacheService;
    private final Random random = new Random();
    
    /**
     * 防止缓存雪崩的设置方法
     */
    public void setWithRandomTTL(String key, Object value, Duration baseTtl) {
        // 添加随机时间（基础TTL的10%-20%）
        long randomSeconds = (long) (baseTtl.getSeconds() * (0.1 + random.nextDouble() * 0.1));
        Duration finalTtl = baseTtl.plusSeconds(randomSeconds);
        
        cacheService.set(key, value, finalTtl);
    }
    
    /**
     * 缓存预热，防止同时失效
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void refreshCacheBeforeExpiry() {
        // 提前刷新即将过期的缓存
        List<String> keysToRefresh = getKeysNearExpiry();
        
        keysToRefresh.parallelStream().forEach(key -> {
            try {
                refreshCacheByKey(key);
            } catch (Exception e) {
                log.error("预刷新缓存失败, key: {}", key, e);
            }
        });
    }
}
```

## 10. 部署和运维

### 10.1 Redis集群配置
```yaml
# Redis集群配置
spring:
  redis:
    cluster:
      nodes:
        - redis-node1:7000
        - redis-node1:7001
        - redis-node2:7000
        - redis-node2:7001
        - redis-node3:7000
        - redis-node3:7001
      max-redirects: 3
    lettuce:
      cluster:
        refresh:
          adaptive: true
          period: 30s
```

### 10.2 监控配置
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,caches
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: learn-platform
```

## 11. 性能预期

### 11.1 性能提升指标
- **查询响应时间**: 从平均200ms降低到10-20ms
- **数据库压力**: 减少70-80%的查询请求
- **并发处理能力**: 提升3-5倍
- **用户体验**: 页面加载速度提升50%以上

### 11.2 缓存命中率目标
- **课程信息**: 90%+
- **用户数据**: 85%+
- **统计数据**: 95%+
- **内容数据**: 80%+

## 12. 风险评估和应对

### 12.1 技术风险
- **Redis故障**: 实现降级机制，直接查询数据库
- **数据不一致**: 采用最终一致性，关键数据双写
- **内存溢出**: 设置合理的内存上限和淘汰策略

### 12.2 运维风险
- **缓存雪崩**: 随机TTL + 缓存预热
- **缓存穿透**: 布隆过滤器 + 空值缓存
- **热点数据**: 多级缓存 + 本地缓存

这个Redis缓存层将显著提升系统性能，通过科学的缓存策略和完善的监控体系，确保系统的高可用性和稳定性。