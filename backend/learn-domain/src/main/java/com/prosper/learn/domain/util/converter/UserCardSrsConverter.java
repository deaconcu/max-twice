package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.UserCardSrsDTO;
import com.prosper.learn.persistence.dataobject.UserCardSrsDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 用户卡片SRS状态转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface UserCardSrsConverter {

    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "reviewDueAt")
    @Mapping(target = "lastReviewedAt")
    @Mapping(target = "intervalDays")
    @Mapping(target = "repetitions")
    @Mapping(target = "lapseCount")
    UserCardSrsDTO toDTO(UserCardSrsDO srsStateDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<UserCardSrsDTO> toDTO(List<UserCardSrsDO> srsStateDOList);

}