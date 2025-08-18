package com.prosper.learn.user;

import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM user WHERE id = #{userId}")
    User getUserById(@Param("userId") int userId);

    @Select("SELECT * FROM user WHERE email = #{email}")
    User getUserByEmail(@Param("email") String email);

    @Insert("INSERT INTO user(email, password, password_md5) " +
            "VALUES (#{user.email}, #{user.password}, #{user.passwordMD5})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(@Param("user") User user);
}
