package com.prosper.learn.application.service;

import com.prosper.learn.application.converter.CourseConverter;
import com.prosper.learn.application.converter.UserCourseConverter;
import com.prosper.learn.application.dto.response.course.CourseSummaryDTO;
import com.prosper.learn.application.dto.response.usercourse.UserCourseSummaryDTO;
import com.prosper.learn.application.dto.response.usercourse.UserCourseWithCourseDTO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.learning.enrollment.UserCourseDO;
import com.prosper.learn.learning.enrollment.UserCourseDataService;
import com.prosper.learn.learning.enrollment.UserCourseDomainService;
import com.prosper.learn.shared.domain.event.user.learning.LearningStartedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 用户课程应用服务
 * 负责跨域协调、DTO转换、事件发布
 */
@Service
@RequiredArgsConstructor
public class UserCourseService {

    // 领域服务
    private final UserCourseDomainService domainService;

    // 数据服务
    private final UserCourseDataService userCourseDataService;
    private final CourseDataService courseDataService;

    // 其他 ApplicationService
    private final CourseService courseService;

    // 转换器
    private final CourseConverter courseConverter;
    private final UserCourseConverter userCourseConverter;

    // 事件发布
    private final ApplicationEventPublisher eventPublisher;

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

        // 填充课程简要信息
        CourseDO courseDO = courseDataService.getById(userCourseDO.getCourseId());
        CourseBriefDTO courseDTO = courseService.toBriefDTO(courseDO);
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
        List<CourseBriefDTO> courseDTOList = courseService.toBriefDTO(courseDOList);
        Map<Long, CourseBriefDTO> courseMap = courseDTOList.stream()
                .collect(Collectors.toMap(course -> (long) course.getId(), course -> course));

        // 转换为 DTO 并填充课程信息
        return userCourseDOList.stream()
                .map(userCourseDO -> {
                    UserCourseWithCourseDTO dto = userCourseConverter.toWithCourseDTO(userCourseDO);
                    CourseBriefDTO courseDTO = courseMap.get(userCourseDO.getCourseId());
                    dto.setCourse(courseDTO);  // 课程已删除时为 null
                    return dto;
                }).collect(Collectors.toList());
    }

    // ========== 业务方法 ==========

    public Integer getCourseProgress(long userId, Long courseId) {
        return domainService.getCourseProgress(userId, courseId);
    }

    /**
     * 用户开始学习课程
     */
    public void startCourse(Long userId, Long courseId) {
        // 验证课程是否存在
        courseDataService.validateAndGet(courseId);

        // 调用 DomainService 执行核心逻辑
        domainService.startCourse(userId, courseId);

        // 发布学习开始事件
        eventPublisher.publishEvent(new LearningStartedEvent(
            userId,
            courseId,
            ContentType.course
        ));
    }

    /**
     * 取消学习课程
     */
    public void cancelCourse(Long userId, Long courseId) {
        // 调用 DomainService 执行核心逻辑
        domainService.cancelCourse(userId, courseId);
    }

    /**
     * 获取用户的课程学习进度
     */
    public UserCourseWithCourseDTO getUserCourse(Long userId, Long courseId) {
        UserCourseDO userCourseDo = domainService.getByUserAndCourse(userId, courseId);
        return toWithCourseDTO(userCourseDo);
    }

    /**
     * 获取用户所有课程学习进度
     */
    public List<UserCourseWithCourseDTO> getUserCourseList(Long userId, Long lastId) {
        List<UserCourseDO> userCourseDOList = domainService.getByUserId(userId, lastId);
        return toWithCourseDTO(userCourseDOList);
    }

    /**
     * 更新课程学习进度
     */
    public UserCourseWithCourseDTO update(Long userId, Long courseId, Integer progressPercent) {
        // 验证课程是否存在
        courseDataService.validateAndGet(courseId);

        // 调用 DomainService 更新
        domainService.updateProgress(userId, courseId, progressPercent);

        // 查询更新后的数据并转换为DTO
        UserCourseDO userCourseDO = domainService.getByUserAndCourse(userId, courseId);
        return toWithCourseDTO(userCourseDO);
    }

    /**
     * 删除课程学习记录
     */
    public void delete(Long userId, Long courseId) {
        domainService.delete(userId, courseId);
    }

// --注释掉检查 START (2025/12/10 11:30):
//    /**
//     * 批量查询用户对多个课程的学习进度
//     */
//    public Map<Long, UserCourseDO> getUserCoursesBatch(long userId, List<Long> courseIds) {
//        return domainService.getUserCoursesBatch(userId, courseIds);
//    }
// --注释掉检查 STOP (2025/12/10 11:30)
}
