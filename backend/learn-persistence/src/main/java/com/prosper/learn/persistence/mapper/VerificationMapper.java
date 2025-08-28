package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.dataobject.VerificationDO;
import org.apache.ibatis.annotations.*;

public interface VerificationMapper {

    @Select("SELECT * FROM verification WHERE id = #{id}")
    VerificationDO getById(@Param("id") int id);

    @Select("SELECT * FROM verification WHERE email = #{email} and used = #{used} order by id desc limit 1")
    VerificationDO getByEmail(@Param("email") String email, @Param("used") boolean used);

    @Insert("INSERT INTO verification(email, code, c_time, used) VALUES (#{email}, #{code}, #{cTime}, #{used})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(VerificationDO verificationDO);

    @Update("UPDATE verification SET email = #{email}, code = #{code}, c_time = #{cTime}, used = #{used} " +
            "where id = #{id}")
    void update(VerificationDO verificationDO);
}
