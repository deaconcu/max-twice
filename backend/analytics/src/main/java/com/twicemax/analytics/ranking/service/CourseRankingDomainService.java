package com.twicemax.analytics.ranking.service;

import com.twicemax.analytics.stats.dataservice.ContentStatsDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static com.twicemax.shared.domain.Enums.ContentType;

/**
 * 课程排行榜业务服务
 *
 * 基于 content_stats 表的实时统计数据提供排行榜功能
 *
 * 设计思路：
 * 1. 数据源：content_stats 表（通过事件实时更新）
 * 2. 排序规则：综合热度 = 收藏数 + 学习中人数 + 已完成人数
 * 3. 缓存策略：使用 Spring Cache + Redis，5分钟缓存
 * 4. 性能优化：数据库层面可添加虚拟列 + 索引进一步提升性能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseRankingDomainService {

    private final ContentStatsDataService contentStatsDataService;

    /**
     * 获取热门课程ID列表（按综合热度排序）
     *
     * 综合热度计算公式：
     * popularity = bookmarks + in_progress_users + completed_users
     *
     * 缓存策略：
     * - 缓存键：hotCourses:{limit}
     * - 过期时间：5分钟（通过配置文件设置）
     * - 缓存条件：结果非空
     *
     * @param limit 返回数量限制（1-200）
     * @return 课程ID列表，按热度降序排列
     */
    @Cacheable(value = "hotCourses", key = "#limit", unless = "#result.isEmpty()")
    public List<Long> getHotCourseIds(int limit) {
        // 参数验证
        if (limit <= 0) {
            log.warn("课程排行榜 limit 参数无效: {}", limit);
            return Collections.emptyList();
        }

        if (limit > 200) {
            log.warn("课程排行榜 limit {} 超过最大值，限制为 200", limit);
            limit = 200;
        }

        try {
            List<Long> courseIds = contentStatsDataService.getTopContentIdsByPopularity(
                ContentType.course, limit);

            log.debug("课程排行榜 获取热门课程 {} 个", courseIds.size());
            return courseIds;

        } catch (Exception e) {
            log.error("课程排行榜 获取热门课程失败，limit: {}", limit, e);
            return Collections.emptyList();
        }
    }
}
