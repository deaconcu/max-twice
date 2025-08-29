package com.prosper.learn.api.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.dto.CourseDTO;
import com.prosper.learn.dto.CourseDTOV3;
import com.prosper.learn.dto.CourseDTOV4;
import com.prosper.learn.domain.service.business.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程管理接口
 * 从CourseClient迁移而来
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class CoursesController {

    private final CourseService courseService;

    /**
     * 获取课程详情
     * 映射: GET /course/{id} → GET /api/v1/courses/{id}
     */
    @GetMapping("/courses/{id}")
    public ApiResponse<CourseDTOV4> getCourse(@PathVariable Long id) {
        CourseDTOV4 course = courseService.getCourseById(id);
        return ApiResponse.success(course);
    }

    /**
     * 搜索课程
     * 映射: GET /course/search?name=xxx → GET /api/v1/courses/search?name=xxx
     */
    @GetMapping("/courses/search")
    public ApiResponse<List<CourseDTOV3>> searchCourses(@RequestParam String name) {
        List<CourseDTOV3> courseList = courseService.searchCoursesByName(name);
        return ApiResponse.success(courseList);
    }

    /**
     * 按状态获取课程列表
     * 映射: GET /course/list?state=xxx&lastId=123 → GET /api/v1/courses?state=xxx&lastId=123
     */
    @GetMapping("/courses")
    public ApiResponse<Object> getCoursesByState(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) Integer mainCategory,
            @RequestParam(required = false) Integer subCategory,
            @RequestParam(required = false) Long parentId) {
        
        if (state != null && lastId != null) {
            List<CourseDTOV4> courseList = courseService.getListByStateAndLastId(state, lastId);
            return ApiResponse.success(courseList);
        } else if (mainCategory != null && subCategory != null) {
            List<CourseDTOV4> courseList = courseService.getListByCategory(mainCategory, subCategory);
            return ApiResponse.success(courseList);
        } else if (parentId != null) {
            if ("approved".equals(state)) {
                List<CourseDTOV4> courseList = courseService.getListByParent(parentId, "APPROVED");
                return ApiResponse.success(courseList);
            } else {
                List<CourseDTOV4> courseList = courseService.getListByParent(parentId, "ALL");
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
    public ApiResponse<Object> getHotCourses(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        log.info("开始获取热门课程，limit: {}", limit);
        List<CourseDTOV4> hotCourses = courseService.getHotCourses(limit);
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
        List<CourseDTOV4> hotCoursesRanking = courseService.getHotCoursesRanking();
        log.info("成功获取热门课程排行榜数量: {}", hotCoursesRanking.size());
        return ApiResponse.success(hotCoursesRanking);
    }

    /**
     * 创建课程
     * 映射: POST /course → POST /api/v1/courses
     */
    @PostMapping("/courses")
    public ApiResponse<Object> createCourse(@RequestBody CourseDTO course) {
        long userId = StpUtil.getLoginIdAsLong();
        course.setCreator(userId);
        courseService.createCourse(course);
        return ApiResponse.success("课程创建成功");
    }

    /**
     * 修改课程
     * 映射: PUT /course/{id} → PUT /api/v1/courses/{id}
     */
    @PutMapping("/courses/{id}")
    public ApiResponse<Object> updateCourse(@PathVariable Long id, @RequestBody CourseDTO course) {
        courseService.updateCourse(id, course);
        return ApiResponse.success("更新成功");
    }

    /**
     * 创建子课程
     * 映射: POST /subcourse → POST /api/v1/courses/{parentId}/subcourses
     */
    @PostMapping("/courses/{parentId}/subcourses")
    public ApiResponse<Object> createSubcourse(
            @PathVariable Long parentId,
            @RequestParam String name, 
            @RequestParam String description) {
        
        long userId = StpUtil.getLoginIdAsLong();
        courseService.createSubcourse(name, description, parentId, userId);
        return ApiResponse.success("课程创建成功");
    }

    /**
     * 课程审核操作
     * 映射: POST /course/operate → POST /api/v1/courses/{id}/approve
     */
    @PostMapping("/courses/{id}/approve")
    public ApiResponse<Object> approveCourse(
            @PathVariable Long id, 
            @RequestParam String action, 
            @RequestParam(required = false) String rejectedReason) {
        
        if (!courseService.exist(id)) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        return switch (action.toLowerCase()) {
            case "approve" -> {
                courseService.approve(id);
                yield ApiResponse.success("批准成功");
            }
            case "reject" -> {
                courseService.reject(id, rejectedReason);
                yield ApiResponse.success("拒绝成功");
            }
            case "delete" -> {
                courseService.delete(id);
                yield ApiResponse.success("删除成功");
            }
            default -> throw ErrorCode.SYSTEM_ERROR.exception();
        };
    }
}