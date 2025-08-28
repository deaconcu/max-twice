package com.prosper.learn.common;

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

    public enum CourseState implements ValueEnum<Byte> {
        SUBMITTED((byte)0),
        APPROVED((byte)1),
        REJECTED((byte)2);

        private final byte value;

        CourseState(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static CourseState getByValue(Integer value) {
            return value == null ? null : ValueEnum.getByValue(CourseState.class, value.byteValue());
        }
        
        public static boolean isValid(int value) {
            return ValueEnum.isValid(CourseState.class, (byte)value);
        }
    }

    public enum CourseRequestState implements ValueEnum<Integer> {
        submitted(1),
        proved(2),
        reject(3);

        private final int value;

        CourseRequestState(int value) {
            this.value = value;
        }

        @Override
        public Integer value() {
            return value;
        }

        public static CourseRequestState getByValue(Integer value) {
            return ValueEnum.getByValue(CourseRequestState.class, value);
        }
    }

    public enum PostType implements ValueEnum<Integer> {
        contents(1),
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
    }

    public enum PostState implements ValueEnum<Integer> {
        submited(0),
        approved(1),
        deleted(2);

        private final int value;

        PostState(int value) {
            this.value = value;
        }

        @Override
        public Integer value() {
            return value;
        }

        public static PostState getByValue(Integer value) {
            return ValueEnum.getByValue(PostState.class, value);
        }
    }

    public enum CommentState implements ValueEnum<Integer> {
        submited(0),
        approved(1),
        deleted(2);

        private final int value;

        CommentState(int value) {
            this.value = value;
        }

        @Override
        public Integer value() {
            return value;
        }

        public static CommentState getByValue(Integer value) {
            return ValueEnum.getByValue(CommentState.class, value);
        }
    }

    public enum VoteType implements ValueEnum<Integer> {
        once(1),
        twice(2),
        helpful(3);

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

    public enum MessageType implements ValueEnum<Integer> {
        applyCourse(1),
        follow(2),
        upvote(3),
        invite(4),
        nodeComment(5),
        postComment(6),
        roadmapComment(10),
        replyNodeComment(7),
        replyPostingComment(8),
        replyRoadmapComment(9),
        system(99),
        other(100);

        private final int value;

        MessageType(int value) {
            this.value = value;
        }

        @Override
        public Integer value() {
            return value;
        }

        public static MessageType getByValue(Integer value) {
            return ValueEnum.getByValue(MessageType.class, value);
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(MessageType.class, value);
        }
    }

    public enum ObjectType implements ValueEnum<Integer> {
        post(0),
        node(1),
        comment(2),
        roadmap(3);

        private final int value;

        ObjectType(int value) {
            this.value = value;
        }

        @Override
        public Integer value() {
            return value;
        }

        public static ObjectType getByValue(Integer value) {
            return ValueEnum.getByValue(ObjectType.class, value);
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(ObjectType.class, value);
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
     * Profession表状态枚举
     * SUBMITTED=0, APPROVED=1, REJECTED=2
     */
    public enum ProfessionState implements ValueEnum<Byte> {
        SUBMITTED((byte)0),
        APPROVED((byte)1),
        REJECTED((byte)2);

        private final byte value;

        ProfessionState(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static ProfessionState getByValue(Integer value) {
            return value == null ? null : ValueEnum.getByValue(ProfessionState.class, value.byteValue());
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(ProfessionState.class, (byte)value);
        }
    }

    /**
     * UserCourse表状态枚举 (原status字段，现改为state)
     * NOT_STARTED=0, IN_PROGRESS=1, COMPLETED=2
     */
    public enum UserCourseState implements ValueEnum<Byte> {
        NOT_STARTED((byte)0),
        IN_PROGRESS((byte)1),
        COMPLETED((byte)2);

        private final byte value;

        UserCourseState(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static UserCourseState getByValue(Integer value) {
            return value == null ? null : ValueEnum.getByValue(UserCourseState.class, value.byteValue());
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(UserCourseState.class, (byte)value);
        }
    }

    /**
     * UserRoadmap表状态枚举 (原status字段，现改为state)
     * NOT_STARTED=0, IN_PROGRESS=1, COMPLETED=2
     */
    public enum UserRoadmapState implements ValueEnum<Byte> {
        NOT_STARTED((byte)0),
        IN_PROGRESS((byte)1),
        COMPLETED((byte)2);

        private final byte value;

        UserRoadmapState(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static UserRoadmapState getByValue(Integer value) {
            return value == null ? null : ValueEnum.getByValue(UserRoadmapState.class, value.byteValue());
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(UserRoadmapState.class, (byte)value);
        }
    }

    /**
     * PostStats表类型枚举
     * POST=0, ROADMAP=1
     */
    public enum PostStatsType implements ValueEnum<Byte> {
        POST((byte)0),
        ROADMAP((byte)1);

        private final byte value;

        PostStatsType(byte value) {
            this.value = value;
        }

        @Override
        public Byte value() {
            return value;
        }

        public static PostStatsType getByValue(Integer value) {
            return value == null ? null : ValueEnum.getByValue(PostStatsType.class, value.byteValue());
        }

        public static boolean isValid(int value) {
            return ValueEnum.isValid(PostStatsType.class, (byte)value);
        }
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
}
