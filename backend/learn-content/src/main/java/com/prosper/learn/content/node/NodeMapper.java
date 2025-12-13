package com.prosper.learn.content.node;

import org.apache.ibatis.annotations.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface NodeMapper {

    @Select("SELECT n.id FROM node n WHERE n.id > #{afterId} AND NOT EXISTS (SELECT 1 FROM post p WHERE p.node_id = n.id AND p.creator_id = #{userId} AND p.state != 2) ORDER BY n.id ASC LIMIT #{limit}")
    List<Long> selectIdsByUserIdAndPost(@Param("afterId") long afterId, @Param("userId") long userId, @Param("limit") int limit);


    @Select("SELECT * FROM node WHERE id = #{id}")
    NodeDO getById(long id);

    @Select({"<script>SELECT * FROM node where id in " +
                 "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
             "</script>"})
    List<NodeDO> getByIds(List<Long> ids);

    @Select({"<script>SELECT * FROM node where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, NodeDO> getMapByIds(Collection<Long> ids);

// --注释掉检查 START (2025/12/10 12:02):
//    @Select("SELECT * FROM node where parent = #{parentId}")
//    List<NodeDO> getByParent(long parentId);
// --注释掉检查 STOP (2025/12/10 12:02)

// --注释掉检查 START (2025/12/10 12:02):
//    @Select("SELECT * FROM node where course_id = #{courseId}")
//    List<NodeDO> listBySubcourse(long courseId);
// --注释掉检查 STOP (2025/12/10 12:02)

    @Select("SELECT * FROM node WHERE course_id = #{courseId} AND name = #{name} LIMIT 1")
    NodeDO getByCourseAndName(@Param("courseId") long courseId, @Param("name") String name);

    @Insert("INSERT INTO node(name, description, course_id, creator_id) " +
            "VALUES (#{name}, #{description}, #{courseId}, #{creatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(NodeDO Node);

    @Update("UPDATE node SET name = #{name}, description = #{description}, " +
            "creator_id = #{creatorId}, course_id = #{courseId} where id = #{id}")
    void update(NodeDO Node);

    @Update("UPDATE node SET state = #{state}, reason = #{reason} where id = #{id}")
    void updateStateAndReason(@Param("id") Long id, @Param("state") Byte state, @Param("reason") String reason);

    // 平台统计相关方法
    @Select("SELECT COUNT(*) FROM node WHERE course_id > 0")
    Long countActiveNodes();

    @Select({"<script>",
            "SELECT * FROM node WHERE id &lt; #{lastId}",
            "<if test='nodeId != null'> AND id = #{nodeId}</if>",
            "<if test='courseId != null'> AND course_id = #{courseId}</if>",
            "<if test='creatorId != null'> AND creator_id = #{creatorId}</if>",
            "<if test='state != null'> AND state = #{state}</if>",
            "ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<NodeDO> getListByFilterWithPagination(
            @Param("nodeId") Long nodeId,
            @Param("courseId") Long courseId,
            @Param("creatorId") Long creatorId,
            @Param("state") Byte state,
            @Param("lastId") Long lastId,
            @Param("limit") int limit);

}
