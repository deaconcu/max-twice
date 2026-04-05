package com.prosper.learn.interaction.bookmark;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BookmarkMapper {

    @Select("SELECT * FROM bookmark WHERE user_id = #{userId} AND object_id = #{objectId} AND object_type = #{objectType}")
    BookmarkDO getByUserAndObject(@Param("userId") long userId, @Param("objectId") long objectId, @Param("objectType") int objectType);

    @Select({
        "<script>",
        "SELECT object_id FROM bookmark WHERE user_id = #{userId} AND object_type = #{objectType} AND object_id IN ",
        "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    List<Long> getBookmarkedIds(@Param("userId") long userId, @Param("ids") List<Long> ids, @Param("objectType") int objectType);

    @Insert("INSERT INTO bookmark(user_id, object_id, object_type, parent_id, created_at) " +
            "VALUES (#{userId}, #{objectId}, #{objectType}, #{parentId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(BookmarkDO bookmark);

    @Delete("DELETE FROM bookmark WHERE id = #{id}")
    void delete(long id);

    @Select({
            "<script>",
            "SELECT * FROM bookmark WHERE user_id = #{userId} AND object_type = #{objectType}",
            "<if test='lastId != null and lastId > 0'> AND id &lt; #{lastId}</if>",
            " ORDER BY id DESC LIMIT #{limit}",
            "</script>"
    })
    List<BookmarkDO> listByUserAndLastId(@Param("userId") long userId, @Param("objectType") int objectType,
                                         @Param("lastId") Long lastId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM bookmark WHERE user_id = #{userId} AND object_type = #{objectType}")
    int countByUser(@Param("userId") long userId, @Param("objectType") int objectType);

    @Select("SELECT EXISTS(SELECT 1 FROM bookmark WHERE user_id = #{userId} AND object_id = #{objectId} AND object_type = #{objectType})")
    boolean isBookmarked(@Param("userId") long userId, @Param("objectId") long objectId, @Param("objectType") int objectType);
}
