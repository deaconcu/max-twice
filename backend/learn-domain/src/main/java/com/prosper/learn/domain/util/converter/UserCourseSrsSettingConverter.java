package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.UserCourseSrsSettingDTO;
import com.prosper.learn.persistence.dataobject.UserCourseSrsSettingDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 用户课程SRS设置转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserCourseSrsSettingConverter {

    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "frequencySetting")
    @Mapping(target = "state")
    UserCourseSrsSettingDTO toDTO(UserCourseSrsSettingDO settingDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<UserCourseSrsSettingDTO> toDTO(List<UserCourseSrsSettingDO> settingDOList);

}