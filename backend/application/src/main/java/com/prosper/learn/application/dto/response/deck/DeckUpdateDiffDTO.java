package com.prosper.learn.application.dto.response.deck;

import com.prosper.learn.application.dto.response.card.CardDiffDTO;
import lombok.Data;

import java.util.List;

/**
 * 卡片组更新对比信息响应DTO
 */
@Data
public class DeckUpdateDiffDTO {

    private Long deckId;

    private DescriptionChange description;

    private List<CardDiffDTO> cardDiffs;

    private Summary summary;

    @Data
    public static class DescriptionChange {
        private String old;
        private String newValue;
    }

    @Data
    public static class Summary {
        private Integer addedCount;
        private Integer modifiedCount;
        private Integer deletedCount;
    }

}