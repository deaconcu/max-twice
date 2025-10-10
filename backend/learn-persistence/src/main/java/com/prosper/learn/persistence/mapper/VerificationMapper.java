package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.UserDO;
import com.prosper.learn.persistence.dataobject.VerificationDO;
import org.apache.ibatis.annotations.*;

public interface VerificationMapper {

    @Select("SELECT * FROM verification WHERE id = #{id}")
    VerificationDO getById(long id);

    @Select("SELECT * FROM verification WHERE email = #{email} and used = #{used} order by id desc limit 1")
    VerificationDO getByEmail(String email, boolean used);

    @Insert("INSERT INTO verification(email, code, used) VALUES (#{email}, #{code}, #{used})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(VerificationDO verificationDO);

    @Update("UPDATE verification SET email = #{email}, code = #{code}, created_at = #{createdAt}, used = #{used} " +
            "where id = #{id}")
    void update(VerificationDO verificationDO);
}