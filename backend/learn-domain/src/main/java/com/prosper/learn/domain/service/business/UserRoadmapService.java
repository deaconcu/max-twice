package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.config.SystemProperties;

import static com.prosper.learn.common.Enums.UserRoadmapState;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoadmapService {

    private final UserRoadmapMapper userRoadmapMapper;
    private final RoadmapMapper roadmapMapper;
    private final UserMapper userMapper;
    private final RoadmapService roadmapService;
    private final ProfessionMapper professionMapper;
    private final SystemProperties systemProperties;

    // 不变常量 - 进度相关
    private static final int INITIAL_PROGRESS = 0;
    private static final double COMPLETE_PROGRESS = 100.0;
    private static final double PROGRESS_PRECISION = 100.0;

    // ========== 私有辅助方法 ==========

    /**
     * 验证用户ID
     */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw ErrorCode.USER_NOT_FOUND.exception();
        }
    }

    /**
     * 验证路线图ID
     */
    private void validateRoadmapId(Long roadmapId) {
        if (roadmapId == null || roadmapId <= 0) {
            throw ErrorCode.ROADMAP_NOT_FOUND.exception();
        }
    }

    /**
     * 验证进度百分比
     */
    private void validateProgressPercent(int progressPercent) {
        int threshold = systemProperties.getRoadmap().getCompletionThreshold();
        if (progressPercent < 0 || progressPercent > threshold) {
            throw ErrorCode.USER_COURSE_PROGRESS_INVALID.exception();
        }
    }

    /**
     * 创建初始用户路线图记录
     */
    private UserRoadmapDO createInitialUserRoadmap(Long userId, Long roadmapId) {
        UserRoadmapDO userRoadmapDO = new UserRoadmapDO();
        userRoadmapDO.setUserId(userId);
        userRoadmapDO.setRoadmapId(roadmapId);
        userRoadmapDO.setProgressPercent(INITIAL_PROGRESS);
        userRoadmapDO.setState(UserRoadmapState.IN_PROGRESS.value());
        userRoadmapDO.setStartedAt(LocalDateTime.now());
        return userRoadmapDO;
    }

    // ========== 公共业务方法 ==========

    /**
     * 用户开始学习路线图
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     * @return 学习进度记录
     */
    public boolean startRoadmap(Long userId, Long roadmapId) {
        validateUserId(userId);
        validateRoadmapId(roadmapId);
        
        try {
            // 检查是否已经存在学习记录
            UserRoadmapDO existing = userRoadmapMapper.getByUserAndRoadmap(userId, roadmapId);

            if (existing != null) {
                // 如果已存在，删除现有记录重新开始
                userRoadmapMapper.deleteByUserAndRoadmap(userId, roadmapId);
                return false;
            }

            // 创建新的学习记录
            UserRoadmapDO userRoadmapDO = createInitialUserRoadmap(userId, roadmapId);
            userRoadmapMapper.insert(userRoadmapDO);
            
            log.info("用户 {} 开始学习路线图 {}", userId, roadmapId);
            return true;
            
        } catch (Exception e) {
            log.error("用户开始学习路线图失败: userId={}, roadmapId={}", userId, roadmapId, e);
            throw ErrorCode.USER_ROADMAP_NOT_FOUND.exception(e);
        }
    }

    /**
     * 获取用户的路线图学习进度
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     * @return 学习进度记录，如果不存在返回null
     */
    public UserRoadmapDTO getUserRoadmap(Long userId, Long roadmapId) {
        validateUserId(userId);
        validateRoadmapId(roadmapId);
        
        try {
            UserRoadmapDO progressDO = userRoadmapMapper.getByUserAndRoadmap(userId, roadmapId);
            if (progressDO == null) {
                return null;
            }

            UserRoadmapDTO dto = Converter.INSTANCE.toUserRoadmapDTO(progressDO);

            // 批量查询 roadmap 信息
            RoadmapDO roadmapDO = roadmapMapper.getById(roadmapId.intValue());
            if (roadmapDO != null) {
                RoadmapDTOV2 roadmapDTO = Converter.INSTANCE.toRoadmapDTOV2WithUser(roadmapDO, userMapper);
                dto.setRoadmap(roadmapDTO);
            }

            return dto;
            
        } catch (Exception e) {
            log.error("获取用户路线图进度失败: userId={}, roadmapId={}", userId, roadmapId, e);
            throw ErrorCode.USER_ROADMAP_NOT_FOUND.exception(e);
        }
    }

    /**
     * 获取用户所有路线图学习进度
     * @param userId 用户ID
     * @return 用户所有路线图学习进度列表
     */
    public List<UserRoadmapDTO> getUserAllRoadmap(Long userId) {
        List<UserRoadmapDO> userRoadmapList = userRoadmapMapper.getByUser(userId);
        if (userRoadmapList.isEmpty()) {
            return List.of();
        }

        // 提取所有 roadmap IDs
        List<Long> roadmapIds = userRoadmapList.stream()
                .map(progress -> progress.getRoadmapId().longValue())
                .collect(Collectors.toList());

        // 批量查询 roadmap 信息
        List<RoadmapDO> roadmapDOList = roadmapMapper.getByIds(roadmapIds);
        Map<Long, RoadmapDO> roadmapMap = roadmapDOList.stream()
                .collect(Collectors.toMap(RoadmapDO::getId, roadmap -> roadmap));

        // 提取所有 profession IDs
        List<Long> professionIds = roadmapDOList.stream()
                .map(RoadmapDO::getProfessionId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询 profession 信息
        Map<Integer, ProfessionDO> professionMap = professionMapper.getMapByIds(professionIds);

        // 需要更新的路线图记录列表
        List<UserRoadmapDO> toUpdateList = new ArrayList<>();
        
        // 先批量检查和收集所有需要更新的路线图状态
        for (UserRoadmapDO progressDO : userRoadmapList) {
            RoadmapDO roadmapDO = roadmapMap.get(progressDO.getRoadmapId().intValue());
            if (roadmapDO != null) {
                try {
                    String parsedContent = roadmapService.parseContentToGraphFormat(roadmapDO.getContent(), userId.intValue());
                    checkAndCollectRoadmapUpdate(progressDO, parsedContent, toUpdateList);
                    // 将解析后的内容设置回去，避免重复解析
                    roadmapDO.setContent(parsedContent);
                } catch (Exception e) {
                    // 解析失败时继续处理其他路线图
                    log.error("Failed to parse roadmap content: roadmapId={}", progressDO.getRoadmapId(), e);
                }
            }
        }
        
        // 批量更新数据库
        if (!toUpdateList.isEmpty()) {
            userRoadmapMapper.updateBatch(toUpdateList);
        }

        // 转换为 DTO 并填充 roadmap 信息
        return userRoadmapList.stream()
                .map(progressDO -> {
                    UserRoadmapDTO dto = Converter.INSTANCE.toUserRoadmapDTO(progressDO);
                    RoadmapDO roadmapDO = roadmapMap.get(progressDO.getRoadmapId().intValue());

                    if (roadmapDO != null) {
                        // 这里的content已经在上面解析过了
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
                }).collect(Collectors.toList());
    }

    /**
     * 检查路线图完成状态并收集需要更新的记录
     * @param userRoadmapDO 用户路线图进度记录
     * @param content 已解析的路线图内容（包含课程进度信息）
     * @param toUpdateList 需要更新的记录列表
     * @return 是否有状态更新
     */
    private boolean checkAndCollectRoadmapUpdate(UserRoadmapDO userRoadmapDO, String content, List<UserRoadmapDO> toUpdateList) {
        try {
            // 如果已经是COMPLETED状态，无需再检查
            if (UserRoadmapState.COMPLETED.value() == userRoadmapDO.getState()) {
                return false;
            }
            
            // 解析content获取节点信息
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(content);
            com.fasterxml.jackson.databind.JsonNode nodesNode = rootNode.get("nodes");
            
            if (nodesNode != null && nodesNode.isArray()) {
                int totalCourses = 0;
                int completedCourses = 0;
                double totalProgress = 0.0;
                
                for (com.fasterxml.jackson.databind.JsonNode node : nodesNode) {
                    totalCourses++;
                    boolean finished = node.get("finished").asBoolean(false);
                    double progress = node.get("progress").asDouble(0.0);
                    
                    if (finished) {
                        completedCourses++;
                        totalProgress += 100.0;
                    } else {
                        totalProgress += progress;
                    }
                }
                
                // 计算整体完成度
                double overallProgress = totalCourses > 0 ? totalProgress / totalCourses : 0.0;
                
                boolean needUpdate = false;
                
                // 如果完成度达到100%，更新状态为COMPLETED
                if (overallProgress >= 100.0) {
                    userRoadmapDO.setState(UserRoadmapState.COMPLETED.value());
                    userRoadmapDO.setProgressPercent(100);
                    if (userRoadmapDO.getCompletedAt() == null) {
                        userRoadmapDO.setCompletedAt(LocalDateTime.now());
                    }
                    needUpdate = true;
                } else if (overallProgress > 0 && UserRoadmapState.NOT_STARTED.value() == userRoadmapDO.getState()) {
                    // 如果有进度但状态还是NOT_STARTED，更新为IN_PROGRESS
                    userRoadmapDO.setState(UserRoadmapState.IN_PROGRESS.value());
                    userRoadmapDO.setProgressPercent((int) Math.round(overallProgress));
                    needUpdate = true;
                }
                
                if (needUpdate) {
                    toUpdateList.add(userRoadmapDO);
                    return true;
                }
            }
        } catch (Exception e) {
            // 解析失败时不抛出异常，避免影响正常流程
            log.warn("Failed to parse roadmap content for completion check: roadmapId={}", 
                    userRoadmapDO.getRoadmapId(), e);
        }
        return false;
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
            throw ErrorCode.USER_ROADMAP_NOT_FOUND.exception();
        }

        progressDO.setProgressPercent(progressPercent);

        // 如果进度达到100%，标记为完成
        if (progressPercent >= 100) {
            progressDO.setState(UserRoadmapState.COMPLETED.value());
            progressDO.setCompletedAt(LocalDateTime.now());
        } else if (progressPercent > 0) {
            progressDO.setState(UserRoadmapState.IN_PROGRESS.value());
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
