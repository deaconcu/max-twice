package com.prosper.learn.application.dto.response;

import com.prosper.learn.application.dto.response.card.CardWithSrsDTO;
import lombok.Data;

/**
 * 复习提交结果DTO
 *
 * 包含下一张待复习卡片和当前课程卡片统计
 */
@Data
public class ReviewSubmitResultDTO {

    /**
     * 下一张待复习卡片（无卡片时为 null）
     */
    private CardWithSrsDTO nextCard;

    /**
     * 当前课程卡片统计（提交后实时更新）
     */
    private CourseMemoryBankDTO courseStats;

    public static ReviewSubmitResultDTO of(CardWithSrsDTO nextCard) {
        ReviewSubmitResultDTO dto = new ReviewSubmitResultDTO();
        dto.setNextCard(nextCard);
        return dto;
    }

    public static ReviewSubmitResultDTO empty() {
        ReviewSubmitResultDTO dto = new ReviewSubmitResultDTO();
        dto.setNextCard(null);
        return dto;
    }
}
