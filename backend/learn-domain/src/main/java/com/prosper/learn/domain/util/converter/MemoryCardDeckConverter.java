package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.MemoryCardDeckDTO;
import com.prosper.learn.dto.response.DeckDetailDTO;
import com.prosper.learn.persistence.dataobject.MemoryCardDeckDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 记忆卡片组转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface MemoryCardDeckConverter {

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

    @IterableMapping(qualifiedByName = "toDTO")
    List<MemoryCardDeckDTO> toDTO(List<MemoryCardDeckDO> deckDOList);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "title")
    MemoryCardDeckDTO toDTOV2(MemoryCardDeckDO deckDTO);
    
    /**
     * 从MemoryCardDeckDTO转换为DeckDetailDTO
     */
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
    DeckDetailDTO toDeckDetailDTO(MemoryCardDeckDTO deckDTO);



}