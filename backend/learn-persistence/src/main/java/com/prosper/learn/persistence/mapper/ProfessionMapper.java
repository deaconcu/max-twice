package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.ProfessionDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ProfessionMapper {

    @Select("SELECT * FROM profession WHERE id = #{id}")
    ProfessionDO get(@Param("id") int id);

    @Select("SELECT * FROM profession ORDER BY id DESC LIMIT #{size} OFFSET #{offset}")
    List<ProfessionDO> listByPage(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT * FROM profession WHERE state = #{state} AND id > #{lastId} ORDER BY id ASC LIMIT 20")
    List<ProfessionDO> listByStateAndLastId(@Param("state") String state, @Param("lastId") int lastId);

    @Select("SELECT * FROM profession WHERE main_category = #{mainCategory} AND state = 'APPROVED' AND id > #{lastId} ORDER BY id ASC LIMIT 20")
    List<ProfessionDO> listByMainCategoryAndLastId(@Param("mainCategory") int mainCategory, @Param("lastId") int lastId);

    @Select("SELECT * FROM profession WHERE sub_category = #{subCategory} AND state = 'APPROVED' AND id > #{lastId} ORDER BY id ASC LIMIT 20")
    List<ProfessionDO> listBySubCategoryAndLastId(@Param("subCategory") int subCategory, @Param("lastId") int lastId);

    @Select("SELECT * FROM profession WHERE main_category = #{mainCategory} AND sub_category = #{subCategory} AND state = 'APPROVED' AND id > #{lastId} ORDER BY id ASC LIMIT 20")
    List<ProfessionDO> listByMainCategoryAndSubCategoryAndLastId(@Param("mainCategory") int mainCategory, @Param("subCategory") int subCategory, @Param("lastId") int lastId);

    @Select("SELECT * FROM profession WHERE INSTR(name, #{name}) > 0 limit 20")
    List<ProfessionDO> searchByName(@Param("name") String name);

    @Select({"<script>SELECT * FROM profession where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<ProfessionDO> getByIds(@Param("ids") Collection<Integer> ids);

    @Select({"<script>SELECT * FROM profession where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Integer, ProfessionDO> getMapByIds(@Param("ids") Collection<Integer> ids);

    @Insert("INSERT INTO profession(name, description, icon, price, skills, main_category, sub_category, " +
            "state, rejected_reason, creator) " +
            "VALUES (#{name}, #{description}, #{icon}, #{price}, #{skills}, #{mainCategory}, #{subCategory}, " +
            "#{state}, #{rejectedReason}, #{creator})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ProfessionDO professionDO);

    @Update("UPDATE profession SET name = #{name}, description = #{description}, icon = #{icon}, " +
            "price = #{price}, skills = #{skills}, main_category = #{mainCategory}, " +
            "sub_category = #{subCategory}, state = #{state}, rejected_reason = #{rejectedReason}, " +
            "creator = #{creator}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    void update(ProfessionDO professionDO);

    @Update("UPDATE profession SET state = 'APPROVED', updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int approve(@Param("id") int id);

    @Update("UPDATE profession SET state = 'REJECTED', rejected_reason = #{rejectedReason}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int reject(@Param("id") int id, @Param("rejectedReason") String rejectedReason);

    @Update("UPDATE profession SET state = 'BLOCKED', updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int block(@Param("id") int id);

    @Delete("DELETE FROM profession WHERE id = #{id}")
    void delete(@Param("id") int id);
}
