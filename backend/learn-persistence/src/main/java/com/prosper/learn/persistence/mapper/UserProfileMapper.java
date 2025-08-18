package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserProfileDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;

public interface UserProfileMapper {

    @Select("SELECT * FROM user_profile WHERE id = #{id}")
    UserProfileDO getById(@Param("id") int id);

    @Select({"<script>SELECT * FROM user_profile where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<UserProfileDO> getByIds(@Param("ids") Collection<Integer> ids);

    @Insert("INSERT INTO user_profile(id, subscription, roadmap_pin) " +
            "VALUES (#{id}, #{subscription}, #{roadmapPin})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(UserProfileDO user);

    @Update("UPDATE user_profile SET subscription = #{subscription}, roadmap_pin = #{roadmapPin} where id = #{id}")
    void update(UserProfileDO user);

    @Update("UPDATE user_profile SET roadmap_pin = #{roadmapPin} where id = #{id}")
    void updateRoadmapPin(@Param("id") int id, @Param("roadmapPin") String roadmapPin);

}
