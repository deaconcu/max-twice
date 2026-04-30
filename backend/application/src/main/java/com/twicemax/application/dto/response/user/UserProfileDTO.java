package com.twicemax.application.dto.response.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户完整资料 DTO
 *
 * 用途：用户自己的完整个人资料（包含所有信息和敏感字段）
 *
 * 使用场景：
 * - 用户查看自己的个人资料
 * - 用户编辑个人信息
 * - 管理后台查看用户详情
 *
 * 替代关系：
 * - 替代原 toDTO（完整字段版本）
 *
 * 安全性：
 * - ⚠️ 包含敏感信息（phone, email）
 * - ⚠️ 仅允许用户本人或管理员访问
 * - ❌ 不应返回 password 字段（已排除）
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserProfileDTO extends UserSummaryDTO {

    /**
     * 手机号
     * 说明：用户的手机号码
     * 安全性：敏感信息，仅用户本人和管理员可见
     */
    private String phone;

    /**
     * 邮箱
     * 说明：用户的电子邮箱
     * 安全性：敏感信息，仅用户本人和管理员可见
     */
    private String email;

    /**
     * 邮箱验证状态
     * 说明：邮箱是否已验证
     */
    private Boolean emailValidated;

    /**
     * 用户角色
     * 说明：USER, MODERATOR, ADMIN, SUPER
     */
    private Integer role;

    /**
     * 创建时间
     * 说明：用户注册时间
     */
    private String createdAt;

    /**
     * 更新时间
     * 说明：用户信息最后更新时间
     */
    private String updatedAt;

    /**
     * 用户时区
     * 说明：IANA 时区格式，如 America/Los_Angeles
     */
    private String timezone;

    /**
     * 用户偏好语言
     * 说明：'zh' / 'en'，决定 UI 语言和内容库路由
     */
    private String locale;

    /**
     * 是否已设置密码
     * 说明：邮箱验证码登录自动建号时 password=null；用户可在登录后单独设置密码
     */
    private Boolean hasPassword;
}
