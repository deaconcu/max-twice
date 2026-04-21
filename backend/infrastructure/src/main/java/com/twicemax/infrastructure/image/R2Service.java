package com.twicemax.infrastructure.image;

import com.twicemax.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.UUID;

/**
 * Cloudflare R2 存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class R2Service {

    private final R2Config r2Config;

    /**
     * 上传文件到R2
     *
     * @param data 文件数据
     * @param prefix 文件前缀目录（如 posts, avatars, courses）
     * @param contentType 文件类型
     * @return 公开访问URL
     */
    public String upload(byte[] data, String prefix, String contentType) {
        try {
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + ".webp";
            String key = prefix + "/" + fileName;

            S3Client s3Client = r2Config.getS3Client();
            String bucketName = r2Config.getBucketName();

            // 上传到R2，设置缓存策略
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .cacheControl("public, max-age=31536000, immutable")  // 缓存1年，不可变
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(data));

            // 返回公开URL
            String publicUrl = r2Config.getPublicDomain() + "/" + key;

            log.info("文件上传成功: {}, 大小: {} bytes", publicUrl, data.length);

            return publicUrl;

        } catch (S3Exception e) {
            log.error("R2上传失败", e);
            throw StatusCode.FILE_UPLOAD_FAILED.exception("上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文件上传异常", e);
            throw StatusCode.FILE_UPLOAD_FAILED.exception("上传失败");
        }
    }

    /**
     * 删除文件
     *
     * @param fileUrl 文件URL
     */
    public void delete(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            S3Client s3Client = r2Config.getS3Client();
            String bucketName = r2Config.getBucketName();

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);

            log.info("文件删除成功: {}", fileUrl);

        } catch (S3Exception e) {
            log.error("R2删除失败: {}", fileUrl, e);
            throw StatusCode.FILE_DELETE_FAILED.exception("删除失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文件删除异常: {}", fileUrl, e);
            throw StatusCode.FILE_DELETE_FAILED.exception("删除失败");
        }
    }

    /**
     * 从URL提取R2的key
     *
     * 验证流程：
     * 1. 验证 URL 不为空
     * 2. 验证 URL 来自配置的公开域名
     * 3. 提取文件路径
     * 4. 验证路径不包含危险字符（路径遍历）
     *
     * @param url 完整的文件URL，例如: https://images.maxtwice.com/posts/abc.jpg
     * @return R2的key，例如: posts/abc.jpg
     */
    private String extractKeyFromUrl(String url) {
        // 1. 验证 URL 不为空
        if (url == null || url.trim().isEmpty()) {
            throw StatusCode.INVALID_PARAMETER.exception("图片URL不能为空");
        }

        // 2. 验证 URL 必须来自配置的公开域名
        String expectedPrefix = r2Config.getPublicDomain() + "/";
        if (!url.startsWith(expectedPrefix)) {
            log.warn("URL域名不匹配，预期: {}, 实际: {}", expectedPrefix, url);
            throw StatusCode.INVALID_PARAMETER.exception("无效的图片URL");
        }

        // 3. 提取 key
        String key = url.substring(expectedPrefix.length());

        // 4. 验证 key 不包含路径遍历字符
        if (key.contains("..") || key.contains("//") || key.startsWith("/")) {
            log.error("检测到路径遍历攻击尝试: {}", url);
            throw StatusCode.INVALID_PARAMETER.exception("无效的文件路径");
        }

        // 5. 验证 key 不为空
        if (key.isEmpty()) {
            throw StatusCode.INVALID_PARAMETER.exception("文件路径不能为空");
        }

        return key;
    }
}
