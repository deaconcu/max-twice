package com.prosper.learn.shared.domain;

import com.prosper.learn.shared.domain.exception.StatusCode;

import java.util.Arrays;
import java.util.Objects;

public class Enums {

    /**
     * 基础枚举接口，用于减少重复代码
     */
    public interface ValueEnum<T> {
        T value();
        
        static <E extends Enum<E> & ValueEnum<T>, T> E getByValue(Class<E> enumClass, T value) {
            if (value == null) return null;
            return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> Objects.equals(e.value(), value))
                .findFirst()
                .orElse(null);
        }
        
        static <E extends Enum<E> & ValueEnum<T>, T> boolean isValid(Class<E> enumClass, T value) {
            return getByValue(enumClass, value) != null;
        }
    }

    public enum UserState implements ValueEnum<Byte> {
        ACTIVE((byte)1),
        BANNED((byte)2);

        // 静态常量用于 MyBatis 注解（注解需要编译时常量）
        public static final byte ACTIVE_VALUE = 1;
        public static final byte BANNED_VALUE = 2;

        private final byte value;

        UserState(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static UserState getByValue(Integer value) {
            return value == null ? null : ValueEnum.getByValue(UserState.class, value.byteValue());
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(UserState.class, (byte)value);
        }
    }

    /**
     * 用户角色枚举
     * 角色代码按添加顺序分配，用于唯一标识角色
     * 权限级别用于权限比较
     */
    public enum UserRole implements ValueEnum<Integer> {
        USER(0, "user", "普通用户", 0),
        MODERATOR(1, "moderator", "审核员", 30),
        ADMIN(2, "admin", "管理员", 60),
        SUPER_ADMIN(3, "super_admin", "超级管理员", 100);

        private final int code;           // 数据库存储值
        private final String name;        // Sa-Token 使用的角色名
        private final String description; // 角色描述
        private final int level;          // 权限级别（用于权限比较）

        UserRole(int code, String name, String description, int level) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.level = level;
        }

        @Override
        public Integer value() {
            return code;
        }

        public int getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public int getLevel() {
            return level;
        }

        /**
         * 根据 code 获取枚举
         */
        public static UserRole fromCode(Integer code) {
            if (code == null) {
                return USER; // 默认为普通用户
            }
            for (UserRole role : values()) {
                if (role.code == code) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Unknown role code: " + code);
        }

        public static UserRole getByValue(Integer value) {
            return fromCode(value);
        }

        public static boolean isValid(int code) {
            try {
                fromCode(code);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        /**
         * 判断当前角色是否等于或高于指定角色
         */
        public boolean equalOrHigher(UserRole role) {
            return this.level >= role.level;
        }
    }

    public enum ContentType implements ValueEnum<Integer> {
        post(1),
        node(2),
        comment(3),
        roadmap(4),
        memory_card_deck(5),
        memory_card(6),
        role(7),
        course(8);

        // 静态常量用于 MyBatis 注解（注解需要编译时常量）
        public static final byte POST_VALUE = 1;
        public static final byte NODE_VALUE = 2;
        public static final byte COMMENT_VALUE = 3;
        public static final byte ROADMAP_VALUE = 4;
        public static final byte MEMORY_CARD_DECK_VALUE = 5;
        public static final byte MEMORY_CARD_VALUE = 6;
        public static final byte ROLE_VALUE = 7;
        public static final byte COURSE_VALUE = 8;

        private final int value;

        ContentType(int value) {
            this.value = value;
        }

        @Override
        public Integer value() {
            return value;
        }

        public byte byteValue() {
            return (byte) value;
        }

        public static ContentType getByValue(Integer value) {
            return ValueEnum.getByValue(ContentType.class, value);
        }

        public static ContentType parse(String name) {
            try {
                return ContentType.valueOf(name.toLowerCase());
            } catch (IllegalArgumentException e) {
                throw StatusCode.INVALID_PARAMETER.exception("不支持的内容类型: " + name);
            }
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(ContentType.class, value);
        }
    }

    // TODO 上线前需要都+1
    public enum ContentState implements ValueEnum<Byte> {
        DRAFT((byte)0),
        SUBMITTED((byte)1),
        PUBLISHED((byte)2),
        REJECTED((byte)3),
        BANNED((byte)4);

        // 静态常量用于 MyBatis 注解（注解需要编译时常量）
        public static final byte DRAFT_VALUE = 0;
        public static final byte SUBMITTED_VALUE = 1;
        public static final byte PUBLISHED_VALUE = 2;
        public static final byte REJECTED_VALUE = 3;
        public static final byte BANNED_VALUE = 4;

        private final byte value;

        ContentState(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static ContentState getByValue(Byte value) {
            return value == null ? null : ValueEnum.getByValue(ContentState.class, value);
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(ContentState.class, (byte)value);
        }
    }

    public enum PostType implements ValueEnum<Integer> {
        index(1),
        article(2);

        private final int value;

        PostType(int value) {
            this.value = value;
        }

        @Override
        public Integer value() {
            return value;
        }

        public static PostType getByValue(Integer value) {
            return ValueEnum.getByValue(PostType.class, value);
        }

        /**
         * 从字符串转换为 PostType
         * @param type "content" 或 "article"
         * @return 对应的 PostType，默认返回 article
         */
        public static PostType fromString(String type) {
            return "content".equals(type) ? index : article;
        }
    }

    public enum VoteType implements ValueEnum<Integer> {
        twice(1),
        like(2);

        private final int value;

        VoteType(int value) {
            this.value = value;
        }

        @Override
        public Integer value() {
            return value;
        }

        public static VoteType getByValue(Integer value) {
            return ValueEnum.getByValue(VoteType.class, value);
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(VoteType.class, value);
        }
    }

    public enum Bool implements ValueEnum<Byte> {
        TRUE((byte)1),
        FALSE((byte)0);

        private final byte value;

        Bool(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static Bool getByValue(Integer value) {
            return value == null ? null : ValueEnum.getByValue(Bool.class, value.byteValue());
        }
    }

    /**
     * 消息分类枚举
     * INTERACTION=1 互动消息
     * SYSTEM=2 系统消息
     * ALL=3 全部（互动+系统，不包括私信）
     * PRIVATE=4 私信
     */
    public enum MessageCategory implements ValueEnum<Integer> {
        INTERACTION(1, "互动消息"),
        SYSTEM(2, "系统消息"),
        ALL(3, "全部"),
        PRIVATE(4, "私信");

        private final int value;
        private final String description;

        MessageCategory(int value, String description) {
            this.value = value;
            this.description = description;
        }

        @Override
        public Integer value() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static MessageCategory getByValue(Integer value) {
            return ValueEnum.getByValue(MessageCategory.class, value);
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(MessageCategory.class, value);
        }
    }

    public enum MessageType implements ValueEnum<Integer> {
        applyCourse(1, 2),           // 课程申请 - 系统消息
        follow(2, 1),                // 关注 - 互动消息
        upvote(3, 1),                // 点赞 - 互动消息
        invite(4, 1),                // 邀请 - 互动消息
        nodeComment(5, 1),           // 节点评论 - 互动消息
        postComment(6, 1),           // 帖子评论 - 互动消息
        replyNodeComment(7, 1),      // 回复节点评论 - 互动消息
        replyPostingComment(8, 1),   // 回复帖子评论 - 互动消息
        replyRoadmapComment(9, 1),   // 回复路线图评论 - 互动消息
        roadmapComment(10, 1),       // 路线图评论 - 互动消息

        // 审核消息类型（新增14个）
        courseRejected(11, 2),       // 课程被拒绝 - 系统消息
        courseBanned(12, 2),         // 课程被封禁 - 系统消息
        postRejected(13, 2),         // 帖子被拒绝 - 系统消息
        postBanned(14, 2),           // 帖子被封禁 - 系统消息
        commentRejected(15, 2),      // 评论被拒绝 - 系统消息
        commentBanned(16, 2),        // 评论被封禁 - 系统消息
        roleRejected(17, 2),   // 角色被拒绝 - 系统消息
        roleBanned(18, 2),     // 角色被封禁 - 系统消息
        roadmapRejected(19, 2),      // 路线图被拒绝 - 系统消息
        roadmapBanned(20, 2),        // 路线图被封禁 - 系统消息
        memoryDeckRejected(21, 2),   // 卡片组被拒绝 - 系统消息
        memoryDeckBanned(22, 2),     // 卡片组被封禁 - 系统消息
        nodeRejected(23, 2),         // 节点被拒绝 - 系统消息
        nodeBanned(24, 2),           // 节点被封禁 - 系统消息
        courseApproved(25, 2),       // 课程审核通过 - 系统消息
        roleApproved(26, 2),   // 角色审核通过 - 系统消息

        system(99, 2),               // 其他系统消息 - 系统消息
        other(100, 4);               // 私信 - 私信（category=4）

        private final int value;
        private final int category;

        MessageType(int value, int category) {
            this.value = value;
            this.category = category;
        }

        @Override
        public Integer value() {
            return value;
        }

        public int getCategory() {
            return category;
        }

        public static MessageType getByValue(Integer value) {
            return ValueEnum.getByValue(MessageType.class, value);
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(MessageType.class, value);
        }
    }

    /**
     * 审核操作枚举
     */
    public enum ModerationAction {
        APPROVED,   // 审核通过
        REJECTED,   // 审核拒绝
        BANNED      // 封禁
    }

    /**
     * 操作级别枚举（用于操作日志）
     */
    public enum OperationLevel implements ValueEnum<Integer> {
        LOW(1, "低"),      // 审核通过、恢复内容
        MEDIUM(2, "中"),   // 审核拒绝、临时屏蔽
        HIGH(3, "高");     // 删除、封禁、修改角色

        private final int code;
        private final String description;

        OperationLevel(int code, String description) {
            this.code = code;
            this.description = description;
        }

        @Override
        public Integer value() {
            return code;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static OperationLevel getByValue(Integer value) {
            return ValueEnum.getByValue(OperationLevel.class, value);
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(OperationLevel.class, value);
        }
    }



    /**
     * 主分类枚举
     */
    public enum MainCategory {
        TECH(1, "技术开发"),
        DESIGN(2, "设计创意"),
        MARKETING(3, "市场营销"),
        FINANCE(4, "金融财会"),
        MANAGEMENT(5, "管理咨询"),
        EDUCATION(6, "教育培训"),
        LIFE(7, "生活角色");

        private final int id;
        private final String name;

        MainCategory(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        /**
         * 根据ID获取枚举
         */
        public static MainCategory getById(int id) {
            for (MainCategory category : values()) {
                if (category.getId() == id) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Invalid main category id: " + id);
        }

        /**
         * 根据名称获取枚举
         */
        public static MainCategory getByName(String name) {
            for (MainCategory category : values()) {
                if (category.getName().equals(name)) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Invalid main category name: " + name);
        }
    }

    /**
     * 用户进度状态枚举 (用于 UserCourse 和 UserRoadmap)
     * NOT_STARTED=0, IN_PROGRESS=1, COMPLETED=2
     */
    public enum UserProgressState implements ValueEnum<Byte> {
        NOT_STARTED((byte)0),
        IN_PROGRESS((byte)1),
        COMPLETED((byte)2);

        // 静态常量用于 MyBatis 注解（注解需要编译时常量）
        public static final byte NOT_STARTED_VALUE = 0;
        public static final byte IN_PROGRESS_VALUE = 1;
        public static final byte COMPLETED_VALUE = 2;

        private final byte value;

        UserProgressState(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static UserProgressState getByValue(Integer value) {
            return value == null ? null : ValueEnum.getByValue(UserProgressState.class, value.byteValue());
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(UserProgressState.class, (byte)value);
        }

        /**
         * 根据字符串名称解析状态
         * @param name 状态名称（learning/completed，不区分大小写）
         * @return 对应的状态枚举，无效值返回 null
         */
        public static UserProgressState fromName(String name) {
            if (name == null || name.isEmpty()) {
                return null;
            }
            return switch (name.toLowerCase()) {
                case "learning" -> IN_PROGRESS;
                case "completed" -> COMPLETED;
                default -> null;
            };
        }
    }

    /**
     * 验证码类型枚举
     * REGISTER=1, RESET_PASSWORD=2, CHANGE_EMAIL=3
     */
    public enum VerificationType implements ValueEnum<Byte> {
        REGISTER((byte)1, "注册"),
        RESET_PASSWORD((byte)2, "找回密码"),
        CHANGE_EMAIL((byte)3, "修改邮箱");

        // 静态常量用于 MyBatis 注解
        public static final byte REGISTER_VALUE = 1;
        public static final byte RESET_PASSWORD_VALUE = 2;
        public static final byte CHANGE_EMAIL_VALUE = 3;

        private final byte value;
        private final String description;

        VerificationType(byte value, String description) {
            this.value = value;
            this.description = description;
        }

        @Override
        public Byte value() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static VerificationType getByValue(Integer value) {
            return value == null ? null : ValueEnum.getByValue(VerificationType.class, value.byteValue());
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(VerificationType.class, (byte)value);
        }
    }


    public enum DTOVersion {
        V1, V2, V3, V4, V5, V6, V7, V8, V9, V10
    }

    /**
     * 子分类枚举
     */
    public enum SubCategory {
        // 技术开发子分类 (1-4)
        FRONTEND_DEV(1, "前端开发"),
        BACKEND_DEV(2, "后端开发"),
        MOBILE_DEV(3, "移动开发"),
        DATA_SCIENCE(4, "数据科学"),

        // 设计创意子分类 (5-6)
        UX_DESIGN(5, "用户体验设计"),
        VISUAL_DESIGN(6, "视觉设计"),

        // 市场营销子分类 (7-8)
        DIGITAL_MARKETING(7, "数字营销"),
        BRAND_MANAGEMENT(8, "品牌管理"),

        // 金融财会子分类 (9-10)
        FINANCIAL_MANAGEMENT(9, "财务管理"),
        INVESTMENT_MANAGEMENT(10, "投资管理"),

        // 管理咨询子分类 (11-12)
        PRODUCT_MANAGEMENT(11, "产品管理"),
        STRATEGY_CONSULTING(12, "战略咨询"),

        // 教育培训子分类 (13-14)
        SUBJECT_EDUCATION(13, "学科教育"),
        ONLINE_EDUCATION(14, "在线教育"),

        // 生活角色子分类 (15-18)
        FAMILY_ROLE(15, "家庭角色"),
        SOCIAL_ROLE(16, "社会角色"),
        PROFESSIONAL_SERVICE(17, "专业服务"),
        PERSONAL_GROWTH(18, "个人成长");

        private final int id;
        private final String name;

        SubCategory(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        /**
         * 根据ID获取枚举
         */
        public static SubCategory getById(int id) {
            for (SubCategory category : values()) {
                if (category.getId() == id) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Invalid sub category id: " + id);
        }

        /**
         * 根据名称获取枚举
         */
        public static SubCategory getByName(String name) {
            for (SubCategory category : values()) {
                if (category.getName().equals(name)) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Invalid sub category name: " + name);
        }

        /**
         * 根据主分类获取对应的子分类列表
         */
        public static SubCategory[] getByMainCategory(MainCategory mainCategory) {
            switch (mainCategory) {
                case TECH:
                    return new SubCategory[]{FRONTEND_DEV, BACKEND_DEV, MOBILE_DEV, DATA_SCIENCE};
                case DESIGN:
                    return new SubCategory[]{UX_DESIGN, VISUAL_DESIGN};
                case MARKETING:
                    return new SubCategory[]{DIGITAL_MARKETING, BRAND_MANAGEMENT};
                case FINANCE:
                    return new SubCategory[]{FINANCIAL_MANAGEMENT, INVESTMENT_MANAGEMENT};
                case MANAGEMENT:
                    return new SubCategory[]{PRODUCT_MANAGEMENT, STRATEGY_CONSULTING};
                case EDUCATION:
                    return new SubCategory[]{SUBJECT_EDUCATION, ONLINE_EDUCATION};
                case LIFE:
                    return new SubCategory[]{FAMILY_ROLE, SOCIAL_ROLE, PROFESSIONAL_SERVICE, PERSONAL_GROWTH};
                default:
                    return new SubCategory[]{};
            }
        }

        /**
         * 根据子分类ID获取对应的主分类
         */
        public MainCategory getMainCategory() {
            if (id >= 1 && id <= 4) {
                return MainCategory.TECH;
            } else if (id >= 5 && id <= 6) {
                return MainCategory.DESIGN;
            } else if (id >= 7 && id <= 8) {
                return MainCategory.MARKETING;
            } else if (id >= 9 && id <= 10) {
                return MainCategory.FINANCE;
            } else if (id >= 11 && id <= 12) {
                return MainCategory.MANAGEMENT;
            } else if (id >= 13 && id <= 14) {
                return MainCategory.EDUCATION;
            } else if (id >= 15 && id <= 18) {
                return MainCategory.LIFE;
            } else {
                throw new IllegalArgumentException("Invalid sub category id: " + id);
            }
        }
    }



    /**
     * 复习频率设置枚举
     * HIGH=0, NORMAL=1, LOW=2
     */
    public enum FrequencySetting implements ValueEnum<Byte> {
        HIGH((byte) 1),         // 高频
        NORMAL((byte) 2),       // 普通
        LOW((byte) 3);          // 低频

        private final byte value;

        FrequencySetting(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static FrequencySetting getByValue(Integer value) {
            return value == null ? null : ValueEnum.getByValue(FrequencySetting.class, value.byteValue());
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(FrequencySetting.class, (byte)value);
        }
    }

    /**
     * 课程学习状态枚举
     * STUDYING=1, FROZEN=2, HIDDEN=3
     */
    public enum DeckCourseStudyState implements ValueEnum<Byte> {
        STUDYING((byte) 1),     // 学习中
        FROZEN((byte) 2),       // 冻结（到期时间暂停累积）
        HIDDEN((byte) 3);       // 隐藏（到期时间继续累积）

        private final byte value;

        DeckCourseStudyState(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static DeckCourseStudyState getByValue(Integer value) {
            return value == null ? null : ValueEnum.getByValue(DeckCourseStudyState.class, value.byteValue());
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(DeckCourseStudyState.class, (byte)value);
        }
    }

    /**
     * 卡片顺序枚举
     * REVIEW_FIRST=0 先复习后新卡（默认）
     * NEW_FIRST=1 先新卡后复习
     */
    public enum CardOrder implements ValueEnum<Byte> {
        REVIEW_FIRST((byte) 0),   // 先复习后新卡
        NEW_FIRST((byte) 1);      // 先新卡后复习

        private final byte value;

        CardOrder(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static CardOrder getByValue(Byte value) {
            return value == null ? REVIEW_FIRST : ValueEnum.getByValue(CardOrder.class, value);
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(CardOrder.class, (byte)value);
        }
    }

    /**
     * 复习结果枚举
     * FAILED=0, HARD=1, GOOD=2, EASY=3
     */
    public enum ReviewResult implements ValueEnum<Integer> {
        FAILED(1),              // 忘记了
        HARD(2),                // 困难
        GOOD(3),                // 良好
        EASY(4);                // 简单

        private final int value;

        ReviewResult(int value) {
            this.value = value;
        }

        @Override
        public Integer value() {
            return value;
        }

        public static ReviewResult getByValue(Integer value) {
            return ValueEnum.getByValue(ReviewResult.class, value);
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(ReviewResult.class, value);
        }
    }

    /**
     * 统计周期枚举
     */
    public enum Period implements ValueEnum<String> {
        DAY("day"),
        WEEK("week"),
        MONTH("month"),
        YEAR("year");

        private final String value;

        Period(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        public static Period getByValue(String value) {
            return ValueEnum.getByValue(Period.class, value);
        }

        public static boolean isValid(String value) {
            return ValueEnum.isValid(Period.class, value);
        }
    }

    /**
     * 日度增量统计类型枚举
     */
    public enum DailyStatType {
        VIEWS("daily_views", "当日浏览量"),
        TWICE("daily_twice", "当日两次能懂点赞数"),
        HELPFUL("daily_helpful", "当日有帮助点赞数"),
        COMMENTS("daily_comments", "当日评论数");

        private final String fieldName;
        private final String description;

        DailyStatType(String fieldName, String description) {
            this.fieldName = fieldName;
            this.description = description;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 内容操作类型枚举（目录选择、固定等）
     */
    public enum ContentAction implements ValueEnum<Integer> {
        CHOOSE(1, "选择内容"),
        UNCHOOSE(2, "取消选择");

        private final int value;
        private final String description;

        ContentAction(int value, String description) {
            this.value = value;
            this.description = description;
        }

        @Override
        public Integer value() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public static ContentAction getByValue(Integer value) {
            return ValueEnum.getByValue(ContentAction.class, value);
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(ContentAction.class, value);
        }
    }

    /**
     * 累计统计类型枚举
     */
    public enum CumulativeStatType {
        // 学习统计
        LEARNING_COURSES("learning_courses", "正在学习课程数"),
        COMPLETED_COURSES("completed_courses", "已完成课程数"),
        IN_PROGRESS_ROLES("in_progress_roles", "正在进行角色数"),
        COMPLETED_ROLES("completed_roles", "已完成角色数"),

        // 社交统计
        FOLLOWING_USERS("following_users", "关注的人数"),
        FOLLOWING_COURSES("following_courses", "关注的课程数"),
        FOLLOWING_ROLES("following_roles", "关注的角色数"),

        // 创作统计
        CREATED_ARTICLES("created_articles", "创建的文章数"),
        CREATED_INDEXS("created_indexs", "创建的目录数"),
        CREATED_ROADMAPS("created_roadmaps", "创建的路线图数"),
        CREATED_CARD_DECKS("created_card_decks", "创建的卡片组数");

        private final String fieldName;
        private final String description;

        CumulativeStatType(String fieldName, String description) {
            this.fieldName = fieldName;
            this.description = description;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getDescription() {
            return description;
        }
    }
}
