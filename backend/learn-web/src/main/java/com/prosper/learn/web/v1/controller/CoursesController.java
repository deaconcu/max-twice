package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.dto.request.CreateCourseRequest;
import com.prosper.learn.application.dto.request.CreateSubcourseRequest;
import com.prosper.learn.application.dto.response.course.CourseBriefDTO;
import com.prosper.learn.application.dto.response.course.CourseDetailDTO;
import com.prosper.learn.application.dto.response.course.CourseWithStatsDTO;
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
@RateLimit(capacity = 40, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class CoursesController {

    private final CourseService courseService;

    /**
     * 获取课程详情
     * 映射: GET /course/{id} → GET /api/v1/courses/{id}
     */
    @GetMapping("/courses/{id}")
    public ApiResponse<CourseDetailDTO> getCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID不正确") Long id) {
        CourseDetailDTO course = courseService.getCourseById(id);
        return ApiResponse.success(course);
    }

    /**
     * 搜索课程
     * 映射: GET /course/search?name=xxx → GET /api/v1/courses/search?name=xxx
     */
    @GetMapping("/courses/search")
    public ApiResponse<List<CourseBriefDTO>> searchCourses(
            @RequestParam @NotBlank(message = "搜索名称不能为空") String name) {
        List<CourseBriefDTO> courseList = courseService.searchCoursesByName(name);
        return ApiResponse.success(courseList);
    }

    /**
     * 获取课程列表（普通用户接口）
     * 映射: GET /course/list?mainCategory=1&subCategory=2 → GET /api/v1/courses?mainCategory=1&subCategory=2
     *
     * 参数组合：
     * 1. mainCategory（可选 subCategory）：按分类筛选
     * 2. parentId：获取子课程
     * 3. 无参数或只有 lastId：返回所有已发布课程（分页）
     *
     * 注意：不支持按状态查询，普通用户只能看到已发布课程
     */
    @GetMapping("/courses")
    public ApiResponse<Object> getCourses(
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @RequestParam(required = false) @Positive(message = "主分类必须大于0") Integer mainCategory,
            @RequestParam(required = false) @Positive(message = "子分类必须大于0") Integer subCategory,
            @RequestParam(required = false) @Positive(message = "父课程ID必须大于0") Long parentId) {

        // 1. 按分类筛选（支持只传主分类）
        if (mainCategory != null) {
            List<CourseDetailDTO> courseList = courseService.getListByCategory(mainCategory, subCategory, lastId);
            return ApiResponse.success(courseList);
        }
        // 2. 获取子课程
        else if (parentId != null) {
            List<CourseDetailDTO> courseList = courseService.getListByParent(parentId, ContentState.PUBLISHED);
            return ApiResponse.success(courseList);
        }
        // 3. 默认：返回所有已发布课程（分页）
        else {
            List<CourseDetailDTO> courseList = courseService.getListByStateAndLastId(ContentState.PUBLISHED, lastId);
            return ApiResponse.success(courseList);
        }
    }

    /**
     * 热门课程
     * 映射: GET /course/hot → GET /api/v1/courses/hot
     */
    @GetMapping("/courses/hot")
    public ApiResponse<Object> getHotCourses(
            @RequestParam(value = "limit", defaultValue = "10")
            @Positive(message = "限制数量必须大于0")
            Integer limit) {
        log.info("开始获取热门课程，limit: {}", limit);
        List<CourseWithStatsDTO> hotCourses = courseService.getHotCourses(limit);
        log.info("成功获取热门课程数量: {}", hotCourses.size());
        return ApiResponse.success(hotCourses);
    }

    /**
     * 课程排行榜
     * 映射: GET /course/ranking → GET /api/v1/courses/ranking
     */
    @GetMapping("/courses/ranking")
    public ApiResponse<Object> getCoursesRanking() {
        log.info("开始获取热门课程完整排行榜");
        List<CourseWithStatsDTO> hotCoursesRanking = courseService.getHotCoursesRanking();
        log.info("成功获取热门课程排行榜数量: {}", hotCoursesRanking.size());
        return ApiResponse.success(hotCoursesRanking);
    }

    /**
     * 创建课程
     * 映射: POST /course → POST /api/v1/courses
     */
    @PostMapping("/courses")
    @SaCheckLogin
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