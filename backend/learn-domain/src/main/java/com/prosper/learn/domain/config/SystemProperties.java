package com.prosper.learn.domain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class SystemProperties {

    /**
     * 内容管理相关配置
     */
    private Contents contents = new Contents();

    /**
     * 课程相关配置
     */
    private Course course = new Course();

    /**
     * 课程排行相关配置
     */
    private CourseRanking courseRanking = new CourseRanking();

    /**
     * 统计服务相关配置
     */
    private Stats stats = new Stats();

    /**
     * 平台统计相关配置
     */
    private PlatformStats platformStats = new PlatformStats();

    /**
     * 统计监控相关配置
     */
    private StatsMonitor statsMonitor = new StatsMonitor();

    /**
     * 评论相关配置
     */
    private Comment comment = new Comment();

    /**
     * 专业相关配置
     */
    private Profession profession = new Profession();

    /**
     * 用户相关配置
     */
    private User user = new User();

    /**
     * 学习进度相关配置
     */
    private LearningProgress learningProgress = new LearningProgress();

    /**
     * 页面服务相关配置
     */
    private Page page = new Page();

    /**
     * AI服务相关配置
     */
    private Ai ai = new Ai();

    /**
     * 帖子服务相关配置
     */
    private Posting posting = new Posting();

    /**
     * 调度器相关配置
     */
    private Scheduler scheduler = new Scheduler();

    /**
     * 路线图相关配置
     */
    private Roadmap roadmap = new Roadmap();

    @Data
    public static class Contents {
        /**
         * 最大置顶帖子数量
         */
        private int maxPinnedItems = 10;

        /**
         * 置顶字段标识
         */
        private String pinField = "^";

        /**
         * 选中字段标识
         */
        private String chosenField = "+";
    }

    @Data
    public static class CourseRanking {
        /**
         * 默认热门课程列表大小
         */
        private int defaultHotCoursesLimit = 20;

        /**
         * 最大热门课程列表大小
         */
        private int maxHotCoursesLimit = 100;

        /**
         * 是否启用排行榜更新
         */
        private boolean enableRankingUpdate = true;

        /**
         * 统计数据清理时的批量大小
         */
        private int clearBatchSize = 1000;
    }

    @Data
    public static class Stats {
        /**
         * 系统开始日期（用于历史数据查询起始点）
         */
        private String systemStartDate = "2020-01-01";

        /**
         * 最大查询天数范围
         */
        private int maxQueryDaysRange = 365;

        /**
         * 最大查询年份范围 
         */
        private int maxQueryYearRange = 5;

        /**
         * 是否启用统计数据缓存
         */
        private boolean enableStatsCache = true;

        /**
         * 缓存过期时间（分钟）
         */
        private int cacheTtlMinutes = 60;
    }

    @Data
    public static class StatsMonitor {
        /**
         * Redis健康检查间隔（毫秒）- 可能根据环境调整
         */
        private long healthCheckInterval = 60000;

        /**
         * Redis内存监控间隔（毫秒）- 可能根据环境调整
         */
        private long memoryMonitorInterval = 3600000;

        /**
         * 待同步数据警告阈值 - 可能根据业务量调整
         */
        private int pendingDataThreshold = 1000;

        /**
         * 是否启用Redis健康监控 - 可能在不同环境开关
         */
        private boolean enableHealthMonitor = true;

        /**
         * 是否启用内存监控 - 可能在不同环境开关
         */
        private boolean enableMemoryMonitor = true;

        /**
         * 是否启用同步状态检查 - 可能在不同环境开关
         */
        private boolean enableSyncStatusCheck = true;

        /**
         * 是否启用关闭时强制同步 - 可能在不同环境开关
         */
        private boolean enableShutdownSync = true;
    }

    @Data
    public static class Comment {
        /**
         * 默认分页大小
         */
        private int defaultPageSize = 10;

        /**
         * 待审核评论查询数量
         */
        private int pendingCommentsLimit = 50;
    }

    @Data
    public static class Course {
        /**
         * 课程名称搜索结果限制数量
         */
        private int searchLimit = 20;

        /**
         * 热门课程排行榜数量限制
         */
        private int hotCoursesRankingLimit = 100;

        /**
         * 是否启用课程状态验证
         */
        private boolean enableStateValidation = true;

        /**
         * 是否启用父课程存在性验证
         */
        private boolean enableParentValidation = true;
    }

    @Data
    public static class PlatformStats {
        /**
         * 平台统计数据的默认缓存时间（分钟）
         */
        private int defaultCacheTtlMinutes = 30;

        /**
         * 平台统计数据的最大查询范围（天）
         */
        private int maxQueryRangeDays = 90;
    }

    @Data
    public static class Profession {
        /**
         * 分页查询默认页面大小
         */
        private int defaultPageSize = 20;

        /**
         * 默认专业图标
         */
        private String defaultIcon = "mdi-triangle-outline";

        /**
         * 默认热门专业数量限制
         */
        private int defaultHotProfessionsLimit = 20;

        /**
         * 最大热门专业数量限制
         */
        private int maxHotProfessionsLimit = 100;

        /**
         * 是否启用专业状态验证
         */
        private boolean enableStateValidation = true;

        /**
         * 是否启用并发状态检查
         */
        private boolean enableConcurrencyCheck = true;
    }

    @Data
    public static class User {
        /**
         * 用户名最大长度
         */
        private int maxUsernameLength = 50;

        /**
         * 密码最小长度
         */
        private int minPasswordLength = 6;

        /**
         * 关注列表分页大小
         */
        private int followPageSize = 10;

        /**
         * 验证码最小值
         */
        private int verificationCodeMin = 100000;

        /**
         * 验证码最大值
         */
        private int verificationCodeMax = 999999;

        /**
         * 邮件发送者地址
         */
        private String emailSender = "deaconcc@126.com";

        /**
         * 邮件验证主题
         */
        private String emailSubject = "Your Verification Code";

        /**
         * 是否启用邮箱验证
         */
        private boolean enableEmailValidation = true;

        /**
         * 是否启用重复关注检查
         */
        private boolean enableDuplicateFollowCheck = true;

        /**
         * 是否启用订阅重复检查
         */
        private boolean enableDuplicateSubscriptionCheck = true;
    }

    @Data
    public static class LearningProgress {
        /**
         * Redis缓存过期时间（天）
         */
        private int cacheExpireDays = 365;

        /**
         * 同步失败重试间隔（毫秒）
         */
        private long syncRetryInterval = 60000;

        /**
         * 课程完成百分比阈值（基点，10000=100%）
         */
        private int completionThreshold = 10000;

        /**
         * 进度百分比精度倍数（基点转换）
         */
        private int progressPrecisionMultiplier = 10000;

        /**
         * 默认目录索引
         */
        private int defaultTocIndex = 1;

        /**
         * 是否启用Redis降级到数据库
         */
        private boolean enableDatabaseFallback = true;

        /**
         * 是否启用定期补偿同步
         */
        private boolean enableRetrySync = true;

        /**
         * 是否启用层级进度计算
         */
        private boolean enableHierarchicalProgress = true;
    }

    @Data
    public static class Page {
        /**
         * 是否启用路径格式验证
         */
        private boolean enablePathValidation = true;

        /**
         * 是否启用自动路径修复
         */
        private boolean enableAutoPathRepair = true;
    }
    
    @Data
    public static class Ai {
        /**
         * AI服务API URL
         */
        private String apiUrl = "https://openrouter.ai/api/v1/chat/completions";
        
        /**
         * AI服务API密钥
         */
        private String apiKey = "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";
        
        /**
         * 默认AI模型
         */
        private String defaultModel = "gpt-3.5-turbo";
        
        /**
         * 温度参数（控制回复的随机性）
         */
        private double temperature = 0.7;
        
        /**
         * 系统角色提示词
         */
        private String systemPrompt = "你是一个老师，能把复杂的问题用生动的方式讲的很容易让人理解";
        
        /**
         * 请求超时时间（毫秒）
         */
        private long requestTimeoutMs = 30000;
        
        /**
         * 最大重试次数
         */
        private int maxRetryAttempts = 3;
        
        /**
         * 是否启用请求日志
         */
        private boolean enableRequestLogging = false;
    }
    
    @Data
    public static class Posting {
        /**
         * 默认分页大小
         */
        private int defaultPageSize = 10;
        
        /**
         * 节点下默认帖子数量
         */
        private int defaultNodePostCount = 2;
        
        /**
         * 节点列表默认数量
         */
        private int defaultNodeListCount = 3;
        
        /**
         * 待审核帖子查询限制
         */
        private int pendingPostsLimit = 200;
        
        /**
         * 用户内容列表默认数量
         */
        private int userContentsPageSize = 10;
        
        /**
         * 是否启用帖子内容ID转名称
         */
        private boolean enableContentIdToName = true;
    }

    @Data
    public static class Scheduler {
        /**
         * 职业排行榜同步Cron表达式
         */
        private String professionRankingSyncCron = "0 15 * * * ?";
        
        /**
         * 课程排行榜同步Cron表达式
         */
        private String courseRankingSyncCron = "0 30 * * * ?";

        /**
         * 应用启动后初始化延迟时间（毫秒）
         */
        private long initializationDelayMs = 10000;

        /**
         * 是否启用职业排行榜定时同步
         */
        private boolean enableProfessionRankingSync = true;
        
        /**
         * 是否启用课程排行榜定时同步
         */
        private boolean enableCourseRankingSync = true;

        /**
         * 是否启用应用启动时初始化
         */
        private boolean enableStartupInitialization = true;

        /**
         * 数据同步批处理大小
         */
        private int syncBatchSize = 1000;
    }

    @Data
    public static class Roadmap {
        /**
         * 默认分页大小
         */
        private int defaultPageSize = 20;

        /**
         * 最大置顶路线图数量
         */
        private int maxPinnedCount = 19;

        /**
         * 完成进度阈值（基点，10000=100%）
         */
        private int completionThreshold = 10000;

        /**
         * 进度精度倍数（基点转换）
         */
        private double progressPrecisionDivisor = 100.0;

        /**
         * 是否启用内容格式验证
         */
        private boolean enableContentValidation = true;

        /**
         * 是否启用权限检查
         */
        private boolean enablePermissionCheck = true;

        /**
         * 是否启用批量状态查询优化
         */
        private boolean enableBatchStatusQuery = true;
    }
}