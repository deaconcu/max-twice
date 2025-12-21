package com.prosper.learn.web.v1.controller;

import com.prosper.learn.application.dto.request.ChatRequest;
import com.prosper.learn.infrastructure.AiService;
import com.prosper.learn.web.ratelimit.LimitType;
import com.prosper.learn.web.ratelimit.RateLimit;
import com.prosper.learn.application.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import java.util.concurrent.TimeUnit;

/**
 * AI功能接口
 * 从AggregateClient拆分出的AI功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@RateLimit(capacity = 20, refillPeriod = 1, refillUnit = TimeUnit.MINUTES, limitType = LimitType.USER)
public class AiController {

    private final AiService aiService;

    /**
     * AI聊天功能
     * 映射: POST /openai → POST /api/v1/ai/chat
     */
    // @PostMapping("/ai/chat")
    // 不需要这个接口了
    public ApiResponse<String> chatWithGPT(@RequestBody @Valid ChatRequest request) {
        
        String answer = aiService.chatWithGPT(request.getPrompt(), request.getModel());
        return ApiResponse.success(answer);
    }
}