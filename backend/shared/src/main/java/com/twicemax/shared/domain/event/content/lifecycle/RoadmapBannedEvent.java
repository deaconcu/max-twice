package com.twicemax.shared.domain.event.content.lifecycle;

import com.twicemax.shared.domain.Enums.NewContentState;
import lombok.Data;

/**
 * Roadmap 主体被封禁事件。
 * <p>
 * 触发时机：管理员调 ban，导致：
 * - roadmap.state: → BANNED
 * - 如果存在 pending revision：连带 status → REJECTED
 * - draft_content: 回填 pending payload（如有）
 * <p>
 * 用途：通知作者、回滚发布相关统计、从 Meilisearch 索引剔除等。
 */
@Data
public class RoadmapBannedEvent {

    private Long authorId;

    private Long operatorId;

    private Long roadmapId;

    /** 被封禁前的状态（NEVER_PUBLISHED / PUBLISHED）。统计回滚需用。 */
    private NewContentState previousState;

    private Long roleId;

    private String roleName;

    private String reason;

    public static RoadmapBannedEvent of(Long authorId, Long operatorId, Long roadmapId,
                                        NewContentState previousState, Long roleId,
                                        String roleName, String reason) {
        RoadmapBannedEvent event = new RoadmapBannedEvent();
        event.authorId = authorId;
        event.operatorId = operatorId;
        event.roadmapId = roadmapId;
        event.previousState = previousState;
        event.roleId = roleId;
        event.roleName = roleName;
        event.reason = reason;
        return event;
    }
}
