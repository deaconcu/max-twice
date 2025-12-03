package com.prosper.learn.business.service.autoauthor;

import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.business.service.data.NodeDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AutoAuthor 兜底扫描器（分页扫描）
 *
 * 段落说明：
 * 1) 维护扫描游标：Redis key autoAuthor:scan:lastId
 * 2) 每次按 id 递增获取一页缺少指定用户帖子（未删除）的节点
 * 3) 逐个入就绪队列；更新游标；到尾部则重置为0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoAuthorScanner {

    // ========= 依赖 =========

    private final NodeDataService nodeDataService;
    private final AutoAuthorQueueService queueService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SystemProperties systemProperties;

    // ========= 进度游标 =========

    private String progressKey() {
        return systemProperties.getAutoAuthor().getRedisKeyPrefix() + "scan:lastId";
    }

    private long getLastId() {
        Object v = redisTemplate.opsForValue().get(progressKey());
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; }
    }

    private void setLastId(long id) {
        redisTemplate.opsForValue().set(progressKey(), Long.toString(id));
    }

    // ========= 扫描实现 =========

    /**
     * 扫描一页并入队，返回入队数量
     */
    public int scanOnePage() {
        if (!systemProperties.getAutoAuthor().isEnabled()) return 0;
        long userId = systemProperties.getAutoAuthor().getAiUserId();
        long afterId = getLastId();
        int limit = 1000;
        List<Long> ids = nodeDataService.selectIdsByUserIdAndPost(afterId, userId, limit);
        if (ids == null || ids.isEmpty()) {
            setLastId(0L);
            return 0;
        }
        int enqueued = 0;
        long last = afterId;
        for (Long id : ids) {
            if (id == null || id <= 0) continue;
            queueService.enqueue(id);
            enqueued++;
            last = id;
        }
        setLastId(last);
        return enqueued;
    }

    /**
     * 定时扫描：限制最多10页，避免长时间占用
     */
    @Scheduled(cron = "#{systemProperties.autoAuthor.scanCron}")
    public void scheduledScan() {
        int total = 0;
        for (int i = 0; i < 10; i++) {
            int c = scanOnePage();
            total += c;
            if (c == 0) break;
        }
        if (total > 0) log.info("auto-author scan enqueued {} nodes", total);
    }
}
