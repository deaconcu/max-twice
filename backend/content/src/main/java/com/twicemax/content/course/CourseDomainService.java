package com.twicemax.content.course;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 课程领域服务
 *
 * 职责：
 * - 课程领域的业务规则验证
 * - 课程的核心业务逻辑（CRUD）
 * - 课程状态管理
 *
 * 不包含：
 * - DTO 转换（由 ApplicationService 负责）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDomainService {

    private final CourseDataService courseDataService;

    // ========== 查询方法 ==========

    /**
     * 检查用户是否是课程创建者
     */
    public boolean isCreator(long courseId, long userId) {
        CourseDO course = courseDataService.validateAndGet(courseId);
        return course.getCreatorId().equals(userId);
    }

    // ========== 业务方法 ==========

    /**
     * 更新课程基本信息
     *
     * 注意：权限验证应该由调用方（ApplicationService）完成
     */
    @Transactional
    public void updateCourse(long courseId, String name, String description,
                            Integer mainCategory, Integer subCategory, String icon) {
        CourseDO courseDO = courseDataService.validateAndGet(courseId);

        courseDO.setName(name);
        courseDO.setDescription(description);
        courseDO.setMainCategory(mainCategory);
        courseDO.setSubCategory(subCategory);
        courseDO.setIcon(icon);
        courseDataService.update(courseDO);
        log.info("课程 更新成功: courseId={}", courseId);
    }

    /**
     * 删除课程
     *
     * 注意：权限验证应该由调用方完成
     */
    @Transactional
    public void deleteCourse(long courseId) {
        courseDataService.validateAndGet(courseId);
        courseDataService.delete(courseId);
        log.info("课程 删除成功: courseId={}", courseId);
    }
}
