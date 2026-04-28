package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.CreateRoadmapRequest;
import com.twicemax.application.dto.request.UpdateRoadmapRequest;
import com.twicemax.application.dto.response.roadmap.RoadmapDetailDTO;
import com.twicemax.application.dto.response.roadmap.RoadmapWithStatusDTO;
import com.twicemax.application.dto.v2.CreateAcceptedResponse;
import com.twicemax.application.service.RoadmapService;
import com.twicemax.shared.domain.Enums.ContentState;
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

/**
 * 路线图接口
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

    @PutMapping("/roadmaps/{id}")
    @SaCheckLogin
    @RateLimit(capacity = 30, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> updateRoadmap(
            @PathVariable @NotNull(message = "路线图ID不能为空") @Positive(message = "路线图ID必须大于0") Long id,
            @RequestBody @Valid UpdateRoadmapRequest request,
            @CurrentUser UserDO currentUser) {
        roadmapService.updateRoadmap(id, request.getContent(), request.getDescription(), request.getState(), currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/roadmaps")
    @SaCheckLogin
    @RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<CreateAcceptedResponse> createRoadmap(
            @RequestBody @Valid CreateRoadmapRequest request,
            @CurrentUser UserDO currentUser) {
        Long roadmapId = roadmapService.createRoadmap(
                request.getRoleId(), request.getContent(), request.getDescription(),
                currentUser.getId(), request.getState());
        return ResponseEntity.accepted().body(new CreateAcceptedResponse(roadmapId));
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
            @RequestParam(required = false) Integer state,
            @RequestParam(defaultValue = "20") @Positive(message = "pageSize必须大于0") Integer pageSize,
            @CurrentUser UserDO currentUser) {

        ContentState contentState = null;
        if (state != null) {
            if (state.byteValue() == ContentState.BANNED.value()) {
                throw StatusCode.INVALID_PARAMETER.exception("无效的状态参数");
            }
            contentState = ContentState.getByValue(state.byteValue());
            if (contentState == null) {
                throw StatusCode.INVALID_PARAMETER.exception("无效的状态参数");
            }
        }
        return roadmapService.getUserRoadmaps(currentUser.getId(), cursor, contentState, pageSize);
    }

    @GetMapping("/users/{userId}/roadmaps")
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public List<RoadmapDetailDTO> getUserRoadmaps(
            @PathVariable @NotNull(message = "用户ID不能为空") @Positive(message = "用户ID必须大于0") Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Positive(message = "pageSize必须大于0") Integer pageSize) {
        return roadmapService.getUserRoadmaps(userId, cursor, ContentState.PUBLISHED, pageSize);
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
