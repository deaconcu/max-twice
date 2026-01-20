package com.prosper.learn.infrastructure.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * OpenAI Embedding服务
 * 负责调用OpenAI API将文本转换为向量
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final RestTemplate restTemplate;

    @Value("${app.openai.api-key}")
    private String apiKey;

    @Value("${app.openai.api-url}")
    private String apiUrl;

    @Value("${app.openai.model}")
    private String model;

    /**
     * 生成文本的embedding向量
     *
     * @param text 输入文本
     * @return 1536维浮点向量数组
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> request = Map.of(
                    "model", model,
                    "input", text.trim()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                    apiUrl,
                    entity,
                    EmbeddingResponse.class
            );

            log.info("Embedding API Response: status={}, body={}", response.getStatusCode(), response.getBody());

            if (response.getBody() == null || response.getBody().getData().isEmpty()) {
                throw new RuntimeException("Empty response from OpenAI");
            }

            float[] embedding = response.getBody().getData().get(0).getEmbedding();
            log.debug("Generated embedding for text (length={}), vector dim={}", text.length(), embedding.length);

            return embedding;

        } catch (Exception e) {
            log.error("Failed to generate embedding for text: {}", text.substring(0, Math.min(50, text.length())), e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }

    /**
     * 批量生成embedding（提高效率）
     *
     * @param texts 文本列表（最多2048个）
     * @return 向量列表
     */
    public List<float[]> generateEmbeddingsBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("Texts cannot be empty");
        }

        if (texts.size() > 2048) {
            throw new IllegalArgumentException("Maximum 2048 texts allowed in batch");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> request = Map.of(
                    "model", model,
                    "input", texts
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                    apiUrl,
                    entity,
                    EmbeddingResponse.class
            );

            log.info("Batch Embedding API Response: status={}, body={}", response.getStatusCode(), response.getBody());

            if (response.getBody() == null || response.getBody().getData().isEmpty()) {
                throw new RuntimeException("Empty response from OpenAI");
            }

            List<float[]> embeddings = response.getBody().getData().stream()
                    .map(EmbeddingData::getEmbedding)
                    .toList();

            log.debug("Generated {} embeddings in batch", embeddings.size());

            return embeddings;

        } catch (Exception e) {
            log.error("Failed to generate embeddings batch (size={})", texts.size(), e);
            throw new RuntimeException("Batch embedding generation failed", e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class EmbeddingResponse {
        private List<EmbeddingData> data;
        private String model;
        private Usage usage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class EmbeddingData {
        private float[] embedding;
        private int index;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
