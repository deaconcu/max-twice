package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.PostDO;
import com.prosper.learn.common.Enums.CourseState;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface CourseMapper {

    @Select("SELECT * FROM course WHERE id = #{id}")
    CourseDO getById(long id);

    @Select({"<script>SELECT * FROM course where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<CourseDO> getByIds(List<Long> ids);

    @Select("SELECT * FROM course WHERE name LIKE CONCAT('%', #{name}, '%') limit #{limit}")
    List<CourseDO> searchByName(String name, int limit);

    @Select("SELECT * FROM course where state = #{state.value} and parent = #{parent} ORDER BY created_at DESC")
    List<CourseDO> listByParentAndState(CourseState state, long parent);

    @Select("SELECT * FROM course where parent = #{parent} ORDER BY created_at DESC")
    List<CourseDO> listByParent(long parent);

    @Select("SELECT * FROM course where state = #{state.value} and creator = #{creator} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<CourseDO> list(CourseState state, long creator, int limit, int offset);

    // 新增：根据状态和lastId获取列表
    @Select("SELECT * FROM course WHERE state = #{state.value} AND id > #{lastId} ORDER BY updated_at DESC LIMIT 20")
    List<CourseDO> listByStateAndLastId(CourseState state, long lastId);

    // 新增：根据主分类和子分类获取已批准的课程列表
    // 修正状态值：使用数字1代替字符串'APPROVED'，对应CourseState.APPROVED.value()
    @Select("SELECT * FROM course WHERE main_category = #{mainCategory} AND sub_category = #{subCategory} " +
            "AND state = 1 AND parent = 0 ORDER BY id ASC LIMIT 20")
    List<CourseDO> listRootByCategory(int mainCategory, int subCategory);

    @Insert("INSERT INTO course(name, description, creator, parent, state, root_node, main_category, sub_category) " +
            "VALUES (#{name}, #{description}, #{creator}, #{parent}, #{state}, #{rootNode}, #{mainCategory}, #{subCategory})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(CourseDO course);

    @Update("UPDATE course SET name = #{name}, description = #{description}, creator = #{creator}, root_node = #{rootNode}, " +
            "parent = #{parent}, state = #{state}, main_category = #{mainCategory}, sub_category = #{subCategory} where id = #{id}")
    void update(CourseDO course);

    // 新增：课程状态操作方法
    // 修正状态值：使用数字1代替字符串'APPROVED'，对应CourseState.APPROVED.value()
    @Update("UPDATE course SET state = 1, rejected_reason = '', updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int approve(long id);

    // 修正状态值：使用数字2代替字符串'REJECTED'，对应CourseState.REJECTED.value()
    @Update("UPDATE course SET state = 2, rejected_reason = #{rejectedReason}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int reject(long id, String rejectedReason);

    @Delete("DELETE FROM course WHERE id = #{id}")
    int delete(long id);
    
    // 平台统计相关方法
    // 修正状态值：使用数字1代替字符串'APPROVED'，对应CourseState.APPROVED.value()
    @Select("SELECT COUNT(*) FROM course WHERE state = 1")
    Long countActiveCourses();
}
