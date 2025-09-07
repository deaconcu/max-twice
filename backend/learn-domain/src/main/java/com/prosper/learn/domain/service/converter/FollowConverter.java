package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.FollowDTO;
import com.prosper.learn.persistence.dataobject.FollowDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class FollowConverter {
    
    @Named("toDTO")
    public abstract FollowDTO toDTO(FollowDO followDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    public abstract List<FollowDTO> toDTO(List<FollowDO> followDOList);
}