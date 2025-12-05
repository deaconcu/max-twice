package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.usercourse.UserCourseSummaryDTO;
import com.prosper.learn.application.dto.response.usercourse.UserCourseWithCourseDTO;
import com.prosper.learn.learning.enrollment.UserCourseDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 用户课程转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface UserCourseConverter {

    /**
     * 转换为摘要 DTO（基础信息，不含课程）
     */
    @Named("toSummaryDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "userId")
    @Mapping(target = "courseId")
    @Mapping(target = "progressPercent")
    @Mapping(target = "state")
    @Mapping(target = "startedAt")
    @Mapping(target = "completedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    UserCourseSummaryDTO toSummaryDTO(UserCourseDO userCourseDO);

    @IterableMapping(qualifiedByName = "toSummaryDTO")
    List<UserCourseSummaryDTO> toSummaryDTO(List<UserCourseDO> userCourseDOList);

    /**
     * 转换为带课程信息的 DTO（需要在 Service 层填充 course 字段）
     */
    @Named("toWithCourseDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "userId")
    @Mapping(target = "courseId")
    @Mapping(target = "progressPercent")
    @Mapping(target = "state")
    @Mapping(target = "startedAt")
    @Mapping(target = "completedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    UserCourseWithCourseDTO toWithCourseDTO(UserCourseDO userCourseDO);

    @IterableMapping(qualifiedByName = "toWithCourseDTO")
    List<UserCourseWithCourseDTO> toWithCourseDTO(List<UserCourseDO> userCourseDOList);
}