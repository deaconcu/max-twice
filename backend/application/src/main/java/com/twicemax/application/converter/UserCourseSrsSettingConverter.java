package com.twicemax.application.converter;

import com.twicemax.application.dto.response.UserCourseSrsSettingDTO;
import com.twicemax.memory.review.UserCourseSrsSettingDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 用户课程SRS设置转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface UserCourseSrsSettingConverter {

    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "frequencySetting")
    @Mapping(target = "state")
    @Mapping(target = "cardOrder")
    @Mapping(target = "dailyNewLimit")
    @Mapping(target = "dailyReviewLimit")
    @Mapping(target = "frozenAt")
    @Mapping(target = "frozenDuration")
    UserCourseSrsSettingDTO toDTO(UserCourseSrsSettingDO settingDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<UserCourseSrsSettingDTO> toDTO(List<UserCourseSrsSettingDO> settingDOList);

}