package com.prosper.learn.infrastructure.image;

import com.prosper.learn.shared.domain.exception.StatusCode;
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
     * https://images.maxtwice.com/posts/abc.jpg -> posts/abc.jpg
     */
    private String extractKeyFromUrl(String url) {
        int index = url.indexOf(".com/");
        if (index != -1) {
            return url.substring(index + 5);
        }
        throw new IllegalArgumentException("Invalid image URL: " + url);
    }
}
