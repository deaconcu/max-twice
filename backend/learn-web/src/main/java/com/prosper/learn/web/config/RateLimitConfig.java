package com.prosper.learn.web.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import org.redisson.api.RedissonClient;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

/**
 * 限流配置类
 *
 * @author Claude Code
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public CacheManager rateLimitCacheManager(RedissonClient redissonClient) {
        CacheManager manager = Caching.getCachingProvider().getCacheManager();
        MutableConfiguration<String, byte[]> config = new MutableConfiguration<>();
        // 设置缓存过期策略为10分钟，防止Redis内存泄漏
        config.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.TEN_MINUTES));
        manager.createCache("rateLimitBuckets", RedissonConfiguration.fromConfig(redissonClient.getConfig(), config));
        return manager;
    }

    @Bean
    public ProxyManager<String> rateLimitProxyManager(CacheManager rateLimitCacheManager) {
        return new JCacheProxyManager<>(rateLimitCacheManager.getCache("rateLimitBuckets"));
    }
}
