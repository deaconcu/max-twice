package com.twicemax.shared.domain;

import com.twicemax.shared.domain.exception.StatusCode;

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

    /**
     * String 枚举接口：value() 默认返回 name().toLowerCase()
     * 适用于枚举常量名即为存储值（大写常量名，小写存储）的场景
     */
    public interface StringValueEnum extends ValueEnum<String> {
        String name(); // Enum 天然实现，无需手写

        @Override
        default String value() {
            return name().toLowerCase();
        }
    }

    public enum UserState implements StringValueEnum {
        ACTIVE,
        BANNED;

        public static UserState getByValue(String value) {
            return ValueEnum.getByValue(UserState.class, value);
        }
    }

    /**
     * 用户角色枚举
     * 权限级别用于权限比较
     */
    public enum UserRole implements StringValueEnum {
        USER("普通用户", 0),
        MODERATOR("审核员", 30),
        ADMIN("管理员", 60),
        SUPER("超级管理员", 100);

        private final String description;
        private final int level;

        UserRole(String description, int level) {
            this.description = description;
            this.level = level;
        }

        public String getName() {
            return value();
        }

        public String getDescription() {
            return description;
        }

        public int getLevel() {
            return level;
        }

        public static UserRole fromName(String name) {
            if (name == null) {
                return USER;
            }
            return ValueEnum.getByValue(UserRole.class, name);
        }

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

    /**
     * roadmap 主体状态。配合 content_revision 表使用：
     * - NEVER_PUBLISHED：从未发布过，对外完全不可见
     * - PUBLISHED：至少发布过一次（可能同时存在 pending revision 在审核）
     * - BANNED：被管理员封禁，对外不可见
     *
     * SUBMITTED / REJECTED / WITHDRAWN 是 revision 的生命周期，不在这里，见 {@link RevisionStatus}。
     */
    public enum NewContentState implements StringValueEnum {
        NEVER_PUBLISHED,
        PUBLISHED,
        BANNED;

        public static NewContentState getByValue(String value) {
            return ValueEnum.getByValue(NewContentState.class, value);
        }
    }

    /**
     * content_revision 表中每一行（一次提交快照）的生命周期状态。
     * 与 RoadmapState 解耦：roadmap 主体状态描述对外可见性，revision 状态描述这次提交本身。
     *
     * - SUBMITTED：已提交，等待审核
     * - PUBLISHED：审核通过，是某个时刻被发布过的版本（历史已发布版仍保留 PUBLISHED）
     * - REJECTED：审核被拒
     * - WITHDRAWN：作者主动撤回
     */
    public enum RevisionStatus implements StringValueEnum {
        SUBMITTED,
        PUBLISHED,
        REJECTED,
        WITHDRAWN;

        public static RevisionStatus getByValue(String value) {
            return ValueEnum.getByValue(RevisionStatus.class, value);
        }
    }

    /**
     * 字符串版的内容类型枚举，用于新链路（如 content_revision 表）。
     * 老链路仍使用 {@link ContentType}（Integer），未来逐步迁移到此处后废弃旧版。
     */
    public enum NewContentType implements StringValueEnum {
        ROADMAP,
        ROLE,
        COURSE;

        public static NewContentType getByValue(String value) {
            return ValueEnum.getByValue(NewContentType.class, value);
        }
    }

    public enum PostType implements StringValueEnum {
        INDEX,
        ARTICLE;

        public static PostType getByValue(String value) {
            return ValueEnum.getByValue(PostType.class, value);
        }
    }

    public enum VoteType implements StringValueEnum {
        TWICE,
        LIKE;

        public static VoteType getByValue(String value) {
            return ValueEnum.getByValue(VoteType.class, value);
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
    public enum OperationLevel implements StringValueEnum {
        LOW("低"),      // 审核通过、恢复内容
        MEDIUM("中"),   // 审核拒绝、临时屏蔽
        HIGH("高");     // 删除、封禁、修改角色

        private final String description;

        OperationLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static OperationLevel getByValue(String value) {
            return ValueEnum.getByValue(OperationLevel.class, value);
        }

        public static boolean isValid(String value) {
            return ValueEnum.isValid(OperationLevel.class, value);
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
}
