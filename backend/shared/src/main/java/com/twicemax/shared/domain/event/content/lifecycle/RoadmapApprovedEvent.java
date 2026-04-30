package com.twicemax.shared.domain.event.content.lifecycle;

import lombok.Data;

/**
 * Roadmap revision 审核通过事件。
 * <p>
 * 触发时机：审核员对一条 SUBMITTED 状态的 revision 调 approve，导致：
 * - revision.status: SUBMITTED → PUBLISHED
 * - roadmap.state: → PUBLISHED
 * - roadmap.current_revision_id: → 该 revision.id
 * <p>
 * 用途：发送审核通过通知、初始化/刷新统计、Meilisearch 索引等。
 */
@Data
public class RoadmapApprovedEvent {

    /** 路线图作者 id（被通知对象）。 */
    private Long authorId;

    /** 审核员 id。 */
    private Long reviewerId;

    /** 路线图 id。 */
    private Long roadmapId;

    /** 本次通过的 revision id。 */
    private Long revisionId;

    /** 路线图所属角色 id。 */
    private Long roleId;

    /** 角色名称（消息文案用）。 */
    private String roleName;

    public static RoadmapApprovedEvent of(Long authorId, Long reviewerId, Long roadmapId,
                                          Long revisionId, Long roleId, String roleName) {
        RoadmapApprovedEvent event = new RoadmapApprovedEvent();
        event.authorId = authorId;
        event.reviewerId = reviewerId;
        event.roadmapId = roadmapId;
        event.revisionId = revisionId;
        event.roleId = roleId;
        event.roleName = roleName;
        return event;
    }
}
