package com.twicemax.content.course;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseDO {

    private Long id;

    private String name;

    private String description;

    private Long creatorId;

    private Long rootNodeId;

    private Long parentCourseId;

    /**
     * 主体状态：NEVER_PUBLISHED / PUBLISHED / BANNED（{@link com.twicemax.shared.domain.Enums.NewContentState}）。
     * SUBMITTED / REJECTED / WITHDRAWN 是 revision 的生命周期，落在 content_revision 表。
     */
    private String state;

    private Integer mainCategory;

    private Integer subCategory;

    private String icon; // 图标，可以是 MDI 图标名或图片 URL

    private Integer subCourseCount; // 子课程数量

    /**
     * 当前对外展示的 revision id（指向 content_revision.id）。
     * NEVER_PUBLISHED 时为 null。
     */
    private Long currentRevisionId;

    /**
     * 正在审核中的 revision id（同一时间至多 1 个）。
     */
    private Long pendingRevisionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
