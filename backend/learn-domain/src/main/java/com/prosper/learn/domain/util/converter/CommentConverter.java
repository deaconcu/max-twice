package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.CommentDTO;
import com.prosper.learn.persistence.dataobject.CommentDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface CommentConverter {

    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "objectType")
    @Mapping(target = "objectId")
    @Mapping(target = "replyCount")
    @Mapping(target = "replyToCommentId")
    @Mapping(target = "creatorId")
    @Mapping(target = "toUserId")
    @Mapping(target = "upvoteCount")
    @Mapping(target = "state")
    @Mapping(target = "score")
    @Mapping(target = "createdAt")
    CommentDTO toDTO(CommentDO commentDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<CommentDTO> toDTO(List<CommentDO> commentDOList);

    default void addChild(CommentDTO parent, CommentDTO child) {
        if (parent != null && child != null) {
            parent.addChild(child);
        }
    }
}