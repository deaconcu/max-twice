package com.prosper.learn.application.assembler;

import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsDO;
import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.dto.response.course.*;
import com.prosper.learn.application.dto.response.user.UserBriefDTO;
import com.prosper.learn.application.service.BookmarkService;
import com.prosper.learn.application.service.UserService;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.learning.enrollment.UserLearningDO;
import com.prosper.learn.learning.enrollment.UserLearningDomainService;
import com.prosper.learn.shared.domain.Enums;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.ContentType;

/**
 * Course DTO 组装器
 * 负责将 CourseDO 转换为各种 DTO（需要联查数据库）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseAssembler {

    private final CourseConverter courseConverter;
    private final CourseDataService courseDataService;
    private final ContentStatsDataService contentStatsDataService;
    private final BookmarkService bookmarkService;
    private final UserLearningDomainService userLearningDomainService;
    private final UserService userService;

    // ========== Brief DTO ==========

    public CourseBriefDTO toBriefDTO(CourseDO courseDO) {
        return courseConverter.toBriefDTO(courseDO);
    }

    public List<CourseBriefDTO> toBriefDTO(List<CourseDO> courseDOList) {
        return courseConverter.toBriefDTO(courseDOList);
    }

    // ========== Summary DTO ==========

    public CourseSummaryDTO toSummaryDTO(CourseDO courseDO) {
        if (courseDO == null) return null;
        CourseSummaryDTO dto = courseConverter.toSummaryDTO(courseDO);
        fillParentCourse(dto, courseDO.getParentCourseId());
        return dto;
    }

    public List<CourseSummaryDTO> toSummaryDTO(List<CourseDO> courseDOList) {
        if (courseDOList == null || courseDOList.isEmpty()) {
            return List.of();
        }

        // 批量填充父课程信息
        Map<Long, CourseBriefDTO> parentCourseMap = getBatchParentCourses(courseDOList);

        return courseDOList.stream()
                .map(courseDO -> {
                    CourseSummaryDTO dto = courseConverter.toSummaryDTO(courseDO);
                    if (courseDO.getParentCourseId() != null && courseDO.getParentCourseId() > 0) {
                        dto.setParentCourse(parentCourseMap.get(courseDO.getParentCourseId()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ========== Full DTO ==========

    /**
     * 转换为课程完整信息 DTO（摘要 + 统计 + 进度）
     */
    public CourseFullDTO toFullDTO(CourseDO courseDO, Long userId) {
        if (courseDO == null) return null;

        CourseFullDTO dto = courseConverter.toFullDTO(courseDO);

        // 填充父课程信息
        fillParentCourse(dto, courseDO.getParentCourseId());

        // 填充统计信息
        try {
            ContentStatsDO stats = contentStatsDataService.getByContent(ContentType.course, courseDO.getId())
                .orElse(null);
            if (stats != null) {
                dto.setLearnerCount(stats.getLearnerCount());
                dto.setBookmarkCount(stats.getBookmarkCount());
            } else {
                dto.setLearnerCount(0);
                dto.setBookmarkCount(0);
            }
        } catch (Exception e) {
            log.warn("获取课程统计信息失败, courseId={}", courseDO.getId(), e);
            dto.setLearnerCount(0);
            dto.setBookmarkCount(0);
        }

        // 填充用户相关信息
        if (userId != null) {
            dto.setBookmarked(bookmarkService.isBookmarked(userId, courseDO.getId(), ContentType.course));
            Integer progress = userLearningDomainService.getProgress(userId, Enums.ContentType.node, courseDO.getRootNodeId());
            dto.setProgress(progress != null ? progress : 0);
        } else {
            dto.setBookmarked(false);
            dto.setProgress(0);
        }

        return dto;
    }

    /**
     * 批量转换为课程完整信息 DTO
     */
    public List<CourseFullDTO> toFullDTOList(List<CourseDO> courseDOList, Long userId) {
        if (courseDOList == null || courseDOList.isEmpty()) {
            return List.of();
        }

        // 批量查询所有课程的统计信息
        List<Long> courseIds = courseDOList.stream()
            .map(CourseDO::getId)
            .collect(Collectors.toList());

        Map<Long, ContentStatsDO> statsMap = getBatchStats(courseIds);

        // 批量查询用户收藏状态
        final Set<Long> bookmarkedCourseIds;
        if (userId != null) {
            List<Long> bookmarkedIds = bookmarkService.getBookmarkedIds(userId, courseIds, ContentType.course);
            bookmarkedCourseIds = new HashSet<>(bookmarkedIds);
        } else {
            bookmarkedCourseIds = new HashSet<>();
        }

        // 批量查询学习进度
        List<Long> rootNodeIds = courseDOList.stream()
            .map(CourseDO::getRootNodeId)
            .collect(Collectors.toList());
        final Map<Long, Integer> progressMap;
        if (userId != null) {
            progressMap = getBatchProgress(userId, rootNodeIds);
        } else {
            progressMap = new HashMap<>();
        }

        // 批量填充父课程信息
        Map<Long, CourseBriefDTO> parentCourseMap = getBatchParentCourses(courseDOList);

        // 转换并填充
        List<CourseFullDTO> result = new ArrayList<>();
        for (CourseDO courseDO : courseDOList) {
            CourseFullDTO dto = courseConverter.toFullDTO(courseDO);

            // 父课程
            if (courseDO.getParentCourseId() != null && courseDO.getParentCourseId() > 0) {
                dto.setParentCourse(parentCourseMap.get(courseDO.getParentCourseId()));
            }

            // 统计信息
            ContentStatsDO stats = statsMap.get(courseDO.getId());
            if (stats != null) {
                dto.setLearnerCount(stats.getLearnerCount());
                dto.setBookmarkCount(stats.getBookmarkCount());
            } else {
                dto.setLearnerCount(0);
                dto.setBookmarkCount(0);
            }

            // 用户相关信息
            dto.setBookmarked(bookmarkedCourseIds.contains(courseDO.getId()));
            dto.setProgress(progressMap.getOrDefault(courseDO.getRootNodeId(), 0));

            result.add(dto);
        }

        return result;
    }

    // ========== Admin DTO ==========

    /**
     * 转换为管理后台 DTO（含 creator、parentCourse、统计信息）
     */
    public CourseAdminDTO toAdminDTO(CourseDO courseDO) {
        if (courseDO == null) return null;

        CourseAdminDTO dto = courseConverter.toAdminDTO(courseDO);

        // 填充父课程信息
        if (courseDO.getParentCourseId() != null && courseDO.getParentCourseId() > 0) {
            CourseDO parentCourseDO = courseDataService.getById(courseDO.getParentCourseId());
            if (parentCourseDO != null) {
                dto.setParentCourse(courseConverter.toBriefDTO(parentCourseDO));
            }
        }

        // 填充创建者信息
        if (courseDO.getCreatorId() != null) {
            dto.setCreator(userService.getUserBriefById(courseDO.getCreatorId()));
        }

        // 填充统计信息
        try {
            ContentStatsDO stats = contentStatsDataService.getByContent(ContentType.course, courseDO.getId())
                .orElse(null);
            if (stats != null) {
                dto.setBookmarkCount(stats.getBookmarkCount());
                dto.setCompletedUserCount(stats.getCompletedUserCount());
                dto.setLearnerCount(stats.getLearnerCount());
            }
        } catch (Exception e) {
            log.warn("获取课程统计信息失败, courseId={}", courseDO.getId(), e);
        }

        return dto;
    }

    /**
     * 批量转换为管理后台 DTO
     */
    public List<CourseAdminDTO> toAdminDTOList(List<CourseDO> courseDOList) {
        if (courseDOList == null || courseDOList.isEmpty()) {
            return List.of();
        }

        List<CourseAdminDTO> dtoList = courseConverter.toAdminDTO(courseDOList);

        // 批量填充父课程信息
        Map<Long, CourseBriefDTO> parentCourseMap = getBatchParentCourses(courseDOList);

        // 批量填充创建者信息
        Set<Long> creatorIds = courseDOList.stream()
            .map(CourseDO::getCreatorId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, UserBriefDTO> creatorMap = creatorIds.isEmpty()
            ? new HashMap<>()
            : userService.getUserBriefMapByIds(creatorIds);

        // 批量获取统计信息
        List<Long> courseIds = courseDOList.stream()
            .map(CourseDO::getId)
            .collect(Collectors.toList());
        Map<Long, ContentStatsDO> statsMap = getBatchStats(courseIds);

        // 填充到 DTO
        for (int i = 0; i < dtoList.size(); i++) {
            CourseAdminDTO dto = dtoList.get(i);
            CourseDO courseDO = courseDOList.get(i);

            // 父课程
            if (courseDO.getParentCourseId() != null && courseDO.getParentCourseId() > 0) {
                dto.setParentCourse(parentCourseMap.get(courseDO.getParentCourseId()));
            }

            // 创建者
            if (courseDO.getCreatorId() != null) {
                dto.setCreator(creatorMap.get(courseDO.getCreatorId()));
            }

            // 统计信息
            ContentStatsDO stats = statsMap.get(courseDO.getId());
            if (stats != null) {
                dto.setBookmarkCount(stats.getBookmarkCount());
                dto.setCompletedUserCount(stats.getCompletedUserCount());
                dto.setLearnerCount(stats.getLearnerCount());
            }
        }

        return dtoList;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 填充父课程信息到 DTO
     */
    private void fillParentCourse(CourseSummaryDTO dto, Long parentCourseId) {
        if (parentCourseId != null && parentCourseId > 0) {
            CourseDO parentCourseDO = courseDataService.getById(parentCourseId);
            if (parentCourseDO != null) {
                dto.setParentCourse(courseConverter.toBriefDTO(parentCourseDO));
            }
        }
    }

    /**
     * 批量获取父课程信息
     */
    private Map<Long, CourseBriefDTO> getBatchParentCourses(List<CourseDO> courseDOList) {
        Set<Long> parentCourseIds = courseDOList.stream()
            .map(CourseDO::getParentCourseId)
            .filter(id -> id != null && id > 0)
            .collect(Collectors.toSet());

        if (parentCourseIds.isEmpty()) {
            return new HashMap<>();
        }

        List<CourseDO> parentCourses = courseDataService.getByIds(new ArrayList<>(parentCourseIds));
        return parentCourses.stream()
            .collect(Collectors.toMap(CourseDO::getId, courseConverter::toBriefDTO));
    }

    /**
     * 批量获取课程统计信息（异常安全）
     */
    private Map<Long, ContentStatsDO> getBatchStats(List<Long> courseIds) {
        try {
            List<ContentStatsDO> statsList = contentStatsDataService.batchGetByContentIds(ContentType.course, courseIds);
            return statsList.stream()
                .collect(Collectors.toMap(ContentStatsDO::getContentId, stats -> stats));
        } catch (Exception e) {
            log.error("批量获取课程统计信息失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 批量获取用户学习进度
     */
    private Map<Long, Integer> getBatchProgress(Long userId, List<Long> rootNodeIds) {
        if (userId == null || rootNodeIds == null || rootNodeIds.isEmpty()) {
            return new HashMap<>();
        }

        // 使用批量查询避免 N+1 问题
        Map<Long, UserLearningDO> nodeProgressMap = userLearningDomainService.getBatch(
            userId,
            Enums.ContentType.node,
            rootNodeIds
        );

        Map<Long, Integer> result = new HashMap<>();
        for (Long rootNodeId : rootNodeIds) {
            UserLearningDO learning = nodeProgressMap.get(rootNodeId);
            result.put(rootNodeId, learning != null && learning.getProgressPercent() != null
                ? learning.getProgressPercent() : 0);
        }

        return result;
    }
}
