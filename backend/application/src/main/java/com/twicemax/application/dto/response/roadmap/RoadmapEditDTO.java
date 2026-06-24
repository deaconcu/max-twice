package com.twicemax.application.dto.response.roadmap;

import lombok.Data;

/**
 * 作者编辑页响应 DTO。
 * <p>
 * 鉴权：creatorId == currentUserId 或 hasRole(ADMIN)。
 * <p>
 * 编辑器初始内容来源（按优先级）：
 * <ol>
 *     <li>有 draft → draft（编辑中草稿，已回填 c/n label）</li>
 *     <li>无 draft 但 PUBLISHED → current revision payload（已回填 c/n label）</li>
 *     <li>无 draft 且无 current → 空（NEVER_PUBLISHED 且未保存过）</li>
 * </ol>
 * pending（如有）只读展示，给"撤回"按钮用。
 * lastReject（如有）展示最近一次驳回原因。
 */
@Data
public class RoadmapEditDTO {

    private Long id;

    private Long roleId;

    private String description;

    /** 主体状态：NEVER_PUBLISHED / PUBLISHED / BANNED。 */
    private String state;

    /** 当前可编辑内容（已回填 c/n label）。BANNED 时为 null。 */
    private String content;

    /** content 来源：DRAFT / CURRENT / EMPTY。 */
    private String contentSource;

    /** 草稿/current 最近修改时间（ISO 字符串）。 */
    private String contentUpdatedAt;

    /** 审核中版本（无则为 null）。 */
    private PendingInfo pending;

    /** 最近一次被驳回的版本信息（无则为 null）。 */
    private LastRejectInfo lastReject;

    @Data
    public static class PendingInfo {
        private Long revisionId;
        private Integer revisionNo;
        private String submittedAt;
    }

    @Data
    public static class LastRejectInfo {
        private Long revisionId;
        private Integer revisionNo;
        private String reason;
        private String reviewedAt;
    }
}
