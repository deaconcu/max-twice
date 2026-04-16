package com.prosper.learn.application.listener;

import com.prosper.learn.application.service.MeilisearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Meilisearch 初始化监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeilisearchInitListener {

    private final MeilisearchService meilisearchService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Meilisearch 索引初始化开始...");
        meilisearchService.initializeIndexes();
    }
}
