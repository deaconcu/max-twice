package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.CreateRoadmapRequest;
import com.twicemax.application.dto.request.UpdateRoadmapRequest;
import com.twicemax.application.dto.response.roadmap.RoadmapDetailDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapEditDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapSaveResult;
import com.twicemax.application.dto.response.roadmap.RoadmapWithStatusDTO;
import com.twicemax.application.service.RoadmapService;
import com.twicemax.shared.domain.Enums.NewContentState;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 路线图接口（revision 模型）。
 * <ul>
 *   <li>POST /roadmaps：创建（submit=true 时立即提交审核，否则仅存草稿）</li>
 *   <li>PUT /roadmaps/{id}：保存草稿（不会触发审核）</li>
 *   <li>POST /roadmaps/{id}/submit：把当前 draft 提交审核</li>
 *   <li>POST /roadmaps/{id}/withdraw：作者撤回 pending revision</li>
 *   <li>GET /roadmaps/{id}/edit：作者/admin 编辑页数据</li>
 * </ul>
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class RoadmapsController {

    private final RoadmapService roadmapService;

    @GetMapping("/roles/{roleId}/roadmaps")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<RoadmapWithStatusDTO> getRoadmapsByRole(
            @PathVariable @NotNull(message = "角色ID不能为空") @Positive(message = "角色ID必须大于0") Long roleId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "20") @Positive(message = "pageSize必须大于0") Integer pageSize,
            @CurrentUser UserDO currentUser) {
        return roadmapService.getRoadmapsByRole(roleId, cursor, sortBy, pageSize, currentUser);
    }

    @PostMapping("/roadmaps")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<RoadmapSaveResult> createRoadmap(
            @RequestBody @Valid CreateRoadmapRequest request,
            @CurrentUser UserDO currentUser) {
        boolean submit = Boolean.TRUE.equals(request.getSubmit());
        RoadmapSaveResult result = roadmapService.createRoadmap(
                request.getRoleId(), request.getContent(), request.getDescription(),
                currentUser.getId(), submit);
        return ResponseEntity.accepted().body(result);
    }

    @PutMapping("/roadmaps/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public RoadmapSaveResult updateRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long id,
            @RequestBody @Valid UpdateRoadmapRequest request,
            @CurrentUser UserDO currentUser) {
        return roadmapService.updateRoadmap(id, request.getContent(), request.getDescription(), currentUser);
    }

    @PostMapping("/roadmaps/{id}/submit")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public RoadmapSaveResult submitRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {
        return roadmapService.submit(id, currentUser);
    }

    @PostMapping("/roadmaps/{id}/withdraw")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> withdrawRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {
        roadmapService.withdraw(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roadmaps/{id}/edit")
    @SaCheckLogin
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public RoadmapEditDTO getRoadmapEdit(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {
        return roadmapService.getEditView(id, currentUser);
    }

    @GetMapping("/roadmaps/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 150, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public RoadmapWithStatusDTO getRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {
        return roadmapService.getRoadmapWithContent(id, currentUser.getId());
    }

    @GetMapping("/users/me/roadmaps")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<RoadmapDetailDTO> getCurrentUserRoadmaps(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "20") @Positive(message = "pageSize必须大于0") Integer pageSize,
            @CurrentUser UserDO currentUser) {

        NewContentState newContentState = null;
        if (state != null && !state.isBlank()) {
            // 用户列表禁止查 BANNED（默认就被 mapper 排除；这里再守一道）
            if (NewContentState.BANNED_VALUE.equals(state)) {
                throw StatusCode.INVALID_PARAMETER.exception("无效的状态参数");
            }
            newContentState = NewContentState.getByValue(state);
            if (newContentState == null) {
                throw StatusCode.INVALID_PARAMETER.exception("无效的状态参数");
            }
        }
        return roadmapService.getUserRoadmaps(currentUser.getId(), cursor, newContentState, pageSize);
    }

    @GetMapping("/users/{userId}/roadmaps")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<RoadmapDetailDTO> getUserRoadmaps(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Positive(message = "pageSize必须大于0") Integer pageSize) {
        return roadmapService.getUserRoadmaps(userId, cursor, NewContentState.PUBLISHED, pageSize);
    }

    @DeleteMapping("/roadmaps/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> deleteRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long id,
            @CurrentUser UserDO currentUser) {
        roadmapService.deleteRoadmap(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
