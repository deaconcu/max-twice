package com.twicemax.application.converter;

import com.twicemax.application.dto.response.FollowDTO;
import com.twicemax.interaction.follow.FollowDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface FollowConverter {
    
    @Named("toDTO")
    FollowDTO toDTO(FollowDO followDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<FollowDTO> toDTO(List<FollowDO> followDOList);
}