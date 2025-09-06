package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.UserRoadmapDTO;
import com.prosper.learn.persistence.dataobject.UserRoadmapDO;
import com.prosper.learn.domain.service.data.RoadmapDataService;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", 
        uses = {RoadmapDataService.class}, 
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserRoadmapConverter {
    
    @Named("toDTO")
    @Mapping(target = "roadmap", ignore = true)
    UserRoadmapDTO toDTO(UserRoadmapDO userRoadmapDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<UserRoadmapDTO> toDTO(List<UserRoadmapDO> userRoadmapDOList);
}