package com.prosper.learn.application.service;

import com.prosper.learn.application.dto.request.MarkImageUsedRequest;
import com.prosper.learn.application.dto.response.ImageUploadResponse;
import com.prosper.learn.application.dto.response.ImageUploadHistoryDTO;
import com.prosper.learn.application.dto.response.QuotaUsageDTO;
import com.prosper.learn.infrastructure.image.ImageCompressionService;
import com.prosper.learn.infrastructure.image.ImageQuotaService;
import com.prosper.learn.infrastructure.image.ImageUploadDO;
import com.prosper.learn.infrastructure.image.ImageUploadDataService;
import com.prosper.learn.infrastructure.image.R2Service;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片上传应用服务
 * 负责图片上传的完整流程编排
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final ImageUploadDataService imageUploadDataService;
    private final ImageQuotaService imageQuotaService;
    private final ImageCompressionService imageCompressionService;
    private final R2Service r2Service;

    @Value("${upload.file.max-size:5242880}")
    private long maxFileSize; // 5MB

    @Value("${upload.file.max-dimension:4096}")
    private int maxDimension;

    @Value("${upload.file.allowed-types:image/jpeg,image/png,image/webp}")
    private String[] allowedTypes;

    /**
     * 上传图片
     * 完整流程：验证 -> 配额检查 -> 压缩 -> 上传R2 -> 保存记录
     *
     * @param file 上传的文件
     * @param userId 用户ID
     * @param refType 引用类型：post/comment/avatar/course/roadmap
     * @return 上传结果
     */
    @Transactional
    public ImageUploadResponse upload(MultipartFile file, Long userId, String refType) {
        log.info("用户{}开始上传图片，类型：{}", userId, refType);

        // 1. 验证文件
        validateFile(file);

        // 2. 检查配额
        imageQuotaService.checkQuota(userId);

        try {
            // 3. 压缩图片 - 根据类型使用不同尺寸
            byte[] originalData = file.getBytes();
            byte[] compressedData;

            if ("avatar".equals(refType)) {
                // 头像：居中裁切成 200x200 正方形
                compressedData = imageCompressionService.compress(originalData, file.getContentType(), 200, 200, true);
            } else {
                // 其他图片：等比例缩放，使用默认配置
                compressedData = imageCompressionService.compress(originalData, file.getContentType());
            }

            String contentType = imageCompressionService.getCompressedContentType();

            // 4. 上传到R2
            String prefix = getUploadPrefix(refType);
            String fileUrl = r2Service.upload(compressedData, prefix, contentType);

            // 5. 保存记录
            ImageUploadDO imageUpload = new ImageUploadDO();
            imageUpload.setUserId(userId);
            imageUpload.setFileName(file.getOriginalFilename());
            imageUpload.setFileSize((long) compressedData.length);
            imageUpload.setFileUrl(fileUrl);
            imageUpload.setRefType(refType);
            imageUpload.setCreatedAt(LocalDateTime.now());

            // avatar/course/roadmap 立即标记为使用中，post/comment 需要等保存时标记
            if (isImmediateUse(refType)) {
                imageUpload.setStatus(1);
                imageUpload.setUsedAt(LocalDateTime.now());

                // 删除用户该类型的旧图片
                deleteOldImage(userId, refType);
            } else {
                imageUpload.setStatus(0);
            }

            imageUploadDataService.insert(imageUpload);

            // 6. 记录配额使用
            imageQuotaService.recordUpload(userId);

            log.info("图片上传成功: userId={}, fileUrl={}, size={} bytes", userId, fileUrl, compressedData.length);

            return new ImageUploadResponse(fileUrl);

        } catch (Exception e) {
            log.error("图片上传失败", e);
            throw StatusCode.FILE_UPLOAD_FAILED.exception("图片上传失败：" + e.getMessage());
        }
    }

    /**
     * 标记图片为使用中
     * 当文章/评论保存时调用
     *
     * @param request 标记请求
     */
    @Transactional
    public void markAsUsed(MarkImageUsedRequest request) {
        for (String fileUrl : request.getFileUrls()) {
            ImageUploadDO imageUpload = imageUploadDataService.getByFileUrl(fileUrl);
            if (imageUpload != null && imageUpload.getStatus() == 0) {
                imageUpload.setStatus(1);
                imageUpload.setRefType(request.getRefType());
                imageUpload.setRefId(request.getRefId());
                imageUpload.setUsedAt(LocalDateTime.now());
                imageUploadDataService.update(imageUpload);
                log.info("标记图片为使用中: {}", fileUrl);
            }
        }
    }

    /**
     * 删除图片
     * 物理删除：删除R2文件和数据库记录
     *
     * @param fileUrl 文件URL
     * @param userId 用户ID
     */
    @Transactional
    public void delete(String fileUrl, Long userId) {
        ImageUploadDO imageUpload = imageUploadDataService.getByFileUrl(fileUrl);
        if (imageUpload == null) {
            throw StatusCode.NOT_FOUND.exception("图片不存在");
        }

        if (!imageUpload.getUserId().equals(userId)) {
            throw StatusCode.PERMISSION_DENIED.exception("无权删除该图片");
        }

        // 删除R2文件
        r2Service.delete(fileUrl);

        // 删除数据库记录
        imageUploadDataService.delete(imageUpload.getId());

        log.info("图片删除成功: {}", fileUrl);
    }

    /**
     * 清理未使用的图片
     * 定时任务调用，删除24小时前上传但未使用的图片
     */
    @Transactional
    public void cleanupUnusedImages() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<ImageUploadDO> unusedImages = imageUploadDataService.getByStatusAndCreatedAtBefore(0, cutoffTime);

        log.info("开始清理未使用图片，找到{}张", unusedImages.size());

        for (ImageUploadDO image : unusedImages) {
            try {
                r2Service.delete(image.getFileUrl());
                imageUploadDataService.delete(image.getId());
                log.info("清理未使用图片: {}", image.getFileUrl());
            } catch (Exception e) {
                log.error("清理图片失败: {}", image.getFileUrl(), e);
            }
        }

        log.info("清理完成，共清理{}张图片", unusedImages.size());
    }

    /**
     * 获取用户配额使用情况
     */
    public QuotaUsageDTO getQuotaUsage(Long userId) {
        ImageQuotaService.QuotaUsage usage = imageQuotaService.getQuotaUsage(userId);
        return new QuotaUsageDTO(
                usage.minuteUsed, usage.minuteLimit,
                usage.hourUsed, usage.hourLimit,
                usage.dailyUsed, usage.dailyLimit
        );
    }

    /**
     * 获取用户上传历史
     */
    public List<ImageUploadHistoryDTO> getUploadHistory(Long userId, int limit) {
        List<ImageUploadDO> history = imageUploadDataService.getByUserId(userId, limit);
        return history.stream()
                .map(h -> new ImageUploadHistoryDTO(
                        h.getId(),
                        h.getFileUrl(),
                        h.getFileName(),
                        h.getFileSize(),
                        h.getRefType(),
                        h.getRefId(),
                        h.getStatus(),
                        h.getCreatedAt(),
                        h.getUsedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw StatusCode.INVALID_PARAMETER.exception("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > maxFileSize) {
            throw StatusCode.FILE_TOO_LARGE.exception("文件大小不能超过" + (maxFileSize / 1024 / 1024) + "MB");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(allowedTypes).contains(contentType)) {
            throw StatusCode.FILE_TYPE_NOT_ALLOWED.exception("只支持JPG、PNG、WebP格式");
        }
    }

    /**
     * 获取上传前缀
     */
    private String getUploadPrefix(String refType) {
        switch (refType) {
            case "post":
                return "posts";
            case "comment":
                return "comments";
            case "avatar":
                return "avatars";
            case "course":
                return "courses";
            case "roadmap":
                return "roadmaps";
            default:
                throw StatusCode.INVALID_PARAMETER.exception("不支持的引用类型：" + refType);
        }
    }

    /**
     * 判断是否立即使用
     * avatar/course/roadmap 立即使用，post/comment 需要等保存时标记
     */
    private boolean isImmediateUse(String refType) {
        return "avatar".equals(refType) || "course".equals(refType) || "roadmap".equals(refType);
    }

    /**
     * 删除旧图片
     * 用于avatar/course/roadmap替换场景
     */
    private void deleteOldImage(Long userId, String refType) {
        ImageUploadDO oldImage = imageUploadDataService.getByUserIdAndRefTypeAndStatus(userId, refType, 1);
        if (oldImage != null) {
            try {
                r2Service.delete(oldImage.getFileUrl());
                imageUploadDataService.delete(oldImage.getId());
                log.info("删除旧图片: {}", oldImage.getFileUrl());
            } catch (Exception e) {
                log.error("删除旧图片失败: {}", oldImage.getFileUrl(), e);
            }
        }
    }
}
