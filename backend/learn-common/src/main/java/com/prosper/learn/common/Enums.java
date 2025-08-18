package com.prosper.learn.common;

import java.util.Arrays;

public class Enums {

    public enum CourseState {
        all((byte)0),
        created((byte)1),
        proved((byte)2),
        rejected((byte)3);

        public final byte value;

        CourseState(byte value) {
            this.value = value;
        }

        public static CourseState getStateByValue(Integer value) {
            return Arrays.stream(CourseState.values())
                .filter(enumStateValue -> enumStateValue.value == value).findFirst().orElse(null);
        }
    }

    public enum CourseRequestState {
        submitted(1),
        proved(2),
        reject(3);

        public final int value;

        CourseRequestState(int value) {
            this.value = value;
        }

        public static CourseRequestState getStateByValue(Integer value) {
            return Arrays.stream(CourseRequestState.values())
                .filter(enumStateValue -> enumStateValue.value == value).findFirst().orElse(null);
        }
    }

    public enum PostType {
        contents(1),
        article(2);

        public final int value;

        PostType(int value) {
            this.value = value;
        }

        public static PostType getStateByValue(Integer value) {
            return Arrays.stream(PostType.values())
                    .filter(enumStateValue -> enumStateValue.value == value).findFirst().orElse(null);
        }
    }

    public enum PostState {
        submited(0),
        approved(1),
        deleted(2);

        public final int value;

        PostState(int value) {
            this.value = value;
        }

        public static PostState getStateByValue(Integer value) {
            return Arrays.stream(PostState.values())
                    .filter(enumStateValue -> enumStateValue.value == value).findFirst().orElse(null);
        }
    }

    public enum CommentState {
        submited(0),
        approved(1),
        deleted(2);

        public final int value;

        CommentState(int value) {
            this.value = value;
        }

        public static CommentState getStateByValue(Integer value) {
            return Arrays.stream(CommentState.values())
                    .filter(enumStateValue -> enumStateValue.value == value).findFirst().orElse(null);
        }
    }

    public enum VoteType {
        once(1),
        twice(2),
        helpful(3);

        public final int value;

        VoteType(int value) {
            this.value = value;
        }

        public static VoteType getStateByValue(Integer value) {
            return Arrays.stream(VoteType.values())
                    .filter(enumStateValue -> enumStateValue.value == value).findFirst().orElse(null);
        }

        public static boolean isValid(int value) {
            for (VoteType s : VoteType.values()) {
                if (s.value == value) return true;
            }
            return false;
        }
    }

    public enum Bool {
        TRUE((byte)1),
        FALSE((byte)0);

        public final byte value;

        Bool(byte value) {
            this.value = value;
        }

        public static Bool getStateByValue(Integer value) {
            return Arrays.stream(Bool.values())
                    .filter(enumStateValue -> enumStateValue.value == value).findFirst().orElse(null);
        }
    }

    public enum MessageType {
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

        public final int value;

        MessageType(int value) {
            this.value = value;
        }

        public static MessageType getStateByValue(Integer value) {
            return Arrays.stream(MessageType.values())
                    .filter(enumStateValue -> enumStateValue.value == value).findFirst().orElse(null);
        }

        public static boolean isValid(int value) {
            for (MessageType s : MessageType.values()) {
                if (s.value == value) return true;
            }
            return false;
        }
    }

    public enum ObjectType {
        post(0),
        node(1),
        comment(2),
        roadmap(3);

        public final int value;

        ObjectType(int value) {
            this.value = value;
        }

        public static ObjectType getStateByValue(Integer value) {
            return Arrays.stream(ObjectType.values())
                    .filter(enumStateValue -> enumStateValue.value == value).findFirst().orElse(null);
        }

        public static boolean isValid(int value) {
            for (ObjectType s : ObjectType.values()) {
                if (s.value == value) return true;
            }
            return false;
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
