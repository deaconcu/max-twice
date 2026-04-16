package com.prosper.learn.application.dto.response.post;

import lombok.Data;

/**
 * 帖子摘要 DTO
 *
 * 用途：基础帖子信息
 *
 * 使用场景：
 * - 作为其他 PostDTO 的基类
 * - 不需要关联信息的场景
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class PostSummaryDTO {

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
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 帖子类型
     * 说明：0-普通帖子，1-内容帖子
     */
    private Integer type;

    /**
     * max twice 点赞数
     */
    private Integer twiceCount;

    /**
     * 喜欢点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 浏览数
     */
    private Integer viewCount;

    /**
     * 状态
     * 说明：0-待审核，1-已发布，2-已拒绝
     */
    private Integer state;

    /**
     * 分数
     */
    private Double score;

    /**
     * 创建时间
     */
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;
}
