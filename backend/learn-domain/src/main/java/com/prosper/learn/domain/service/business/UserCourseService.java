package com.prosper.learn.domain.service.business;

import static com.prosper.learn.common.Enums.UserProgressState;

import com.prosper.learn.common.Enums;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.basic.CourseRankingService;
import com.prosper.learn.domain.util.converter.CourseConverter;
import com.prosper.learn.domain.util.converter.UserCourseConverter;
import com.prosper.learn.dto.response.course.CourseSummaryDTO;
import com.prosper.learn.dto.response.usercourse.UserCourseSummaryDTO;
import com.prosper.learn.dto.response.usercourse.UserCourseWithCourseDTO;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.UserCourseDO;
import com.prosper.learn.domain.service.data.CourseDataService;
import com.prosper.learn.domain.service.data.UserCourseDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCourseService {

    private final UserCourseDataService userCourseDataService;
    private final CourseDataService courseDataService;
    private final CourseRankingService courseRankingService;
    private final CourseConverter courseConverter;
    private final UserCourseConverter userCourseConverter;
    private final CourseService courseService;

    // ========== 常量定义 ==========
    private static final int MIN_PROGRESS = 0;
    private static final int MAX_PROGRESS = 100;
    private static final long DEFAULT_MAX_ID = Long.MAX_VALUE;

    // ========== DTO转换方法 ==========

    /**
     * 转换为摘要 DTO（基础信息，不含课程详情）
     */
    public UserCourseSummaryDTO toSummaryDTO(UserCourseDO userCourseDO) {
        return userCourseConverter.toSummaryDTO(userCourseDO);
    }

    public List<UserCourseSummaryDTO> toSummaryDTO(List<UserCourseDO> userCourseDOList) {
        return userCourseConverter.toSummaryDTO(userCourseDOList);
    }

    /**
     * 转换为含课程信息的 DTO（单个）
     */
    public UserCourseWithCourseDTO toWithCourseDTO(UserCourseDO userCourseDO) {
        if (userCourseDO == null) return null;

        UserCourseWithCourseDTO dto = userCourseConverter.toWithCourseDTO(userCourseDO);

        // 填充课程摘要信息
        CourseDO courseDO = courseDataService.getById(userCourseDO.getCourseId());
        CourseSummaryDTO courseDTO = courseService.toSummaryDTO(courseDO);
        dto.setCourse(courseDTO);

        return dto;
    }

    /**
     * 转换为含课程信息的 DTO（批量）
     */
    public List<UserCourseWithCourseDTO> toWithCourseDTO(List<UserCourseDO> userCourseDOList) {
        if (userCourseDOList.isEmpty()) {
            return List.of();
        }

        // 提取所有课程 IDs
        List<Long> courseIds = userCourseDOList.stream()
                .map(userCourse -> userCourse.getCourseId().longValue())
                .collect(Collectors.toList());

        // 批量查询课程信息
        List<CourseDO> courseDOList = courseDataService.getByIds(courseIds);
        List<CourseSummaryDTO> courseDTOList = courseService.toSummaryDTO(courseDOList);
        Map<Long, CourseSummaryDTO> courseMap = courseDTOList.stream()
                .collect(Collectors.toMap(course -> (long) course.getId(), course -> course));

        // 转换为 DTO 并填充课程信息
        return userCourseDOList.stream()
                .map(userCourseDO -> {
                    UserCourseWithCourseDTO dto = userCourseConverter.toWithCourseDTO(userCourseDO);
                    CourseSummaryDTO courseDTO = courseMap.get(userCourseDO.getCourseId());
                    if (courseDTO != null) {
                        dto.setCourse(courseDTO);
                    }
                    return dto;
                }).collect(Collectors.toList());
    }

    // ========== 私有验证方法 ==========
    
    /**
     * 验证用户ID
     */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    /**
     * 验证课程ID
     */
    private void validateCourseId(Long courseId) {
        if (courseId == null || courseId <= 0) {
            throw ErrorCode.INVALID_PARAMETER.exception();
        }
    }
    
    /**
     * 验证进度百分比
     */
    private void validateProgressPercent(Integer progressPercent) {
        if (progressPercent == null || progressPercent < MIN_PROGRESS || progressPercent > MAX_PROGRESS) {
            throw ErrorCode.USER_COURSE_PROGRESS_INVALID.exception();
        }
    }
    
    /**
     * 验证并获取用户课程记录
     */
    private UserCourseDO validateAndGetUserCourse(Long userId, Long courseId) {
        UserCourseDO userCourseDO = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        if (userCourseDO == null) {
            throw ErrorCode.USER_COURSE_NOT_FOUND.exception();
        }
        return userCourseDO;
    }

    
    /**
     * 更新学习状态
     */
    private void updateLearningState(UserCourseDO progressDO, Integer progressPercent) {
        progressDO.setProgressPercent(progressPercent);
        
        if (progressPercent >= MAX_PROGRESS) {
            progressDO.setState(UserProgressState.COMPLETED.value());
            progressDO.setCompletedAt(LocalDateTime.now());
        } else if (progressPercent > MIN_PROGRESS) {
            progressDO.setState(UserProgressState.IN_PROGRESS.value());
        }
    }

    public Integer getCourseProgress(long userId, Long courseId) {
        UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        return userCourse != null ? userCourse.getProgressPercent() : 0;
    }

    /**
     * 用户开始学习课程
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 学习进度记录
     */
    public boolean startCourse(Long userId, Long courseId) {
        validateUserId(userId);
        validateCourseId(courseId);
        
        // 检查是否已经存在学习记录
        UserCourseDO existing = userCourseDataService.getByUserIdAndCourseId(userId, courseId);

        if (existing != null) {
            // 如果已存在，删除记录并更新Redis
            userCourseDataService.deleteByUserAndCourse(userId, courseId);
            // 减少学习人数
            courseRankingService.decrementLearning(courseId);
            return false;
        }

        // 创建新的学习记录
        UserCourseDO progressDO = new UserCourseDO();
        progressDO.setUserId(userId);
        progressDO.setCourseId(courseId);
        progressDO.setProgressPercent(MIN_PROGRESS);
        progressDO.setState(UserProgressState.IN_PROGRESS.value());
        progressDO.setStartedAt(LocalDateTime.now());

        userCourseDataService.insert(progressDO);
        
        // 增加学习人数
        courseRankingService.incrementLearning(courseId);

        return true;
    }

    /**
     * 获取用户的课程学习进度
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 学习进度记录，如果不存在返回null
     */
    public UserCourseWithCourseDTO getUserCourse(Long userId, Long courseId) {
        validateUserId(userId);
        validateCourseId(courseId);

        UserCourseDO userCourseDo = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        return toWithCourseDTO(userCourseDo);
    }

    /**
     * 获取用户所有课程学习进度
     * @param userId 用户ID
     * @return 用户所有课程学习进度列表
     */
    public List<UserCourseWithCourseDTO> getUserCourseList(Long userId, Long lastId) {
        validateUserId(userId);

        if (lastId == null || lastId <= 0) {
            lastId = DEFAULT_MAX_ID;
        }

        List<UserCourseDO> userCourseDOList = userCourseDataService.getByUserId(userId, lastId);
        return toWithCourseDTO(userCourseDOList);
    }

    /**
     * 更新课程学习进度
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param progressPercent 进度百分比
     * @return 更新后的学习进度记录
     */
    public UserCourseWithCourseDTO update(Long userId, Long courseId, Integer progressPercent) {
        validateUserId(userId);
        validateCourseId(courseId);
        validateProgressPercent(progressPercent);

        UserCourseDO userCourseDO = validateAndGetUserCourse(userId, courseId);

        updateLearningState(userCourseDO, progressPercent);
        userCourseDataService.update(userCourseDO);

        return toWithCourseDTO(userCourseDO);
    }

    /**
     * 删除课程学习记录
     * @param userId 用户ID
     * @param courseId 课程ID
     */
    public void delete(Long userId, Long courseId) {
        validateUserId(userId);
        validateCourseId(courseId);
        
        userCourseDataService.deleteByUserAndCourse(userId, courseId);
        // 减少学习人数
        courseRankingService.decrementLearning(courseId);
    }

    /**
     * 批量查询用户对多个课程的学习进度
     * @param userId 用户ID
     * @param courseIds 课程ID列表
     * @return 课程ID到用户课程DTO的映射
     */
    public Map<Long, UserCourseDO> getUserCoursesBatch(long userId, List<Long> courseIds) {
        validateUserId(userId);
        
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        
        // 批量查询用户课程记录，直接返回Map
        Map<Long, UserCourseDO> userCourseMap = userCourseDataService.getByUserIdAndCourseIdsAsMap((long) userId, courseIds);
        return userCourseMap;
    }
}
