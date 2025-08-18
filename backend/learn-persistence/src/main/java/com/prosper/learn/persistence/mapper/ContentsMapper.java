package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.ContentsDO;
import org.apache.ibatis.annotations.*;

public interface ContentsMapper {

    @Select("SELECT * FROM contents WHERE id = #{id}")
    ContentsDO getById(@Param("id") int id);

    @Select("SELECT * FROM contents WHERE userId = #{userId} limit 1")
    ContentsDO getByUser(@Param("userId") int userId);

    @Insert("INSERT INTO contents(userId, contents, ctime, utime) " +
        "VALUES (#{userId}, #{contents}, #{cTime}, #{uTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ContentsDO contentsDO);

    @Update("UPDATE contents SET contents = #{contents} where userId = #{userId}")
    void update(ContentsDO contentsDO);
}
