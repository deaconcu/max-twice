package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.FollowDO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

public interface FollowMapper {

    @Select("SELECT * FROM `follow` WHERE follower_id = #{followerId} and followee_id = #{followeeId}")
    FollowDO get(@Param("followerId") int followerId, @Param("followeeId") int followeeId);

    @Select("SELECT * FROM `follow` " +
            "WHERE follower_id = #{followerId} and create_time < #{createTime} " +
            "order by create_time DESC " +
            "LIMIT #{limit} ")
    List<FollowDO> getList(@Param("followerId") int followerId,
                           @Param("createTime") LocalDateTime createTime,
                           @Param("limit") int limit);

    @Insert("INSERT INTO follow(follower_id, followee_id) VALUES (#{followerId}, #{followeeId})")
    int insert(@Param("followerId") int followerId, @Param("followeeId") int followeeId);

    @Update("DELETE FROM `follow` WHERE follower_id = #{followerId} and followee_id = #{followeeId}")
    void delete(@Param("followerId") int followerId, @Param("followeeId") int followeeId);
}
