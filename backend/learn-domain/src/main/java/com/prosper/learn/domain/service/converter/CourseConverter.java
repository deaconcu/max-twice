package com.prosper.learn.domain.service.converter;

import com.prosper.learn.dto.response.CourseDTO;
import com.prosper.learn.dto.response.NodeDTO;
import com.prosper.learn.dto.response.old.CourseDTOV3;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.domain.service.data.CourseDataService;
import com.prosper.learn.persistence.dataobject.NodeDO;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class CourseConverter {

    @Autowired
    protected CourseDataService courseDataService;

    @Named("toDTO")
    public abstract CourseDTO toDTO(CourseDO courseDO);
    
    @IterableMapping(qualifiedByName = "toDTO")
    public abstract List<CourseDTO> toDTO(List<CourseDO> courseDOList);

    public abstract CourseDO toCourseDO(CourseDTO courseDTO);

    @Named("toDTO2")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    public abstract CourseDTO toDTOV2(CourseDO courseDO);
    
    @IterableMapping(qualifiedByName = "toDTO2")
    public abstract List<CourseDTO> toDTOV2(List<CourseDO> courseDOList);

    @Named("toDTO3")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    public abstract CourseDTO toCourseDTOV3(CourseDO courseDO);
    
    @IterableMapping(qualifiedByName = "toDTO3")
    public abstract List<CourseDTO> toDTOV3(List<CourseDO> courseDOList);

    @Named("toDTO4")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "creatorId")
    @Mapping(target = "rootNodeId")
    @Mapping(target = "parentCourseId")
    @Mapping(target = "parentCourse", source = "parentCourseId", qualifiedByName = "getParentCourse")
    @Mapping(target = "state")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    @Mapping(target = "rejectedReason")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    @Mapping(target = "learnerCount", ignore = true)
    @Mapping(target = "subscriptionCount", ignore = true)
    @Mapping(target = "subscribed", ignore = true)
    @Mapping(target = "progress", ignore = true)
    public abstract CourseDTO toDTOV4(CourseDO courseDO);
    
    @IterableMapping(qualifiedByName = "toDTO4")
    public abstract List<CourseDTO> toDTOV4(List<CourseDO> courseDOList);
    
    @Named("getParentCourse")
    public CourseDTO getParentCourse(Long parentCourseId) {
        if (parentCourseId == null || parentCourseId <= 0) return null;
        
        CourseDO parentCourse = courseDataService.getById(parentCourseId);
        return parentCourse != null ? toCourseDTOV3(parentCourse) : null;
    }

    public CourseDTO toDTOV4(CourseDO courseDO, boolean subscribed, int progress) {
        if (courseDO == null) return null;

        CourseDTO dto = toDTOV4(courseDO);
        dto.setSubscribed(subscribed);
        dto.setProgress(progress);
        return dto;
    }
}