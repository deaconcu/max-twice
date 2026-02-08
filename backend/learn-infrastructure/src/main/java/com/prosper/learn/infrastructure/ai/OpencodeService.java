package com.prosper.learn.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * OpenCode API 调用服务
 * 负责与本地 OpenCode 服务交互，包括 session 管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpencodeService {

    private final SystemProperties systemProperties;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String SESSION_KEY = "robot:session:id";
    private static final long SESSION_EXPIRE_HOURS = 24;

    /**
     * 生成内容
     *
     * @param prompt 用户提示词
     * @param systemPrompt 系统提示词（可选）
     * @return OpenCode 响应的完整 JSON 字符串
     */
    public String generateContent(String prompt, String systemPrompt) {
        String sessionId = getOrCreateSession();
        return sendMessageWithRetry(sessionId, prompt, systemPrompt);
    }

    /**
     * 重置会话：删除 Redis 中的会话键
     */
    public void resetSession() {
        redisTemplate.delete(SESSION_KEY);
        log.info("OpenCode session reset, deleted key: {}", SESSION_KEY);
    }

    /**
     * 压缩当前 session 的上下文
     */
    public void summarizeSession() {
        String sessionId = redisTemplate.opsForValue().get(SESSION_KEY);
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("No active OpenCode session to summarize");
            return;
        }
        summarizeSessionById(sessionId);
    }

    /**
     * 获取或创建 sessionId
     */
    private String getOrCreateSession() {
        String sessionId = redisTemplate.opsForValue().get(SESSION_KEY);
        if (sessionId != null && !sessionId.isEmpty()) {
            log.debug("Reusing existing OpenCode session: {}", sessionId);
            return sessionId;
        }

        sessionId = createSession();
        redisTemplate.opsForValue().set(SESSION_KEY, sessionId, SESSION_EXPIRE_HOURS, TimeUnit.HOURS);
        log.info("Created new OpenCode session: {}", sessionId);
        return sessionId;
    }

    /**
     * 创建 OpenCode 会话（POST /session）
     */
    private String createSession() {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("title", "auto-author");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(systemProperties.getRobot().getOpencodeBaseUrl() + "/session"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) throw new RuntimeException("create session failed: " + resp.statusCode());
            JsonNode json = objectMapper.readTree(resp.body());
            String id = json.path("id").asText();
            if (id == null || id.isEmpty()) throw new RuntimeException("empty session id");
            return id;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 压缩 session 上下文（调用 summarize 接口）
     */
    private void summarizeSessionById(String sessionId) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("providerID", systemProperties.getRobot().getProviderId());
            body.put("modelID", systemProperties.getRobot().getModel());
            body.put("auto", false);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(systemProperties.getRobot().getOpencodeBaseUrl() + "/session/" + sessionId + "/summarize"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("Summarize OpenCode session failed: {}", resp.statusCode());
            } else {
                log.info("OpenCode session summarized successfully");
            }
        } catch (Exception e) {
            log.warn("Failed to summarize OpenCode session", e);
            // 不抛出异常，summarize 失败不影响主流程
        }
    }

    /**
     * 发送消息（带重试机制）
     */
    private String sendMessageWithRetry(String sessionId, String prompt, String systemPrompt) {
        try {
            return sendMessage(sessionId, prompt, systemPrompt);
        } catch (Exception e) {
            log.warn("Send message to OpenCode failed with session {}: {}", sessionId, e.getMessage());
            // 检查 Redis 中是否还有 sessionId，有的话就抛出异常不重连
            String existingSessionId = redisTemplate.opsForValue().get(SESSION_KEY);
            if (existingSessionId != null && !existingSessionId.isEmpty()) {
                log.info("Session exists in redis, not reconnecting");
                throw e;
            }

            // Redis 中没有 sessionId 时才重新创建
            log.info("No session in redis, creating new session");
            String newSessionId = getOrCreateSession();
            return sendMessage(newSessionId, prompt, systemPrompt);
        }
    }

    /**
     * 发送消息到 OpenCode（POST /session/:id/message）
     */
    private String sendMessage(String sessionId, String prompt, String systemPrompt) {
        try {
            ObjectNode model = objectMapper.createObjectNode();
            model.put("providerID", systemProperties.getRobot().getProviderId());
            model.put("modelID", systemProperties.getRobot().getModel());

            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("type", "text");
            part.put("text", prompt);
            parts.add(part);

            ObjectNode root = objectMapper.createObjectNode();
            root.set("model", model);
            root.set("parts", parts);
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                root.put("system", systemPrompt);
            }

            log.info("Send to OpenCode: {}", root);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(systemProperties.getRobot().getOpencodeBaseUrl() + "/session/" + sessionId + "/message"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(300)) // 5分钟超时
                    .POST(HttpRequest.BodyPublishers.ofString(root.toString(), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) throw new RuntimeException("send message failed: " + resp.statusCode());

            String responseBody = resp.body();
            log.info("OpenCode full response: {}", responseBody);

            return responseBody;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
