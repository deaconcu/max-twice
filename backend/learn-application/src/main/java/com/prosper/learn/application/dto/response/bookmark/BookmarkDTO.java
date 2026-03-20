package com.prosper.learn.application.dto.response.bookmark;

import lombok.Data;

/**
 * 收藏记录响应 DTO（带关联对象）
 *
 * @param <T> 关联对象类型
 *           - objectType=profession 时，T 为 ProfessionBriefDTO
 *           - objectType=roadmap 时，T 为 RoadmapBriefDTO
 *           - objectType=course 时，T 为 CourseBriefDTO
 *           - objectType=post 时，T 为 PostBriefDTO
 *           - objectType=memory_card 时，T 为 MemoryCardBriefDTO
 */
@Data
public class BookmarkDTO<T> {

    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 对象类型
     */
    private Integer objectType;

    /**
     * 对象ID
     */
    private Long objectId;

    /**
     * 父对象ID
     */
    private Long parentId;

    /**
     * 收藏时间
     */
    private String createdAt;

    /**
     * 关联对象（根据 objectType 决定具体类型）
     */
    private T object;
}
