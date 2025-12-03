package com.prosper.learn.common.constants;

/**
 * Redis统计数据相关常量
 *
 * 定义Redis统计系统中使用的键名格式、后缀、过期时间等常量，
 * 确保RedisStatsService和DailyStatsService使用一致的配置。
 */
public final class RedisStatsConstants {

    private RedisStatsConstants() {
        // 防止实例化
    }

    // ========== Redis键相关常量 ==========

    /**
     * Redis统计键前缀
     * 格式：stats:YYYY-MM-DD
     */
    public static final String STATS_KEY_PREFIX = "stats:";

    /**
     * 用户统计键后缀
     * 完整格式：stats:YYYY-MM-DD:user
     */
    public static final String USER_STATS_SUFFIX = ":user";

    /**
     * 内容统计键后缀
     * 完整格式：stats:YYYY-MM-DD:content
     */
    public static final String CONTENT_STATS_SUFFIX = ":content";

    // ========== 统计类型常量 ==========

    /**
     * 浏览量统计类型
     */
    public static final String STAT_TYPE_VIEW = "view";

    /**
     * twice点赞统计类型
     */
    public static final String STAT_TYPE_TWICE = "twice";

    /**
     * like点赞统计类型
     */
    public static final String STAT_TYPE_LIKE = "like";

    /**
     * 评论统计类型
     */
    public static final String STAT_TYPE_COMMENT = "comment";

    // ========== Redis配置常量 ==========

    /**
     * Redis数据默认过期时间（天数）
     * 防止Redis内存无限增长
     */
    public static final int DEFAULT_EXPIRE_DAYS = 3;

    // ========== 数据格式说明 ==========

    /**
     * 内容统计字段格式：contentType:contentId:statType
     * 例如：1:123:view（POST类型，ID=123，浏览量）
     */
    public static final String CONTENT_FIELD_FORMAT = "contentType:contentId:statType";

    /**
     * 用户统计字段格式：userId:statType
     * 例如：456:view（用户456的浏览量）
     */
    public static final String USER_FIELD_FORMAT = "userId:statType";
}