package com.prosper.learn.application.assembler;

import com.prosper.learn.application.dto.response.roadmap.RoadmapBriefDTO;
import com.prosper.learn.content.profession.ProfessionDO;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Roadmap DTO 组装器
 * 负责将 RoadmapDO 转换为各种 DTO（需要联查数据库）
 */
@Component
@RequiredArgsConstructor
public class RoadmapAssembler {

    private final ProfessionDataService professionDataService;

    /**
     * 转换为简要 DTO（单个）
     */
    public RoadmapBriefDTO toBriefDTO(RoadmapDO roadmapDO) {
        if (roadmapDO == null) {
            return null;
        }

        RoadmapBriefDTO dto = new RoadmapBriefDTO();
        dto.setId(roadmapDO.getId());
        dto.setNodeCount(roadmapDO.getNodeCount());

        if (roadmapDO.getProfessionId() != null) {
            ProfessionDO profession = professionDataService.getById(roadmapDO.getProfessionId());
            if (profession != null) {
                dto.setProfessionName(profession.getName());
                dto.setProfessionIcon(profession.getIcon());
            }
        }

        return dto;
    }

    /**
     * 批量转换为简要 DTO（优化：批量查询 Profession）
     */
    public List<RoadmapBriefDTO> toBriefDTO(List<RoadmapDO> roadmapDOList) {
        if (roadmapDOList == null || roadmapDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取所有 profession IDs
        List<Long> professionIds = roadmapDOList.stream()
            .map(RoadmapDO::getProfessionId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        // 批量查询 professions
        Map<Long, ProfessionDO> professionMap = Map.of();
        if (!professionIds.isEmpty()) {
            List<ProfessionDO> professions = professionDataService.getByIds(professionIds);
            professionMap = professions.stream()
                .collect(Collectors.toMap(ProfessionDO::getId, p -> p));
        }

        // 组装 DTO
        List<RoadmapBriefDTO> result = new ArrayList<>();
        for (RoadmapDO roadmapDO : roadmapDOList) {
            RoadmapBriefDTO dto = new RoadmapBriefDTO();
            dto.setId(roadmapDO.getId());
            dto.setNodeCount(roadmapDO.getNodeCount());

            if (roadmapDO.getProfessionId() != null) {
                ProfessionDO profession = professionMap.get(roadmapDO.getProfessionId());
                if (profession != null) {
                    dto.setProfessionName(profession.getName());
                    dto.setProfessionIcon(profession.getIcon());
                }
            }

            result.add(dto);
        }

        return result;
    }
}
