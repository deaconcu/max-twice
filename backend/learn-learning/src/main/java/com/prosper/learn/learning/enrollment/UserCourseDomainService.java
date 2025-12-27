package com.prosper.learn.learning.enrollment;

import com.prosper.learn.shared.common.utils.ValidationUtils;
import com.prosper.learn.shared.domain.Enums.UserProgressState;
import com.prosper.learn.shared.domain.exception.StatusCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户课程领域服务
 * 只依赖本领域（learning/enrollment）模块，处理核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCourseDomainService {

    private final UserCourseDataService userCourseDataService;

    // ========== 常量定义 ==========
    private static final int MIN_PROGRESS = 0;
    private static final int MAX_PROGRESS = 100;
    private static final long DEFAULT_MAX_ID = Long.MAX_VALUE;

    // ========== Query 方法 ==========

    /**
     * 根据用户和课程获取学习记录
     */
    public UserCourseDO getByUserAndCourse(Long userId, Long courseId) {
        return userCourseDataService.getByUserIdAndCourseId(userId, courseId);
    }

    /**
     * 获取用户的课程列表
     */
    public List<UserCourseDO> getByUserId(Long userId, Long lastId) {
        ValidationUtils.requirePositiveId(userId);

        if (lastId == null || lastId <= 0) {
            lastId = DEFAULT_MAX_ID;
        }

        return userCourseDataService.getByUserId(userId, lastId);
    }

    /**
     * 批量查询用户对多个课程的学习进度
     */
    public Map<Long, UserCourseDO> getUserCoursesBatch(long userId, List<Long> courseIds) {
        ValidationUtils.requirePositiveId(userId);

        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }

        return userCourseDataService.getByUserIdAndCourseIdsAsMap(userId, courseIds);
    }

    /**
     * 获取课程学习进度百分比
     */
    public Integer getCourseProgress(long userId, Long courseId) {
        UserCourseDO userCourse = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        return userCourse != null ? userCourse.getProgressPercent() : 0;
    }

    // ========== Command 方法 ==========

    /**
     * 用户开始学习课程（不发布事件）
     */
    @Transactional
    public boolean startCourse(Long userId, Long courseId) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(courseId);

        UserCourseDO existing = userCourseDataService.getByUserIdAndCourseId(userId, courseId);

        if (existing != null) {
            userCourseDataService.deleteByUserAndCourse(userId, courseId);
            return false;
        }

        UserCourseDO progressDO = new UserCourseDO();
        progressDO.setUserId(userId);
        progressDO.setCourseId(courseId);
        progressDO.setProgressPercent(MIN_PROGRESS);
        progressDO.setState(UserProgressState.IN_PROGRESS.value());
        progressDO.setStartedAt(LocalDateTime.now());

        userCourseDataService.insert(progressDO);

        log.info("用户 {} 开始学习课程 {}", userId, courseId);
        return true;
    }

    /**
     * 更新课程学习进度
     */
    @Transactional
    public void updateProgress(Long userId, Long courseId, Integer progressPercent) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(courseId);
        ValidationUtils.requireValidPercent(progressPercent);

        UserCourseDO userCourseDO = userCourseDataService.getByUserIdAndCourseId(userId, courseId);
        if (userCourseDO == null) {
            throw StatusCode.USER_COURSE_NOT_FOUND.exception();
        }

        updateLearningState(userCourseDO, progressPercent);
        userCourseDataService.update(userCourseDO);

        log.info("更新用户 {} 课程 {} 进度为 {}%", userId, courseId, progressPercent);
    }

    /**
     * 删除课程学习记录
     */
    @Transactional
    public void delete(Long userId, Long courseId) {
        ValidationUtils.requirePositiveId(userId);
        ValidationUtils.requirePositiveId(courseId);

        userCourseDataService.deleteByUserAndCourse(userId, courseId);

        log.info("删除用户 {} 课程 {} 学习记录", userId, courseId);
    }

    // ========== Private 辅助方法 ==========

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
}
