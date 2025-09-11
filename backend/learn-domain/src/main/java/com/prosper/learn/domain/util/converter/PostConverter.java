package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.PostDTO;
import com.prosper.learn.persistence.dataobject.PostDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostConverter {
    
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "nodeId")
    @Mapping(target = "creatorId")
    @Mapping(target = "type")
    @Mapping(target = "content")
    @Mapping(target = "twice")
    @Mapping(target = "helpful")
    @Mapping(target = "commentCount")
    @Mapping(target = "viewCount")
    @Mapping(target = "state")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    PostDTO toDTO(PostDO postDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<PostDTO> toDTO(List<PostDO> postDOList);
}