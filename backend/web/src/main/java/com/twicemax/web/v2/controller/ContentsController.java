package com.twicemax.web.v2.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.twicemax.application.dto.request.PostContentsRequest;
import com.twicemax.content.toc.TocDomainService;
import com.twicemax.shared.domain.Enums;
import com.twicemax.shared.domain.exception.StatusCode;
import com.twicemax.user.profile.UserDO;
import com.twicemax.web.ratelimit.LimitType;
import com.twicemax.web.ratelimit.RateLimit;
import com.twicemax.web.v2.annotation.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 内容管理接口
 */
@RestController
@RequestMapping("/contents")
@RequiredArgsConstructor
@Validated
public class ContentsController {

    private final TocDomainService tocDomainService;

    @PostMapping
    @SaCheckLogin
    @RateLimit(capacity = 60, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
    public ResponseEntity<Void> postContents(
            @RequestBody @Valid PostContentsRequest request,
            @CurrentUser UserDO currentUser) {

        Enums.ContentAction action = request.getActionEnum();
        if (action == null) {
            throw StatusCode.INVALID_PARAMETER.exception("无效的操作类型");
        }

        switch (action) {
            case CHOOSE -> tocDomainService.choose(
                    currentUser.getId(),
                    request.getPath(),
                    request.getNodeId(),
                    request.getPostingId()
            );
            case UNCHOOSE -> tocDomainService.unchoose(
                    currentUser.getId(),
                    request.getNodeId(),
                    request.getPath()
            );
            default -> throw StatusCode.NOT_SUPPORTED.exception();
        }
        return ResponseEntity.noContent().build();
    }
}
