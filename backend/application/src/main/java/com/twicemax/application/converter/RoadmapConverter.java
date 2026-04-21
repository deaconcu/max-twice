package com.twicemax.application.converter;

import com.twicemax.application.dto.response.roadmap.RoadmapAdminDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapDetailDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapSummaryDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapWithStatusDTO;
import com.twicemax.content.roadmap.RoadmapDO;
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
    @Mapping(target = "roleId")
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
     * 转换为详情DTO（需要在 Service 层填充 role 信息）
     */
    @Named("toDetailDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "roleId")
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
     * 转换为带状态的DTO（需要在 Service 层填充 creator, role, liked, learning）
     */
    @Named("toWithStatusDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "roleId")
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
     * 转换为管理后台DTO（包含 reason，需要在 Service 层填充 role 和 creator）
     */
    @Named("toAdminDTO")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "content")
    @Mapping(target = "roleId")
    @Mapping(target = "creatorId")
    @Mapping(target = "description")
    @Mapping(target = "state")
    @Mapping(target = "reason")
    @Mapping(target = "nodeCount")
    @Mapping(target = "updatedAt")
    @Mapping(target = "createdAt")
    @Mapping(target = "score")
    RoadmapAdminDTO toAdminDTO(RoadmapDO roadmapDO);

    @IterableMapping(qualifiedByName = "toAdminDTO")
    List<RoadmapAdminDTO> toAdminDTO(List<RoadmapDO> roadmapDOList);
}
