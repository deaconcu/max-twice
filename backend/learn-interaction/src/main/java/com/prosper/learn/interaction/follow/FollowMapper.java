package com.prosper.learn.interaction.follow;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

public interface FollowMapper {

    @Select("SELECT * FROM `follow` WHERE follower_id = #{followerId} and followee_id = #{followeeId}")
    FollowDO get(long followerId, long followeeId);

    @Select("SELECT * FROM `follow` " +
            "WHERE follower_id = #{followerId} and created_at < #{createdAt} " +
            "order by created_at DESC " +
            "LIMIT #{limit} ")
    List<FollowDO> getList(long followerId, LocalDateTime createdAt, int limit);

    @Insert("INSERT INTO follow(follower_id, followee_id) VALUES (#{followerId}, #{followeeId})")
    int insert(long followerId, long followeeId);

    @Update("DELETE FROM `follow` WHERE follower_id = #{followerId} and followee_id = #{followeeId}")
    void delete(long followerId, long followeeId);
}
