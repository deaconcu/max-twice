package com.prosper.learn.application.dto.response.node;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

/**
 * 节点管理 DTO
 *
 * 用途：管理后台使用的节点信息
 *
 * 使用场景：
 * - 管理后台节点审核列表
 * - 需要显示拒绝/封禁原因的场景
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class NodeAdminDTO {

    private Long id;

    private String name;

    private String description;

    private Long courseId;

    private Long creatorId;

    private UserBriefDTO creator;

    private Byte state;

    /**
     * 拒绝/封禁原因
     */
    private String reason;

    private String createdAt;

    private String updatedAt;

    // 统计字段
    private Integer postCount;

    private Integer articleCount;

    private Integer indexCount;

    private Integer commentCount;

    private Integer nodeReferenceCount;

    private Integer cardDeckCount;
}
