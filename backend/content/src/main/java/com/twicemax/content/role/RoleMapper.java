package com.twicemax.content.role;

import com.twicemax.shared.domain.Enums;
import org.apache.ibatis.annotations.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface RoleMapper {

    @Select("SELECT * FROM role WHERE id = #{id} AND deleted_at IS NULL")
    RoleDO getById(long id);

    /**
     * 按主体状态分页（state 为 NewContentState 字符串值）。
     */
    @Select({"<script>",
            "SELECT * FROM role WHERE deleted_at IS NULL",
            "<if test='state != null'> AND state = #{state}</if>",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            "ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<RoleDO> listByState(@Param("state") String state, @Param("lastId") Long lastId, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT * FROM role WHERE main_category = #{mainCategory} AND state = '" + Enums.NewContentState.PUBLISHED_VALUE + "' AND deleted_at IS NULL " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<RoleDO> listByMainCategoryAndLastId(int mainCategory, Long lastId, int limit);

    /**
     * 按创建者分页查询：state 可为 null（默认排除 BANNED），或指定 NewContentState 字符串值。
     */
    @Select({"<script>",
            "SELECT * FROM role WHERE creator_id = #{creatorId} AND deleted_at IS NULL",
            "<if test='state != null'> AND state = #{state}</if>",
            "<if test='state == null'> AND state != '" + Enums.NewContentState.BANNED_VALUE + "'</if>",
            "<if test='lastId != null and lastId > 0'> AND id &lt; #{lastId}</if>",
            "ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<RoleDO> listByCreator(@Param("creatorId") long creatorId,
                               @Param("lastId") Long lastId,
                               @Param("limit") int limit,
                               @Param("state") String state);

    @Select("<script>" +
            "SELECT * FROM role WHERE sub_category = #{subCategory} AND state = '" + Enums.NewContentState.PUBLISHED_VALUE + "' AND deleted_at IS NULL " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<RoleDO> listBySubCategoryAndLastId(int subCategory, Long lastId, int limit);

    @Select("<script>" +
            "SELECT * FROM role WHERE main_category = #{mainCategory} AND sub_category = #{subCategory} AND state = '" + Enums.NewContentState.PUBLISHED_VALUE + "' AND deleted_at IS NULL " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<RoleDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit);

    @Select("SELECT * FROM role WHERE state = '" + Enums.NewContentState.PUBLISHED_VALUE + "' AND deleted_at IS NULL AND name LIKE CONCAT('%', #{keyword}, '%') ORDER BY id DESC LIMIT 20")
    List<RoleDO> searchByKeyword(String keyword);

    /**
     * 管理后台按名称搜索角色（搜索所有状态，支持分页）
     */
    @Select("<script>" +
            "SELECT * FROM role " +
            "WHERE deleted_at IS NULL AND name LIKE CONCAT('%', #{name}, '%') " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<RoleDO> searchByName(@Param("name") String name, @Param("lastId") Long lastId, @Param("limit") int limit);

    @Select({"<script>SELECT * FROM role WHERE deleted_at IS NULL AND id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<RoleDO> getByIds(Collection<Long> ids);

    @Select({"<script>SELECT * FROM role WHERE deleted_at IS NULL AND id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Integer, RoleDO> getMapByIds(Collection<Long> ids);

    /**
     * 创建 role 主体（无 reason 列）。pending_revision_id 由调用方在写入 SUBMITTED revision 后单独设置。
     */
    @Insert("INSERT INTO role " +
            "(name, description, icon, skills, main_category, sub_category, state, creator_id) " +
            "VALUES (#{name}, #{description}, #{icon}, #{skills}, #{mainCategory}, #{subCategory}, " +
            "#{state}, #{creatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(RoleDO roleDO);

    /**
     * 全量更新主表镜像字段（管理员/作者通过新 revision 触发，主表跟着同步）。
     */
    @Update("UPDATE role " +
            "SET name = #{name}, description = #{description}, icon = #{icon}, skills = #{skills}, " +
            "main_category = #{mainCategory}, sub_category = #{subCategory}, " +
            "state = #{state} " +
            "WHERE id = #{id}")
    void update(RoleDO roleDO);

    /**
     * 仅切换 pending_revision_id（提交 / 撤回 / 驳回 时用）。
     */
    @Update("UPDATE role SET pending_revision_id = #{pendingRevisionId} WHERE id = #{id} AND deleted_at IS NULL")
    int updatePending(@Param("id") long id, @Param("pendingRevisionId") Long pendingRevisionId);

    /**
     * 审核通过：state=PUBLISHED，刷新 payload 镜像字段（name/description/icon/skills/mainCategory/subCategory），
     * 设置 current_revision_id，清空 pending_revision_id。
     */
    @Update("UPDATE role SET " +
            "state = '" + Enums.NewContentState.PUBLISHED_VALUE + "', " +
            "name = #{name}, description = #{description}, icon = #{icon}, skills = #{skills}, " +
            "main_category = #{mainCategory}, sub_category = #{subCategory}, " +
            "current_revision_id = #{currentRevisionId}, pending_revision_id = NULL " +
            "WHERE id = #{id} AND deleted_at IS NULL")
    int approve(@Param("id") long id,
                @Param("name") String name,
                @Param("description") String description,
                @Param("icon") String icon,
                @Param("skills") String skills,
                @Param("mainCategory") int mainCategory,
                @Param("subCategory") int subCategory,
                @Param("currentRevisionId") long currentRevisionId);

    /**
     * 封禁：state=BANNED，pending_revision_id 清空。
     */
    @Update("UPDATE role SET state = '" + Enums.NewContentState.BANNED_VALUE + "', pending_revision_id = NULL " +
            "WHERE id = #{id} AND deleted_at IS NULL")
    int ban(@Param("id") long id);

    /**
     * 简单状态切换（解封时恢复到 PUBLISHED 或 NEVER_PUBLISHED）。
     */
    @Update("UPDATE role SET state = #{state} WHERE id = #{id} AND deleted_at IS NULL")
    int updateState(@Param("id") long id, @Param("state") String state);

    @Update("UPDATE role SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    void delete(long id);

    // 平台统计相关方法
    @Select("SELECT COUNT(*) FROM role WHERE state = '" + Enums.NewContentState.PUBLISHED_VALUE + "' AND deleted_at IS NULL")
    Long countActiveRoles();
}
