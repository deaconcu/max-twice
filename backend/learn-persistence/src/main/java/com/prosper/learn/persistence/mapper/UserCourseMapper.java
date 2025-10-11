package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserCourseDO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserCourseMapper {

    /**
     * 插入用户课程进度记录
     */
    @Insert("INSERT INTO user_course (user_id, course_id, state, started_at) " +
            "VALUES (#{userId}, #{courseId}, #{state}, #{startedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserCourseDO userCourseDO);

    /**
     * 根据用户ID和课程ID查询进度
     */
    @Select("SELECT * FROM user_course WHERE user_id = #{userId} AND course_id = #{courseId}")
    UserCourseDO getByUserIdAndCourseId(long userId, long courseId);

    /**
     * 更新用户课程进度
     */
    @Update("UPDATE user_course SET progress_percent = #{progressPercent}, state = #{state}, " +
            "completed_at = #{completedAt} WHERE id = #{id}")
    int update(UserCourseDO userCourseDO);

    @Select("SELECT * FROM user_course WHERE user_id = #{userId} and id < #{lastId} ORDER BY id DESC LIMIT 20")
    List<UserCourseDO> getByUserId(long userId, long lastId);

    @Select("SELECT * FROM user_course WHERE course_id = #{courseId}")
    List<UserCourseDO> getByCourseId(long courseId);

    @Delete("DELETE FROM user_course WHERE id = #{id}")
    void delete(long id);

    @Delete("DELETE FROM user_course WHERE user_id = #{userId} AND course_id = #{courseId}")
    void deleteByUserAndCourse(long userId, long courseId);
    
    /**
     * 统计用户正在学习的课程数量
     */
    @Select("SELECT COUNT(*) FROM user_course WHERE user_id = #{userId} AND state = 1")
    Integer countActiveCoursesByUserId(long userId);

    /**
     * 批量查询用户对多个课程的学习进度，返回Map<courseId, UserCourseDO>
     */
    @MapKey("courseId") 
    @Select("<script>" +
            "SELECT * FROM user_course WHERE user_id = #{userId} AND course_id IN " +
            "<foreach collection='courseIds' item='courseId' open='(' separator=',' close=')'>" +
            "#{courseId}" +
            "</foreach>" +
            "</script>")
    Map<Long, UserCourseDO> getByUserIdAndCourseIdsAsMap(long userId, List<Long> courseIds);
}
