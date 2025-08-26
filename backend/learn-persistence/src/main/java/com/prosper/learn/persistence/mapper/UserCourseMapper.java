package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserCourseDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserCourseMapper {

    /**
     * 插入用户课程进度记录
     */
    @Insert("INSERT INTO user_course(user_id, course_id, progress_percent, status, started_at, completed_at, created_at, updated_at) " +
            "VALUES (#{userId}, #{courseId}, #{progressPercent}, #{status}, #{startedAt}, #{completedAt}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserCourseDO userCourseDO);

    /**
     * 根据用户ID和课程ID查询进度
     */
    @Select("SELECT * FROM user_course WHERE user_id = #{userId} AND course_id = #{courseId}")
    UserCourseDO getByUserIdAndCourseId(Long userId, Long courseId);

    /**
     * 更新用户课程进度
     */
    @Update("UPDATE user_course SET progress_percent = #{progressPercent}, status = #{status}, " +
            "completed_at = #{completedAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(UserCourseDO userCourseDO);

    @Select("SELECT * FROM user_course WHERE user_id = #{userId} and id < #{lastId} ORDER BY id DESC LIMIT 20")
    List<UserCourseDO> getByUserId(Long userId, Long lastId);

    @Select("SELECT * FROM user_course WHERE course_id = #{courseId}")
    List<UserCourseDO> getByCourseId(Long courseId);

    @Delete("DELETE FROM user_course WHERE id = #{id}")
    void delete(Long id);

    @Delete("DELETE FROM user_course WHERE user_id = #{userId} AND course_id = #{courseId}")
    void deleteByUserAndCourse(Long userId, Long courseId);
    
    /**
     * 统计用户正在学习的课程数量
     */
    @Select("SELECT COUNT(*) FROM user_course WHERE user_id = #{userId} AND status = 'IN_PROGRESS'")
    Integer countActiveCoursesByUserId(Long userId);
}
