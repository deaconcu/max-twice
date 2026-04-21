package com.twicemax.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图片上传响应
 */
@Data
@AllArgsConstructor
public class ImageUploadResponse {

    /**
     * 图片URL
     */
    private String fileUrl;
}
