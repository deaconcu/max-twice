package com.prosper.learn.application.dto.response.post;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子完整信息 DTO
 *
 * 用途：包含节点、创建者、投票状态的完整帖子信息
 *
 * 使用场景：
 * - 帖子列表（完整信息）
 * - 需要所有关联信息的场景
 *
 * 替代关系：
 * - 替代原 V2（完整版本，含 voteType）
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostFullDTO extends PostDetailDTO {

    /**
     * 投票类型
     * 说明：0-未投票，1-点赞，-1-踩
     * 何时填充：需要显示用户投票状态时
     */
    private Integer voteType;
}
