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

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT * FROM user_course_srs_setting WHERE user_id = #{userId} AND state = #{state} " +
//            "ORDER BY created_at DESC")
//    List<UserCourseSrsSettingDO> getByUserAndState(long userId, int state);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT * FROM user_course_srs_setting WHERE course_id = #{courseId} AND state = #{state}")
//    List<UserCourseSrsSettingDO> getByCourseAndState(long courseId, int state);
// --注释掉检查 STOP (2025/12/10 12:04)

    @Insert("INSERT INTO user_course_srs_setting " +
            "(user_id, course_id, frequency_setting, state) " +
            "VALUES " +
            "(#{userId}, #{courseId}, #{frequencySetting}, #{state})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserCourseSrsSettingDO setting);

    @Update("UPDATE user_course_srs_setting SET " +
            "frequency_setting = #{frequencySetting}, state = #{state} " +
            "WHERE id = #{id}")
    void update(UserCourseSrsSettingDO setting);

// --注释掉检查 START (2025/12/10 12:04):
//    @Update("UPDATE user_course_srs_setting SET frequency_setting = #{frequencySetting} " +
//            "WHERE user_id = #{userId} AND course_id = #{courseId}")
//    int updateFrequencySetting(long userId, long courseId, int frequencySetting);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Update("UPDATE user_course_srs_setting SET state = #{state} " +
//            "WHERE user_id = #{userId} AND course_id = #{courseId}")
//    int updateState(long userId, long courseId, int state);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Delete("DELETE FROM user_course_srs_setting WHERE user_id = #{userId} AND course_id = #{courseId}")
//    int deleteByUserAndCourse(long userId, long courseId);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT COUNT(*) FROM user_course_srs_setting WHERE user_id = #{userId}")
//    int countByUser(long userId);
// --注释掉检查 STOP (2025/12/10 12:04)

// --注释掉检查 START (2025/12/10 12:04):
//    @Select("SELECT COUNT(*) FROM user_course_srs_setting WHERE user_id = #{userId} AND state = #{state}")
//    int countByUserAndState(long userId, int state);
// --注释掉检查 STOP (2025/12/10 12:04)

}