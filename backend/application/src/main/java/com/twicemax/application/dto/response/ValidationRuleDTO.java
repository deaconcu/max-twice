package com.twicemax.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证规则 DTO
 * 用于前端获取字段验证规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRuleDTO {
    /**
     * 最小长度
     */
    private Integer minLength;

    /**
     * 最大长度
     */
    private Integer maxLength;

    /**
     * 字段中文名称
     */
    private String label;
}
