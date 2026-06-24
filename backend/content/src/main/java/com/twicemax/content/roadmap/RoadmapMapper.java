package com.twicemax.content.roadmap;

import com.twicemax.shared.domain.Enums;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RoadmapMapper {

    @Select("SELECT * FROM roadmap WHERE id = #{id} AND deleted_at IS NULL")
    RoadmapDO getById(long id);

    @Select({"<script>",
             "SELECT * FROM roadmap WHERE id IN ",
             "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
             " AND deleted_at IS NULL ORDER BY created_at DESC",
             "</script>"})
    List<RoadmapDO> getByIds(List<Long> ids);

    /**
     * 创建者维度分页：
     *   state 不传 → 排除 BANNED；
     *   state 传  → 精确匹配该状态。
     */
    @Select({"<script>",
             "SELECT r.* FROM roadmap r",
             "JOIN role ro ON r.role_id = ro.id",
             "WHERE r.creator_id = #{creatorId} AND r.deleted_at IS NULL",
             "AND ro.state = " + Enums.ContentState.PUBLISHED_VALUE,
             "<if test='state != null'> AND r.state = #{state}</if>",
             "<if test='state == null'> AND r.state != '" + Enums.NewContentState.BANNED_VALUE + "'</if>",
             "<if test='lastId != null and lastId > 0'> AND r.id &lt; #{lastId}</if>",
             "ORDER BY r.id DESC LIMIT #{limit}",
             "</script>"})
    List<RoadmapDO> getListByCreatorWithPaging(long creatorId, Long lastId, int limit, String state);

    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    @Insert("INSERT INTO roadmap(content, content_hash, description, role_id, creator_id, " +
            " state, current_revision_id, pending_revision_id, draft_content, draft_updated_at, " +
            " node_count, score) " +
            "VALUES (#{content}, #{contentHash}, #{description}, #{roleId}, #{creatorId}, " +
            " #{state}, #{currentRevisionId}, #{pendingRevisionId}, #{draftContent}, #{draftUpdatedAt}, " +
            " #{nodeCount}, #{score})")
    int insert(RoadmapDO roadmapDO);

    /**
     * 更新主体（不含状态机/版本引用相关字段）。
     * description / draft_content / draft_updated_at / node_count 会被覆盖；
     * state / current_revision_id / pending_revision_id 通过专门的方法更新。
     */
    @Update("UPDATE roadmap SET " +
            "description = #{description}, " +
            "draft_content = #{draftContent}, " +
            "draft_updated_at = #{draftUpdatedAt}, " +
            "node_count = #{nodeCount} " +
            "WHERE id = #{id}")
    void update(RoadmapDO roadmapDO);

    @Update("UPDATE roadmap SET score = #{score}, score_calculated_at = NOW() WHERE id = #{id}")
    int updateScore(long id, double score);

    /**
     * 切换 pending_revision_id（提交 / 撤回 / 驳回 / 封禁连带重置时用）。
     * draft_content 与 draft_updated_at 一并写入：submit 时清空，withdraw/reject 时回填。
     */
    @Update("UPDATE roadmap SET " +
            "pending_revision_id = #{pendingRevisionId}, " +
            "draft_content = #{draftContent}, " +
            "draft_updated_at = #{draftUpdatedAt} " +
            "WHERE id = #{id}")
    int updatePending(@Param("id") long id,
                      @Param("pendingRevisionId") Long pendingRevisionId,
                      @Param("draftContent") String draftContent,
                      @Param("draftUpdatedAt") java.time.LocalDateTime draftUpdatedAt);

    /**
     * 仅更新草稿内容（save-draft 用）。
     */
    @Update("UPDATE roadmap SET " +
            "draft_content = #{draftContent}, " +
            "draft_updated_at = #{draftUpdatedAt}, " +
            "description = #{description} " +
            "WHERE id = #{id}")
    int updateDraft(@Param("id") long id,
                    @Param("draftContent") String draftContent,
                    @Param("draftUpdatedAt") java.time.LocalDateTime draftUpdatedAt,
                    @Param("description") String description);

    /**
     * 审核通过：state = PUBLISHED，content/content_hash/node_count/current_revision_id 一并更新，pending 清空。
     */
    @Update("UPDATE roadmap SET " +
            "state = '" + Enums.NewContentState.PUBLISHED_VALUE + "', " +
            "content = #{content}, " +
            "content_hash = #{contentHash}, " +
            "node_count = #{nodeCount}, " +
            "current_revision_id = #{currentRevisionId}, " +
            "pending_revision_id = NULL " +
            "WHERE id = #{id}")
    int approve(@Param("id") long id,
                @Param("content") String content,
                @Param("contentHash") String contentHash,
                @Param("nodeCount") Integer nodeCount,
                @Param("currentRevisionId") long currentRevisionId);

    /**
     * 封禁：state = BANNED，pending 清空（pending revision 由调用方在事务内单独标 REJECTED）。
     */
    @Update("UPDATE roadmap SET " +
            "state = '" + Enums.NewContentState.BANNED_VALUE + "', " +
            "pending_revision_id = NULL, " +
            "draft_content = #{draftContent}, " +
            "draft_updated_at = #{draftUpdatedAt} " +
            "WHERE id = #{id}")
    int ban(@Param("id") long id,
            @Param("draftContent") String draftContent,
            @Param("draftUpdatedAt") java.time.LocalDateTime draftUpdatedAt);

    /**
     * 解封：根据是否存在已发布版本恢复到 PUBLISHED 或 NEVER_PUBLISHED。
     */
    @Update("UPDATE roadmap SET state = #{state} WHERE id = #{id}")
    int updateState(@Param("id") long id, @Param("state") String state);

    @Select("SELECT * FROM roadmap WHERE role_id = #{roleId} AND state = '"
            + Enums.NewContentState.PUBLISHED_VALUE + "' AND deleted_at IS NULL " +
            "ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<RoadmapDO> getListByRoleOrderByLatest(
            @Param("roleId") long roleId,
            @Param("limit") int limit);

    @Select("SELECT * FROM roadmap WHERE role_id = #{roleId} AND state = '"
            + Enums.NewContentState.PUBLISHED_VALUE + "' AND deleted_at IS NULL " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<RoadmapDO> getListByRoleOrderByScore(
            @Param("roleId") long roleId,
            @Param("limit") int limit);

    @Select("SELECT * FROM roadmap WHERE role_id = #{roleId} AND state = '"
            + Enums.NewContentState.PUBLISHED_VALUE + "' AND deleted_at IS NULL " +
            "AND (created_at < #{lastCreatedAt} OR (created_at = #{lastCreatedAt} AND id < #{lastId})) " +
            "ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<RoadmapDO> getListByRoleAfterCreatedAt(
            @Param("roleId") long roleId,
            @Param("lastCreatedAt") java.time.LocalDateTime lastCreatedAt,
            @Param("lastId") long lastId,
            @Param("limit") int limit);

    @Select("SELECT * FROM roadmap WHERE role_id = #{roleId} AND state = '"
            + Enums.NewContentState.PUBLISHED_VALUE + "' AND deleted_at IS NULL " +
            "AND (score < #{lastScore} OR (score = #{lastScore} AND id < #{lastId})) " +
            "ORDER BY score DESC, id DESC LIMIT #{limit}")
    List<RoadmapDO> getListByRoleAfterScore(
            @Param("roleId") long roleId,
            @Param("lastScore") Double lastScore,
            @Param("lastId") long lastId,
            @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM roadmap WHERE state = '"
            + Enums.NewContentState.PUBLISHED_VALUE + "' AND deleted_at IS NULL")
    Long countPublicRoadmaps();

    /**
     * Admin 列表查询：
     *   入参 state 为字符串枚举（NEVER_PUBLISHED / PUBLISHED / BANNED）；
     *   不传 state 时默认排除 BANNED。
     */
    @Select("<script>" +
            "SELECT * FROM roadmap WHERE deleted_at IS NULL " +
            "<if test='state != null'>AND state = #{state}</if> " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<RoadmapDO> listByState(String state, Long lastId, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT * FROM roadmap WHERE deleted_at IS NULL " +
            "<if test='roadmapId != null'>AND id = #{roadmapId}</if> " +
            "<if test='roleId != null'>AND role_id = #{roleId}</if> " +
            "<if test='creatorId != null'>AND creator_id = #{creatorId}</if> " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<RoadmapDO> listByFilter(Long roadmapId, Long roleId, Long creatorId, Long lastId, @Param("limit") int limit);

    @Update("UPDATE roadmap SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    int softDelete(long id);
}
