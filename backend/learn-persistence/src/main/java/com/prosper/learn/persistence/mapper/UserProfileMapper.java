package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserProfileDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

public interface UserProfileMapper {

    @Select("SELECT * FROM user_profile WHERE id = #{id}")
    UserProfileDO getById(int id);

    @Select({"<script>SELECT * FROM user_profile where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<UserProfileDO> getByIds(Collection<Integer> ids);

    @Insert("INSERT INTO user_profile(id, subscription, roadmap_pin) " +
            "VALUES (#{id}, #{subscription}, #{roadmapPin})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(UserProfileDO user);

    @Update("UPDATE user_profile SET subscription = #{subscription}, roadmap_pin = #{roadmapPin} where id = #{id}")
    void update(UserProfileDO user);

    @Update("UPDATE user_profile SET roadmap_pin = #{roadmapPin} where id = #{id}")
    void updateRoadmapPin(int id, String roadmapPin);

    @Select("SELECT id, subscription FROM user_profile LIMIT #{offset}, #{limit}")
    List<UserProfileDO> getSubscriptionDataByPage(int offset, int limit);

}
