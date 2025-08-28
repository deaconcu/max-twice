package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.ProfessionDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ProfessionMapper {

    @Select("SELECT * FROM profession WHERE id = #{id}")
    ProfessionDO get(int id);

    @Select("SELECT * FROM profession ORDER BY id DESC LIMIT #{size} OFFSET #{offset}")
    List<ProfessionDO> listByPage(int offset, int size);

    @Select("SELECT * FROM profession WHERE state = #{state} AND id > #{lastId} ORDER BY id ASC LIMIT 20")
    List<ProfessionDO> listByStateAndLastId(String state, int lastId);

    @Select("SELECT * FROM profession WHERE main_category = #{mainCategory} AND state = 'APPROVED' AND id > #{lastId} ORDER BY id ASC LIMIT 20")
    List<ProfessionDO> listByMainCategoryAndLastId(int mainCategory, int lastId);

    @Select("SELECT * FROM profession WHERE sub_category = #{subCategory} AND state = 'APPROVED' AND id > #{lastId} ORDER BY id ASC LIMIT 20")
    List<ProfessionDO> listBySubCategoryAndLastId(int subCategory, int lastId);

    @Select("SELECT * FROM profession WHERE main_category = #{mainCategory} AND sub_category = #{subCategory} AND state = 'APPROVED' AND id > #{lastId} ORDER BY id ASC LIMIT 20")
    List<ProfessionDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, int lastId);

    @Select("SELECT * FROM profession WHERE INSTR(name, #{name}) > 0 limit 20")
    List<ProfessionDO> searchByName(String name);

    @Select({"<script>SELECT * FROM profession where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<ProfessionDO> getByIds(Collection<Integer> ids);

    @Select({"<script>SELECT * FROM profession where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Integer, ProfessionDO> getMapByIds(Collection<Integer> ids);

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
    int approve(int id);

    @Update("UPDATE profession SET state = 'REJECTED', rejected_reason = #{rejectedReason}, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id}")
    int reject(int id, String rejectedReason);

    @Update("UPDATE profession SET state = 'BLOCKED', updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int block(int id);

    @Delete("DELETE FROM profession WHERE id = #{id}")
    void delete(int id);
    
    // 平台统计相关方法
    @Select("SELECT COUNT(*) FROM profession WHERE state = 'APPROVED'")
    Long countActiveProfessions();
}
