package com.prosper.learn.content.toc;

import org.apache.ibatis.annotations.*;

public interface UserCourseTocMapper {

    @Select("SELECT * FROM user_course_toc WHERE id = #{id}")
    UserCourseTocDO get(long id);

    @Select("SELECT * FROM user_course_toc WHERE user_id = #{userId} and course_id = #{courseId}")
    UserCourseTocDO getByUserAndCourse(long userId, long courseId);

    @Insert("INSERT INTO user_course_toc(user_id, course_id, toc) " +
        "VALUES (#{userId}, #{courseId}, #{toc})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserCourseTocDO userCourseTocDO);

    @Update("UPDATE user_course_toc SET toc = #{toc} where id = #{id}")
    void update(UserCourseTocDO userCourseTocDO);

    @Delete("DELETE FROM user_course_toc where id = #{id}")
    void delete(long id);
}
