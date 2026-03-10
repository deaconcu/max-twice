package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.course.*;
import com.prosper.learn.content.course.CourseDO;
import org.mapstruct.*;

import java.util.List;

/**
 * 课程 DTO 转换器
 *
 * 提供 CourseDO 到各种 DTO 的转换方法
 * 使用 MapStruct 自动生成实现代码
 *
 * @author Claude
 * @since 2025-01-18
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface CourseConverter {

    // ==================== 语义化方法 ====================

    /**
     * 转换为课程简要 DTO（仅 id + name + icon）
     * 用途：搜索结果列表、父课程引用
     */
    @Named("toBriefDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "icon")
    CourseBriefDTO toBriefDTO(CourseDO courseDO);

    /**
     * 批量转换为课程简要 DTO
     */
    @IterableMapping(qualifiedByName = "toBriefDTO")
    List<CourseBriefDTO> toBriefDTO(List<CourseDO> courseDOList);

    /**
     * 转换为课程摘要 DTO（列表信息）
     * 用途：子课程列表、分类浏览
     * 注意：parentCourse 需要在 Service 层额外填充
     */
    @Named("toSummaryDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "rootNodeId")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    @Mapping(target = "icon")
    CourseSummaryDTO toSummaryDTO(CourseDO courseDO);

    /**
     * 批量转换为课程摘要 DTO
     */
    @IterableMapping(qualifiedByName = "toSummaryDTO")
    List<CourseSummaryDTO> toSummaryDTO(List<CourseDO> courseDOList);

    /**
     * 转换为课程完整信息 DTO
     * 用途：课程详情页、课程列表、用户学习中心、热门排行榜
     * 注意：learnerCount, bookmarkCount, bookmarked, progress, parentCourse 需要在 Service 层额外填充
     */
    @Named("toFullDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "rootNodeId")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    @Mapping(target = "icon")
    CourseFullDTO toFullDTO(CourseDO courseDO);

    /**
     * 批量转换为课程完整信息 DTO
     */
    @IterableMapping(qualifiedByName = "toFullDTO")
    List<CourseFullDTO> toFullDTO(List<CourseDO> courseDOList);

    /**
     * 转换为管理后台DTO（包含 reason）
     * 注意：creator 和 parentCourse 需要在 Service 层额外填充
     */
    @Named("toAdminDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    @Mapping(target = "creatorId")
    @Mapping(target = "rootNodeId")
    @Mapping(target = "parentCourseId")
    @Mapping(target = "state")
    @Mapping(target = "reason")
    @Mapping(target = "icon")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    CourseAdminDTO toAdminDTO(CourseDO courseDO);

    /**
     * 批量转换为管理后台DTO
     */
    @IterableMapping(qualifiedByName = "toAdminDTO")
    List<CourseAdminDTO> toAdminDTO(List<CourseDO> courseDOList);
}