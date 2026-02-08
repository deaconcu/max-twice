package com.prosper.learn.infrastructure.ai;

import com.prosper.learn.shared.infrastructure.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 服务统一入口
 * 根据配置的 aiService 选择对应的服务提供商
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final GeminiService geminiService;
    private final OpencodeService opencodeService;
    private final OpenRouterService openRouterService;
    private final SystemProperties systemProperties;

    /**
     * 生成内容（统一入口）
     *
     * @param prompt 用户提示词
     * @param systemPrompt 系统提示词（可选）
     * @return AI 生成的文本内容
     */
    public String generateContent(String prompt, String systemPrompt) {
        String aiService = systemProperties.getRobot().getAiService();

        log.info("Using AI service: {}, model: {}", aiService, systemProperties.getRobot().getModel());

        if ("gemini".equalsIgnoreCase(aiService)) {
            return geminiService.generateContent(prompt, systemPrompt);
        } else if ("openrouter".equalsIgnoreCase(aiService)) {
            return openRouterService.generateContent(prompt, systemPrompt);
        } else {
            // 默认使用 opencode
            return opencodeService.generateContent(prompt, systemPrompt);
        }
    }

    /**
     * 重置会话（仅 OpenCode 支持）
     */
    public void resetSession() {
        String aiService = systemProperties.getRobot().getAiService();
        if ("opencode".equalsIgnoreCase(aiService)) {
            opencodeService.resetSession();
        } else {
            log.info("{} service does not have session, reset ignored", aiService);
        }
    }

    /**
     * 压缩当前 session 的上下文（仅 OpenCode 支持）
     */
    public void summarizeCurrentSession() {
        String aiService = systemProperties.getRobot().getAiService();
        if ("opencode".equalsIgnoreCase(aiService)) {
            opencodeService.summarizeSession();
        } else {
            log.info("{} service does not have session, summarize ignored", aiService);
        }
    }
}
