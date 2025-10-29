package com.prosper.learn.dto.request;

import lombok.Data;

/**
 * 操作日志查询DTO（用于查询条件）
 */
@Data
public class OperationLogRequest {

    /** 操作人ID */
    private Long operatorId;

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

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 最后一条记录的ID（keyset分页） */
    private Long lastId;

    /** 每页数量 */
    private Integer limit;
}
