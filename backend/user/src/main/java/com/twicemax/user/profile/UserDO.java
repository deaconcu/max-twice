package com.twicemax.user.profile;

import com.twicemax.shared.domain.Enums;
import lombok.Data;

import java.time.LocalDateTime;

import static com.twicemax.shared.domain.Enums.*;

@Data
public class UserDO {

    private Long id;

    private String password;

    private String email;

    private String phone;

    private String name;

    private Boolean emailValidated;

    private String biography;

    private String avatar;

    private Byte state;

    private Integer role;  // 角色代码字段

    /**
     * 用户时区
     * 注册时由前端传入，用户可修改
     * 例如：Asia/Shanghai, America/New_York
     */
    private String timezone;

    /**
     * 用户偏好语言，决定 UI 语言以及内容库路由（zh → twicemax_zh，en → twicemax_en）。
     * 注册时根据 Accept-Language 决定初始值（zh* → zh，其余 → en），用户可在 UI 切换。
     */
    private String locale;

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
