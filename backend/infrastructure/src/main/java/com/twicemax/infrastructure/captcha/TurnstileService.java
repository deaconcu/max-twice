package com.twicemax.infrastructure.captcha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Cloudflare Turnstile 验证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TurnstileService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @Value("${turnstile.secret-key:1x0000000000000000000000000000000AA}")
    private String secretKey;

    @Value("${turnstile.enabled:true}")
    private boolean enabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 验证 Turnstile token
     *
     * @param token 前端传来的 turnstile token
     * @param remoteIp 用户 IP（可选）
     * @return 验证是否通过
     */
    public boolean verify(String token, String remoteIp) {
        if (!enabled) {
            log.debug("Turnstile 已禁用，跳过验证");
            return true;
        }

        if (token == null || token.isBlank()) {
            log.warn("Turnstile token 为空");
            return false;
        }

        log.debug("Turnstile 验证开始: remoteIp={}", remoteIp);

        try {
            // 构建请求参数
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", secretKey);
            params.add("response", token);
            if (remoteIp != null && !remoteIp.isBlank()) {
                params.add("remoteip", remoteIp);
            }

            // 发送验证请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(VERIFY_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                TurnstileResponse result = objectMapper.readValue(response.getBody(), TurnstileResponse.class);

                if (result.isSuccess()) {
                    log.debug("Turnstile 验证通过");
                    return true;
                } else {
                    log.warn("Turnstile 验证失败: errorCodes={}", result.getErrorCodes());
                    return false;
                }
            }

            log.error("Turnstile 验证请求失败: status={}", response.getStatusCode());
            return false;

        } catch (Exception e) {
            log.error("Turnstile 验证异常", e);
            // 验证服务异常时，根据配置决定是否放行
            return false;
        }
    }

    /**
     * Turnstile 验证响应
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TurnstileResponse {
        private boolean success;

        @JsonProperty("error-codes")
        private List<String> errorCodes;

        @JsonProperty("challenge_ts")
        private String challengeTs;

        private String hostname;
    }
}
