package com.prosper.learn.api.v1.controller;

import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.domain.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<String> chatWithGPT(
            @RequestParam String prompt, 
            @RequestParam String model) {
        
        String answer = aiService.chatWithGPT(prompt, model);
        return ApiResponse.success(answer);
    }
}