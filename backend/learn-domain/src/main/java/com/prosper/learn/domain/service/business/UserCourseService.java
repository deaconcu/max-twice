package com.prosper.learn.domain.service.business;

import static com.prosper.learn.common.Enums.UserCourseState;

import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.domain.service.basic.CourseRankingService;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.dto.CourseDTOV2;
import com.prosper.learn.dto.UserCourseDTO;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.UserCourseDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import com.prosper.learn.persistence.mapper.UserCourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCourseService {

    private final UserCourseMapper userCourseMapper;
    private final CourseMapper courseMapper;
    private final CourseRankingService courseRankingService;

    // ========== 常量定义 ==========
    private static final int MIN_PROGRESS = 0;
    private static final int MAX_PROGRESS = 100;
    private static final long DEFAULT_MAX_ID = Long.MAX_VALUE;

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
        UserCourseDO userCourseDO = userCourseMapper.getByUserIdAndCourseId(userId, courseId);
        if (userCourseDO == null) {
            throw ErrorCode.USER_COURSE_NOT_FOUND.exception();
        }
        return userCourseDO;
    }
    
    /**
     * 加载课程信息到DTO
     */
    private void loadCourseInfo(UserCourseDTO dto, Long courseId) {
        CourseDO courseDO = courseMapper.getById(courseId.intValue());
        if (courseDO != null) {
            List<CourseDTOV2> courseList = Converter.INSTANCE.toCourseDTOV2(List.of(courseDO));
            if (!courseList.isEmpty()) {
                dto.setCourse(courseList.get(0));
            }
        }
    }
    
    /**
     * 更新学习状态
     */
    private void updateLearningState(UserCourseDO progressDO, Integer progressPercent) {
        progressDO.setProgressPercent(progressPercent);
        
        if (progressPercent >= MAX_PROGRESS) {
            progressDO.setState(UserCourseState.COMPLETED.value());
            progressDO.setCompletedAt(LocalDateTime.now());
        } else if (progressPercent > MIN_PROGRESS) {
            progressDO.setState(UserCourseState.IN_PROGRESS.value());
        }
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
        UserCourseDO existing = userCourseMapper.getByUserIdAndCourseId(userId, courseId);

        if (existing != null) {
            // 如果已存在，删除记录并更新Redis
            userCourseMapper.deleteByUserAndCourse(userId, courseId);
            // 减少学习人数
            courseRankingService.decrementLearning(courseId.intValue());
            return false;
        }

        // 创建新的学习记录
        UserCourseDO progressDO = new UserCourseDO();
        progressDO.setUserId(userId);
        progressDO.setCourseId(courseId);
        progressDO.setProgressPercent(MIN_PROGRESS);
        progressDO.setState(UserCourseState.IN_PROGRESS.value());
        progressDO.setStartedAt(LocalDateTime.now());

        userCourseMapper.insert(progressDO);
        
        // 增加学习人数
        courseRankingService.incrementLearning(courseId.intValue());

        return true;
    }

    /**
     * 获取用户的课程学习进度
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 学习进度记录，如果不存在返回null
     */
    public UserCourseDTO getUserCourse(Long userId, Long courseId) {
        validateUserId(userId);
        validateCourseId(courseId);
        
        UserCourseDO userCourseDo = userCourseMapper.getByUserIdAndCourseId(userId, courseId);
        if (userCourseDo == null) {
            return null;
        }

        UserCourseDTO dto = Converter.INSTANCE.toUserCourseDTO(userCourseDo);
        // 加载课程信息
        loadCourseInfo(dto, userCourseDo.getCourseId());

        return dto;
    }

    /**
     * 获取用户所有课程学习进度
     * @param userId 用户ID
     * @return 用户所有课程学习进度列表
     */
    public List<UserCourseDTO> getUserCourseList(Long userId, Long lastId) {
        validateUserId(userId);
        
        if (lastId == null || lastId <= 0) {
            lastId = DEFAULT_MAX_ID;
        }
        List<UserCourseDO> userCourseDOList = userCourseMapper.getByUserId(userId, lastId);
        List<UserCourseDTO> dtoList = Converter.INSTANCE.toUserCourseDTO(userCourseDOList);

        // 批量加载课程信息
        Set<Long> courseIds = userCourseDOList.stream()
            .map(progress -> progress.getCourseId().longValue())
            .collect(Collectors.toSet());

        if (!courseIds.isEmpty()) {
            List<CourseDO> courses = courseMapper.getByIds(courseIds.stream().toList());
            List<CourseDTOV2> courseDTOList = Converter.INSTANCE.toCourseDTOV2(courses);

            Map<Long, CourseDTOV2> courseMap = courseDTOList.stream()
                .collect(Collectors.toMap(course -> (long) course.getId(), course -> course));

            // 为每个UserCourseDTO设置对应的课程信息
            dtoList.forEach(dto -> {
                userCourseDOList.stream()
                    .filter(progress -> progress.getId().equals(dto.getId()))
                    .findFirst()
                    .ifPresent(progress -> dto.setCourse(courseMap.get(progress.getCourseId())));
            });
        }

        return dtoList;
    }

    /**
     * 更新课程学习进度
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param progressPercent 进度百分比
     * @return 更新后的学习进度记录
     */
    public UserCourseDTO update(Long userId, Long courseId, Integer progressPercent) {
        validateUserId(userId);
        validateCourseId(courseId);
        validateProgressPercent(progressPercent);
        
        UserCourseDO progressDO = validateAndGetUserCourse(userId, courseId);

        updateLearningState(progressDO, progressPercent);
        userCourseMapper.update(progressDO);

        UserCourseDTO dto = Converter.INSTANCE.toUserCourseDTO(progressDO);
        // 加载课程信息
        loadCourseInfo(dto, courseId);

        return dto;
    }

    /**
     * 删除课程学习记录
     * @param userId 用户ID
     * @param courseId 课程ID
     */
    public void delete(Long userId, Long courseId) {
        validateUserId(userId);
        validateCourseId(courseId);
        
        userCourseMapper.deleteByUserAndCourse(userId, courseId);
        // 减少学习人数
        courseRankingService.decrementLearning(courseId.intValue());
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
        Map<Long, UserCourseDO> userCourseMap = userCourseMapper.getByUserIdAndCourseIdsAsMap((long) userId, courseIds);
        return userCourseMap;
    }
}
