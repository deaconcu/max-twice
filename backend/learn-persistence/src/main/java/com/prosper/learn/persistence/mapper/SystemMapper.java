package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.SystemDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SystemMapper {

    @Select("SELECT * FROM `system` WHERE id = #{id}")
    SystemDO get(long id);

    @Update("UPDATE `system` SET config = #{config} where id = #{id}")
    void update(SystemDO systemDO);
}
