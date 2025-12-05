package com.prosper.learn.application.dto.response.card;

import lombok.Data;

/**
 * 卡片内容 DTO
 *
 * 用途：基础卡片内容（正反面）
 *
 * @author Claude
 * @since 2025-01-18
 */
@Data
public class CardContentDTO {

    private Long id;

    private String front;

    private String back;
}
