package com.twicemax.shared.domain.event.content.lifecycle;

import lombok.Data;

/**
 * 作者主动撤回 pending 的 roadmap revision 事件。
 * <p>
 * 触发时机：作者对自己 SUBMITTED 状态的 revision 调 withdraw，导致：
 * - revision.status: SUBMITTED → WITHDRAWN
 * - roadmap.pending_revision_id: → NULL
 * - roadmap.draft_content: → revision.payload
 * - roadmap.state: 不动
 * <p>
 * 用途：本身不一定要发通知（作者自己操作的），但留事件供统计/审计使用。
 */
@Data
public class RoadmapWithdrawnEvent {

    private Long authorId;

    private Long roadmapId;

    private Long revisionId;

    private Long roleId;

    private String roleName;

    public static RoadmapWithdrawnEvent of(Long authorId, Long roadmapId, Long revisionId,
                                           Long roleId, String roleName) {
        RoadmapWithdrawnEvent event = new RoadmapWithdrawnEvent();
        event.authorId = authorId;
        event.roadmapId = roadmapId;
        event.revisionId = revisionId;
        event.roleId = roleId;
        event.roleName = roleName;
        return event;
    }
}
