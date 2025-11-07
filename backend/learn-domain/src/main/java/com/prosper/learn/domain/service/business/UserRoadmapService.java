package com.prosper.learn.domain.service.business;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;

import static com.prosper.learn.common.Enums.UserProgressState;

import com.prosper.learn.domain.util.converter.ProfessionConverter;
import com.prosper.learn.domain.util.converter.RoadmapConverter;
import com.prosper.learn.domain.util.converter.UserConverter;
import com.prosper.learn.domain.util.converter.UserRoadmapConverter;
import com.prosper.learn.dto.response.RoadmapDTO;
import com.prosper.learn.dto.response.UserRoadmapDTO;
import com.prosper.learn.persistence.dataobject.RoadmapDO;
import com.prosper.learn.persistence.dataobject.UserRoadmapDO;
import com.prosper.learn.domain.service.data.ProfessionDataService;
import com.prosper.learn.domain.service.data.RoadmapDataService;
import com.prosper.learn.domain.service.data.UserRoadmapDataService;
import com.prosper.learn.domain.service.data.UserDataService;
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

    private final UserRoadmapDataService userRoadmapDataService;
    private final RoadmapDataService roadmapDataService;
    private final UserDataService userDataService;
    private final RoadmapService roadmapService;
    private final ProfessionDataService professionDataService;
    private final SystemProperties systemProperties;
    private final UserRoadmapConverter userRoadmapConverter;
    private final RoadmapConverter roadmapConverter;
    private final UserConverter userConverter;
    private final ProfessionConverter professionConverter;

    // 不变常量 - 进度相关
    private static final int INITIAL_PROGRESS = 0;
    private static final double COMPLETE_PROGRESS = 100.0;
    private static final double PROGRESS_PRECISION = 100.0;
    
    // ========== DTO转换方法 ==========
    
    /**
     * 转换单个对象为DTO
     */
    public UserRoadmapDTO toDTO(UserRoadmapDO userRoadmapDO) {
        return userRoadmapConverter.toDTO(userRoadmapDO);
    }
    
    /**
     * 转换列表为DTO列表
     */
    public List<UserRoadmapDTO> toDTO(List<UserRoadmapDO> userRoadmapDOList) {
        return userRoadmapConverter.toDTO(userRoadmapDOList);
    }

    /**
     * v1 = v0 + roadmap
     */
    public UserRoadmapDTO toDTOV1(UserRoadmapDO userRoadmapDO, Long userId) {
        UserRoadmapDTO userRoadmapDTO = toDTO(userRoadmapDO);
        RoadmapDTO roadmapDTO = roadmapService.getById(userRoadmapDO.getRoadmapId(), userId);
        userRoadmapDTO.setRoadmap(roadmapDTO);
        return userRoadmapDTO;
    }

    /**
     * 转换列表为DTO列表
     */
    public List<UserRoadmapDTO> toDTOV1(List<UserRoadmapDO> userRoadmapList, long userId) {
        if (userRoadmapList.isEmpty()) {
            return List.of();
        }

        // 提取所有 roadmap IDs
        List<Long> roadmapIds = userRoadmapList.stream()
                .map(userRoadMap -> userRoadMap.getRoadmapId().longValue())
                .collect(Collectors.toList());

        // 批量查询 roadmap 信息
        List<RoadmapDO> roadmapDOList = roadmapDataService.getByIds(roadmapIds);
        Map<Long, RoadmapDO> roadmapMap = roadmapDOList.stream()
                .collect(Collectors.toMap(RoadmapDO::getId, roadmap -> roadmap));

        // 需要更新的路线图记录列表
        List<UserRoadmapDO> toUpdateList = new ArrayList<>();

        // 先批量检查和收集所有需要更新的路线图状态
        for (UserRoadmapDO userRoadmapDO : userRoadmapList) {
            RoadmapDO roadmapDO = roadmapMap.get(userRoadmapDO.getRoadmapId());
            if (roadmapDO != null) {
                try {
                    String parsedContent = roadmapService.parseContentToGraphFormat(roadmapDO.getContent(), userId);
                    checkAndCollectRoadmapUpdate(userRoadmapDO, parsedContent, toUpdateList);
                    // 将解析后的内容设置回去，避免重复解析
                    // roadmapDO.setContent(parsedContent);
                } catch (Exception e) {
                    // 解析失败时继续处理其他路线图
                    log.error("Failed to parse roadmap content: roadmapId={}", userRoadmapDO.getRoadmapId(), e);
                }
            }
        }

        // 批量更新数据库
        if (!toUpdateList.isEmpty()) {
            userRoadmapDataService.updateBatch(toUpdateList);
        }

        // 转换为 DTO 并填充 roadmap 信息，过滤掉 roadmap 已被删除的记录
        return userRoadmapList.stream()
                .map(userRoadmapDO -> {
                    UserRoadmapDTO dto = toDTO(userRoadmapDO);
                    RoadmapDO roadmapDO = roadmapMap.get(userRoadmapDO.getRoadmapId());

                    if (roadmapDO != null) {
                        // 这里的content已经在上面解析过了
                        RoadmapDTO roadmapDTO = roadmapService.toDTOV1(roadmapDO, userId);
                        dto.setRoadmap(roadmapDTO);
                        return dto;
                    }
                    // roadmap 已被删除，返回 null
                    return null;
                })
                .filter(dto -> dto != null && dto.getRoadmap() != null) // 过滤掉 roadmap 为 null 的记录
                .collect(Collectors.toList());
    }


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
        userRoadmapDO.setState(UserProgressState.IN_PROGRESS.value());
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
            UserRoadmapDO existing = userRoadmapDataService.getByUserAndRoadmap(userId, roadmapId);

            if (existing != null) {
                // 如果已存在，删除现有记录重新开始
                userRoadmapDataService.deleteByUserAndRoadmap(userId, roadmapId);
                return false;
            }

            // 创建新的学习记录
            UserRoadmapDO userRoadmapDO = createInitialUserRoadmap(userId, roadmapId);
            userRoadmapDataService.insert(userRoadmapDO);
            
            log.info("用户 {} 开始学习路线图 {}", userId, roadmapId);
            return true;
            
        } catch (Exception e) {
            log.error("用户开始学习路线图失败: userId={}, roadmapId={}", userId, roadmapId, e);
            throw ErrorCode.USER_ROADMAP_NOT_FOUND.exception(e);
        }
    }

    /**
     * 获取用户的路线图学习进度
     */
    public UserRoadmapDTO getUserRoadmap(Long userId, Long roadmapId) {
        validateUserId(userId);
        validateRoadmapId(roadmapId);

        UserRoadmapDO userRoadmapDO = userRoadmapDataService.getByUserAndRoadmap(userId, roadmapId);
        return toDTOV1(userRoadmapDO, userId);
    }

    /**
     * 获取用户的全部路线图学习进度
     * @param userId
     * @return
     */
    public List<UserRoadmapDTO> getUserAllRoadmap(Long userId) {
        List<UserRoadmapDO> userRoadmapList = userRoadmapDataService.getByUser(userId);
        return toDTOV1(userRoadmapList, userId);
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
            if (UserProgressState.COMPLETED.value() == userRoadmapDO.getState()) {
                return false;
            }
            
            // 解析content获取节点信息
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(content);
            JsonNode nodesNode = rootNode.get("nodes");
            
            if (nodesNode != null && nodesNode.isArray()) {
                int totalCourses = 0;
                int completedCourses = 0;
                double totalProgress = 0.0;
                
                for (JsonNode node : nodesNode) {
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
                    userRoadmapDO.setState(UserProgressState.COMPLETED.value());
                    userRoadmapDO.setProgressPercent(100);
                    if (userRoadmapDO.getCompletedAt() == null) {
                        userRoadmapDO.setCompletedAt(LocalDateTime.now());
                    }
                    needUpdate = true;
                } else if (overallProgress > 0 && UserProgressState.NOT_STARTED.value() == userRoadmapDO.getState()) {
                    // 如果有进度但状态还是NOT_STARTED，更新为IN_PROGRESS
                    userRoadmapDO.setState(UserProgressState.IN_PROGRESS.value());
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
        UserRoadmapDO userRoadmapDO = userRoadmapDataService.getByUserAndRoadmap(userId, roadmapId);

        if (userRoadmapDO == null) {
            throw ErrorCode.USER_ROADMAP_NOT_FOUND.exception();
        }

        userRoadmapDO.setProgressPercent(progressPercent);

        // 如果进度达到100%，标记为完成
        if (progressPercent >= 100) {
            userRoadmapDO.setState(UserProgressState.COMPLETED.value());
            userRoadmapDO.setCompletedAt(LocalDateTime.now());
        } else if (progressPercent > 0) {
            userRoadmapDO.setState(UserProgressState.IN_PROGRESS.value());
        }

        userRoadmapDataService.update(userRoadmapDO);
        return toDTO(userRoadmapDO);
    }

    /**
     * 删除路线图学习记录
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     */
    public void deleteRoadmap(Long userId, Long roadmapId) {
        userRoadmapDataService.deleteByUserAndRoadmap(userId, roadmapId);
    }
}
