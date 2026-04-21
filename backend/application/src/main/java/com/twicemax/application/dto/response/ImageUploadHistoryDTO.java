package com.twicemax.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图片上传历史DTO
 */
@Data
@AllArgsConstructor
public class ImageUploadHistoryDTO {

    /**
     * 图片ID
     */
    private Long id;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 引用类型
     */
    private String refType;

    /**
     * 引用ID
     */
    private Long refId;

    /**
     * 状态：0-未使用，1-使用中
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 首次被引用时间
     */
    private LocalDateTime usedAt;
}
