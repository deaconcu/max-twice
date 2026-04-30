package com.twicemax.content.roadmap;

import com.twicemax.shared.domain.Enums;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Roadmap 主表实体。
 * <p>
 * 配合 {@code content_revision} 表实现 revision 模型：state 仅描述对外可见性
 * （NEVER_PUBLISHED / PUBLISHED / BANNED），具体一次提交的生命周期（SUBMITTED /
 * REJECTED / WITHDRAWN）落在 content_revision.status。
 */
@Data
public class RoadmapDO {

    private Long id;

    /** 当前对外展示的内容快照（来自 current_revision.payload 的冗余）；NEVER_PUBLISHED 时为 null。 */
    private String content;

    /** content 的 SHA-256 hex；与 content 同时为空或同时有值。 */
    private String contentHash;

    private Long roleId;

    private Long creatorId;

    private String description;

    /** 主体状态：NEVER_PUBLISHED / PUBLISHED / BANNED，参见 {@link Enums.NewContentState}。 */
    private String state;

    /** 当前对外展示的 revision id（指向 content_revision.id）；NEVER_PUBLISHED 时为 null。 */
    private Long currentRevisionId;

    /** 正在审核中的 revision id；同一时间至多 1 个，无审核中版本时为 null。 */
    private Long pendingRevisionId;

    /** 作者编辑中的草稿；提交审核后置 null。 */
    private String draftContent;

    /** 草稿最后修改时间。 */
    private LocalDateTime draftUpdatedAt;

    /** 排序分数。 */
    private Double score;

    /** 分数计算时间。 */
    private LocalDateTime scoreCalculatedAt;

    /**
     * 当前已发布版本中的叶子节点数量（c+n 类型，不含 group/note）。
     * 仅在 approve 时根据 current_revision.payload 重算并落主表，列表/详情页直接读取。
     */
    private Integer nodeCount;

    /** 软删除时间。 */
    private LocalDateTime deletedAt;

    private LocalDateTime updatedAt;

    private LocalDateTime createdAt;
}
