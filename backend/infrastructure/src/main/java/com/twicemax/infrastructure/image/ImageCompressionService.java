package com.twicemax.infrastructure.image;

import com.twicemax.shared.domain.exception.StatusCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 图片压缩服务
 */
@Slf4j
@Service
public class ImageCompressionService {

    @Value("${app.upload.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${app.upload.compression.max-width:2048}")
    private int maxWidth;

    @Value("${app.upload.compression.max-height:2048}")
    private int maxHeight;

    @Value("${app.upload.compression.quality:0.85}")
    private double quality;

    @Value("${app.upload.compression.format:webp}")
    private String format;

    /**
     * 验证配置参数
     * 在 Bean 初始化时检查配置的合法性，避免运行时错误
     */
    @PostConstruct
    public void validateConfig() {
        // 1. 验证 quality 范围
        if (quality < 0.0 || quality > 1.0) {
            throw new IllegalStateException(
                    "upload.compression.quality 必须在 0.0-1.0 之间，当前值: " + quality);
        }

        // 2. 验证 maxWidth 和 maxHeight
        if (maxWidth <= 0) {
            throw new IllegalStateException(
                    "upload.compression.max-width 必须大于0，当前值: " + maxWidth);
        }
        if (maxHeight <= 0) {
            throw new IllegalStateException(
                    "upload.compression.max-height 必须大于0，当前值: " + maxHeight);
        }

        // 3. 验证 format
        List<String> supportedFormats = Arrays.asList("jpg", "jpeg", "png", "webp");
        if (!supportedFormats.contains(format.toLowerCase())) {
            throw new IllegalStateException(
                    "不支持的图片格式: " + format + "，支持的格式: " + supportedFormats);
        }

        log.info("图片压缩配置验证通过: enabled={}, maxWidth={}, maxHeight={}, quality={}, format={}",
                compressionEnabled, maxWidth, maxHeight, quality, format);
    }

    /**
     * 压缩图片
     *
     * @param imageData 原始图片数据
     * @param contentType 图片类型
     * @return 压缩后的图片数据
     */
    public byte[] compress(byte[] imageData, String contentType) {
        return compress(imageData, contentType, maxWidth, maxHeight);
    }

    /**
     * 压缩图片（指定尺寸）
     *
     * @param imageData 原始图片数据
     * @param contentType 图片类型
     * @param targetMaxWidth 目标最大宽度
     * @param targetMaxHeight 目标最大高度
     * @return 压缩后的图片数据
     */
    public byte[] compress(byte[] imageData, String contentType, int targetMaxWidth, int targetMaxHeight) {
        return compress(imageData, contentType, targetMaxWidth, targetMaxHeight, false);
    }

    /**
     * 压缩图片（指定尺寸，可选裁切）
     *
     * @param imageData 原始图片数据
     * @param contentType 图片类型
     * @param targetMaxWidth 目标最大宽度
     * @param targetMaxHeight 目标最大高度
     * @param crop 是否裁切（true=居中裁切，false=等比例缩放）
     * @return 压缩后的图片数据
     */
    public byte[] compress(byte[] imageData, String contentType, int targetMaxWidth, int targetMaxHeight, boolean crop) {
        if (!compressionEnabled) {
            return imageData;
        }

        BufferedImage originalImage = null;
        try {
            // 读取原始图片
            originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (originalImage == null) {
                throw StatusCode.INVALID_IMAGE.exception("无法读取图片");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            log.info("原始图片尺寸: {}x{}, 大小: {} bytes", originalWidth, originalHeight, imageData.length);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            if (crop) {
                // 裁切模式：居中裁切成正方形，然后缩放到目标尺寸
                int cropSize = Math.min(originalWidth, originalHeight);
                int x = (originalWidth - cropSize) / 2;
                int y = (originalHeight - cropSize) / 2;

                Thumbnails.of(originalImage)
                        .sourceRegion(x, y, cropSize, cropSize)  // 居中裁切
                        .size(targetMaxWidth, targetMaxHeight)    // 缩放到目标尺寸
                        .outputFormat(format)
                        .outputQuality(quality)
                        .toOutputStream(outputStream);

                // 只调用一次 toByteArray()，避免重复创建数组
                byte[] compressedData = outputStream.toByteArray();

                log.info("裁切后尺寸: {}x{}, 大小: {} bytes",
                        targetMaxWidth, targetMaxHeight, compressedData.length);

                return compressedData;
            } else {
                // 等比例缩放模式
                int targetWidth = originalWidth;
                int targetHeight = originalHeight;

                if (originalWidth > targetMaxWidth || originalHeight > targetMaxHeight) {
                    double widthRatio = (double) targetMaxWidth / originalWidth;
                    double heightRatio = (double) targetMaxHeight / originalHeight;
                    double ratio = Math.min(widthRatio, heightRatio);

                    targetWidth = (int) (originalWidth * ratio);
                    targetHeight = (int) (originalHeight * ratio);
                }

                Thumbnails.of(originalImage)
                        .size(targetWidth, targetHeight)
                        .outputFormat(format)
                        .outputQuality(quality)
                        .toOutputStream(outputStream);

                // 只调用一次 toByteArray()，避免重复创建数组
                byte[] compressedData = outputStream.toByteArray();
                double compressionRatio = (1 - (double) compressedData.length / imageData.length) * 100;

                log.info("压缩后尺寸: {}x{}, 大小: {} bytes, 压缩率: {}%",
                        targetWidth, targetHeight, compressedData.length,
                        String.format("%.2f", compressionRatio));

                return compressedData;
            }

        } catch (IOException e) {
            log.error("图片压缩失败", e);
            throw StatusCode.IMAGE_COMPRESSION_FAILED.exception("图片压缩失败");
        } finally {
            // 显式释放 BufferedImage 的图像缓冲区，帮助 GC 更快回收内存
            if (originalImage != null) {
                originalImage.flush();
            }
        }
    }

    /**
     * 获取压缩后的Content-Type
     */
    public String getCompressedContentType() {
        return "image/" + format;
    }
}
