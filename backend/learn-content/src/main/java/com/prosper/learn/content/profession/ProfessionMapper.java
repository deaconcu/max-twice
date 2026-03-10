package com.prosper.learn.content.profession;

import com.prosper.learn.shared.domain.Enums;
import org.apache.ibatis.annotations.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.prosper.learn.shared.domain.Enums.*;

@Mapper
public interface ProfessionMapper {

    @Select("SELECT * FROM profession WHERE id = #{id} AND deleted_at IS NULL")
    ProfessionDO getById(long id);

    @Select({"<script>",
            "SELECT * FROM profession WHERE deleted_at IS NULL",
            "<if test='state != null'> AND state = #{state}</if>",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            "ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<ProfessionDO> listByState(@Param("state") Byte state, @Param("lastId") Long lastId, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT * FROM profession WHERE main_category = #{mainCategory} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<ProfessionDO> listByMainCategoryAndLastId(int mainCategory, Long lastId, int limit);

    @Select("<script>" +
            "SELECT * FROM profession WHERE sub_category = #{subCategory} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<ProfessionDO> listBySubCategoryAndLastId(int subCategory, Long lastId, int limit);

    @Select("<script>" +
            "SELECT * FROM profession WHERE main_category = #{mainCategory} AND sub_category = #{subCategory} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<ProfessionDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit);

    @Select("SELECT * FROM profession WHERE state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL AND name LIKE CONCAT('%', #{keyword}, '%') ORDER BY id DESC LIMIT 20")
    List<ProfessionDO> searchByKeyword(String keyword);

    /**
     * 管理后台按名称搜索职业（搜索所有状态，支持分页）
     */
    @Select("<script>" +
            "SELECT * FROM profession " +
            "WHERE deleted_at IS NULL AND name LIKE CONCAT('%', #{name}, '%') " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<ProfessionDO> searchByName(@Param("name") String name, @Param("lastId") Long lastId, @Param("limit") int limit);

    @Select({"<script>SELECT * FROM profession WHERE deleted_at IS NULL AND id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<ProfessionDO> getByIds(Collection<Long> ids);

    @Select({"<script>SELECT * FROM profession WHERE deleted_at IS NULL AND id in " +
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

    @Update("UPDATE profession SET state = #{state}, reason = #{reason} WHERE id = #{id} AND deleted_at IS NULL")
    int updateState(long id, byte state, String reason);

    @Update("UPDATE profession SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    void delete(long id);

    // 平台统计相关方法
    @Select("SELECT COUNT(*) FROM profession WHERE state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL")
    Long countActiveProfessions();
}
