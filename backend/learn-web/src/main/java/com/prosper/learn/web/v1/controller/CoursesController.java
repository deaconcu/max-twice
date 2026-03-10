package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.application.dto.request.CreateCourseRequest;
import com.prosper.learn.application.dto.request.CreateSubcourseRequest;
import com.prosper.learn.application.dto.response.KeysetPageResponse;
import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.course.CourseFullDTO;
import com.prosper.learn.application.service.CourseService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.application.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.prosper.learn.shared.domain.Enums.*;

/**
 * 课程管理接口
 * 从CourseClient迁移而来
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
@Validated
public class CoursesController {

    private final CourseService courseService;

    /**
     * 获取课程详情（用户接口）
     * 映射: GET /course/{id} → GET /api/v1/courses/{id}
     *
     * 注意：需要登录才能访问
     * 返回课程基本信息、统计数据和用户学习状态
     * 如果不需要登录，请使用 GET /api/v1/public/courses/{id}
     */
    @GetMapping("/courses/{id}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<CourseFullDTO> getCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID不正确") Long id,
            @CurrentUser UserDO currentUser) {
        CourseFullDTO course = courseService.getCourseById(id, currentUser.getId());
        return ApiResponse.query(course);
    }

    /**
     * 获取课程列表（用户接口）
     * 映射: GET /course/list?mainCategory=1&subCategory=2 → GET /api/v1/courses?mainCategory=1&subCategory=2
     *
     * 注意：需要登录才能访问
     * 返回带统计信息和用户学习状态的课程列表（订阅状态、学习进度等）
     * 如果不需要登录，请使用 GET /api/v1/public/courses
     *
     * 参数组合：
     * 1. mainCategory（可选 subCategory）：按分类筛选
     * 2. parentId：获取子课程
     * 3. 无参数或只有 lastId：返回所有已发布课程（分页）
     */
    @GetMapping("/courses")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<KeysetPageResponse<CourseFullDTO>> getCourses(
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @RequestParam(required = false) @Positive(message = "主分类必须大于0") Integer mainCategory,
            @RequestParam(required = false) @Positive(message = "子分类必须大于0") Integer subCategory,
            @RequestParam(required = false) @Positive(message = "父课程ID必须大于0") Long parentId,
            @CurrentUser UserDO currentUser) {

        Long userId = currentUser.getId();

        KeysetPageResponse<CourseFullDTO> response;

        // 1. 按分类筛选（支持只传主分类）
        if (mainCategory != null) {
            response = courseService.getListByCategoryPage(mainCategory, subCategory, lastId, userId);
        }
        // 2. 获取子课程
        else if (parentId != null) {
            response = courseService.getListByParentPage(parentId, ContentState.PUBLISHED, lastId, userId);
        }
        // 3. 默认：返回所有已发布课程（分页）
        else {
            response = courseService.getListByStatePage(ContentState.PUBLISHED, lastId, userId);
        }

        return ApiResponse.query(response);
    }

    /**
     * 搜索课程（用户端）
     * 映射: GET /course/search?name=xxx → GET /api/v1/courses/search?name=xxx
     * 只搜索已发布的课程
     */
    @GetMapping("/courses/search")
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<CourseBriefDTO>> searchCourses(
            @RequestParam @NotBlank(message = "搜索名称不能为空") String name) {
        List<CourseBriefDTO> courseList = courseService.searchPublishedCourses(name);
        return ApiResponse.query(courseList);
    }

    /**
     * 热门课程
     * 映射: GET /course/hot → GET /api/v1/courses/hot
     */
    @GetMapping("/courses/hot")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<List<CourseFullDTO>> getHotCourses(
            @RequestParam(value = "limit", defaultValue = "10")
            @Positive(message = "限制数量必须大于0")
            Integer limit) {
        log.info("开始获取热门课程，limit: {}", limit);
        List<CourseFullDTO> hotCourses = courseService.getHotCourses(limit);
        log.info("成功获取热门课程数量: {}", hotCourses.size());
        return ApiResponse.query(hotCourses);
    }

    /**
     * 创建课程
     * 映射: POST /course → POST /api/v1/courses
     */
    @PostMapping("/courses")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Object> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @CurrentUser UserDO currentUser) {
        courseService.createCourse(request, currentUser);
        return ApiResponse.success("课程创建成功");
    }

    /**
     * 创建子课程
     * 映射: POST /subcourse → POST /api/v1/courses/{parentId}/subcourses
     */
    @PostMapping("/courses/{parentId}/subcourses")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Object> createSubcourse(
            @PathVariable @NotNull(message = "父课程ID不能为空")
            @Positive(message = "父课程ID必须大于0")
            Long parentId,
            @RequestBody @Valid CreateSubcourseRequest request,
            @CurrentUser UserDO currentUser) {

        courseService.createSubcourse(request.getName(), request.getDescription(), parentId, currentUser);
        return ApiResponse.success("课程创建成功");
    }
}
