package com.twicemax.application.dto.response.userlearning;

import lombok.Data;

/**
 * 用户学习记录响应 DTO（带关联对象）
 *
 * @param <T> 关联对象类型
 *           - objectType=4 (roadmap) 时，T 为 RoadmapBriefDTO
 *           - objectType=8 (course) 时，T 为 CourseBriefDTO
 */
@Data
public class UserLearningDTO<T> {

    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 对象类型 (4=roadmap, 8=course)
     */
    private Byte objectType;

    /**
     * 对象ID
     */
    private Long objectId;

    /**
     * 进度百分比 (0-10000，精度到万分位)
     */
    private Integer progressPercent;

    /**
     * 学习状态 (0=未开始, 1=进行中, 2=已完成)
     */
    private Byte state;

    /**
     * 开始学习时间
     */
    private String startedAt;

    /**
     * 完成时间
     */
    private String completedAt;

    /**
     * 记录创建时间
     */
    private String createdAt;

    /**
     * 记录更新时间
     */
    private String updatedAt;

    /**
     * 关联对象（根据 objectType 决定具体类型）
     * - roadmap: RoadmapBriefDTO
     * - course: CourseBriefDTO
     */
    private T object;
}
