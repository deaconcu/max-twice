package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.RoadmapDTO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoadmapConverter {
    
    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "professionId")
    @Mapping(target = "creatorId")
    @Mapping(target = "description")
    @Mapping(target = "vote")
    @Mapping(target = "comment")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    RoadmapDTO toDTO(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<RoadmapDTO> toDTO(List<RoadmapDO> roadmapDOList);
}