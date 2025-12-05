package com.prosper.learn.application.dto.response.comment;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论管理员 DTO
 *
 * 用途：管理员审核评论、查看审核记录
 *
 * 使用场景：
 * - 管理员查看待审核评论列表
 * - 管理员查看已拒绝/已封禁评论及其原因
 * - 审核历史记录查看
 *
 * 与 CommentSummaryDTO 的区别：
 * - 新增：reason（拒绝/封禁原因）
 *
 * 权限说明：
 * - 该 DTO 仅供管理员使用，不应暴露给普通用户
 * - reason 字段包含敏感的审核信息
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentAdminDTO extends CommentSummaryDTO {

    /**
     * 拒绝/封禁原因
     * 动态填充：从 CommentDO.reason 字段获取
     *
     * 使用说明：
     * - 当 state = 2（已拒绝）时，存储拒绝原因
     * - 当 state = 3（已封禁）时，存储封禁原因
     * - 当 state = 0 或 1 时，为 null
     *
     * 示例值：
     * - "包含不当言论"
     * - "涉嫌广告营销"
     * - "违反社区规范第X条"
     */
    private String reason;
}
