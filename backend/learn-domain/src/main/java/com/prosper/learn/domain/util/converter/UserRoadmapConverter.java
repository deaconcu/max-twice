package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.UserRoadmapDTO;
import com.prosper.learn.persistence.dataobject.UserRoadmapDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface UserRoadmapConverter {
    
    @Named("toDTO")
    UserRoadmapDTO toDTO(UserRoadmapDO userRoadmapDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<UserRoadmapDTO> toDTO(List<UserRoadmapDO> userRoadmapDOList);
}