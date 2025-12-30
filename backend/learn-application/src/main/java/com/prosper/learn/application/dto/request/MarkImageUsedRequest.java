package com.prosper.learn.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 标记图片为使用中请求
 */
@Data
public class MarkImageUsedRequest {

    /**
     * 图片URL列表
     */
    @NotEmpty(message = "图片URL列表不能为空")
    private List<String> fileUrls;

    /**
     * 引用类型（post/comment等）
     */
    @NotNull(message = "引用类型不能为空")
    private String refType;

    /**
     * 引用的资源ID（文章ID、评论ID等）
     */
    @NotNull(message = "引用资源ID不能为空")
    private Long refId;
}
