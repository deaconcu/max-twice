package com.prosper.learn.application.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 审批操作响应DTO
 */
@Data
@Builder
@Deprecated
public class ApprovalResponseDTO {
    
    /**
     * 操作结果
     */
    private Boolean success;
    
    /**
     * 操作消息
     */
    private String message;
    
    /**
     * 对象ID（课程ID或职业ID）
     */
    private Long objectId;
    
    /**
     * 对象类型（course/profession）
     */
    private String objectType;
    
    /**
     * 操作类型（approve/reject/delete）
     */
    private String action;
}