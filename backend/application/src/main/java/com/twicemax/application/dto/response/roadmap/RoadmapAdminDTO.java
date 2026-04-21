package com.twicemax.application.dto.response.roadmap;

import com.twicemax.application.dto.response.role.RoleBriefDTO;
import com.twicemax.application.dto.response.user.UserBriefDTO;
import lombok.Data;

/**
 * 路线图管理 DTO
 *
 * 用途：管理后台使用的路线图信息
 *
 * 使用场景：
 * - 管理后台路线图审核列表
 * - 需要显示拒绝/封禁原因的场景
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class RoadmapAdminDTO {

    private Long id;

    private String content;

    private Long roleId;

    private RoleBriefDTO role;

    private String description;

    private Byte state;

    /**
     * 拒绝/封禁原因
     */
    private String reason;

    private Integer nodeCount;

    private Long creatorId;

    private UserBriefDTO creator;

    private String updatedAt;

    private String createdAt;

    // ==================== 统计字段 ====================

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 收藏数
     */
    private Integer bookmarkCount;

    /**
     * 学习人数
     */
    private Integer learnerCount;

    /**
     * 完成人数
     */
    private Integer completedUserCount;

    /**
     * 被拒次数
     */
    private Integer rejectCount;

    /**
     * 排序分数
     */
    private Double score;
}
