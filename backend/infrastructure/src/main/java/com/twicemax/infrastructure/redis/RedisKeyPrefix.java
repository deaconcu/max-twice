package com.twicemax.infrastructure.redis;

import com.twicemax.infrastructure.datasource.DataSourceContextHolder;

/**
 * Redis Key 前缀工具类
 * 用于在分库场景下，为 Redis key 添加语言前缀
 */
public class RedisKeyPrefix {

    private RedisKeyPrefix() {
        // 工具类，禁止实例化
    }

    /**
     * 获取当前语言前缀
     * 格式: "zh:" 或 "en:"
     */
    public static String get() {
        return DataSourceContextHolder.getLanguage() + ":";
    }

    /**
     * 获取当前语言
     */
    public static String getLanguage() {
        return DataSourceContextHolder.getLanguage();
    }

    /**
     * 为 key 添加语言前缀
     * 例如: "course:123" -> "zh:course:123"
     */
    public static String prefix(String key) {
        return get() + key;
    }

    /**
     * 为多个 key 片段添加语言前缀并拼接
     * 例如: prefix("stats", "2025-01-01", "user") -> "zh:stats:2025-01-01:user"
     */
    public static String prefix(String... parts) {
        return get() + String.join(":", parts);
    }
}
