package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.CommentDTO;
import com.prosper.learn.persistence.dataobject.CommentDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentConverter {

    @Named("toDTO")
    CommentDTO toDTO(CommentDO commentDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<CommentDTO> toDTO(List<CommentDO> commentDOList);

    @Named("toDTOV1")
    @Mapping(target = "upvoted", ignore = true)
    @Mapping(target = "children", ignore = true)
    CommentDTO toDTOV1(CommentDO commentDO);

    @IterableMapping(qualifiedByName = "toDTOV1")
    List<CommentDTO> toDTOV1(List<CommentDO> commentDOList);
    
    default void addChild(CommentDTO parent, CommentDTO child) {
        if (parent != null && child != null) {
            parent.addChild(child);
        }
    }
}