package com.prosper.learn.infrastructure.image;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图片上传记录实体
 */
@Data
public class ImageUploadDO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 引用类型：post/comment/avatar/course/roadmap
     */
    private String refType;

    /**
     * 引用的资源ID
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
