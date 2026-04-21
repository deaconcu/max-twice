package com.twicemax.user.auth;

import org.apache.ibatis.annotations.*;

@Mapper
public interface VerificationMapper {

    @Select("SELECT * FROM verification WHERE id = #{id}")
    VerificationDO getById(long id);

    @Select("SELECT * FROM verification WHERE email = #{email} AND type = #{type} AND used = #{used} ORDER BY id DESC LIMIT 1")
    VerificationDO getByEmailAndType(@Param("email") String email, @Param("type") byte type, @Param("used") boolean used);

    @Insert("INSERT INTO verification(email, code, type, used, created_at) VALUES (#{email}, #{code}, #{type}, #{used}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(VerificationDO verificationDO);

    @Update("UPDATE verification SET email = #{email}, code = #{code}, type = #{type}, created_at = #{createdAt}, used = #{used} " +
            "WHERE id = #{id}")
    void update(VerificationDO verificationDO);
}