package com.prosper.learn.shared.converter;

import com.prosper.learn.persistence.dataobject.RoadmapDO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommonConverter.class)
public interface RoadmapConverter {

    /**
     * 转换为摘要DTO（基础信息）
     */
    @Named("toSummaryDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "professionId")
    @Mapping(target = "creatorId")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    RoadmapSummaryDTO toSummaryDTO(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toSummaryDTO")
    List<RoadmapSummaryDTO> toSummaryDTO(List<RoadmapDO> roadmapDOList);

    /**
     * 转换为带状态的DTO（需要在 Service 层填充 creator, profession, upvoted, pinned, learning）
     */
    @Named("toWithStatusDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "professionId")
    @Mapping(target = "creatorId")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    RoadmapWithStatusDTO toWithStatusDTO(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toWithStatusDTO")
    List<RoadmapWithStatusDTO> toWithStatusDTO(List<RoadmapDO> roadmapDOList);
}
