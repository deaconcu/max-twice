package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.UserCourseDTO;
import com.prosper.learn.persistence.dataobject.UserCourseDO;
import com.prosper.learn.domain.service.data.CourseDataService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class UserCourseConverter {

    @Autowired
    protected CourseDataService courseDataService;
    
    @Named("toDTO")
    public abstract UserCourseDTO toDTO(UserCourseDO userCourseDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    public abstract List<UserCourseDTO> toDTO(List<UserCourseDO> userCourseDOList);
}