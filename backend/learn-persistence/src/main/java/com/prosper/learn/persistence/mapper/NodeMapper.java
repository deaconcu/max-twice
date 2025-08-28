package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.NodeDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface NodeMapper {


    @Select("SELECT * FROM node WHERE id = #{id}")
    NodeDO getById(int id);

    @Select({"<script>SELECT * FROM node where id in " +
                 "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
             "</script>"})
    List<NodeDO> getByIds(List<Integer> ids);

    @Select({"<script>SELECT * FROM node where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Integer, NodeDO> getMapByIds(Collection<Integer> ids);

    @Select("SELECT * FROM node where parent = #{parentId}")
    List<NodeDO> getByParent(int parentId);

    /*
    @Select("SELECT * FROM node limit #{offset}, #{limit}")
    List<NodeDO> list(int limit, int offset);
     */

    @Select("SELECT * FROM node where course_id = #{courseId}")
    List<NodeDO> listBySubcourse(int courseId);

    /*
    @Select("SELECT * FROM node order by created_at desc limit #{offset}, #{limit}") // TODO
    List<NodeDO> listByUser(int userId, int limit, int offset);
     */

    @Insert("INSERT INTO node(name, description, course_id, root, creator) " +
            "VALUES (#{name}, #{description}, #{courseId}, #{root}, #{creator})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(NodeDO Node);

    @Update("UPDATE node SET name = #{name}, description = #{description}, " +
            "creator = #{creator}, course_id = #{courseId}, root = #{root}, " +
            "comment_count = #{commentCount} where id = #{id}")
    void update(NodeDO Node);
    
    // 平台统计相关方法
    @Select("SELECT COUNT(*) FROM node WHERE course_id > 0")
    Long countActiveNodes();

}
