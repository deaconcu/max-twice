package com.prosper.learn.shared.infrastructure.config;

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

    /**
     * 缓存相关配置
     */
    private Cache cache = new Cache();

    /**
     * 数据服务相关配置
     */
    private DataService dataService = new DataService();

    /**
     * 收藏相关配置
     */
    private Bookmark bookmark = new Bookmark();

    private Robot robot = new Robot();

    /**
     * 验证规则相关配置
     */
    private Validation validation = new Validation();

    /**
     * SRS复习算法相关配置
     */
    private Srs srs = new Srs();

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
        private int maxHotCoursesLimit = 200;

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

        /**
         * 评论缓存过期时间（分钟）
         */
        private int cacheTtlMinutes = 30;
    }

    @Data
    public static class Course {
        /**
         * 课程名称搜索结果限制数量
         */
        private int searchLimit = 50;

        /**
         * 热门课程排行榜数量限制
         */
        private int hotCoursesRankingLimit = 100;
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
         * 最大订阅数量
         */
        private int maxSubscriptions = 100;

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
         * 验证码过期时间（分钟）
         */
        private int verificationCodeExpiryMinutes = 10;

        /**
         * 验证码发送间隔（秒）
         */
        private int verificationCodeSendIntervalSeconds = 60;
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
        private int pendingPostsLimit = 20;
        
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
    }

    @Data
    public static class Cache {
        /**
         * 缓存类型 - 控制Spring Cache的类型
         * 值：redis（启用缓存）、none（关闭缓存）
         * 影响：spring.cache.type配置，控制所有@Cacheable等注解是否生效
         */
        private String type = "redis";
    }

    @Data
    public static class Robot {
        /** 是否启用自动作者功能 */
        private boolean enabled = true;
        /** 用于创建AI帖子/目录的系统AI用户ID */
        private long aiUserId = 85L;
        /** 执行器轮询间隔（秒） */
        private int pollIntervalSec = 10;
        /** DIRECTORY 分支每个节点最多创建的子节点数量 */
        private int maxChildrenPerNode = 30;
        /** 兜底扫描CRON表达式（默认每日03:00） */
        private String scanCron = "0 0 3 * * ?";
        /** Redis键前缀，例如 robot:ready */
        private String redisKeyPrefix = "robot:";

        /** AI服务: opencode, gemini, openrouter */
        private String aiService = "openrouter";
        /** AI模型名称 */
        private String model = "deepseek/deepseek-chat";

        // ========== OpenCode 相关配置 ==========
        /** OpenCode 本地服务基础URL，用于调用生成接口 */
        private String opencodeBaseUrl = "http://127.0.0.1:4096";
        /** OpenCode 的 provider ID */
        private String providerId = "github-copilot";

        // ========== Gemini API 相关配置 ==========
        /** Gemini API Key */
        private String geminiApiKey;

        // ========== OpenRouter 相关配置 ==========
        /** OpenRouter API Key */
        private String openrouterApiKey;
    }

    @Data
    public static class Validation {
        // ========== 评论相关 ==========
        /** 评论内容最小长度 */
        private int commentContentMinLength = 1;
        /** 评论内容最大长度 */
        private int commentContentMaxLength = 500;   // 10行左右

        // ========== 用户相关 ==========
        /** 用户名最小长度 */
        private int usernameMinLength = 3;
        /** 用户名最大长度 */
        private int usernameMaxLength = 20;
        /** 密码最小长度 */
        private int passwordMinLength = 8;
        /** 密码最大长度 */
        private int passwordMaxLength = 20;
        /** 个人简介最大长度 */
        private int biographyMaxLength = 100;
        /** 邮箱最大长度 */
        private int emailMaxLength = 254;
        /** 手机号最大长度 */
        private int phoneMaxLength = 20;

        // ========== 课程相关 ==========
        /** 课程名称最小长度 */
        private int courseNameMinLength = 2;
        /** 课程名称最大长度 */
        private int courseNameMaxLength = 40;   // 英文应该设置为60
        /** 课程描述最小长度 */
        private int courseDescriptionMinLength = 20;
        /** 课程描述最大长度 */
        private int courseDescriptionMaxLength = 1000;

        // ========== 帖子相关 ==========
        /** 帖子标题最小长度 */
        //private int postTitleMinLength = 5;
        /** 帖子标题最大长度 */
        //private int postTitleMaxLength = 100;
        /** 帖子内容最小长度 */
        private int postContentMinLength = 10;
        /** 帖子内容最大长度 */
        private int postContentMaxLength = 20000; // 约400行

        // ========== 专业相关 ==========
        /** 专业名称最小长度 */
        private int professionNameMinLength = 2;
        /** 专业名称最大长度 */
        private int professionNameMaxLength = 30;
        /** 专业描述最小长度 */
        private int professionDescriptionMinLength = 20;
        /** 专业描述最大长度 */
        private int professionDescriptionMaxLength = 2000;

        // ========== 记忆卡片相关 ==========
        /** 卡片正面最小长度 */
        private int cardFrontMinLength = 5;
        /** 卡片正面最大长度 */
        private int cardFrontMaxLength = 500;
        /** 卡片背面最小长度 */

        private int cardBackMinLength = 1;
        /** 卡片背面最大长度 */
        private int cardBackMaxLength = 500;
        /** 卡片组标题最小长度 */
        private int deckTitleMinLength = 2;
        /** 卡片组标题最大长度 */
        private int deckTitleMaxLength = 30;
        /** 卡片组描述最大长度 */
        private int deckDescriptionMaxLength = 200;

        // ========== 消息相关 ==========
        /** 消息内容最小长度 */
        private int messageContentMinLength = 1;
        /** 消息内容最大长度 */
        private int messageContentMaxLength = 1000;

        // ========== 路线图相关 ==========
        /** 路线图内容最小长度 */
        private int roadmapContentMinLength = 1;
        /** 路线图内容最大长度 */
        private int roadmapContentMaxLength = 5000;
        /** 路线图描述最小长度 */
        private int roadmapDescriptionMinLength = 1;
        /** 路线图描述最大长度 */
        private int roadmapDescriptionMaxLength = 500;
    }

    @Data
    public static class Srs {
        /**
         * 节点下最大卡片数量限制
         */
        private int maxCardsPerNode = 200;

        /**
         * SRS复习算法配置
         */
        private Algorithm algorithm = new Algorithm();

        @Data
        public static class Algorithm {
            /**
             * 卡片间隔数组
             * LEARNING/RELEARNING 阶段共用，表示每个步骤需要间隔多少张卡片
             * 默认 [3, 8] 表示 Step 0 间隔 3 张，Step 1 间隔 8 张
             */
            private int[] cardGaps = {3, 8};

            /**
             * 毕业间隔(天)
             * 学习中卡片首次毕业后的复习间隔
             */
            private int graduatingInterval = 1;

            /**
             * 简单间隔(天)
             * 新卡被评为"简单"后，直接毕业的间隔
             */
            private int easyInterval = 4;

            /**
             * 简单奖励
             * 对"复习"卡片评为"简单"时的额外间隔奖励乘数
             */
            private double easyBonus = 1.3;

            /**
             * 新间隔乘数
             * 遗忘后重新毕业时的间隔恢复比例(0.2-0.8, Anki推荐0.5-0.7)
             */
            private double newIntervalMultiplier = 0.5;

            /**
             * 最小难度因子
             * EF的最小值
             */
            private double minEaseFactor = 1.3;
        }
    }

    @Data
    public static class DataService {
        /**
         * 批量更新最大数量限制
         */
        private int maxBatchUpdateSize = 50;
    }

    @Data
    public static class Bookmark {
        /**
         * 每个用户每种类型最大收藏数量
         */
        private int maxBookmarksPerType = 1000;
    }
}