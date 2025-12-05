package com.prosper.learn.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 操作日志DTO（用于记录日志和前端展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogDTO {

    /** 主键ID */
    private Long id;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 操作人角色 */
    private Integer operatorRole;

    /** 模块名称 */
    private String module;

    /** 操作类型 */
    private String operationType;

    /** 操作级别 */
    private Integer operationLevel;

    /** 目标类型 */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    /** 目标名称 */
    private String targetName;

    /** 操作原因 */
    private String reason;

    /** 额外数据 */
    private String extraData;

    /** 操作IP地址 */
    private String ipAddress;

    /** 操作时间 */
    private String createdAt;
}
