package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UpvoteDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface UpvoteMapper {

    @Select("SELECT * FROM upvote where userId = #{userId} and object_id = #{objectId} and object_type = #{objectType}")
    UpvoteDO get(int userId, int objectId, int objectType);

    @Select({"<script>SELECT object_id, type FROM upvote where userId = #{userId} and object_type = #{objectType} and object_id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<UpvoteDO> getList(int userId, List<Integer> ids, int objectType);

    @Insert("INSERT INTO upvote(userId, object_id, object_type, type) VALUES (#{userId}, #{objectId}, #{objectType}, #{type})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UpvoteDO upvoteDO);

    @Update("UPDATE upvote SET userId = #{userId}, object_id = #{objectId}, object_type = #{objectType}, type = #{type} where id = #{id}")
    void update(UpvoteDO upvoteDO);

    @Delete("DELETE FROM upvote where id = #{id}")
    void delete(int id);
}
