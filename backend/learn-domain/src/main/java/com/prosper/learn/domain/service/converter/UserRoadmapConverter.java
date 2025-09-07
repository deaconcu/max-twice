package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.UserRoadmapDTO;
import com.prosper.learn.persistence.dataobject.UserRoadmapDO;
import com.prosper.learn.domain.service.data.RoadmapDataService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class UserRoadmapConverter {

    @Autowired
    protected RoadmapDataService roadmapDataService;
    
    @Named("toDTO")
    public abstract UserRoadmapDTO toDTO(UserRoadmapDO userRoadmapDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    public abstract List<UserRoadmapDTO> toDTO(List<UserRoadmapDO> userRoadmapDOList);
}