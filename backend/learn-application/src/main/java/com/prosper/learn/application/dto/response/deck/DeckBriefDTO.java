package com.prosper.learn.application.dto.response.deck;

import lombok.Data;

/**
 * 卡片组简要信息 DTO
 *
 * 用途：卡片关联的卡片组基本信息（CardWithSrsDTO 使用）
 */
@Data
public class DeckBriefDTO {

    private Long id;

    private Long postId;

    private Long nodeId;

    private Long courseId;

    private String nodeName;

    private String courseName;
}
