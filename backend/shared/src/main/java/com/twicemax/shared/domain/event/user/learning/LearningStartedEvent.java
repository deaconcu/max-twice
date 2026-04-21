package com.twicemax.shared.domain.event.user.learning;

import static com.twicemax.shared.domain.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 学习开始事件
 * 当用户开始学习课程或路线图时触发
 */
@Data
@AllArgsConstructor
public class LearningStartedEvent {

    /** 学习者ID */
    private Long userId;

    /** 学习内容ID */
    private Long contentId;

    /** 内容类型（课程、路线图等） */
    private ContentType contentType;
}