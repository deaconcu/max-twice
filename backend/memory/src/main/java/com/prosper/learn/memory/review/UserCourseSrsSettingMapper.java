package com.prosper.learn.memory.review;

import org.apache.ibatis.annotations.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserCourseSrsSettingMapper {

    @Select("SELECT * FROM user_course_srs_setting WHERE id = #{id}")
    UserCourseSrsSettingDO get(long id);

    @Select("SELECT * FROM user_course_srs_setting WHERE user_id = #{userId} AND course_id = #{courseId}")
    UserCourseSrsSettingDO getByUserAndCourse(long userId, long courseId);

    @Select({"<script>SELECT * FROM user_course_srs_setting WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<UserCourseSrsSettingDO> getByIds(List<Long> ids);

    @Select({"<script>SELECT * FROM user_course_srs_setting WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, UserCourseSrsSettingDO> getMapByIds(Collection<Long> ids);

    @Select("SELECT * FROM user_course_srs_setting WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC")
    List<UserCourseSrsSettingDO> getByUser(long userId);

    @Insert("INSERT INTO user_course_srs_setting " +
            "(user_id, course_id, frequency_setting, state, card_order, daily_new_limit, daily_review_limit, frozen_at, frozen_duration) " +
            "VALUES " +
            "(#{userId}, #{courseId}, #{frequencySetting}, #{state}, #{cardOrder}, #{dailyNewLimit}, #{dailyReviewLimit}, #{frozenAt}, #{frozenDuration})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserCourseSrsSettingDO setting);

    @Update("UPDATE user_course_srs_setting SET " +
            "frequency_setting = #{frequencySetting}, state = #{state}, card_order = #{cardOrder}, " +
            "daily_new_limit = #{dailyNewLimit}, daily_review_limit = #{dailyReviewLimit}, " +
            "frozen_at = #{frozenAt}, frozen_duration = #{frozenDuration}, " +
            "updated_at = NOW() " +
            "WHERE id = #{id}")
    void update(UserCourseSrsSettingDO setting);

}
