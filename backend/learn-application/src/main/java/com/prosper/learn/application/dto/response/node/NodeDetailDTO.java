package com.prosper.learn.application.dto.response.node;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 节点详情 DTO
 *
 * 用途：包含管理信息的节点（不含课程对象）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NodeDetailDTO extends NodeSummaryDTO {

    private Long courseId;

    private Long creatorId;

    private UserBriefDTO creator;

    private Byte state;

    private String createdAt;

    private String updatedAt;
}
