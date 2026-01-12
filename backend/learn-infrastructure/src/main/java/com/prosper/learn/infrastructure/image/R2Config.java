package com.prosper.learn.infrastructure.image;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Cloudflare R2 配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class R2Config {

    @Value("${cloudflare.r2.account-id}")
    private String accountId;

    @Value("${cloudflare.r2.access-key-id}")
    private String accessKeyId;

    @Value("${cloudflare.r2.secret-access-key}")
    private String secretAccessKey;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Value("${cloudflare.r2.public-domain}")
    private String publicDomain;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", accountId);

        // 仅在 DEBUG 级别打印配置信息，且脱敏处理
        if (log.isDebugEnabled()) {
            log.debug("=== R2 Configuration ===");
            log.debug("Account ID: {}***", accountId.substring(0, Math.min(5, accountId.length())));
            log.debug("Access Key ID: {}***", accessKeyId.substring(0, Math.min(5, accessKeyId.length())));
            log.debug("Bucket Name: {}", bucketName);
            log.debug("Public Domain: {}", publicDomain);
            log.debug("Endpoint: {}", endpoint);
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)  // R2 doesn't use regions, but SDK requires one
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)  // 使用路径样式访问，R2 要求
                .build();

        log.info("R2 client initialized successfully");
    }

    public S3Client getS3Client() {
        return s3Client;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getPublicDomain() {
        return publicDomain;
    }
}
