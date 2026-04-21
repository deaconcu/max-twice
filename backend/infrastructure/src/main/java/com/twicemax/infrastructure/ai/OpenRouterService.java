package com.twicemax.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twicemax.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenRouter API 调用服务
 * 使用 OpenRouter 统一接口访问多个 AI 模型
 * API Key 通过环境变量 OPENROUTER_API_KEY 传入
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterService {

    private final SystemProperties systemProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";

    /**
     * 生成内容
     *
     * @param prompt 用户提示词
     * @param systemPrompt 系统提示词（可选）
     * @return AI 生成的文本内容
     */
    public String generateContent(String prompt, String systemPrompt) {
        try {
            String apiKey = getApiKey();
            String modelName = systemProperties.getRobot().getModel();

            log.info("OpenRouter 调用 API: model={}，prompt 长度={}，system prompt 长度={}",
                modelName, prompt.length(), systemPrompt != null ? systemPrompt.length() : 0);

            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messages.add(systemMessage);
            }

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("messages", messages);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            //headers.set("HTTP-Referer", systemProperties.getRobot().getSiteUrl());
            //headers.set("X-Title", systemProperties.getRobot().getSiteName());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 调用 API
            String response = restTemplate.postForObject(OPENROUTER_API_URL, entity, String.class);

            //log.info("OpenRouter raw response: {}", response);

            // 解析响应
            JsonNode jsonNode = objectMapper.readTree(response);
            String text = jsonNode.path("choices").get(0).path("message").path("content").asText();

            log.info("OpenRouter 响应长度: {}", text != null ? text.length() : 0);
            log.info("OpenRouter 提取的文本: {}", text);

            return text;

        } catch (Exception e) {
            log.error("OpenRouter API 调用失败", e);
            throw new RuntimeException("OpenRouter API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 从配置获取 API Key
     */
    private String getApiKey() {
        String apiKey = systemProperties.getRobot().getOpenrouterApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OpenRouter API Key not configured in app.robot.openrouter-api-key");
        }
        return apiKey;
    }
}
