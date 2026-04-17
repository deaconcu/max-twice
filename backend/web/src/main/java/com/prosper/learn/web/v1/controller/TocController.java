package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.prosper.learn.application.service.TocService;
import com.prosper.learn.content.toc.UserNodeTocDO;
import com.prosper.learn.content.toc.UserNodeTocDataService;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.web.v1.annotation.CurrentUser;
import com.prosper.learn.web.v1.annotation.JsonParam;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import com.prosper.learn.application.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 节点目录接口
 * 处理用户节点目录的相关操作
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Validated
public class TocController {

    private final TocService tocService;
    private final UserNodeTocDataService userNodeTocDataService;

    /**
     * 更新用户节点目录
     * 映射: PUT /api/v1/users/current/nodes/{nodeId}/toc
     */
    @PutMapping("/users/current/nodes/{nodeId}/toc")
    @SaCheckLogin
    @RateLimit(capacity = 50, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<Void> updateUserNodeToc(
            @PathVariable @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            Long nodeId,
            @JsonParam("indexArray") @NotBlank(message = "索引数组不能为空") String indexArray,
            @CurrentUser UserDO currentUser) {

        tocService.updateUserNodeToc(currentUser.getId(), nodeId, indexArray);
        return ApiResponse.success("目录更新成功", null);
    }

    /**
     * 获取用户节点目录
     * GET /api/v1/users/current/nodes/{nodeId}/toc
     */
    @GetMapping("/users/current/nodes/{nodeId}/toc")
    @SaCheckLogin
    @RateLimit(capacity = 100, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ApiResponse<String> getUserNodeToc(
            @PathVariable @NotNull(message = "节点ID不能为空")
            @Positive(message = "节点ID必须大于0")
            Long nodeId,
            @CurrentUser UserDO currentUser) {

        UserNodeTocDO userNodeTocDO = userNodeTocDataService.getByUserAndNode(currentUser.getId(), nodeId);
        if (userNodeTocDO == null) {
            return ApiResponse.success(null);
        }

        return ApiResponse.success(userNodeTocDO.getToc());
    }
}