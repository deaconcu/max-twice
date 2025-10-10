package com.prosper.learn.persistence.mapper;

import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.persistence.dataobject.UserDO;
import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    UserDO getById(long id);

    @Select("SELECT * FROM user WHERE INSTR(name, #{name}) > 0 limit 20")
    List<UserDO> searchByName(String name);

    @Select({"<script>SELECT * FROM user where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    List<UserDO> getByIds(Collection<Long> ids);

    @Select({"<script>SELECT * FROM user where id in " +
            "<foreach item='id' collection='ids' open='(' separator=', ' close=')'>#{id}</foreach>" +
            "</script>"})
    @MapKey("id")
    Map<Long, UserDO> getMapByIds(Collection<Long> ids);

    @Select("SELECT * FROM user WHERE email = #{email} limit 1")
    UserDO getByEmail(String email);

    @Insert("INSERT INTO user(name, password, phone, email, email_validated, biography) " +
            "VALUES (#{name}, #{password}, #{phone}, #{email}, #{emailValidated}, #{biography}")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserDO user);

    @Update("UPDATE user SET name = #{name}, password = #{password}, phone = #{phone}, email = #{email}, " +
            "email_validated = #{emailValidated}, biography = #{biography}, updated_at = #{updatedAt}, msg_read_time = #{msgReadTime} where id = #{id}")
    void update(UserDO user);
}
