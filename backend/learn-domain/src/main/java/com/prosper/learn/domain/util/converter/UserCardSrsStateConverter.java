package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.UserCardSrsStateDTO;
import com.prosper.learn.persistence.dataobject.UserCardSrsStateDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 用户卡片SRS状态转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserCardSrsStateConverter {

    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "reviewDueAt")
    @Mapping(target = "lastReviewedAt")
    @Mapping(target = "intervalDays")
    @Mapping(target = "repetitions")
    @Mapping(target = "lapseCount")
    UserCardSrsStateDTO toDTO(UserCardSrsStateDO srsStateDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<UserCardSrsStateDTO> toDTO(List<UserCardSrsStateDO> srsStateDOList);

}