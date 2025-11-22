package com.prosper.learn.dto.response.user;

import com.prosper.learn.dto.response.SubscriptionDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带订阅信息的用户 DTO
 *
 * 用途：用户个人中心，显示用户订阅的课程和路线图
 *
 * 使用场景：
 * - 用户个人中心页面
 * - 需要显示用户订阅信息的场景
 *
 * 替代关系：
 * - 替代原 V3（V2 + subscriptions）
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserWithSubscriptionsDTO extends UserBriefDTO {

    /**
     * 用户状态
     * 说明：NORMAL(0-正常), BANNED(1-已封禁)
     */
    private Byte state;

    /**
     * 用户角色
     * 说明：USER(0-普通用户), MODERATOR(1-审核员), ADMIN(2-管理员), SUPER_ADMIN(3-超级管理员)
     */
    private Integer role;

    /**
     * 订阅信息
     * 说明：用户订阅的课程和路线图列表
     * 何时填充：从用户配置表(user_profile)动态查询并解析
     * 数据格式：JSON 数组，包含 courseId 和 roadmapId
     */
    private SubscriptionDTO[] subscriptions;
}
