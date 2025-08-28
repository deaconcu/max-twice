package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.PostDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface CourseMapper {

    @Select("SELECT * FROM course WHERE id = #{id}")
    CourseDO getById(@Param("id") int id);

    @Select({"<script>SELECT * FROM course where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<CourseDO> getByIds(@Param("ids") List<Integer> ids);

    @Select("SELECT * FROM course WHERE name LIKE CONCAT('%', #{name}, '%') limit #{limit}")
    List<CourseDO> searchByName(@Param("name") String name, @Param("limit") int limit);

    @Select("SELECT * FROM course where state = #{state} and parent = #{parent} ORDER BY ctime DESC")
    List<CourseDO> listByParentAndState(@Param("state") String state,
                                        @Param("parent") int parent);

    @Select("SELECT * FROM course where parent = #{parent} ORDER BY ctime DESC")
    List<CourseDO> listByParent(@Param("parent") int parent);

    @Select("SELECT * FROM course where state = #{state} and creator = #{creator} ORDER BY ctime DESC LIMIT #{offset}, #{limit}")
    List<CourseDO> list(@Param("state") String state,
                        @Param("creator") int creator,
                        @Param("limit") int limit,
                        @Param("offset") int offset);

    // 新增：根据状态和lastId获取列表
    @Select("SELECT * FROM course WHERE state = #{state} AND id > #{lastId} ORDER BY utime DESC LIMIT 20")
    List<CourseDO> listByStateAndLastId(@Param("state") String state, @Param("lastId") int lastId);

    // 新增：根据主分类和子分类获取已批准的课程列表
    @Select("SELECT * FROM course WHERE main_category = #{mainCategory} AND sub_category = #{subCategory} " +
            "AND state = 'APPROVED' AND parent = 0 ORDER BY id ASC LIMIT 20")
    List<CourseDO> listRootByCategory(@Param("mainCategory") int mainCategory, @Param("subCategory") int subCategory);

    @Insert("INSERT INTO course(name, description, creator, parent, state, root_node, main_category, sub_category) " +
            "VALUES (#{name}, #{description}, #{creator}, #{parent}, #{state}, #{rootNode}, #{mainCategory}, #{subCategory})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(CourseDO course);

    @Update("UPDATE course SET name = #{name}, description = #{description}, creator = #{creator}, root_node = #{rootNode}, " +
            "parent = #{parent}, state = #{state}, main_category = #{mainCategory}, sub_category = #{subCategory} where id = #{id}")
    void update(CourseDO course);

    // 新增：课程状态操作方法
    @Update("UPDATE course SET state = 'APPROVED', rejected_reason = '', utime = CURRENT_TIMESTAMP WHERE id = #{id}")
    int approve(@Param("id") int id);

    @Update("UPDATE course SET state = 'REJECTED', rejected_reason = #{rejectedReason}, utime = CURRENT_TIMESTAMP WHERE id = #{id}")
    int reject(@Param("id") int id, @Param("rejectedReason") String rejectedReason);

    @Delete("DELETE FROM course WHERE id = #{id}")
    int delete(@Param("id") int id);
    
    // 平台统计相关方法
    @Select("SELECT COUNT(*) FROM course WHERE state = 'APPROVED'")
    Long countActiveCourses();
}
