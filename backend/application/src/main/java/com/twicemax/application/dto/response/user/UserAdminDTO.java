package com.twicemax.application.dto.response.user;

import lombok.Data;

/**
 * 用户管理 DTO
 *
 * 用途：管理后台使用的用户信息
 *
 * 使用场景：
 * - 管理后台用户列表
 * - 管理后台用户详情
 *
 * @author Claude
 * @since 2025-03-04
 */
@Data
public class UserAdminDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String name;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 邮箱验证状态
     */
    private Boolean emailValidated;

    /**
     * 个人简介
     */
    private String biography;

    /**
     * 用户状态
     * 说明：1-正常, 2-已屏蔽
     */
    private Byte state;

    /**
     * 用户角色
     * 说明：USER(0), MODERATOR(1), ADMIN(2), SUPER_ADMIN(3)
     */
    private Integer role;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;

    // ==================== 统计字段 ====================

    /**
     * 总浏览量
     */
    private Integer viewCount;

    /**
     * 总两次能懂点赞数
     */
    private Integer twiceCount;

    /**
     * 总有用点赞数
     */
    private Integer likeCount;

    /**
     * 总评论数
     */
    private Integer commentCount;

    /**
     * 正在学习课程数
     */
    private Integer learningCourseCount;

    /**
     * 已完成课程数
     */
    private Integer completedCourseCount;

    /**
     * 正在学习角色数
     */
    private Integer inProgressRoleCount;

    /**
     * 已完成角色数
     */
    private Integer completedRoleCount;

    /**
     * 关注用户数
     */
    private Integer followingUserCount;

    /**
     * 关注课程数
     */
    private Integer followingCourseCount;

    /**
     * 关注角色数
     */
    private Integer followingRoleCount;

    /**
     * 创建文章数
     */
    private Integer createdArticleCount;

    /**
     * 创建目录数
     */
    private Integer createdIndexCount;

    /**
     * 创建路线图数
     */
    private Integer createdRoadmapCount;

    /**
     * 创建卡片组数
     */
    private Integer createdCardDeckCount;
}
