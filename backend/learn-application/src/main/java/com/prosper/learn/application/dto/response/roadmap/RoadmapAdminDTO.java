package com.prosper.learn.application.dto.response.roadmap;

import com.prosper.learn.application.dto.response.ProfessionBriefDTO;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
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

    private Long professionId;

    private ProfessionBriefDTO profession;

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
}
