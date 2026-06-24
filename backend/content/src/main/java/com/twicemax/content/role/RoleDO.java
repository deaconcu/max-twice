package com.twicemax.content.role;

import com.twicemax.shared.domain.Enums;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Role 主表实体。
 * <p>
 * 配合 {@code content_revision} 表实现 revision 模型：state 仅描述对外可见性
 * （NEVER_PUBLISHED / PUBLISHED / BANNED），具体一次提交的生命周期（SUBMITTED /
 * REJECTED / WITHDRAWN）落在 content_revision.status。
 * <p>
 * name / description / icon / skills / mainCategory / subCategory 是当前对外展示的
 * 内容快照，来自 current_revision.payload 的冗余；NEVER_PUBLISHED 时应视为占位/初始值。
 */
@Data
public class RoleDO {

    private Long id;

    private String name;

    private String description;

    private String skills;

    private Integer mainCategory;

    private Integer subCategory;

    /** 图标字段。 */
    private String icon;

    /** 创建者 ID。 */
    private Long creatorId;

    /** 主体状态：NEVER_PUBLISHED / PUBLISHED / BANNED，参见 {@link Enums.NewContentState}。 */
    private String state;

    /** 当前对外展示的 revision id（指向 content_revision.id）；NEVER_PUBLISHED 时为 null。 */
    private Long currentRevisionId;

    /** 正在审核中的 revision id；同一时间至多 1 个，无审核中版本时为 null。 */
    private Long pendingRevisionId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 软删除时间，NULL 表示未删除。 */
    private LocalDateTime deletedAt;
}
