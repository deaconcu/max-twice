package com.prosper.learn.user.profile;

import com.prosper.learn.shared.domain.Enums;
import lombok.Data;

import java.time.LocalDateTime;

import static com.prosper.learn.shared.domain.Enums.*;

@Data
public class UserDO {

    private Long id;

    private String password;

    private String email;

    private String phone;

    private String name;

    private Boolean emailValidated;

    private String biography;

    private LocalDateTime msgReadTime;

    private Byte state;

    private Integer role;  // 角色代码字段

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ========== 角色相关便捷方法 ==========

    /**
     * 获取用户角色枚举
     */
    public UserRole getRoleEnum() {
        return UserRole.fromCode(this.role);
    }

    /**
     * 设置用户角色
     */
    public void setRoleEnum(UserRole role) {
        this.role = role != null ? role.getCode() : UserRole.USER.getCode();
    }

    /**
     * 判断是否等于或高于指定角色
     */
    public boolean hasRole(UserRole role) {
        return getRoleEnum().equalOrHigher(role);
    }
}
