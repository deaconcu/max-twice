package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.course.CourseDTO;
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

    // ==================== 旧版方法（保留以兼容现有代码）====================

    /**
     * 转换为旧版 CourseDTO（完整字段）
     * @deprecated 使用 toDetailDTO 替代
     */
    @Deprecated
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
    @Mapping(target = "reason")
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    CourseDTO toDTO(CourseDO courseDO);

    /**
     * 批量转换为旧版 CourseDTO
     * @deprecated 使用 toDetailDTO 替代
     */
    @Deprecated
    @IterableMapping(qualifiedByName = "toDTO")
    List<CourseDTO> toDTO(List<CourseDO> courseDOList);

    /**
     * 转换为旧版 V2（列表信息）
     * @deprecated 使用 toSummaryDTO 替代
     */
    @Deprecated
    @Named("toDTO2")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    CourseDTO toDTOV2(CourseDO courseDO);

    /**
     * 批量转换为旧版 V2
     * @deprecated 使用 toSummaryDTO 替代
     */
    @Deprecated
    @IterableMapping(qualifiedByName = "toDTO2")
    List<CourseDTO> toDTOV2(List<CourseDO> courseDOList);

    /**
     * 转换为旧版 V3（极简信息）
     * @deprecated 使用 toBriefDTO 替代
     */
    @Deprecated
    @Named("toDTO3")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    CourseDTO toDTOV3(CourseDO courseDO);

    /**
     * 批量转换为旧版 V3
     * @deprecated 使用 toBriefDTO 替代
     */
    @Deprecated
    @IterableMapping(qualifiedByName = "toDTO3")
    List<CourseDTO> toDTOV3(List<CourseDO> courseDOList);

    // ==================== 新版语义化方法 ====================

    /**
     * 转换为课程简要 DTO（仅 id + name）
     * 用途：搜索结果列表、父课程引用
     */
    @Named("toBriefDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
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
    CourseSummaryDTO toSummaryDTO(CourseDO courseDO);

    /**
     * 批量转换为课程摘要 DTO
     */
    @IterableMapping(qualifiedByName = "toSummaryDTO")
    List<CourseSummaryDTO> toSummaryDTO(List<CourseDO> courseDOList);

    /**
     * 转换为课程详情 DTO（完整信息）
     * 用途：课程详情页、课程编辑
     * 注意：parentCourse 需要在 Service 层额外填充
     */
    @Named("toDetailDTO")
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
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    CourseDetailDTO toDetailDTO(CourseDO courseDO);

    /**
     * 批量转换为课程详情 DTO
     */
    @IterableMapping(qualifiedByName = "toDetailDTO")
    List<CourseDetailDTO> toDetailDTO(List<CourseDO> courseDOList);

    /**
     * 转换为带学习进度的课程 DTO
     * 用途：用户学习中心
     * 注意：subscribed 和 progress 需要在 Service 层额外填充
     */
    @Named("toWithProgressDTO")
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
    @Mapping(target = "createdAt")
    @Mapping(target = "updatedAt")
    CourseWithProgressDTO toWithProgressDTO(CourseDO courseDO);

    /**
     * 批量转换为带学习进度的课程 DTO
     */
    @IterableMapping(qualifiedByName = "toWithProgressDTO")
    List<CourseWithProgressDTO> toWithProgressDTO(List<CourseDO> courseDOList);

    /**
     * 转换为带统计信息的课程 DTO
     * 用途：热门课程排行榜
     * 注意：learnerCount 和 subscriptionCount 需要在 Service 层额外填充
     */
    @Named("toWithStatsDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    CourseWithStatsDTO toWithStatsDTO(CourseDO courseDO);

    /**
     * 批量转换为带统计信息的课程 DTO
     */
    @IterableMapping(qualifiedByName = "toWithStatsDTO")
    List<CourseWithStatsDTO> toWithStatsDTO(List<CourseDO> courseDOList);

    /**
     * 转换为课程摘要（含统计和进度）DTO
     * 用途：课程详情页面、用户订阅列表
     * 注意：learnerCount, subscriptionCount, subscribed, progress, parentCourseName 需要在 Service 层额外填充
     */
    @Named("toSummaryWithStatsAndProgressDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "name")
    @Mapping(target = "description")
    @Mapping(target = "mainCategory")
    @Mapping(target = "subCategory")
    @Mapping(target = "parentCourseId")
    CourseSummaryWithStatsAndProgressDTO toSummaryWithStatsAndProgressDTO(CourseDO courseDO);

    /**
     * 批量转换为课程摘要（含统计和进度）DTO
     */
    @IterableMapping(qualifiedByName = "toSummaryWithStatsAndProgressDTO")
    List<CourseSummaryWithStatsAndProgressDTO> toSummaryWithStatsAndProgressDTO(List<CourseDO> courseDOList);
}