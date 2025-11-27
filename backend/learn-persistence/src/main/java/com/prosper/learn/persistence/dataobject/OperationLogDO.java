package com.prosper.learn.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志数据对象
 */
@Data
public class OperationLogDO {

    /** 主键ID */
    private Long id;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人名称（冗余字段，避免用户改名后无法追溯） */
    private String operatorName;

    /** 操作人角色（0=普通用户, 1=审核员, 2=管理员, 3=超级管理员） */
    private Integer operatorRole;

    /** 模块名称（用户管理、内容管理、系统配置等） */
    private String module;

    /** 操作类型（封禁用户、删除帖子、审核通过等） */
    private String operationType;

    /** 操作级别（1=低, 2=中, 3=高） */
    private Integer operationLevel;

    /** 目标类型（User, Post, Course, Comment, SystemConfig等） */
    private String targetType;

    /** 目标ID（SystemConfig类型时为0） */
    private Long targetId;

    /** 目标名称（冗余字段，便于查看） */
    private String targetName;

    /** 操作原因（如拒绝理由、屏蔽原因、封禁原因） */
    private String reason;

    /** 额外数据（如修改前后的值、详细参数等） */
    private String extraData;

    /** 操作IP地址 */
    private String ipAddress;

    /** 操作时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
