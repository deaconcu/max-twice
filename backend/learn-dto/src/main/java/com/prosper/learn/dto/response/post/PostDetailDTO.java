package com.prosper.learn.dto.response.post;

import com.prosper.learn.dto.response.NodeDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子详情 DTO
 *
 * 用途：包含完整节点信息的帖子
 *
 * 使用场景：
 * - 帖子详情页
 * - 需要显示完整节点和课程信息的场景
 *
 * 替代关系：
 * - 替代原 V2（部分，不含 voteType）
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostDetailDTO extends PostWithCreatorDTO {

    /**
     * 节点信息
     * 说明：帖子所属的节点，包含课程信息
     * 何时填充：需要显示节点和课程上下文时
     */
    private NodeDTO node;
}
