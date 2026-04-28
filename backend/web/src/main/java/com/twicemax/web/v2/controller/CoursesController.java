package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.CreateCourseRequest;
import com.twicemax.application.dto.request.CreateSubcourseRequest;
import com.twicemax.application.dto.response.course.CourseBriefDTO;
import com.twicemax.application.dto.response.course.CourseFullDTO;
import com.twicemax.application.dto.v2.CreateAcceptedResponse;
import com.twicemax.application.dto.v2.CursorPage;
import com.twicemax.application.service.CourseService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.twicemax.shared.domain.Enums.*;

/**
 * 课程管理接口
 */
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Validated
public class CoursesController {

    private final CourseService courseService;

    @GetMapping("/{id}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CourseFullDTO getCourse(
            @PathVariable @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID不正确") Long id,
            @CurrentUser UserDO currentUser) {
        return courseService.getCourseById(id, currentUser.getId());
    }

    @GetMapping
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<CourseFullDTO> getCourses(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) @Positive(message = "主分类必须大于0") Integer mainCategory,
            @RequestParam(required = false) @Positive(message = "子分类必须大于0") Integer subCategory,
            @RequestParam(required = false) @Positive(message = "父课程ID必须大于0") Long parentId,
            @CurrentUser UserDO currentUser) {

        Long userId = currentUser.getId();

        if (mainCategory != null) {
            return courseService.getListByCategoryPage(mainCategory, subCategory, cursor, userId);
        } else if (parentId != null) {
            return courseService.getListByParentPage(parentId, ContentState.PUBLISHED, cursor, userId);
        } else {
            return courseService.getListByStatePage(ContentState.PUBLISHED, cursor, userId);
        }
    }

    @GetMapping("/search")
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<CourseBriefDTO> searchCourses(
            @RequestParam @NotBlank(message = "搜索名称不能为空") String name) {
        return courseService.searchPublishedCourses(name);
    }

    @GetMapping("/hot")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<CourseFullDTO> getHotCourses(
            @RequestParam(defaultValue = "10") @Positive(message = "限制数量必须大于0") Integer limit) {
        return courseService.getHotCourses(limit);
    }

    @PostMapping
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<CreateAcceptedResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @CurrentUser UserDO currentUser) {
        Long id = courseService.createCourse(request, currentUser);
        return ResponseEntity.accepted().body(new CreateAcceptedResponse(id));
    }

    @PostMapping("/{parentId}/subcourses")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> createSubcourse(
            @PathVariable @NotNull(message = "父课程ID不能为空") @Positive(message = "父课程ID必须大于0") Long parentId,
            @RequestBody @Valid CreateSubcourseRequest request,
            @CurrentUser UserDO currentUser) {
        courseService.createSubcourse(request.getName(), request.getDescription(), parentId, currentUser);
        return ResponseEntity.accepted().build();
    }
}
