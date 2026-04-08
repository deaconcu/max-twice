package com.prosper.learn.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * @author Claude
 * @since 2024-01-20
 */
@Configuration
@ConditionalOnProperty(name = "app.cache.type", havingValue = "redis", matchIfMissing = false)
public class CacheConfig extends CachingConfigurerSupport {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        // 使用已配置的 ObjectMapper 创建序列化器
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = 
                new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // 默认缓存配置（10分钟）
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer));

        // 针对不同缓存空间的特定配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 平台统计数据缓存：30分钟过期
        cacheConfigurations.put("platformStats", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Repository层实体缓存配置
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("courses", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("nodes", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("posts", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("comments", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("roadmaps", defaultConfig.entryTtl(Duration.ofMinutes(20)));
        cacheConfigurations.put("roles", defaultConfig.entryTtl(Duration.ofMinutes(60)));
        
        // 查询缓存配置（较短TTL）
        cacheConfigurations.put("usersByEmail", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("coursesByCategory", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}