package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.UserCourseSrsSettingDTO;
import com.prosper.learn.memory.review.UserCourseSrsSettingDO;
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
    UserCourseSrsSettingDTO toDTO(UserCourseSrsSettingDO settingDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<UserCourseSrsSettingDTO> toDTO(List<UserCourseSrsSettingDO> settingDOList);

}