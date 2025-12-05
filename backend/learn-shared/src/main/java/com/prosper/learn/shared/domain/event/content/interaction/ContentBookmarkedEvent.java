package com.prosper.learn.shared.domain.event.content.interaction;

import com.prosper.learn.shared.domain.Enums;
import lombok.Data;
import lombok.AllArgsConstructor;

/**
 * 内容收藏事件
 * 当用户收藏内容时触发（包括课程关注、职业关注）
 */
@Data
@AllArgsConstructor
public class ContentBookmarkedEvent {

    /** 收藏者ID */
    private Long userId;

    /** 内容ID */
    private Long contentId;

    /** 内容类型 */
    private Enums.ContentType contentType;
}