package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.deck.*;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 记忆卡片组转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface MemoryCardDeckConverter {

    @Named("toFullDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "postId")
    @Mapping(target = "nodeId")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "cardCount")
    DeckFullDTO toFullDTO(MemoryCardDeckDO deckDO);

    @IterableMapping(qualifiedByName = "toFullDTO")
    List<DeckFullDTO> toFullDTO(List<MemoryCardDeckDO> deckDOList);

    @Named("toDeckDetailDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "description")
    @Mapping(target = "postId")
    @Mapping(target = "nodeId")
    @Mapping(target = "courseId")
    @Mapping(target = "state")
    @Mapping(target = "cardCount")
    @Mapping(target = "likeCount")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    @Mapping(target = "creator")
    @Mapping(target = "course")
    @Mapping(target = "node")
    DeckAndCardsDTO toDeckDetailDTO(DeckFullDTO deckDTO);

    @Named("toBriefDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "postId")
    @Mapping(target = "nodeId")
    DeckBriefDTO toBriefDTO(MemoryCardDeckDO deckDO);

    @IterableMapping(qualifiedByName = "toBriefDTO")
    List<DeckBriefDTO> toBriefDTO(List<MemoryCardDeckDO> deckDOList);

    /**
     * 转换为管理后台DTO（包含 reason）
     * 注意：creator, course, node, cards, courseId, likeCount 需要在 Service 层额外填充
     */
    @Named("toAdminDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "postId")
    @Mapping(target = "nodeId")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "reason")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "cardCount")
    @Mapping(target = "score")
    DeckAdminDTO toAdminDTO(MemoryCardDeckDO deckDO);

    @IterableMapping(qualifiedByName = "toAdminDTO")
    List<DeckAdminDTO> toAdminDTO(List<MemoryCardDeckDO> deckDOList);
}
