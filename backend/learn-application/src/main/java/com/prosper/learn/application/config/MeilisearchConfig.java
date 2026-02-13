package com.prosper.learn.application.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Meilisearch 配置
 */
@Configuration
public class MeilisearchConfig {

    @Value("${meilisearch.host}")
    private String host;

    @Value("${meilisearch.api-key}")
    private String apiKey;

    @Bean
    @ConditionalOnProperty(name = "meilisearch.enabled", havingValue = "true")
    public Client meilisearchClient() {
        Config config = new Config(host, apiKey);
        return new Client(config);
    }
}
