package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.service.TocService;
import com.twicemax.content.toc.UserNodeTocDO;
import com.twicemax.content.toc.UserNodeTocDataService;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import com.twicemax.web.v2.annotation.JsonParam;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 节点目录接口
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class TocController {

    private final TocService tocService;
    private final UserNodeTocDataService userNodeTocDataService;

    @PutMapping("/users/current/nodes/{nodeId}/toc")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> updateUserNodeToc(
            @PathVariable @NotNull(message = "节点ID不能为空") @Positive(message = "节点ID必须大于0") Long nodeId,
            @JsonParam("indexArray") @NotBlank(message = "索引数组不能为空") String indexArray,
            @CurrentUser UserDO currentUser) {
        tocService.updateUserNodeToc(currentUser.getId(), nodeId, indexArray);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/current/nodes/{nodeId}/toc")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public String getUserNodeToc(
            @PathVariable @NotNull(message = "节点ID不能为空") @Positive(message = "节点ID必须大于0") Long nodeId,
            @CurrentUser UserDO currentUser) {
        UserNodeTocDO userNodeTocDO = userNodeTocDataService.getByUserAndNode(currentUser.getId(), nodeId);
        return userNodeTocDO != null ? userNodeTocDO.getToc() : null;
    }
}
