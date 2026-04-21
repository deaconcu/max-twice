package com.twicemax.application.assembler;

import com.twicemax.application.dto.response.roadmap.RoadmapBriefDTO;
import com.twicemax.content.role.RoleDO;
import com.twicemax.content.role.RoleDataService;
import com.twicemax.content.roadmap.RoadmapDO;
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

    private final RoleDataService roleDataService;

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

        if (roadmapDO.getRoleId() != null) {
            RoleDO roleDO = roleDataService.getById(roadmapDO.getRoleId());
            if (roleDO != null) {
                dto.setRoleName(roleDO.getName());
                dto.setRoleIcon(roleDO.getIcon());
            }
        }

        return dto;
    }

    /**
     * 批量转换为简要 DTO（优化：批量查询 Role
     */
    public List<RoadmapBriefDTO> toBriefDTO(List<RoadmapDO> roadmapDOList) {
        if (roadmapDOList == null || roadmapDOList.isEmpty()) {
            return new ArrayList<>();
        }

        // 提取所有 role IDs
        List<Long> roleIds = roadmapDOList.stream()
            .map(RoadmapDO::getRoleId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        // 批量查询 roles
        Map<Long, RoleDO> roleDOMap = Map.of();
        if (!roleIds.isEmpty()) {
            List<RoleDO> roleDOS = roleDataService.getByIds(roleIds);
            roleDOMap = roleDOS.stream()
                .collect(Collectors.toMap(RoleDO::getId, p -> p));
        }

        // 组装 DTO
        List<RoadmapBriefDTO> result = new ArrayList<>();
        for (RoadmapDO roadmapDO : roadmapDOList) {
            RoadmapBriefDTO dto = new RoadmapBriefDTO();
            dto.setId(roadmapDO.getId());
            dto.setNodeCount(roadmapDO.getNodeCount());

            if (roadmapDO.getRoleId() != null) {
                RoleDO roleDO = roleDOMap.get(roadmapDO.getRoleId());
                if (roleDO != null) {
                    dto.setRoleName(roleDO.getName());
                    dto.setRoleIcon(roleDO.getIcon());
                }
            }

            result.add(dto);
        }

        return result;
    }
}
