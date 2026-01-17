package com.prosper.learn.application.service;

import com.prosper.learn.analytics.stats.service.RedisStatsDomainService;
import com.prosper.learn.content.post.PostDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 统计应用服务
 *
 * 负责统计相关的业务逻辑和验证：
 * - 验证业务对象是否存在
 * - 协调领域服务完成统计记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatService {

    private final PostDataService postDataService;
    private final RedisStatsDomainService redisStatsDomainService;

    /**
     * 记录文章访问
     *
     * @param postId 文章ID
     * @param userId 用户ID（0表示匿名访问）
     */
    public void recordPostView(long postId, long userId) {
        // 验证文章是否存在
        postDataService.validateAndGet(postId);

        // 记录访问统计
        redisStatsDomainService.recordArticleView(postId, userId);

        log.debug("记录文章访问: postId={}, userId={}", postId, userId);
    }
}
