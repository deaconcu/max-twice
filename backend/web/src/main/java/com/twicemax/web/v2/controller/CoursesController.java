package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.CreateCourseRequest;
import com.twicemax.application.dto.request.CreateSubcourseRequest;
import com.twicemax.application.dto.request.UpdateCourseRequest;
import com.twicemax.application.dto.response.course.CourseSummaryDTO;
import com.twicemax.application.dto.response.course.CourseFullDTO;
import com.twicemax.application.dto.v2.CreateAcceptedResponse;
import com.twicemax.application.dto.v2.Cursor;
import com.twicemax.application.dto.v2.CursorPage;
import com.twicemax.application.service.CourseService;
import com.twicemax.shared.domain.exception.StatusCode;
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
 * 课程管理接口（v2）
 *
 * <p>遵循 v2 API 规范：
 * <ul>
 *   <li>路径前缀 {@code /courses}（用户视角的"我的"列表挂在 {@code /users/me/courses}）</li>
 *   <li>POST 申请类创建返回 {@code 202 Accepted + {id}}</li>
 * </ul>
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class CoursesController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CourseService courseService;

    @GetMapping("/courses/{id}")
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CourseFullDTO getCourse(
            @PathVariable @NotNull(message = "课程ID不能为空") @Positive(message = "课程ID不正确") Long id,
            @CurrentUser UserDO currentUser) {
        return courseService.getCourseById(id, currentUser.getId());
    }

    @GetMapping("/courses")
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
            return courseService.getListByParentPage(parentId, NewContentState.PUBLISHED, cursor, userId);
        } else {
            return courseService.getListByStatePage(NewContentState.PUBLISHED, cursor, userId);
        }
    }

    @GetMapping("/courses/search")
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<CourseSummaryDTO> searchCourses(
            @RequestParam @NotBlank(message = "搜索名称不能为空") String name) {
        return courseService.searchPublishedCourses(name);
    }

    @GetMapping("/courses/hot")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<CourseFullDTO> getHotCourses(
            @RequestParam(defaultValue = "10") @Positive(message = "限制数量必须大于0") Integer limit) {
        return courseService.getHotCourses(limit);
    }

    /**
     * 创建课程（申请，需审核）
     * - parentCourseId 为 null/0：创建主课程，需 mainCategory + subCategory
     * - parentCourseId &gt; 0：创建子课程，分类继承自父课程
     */
    @PostMapping("/courses")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<CreateAcceptedResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @CurrentUser UserDO currentUser) {
        Long id = courseService.createCourse(request, currentUser);
        return ResponseEntity.accepted().body(new CreateAcceptedResponse(id));
    }

    /**
     * 创建子课程（兼容旧路径，内部委托到 createCourse）
     */
    @PostMapping("/courses/{parentId}/subcourses")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<CreateAcceptedResponse> createSubcourse(
            @PathVariable @NotNull(message = "父课程ID不能为空") @Positive(message = "父课程ID必须大于0") Long parentId,
            @RequestBody @Valid CreateSubcourseRequest request,
            @CurrentUser UserDO currentUser) {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setName(request.getName());
        req.setDescription(request.getDescription());
        req.setParentCourseId(parentId);
        Long id = courseService.createCourse(req, currentUser);
        return ResponseEntity.accepted().body(new CreateAcceptedResponse(id));
    }

    /**
     * 重新提交（被驳回 / 撤回后再申请）
     */
    @PostMapping("/courses/{id}/resubmit")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> resubmit(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0") Long id,
            @Valid @RequestBody UpdateCourseRequest request,
            @CurrentUser UserDO currentUser) {
        courseService.resubmit(id, request, currentUser);
        return ResponseEntity.accepted().build();
    }

    /**
     * 作者撤回审核中的版本
     */
    @PostMapping("/courses/{id}/withdraw")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> withdraw(
            @PathVariable @NotNull(message = "课程ID不能为空")
            @Positive(message = "课程ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {
        courseService.withdraw(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * 当前用户创建的课程列表（"我的申请"）
     * 默认返回 NEVER_PUBLISHED + PUBLISHED；BANNED 不在此处返回。
     */
    @GetMapping("/users/me/courses")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public CursorPage<CourseFullDTO> getCurrentUserCourses(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String state,
            @CurrentUser UserDO currentUser) {

        NewContentState newContentState = null;
        if (state != null && !state.isBlank()) {
            if (NewContentState.BANNED_VALUE.equals(state)) {
                throw StatusCode.INVALID_PARAMETER.exception("无效的状态参数");
            }
            newContentState = NewContentState.getByValue(state);
            if (newContentState == null) {
                throw StatusCode.INVALID_PARAMETER.exception("无效的状态参数");
            }
        }

        int pageSize = DEFAULT_PAGE_SIZE;
        int fetchLimit = pageSize + 1;
        List<CourseFullDTO> courseList = courseService.getUserCourses(
                currentUser.getId(), cursor, newContentState, fetchLimit);

        boolean hasMore = courseList.size() > pageSize;
        if (hasMore) {
            courseList = courseList.subList(0, pageSize);
        }

        String nextCursor = (hasMore && !courseList.isEmpty())
                ? Cursor.of(courseList.get(courseList.size() - 1).getId()).encode()
                : null;

        return CursorPage.of(courseList, hasMore, nextCursor);
    }
}
