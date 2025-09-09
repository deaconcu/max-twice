package com.prosper.learn.domain.util.converter;

import com.prosper.learn.dto.response.CourseDTO;
import com.prosper.learn.persistence.dataobject.CourseDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CourseConverter {

    @Named("toDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "creatorId")
    @Mapping(target = "rootNodeId")
    @Mapping(target = "parentCourseId")
    @Mapping(target = "state")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    @Mapping(target = "rejectedReason")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    CourseDTO toDTO(CourseDO courseDO);

    @IterableMapping(qualifiedByName = "toDTO")
    List<CourseDTO> toDTO(List<CourseDO> courseDOList);

    @Named("toDTO2")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    CourseDTO toDTOV2(CourseDO courseDO);
    
    @IterableMapping(qualifiedByName = "toDTO2")
    List<CourseDTO> toDTOV2(List<CourseDO> courseDOList);

    @Named("toDTO3")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    CourseDTO toDTOV3(CourseDO courseDO);
    
    @IterableMapping(qualifiedByName = "toDTO3")
    List<CourseDTO> toDTOV3(List<CourseDO> courseDOList);


}