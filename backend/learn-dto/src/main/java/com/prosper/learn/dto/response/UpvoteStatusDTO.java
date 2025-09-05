package com.prosper.learn.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 点赞状态响应DTO
 */
@Data
@Builder
public class UpvoteStatusDTO {
    
    /**
     * 对象ID
     */
    private Long objectId;
    
    /**
     * 对象类型（1=post, 2=comment）
     */
    private Integer objectType;
    
    /**
     * 总赞数
     */
    private Integer upvotes;
    
    /**
     * 是否已点赞
     */
    private Boolean upvoted;
    
    /**
     * twice类型点赞数
     */
    private Integer twiceUpvotes;
    
    /**
     * 是否已twice点赞
     */
    private Boolean twiceUpvoted;
    
    /**
     * helpful类型点赞数
     */
    private Integer helpfulUpvotes;
    
    /**
     * 是否已helpful点赞
     */
    private Boolean helpfulUpvoted;
}