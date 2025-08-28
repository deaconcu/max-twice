package com.prosper.learn.domain.service;

import com.prosper.learn.common.Enums;
import static com.prosper.learn.common.Enums.UserCourseState;
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

    /**
     * 用户开始学习课程
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 学习进度记录
     */
    public boolean startCourse(Long userId, Long courseId) {
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
        progressDO.setProgressPercent(0);
        progressDO.setState(UserCourseState.IN_PROGRESS.value);
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
        UserCourseDO userCourseDo = userCourseMapper.getByUserIdAndCourseId(userId, courseId);
        if (userCourseDo == null) {
            return null;
        }

        UserCourseDTO dto = Converter.INSTANCE.toUserCourseDTO(userCourseDo);
        // 加载课程信息
        CourseDO courseDO = courseMapper.getById(userCourseDo.getCourseId().intValue());
        if (courseDO != null) {
            List<CourseDTOV2> courseList = Converter.INSTANCE.toCourseDTOV2(List.of(courseDO));
            if (!courseList.isEmpty()) {
                dto.setCourse(courseList.get(0));
            }
        }

        return dto;
    }

    /**
     * 获取用户所有课程学习进度
     * @param userId 用户ID
     * @return 用户所有课程学习进度列表
     */
    public List<UserCourseDTO> getUserCourseList(Long userId, Long lastId) {
        if (lastId == null || lastId <= 0) {
            lastId = Long.MAX_VALUE;
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
        UserCourseDO progressDO = userCourseMapper.getByUserIdAndCourseId(userId, courseId);

        if (progressDO == null) {
            throw new RuntimeException("课程学习记录不存在");
        }

        progressDO.setProgressPercent(progressPercent);

        // 如果进度达到100%，标记为完成
        if (progressPercent >= 100) {
            progressDO.setState(UserCourseState.COMPLETED.value);
            progressDO.setCompletedAt(LocalDateTime.now());
        } else if (progressPercent > 0) {
            progressDO.setState(UserCourseState.IN_PROGRESS.value);
        }

        userCourseMapper.update(progressDO);

        UserCourseDTO dto = Converter.INSTANCE.toUserCourseDTO(progressDO);
        // 加载课程信息
        CourseDO courseDO = courseMapper.getById(courseId.intValue());
        if (courseDO != null) {
            List<CourseDTOV2> courseList = Converter.INSTANCE.toCourseDTOV2(List.of(courseDO));
            if (!courseList.isEmpty()) {
                dto.setCourse(courseList.get(0));
            }
        }

        return dto;
    }

    /**
     * 删除课程学习记录
     * @param userId 用户ID
     * @param courseId 课程ID
     */
    public void delete(Long userId, Long courseId) {
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
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }
        
        // 批量查询用户课程记录，直接返回Map
        Map<Long, UserCourseDO> userCourseMap = userCourseMapper.getByUserIdAndCourseIdsAsMap((long) userId, courseIds);
        return userCourseMap;
    }
}
