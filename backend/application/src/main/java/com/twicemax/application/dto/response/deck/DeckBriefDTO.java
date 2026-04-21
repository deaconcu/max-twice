package com.twicemax.application.dto.response.deck;

import com.twicemax.shared.domain.Enums;
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

    /**
     * 卡片组状态
     * @see Enums.ContentState
     */
    private Byte state;
}
