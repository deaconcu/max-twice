package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.SubcourseDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface SubcourseMapper {

    @Select("SELECT * FROM subcourse WHERE id = #{id}")
    SubcourseDO getById(@Param("id") int id);

    /*
    @Select("SELECT * FROM subcourse WHERE name = #{name} and fk_course_Id = #{courseId}")
    SubcourseDO getByNameAndCourse(int courseId, String name);
     */

    @Select("SELECT * FROM subcourse WHERE courseId = #{courseId} order by ctime")
    List<SubcourseDO> getByCourse(@Param("courseId") int courseId);

    /*
    @Select("SELECT * FROM subcourse limit #{limit}, #{offset}")
    List<SubcourseDO> list(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM subcourse limit #{limit}, #{offset}")
    List<SubcourseDO> listByCourse(int courseId, @Param("limit") int limit, @Param("offset") int offset);
     */

    @Insert("INSERT INTO subcourse(name, description, creator, courseId, state) VALUES (#{subcourse.name}, " +
            "#{subcourse.description}, #{subcourse.creator}, #{subcourse.courseId}, #{subcourse.state})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(@Param("subcourse") SubcourseDO subcourse);

    @Update("UPDATE subcourse SET name = #{subcourse.name}, description = #{subcourse.description}, " +
            "state = #{subcourse.state} where id = #{subcourse.id}")
    void update(@Param("subcourse") SubcourseDO subcourse);

}
