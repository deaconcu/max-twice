package com.prosper.learn.application.dto.response.post;

import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子（含创建者）DTO
 *
 * 用途：包含创建者信息的帖子
 *
 * 使用场景：
 * - 帖子列表（基础展示）
 * - 需要显示作者的场景
 *
 * 替代关系：
 * - 替代原 V1
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostWithCreatorDTO extends PostSummaryDTO {

    /**
     * 创建者信息
     * 说明：帖子作者的简要信息
     */
    private UserBriefDTO creator;
}
