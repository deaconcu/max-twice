package com.prosper.learn.content.role;

import org.apache.ibatis.annotations.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.prosper.learn.shared.domain.Enums.*;

@Mapper
public interface RoleMapper {

    @Select("SELECT * FROM role WHERE id = #{id} AND deleted_at IS NULL")
    RoleDO getById(long id);

    @Select({"<script>",
            "SELECT * FROM role WHERE deleted_at IS NULL",
            "<if test='state != null'> AND state = #{state}</if>",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            "ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<RoleDO> listByState(@Param("state") Byte state, @Param("lastId") Long lastId, @Param("limit") int limit);

    @Select("<script>" +
            "SELECT * FROM role WHERE main_category = #{mainCategory} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<RoleDO> listByMainCategoryAndLastId(int mainCategory, Long lastId, int limit);

    @Select("<script>" +
            "SELECT * FROM role WHERE sub_category = #{subCategory} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<RoleDO> listBySubCategoryAndLastId(int subCategory, Long lastId, int limit);

    @Select("<script>" +
            "SELECT * FROM role WHERE main_category = #{mainCategory} AND sub_category = #{subCategory} AND state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC LIMIT #{limit}" +
            "</script>")
    List<RoleDO> listByMainCategoryAndSubCategoryAndLastId(int mainCategory, int subCategory, Long lastId, int limit);

    @Select("SELECT * FROM role WHERE state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL AND name LIKE CONCAT('%', #{keyword}, '%') ORDER BY id DESC LIMIT 20")
    List<RoleDO> searchByKeyword(String keyword);

    /**
     * 管理后台按名称搜索角色（搜索所有状态，支持分页）
     */
    @Select("<script>" +
            "SELECT * FROM role " +
            "WHERE deleted_at IS NULL AND name LIKE CONCAT('%', #{name}, '%') " +
            "<if test='lastId != null'>AND id &lt; #{lastId}</if> " +
            "ORDER BY id DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<RoleDO> searchByName(@Param("name") String name, @Param("lastId") Long lastId, @Param("limit") int limit);

    @Select({"<script>SELECT * FROM role WHERE deleted_at IS NULL AND id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<RoleDO> getByIds(Collection<Long> ids);

    @Select({"<script>SELECT * FROM role WHERE deleted_at IS NULL AND id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Integer, RoleDO> getMapByIds(Collection<Long> ids);

    @Insert("INSERT INTO role " +
            "(name, description, icon, skills, main_category, sub_category, state, creator_id) " +
            "VALUES (#{name}, #{description}, #{icon}, #{skills}, #{mainCategory}, #{subCategory}, " +
            "#{state}, #{creatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(RoleDO roleDO);

    @Update("UPDATE role " +
            "SET " +
            "name = #{name}, description = #{description}, icon = #{icon}, skills = #{skills}, " +
            "main_category = #{mainCategory}, sub_category = #{subCategory}, state = #{state}, " +
            "reason = #{reason}, creator_id = #{creatorId} " +
            "WHERE id = #{id}")
    void update(RoleDO roleDO);

    @Update("UPDATE role SET state = #{state}, reason = #{reason} WHERE id = #{id} AND deleted_at IS NULL")
    int updateState(long id, byte state, String reason);

    @Update("UPDATE role SET deleted_at = NOW() WHERE id = #{id} AND deleted_at IS NULL")
    void delete(long id);

    // 平台统计相关方法
    @Select("SELECT COUNT(*) FROM role WHERE state = " + ContentState.PUBLISHED_VALUE + " AND deleted_at IS NULL")
    Long countActiveRoles();
}
