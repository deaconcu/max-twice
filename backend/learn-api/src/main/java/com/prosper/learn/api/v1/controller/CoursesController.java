package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.api.ratelimit.LimitType;
import com.prosper.learn.api.ratelimit.RateLimit;
import com.prosper.learn.api.v1.annotation.CurrentUser;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.Enums.ContentState;
import com.prosper.learn.dto.request.*;
import com.prosper.learn.dto.response.CourseDTO;
import com.prosper.learn.persistence.dataobject.UserDO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import com.prosper.learn.domain.service.business.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
    public ApiResponse<CourseDTO> getCourse(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID不正确") Long id) {
        CourseDTO course = courseService.getCourseById(id);
        return ApiResponse.success(course);
    }

    /**
     * 搜索课程
     * 映射: GET /course/search?name=xxx → GET /api/v1/courses/search?name=xxx
     */
    @GetMapping("/courses/search")
    public ApiResponse<List<CourseDTO>> searchCourses(
            @RequestParam @NotBlank(message = "搜索名称不能为空") String name) {
        List<CourseDTO> courseList = courseService.searchCoursesByName(name);
        return ApiResponse.success(courseList);
    }

    /**
     * 按状态获取课程列表
     * 映射: GET /course/list?state=xxx&lastId=123 → GET /api/v1/courses?state=xxx&lastId=123
     */
    @GetMapping("/courses")
    public ApiResponse<Object> getCoursesByState(
            @RequestParam(required = false) @Positive(message = "状态必须大于0") Integer state,
            @RequestParam(required = false) @Positive(message = "最后ID必须大于0") Long lastId,
            @RequestParam(required = false) @Positive(message = "主分类必须大于0") Integer mainCategory,
            @RequestParam(required = false) @Positive(message = "子分类必须大于0") Integer subCategory,
            @RequestParam(required = false) @Positive(message = "父课程ID必须大于0") Long parentId) {

        ContentState courseState = ContentState.getByValue(state);
        if (state != null && lastId != null) {
            List<CourseDTO> courseList = courseService.getListByStateAndLastId(courseState, lastId);
            return ApiResponse.success(courseList);
        } else if (mainCategory != null && subCategory != null) {
            List<CourseDTO> courseList = courseService.getListByCategory(mainCategory, subCategory);
            return ApiResponse.success(courseList);
        } else if (parentId != null) {
            if (courseState != null && courseState == ContentState.PUBLISHED) {
                List<CourseDTO> courseList = courseService.getListByParent(parentId, ContentState.PUBLISHED);
                return ApiResponse.success(courseList);
            } else {
                List<CourseDTO> courseList = courseService.getListByParent(parentId, null);
                return ApiResponse.success(courseList);
            }
        } else {
            throw new IllegalArgumentException("缺少必要参数");
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
        List<CourseDTO> hotCourses = courseService.getHotCourses(limit);
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
        List<CourseDTO> hotCoursesRanking = courseService.getHotCoursesRanking();
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