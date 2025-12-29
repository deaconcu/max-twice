package com.prosper.learn.interaction.follow;

import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FollowMapper {

    @Select("SELECT * FROM `follow` WHERE follower_id = #{followerId} and followee_id = #{followeeId}")
    FollowDO get(long followerId, long followeeId);

    @Select({"<script>",
            "SELECT * FROM `follow` ",
            "WHERE follower_id = #{followerId} ",
            "<if test='lastId != null'>",
            "  AND id &lt; #{lastId} ",
            "</if>",
            "ORDER BY id DESC ",
            "LIMIT #{limit}",
            "</script>"})
    List<FollowDO> getList(@Param("followerId") long followerId,
                           @Param("lastId") Long lastId,
                           @Param("limit") int limit);

    @Insert("INSERT INTO follow(follower_id, followee_id) VALUES (#{followerId}, #{followeeId})")
    int insert(long followerId, long followeeId);

    @Update("DELETE FROM `follow` WHERE follower_id = #{followerId} and followee_id = #{followeeId}")
    void delete(long followerId, long followeeId);
}
