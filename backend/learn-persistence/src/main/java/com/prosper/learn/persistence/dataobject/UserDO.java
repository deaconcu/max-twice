package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.prosper.learn.common.Enums;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class UserDO {

    @TableId(value = "id", type = IdType.AUTO)
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ========== 角色相关便捷方法 ==========

    /**
     * 获取用户角色枚举
     */
    public Enums.UserRole getRoleEnum() {
        return Enums.UserRole.fromCode(this.role);
    }

    /**
     * 设置用户角色
     */
    public void setRoleEnum(Enums.UserRole role) {
        this.role = role != null ? role.getCode() : Enums.UserRole.USER.getCode();
    }

    /**
     * 判断是否等于或高于指定角色
     */
    public boolean hasRole(Enums.UserRole role) {
        return getRoleEnum().equalOrHigher(role);
    }
}
