package com.twicemax.learning.progress;

import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

/**
 * 用户节点完成记录 Mapper 接口
 */
@Mapper
public interface UserNodeCompletionMapper {

    /**
     * 批量查询用户完成的节点（用于检查哪些节点已完成）
     */
    @Select("<script>" +
            "SELECT node_id FROM user_node_completion " +
            "WHERE user_id = #{userId} AND node_id IN " +
            "<foreach item='id' collection='nodeIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Long> getCompletedNodeIds(@Param("userId") long userId, @Param("nodeIds") Collection<Long> nodeIds);

    /**
     * 检查节点是否完成
     */
    @Select("SELECT COUNT(1) FROM user_node_completion WHERE user_id = #{userId} AND node_id = #{nodeId}")
    int exists(@Param("userId") long userId, @Param("nodeId") long nodeId);

    /**
     * 标记节点为已完成
     */
    @Insert("INSERT INTO user_node_completion(user_id, node_id, completed_at) " +
            "VALUES (#{userId}, #{nodeId}, #{completedAt})")
    int insert(UserNodeCompletionDO record);

    /**
     * 取消节点完成
     */
    @Delete("DELETE FROM user_node_completion WHERE user_id = #{userId} AND node_id = #{nodeId}")
    int delete(@Param("userId") long userId, @Param("nodeId") long nodeId);
}