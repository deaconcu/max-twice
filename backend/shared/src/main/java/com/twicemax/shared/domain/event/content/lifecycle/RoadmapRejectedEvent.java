package com.twicemax.shared.domain.event.content.lifecycle;

import lombok.Data;

/**
 * Roadmap revision 审核驳回事件。
 * <p>
 * 触发时机：审核员对一条 SUBMITTED 状态的 revision 调 reject，导致：
 * - revision.status: SUBMITTED → REJECTED（带 rejectReason）
 * - roadmap.pending_revision_id: → NULL
 * - roadmap.draft_content: → revision.payload（回填给作者继续改）
 * - roadmap.state: 不动
 * <p>
 * 用途：通知作者审核被拒 + 原因。
 */
@Data
public class RoadmapRejectedEvent {

    private Long authorId;

    private Long reviewerId;

    private Long roadmapId;

    private Long revisionId;

    private Long roleId;

    private String roleName;

    /** 驳回原因。 */
    private String reason;

    public static RoadmapRejectedEvent of(Long authorId, Long reviewerId, Long roadmapId,
                                          Long revisionId, Long roleId, String roleName, String reason) {
        RoadmapRejectedEvent event = new RoadmapRejectedEvent();
        event.authorId = authorId;
        event.reviewerId = reviewerId;
        event.roadmapId = roadmapId;
        event.revisionId = revisionId;
        event.roleId = roleId;
        event.roleName = roleName;
        event.reason = reason;
        return event;
    }
}
