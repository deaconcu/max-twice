package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.ChosenDO;
import org.apache.ibatis.annotations.*;

public interface ChosenMapper {

    @Select("SELECT * FROM chosen where userId = #{userId} and nodeId = #{postingId}")
    ChosenDO get(int userId, int postingId);

    @Insert("INSERT INTO chosen(userId, nodeId, postingId) VALUES (#{userId}, #{nodeId}, #{postingId})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ChosenDO chosenDO);

    @Update("UPDATE chosen SET userId = #{userId}, nodeId = #{nodeId}, postingId = #{postingId} where id = #{id}")
    void update(ChosenDO chosenDO);

    @Delete("DELETE FROM chosen where id = #{id}")
    void delete(int id);
}
