package com.twicemax.learning.enrollment;

import com.twicemax.shared.domain.Enums;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 用户学习记录 Mapper 接口
 */
@Mapper
public interface UserLearningMapper {

    /**
     * 插入学习记录
     */
    @Insert("INSERT INTO user_learning(user_id, object_type, object_id, parent_id, is_root_node, progress_percent, " +
            "started_at, completed_at, created_at, updated_at, nodes) " +
            "VALUES (#{userId}, #{objectType}, #{objectId}, #{parentId}, #{isRootNode}, #{progressPercent}, " +
            "#{startedAt}, #{completedAt}, #{createdAt}, #{updatedAt}, #{nodes})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserLearningDO record);

    /**
     * 更新学习记录
     */
    @Update("UPDATE user_learning SET " +
            "parent_id = #{parentId}, " +
            "is_root_node = #{isRootNode}, " +
            "progress_percent = #{progressPercent}, " +
            "started_at = #{startedAt}, " +
            "completed_at = #{completedAt}, " +
            "updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    int update(UserLearningDO record);

    /**
     * 更新课程的节点列表
     * 用于用户切换ToC时更新节点缓存
     */
    @Update("UPDATE user_learning SET " +
            "nodes = #{nodes}, " +
            "updated_at = #{updatedAt} " +
            "WHERE user_id = #{userId} AND object_type = #{objectType} AND object_id = #{objectId}")
    int updateNodes(@Param("userId") long userId,
                    @Param("objectType") byte objectType,
                    @Param("objectId") long objectId,
                    @Param("nodes") String nodes,
                    @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 根据用户ID和对象查询学习记录
     */
    @Select("SELECT * FROM user_learning " +
            "WHERE user_id = #{userId} AND object_type = #{objectType} AND object_id = #{objectId}")
    UserLearningDO getByUserAndObject(@Param("userId") long userId,
                                      @Param("objectType") byte objectType,
                                      @Param("objectId") long objectId);

    /**
     * 根据用户ID和对象类型查询学习记录（支持滚动分页，可选状态过滤）
     * 按 id 降序排列
     * state 为 null 时查询所有状态
     * state 通过 progress_percent 判断：1=进行中(progress<10000), 2=已完成(progress>=10000)
     * lastId 为 null 时返回第一页
     */
    @Select("<script>" +
            "SELECT * FROM user_learning " +
            "WHERE user_id = #{userId} AND object_type = #{objectType} " +
            "<if test='state != null'>" +
            "AND (" +
            "  <choose>" +
            "    <when test='state == 1'>progress_percent &lt; 10000</when>" +
            "    <when test='state == 2'>progress_percent &gt;= 10000</when>" +
            "  </choose>" +
            ") " +
            "</if>" +
            "<if test='lastId != null'>" +
            "AND id &lt; #{lastId} " +
            "</if>" +
            "ORDER BY id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<UserLearningDO> getByUserAndType(@Param("userId") long userId,
                                           @Param("objectType") byte objectType,
                                           @Param("state") Byte state,
                                           @Param("lastId") Long lastId,
                                           @Param("limit") int limit);

    /**
     * 根据用户、对象类型和父对象ID查询学习记录（支持滚动分页和状态过滤）
     * 用于查询：某个 role 下正在学习的 roadmap
     * state 通过 progress_percent 判断：1=进行中(progress<10000), 2=已完成(progress>=10000)
     * lastId 为 null 时返回第一页
     */
    @Select("<script>" +
            "SELECT * FROM user_learning " +
            "WHERE user_id = #{userId} AND object_type = #{objectType} AND parent_id = #{parentId} " +
            "<if test='state != null'>" +
            "AND (" +
            "  <choose>" +
            "    <when test='state == 1'>progress_percent &lt; 10000</when>" +
            "    <when test='state == 2'>progress_percent &gt;= 10000</when>" +
            "  </choose>" +
            ") " +
            "</if>" +
            "<if test='lastId != null'>" +
            "AND id &lt; #{lastId} " +
            "</if>" +
            "ORDER BY id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<UserLearningDO> getByUserAndTypeAndParent(@Param("userId") long userId,
                                                     @Param("objectType") byte objectType,
                                                     @Param("parentId") long parentId,
                                                     @Param("state") Byte state,
                                                     @Param("lastId") Long lastId,
                                                     @Param("limit") int limit);

    /**
     * 根据用户ID查询所有学习记录（支持滚动分页和状态过滤）
     * 按 id 降序排列
     * state 通过 progress_percent 判断：1=进行中(progress<10000), 2=已完成(progress>=10000)
     * lastId 为 null 时返回第一页
     */
    @Select("<script>" +
            "SELECT * FROM user_learning " +
            "WHERE user_id = #{userId} " +
            "<if test='state != null'>" +
            "AND (" +
            "  <choose>" +
            "    <when test='state == 1'>progress_percent &lt; 10000</when>" +
            "    <when test='state == 2'>progress_percent &gt;= 10000</when>" +
            "  </choose>" +
            ") " +
            "</if>" +
            "<if test='lastId != null'>" +
            "AND id &lt; #{lastId} " +
            "</if>" +
            "ORDER BY id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<UserLearningDO> getByUserId(@Param("userId") long userId,
                                      @Param("state") Byte state,
                                      @Param("lastId") Long lastId,
                                      @Param("limit") int limit);

    /**
     * 批量查询学习记录
     */
    @Select("<script>" +
            "SELECT * FROM user_learning " +
            "WHERE user_id = #{userId} AND object_type = #{objectType} " +
            "AND object_id IN " +
            "<foreach item='id' collection='objectIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<UserLearningDO> batchGetByUserAndObjects(@Param("userId") long userId,
                                                   @Param("objectType") byte objectType,
                                                   @Param("objectIds") Collection<Long> objectIds);

    /**
     * 删除学习记录
     */
    @Delete("DELETE FROM user_learning WHERE id = #{id}")
    int delete(long id);

    /**
     * 根据用户和对象删除学习记录
     */
    @Delete("DELETE FROM user_learning " +
            "WHERE user_id = #{userId} AND object_type = #{objectType} AND object_id = #{objectId}")
    int deleteByUserAndObject(@Param("userId") long userId,
                              @Param("objectType") byte objectType,
                              @Param("objectId") long objectId);

    /**
     * 查询包含指定节点的课程学习记录
     * 用于节点完成时，反向查找需要更新进度的课程
     * 固定查询 objectType=node
     */
    @Select("SELECT * FROM user_learning " +
            "WHERE user_id = #{userId} " +
            "AND object_type = " + Enums.ContentType.NODE_VALUE + " " +
            "AND nodes LIKE CONCAT('%', #{nodeId}, '%')")
    List<UserLearningDO> findByNodeContained(@Param("userId") long userId,
                                               @Param("nodeId") long nodeId);

    /**
     * 检查学习记录是否存在
     */
    @Select("SELECT COUNT(1) FROM user_learning " +
            "WHERE user_id = #{userId} AND object_type = #{objectType} AND object_id = #{objectId}")
    int exists(@Param("userId") long userId,
               @Param("objectType") byte objectType,
               @Param("objectId") long objectId);

    /**
     * 查询用户学习的所有课程（通过 is_root_node 字段过滤）
     * 返回 objectType=node 且 is_root_node=1 的学习记录
     * 固定查询 objectType=node
     *
     * @param userId 用户ID
     * @param state 状态过滤（null=全部, 1=进行中, 2=已完成）
     * @param lastId 分页游标（null=第一页）
     * @param limit 每页数量
     */
    @Select("<script>" +
            "SELECT * FROM user_learning " +
            "WHERE user_id = #{userId} " +
            "AND object_type = " + Enums.ContentType.NODE_VALUE + " " +
            "AND is_root_node = 1 " +
            "<if test='state != null'>" +
            "AND (" +
            "  <choose>" +
            "    <when test='state == 1'>progress_percent &lt; 10000</when>" +
            "    <when test='state == 2'>progress_percent &gt;= 10000</when>" +
            "  </choose>" +
            ") " +
            "</if>" +
            "<if test='lastId != null'>" +
            "AND id &lt; #{lastId} " +
            "</if>" +
            "ORDER BY id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<UserLearningDO> getCoursesByUser(@Param("userId") long userId,
                                           @Param("state") Byte state,
                                           @Param("lastId") Long lastId,
                                           @Param("limit") int limit);
}
