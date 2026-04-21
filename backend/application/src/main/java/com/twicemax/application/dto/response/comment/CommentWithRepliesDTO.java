package com.twicemax.application.dto.response.comment;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 带回复的评论 DTO
 *
 * 用途：评论树结构展示（父评论+子评论列表）
 *
 * 使用场景：
 * - 获取帖子的评论列表（需要展示嵌套回复）
 * - 获取节点的评论列表（需要展示嵌套回复）
 * - 需要显示评论树状结构的场景
 *
 * 与 CommentDetailDTO 的区别：
 * - 新增：children（子评论列表）
 *
 * 数据结构说明：
 * - 通常只支持两层结构（父评论 + 直接回复）
 * - children 中的评论类型为 CommentDetailDTO（不再嵌套）
 * - 如果子评论有更多回复，需要通过"查看更多回复"接口获取
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentWithRepliesDTO extends CommentDetailDTO {

    /**
     * 子评论列表（直接回复）
     * 动态填充：查询 replyToCommentId = 当前评论ID 的评论列表
     *
     * 注意：
     * - 为空列表时表示没有回复，而非 null
     * - 子评论按创建时间或分数排序
     * - 通常限制返回数量（如最多显示 3 条，超过则显示"查看更多"）
     */
    private List<CommentDetailDTO> children;
}
