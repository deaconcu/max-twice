package com.twicemax.shared.domain.event.content.lifecycle;

import com.twicemax.shared.domain.Enums.NewContentState;
import lombok.Data;

/**
 * Roadmap 解封事件。
 * <p>
 * 触发时机：管理员调 restore，将 BANNED 状态的 roadmap 恢复：
 * - 有 current_revision_id → state → PUBLISHED
 * - 无 current_revision_id → state → NEVER_PUBLISHED
 * <p>
 * 用途：通知作者、恢复发布统计、重新索引等。
 */
@Data
public class RoadmapRestoredEvent {

    private Long authorId;

    private Long operatorId;

    private Long roadmapId;

    /** 恢复后的新状态。 */
    private NewContentState newState;

    private Long roleId;

    private String roleName;

    private String reason;

    public static RoadmapRestoredEvent of(Long authorId, Long operatorId, Long roadmapId,
                                          NewContentState newState, Long roleId,
                                          String roleName, String reason) {
        RoadmapRestoredEvent event = new RoadmapRestoredEvent();
        event.authorId = authorId;
        event.operatorId = operatorId;
        event.roadmapId = roadmapId;
        event.newState = newState;
        event.roleId = roleId;
        event.roleName = roleName;
        event.reason = reason;
        return event;
    }
}
