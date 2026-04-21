package com.twicemax.application.dto.response.post;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子（含投票状态）DTO
 *
 * 用途：包含创建者和投票类型的帖子
 *
 * 使用场景：
 * - 帖子列表（含用户投票状态）
 * - 不需要完整节点信息的轻量级场景
 *
 * 替代关系：
 * - 替代原 V3
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostWithVoteDTO extends PostWithCreatorDTO {

    /**
     * 投票类型
     * 说明：0-未投票，1-点赞，-1-踩
     * 何时填充：需要显示用户投票状态时
     */
    private Integer voteType;

    /**
     * 是否已收藏
     * 何时填充：用户已登录且需要显示收藏状态时
     */
    private Boolean bookmarked;
}
