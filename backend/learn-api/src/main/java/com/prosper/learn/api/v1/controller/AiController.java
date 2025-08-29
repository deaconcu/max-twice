package com.prosper.learn.api.v1.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.api.v1.dto.ApiResponse;
import com.prosper.learn.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * AI功能接口
 * 从AggregateClient拆分出的AI功能
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiController {

    private static final String API_KEY = "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";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    private final ObjectMapper objectMapper;

    /**
     * AI聊天功能
     * 映射: POST /openai → POST /api/v1/ai/chat
     */
    @PostMapping("/ai/chat")
    public ApiResponse<Object> chatWithGPT(
            @RequestParam String prompt, 
            @RequestParam String model) {
        
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);

        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个老师，能把复杂的问题用生动的方式讲的很容易让人理解");
        messages.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        requestBody.set("messages", messages);
        requestBody.put("temperature", 0.7);

        String jsonRequest = null;
        try {
            jsonRequest = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw ErrorCode.EXTERNAL_SERVICE_ERROR.exception();
        }

        JsonNode root = null;
        try {
            root = objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw ErrorCode.SYSTEM_ERROR.exception(e);
        }
        String answer = root.path("choices").get(0).path("message").path("content").asText();

        return ApiResponse.success(answer);
    }
}