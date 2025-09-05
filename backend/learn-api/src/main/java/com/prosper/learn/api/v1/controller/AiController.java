package com.prosper.learn.api.v1.controller;

import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.external.AiService;
import com.prosper.learn.dto.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * AI功能接口
 * 从AggregateClient拆分出的AI功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * AI聊天功能
     * 映射: POST /openai → POST /api/v1/ai/chat
     */
    @PostMapping("/ai/chat")
    public ApiResponse<String> chatWithGPT(@RequestBody @Valid ChatRequest request) {
        
        String answer = aiService.chatWithGPT(request.getPrompt(), request.getModel());
        return ApiResponse.success(answer);
    }
}