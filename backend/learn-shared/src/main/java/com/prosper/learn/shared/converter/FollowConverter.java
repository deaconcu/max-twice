package com.prosper.learn.shared.converter;

import com.prosper.learn.dto.response.FollowDTO;
import com.prosper.learn.persistence.dataobject.FollowDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface FollowConverter {
    
    @Named("toDTO")
    FollowDTO toDTO(FollowDO followDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<FollowDTO> toDTO(List<FollowDO> followDOList);
}