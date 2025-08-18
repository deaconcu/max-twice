package com.prosper.learn.domain.service;

import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.ProfessionDTO;
import com.prosper.learn.dto.RoadmapDTOV2;
import com.prosper.learn.dto.UserRoadmapDTO;
import com.prosper.learn.persistence.dataobject.ProfessionDO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.dataobject.UserRoadmapDO;
import com.prosper.learn.persistence.mapper.ProfessionMapper;
import com.prosper.learn.persistence.mapper.RoadmapMapper;
import com.prosper.learn.persistence.mapper.UserRoadmapMapper;
import com.prosper.learn.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoadmapService {

    private final UserRoadmapMapper userRoadmapMapper;
    private final RoadmapMapper roadmapMapper;
    private final UserMapper userMapper;
    private final RoadmapService roadmapService;
    private final ProfessionMapper professionMapper;

    /**
     * 用户开始学习路线图
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     * @return 学习进度记录
     */
    public boolean startRoadmap(Long userId, Long roadmapId) {
        // 检查是否已经存在学习记录
        UserRoadmapDO existing = userRoadmapMapper.getByUserAndRoadmap(userId, roadmapId);

        if (existing != null) {
            // 如果已存在，直接返回
            //return Converter.INSTANCE.userRoadmapProgressDOToDTO(existing);
            userRoadmapMapper.deleteByUserAndRoadmap(userId, roadmapId);
            return false;
        }

        // 创建新的学习记录
        UserRoadmapDO userRoadmapDO = new UserRoadmapDO();
        userRoadmapDO.setUserId(userId);
        userRoadmapDO.setRoadmapId(roadmapId);
        userRoadmapDO.setProgressPercent(0);
        userRoadmapDO.setStatus("IN_PROGRESS");
        userRoadmapDO.setStartedAt(LocalDateTime.now());

        userRoadmapMapper.insert(userRoadmapDO);
        return true;
    }

    /**
     * 获取用户的路线图学习进度
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     * @return 学习进度记录，如果不存在返回null
     */
    public UserRoadmapDTO getUserRoadmap(Long userId, Long roadmapId) {
        UserRoadmapDO progressDO = userRoadmapMapper.getByUserAndRoadmap(userId, roadmapId);
        if (progressDO == null) {
            return null;
        }

        UserRoadmapDTO dto = Converter.INSTANCE.toUserRoadmapDTO(progressDO);

        // 批量查询 roadmap 信息
        RoadmapDO roadmapDO = roadmapMapper.get(roadmapId.intValue());
        if (roadmapDO != null) {
            RoadmapDTOV2 roadmapDTO = Converter.INSTANCE.toRoadmapDTOV2WithUser(roadmapDO, userMapper);
            dto.setRoadmap(roadmapDTO);
        }

        return dto;
    }

    /**
     * 获取用户所有路线图学习进度
     * @param userId 用户ID
     * @return 用户所有路线图学习进度列表
     */
    public List<UserRoadmapDTO> getUserAllRoadmap(Long userId) {
        List<UserRoadmapDO> progressList = userRoadmapMapper.getByUser(userId);
        if (progressList.isEmpty()) {
            return List.of();
        }

        // 提取所有 roadmap IDs
        List<Integer> roadmapIds = progressList.stream()
                .map(progress -> progress.getRoadmapId().intValue())
                .collect(Collectors.toList());

        // 批量查询 roadmap 信息
        List<RoadmapDO> roadmapDOList = roadmapMapper.getByIds(roadmapIds);
        Map<Integer, RoadmapDO> roadmapMap = roadmapDOList.stream()
                .collect(Collectors.toMap(RoadmapDO::getId, roadmap -> roadmap));

        // 提取所有 profession IDs
        List<Integer> professionIds = roadmapDOList.stream()
                .map(RoadmapDO::getProfessionId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询 profession 信息
        Map<Integer, ProfessionDO> professionMap = professionMapper.getMapByIds(professionIds);

        // 转换为 DTO 并填充 roadmap 信息
        return progressList.stream()
                .map(progressDO -> {
                    UserRoadmapDTO dto = Converter.INSTANCE.toUserRoadmapDTO(progressDO);
                    RoadmapDO roadmapDO = roadmapMap.get(progressDO.getRoadmapId().intValue());

                    if (roadmapDO != null) {
                        try {
                            roadmapDO.setContent(roadmapService.parseContentToGraphFormat(roadmapDO.getContent()));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        RoadmapDTOV2 roadmapDTO = Converter.INSTANCE.toRoadmapDTOV2WithUser(roadmapDO, userMapper);

                        // 设置 profession 信息
                        ProfessionDO professionDO = professionMap.get(roadmapDO.getProfessionId());
                        if (professionDO != null) {
                            ProfessionDTO professionDTO = Converter.INSTANCE.toProfessionDTO(professionDO);
                            roadmapDTO.setProfession(professionDTO);
                        }

                        dto.setRoadmap(roadmapDTO);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 更新路线图学习进度
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     * @param progressPercent 进度百分比
     * @return 更新后的学习进度记录
     */
    public UserRoadmapDTO updateProgress(Long userId, Long roadmapId, Integer progressPercent) {
        UserRoadmapDO progressDO = userRoadmapMapper.getByUserAndRoadmap(userId, roadmapId);

        if (progressDO == null) {
            throw new RuntimeException("路线图学习记录不存在");
        }

        progressDO.setProgressPercent(progressPercent);

        // 如果进度达到100%，标记为完成
        if (progressPercent >= 100) {
            progressDO.setStatus("COMPLETED");
            progressDO.setCompletedAt(LocalDateTime.now());
        } else if (progressPercent > 0) {
            progressDO.setStatus("IN_PROGRESS");
        }

        userRoadmapMapper.update(progressDO);

        return Converter.INSTANCE.toUserRoadmapDTO(progressDO);
    }

    /**
     * 删除路线图学习记录
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     */
    public void deleteRoadmap(Long userId, Long roadmapId) {
        userRoadmapMapper.deleteByUserAndRoadmap(userId, roadmapId);
    }
}
