package com.prosper.learn.interaction.upvote;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UpvoteMapper {

    @Select("SELECT * FROM upvote where id = #{id}")
    UpvoteDO getById(long id);

    @Select("SELECT * FROM upvote where user_id = #{userId} and object_id = #{objectId} and object_type = #{objectType}")
    UpvoteDO getByUserAndObject(long userId, long objectId, int objectType);

    @Select({"<script>SELECT object_id, type FROM upvote where user_id = #{userId} and object_type = #{objectType} and object_id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<UpvoteDO> getList(long userId, List<Long> ids, int objectType);

    @Insert("INSERT INTO upvote(user_id, object_id, object_type, type) VALUES (#{userId}, #{objectId}, #{objectType}, #{type})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UpvoteDO upvoteDO);

    @Update("UPDATE upvote SET type = #{type} WHERE id = #{id}")
    void update(UpvoteDO upvoteDO);

    @Delete("DELETE FROM upvote where id = #{id}")
    void delete(long id);
}
