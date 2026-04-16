package com.prosper.learn.shared.domain.event.user.learning;

import static com.prosper.learn.shared.domain.Enums.ContentType;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 学习完成事件
 * 当用户完成学习课程或路线图时触发
 */
@Data
@AllArgsConstructor
public class LearningCompletedEvent {

    /** 学习者ID */
    private Long userId;

    /** 学习内容ID */
    private Long contentId;

    /** 内容类型（课程、路线图等） */
    private ContentType contentType;
}