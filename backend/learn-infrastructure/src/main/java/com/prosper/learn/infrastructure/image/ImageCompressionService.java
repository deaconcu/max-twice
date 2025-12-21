package com.prosper.learn.infrastructure.image;

import com.prosper.learn.shared.domain.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 图片压缩服务
 */
@Slf4j
@Service
public class ImageCompressionService {

    @Value("${upload.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${upload.compression.max-width:2048}")
    private int maxWidth;

    @Value("${upload.compression.max-height:2048}")
    private int maxHeight;

    @Value("${upload.compression.quality:0.85}")
    private double quality;

    @Value("${upload.compression.format:webp}")
    private String format;

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

        try {
            // 读取原始图片
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (originalImage == null) {
                throw ErrorCode.INVALID_IMAGE.exception("无法读取图片");
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

                log.info("裁切后尺寸: {}x{}, 大小: {} bytes",
                        targetMaxWidth, targetMaxHeight, outputStream.toByteArray().length);
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

                log.info("压缩后尺寸: {}x{}, 大小: {} bytes, 压缩率: {:.2f}%",
                        targetWidth, targetHeight, outputStream.toByteArray().length,
                        (1 - (double) outputStream.toByteArray().length / imageData.length) * 100);
            }

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("图片压缩失败", e);
            throw ErrorCode.IMAGE_COMPRESSION_FAILED.exception("图片压缩失败");
        }
    }

    /**
     * 获取压缩后的Content-Type
     */
    public String getCompressedContentType() {
        return "image/" + format;
    }
}
