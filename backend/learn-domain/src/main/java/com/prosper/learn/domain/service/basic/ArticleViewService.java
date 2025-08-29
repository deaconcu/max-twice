package com.prosper.learn.domain.service.basic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ArticleViewService {

    @Autowired
    private RedisStatsService redisStatsService;

    /**
     * 记录文章访问
     * @param articleId 文章ID
     * @param userId 用户ID（可为null，表示匿名访问）
     * @param ipAddress IP地址（可选，用于防重复）
     */
    public void recordView(long articleId, long userId, String ipAddress) {
        try {
            // 记录到Redis统计
            redisStatsService.recordArticleView(articleId, userId);
            
            log.debug("记录文章访问: articleId={}, userId={}", articleId, userId);
        } catch (Exception e) {
            log.error("记录文章访问失败: articleId={}, userId={}", articleId, userId, e);
        }
    }

    /**
     * 批量记录文章访问（用于列表页面等场景）
     */
    public void recordViewBatch(Long[] articleIds, Integer userId) {
        if (articleIds == null || articleIds.length == 0) {
            return;
        }
        
        for (Long articleId : articleIds) {
            recordView(articleId, userId, null);
        }
    }
}