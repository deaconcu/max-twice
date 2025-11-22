package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.MemoryCardDeckDTO;
import com.prosper.learn.dto.response.DeckDetailDTO;
import com.prosper.learn.dto.response.deck.*;
import com.prosper.learn.persistence.dataobject.MemoryCardDeckDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 记忆卡片组转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface MemoryCardDeckConverter {

    // ==================== 旧版方法 ====================

    @Deprecated
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "postId")
    @Mapping(target = "nodeId")
    @Mapping(target = "title")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "upvoteCount")
    @Mapping(target = "cardCount")
    MemoryCardDeckDTO toDTO(MemoryCardDeckDO deckDO);

    @Deprecated
    @IterableMapping(qualifiedByName = "toDTO")
    List<MemoryCardDeckDTO> toDTO(List<MemoryCardDeckDO> deckDOList);

    @Deprecated
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "title")
    MemoryCardDeckDTO toDTOV2(MemoryCardDeckDO deckDTO);

    @Deprecated
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "title")
    @Mapping(target = "description")
    @Mapping(target = "postId")
    @Mapping(target = "nodeId")
    @Mapping(target = "state")
    @Mapping(target = "upvoteCount")
    @Mapping(target = "cardCount")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    @Mapping(target = "creator")
    DeckDetailDTO toDeckDetailDTO(DeckWithCreatorDTO deckDTO);

    // ==================== 新版语义化方法 ====================

    @Named("toSummaryDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "postId")
    @Mapping(target = "nodeId")
    @Mapping(target = "title")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "upvoteCount")
    @Mapping(target = "cardCount")
    DeckSummaryDTO toSummaryDTO(MemoryCardDeckDO deckDO);

    @IterableMapping(qualifiedByName = "toSummaryDTO")
    List<DeckSummaryDTO> toSummaryDTO(List<MemoryCardDeckDO> deckDOList);

    @Named("toWithCreatorDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "postId")
    @Mapping(target = "nodeId")
    @Mapping(target = "title")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "upvoteCount")
    @Mapping(target = "cardCount")
    DeckWithCreatorDTO toWithCreatorDTO(MemoryCardDeckDO deckDO);

    @IterableMapping(qualifiedByName = "toWithCreatorDTO")
    List<DeckWithCreatorDTO> toWithCreatorDTO(List<MemoryCardDeckDO> deckDOList);

    @Named("toWithVoteDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "postId")
    @Mapping(target = "nodeId")
    @Mapping(target = "title")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "upvoteCount")
    @Mapping(target = "cardCount")
    DeckWithVoteDTO toWithVoteDTO(MemoryCardDeckDO deckDO);

    @IterableMapping(qualifiedByName = "toWithVoteDTO")
    List<DeckWithVoteDTO> toWithVoteDTO(List<MemoryCardDeckDO> deckDOList);
}
