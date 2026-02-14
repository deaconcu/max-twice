package com.prosper.learn.application.dto.response;

import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import lombok.Data;

/**
 * 复习提交结果DTO
 *
 * 包含下一张待复习卡片和队列信息
 */
@Data
public class ReviewSubmitResultDTO {

    /**
     * 下一张待复习卡片（队列为空时为 null）
     */
    private CardWithSrsDTO nextCard;

    /**
     * 队列剩余卡片数量
     */
    private Integer queueSize;

    /**
     * 当前位置（从1开始，用于显示进度）
     */
    private Integer position;

    public static ReviewSubmitResultDTO of(CardWithSrsDTO nextCard, int queueSize, int position) {
        ReviewSubmitResultDTO dto = new ReviewSubmitResultDTO();
        dto.setNextCard(nextCard);
        dto.setQueueSize(queueSize);
        dto.setPosition(position);
        return dto;
    }

    public static ReviewSubmitResultDTO empty() {
        ReviewSubmitResultDTO dto = new ReviewSubmitResultDTO();
        dto.setNextCard(null);
        dto.setQueueSize(0);
        dto.setPosition(0);
        return dto;
    }
}
