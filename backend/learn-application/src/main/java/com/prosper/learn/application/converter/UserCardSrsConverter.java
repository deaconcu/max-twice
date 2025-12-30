package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.UserCardSrsDTO;
import com.prosper.learn.memory.review.UserCardSrsDO;
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
    @Mapping(target = "type")
    @Mapping(target = "currentStep")
    @Mapping(target = "interval")
    @Mapping(target = "reviewDueAt")
    @Mapping(target = "lastReviewedAt")
    @Mapping(target = "repetitions")
    @Mapping(target = "lapseCount")
    UserCardSrsDTO toDTO(UserCardSrsDO srsStateDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<UserCardSrsDTO> toDTO(List<UserCardSrsDO> srsStateDOList);

}