package com.twicemax.application.dto.request;

import lombok.Data;

/**
 * 操作日志查询DTO（用于查询条件）
 *
 * 支持4种查询模式：
 * 1. 时间浏览：按时间倒序浏览，可选截止时间
 * 2. 按类型：targetType + 截止时间
 * 3. 按操作人：operatorId + 截止时间
 * 4. 按对象：targetType + targetId
 */
@Data
public class OperationLogRequest {

    /** 操作人ID */
    private Long operatorId;

    /** 目标类型 */
    private String targetType;

    /** 目标ID */
    private Long targetId;

    /** 截止时间（查询此时间之前的记录） */
    private String endTime;

    /** 最后一条记录的ID（keyset分页） */
    private Long lastId;

    /** 每页数量 */
    private Integer limit;
}
