package com.prosper.learn.application.service;

import com.prosper.learn.application.converter.ProfessionConverter;
import com.prosper.learn.application.converter.RoadmapConverter;
import com.prosper.learn.application.converter.UserConverter;
import com.prosper.learn.application.converter.UserRoadmapConverter;
import com.prosper.learn.application.dto.response.roadmap.RoadmapBriefDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapWithStatusDTO;
import com.prosper.learn.application.dto.response.userroadmap.UserRoadmapSummaryDTO;
import com.prosper.learn.application.dto.response.userroadmap.UserRoadmapWithBriefDTO;
import com.prosper.learn.application.dto.response.userroadmap.UserRoadmapWithDetailDTO;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.learning.enrollment.UserRoadmapDO;
import com.prosper.learn.learning.enrollment.UserRoadmapDataService;
import com.prosper.learn.learning.enrollment.UserRoadmapDomainService;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import com.prosper.learn.user.profile.UserDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoadmapService {

    private final UserRoadmapDomainService userRoadmapDomainService;
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

    // ========== DTO转换方法 ==========

    /**
     * 转换为摘要 DTO（基础信息，不含路线图详情）
     */
    public UserRoadmapSummaryDTO toSummaryDTO(UserRoadmapDO userRoadmapDO) {
        return userRoadmapConverter.toSummaryDTO(userRoadmapDO);
    }

    public List<UserRoadmapSummaryDTO> toSummaryDTO(List<UserRoadmapDO> userRoadmapDOList) {
        return userRoadmapConverter.toSummaryDTO(userRoadmapDOList);
    }

    /**
     * 转换为含路线图详细信息的 DTO（单个）
     */
    public UserRoadmapWithDetailDTO toWithDetailDTO(UserRoadmapDO userRoadmapDO, Long userId) {
        if (userRoadmapDO == null) return null;

        UserRoadmapWithDetailDTO dto = userRoadmapConverter.toWithDetailDTO(userRoadmapDO);
        RoadmapWithStatusDTO roadmapDTO = roadmapService.getById(userRoadmapDO.getRoadmapId(), userId);
        dto.setRoadmap(roadmapDTO);
        return dto;
    }

    /**
     * 转换为含路线图简要信息的 DTO（批量）
     * 用于学习进度列表,不包含完整路线图内容和进度计算
     */
    public List<UserRoadmapWithBriefDTO> toWithBriefDTO(List<UserRoadmapDO> userRoadmapList) {
        if (userRoadmapList.isEmpty()) {
            return List.of();
        }

        // 提取所有 roadmap IDs
        List<Long> roadmapIds = userRoadmapList.stream()
                .map(UserRoadmapDO::getRoadmapId)
                .distinct()
                .toList();

        // 批量查询 roadmap 信息
        List<RoadmapDO> roadmapDOList = roadmapDataService.getByIds(roadmapIds);

        // 提取所有 profession IDs
        List<Long> professionIds = roadmapDOList.stream()
                .map(RoadmapDO::getProfessionId)
                .distinct()
                .toList();

        // 批量查询 profession 信息
        Map<Long, String> professionNameMap = professionDataService.getByIds(professionIds).stream()
                .collect(Collectors.toMap(
                    profession -> profession.getId(),
                    profession -> profession.getName()
                ));

        // 构建 roadmap ID -> RoadmapBriefDTO 映射
        Map<Long, RoadmapBriefDTO> roadmapBriefMap = roadmapDOList.stream()
                .collect(Collectors.toMap(
                    RoadmapDO::getId,
                    roadmapDO -> {
                        RoadmapBriefDTO brief = new RoadmapBriefDTO();
                        brief.setId(roadmapDO.getId());
                        brief.setProfessionName(professionNameMap.get(roadmapDO.getProfessionId()));
                        brief.setNodeCount(roadmapDO.getNodeCount());
                        return brief;
                    }
                ));

        // 转换为 DTO 并填充路线图信息
        return userRoadmapList.stream()
                .map(userRoadmapDO -> {
                    UserRoadmapWithBriefDTO dto = userRoadmapConverter.toWithBriefDTO(userRoadmapDO);
                    RoadmapBriefDTO roadmapBrief = roadmapBriefMap.get(userRoadmapDO.getRoadmapId());
                    dto.setRoadmap(roadmapBrief);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换为含路线图详细信息的 DTO（批量）
     */
    public List<UserRoadmapWithDetailDTO> toWithDetailDTO(List<UserRoadmapDO> userRoadmapList, long userId) {
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
                } catch (Exception e) {
                    // 解析失败时继续处理其他路线图
                    log.error("Failed to parse roadmap content: roadmapId={}", userRoadmapDO.getRoadmapId(), e);
                }
            }
        }

        // 批量更新数据库
        if (!toUpdateList.isEmpty()) {
            userRoadmapDomainService.updateBatch(toUpdateList);
        }

        // 转换为 DTO 并填充 roadmap 信息
        return userRoadmapList.stream()
                .map(userRoadmapDO -> {
                    UserRoadmapWithDetailDTO dto = userRoadmapConverter.toWithDetailDTO(userRoadmapDO);
                    RoadmapDO roadmapDO = roadmapMap.get(userRoadmapDO.getRoadmapId());

                    if (roadmapDO != null) {
                        RoadmapWithStatusDTO roadmapDTO = roadmapService.toRoadmapWithStatus(roadmapDO, userId);

                        // 检查是否被屏蔽或拒绝
                        if (roadmapDO.getState() == ContentState.REJECTED.value() ||
                            roadmapDO.getState() == ContentState.BANNED.value()) {
                            roadmapDTO.setAvailable(false);
                        }

                        dto.setRoadmap(roadmapDTO);
                    } else {
                        // roadmap 已被删除，创建占位DTO
                        RoadmapWithStatusDTO placeholderDTO = new RoadmapWithStatusDTO();
                        placeholderDTO.setId(userRoadmapDO.getRoadmapId());
                        placeholderDTO.setAvailable(false);
                        dto.setRoadmap(placeholderDTO);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }


    // ========== 私有辅助方法 ==========

    /**
     * 验证用户ID
     */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw StatusCode.NOT_FOUND.exception("用户不存在");
        }
    }

    /**
     * 验证路线图ID
     */
    private void validateRoadmapId(Long roadmapId) {
        if (roadmapId == null || roadmapId <= 0) {
            throw StatusCode.ROADMAP_NOT_FOUND.exception();
        }
    }

// --注释掉检查 START (2025/12/10 11:32):
//    /**
//     * 验证进度百分比
//     */
//    private void validateProgressPercent(int progressPercent) {
//        int threshold = systemProperties.getRoadmap().getCompletionThreshold();
//        if (progressPercent < 0 || progressPercent > threshold) {
//            throw ErrorCode.USER_COURSE_PROGRESS_INVALID.exception();
//        }
//    }
// --注释掉检查 STOP (2025/12/10 11:32)

    // ========== Command 方法（写操作）==========

    /**
     * 用户开始学习路线图
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     */
    public void startRoadmap(Long userId, Long roadmapId) {
        validateUserId(userId);
        validateRoadmapId(roadmapId);

        // 委托给领域服务处理核心逻辑
        userRoadmapDomainService.startRoadmap(userId, roadmapId);
    }

    /**
     * 取消学习路线图
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     */
    public void cancelRoadmap(Long userId, Long roadmapId) {
        validateUserId(userId);
        validateRoadmapId(roadmapId);

        // 委托给领域服务处理核心逻辑
        userRoadmapDomainService.cancelRoadmap(userId, roadmapId);
    }

    /**
     * 更新路线图学习进度
     * @param userId 用户ID
     * @param roadmapId 路线图ID
     * @param progressPercent 进度百分比
     * @return 更新后的学习进度记录
     */
    public UserRoadmapSummaryDTO updateProgress(Long userId, Long roadmapId, Integer progressPercent) {
        // 委托给领域服务处理核心逻辑
        UserRoadmapDO userRoadmapDO = userRoadmapDomainService.updateProgress(userId, roadmapId, progressPercent);

        // DTO转换
        return toSummaryDTO(userRoadmapDO);
    }

    // ========== Query 方法（读操作）==========

    /**
     * 获取用户的路线图学习进度
     */
    public UserRoadmapWithDetailDTO getUserRoadmap(Long userId, Long roadmapId) {
        validateUserId(userId);
        validateRoadmapId(roadmapId);

        // 委托给领域服务获取数据
        UserRoadmapDO userRoadmapDO = userRoadmapDomainService.getByUserAndRoadmap(userId, roadmapId);

        if (userRoadmapDO == null) {
            throw StatusCode.USER_ROADMAP_NOT_FOUND.exception();
        }

        // DTO转换和跨域数据填充
        return toWithDetailDTO(userRoadmapDO, userId);
    }

    /**
     * 获取用户的全部路线图学习进度（含简要路线图信息）
     * @param userId 用户ID
     * @return 路线图进度列表（含简要信息）
     */
    public List<UserRoadmapWithBriefDTO> getUserAllRoadmapBrief(Long userId) {
        // 委托给领域服务获取数据
        List<UserRoadmapDO> userRoadmapList = userRoadmapDomainService.getByUser(userId);

        // DTO转换和跨域数据填充
        return toWithBriefDTO(userRoadmapList);
    }

    /**
     * 获取用户的全部路线图学习进度
     * @param userId
     * @return
     */
    public List<UserRoadmapWithDetailDTO> getUserAllRoadmap(Long userId) {
        // 委托给领域服务获取数据
        List<UserRoadmapDO> userRoadmapList = userRoadmapDomainService.getByUser(userId);

        // DTO转换和跨域数据填充
        return toWithDetailDTO(userRoadmapList, userId);
    }

    /**
     * 检查路线图完成状态并收集需要更新的记录
     * @param userRoadmapDO 用户路线图进度记录
     * @param content 已解析的路线图内容（包含课程进度信息）
     * @param toUpdateList 需要更新的记录列表
     * @return 是否有状态更新
     */
    private boolean checkAndCollectRoadmapUpdate(UserRoadmapDO userRoadmapDO, String content, List<UserRoadmapDO> toUpdateList) {
        // 委托给领域服务进行进度计算和状态更新
        boolean needUpdate = userRoadmapDomainService.updateRoadmapProgressFromContent(userRoadmapDO, content);

        if (needUpdate) {
            toUpdateList.add(userRoadmapDO);
        }

        return needUpdate;
    }

}
