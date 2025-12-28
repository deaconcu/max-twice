package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.userroadmap.UserRoadmapSummaryDTO;
import com.prosper.learn.application.dto.response.userroadmap.UserRoadmapWithBriefDTO;
import com.prosper.learn.application.dto.response.userroadmap.UserRoadmapWithDetailDTO;
import com.prosper.learn.learning.enrollment.UserRoadmapDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 用户路线图转换器
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface UserRoadmapConverter {

    /**
     * 转换为摘要 DTO（基础信息，不含路线图）
     */
    @Named("toSummaryDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "userId")
    @Mapping(target = "roadmapId")
    @Mapping(target = "progressPercent")
    @Mapping(target = "state")
    @Mapping(target = "startedAt")
    @Mapping(target = "completedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    UserRoadmapSummaryDTO toSummaryDTO(UserRoadmapDO userRoadmapDO);

    @IterableMapping(qualifiedByName = "toSummaryDTO")
    List<UserRoadmapSummaryDTO> toSummaryDTO(List<UserRoadmapDO> userRoadmapDOList);

    /**
     * 转换为带路线图简要信息的 DTO（需要在 Service 层填充 roadmap 字段）
     */
    @Named("toWithBriefDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "userId")
    @Mapping(target = "roadmapId")
    @Mapping(target = "progressPercent")
    @Mapping(target = "state")
    @Mapping(target = "startedAt")
    @Mapping(target = "completedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    UserRoadmapWithBriefDTO toWithBriefDTO(UserRoadmapDO userRoadmapDO);

    @IterableMapping(qualifiedByName = "toWithBriefDTO")
    List<UserRoadmapWithBriefDTO> toWithBriefDTO(List<UserRoadmapDO> userRoadmapDOList);

    /**
     * 转换为带路线图详细信息的 DTO（需要在 Service 层填充 roadmap 字段）
     */
    @Named("toWithDetailDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "userId")
    @Mapping(target = "roadmapId")
    @Mapping(target = "progressPercent")
    @Mapping(target = "state")
    @Mapping(target = "startedAt")
    @Mapping(target = "completedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    UserRoadmapWithDetailDTO toWithDetailDTO(UserRoadmapDO userRoadmapDO);

    @IterableMapping(qualifiedByName = "toWithDetailDTO")
    List<UserRoadmapWithDetailDTO> toWithDetailDTO(List<UserRoadmapDO> userRoadmapDOList);
}