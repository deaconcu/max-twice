package com.twicemax.content.shared.revision;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容版本快照（content_revision 表）。
 * <p>
 * 一行 = 某个内容（如 roadmap）某次"提交审核"产生的不可变快照。生命周期通过 status 字段流转：
 * SUBMITTED → PUBLISHED / REJECTED / WITHDRAWN。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>跨内容类型共享，content_type + content_id 联合定位。</li>
 *   <li>payload 为内容的 canonical JSON，hash 为其 SHA-256，用于 submit 去重。</li>
 *   <li>历史已发布版仍保留 PUBLISHED；roadmap.current_revision_id 决定当前对外哪一版。</li>
 * </ul>
 */
@Data
public class ContentRevisionDO {

    private Long id;

    /** 内容类型，参考 {@link com.twicemax.shared.domain.Enums.NewContentType}。 */
    private String contentType;

    /** 具体内容主表 id（如 roadmap.id）。 */
    private Long contentId;

    /** 该 content 下的版本序号，从 1 递增。 */
    private Integer revisionNo;

    /** 状态，参考 {@link com.twicemax.shared.domain.Enums.RevisionStatus}。 */
    private String status;

    /** 内容快照（canonical JSON）。 */
    private String payload;

    /** SHA-256 hex of canonical payload。 */
    private String hash;

    /** 驳回原因，仅 status=REJECTED 时填写。 */
    private String rejectReason;

    private Long authorId;

    /** 审核员，PUBLISHED / REJECTED 时填写。 */
    private Long reviewerId;

    private LocalDateTime createdAt;

    /** 审核完成时间，PUBLISHED / REJECTED 时填写。 */
    private LocalDateTime reviewedAt;
}
