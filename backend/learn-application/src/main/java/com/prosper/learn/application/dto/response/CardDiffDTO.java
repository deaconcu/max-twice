package com.prosper.learn.application.dto.response;

import lombok.Data;

/**
 * 卡片diff信息响应DTO
 */
@Data
public class CardDiffDTO {

    private Long cardId;

    private String type;

    private CardVersionInfo oldVersion;

    private CardVersionInfo newVersion;

    @Data
    public static class CardVersionInfo {
        private String front;
        private String back;
    }

}