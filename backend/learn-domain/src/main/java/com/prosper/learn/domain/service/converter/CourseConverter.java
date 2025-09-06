package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.old.CourseDTOV1;
import com.prosper.learn.dto.response.old.CourseDTOV3;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.domain.service.data.CourseDataService;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CourseDataService.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseConverter {

    @Named("toDTO")
    CourseDTOV1 toCourseDTO(CourseDO courseDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    List<CourseDTOV1> toCourseDTO(List<CourseDO> courseDOList);

    CourseDO toCourseDO(CourseDTOV1 courseDTOV1);

    @Named("toDTO2")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    CourseDTOV1 toCourseDTOV2(CourseDO courseDO);
    
    @IterableMapping(qualifiedByName = "toDTO2")
    List<CourseDTOV1> toCourseDTOV2(List<CourseDO> courseDOList);

    @Named("toDTO3")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    CourseDTOV1 toCourseDTOV3(CourseDO courseDO);
    
    @IterableMapping(qualifiedByName = "toDTO3")
    List<CourseDTOV1> toCourseDTOV3(List<CourseDO> courseDOList);

    @Named("toDTO4")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "creator")
    @Mapping(target = "rootNode")
    @Mapping(target = "parent", qualifiedByName = "getParentCourse")
    @Mapping(target = "state")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    @Mapping(target = "rejectedReason")
    @Mapping(target = "learnerCount", ignore = true)
    @Mapping(target = "subscriptionCount", ignore = true)
    @Mapping(target = "subscribed", ignore = true)
    @Mapping(target = "progress", ignore = true)
    CourseDTOV1 toCourseDTOV4(CourseDO courseDO);
    
    @IterableMapping(qualifiedByName = "toDTO4")
    List<CourseDTOV1> toCourseDTOV4(List<CourseDO> courseDOList);
    
    @Named("getParentCourse")
    default CourseDTOV3 getParentCourse(Long parentId, @Context CourseDataService courseDataService) {
        if (parentId == null || parentId <= 0) return null;
        
        CourseDO parentCourse = courseDataService.getById(parentId);
        return parentCourse != null ? toCourseDTOV3Basic(parentCourse) : null;
    }
    
    CourseDTOV3 toCourseDTOV3Basic(CourseDO courseDO);
}