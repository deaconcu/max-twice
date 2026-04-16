package com.prosper.learn.infrastructure.ai;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Gemini API 调用服务
 * 使用 Google Gemini SDK 进行内容生成
 * API Key 通过环境变量 GEMINI_API_KEY 传入
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final SystemProperties systemProperties;

    // Gemini Client（延迟初始化，从环境变量 GEMINI_API_KEY 读取 API Key）
    private volatile Client geminiClient;

    /**
     * 生成内容
     *
     * @param prompt 用户提示词
     * @param systemPrompt 系统提示词（可选）
     * @return AI 生成的文本内容
     */
    public String generateContent(String prompt, String systemPrompt) {
        try {
            Client client = getOrCreateClient();
            String modelName = systemProperties.getRobot().getModel();

            // 构建配置
            GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                configBuilder.systemInstruction(
                    Content.fromParts(Part.fromText(systemPrompt)));
            }

            GenerateContentConfig config = configBuilder.build();

            log.info("Gemini 调用 API: model={}，prompt 长度={}，system prompt 长度={}",
                modelName, prompt.length(), systemPrompt != null ? systemPrompt.length() : 0);

            // 调用 Gemini API
            GenerateContentResponse response =
                client.models.generateContent(modelName, prompt, config);

            String text = response.text();
            log.info("Gemini 响应长度: {}", text != null ? text.length() : 0);
            log.info("Gemini 完整响应: {}", text);

            return text;
        } catch (Exception e) {
            log.error("Gemini API 调用失败", e);
            throw new RuntimeException("Gemini API call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获取或创建 Gemini Client（延迟初始化，双重检查锁）
     */
    private Client getOrCreateClient() {
        if (geminiClient == null) {
            synchronized (this) {
                if (geminiClient == null) {
                    // Client 从环境变量 GEMINI_API_KEY 读取 API Key
                    geminiClient = new Client();
                    log.info("Gemini Client 初始化完成（API Key 来自环境变量 GEMINI_API_KEY）");
                }
            }
        }
        return geminiClient;
    }
}
