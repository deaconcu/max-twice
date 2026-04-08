package com.prosper.learn.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Robot 路径草稿 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RobotRoadmapDraftDTO {

    /**
     * 草稿ID
     */
    private String draftId;

    /**
     * 职业ID
     */
    private Long roleId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private String createdAt;
}
