package com.prosper.learn.shared.dto.response.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公开用户信息 DTO
 *
 * 用途：其他用户查看某个用户时显示的公开信息（含关注状态）
 *
 * 使用场景：
 * - 查看其他用户的个人主页
 * - 关注/粉丝列表
 * - 需要显示关注状态的场景
 *
 * 替代关系：
 * - 替代原 V4（V1 + following）
 *
 * 安全性：
 * - ✅ 不包含敏感信息（password, phone, email）
 * - ✅ 包含当前登录用户与该用户的关注关系
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPublicDTO extends UserSummaryDTO {

    /**
     * 当前登录用户是否关注了该用户
     * 说明：表示 viewer 是否关注了该用户
     * 何时填充：动态查询当前登录用户与该用户的关注关系填充
     * 注意：这是个相对字段，不同的 viewer 看到的值不同
     */
    private Boolean isFollowing;
}
