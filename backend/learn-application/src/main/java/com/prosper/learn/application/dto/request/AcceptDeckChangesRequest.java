package com.prosper.learn.application.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

/**
 * 接受卡片组更新请求DTO
 */
@Data
public class AcceptDeckChangesRequest {

    /**
     * 要接受更新的卡片ID列表（空表示接受所有）
     */
    private List<Long> cardIds;

    /**
     * 当前浏览的课程ID（可选，用于创建 user_card_in_course 记录）
     */
    @Positive(message = "课程ID必须大于0")
    private Long courseId;

    /**
     * 是否删除该节点下来自其他卡片组的卡片（完全同步模式）
     */
    private Boolean removeOtherDeckCards;

}
