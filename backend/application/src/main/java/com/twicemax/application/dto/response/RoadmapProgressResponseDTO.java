package com.twicemax.application.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 路线图进度响应DTO
 * 用于开始/取消学习路线图的响应
 */
@Data
@Builder
public class RoadmapProgressResponseDTO {

    /**
     * 路线图ID
     */
    private Long roadmapId;

    /**
     * 是否正在学习
     */
    private Boolean learning;
}
