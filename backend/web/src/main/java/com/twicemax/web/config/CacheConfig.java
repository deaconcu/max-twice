package com.twicemax.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.infrastructure.redis.RedisKeyPrefix;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置
 * 只在缓存启用时生效
 *
 * 分库场景下，内容类缓存需要添加语言前缀，用户类缓存不需要。
 *
 * @author Claude
 * @since 2024-01-20
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
public class CacheConfig extends CachingConfigurerSupport {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        // 复制全局 ObjectMapper（携带 JavaTimeModule 等已注册模块），再在副本上激活 default typing：
        //  - 序列化时写入 @class，反序列化时按 @class 还原为原始类型，否则反序列化是 LinkedHashMap
        //  - 不直接改全局 ObjectMapper，避免 default typing 影响 web 请求/响应 JSON
        // 与 AppConfiguration.redisTemplate 保持一致的做法。
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.activateDefaultTyping(
                cacheObjectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonRedisSerializer =
                new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        // 默认缓存配置（10分钟，带语言前缀）
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer))
                .computePrefixWith(cacheName -> RedisKeyPrefix.get() + cacheName + "::");

        // 用户类缓存配置（不带语言前缀）
        RedisCacheConfiguration userConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer));

        // 针对不同缓存空间的特定配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // === 用户类缓存（不带语言前缀）===
        cacheConfigurations.put("users", userConfig);
        cacheConfigurations.put("userIdByEmail", userConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("userIdByName", userConfig.entryTtl(Duration.ofMinutes(15)));

        // === 内容类缓存（带语言前缀）===
        // 平台统计数据缓存：30分钟过期
        cacheConfigurations.put("platformStats", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Repository层实体缓存配置
        cacheConfigurations.put("courses", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("nodes", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("posts", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("comments", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("roadmaps", defaultConfig.entryTtl(Duration.ofMinutes(20)));
        cacheConfigurations.put("roles", defaultConfig.entryTtl(Duration.ofMinutes(60)));
        cacheConfigurations.put("upvotes", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // 查询缓存配置
        cacheConfigurations.put("coursesByCategory", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}