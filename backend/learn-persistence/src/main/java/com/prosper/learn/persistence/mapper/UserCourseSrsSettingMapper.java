package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserCourseSrsSettingDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    @Select("SELECT * FROM user_course_srs_setting WHERE user_id = #{userId} AND status = #{status} " +
            "ORDER BY created_at DESC")
    List<UserCourseSrsSettingDO> getByUserAndStatus(long userId, int status);

    @Select("SELECT * FROM user_course_srs_setting WHERE course_id = #{courseId} AND status = #{status}")
    List<UserCourseSrsSettingDO> getByCourseAndStatus(long courseId, int status);

    @Insert("INSERT INTO user_course_srs_setting " +
            "(user_id, course_id, frequency_setting, status) " +
            "VALUES " +
            "(#{userId}, #{courseId}, #{frequencySetting}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserCourseSrsSettingDO setting);

    @Update("UPDATE user_course_srs_setting SET " +
            "frequency_setting = #{frequencySetting}, status = #{status} " +
            "WHERE id = #{id}")
    void update(UserCourseSrsSettingDO setting);

    @Update("UPDATE user_course_srs_setting SET frequency_setting = #{frequencySetting} " +
            "WHERE user_id = #{userId} AND course_id = #{courseId}")
    int updateFrequencySetting(long userId, long courseId, int frequencySetting);

    @Update("UPDATE user_course_srs_setting SET status = #{status} " +
            "WHERE user_id = #{userId} AND course_id = #{courseId}")
    int updateStatus(long userId, long courseId, int status);

    @Delete("DELETE FROM user_course_srs_setting WHERE user_id = #{userId} AND course_id = #{courseId}")
    int deleteByUserAndCourse(long userId, long courseId);

    @Select("SELECT COUNT(*) FROM user_course_srs_setting WHERE user_id = #{userId}")
    int countByUser(long userId);

    @Select("SELECT COUNT(*) FROM user_course_srs_setting WHERE user_id = #{userId} AND status = #{status}")
    int countByUserAndStatus(long userId, int status);

}