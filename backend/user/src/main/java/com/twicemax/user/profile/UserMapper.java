package com.twicemax.user.profile;

import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.twicemax.shared.domain.Enums.UserState.ACTIVE_VALUE;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    UserDO getById(long id);

    @Select("SELECT * FROM user WHERE INSTR(name, #{name}) > 0 AND state = " + ACTIVE_VALUE + " LIMIT 20")
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

    @Select("SELECT * FROM user WHERE name = #{name} limit 1")
    UserDO getByName(String name);

    @Insert("INSERT INTO user(name, password, phone, email, email_validated, biography, avatar, role, state) " +
            "VALUES (#{name}, #{password}, #{phone}, #{email}, #{emailValidated}, #{biography}, #{avatar}, #{role}, #{state})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserDO user);

    // 只更新基本信息字段，不更新敏感字段(password, email, email_validated, state, role)
    @Update("UPDATE user SET name = #{name}, phone = #{phone}, biography = #{biography}, " +
            "avatar = #{avatar}, timezone = #{timezone}, updated_at = #{updatedAt} WHERE id = #{id}")
    void update(UserDO user);

    @Update("UPDATE user SET avatar = #{avatar} WHERE id = #{userId}")
    int updateAvatar(@Param("userId") long userId, @Param("avatar") String avatar);

    // 敏感字段的专用更新方法
    @Update("UPDATE user SET state = #{state}, updated_at = #{updatedAt} WHERE id = #{userId}")
    void updateState(@Param("userId") long userId, @Param("state") byte state, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE user SET role = #{role}, updated_at = #{updatedAt} WHERE id = #{userId}")
    void updateRole(@Param("userId") long userId, @Param("role") int role, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE user SET email_validated = #{emailValidated}, updated_at = #{updatedAt} WHERE id = #{userId}")
    void updateEmailValidated(@Param("userId") long userId, @Param("emailValidated") boolean emailValidated, @Param("updatedAt") LocalDateTime updatedAt);

    @Select("SELECT * FROM user ORDER BY id DESC LIMIT #{count}")
    List<UserDO> getList(int count);

    @Select("SELECT * FROM user WHERE id < #{offsetId} ORDER BY id DESC LIMIT #{count}")
    List<UserDO> getListPaginated(long offsetId, int count);

    @Select({"<script>",
            "SELECT * FROM user WHERE 1=1",
            "<if test='state != null'> AND state = #{state}</if>",
            "<if test='lastId != null'> AND id &lt; #{lastId}</if>",
            "ORDER BY id DESC LIMIT #{limit}",
            "</script>"})
    List<UserDO> listByState(@Param("state") Byte state, @Param("lastId") Long lastId, @Param("limit") int limit);
}
