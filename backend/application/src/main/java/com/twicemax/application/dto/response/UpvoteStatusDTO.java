package com.twicemax.application.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 用户点赞状态响应DTO
 *
 * 用于返回用户对特定内容的点赞状态和点赞数量
 * 由 UpvoteDomainService 负责提供数据
 */
@Data
@Builder
public class UpvoteStatusDTO {

    /**
     * 是否已twice点赞
     */
    private Boolean twiced;

    /**
     * 是否已like点赞
     */
    private Boolean liked;

    /**
     * twice点赞总数
     */
    private Integer twiceCount;

    /**
     * like点赞总数
     */
    private Integer likeCount;
}