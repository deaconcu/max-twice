package com.prosper.learn.shared.infrastructure.config;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SystemMapper {

    @Select("SELECT * FROM `system` WHERE `key` = #{key} LIMIT 1" )
    SystemDO getByKey(String key);

    @Select("SELECT COUNT(*) FROM `system` WHERE `key` = #{key}")
    int existsByKey(String key);

    @Select("SELECT * FROM `system`")
    List<SystemDO> getAll();

    @Insert("INSERT INTO `system` (`key`, `value`) VALUES (#{key}, #{value})")
    void insert(SystemDO systemDO);

    @Update("UPDATE `system` SET `value` = #{value} WHERE `key` = #{key}")
    void updateByKey(SystemDO systemDO);

    @Delete("DELETE FROM `system` WHERE `key` = #{key}")
    void deleteByKey(String key);
}
