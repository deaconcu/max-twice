package com.prosper.learn.business.service.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.common.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * AI服务
 * 
 * 负责与外部AI API进行交互，提供聊天对话功能
 * 
 * 主要功能：
 * - 与GPT模型进行对话交互
 * - 支持多种AI模型切换
 * - 请求重试机制
 * - 参数验证和错误处理
 * 
 * @author Claude
 * @since 2024-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ObjectMapper objectMapper;
    private final SystemProperties systemProperties;

    // ========== 私有验证方法 ==========
    
    /**
     * 验证提示词
     */
    private void validatePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw ErrorCode.AI_SERVICE_INVALID_PARAMETER.exception("提示词不能为空");
        }
        if (prompt.length() > 10000) {
            throw ErrorCode.AI_SERVICE_INVALID_PARAMETER.exception("提示词长度不能超过10000字符");
        }
    }
    
    /**
     * 验证模型名称
     */
    private void validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            throw ErrorCode.AI_SERVICE_INVALID_PARAMETER.exception("模型名称不能为空");
        }
    }
    
    /**
     * 创建HTTP客户端
     */
    private HttpClient createHttpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(systemProperties.getAi().getRequestTimeoutMs()))
            .build();
    }
    
    /**
     * 创建消息数组
     */
    private ArrayNode createMessagesArray(String prompt) {
        ArrayNode messages = objectMapper.createArrayNode();

        // 添加系统消息
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemProperties.getAi().getSystemPrompt());
        messages.add(systemMessage);

        // 添加用户消息
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        return messages;
    }

    /**
     * 与GPT模型进行对话
     * 
     * @param prompt 用户输入的提示词
     * @param model AI模型名称
     * @return AI回复内容
     * @throws com.prosper.learn.common.exception.BusinessException 当参数无效或服务调用失败时抛出异常
     */
    public String chatWithGPT(String prompt, String model) {
        validatePrompt(prompt);
        validateModel(model);
        
        if (systemProperties.getAi().isEnableRequestLogging()) {
            log.info("AI请求 - 模型: {}, 提示词长度: {}", model, prompt.length());
        }
        
        String requestBody = buildRequestBody(prompt, model);
        String responseBody = callAiApiWithRetry(requestBody);
        return parseResponse(responseBody);
    }

    /**
     * 与GPT模型进行对话（使用默认模型）
     * 
     * @param prompt 用户输入的提示词
     * @return AI回复内容
     */
    public String chatWithGPT(String prompt) {
        return chatWithGPT(prompt, systemProperties.getAi().getDefaultModel());
    }

    /**
     * 构建请求体
     */
    private String buildRequestBody(String prompt, String model) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.set("messages", createMessagesArray(prompt));
            requestBody.put("temperature", systemProperties.getAi().getTemperature());

            return objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            log.error("构建AI请求体失败", e);
            throw ErrorCode.AI_SERVICE_REQUEST_FAILED.exception(e);
        }
    }

    /**
     * 调用AI API（带重试机制）
     */
    private String callAiApiWithRetry(String requestBody) {
        int maxAttempts = systemProperties.getAi().getMaxRetryAttempts();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callAiApi(requestBody);
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    log.warn("AI服务调用失败，第{}次重试", attempt, e);
                    try {
                        Thread.sleep(1000 * attempt); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ErrorCode.AI_SERVICE_REQUEST_FAILED.exception(ie);
                    }
                }
            }
        }
        
        log.error("AI服务调用失败，已重试{}次", maxAttempts, lastException);
        throw ErrorCode.AI_SERVICE_REQUEST_FAILED.exception(lastException);
    }

    /**
     * 调用AI API
     */
    private String callAiApi(String requestBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(systemProperties.getAi().getApiUrl()))
                .header("Authorization", "Bearer " + systemProperties.getAi().getApiKey())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(systemProperties.getAi().getRequestTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = createHttpClient();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                log.error("AI服务返回错误状态码: {}, 响应: {}", response.statusCode(), response.body());
                throw ErrorCode.AI_SERVICE_REQUEST_FAILED.exception("AI服务返回错误状态码: " + response.statusCode());
            }
            
            return response.body();
        } catch (IOException | InterruptedException e) {
            log.error("AI服务网络请求失败", e);
            throw ErrorCode.AI_SERVICE_REQUEST_FAILED.exception(e);
        }
    }

    /**
     * 解析响应
     */
    private String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            // 检查是否有错误
            if (root.has("error")) {
                String errorMessage = root.path("error").path("message").asText("未知错误");
                log.error("AI服务返回错误: {}", errorMessage);
                throw ErrorCode.AI_SERVICE_REQUEST_FAILED.exception("AI服务错误: " + errorMessage);
            }
            
            // 提取回复内容
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                throw ErrorCode.AI_SERVICE_RESPONSE_PARSE_FAILED.exception("响应中没有choices字段");
            }
            
            String content = choices.get(0).path("message").path("content").asText();
            if (content.isEmpty()) {
                throw ErrorCode.AI_SERVICE_RESPONSE_PARSE_FAILED.exception("响应内容为空");
            }
            
            return content;
        } catch (JsonProcessingException e) {
            log.error("解析AI服务响应失败: {}", responseBody, e);
            throw ErrorCode.AI_SERVICE_RESPONSE_PARSE_FAILED.exception(e);
        }
    }
}