package com.prosper.learn.application.converter;

import com.prosper.learn.application.dto.response.roadmap.RoadmapAdminDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapDetailDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapSummaryDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapWithStatusDTO;
import com.prosper.learn.content.roadmap.RoadmapDO;
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
    @Mapping(target = "nodeCount")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    RoadmapSummaryDTO toSummaryDTO(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toSummaryDTO")
    List<RoadmapSummaryDTO> toSummaryDTO(List<RoadmapDO> roadmapDOList);

    /**
     * 转换为详情DTO（需要在 Service 层填充 profession 信息）
     */
    @Named("toDetailDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "professionId")
    @Mapping(target = "creatorId")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "nodeCount")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    RoadmapDetailDTO toDetailDTO(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toDetailDTO")
    List<RoadmapDetailDTO> toDetailDTO(List<RoadmapDO> roadmapDOList);

    /**
     * 转换为带状态的DTO（需要在 Service 层填充 creator, profession, liked, learning）
     */
    @Named("toWithStatusDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "professionId")
    @Mapping(target = "creatorId")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "nodeCount")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    RoadmapWithStatusDTO toWithStatusDTO(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toWithStatusDTO")
    List<RoadmapWithStatusDTO> toWithStatusDTO(List<RoadmapDO> roadmapDOList);

    /**
     * 转换为管理后台DTO（包含 reason，需要在 Service 层填充 profession 和 creator）
     */
    @Named("toAdminDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "professionId")
    @Mapping(target = "creatorId")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "reason")
    @Mapping(target = "nodeCount")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    RoadmapAdminDTO toAdminDTO(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toAdminDTO")
    List<RoadmapAdminDTO> toAdminDTO(List<RoadmapDO> roadmapDOList);
}
