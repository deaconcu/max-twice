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
     * 统计服务相关配置
     */
    private Stats stats = new Stats();


    /**
     * 统计监控相关配置
     */
    private StatsMonitor statsMonitor = new StatsMonitor();

    /**
     * 用户相关配置
     */
    private User user = new User();

    /**
     * 调度器相关配置
     */
    private Scheduler scheduler = new Scheduler();

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
    public static class Stats {
        /**
         * 系统开始日期（用于历史数据查询起始点）
         */
        private String systemStartDate = "2020-01-01";

        /**
         * 最大查询天数范围
         */
        private int maxQueryDaysRange = 365;
    }

    @Data
    public static class StatsMonitor {
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
    public static class User {
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
    public static class Scheduler {
        /**
         * 是否启用角色排行榜定时同步
         */
        private boolean enableRoleRankingSync = true;

        /**
         * 是否启用应用启动时初始化（角色排行榜）
         */
        private boolean enableRoleRankingStartupInit = true;
    }

    @Data
    public static class Robot {
        /** 是否启用自动作者功能 */
        private boolean enabled = true;
        /** 用于创建AI帖子/目录的系统AI用户ID */
        private long aiUserId = 85L;
        /** 执行器轮询间隔（秒） */
        private int pollIntervalSec = 10;

        /** AI服务: opencode, gemini, openrouter */
        private String aiService = "openrouter";
        /** AI模型名称 */
        private String model = "deepseek/deepseek-chat";

        // ========== OpenCode 相关配置 ==========
        /** OpenCode 本地服务基础URL，用于调用生成接口 */
        private String opencodeBaseUrl = "http://127.0.0.1:4096";
        /** OpenCode 的 provider ID */
        private String providerId = "github-copilot";

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
        private int roleNameMinLength = 2;
        /** 专业名称最大长度 */
        private int roleNameMaxLength = 30;
        /** 专业描述最小长度 */
        private int roleDescriptionMinLength = 20;
        /** 专业描述最大长度 */
        private int roleDescriptionMaxLength = 2000;

        // ========== 记忆卡片相关 ==========
        /** 卡片正面最小长度 */
        private int cardFrontMinLength = 5;
        /** 卡片正面最大长度 */
        private int cardFrontMaxLength = 500;
        /** 卡片背面最小长度 */

        private int cardBackMinLength = 1;
        /** 卡片背面最大长度 */
        private int cardBackMaxLength = 500;
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
         * 用户最大学习卡片数量限制
         */
        private int maxCardsPerUser = 10000;

        /**
         * 每日新卡上限默认值
         */
        private int defaultDailyNewLimit = 20;

        /**
         * 每日复习上限默认值
         */
        private int defaultDailyReviewLimit = 100;

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
}