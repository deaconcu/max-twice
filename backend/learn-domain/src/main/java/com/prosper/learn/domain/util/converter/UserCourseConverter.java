package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.UserCourseDTO;
import com.prosper.learn.persistence.dataobject.UserCourseDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserCourseConverter {
    
    @Named("toDTO")
    UserCourseDTO toDTO(UserCourseDO userCourseDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<UserCourseDTO> toDTO(List<UserCourseDO> userCourseDOList);
}