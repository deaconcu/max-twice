package com.prosper.learn.content.course;

import com.prosper.learn.shared.domain.Enums;
import org.apache.ibatis.annotations.*;
import java.util.List;

import static com.prosper.learn.shared.domain.Enums.*;

public interface CourseMapper {

    @Select("SELECT * FROM course WHERE id = #{id}")
    CourseDO getById(long id);

    @Select({"<script>SELECT * FROM course where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<CourseDO> getByIds(List<Long> ids);

    @Select("SELECT * FROM course WHERE name LIKE CONCAT('%', #{name}, '%') limit #{limit}")
    List<CourseDO> searchByName(String name, int limit);

    @Select("SELECT * FROM course where state = #{state.value} and parent_course_id = #{parentCourseId} ORDER BY created_at DESC")
    List<CourseDO> listByParentAndState(ContentState state, long parentCourseId);

    @Select("SELECT * FROM course where parent_course_id = #{parentCourseId} ORDER BY created_at DESC")
    List<CourseDO> listByParent(long parentCourseId);

    @Select("SELECT * FROM course where state = #{state.value} and creator_id = #{creatorId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<CourseDO> list(ContentState state, long creatorId, int limit, int offset);

    // 新增：根据状态和lastId获取列表
    @Select("<script>" +
            "SELECT * FROM course WHERE state = #{state.value} " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT 20" +
            "</script>")
    List<CourseDO> listByStateAndLastId(ContentState state, Long lastId);

    // 新增：根据主分类获取已批准的课程列表（支持分页）
    @Select("<script>" +
            "SELECT * FROM course WHERE main_category = #{mainCategory} " +
            "AND state = " + ContentState.PUBLISHED_VALUE + " AND parent_course_id = 0 " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT 20" +
            "</script>")
    List<CourseDO> listRootByMainCategory(int mainCategory, Long lastId);

    // 新增：根据主分类和子分类获取已批准的课程列表（支持分页）
    @Select("<script>" +
            "SELECT * FROM course WHERE main_category = #{mainCategory} AND sub_category = #{subCategory} " +
            "AND state = " + ContentState.PUBLISHED_VALUE + " AND parent_course_id = 0 " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT 20" +
            "</script>")
    List<CourseDO> listRootByCategory(int mainCategory, int subCategory, Long lastId);

    @Insert("INSERT INTO course(name, description, creator_id, parent_course_id, state, root_node_id, main_category, sub_category) " +
            "VALUES (#{name}, #{description}, #{creatorId}, #{parentCourseId}, #{state}, #{rootNodeId}, #{mainCategory}, #{subCategory})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(CourseDO course);

    @Update("UPDATE course SET name = #{name}, description = #{description}, creator_id = #{creatorId}, root_node_id = #{rootNodeId}, " +
            "parent_course_id = #{parentCourseId}, state = #{state}, main_category = #{mainCategory}, sub_category = #{subCategory} where id = #{id}")
    void update(CourseDO course);

    // 新增：课程状态操作方法
    @Update("UPDATE course SET state = " + ContentState.PUBLISHED_VALUE + ", reason = '' WHERE id = #{id}")
    int approve(long id);

    @Update("UPDATE course SET state = " + ContentState.REJECTED_VALUE + ", reason = #{reason} WHERE id = #{id}")
    int reject(long id, String reason);

    @Update("UPDATE course SET state = " + ContentState.BANNED_VALUE + ", reason = #{reason} WHERE id = #{id}")
    int ban(long id, String reason);

    @Delete("DELETE FROM course WHERE id = #{id}")
    int delete(long id);
    
    // 平台统计相关方法
    @Select("SELECT COUNT(*) FROM course WHERE state = " + ContentState.PUBLISHED_VALUE)
    Long countActiveCourses();
}
