package com.prosper.learn.content.toc;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserNodeTocMapper {

    @Select("SELECT * FROM user_node_toc WHERE id = #{id}")
    UserNodeTocDO get(long id);

    @Select("SELECT * FROM user_node_toc WHERE user_id = #{userId} and node_id = #{nodeId}")
    UserNodeTocDO getByUserAndNode(long userId, long nodeId);

    @Select("<script>" +
            "SELECT * FROM user_node_toc WHERE user_id = #{userId} AND node_id IN " +
            "<foreach item='item' index='index' collection='nodeIds' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    List<UserNodeTocDO> getByUserAndNodes(@Param("userId") long userId, @Param("nodeIds") List<Long> nodeIds);

    @Insert("INSERT INTO user_node_toc(user_id, node_id, toc) " +
        "VALUES (#{userId}, #{nodeId}, #{toc})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserNodeTocDO userNodeTocDO);

    @Update("UPDATE user_node_toc SET toc = #{toc} where id = #{id}")
    void update(UserNodeTocDO userNodeTocDO);

    @Delete("DELETE FROM user_node_toc where id = #{id}")
    void delete(long id);
}
