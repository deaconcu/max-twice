package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.FollowDTO;
import com.prosper.learn.interaction.follow.FollowDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface FollowConverter {
    
    @Named("toDTO")
    FollowDTO toDTO(FollowDO followDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<FollowDTO> toDTO(List<FollowDO> followDOList);
}