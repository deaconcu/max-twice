package com.prosper.learn.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.application.assembler.CourseAssembler;
import com.prosper.learn.application.assembler.RoadmapAssembler;
import com.prosper.learn.application.converter.RoadmapConverter;
import com.prosper.learn.application.converter.UserLearningConverter;
import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.roadmap.RoadmapBriefDTO;
import com.prosper.learn.application.dto.response.userlearning.UserLearningDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.roadmap.RoadmapDO;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.content.toc.TocDomainService;
import com.prosper.learn.learning.enrollment.UserLearningDO;
import com.prosper.learn.learning.enrollment.UserLearningDomainService;
import com.prosper.learn.shared.common.utils.Utils;
import com.prosper.learn.shared.domain.Enums;
import com.prosper.learn.shared.domain.event.user.learning.LearningCancelledEvent;
import com.prosper.learn.shared.domain.event.user.learning.LearningStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.exception.StatusCode.COURSE_NOT_FOUND;

/**
 * 用户学习记录应用服务
 * 负责跨域协调、DTO转换、事件发布
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLearningService {

    private final UserLearningDomainService userLearningDomainService;
    private final UserLearningConverter userLearningConverter;
    private final CourseDataService courseDataService;
    private final RoadmapDataService roadmapDataService;
    private final CourseService courseService;
    private final CourseAssembler courseAssembler;
    private final TocDomainService tocDomainService;
    private final ObjectMapper objectMapper;
    private final RoadmapAssembler roadmapAssembler;
    private final ApplicationEventPublisher eventPublisher;

    // ========== Query 方法 ==========

    /**
     * 根据用户和对象获取学习记录（不带关联对象）
     */
    public UserLearningDTO<Object> getByUserAndType(Long userId, Enums.ContentType objectType, Long objectId) {
        UserLearningDO learning = userLearningDomainService.getByUserAndType(userId, objectType, objectId);
        return userLearningConverter.toDTO(learning);
    }

    /**
     * 根据用户和课程获取学习记录（带课程对象）
     * 内部通过 course.rootNodeId 查询 node 类型的学习记录
     */
    public UserLearningDTO<Object> getCourseWithObject(Long userId, Long courseId) {
        // 1. 获取课程的根节点ID
        CourseDO courseDO = courseDataService.getById(courseId);
        if (courseDO == null || courseDO.getRootNodeId() == null) {
            return null;
        }

        // 2. 查询节点学习记录（objectType=node, objectId=rootNodeId）
        UserLearningDO learning = userLearningDomainService.getByUserAndType(
            userId,
            Enums.ContentType.node,
            courseDO.getRootNodeId()
        );

        if (learning == null) {
            return null;
        }

        UserLearningDTO<Object> dto = userLearningConverter.toDTO(learning);

        // 3. 填充课程对象
        CourseBriefDTO courseDTO = courseAssembler.toBriefDTO(courseDO);
        dto.setObject(courseDTO);

        return dto;
    }

    /**
     * 根据用户和路径获取学习记录（带路径对象）
     */
    public UserLearningDTO<Object> getRoadmapWithObject(Long userId, Long roadmapId) {
        UserLearningDO learning = userLearningDomainService.getByUserAndType(userId, Enums.ContentType.roadmap, roadmapId);
        if (learning == null) {
            return null;
        }

        UserLearningDTO<Object> dto = userLearningConverter.toDTO(learning);

        // 填充路径对象
        RoadmapDO roadmapDO = roadmapDataService.getById(roadmapId);
        //RoadmapBriefDTO roadmapDTO = roadmapService.toBriefDTO(roadmapDO);
        dto.setObject(roadmapDO);

        return dto;
    }

    /**
     * 获取用户的学习记录列表（带关联对象）
     */
    public List<UserLearningDTO<Object>> getByUserWithObjects(Long userId, Enums.ContentType objectType, Byte state, Long lastId, int limit) {
        List<UserLearningDO> learnings = userLearningDomainService.getByUser(userId, objectType, state, lastId, limit);

        if (learnings.isEmpty()) {
            return List.of();
        }

        // 根据 objectType 批量查询关联对象
        if (objectType == Enums.ContentType.roadmap) {
            return toRoadmapListWithObjects(learnings);
        } else {
            // node 类型或混合类型，不填充对象
            return userLearningConverter.toDTO(learnings);
        }
    }

    /**
     * 获取用户所有正在学习的课程（带课程对象）
     * 查询 objectType=node 且 is_root_node=1 的学习记录
     *
     * @param userId 用户ID
     * @param state 状态过滤（null=全部, 1=进行中, 2=已完成）
     * @param lastId 分页游标（null=第一页）
     * @param limit 每页数量
     */
    public List<UserLearningDTO<Object>> getAllCoursesProgress(Long userId, Byte state, Long lastId, int limit) {
        // 查询课程类型的学习记录（is_root_node=1）
        List<UserLearningDO> learnings = userLearningDomainService.getCoursesByUser(userId, state, lastId, limit);

        if (learnings.isEmpty()) {
            return List.of();
        }

        // 提取所有 rootNodeId
        List<Long> rootNodeIds = learnings.stream()
            .map(UserLearningDO::getObjectId)
            .collect(Collectors.toList());

        // 批量查询课程（通过 rootNodeId）
        List<CourseDO> courses = courseDataService.getByRootNodeIds(rootNodeIds);
        List<CourseBriefDTO> courseDTOs = courseAssembler.toBriefDTO(courses);

        // 建立 rootNodeId → Course 映射
        Map<Long, CourseBriefDTO> rootNodeToCourseMap = new HashMap<>();
        for (int i = 0; i < courses.size(); i++) {
            rootNodeToCourseMap.put(courses.get(i).getRootNodeId(), courseDTOs.get(i));
        }

        // 组装 DTO
        List<UserLearningDTO<Object>> result = new ArrayList<>();
        for (UserLearningDO learning : learnings) {
            UserLearningDTO<Object> dto = userLearningConverter.toDTO(learning);
            dto.setObject(rootNodeToCourseMap.get(learning.getObjectId()));
            result.add(dto);
        }
        return result;
    }

    /**
     * 根据父对象查询用户的路径学习记录（带 roadmap 对象）
     * 用于查询：某个 profession 下的 roadmap 学习记录
     */
    public List<UserLearningDTO<Object>> getRoadmapListByUserWithParent(Long userId, Long professionId, Byte state, Long lastId, int limit) {
        List<UserLearningDO> learnings = userLearningDomainService.getByUserAndTypeAndParent(
            userId, Enums.ContentType.roadmap, professionId, state, lastId, limit
        );

        if (learnings.isEmpty()) {
            return List.of();
        }

        return toRoadmapListWithObjects(learnings);
    }

    /**
     * 获取学习进度百分比
     */
    public Integer getProgress(long userId, Enums.ContentType objectType, Long objectId) {
        return userLearningDomainService.getProgress(userId, objectType, objectId);
    }

    /**
     * 获取课程学习进度百分比
     * 通过 courseId 查询，内部转换为 rootNodeId
     */
    public Integer getCourseProgress(long userId, Long courseId) {
        CourseDO course = courseDataService.getById(courseId);
        if (course == null || course.getRootNodeId() == null) {
            return 0;
        }
        return userLearningDomainService.getProgress(userId, Enums.ContentType.node, course.getRootNodeId());
    }

    /**
     * 检查是否正在学习
     */
    public boolean isLearning(long userId, Enums.ContentType objectType, long objectId) {
        return userLearningDomainService.isLearning(userId, objectType, objectId);
    }

    /**
     * 检查是否正在学习课程
     * 通过 courseId 查询，内部转换为 rootNodeId
     */
    public boolean isLearningCourse(long userId, long courseId) {
        CourseDO course = courseDataService.getById(courseId);
        if (course == null || course.getRootNodeId() == null) {
            return false;
        }
        return userLearningDomainService.isLearning(userId, Enums.ContentType.node, course.getRootNodeId());
    }

    // ========== Command 方法 ==========

    /**
     * 开始学习（发布学习开始事件）
     */
    public void startLearning(Long userId, Enums.ContentType objectType, Long objectId) {
        // 默认 isRootNode=FALSE（普通节点或路线图）
        userLearningDomainService.startLearning(userId, objectType, objectId, Enums.Bool.FALSE.value());

        // 如果是学习节点，初始化 nodes 字段
        if (objectType == Enums.ContentType.node) {
            try {
                Set<Long> nodeIds = tocDomainService.getNodeIdsInToc(userId, objectId, 1);
                if (!nodeIds.isEmpty()) {
                    String nodesJson = Utils.nodeIdsToJsonArray(nodeIds);
                    userLearningDomainService.updateNodes(userId, objectType, objectId, nodesJson);
                    log.info("初始化节点 {} 的节点列表: {} 个节点", objectId, nodeIds.size());
                }
            } catch (Exception e) {
                log.error("初始化 nodes 字段失败", e);
            }
        }

        // 发布学习开始事件
        eventPublisher.publishEvent(new LearningStartedEvent(userId, objectId, objectType));

        log.info("用户 {} 开始学习 {}(type={})", userId, objectId, objectType);
    }

    /**
     * 开始学习课程
     * 接收 courseId，内部转换为 rootNodeId，并标记 isRootNode=TRUE
     */
    public void startLearningCourse(Long userId, Long courseId) {
        // 1. 获取课程的根节点ID
        CourseDO course = courseDataService.getById(courseId);
        if (course == null || course.getRootNodeId() == null) {
            throw COURSE_NOT_FOUND.exception();
        }

        // 2. 开始学习节点（objectType=node, objectId=rootNodeId, isRootNode=TRUE）
        userLearningDomainService.startLearning(userId, Enums.ContentType.node, course.getRootNodeId(), Enums.Bool.TRUE.value());

        // 3. 初始化 nodes 字段（从第一个目录中提取所有节点ID）
        try {
            Set<Long> nodeIds = tocDomainService.getNodeIdsInToc(userId, course.getRootNodeId(), 1);

            if (!nodeIds.isEmpty()) {
                // 转换为JSON数组字符串 "[1,2,3]"
                String nodesJson = Utils.nodeIdsToJsonArray(nodeIds);

                // 更新 nodes 字段
                userLearningDomainService.updateNodes(
                    userId,
                    Enums.ContentType.node,
                    course.getRootNodeId(),
                    nodesJson
                );

                log.info("初始化课程 {} 的节点列表: {} 个节点", courseId, nodeIds.size());
            }
        } catch (Exception e) {
            log.error("初始化 nodes 字段失败", e);
        }

        // 4. 发布学习开始事件（事件中使用 courseId）
        eventPublisher.publishEvent(new LearningStartedEvent(userId, courseId, Enums.ContentType.course));

        log.info("用户 {} 开始学习课程 {} (rootNodeId={})", userId, courseId, course.getRootNodeId());
    }

    /**
     * 取消学习
     */
    public void cancelLearning(Long userId, Enums.ContentType objectType, Long objectId) {
        userLearningDomainService.cancelLearning(userId, objectType, objectId);
    }

    /**
     * 取消学习课程
     * 接收 courseId，内部转换为 rootNodeId
     */
    public void cancelLearningCourse(Long userId, Long courseId) {
        // 1. 获取课程的根节点ID
        CourseDO course = courseDataService.getById(courseId);
        if (course == null || course.getRootNodeId() == null) {
            throw COURSE_NOT_FOUND.exception();
        }

        // 2. 取消学习节点
        userLearningDomainService.cancelLearning(userId, Enums.ContentType.node, course.getRootNodeId());

        // 3. 发布学习取消事件（事件中使用 courseId）
        eventPublisher.publishEvent(new LearningCancelledEvent(userId, courseId, Enums.ContentType.course));

        log.info("用户 {} 取消学习课程 {} (rootNodeId={})", userId, courseId, course.getRootNodeId());
    }

    /**
     * 更新学习进度
     */
    public void updateProgress(Long userId, Enums.ContentType objectType, Long objectId, Integer progressPercent) {
        userLearningDomainService.updateProgress(userId, objectType, objectId, progressPercent);
    }

    /**
     * 更新课程学习进度
     * 接收 courseId，内部转换为 rootNodeId
     */
    public void updateCourseProgress(Long userId, Long courseId, Integer progressPercent) {
        // 1. 获取课程的根节点ID
        CourseDO course = courseDataService.getById(courseId);
        if (course == null || course.getRootNodeId() == null) {
            throw COURSE_NOT_FOUND.exception();
        }

        // 2. 更新节点学习进度
        userLearningDomainService.updateProgress(userId, Enums.ContentType.node, course.getRootNodeId(), progressPercent);

        log.info("用户 {} 更新课程 {} 进度为 {}", userId, courseId, progressPercent);
    }

    // ========== Private 辅助方法 ==========

    /**
     * 转换为路径学习记录列表（带路径对象）
     */
    private List<UserLearningDTO<Object>> toRoadmapListWithObjects(List<UserLearningDO> learnings) {
        // 提取所有路径 IDs
        List<Long> roadmapIds = learnings.stream()
            .map(UserLearningDO::getObjectId)
            .collect(Collectors.toList());

        // 批量查询路径并转换为 DTO
        List<RoadmapDO> roadmaps = roadmapDataService.getByIds(roadmapIds);
        List<RoadmapBriefDTO> roadmapDTOs = roadmapAssembler.toBriefDTO(roadmaps);
        Map<Long, RoadmapBriefDTO> roadmapMap = roadmapDTOs.stream()
            .collect(Collectors.toMap(RoadmapBriefDTO::getId, r -> r));

        // 组装 DTO
        List<UserLearningDTO<Object>> result = new ArrayList<>();
        for (UserLearningDO learning : learnings) {
            UserLearningDTO<Object> dto = userLearningConverter.toDTO(learning);
            dto.setObject(roadmapMap.get(learning.getObjectId()));
            result.add(dto);
        }
        return result;
    }
}
