package com.prosper.learn.user.profile;

import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

public interface UserProfileMapper {

    @Select("SELECT * FROM user_profile WHERE user_id = #{userId}")
    UserProfileDO getById(long id);

    @Select({"<script>SELECT * FROM user_profile where user_id in " +
            "<foreach item='userId' collection='userIds' open='(' separator=', ' close=')'>#{userId}</foreach>" +
            "</script>"})
    List<UserProfileDO> getByIds(Collection<Integer> userIds);

    @Insert("INSERT INTO user_profile(user_id, subscription, roadmap_pin) " +
            "VALUES (#{userId}, #{subscription}, #{roadmapPin})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(UserProfileDO user);

    @Update("UPDATE user_profile SET subscription = #{subscription}, roadmap_pin = #{roadmapPin} where user_id = #{userId}")
    void update(UserProfileDO user);

    @Update("UPDATE user_profile SET roadmap_pin = #{roadmapPin} where user_id = #{userId}")
    void updateRoadmapPin(long id, String roadmapPin);

    @Select("SELECT user_id, subscription FROM user_profile LIMIT #{offset}, #{limit}")
    List<UserProfileDO> getSubscriptionDataByPage(int offset, int limit);

}
