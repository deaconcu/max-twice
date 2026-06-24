package com.twicemax.content.course;

import com.twicemax.shared.domain.Enums;
import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * Course Mapper（revision 模型）。
 * <p>
 * state 列存储 {@link com.twicemax.shared.domain.Enums.NewContentState} 字符串值
 * （NEVER_PUBLISHED / PUBLISHED / BANNED）。SUBMITTED / REJECTED / WITHDRAWN 只存在于
 * content_revision 表，不出现在主表。reason 列已删除，驳回原因通过 content_revision 落库。
 */
@Mapper
public interface CourseMapper {

    @Select("SELECT * FROM course WHERE id = #{id}")
    CourseDO getById(long id);

    @Select({"<script>SELECT * FROM course where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<CourseDO> getByIds(List<Long> ids);

    @Select("<script>" +
            "SELECT * FROM course " +
            "WHERE name LIKE CONCAT('%', #{name}, '%') " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<CourseDO> searchByName(@Param("name") String name, @Param("lastId") Long lastId, @Param("limit") int limit);

    /**
     * 用户端搜索已发布的课程（简单搜索，不分页）
     */
    @Select("SELECT * FROM course WHERE name LIKE CONCAT('%', #{name}, '%') " +
            "AND state = '" + Enums.NewContentState.PUBLISHED_VALUE + "' LIMIT #{limit}")
    List<CourseDO> searchPublishedByName(@Param("name") String name, @Param("limit") int limit);

    /**
     * 按父课程 + 主体状态查询。state 为 NewContentState 字符串值。
     */
    @Select("SELECT * FROM course WHERE state = #{state} AND parent_course_id = #{parentCourseId} " +
            "ORDER BY created_at DESC")
    List<CourseDO> listByParentAndState(@Param("state") String state,
                                        @Param("parentCourseId") long parentCourseId);

    @Select("SELECT * FROM course where parent_course_id = #{parentCourseId} ORDER BY created_at DESC")
    List<CourseDO> listByParent(long parentCourseId);

    /**
     * 按主体状态分页（仅主课程，parent_course_id=0）。state 为 NewContentState 字符串值。
     */
    @Select({"<script>",
            "SELECT * FROM course WHERE parent_course_id = 0",
            "<if test='state != null'> AND state = #{state}</if>",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            "ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<CourseDO> listByState(@Param("state") String state,
                               @Param("lastId") Long lastId,
                               @Param("limit") int limit);

    // 根据主分类获取已发布的主课程列表（支持分页）
    @Select("<script>" +
            "SELECT * FROM course WHERE main_category = #{mainCategory} " +
            "AND state = '" + Enums.NewContentState.PUBLISHED_VALUE + "' AND parent_course_id = 0 " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT 21" +
            "</script>")
    List<CourseDO> listRootByMainCategory(int mainCategory, Long lastId);

    // 根据主分类和子分类获取已发布的主课程列表（支持分页）
    @Select("<script>" +
            "SELECT * FROM course WHERE main_category = #{mainCategory} AND sub_category = #{subCategory} " +
            "AND state = '" + Enums.NewContentState.PUBLISHED_VALUE + "' AND parent_course_id = 0 " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT 21" +
            "</script>")
    List<CourseDO> listRootByCategory(int mainCategory, int subCategory, Long lastId);

    /**
     * 按创建者分页查询：state 可为 null（默认排除 BANNED），或指定 NewContentState 字符串值。
     */
    @Select({"<script>",
            "SELECT * FROM course WHERE creator_id = #{creatorId}",
            "<if test='state != null'> AND state = #{state}</if>",
            "<if test='state == null'> AND state != '" + Enums.NewContentState.BANNED_VALUE + "'</if>",
            "<if test='lastId != null and lastId > 0'> AND id &lt; #{lastId}</if>",
            "ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<CourseDO> listByCreator(@Param("creatorId") long creatorId,
                                 @Param("lastId") Long lastId,
                                 @Param("limit") int limit,
                                 @Param("state") String state);

    // 根据 lastId 获取所有课程列表（不过滤状态，用于数据迁移/重算）
    @Select("<script>" +
            "SELECT * FROM course " +
            "<if test='lastId != null'>WHERE id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT 100" +
            "</script>")
    List<CourseDO> listByLastId(Long lastId);

    /**
     * 创建 course 主体（无 reason 列）。pending_revision_id 由调用方在写入 SUBMITTED revision
     * 后单独设置。state 由调用方传入（通常为 NEVER_PUBLISHED；createAndApprove 流程后续会切到 PUBLISHED）。
     */
    @Insert("INSERT INTO course(name, description, icon, creator_id, parent_course_id, " +
            "root_node_id, state, main_category, sub_category) " +
            "VALUES (#{name}, #{description}, #{icon}, #{creatorId}, #{parentCourseId}, " +
            "#{rootNodeId}, #{state}, #{mainCategory}, #{subCategory})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(CourseDO course);

    /**
     * 全量更新主表镜像字段。state / current_revision_id / pending_revision_id 不在此处修改，
     * 走 updatePending / approve / ban / updateState 等专用方法。
     */
    @Update("UPDATE course SET name = #{name}, description = #{description}, " +
            "creator_id = #{creatorId}, root_node_id = #{rootNodeId}, " +
            "parent_course_id = #{parentCourseId}, " +
            "main_category = #{mainCategory}, sub_category = #{subCategory}, icon = #{icon} " +
            "WHERE id = #{id}")
    void update(CourseDO course);

    /**
     * 仅切换 pending_revision_id（提交 / 撤回 / 驳回 时用）。
     */
    @Update("UPDATE course SET pending_revision_id = #{pendingRevisionId} WHERE id = #{id}")
    int updatePending(@Param("id") long id, @Param("pendingRevisionId") Long pendingRevisionId);

    /**
     * 审核通过：state=PUBLISHED，刷新 payload 镜像字段
     * （name/description/icon/mainCategory/subCategory/parentCourseId），设置 current_revision_id，
     * 清空 pending_revision_id。rootNodeId 不在此刷新（创建时定）。
     */
    @Update("UPDATE course SET " +
            "state = '" + Enums.NewContentState.PUBLISHED_VALUE + "', " +
            "name = #{name}, description = #{description}, icon = #{icon}, " +
            "main_category = #{mainCategory}, sub_category = #{subCategory}, " +
            "parent_course_id = #{parentCourseId}, " +
            "current_revision_id = #{currentRevisionId}, pending_revision_id = NULL " +
            "WHERE id = #{id}")
    int approve(@Param("id") long id,
                @Param("name") String name,
                @Param("description") String description,
                @Param("icon") String icon,
                @Param("mainCategory") int mainCategory,
                @Param("subCategory") int subCategory,
                @Param("parentCourseId") long parentCourseId,
                @Param("currentRevisionId") long currentRevisionId);

    /**
     * 封禁：state=BANNED，pending_revision_id 清空。
     */
    @Update("UPDATE course SET state = '" + Enums.NewContentState.BANNED_VALUE + "', " +
            "pending_revision_id = NULL WHERE id = #{id}")
    int ban(@Param("id") long id);

    /**
     * 简单状态切换（解封时恢复到 PUBLISHED 或 NEVER_PUBLISHED）。
     */
    @Update("UPDATE course SET state = #{state} WHERE id = #{id}")
    int updateState(@Param("id") long id, @Param("state") String state);

    @Delete("DELETE FROM course WHERE id = #{id}")
    int delete(long id);

    // 平台统计相关方法
    @Select("SELECT COUNT(*) FROM course WHERE state = '" + Enums.NewContentState.PUBLISHED_VALUE + "'")
    Long countActiveCourses();

    /**
     * 根据根节点ID查询课程
     */
    @Select("SELECT * FROM course WHERE root_node_id = #{rootNodeId} LIMIT 1")
    CourseDO getByRootNodeId(@Param("rootNodeId") long rootNodeId);

    @Select({"<script>SELECT * FROM course WHERE root_node_id IN " +
            "<foreach item='id' collection='rootNodeIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>"})
    List<CourseDO> getByRootNodeIds(@Param("rootNodeIds") List<Long> rootNodeIds);

    /**
     * 增加子课程数量
     */
    @Update("UPDATE course SET sub_course_count = COALESCE(sub_course_count, 0) + 1 WHERE id = #{id}")
    int incrementSubCourseCount(long id);

    /**
     * 减少子课程数量
     */
    @Update("UPDATE course SET sub_course_count = GREATEST(COALESCE(sub_course_count, 0) - 1, 0) WHERE id = #{id}")
    int decrementSubCourseCount(long id);

    /**
     * 更新子课程数量
     */
    @Update("UPDATE course SET sub_course_count = #{count} WHERE id = #{id}")
    int updateSubCourseCount(@Param("id") long id, @Param("count") int count);

    /**
     * 统计某个父课程的已发布子课程数量
     */
    @Select("SELECT COUNT(*) FROM course WHERE parent_course_id = #{parentCourseId} " +
            "AND state = '" + Enums.NewContentState.PUBLISHED_VALUE + "'")
    int countPublishedSubCourses(long parentCourseId);
}
