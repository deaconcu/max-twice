package com.prosper.learn.application.dto.response.post;

import com.prosper.learn.application.dto.response.node.NodeBriefDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;

/**
 * 帖子管理 DTO
 *
 * 用途：管理后台使用的帖子信息
 *
 * 使用场景：
 * - 管理后台帖子审核列表
 * - 需要显示节点名称和审核原因的场景
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class PostAdminDTO {

    /**
     * 帖子ID
     */
    private Long id;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 节点ID
     */
    private Long nodeId;

    /**
     * 节点信息
     */
    private NodeBriefDTO node;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 创建者信息
     */
    private UserBriefDTO creator;

    /**
     * 帖子类型
     * 说明：0-普通帖子，1-内容帖子
     */
    private Integer type;

    /**
     * 状态
     * 说明：0-待审核，1-已发布，2-已拒绝，3-已封禁
     */
    private Integer state;

    /**
     * 拒绝/封禁原因
     */
    private String reason;

    /**
     * 创建时间
     */
    private String createdAt;
}
