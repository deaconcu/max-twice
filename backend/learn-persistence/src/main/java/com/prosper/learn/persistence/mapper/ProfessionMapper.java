package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.ProfessionDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.prosper.learn.common.Enums.ContentState.*;

public interface ProfessionMapper {

    @Select("SELECT * FROM profession WHERE id = #{id}")
    ProfessionDO getById(long id);

    @Select("SELECT * FROM profession ORDER BY id DESC LIMIT #{size} OFFSET #{offset}")
    List<ProfessionDO> listByPage(int offset, int size);

    @Select("<script>" +
            "SELECT * FROM profession WHERE state = #{state} " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT 20" +
            "</script>")
    List<ProfessionDO> listByStateAndLastId(byte state, Long lastId);

    @Select("<script>" +
            "SELECT * FROM profession WHERE main_category = #{mainCategory} AND state = " + PUBLISHED_VALUE + " " +
            "<if test='lastId != null'>AND id &gt; #{lastId}</if> " +
            "ORDER BY id ASC LIMIT 20" +
            "</script>")
    List<ProfessionDO> listByMainCategoryAndLastId(int mainCategory, Long lastId);

    @Select("<script>" +
            "SELECT * FROM profession WHERE sub_category = #{subCategory} AND state = " + PUBLISHED_VALUE + " " +
            "<if test='lastId != null'>AND id &gt; #{lastId}</if> " +
            "ORDER BY id ASC LIMIT 20" +
            "</script>")
    List<ProfessionDO> listBySubCategoryAndLastId(int subCategory, Long lastId);

    @Select("<script>" +
            "SELECT * FROM profession WHERE main_category = #{mainCategory} AND sub_category = #{subCategory} AND state = " + PUBLISHED_VALUE + " " +
            "<if test='lastId != null'>AND id &gt; #{lastId}</if> " +
            "ORDER BY id ASC LIMIT 20" +
            "</script>")
    List<ProfessionDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, Long lastId);

    @Select("SELECT * FROM profession WHERE INSTR(name, #{name}) > 0 limit 20")
    List<ProfessionDO> searchByName(String name);

    @Select({"<script>SELECT * FROM profession where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<ProfessionDO> getByIds(Collection<Long> ids);

    @Select({"<script>SELECT * FROM profession where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Integer, ProfessionDO> getMapByIds(Collection<Long> ids);

    @Insert("INSERT INTO profession" +
            "(name, description, icon, skills, main_category, sub_category, state, creator_id) " +
            "VALUES (#{name}, #{description}, #{icon}, #{skills}, #{mainCategory}, #{subCategory}, " +
            "#{state}, #{creatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ProfessionDO professionDO);

    @Update("UPDATE profession " +
            "SET " +
            "name = #{name}, description = #{description}, icon = #{icon}, skills = #{skills}, " +
            "main_category = #{mainCategory}, sub_category = #{subCategory}, state = #{state}, " +
            "reason = #{reason}, creator_id = #{creatorId} " +
            "WHERE id = #{id}")
    void update(ProfessionDO professionDO);

    @Update("UPDATE profession SET state = #{state}, reason = #{reason} WHERE id = #{id}")
    int updateState(long id, byte state, String reason);

    @Delete("DELETE FROM profession WHERE id = #{id}")
    void delete(long id);

    // 平台统计相关方法
    @Select("SELECT COUNT(*) FROM profession WHERE state = " + PUBLISHED_VALUE)
    Long countActiveProfessions();
}
