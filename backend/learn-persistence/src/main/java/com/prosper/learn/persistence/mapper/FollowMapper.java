package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.FollowDO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

public interface FollowMapper {

    @Select("SELECT * FROM `follow` WHERE follower_id = #{followerId} and followee_id = #{followeeId}")
    FollowDO get(int followerId, int followeeId);

    @Select("SELECT * FROM `follow` " +
            "WHERE follower_id = #{followerId} and created_at < #{createdAt} " +
            "order by created_at DESC " +
            "LIMIT #{limit} ")
    List<FollowDO> getList(int followerId, LocalDateTime createdAt, int limit);

    @Insert("INSERT INTO follow(follower_id, followee_id) VALUES (#{followerId}, #{followeeId})")
    int insert(int followerId, int followeeId);

    @Update("DELETE FROM `follow` WHERE follower_id = #{followerId} and followee_id = #{followeeId}")
    void delete(int followerId, int followeeId);
}
