package com.twicemax.infrastructure.image;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图片上传数据服务
 * 负责图片上传记录的 CRUD
 *
 * 无缓存：查询不频繁，主要是写操作
 */
@Service
@RequiredArgsConstructor
public class ImageUploadDataService {

    private final ImageUploadMapper imageUploadMapper;

    // ==================== 查询方法 ====================

    /**
     * 根据URL查询图片
     */
    public ImageUploadDO getByFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        return imageUploadMapper.getByFileUrl(fileUrl);
    }

    /**
     * 根据用户ID、引用类型和状态查询
     */
    public ImageUploadDO getByUserIdAndRefTypeAndStatus(Long userId, String refType, Integer status) {
        return imageUploadMapper.getByUserIdAndRefTypeAndStatus(userId, refType, status);
    }

    /**
     * 查询未使用且超过指定时间的图片
     */
    public List<ImageUploadDO> getByStatusAndCreatedAtBefore(Integer status, LocalDateTime createdAt) {
        return imageUploadMapper.getByStatusAndCreatedAtBefore(status, createdAt);
    }

    /**
     * 查询用户上传历史
     */
    public List<ImageUploadDO> getByUserId(Long userId, int limit) {
        return imageUploadMapper.getByUserId(userId, limit);
    }

    // ==================== 写入方法 ====================

    /**
     * 插入图片记录
     */
    public void insert(ImageUploadDO imageUpload) {
        imageUploadMapper.insert(imageUpload);
    }

    /**
     * 更新图片记录
     */
    public void update(ImageUploadDO imageUpload) {
        if (imageUpload == null || imageUpload.getId() == null) {
            throw new IllegalArgumentException("ImageUpload or ID cannot be null");
        }
        imageUploadMapper.update(imageUpload);
    }

    /**
     * 删除图片记录
     */
    public void delete(Long id) {
        if (id == null) {
            return;
        }
        imageUploadMapper.delete(id);
    }
}
