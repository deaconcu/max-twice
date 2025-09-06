package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.UserCourseDTO;
import com.prosper.learn.persistence.dataobject.UserCourseDO;
import com.prosper.learn.domain.service.data.CourseDataService;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", 
        uses = {CourseDataService.class}, 
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserCourseConverter {
    
    @Named("toDTO")
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "state")
    UserCourseDTO toDTO(UserCourseDO userCourseDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<UserCourseDTO> toDTO(List<UserCourseDO> userCourseDOList);
}