package com.prosper.learn.analytics.stats.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.analytics.stats.dataservice.ContentStatsDataService;
import com.prosper.learn.analytics.stats.mapper.ContentStatsYearlyDO;
import com.prosper.learn.analytics.stats.mapper.ContentStatsYearlyMapper;
import com.prosper.learn.shared.common.util.TimeZoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static com.prosper.learn.shared.common.constants.RedisStatsConstants.*;
import static com.prosper.learn.shared.domain.Enums.ContentType;

/**
 * 内容统计同步服务
 *
 * 负责将Redis中的内容统计数据同步到数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentStatsSyncService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ContentStatsYearlyMapper contentStatsYearlyMapper;
    private final ContentStatsDataService contentStatsDataService;
    private final ObjectMapper objectMapper;

    /** 每批处理的Redis记录数 */
    private static final int SCAN_BATCH_SIZE = 1000;
    /** 每批保存到数据库的内容数（避免内存占用过大） */
    private static final int DB_SAVE_BATCH_SIZE = 5000;

    /**
     * 同步文章统计数据
     *
     * 处理逻辑:
     * 1. 使用HSCAN从Redis分批获取指定日期的文章统计数据
     * 2. 解析field格式: "contentType:contentId:statType" -> count
     * 3. 按内容ID分组聚合各种统计类型的数据
     * 4. 分批写入数据库，避免内存溢出
     * 5. 删除Redis中的原始数据
     *
     * @param dateStr 日期字符串，格式: YYYY-MM-DD
     * @return 成功同步的记录数
     */
    public int syncPostStats(String dateStr) {
        String postKey = generatePostStatsKey(dateStr);

        // 使用 HSCAN 游标分批获取数据
        ScanOptions scanOptions = ScanOptions.scanOptions()
            .count(SCAN_BATCH_SIZE)
            .build();

        Cursor<Map.Entry<Object, Object>> cursor = null;
        try {
            cursor = redisTemplate.opsForHash().scan(postKey, scanOptions);

            if (!cursor.hasNext()) {
                log.info("{}没有文章统计数据", dateStr);
                return 0;
            }

            // 解析日期信息
            LocalDate date = LocalDate.parse(dateStr);
            int year = date.getYear();
            String dayKey = dateStr.substring(5).replace("-", "-"); // "08-22"

            // 按内容ID分组聚合数据
            Map<Long, PostDayStats> postStatsMap = new HashMap<>();
            int totalEntries = 0;
            int totalSaved = 0;

            // 分批处理 Redis 数据
            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();
                String field = (String) entry.getKey();
                Integer count = Integer.parseInt((String) entry.getValue());

                if (count <= 0) continue;

                String[] parts = field.split(":");
                if (parts.length != 3) continue; // 格式: contentType:contentId:statType

                // 解析格式: contentType:contentId:statType
                String contentType = parts[0];
                Long postId = Long.parseLong(parts[1]);
                String redisStatType = parts[2];

                // 只处理 POST 类型的内容
                if (!String.valueOf(ContentType.post.value()).equals(contentType)) {
                    continue;
                }

                PostDayStats dayStats = postStatsMap.computeIfAbsent(postId, k -> new PostDayStats());

                // 修复字段名映射：Redis使用单数，数据库使用复数
                switch (redisStatType) {
                    case STAT_TYPE_VIEW:
                        dayStats.views += count;
                        break;
                    case STAT_TYPE_TWICE:
                        dayStats.twice += count;
                        break;
                    case STAT_TYPE_LIKE:
                        dayStats.like += count;
                        break;
                    case STAT_TYPE_COMMENT:
                        dayStats.comments += count;
                        break;
                    default:
                        log.debug("忽略未知的统计类型: {}", redisStatType);
                }

                totalEntries++;

                // 每处理 DB_SAVE_BATCH_SIZE 个内容，批量写入数据库一次
                if (postStatsMap.size() >= DB_SAVE_BATCH_SIZE) {
                    int batchSaved = saveBatchPostStats(postStatsMap, year, dayKey, dateStr);
                    totalSaved += batchSaved;
                    log.info("已处理 {} 条Redis记录，批量保存 {} 个内容的统计", totalEntries, batchSaved);
                    postStatsMap.clear(); // 清空已处理的数据，释放内存
                }
            }

            // 处理剩余的数据
            if (!postStatsMap.isEmpty()) {
                int finalSaved = saveBatchPostStats(postStatsMap, year, dayKey, dateStr);
                totalSaved += finalSaved;
                log.info("最后批次保存 {} 个内容的统计", finalSaved);
            }

            log.info("同步{}的内容数据完成: 总计处理 {} 条Redis记录，保存 {} 个内容",
                dateStr, totalEntries, totalSaved);

            // 同步成功后删除Redis数据（只有非当天的数据才删除）
            LocalDate today = TimeZoneUtil.now();
            LocalDate syncDate = LocalDate.parse(dateStr);
            if (!syncDate.equals(today)) {
                redisTemplate.delete(postKey);
                log.info("删除Redis中{}的文章统计数据", dateStr);
            } else {
                log.info("当天数据同步完成，保留Redis中{}的文章统计数据以继续收集", dateStr);
            }

            return totalSaved;

        } catch (Exception e) {
            log.error("同步{}的内容统计数据失败", dateStr, e);
            throw new RuntimeException("同步内容统计数据失败", e);
        } finally {
            // 必须关闭游标，避免资源泄漏
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 批量保存内容统计数据到数据库
     *
     * @param postStatsMap 内容统计数据Map
     * @param year 年份
     * @param dayKey 日期键
     * @param dateStr 日期字符串
     * @return 成功保存的内容数
     */
    private int saveBatchPostStats(Map<Long, PostDayStats> postStatsMap,
                                    int year, String dayKey, String dateStr) {
        int updateCount = 0;
        for (Map.Entry<Long, PostDayStats> entry : postStatsMap.entrySet()) {
            Long postId = entry.getKey();
            PostDayStats dayStats = entry.getValue();

            try {
                // 确保post_stats年度记录存在
                ensurePostYearRecord(ContentType.post.value(), postId, year);

                // 直接设置当天的完整数据（覆盖而非增量）
                int updated = contentStatsYearlyMapper.setDayStats(ContentType.post.value(), postId, year, dayKey,
                    dayStats.views, dayStats.twice, dayStats.like, dayStats.comments);

                if (updated > 0) {
                    log.debug("覆盖文章{}在{}的统计数据: views={}, twice={}, like={}, comments={}",
                        postId, dateStr, dayStats.views, dayStats.twice, dayStats.like, dayStats.comments);

                    // 同步更新内容总计表
                    contentStatsDataService.increase(ContentType.post, postId,
                        dayStats.views, dayStats.twice, dayStats.like, dayStats.comments);

                    updateCount++;
                } else {
                    log.warn("文章{}的{}年度记录不存在，无法更新统计", postId, year);
                }

            } catch (Exception e) {
                log.error("同步文章{}在{}的统计数据失败", postId, dateStr, e);
            }
        }
        return updateCount;
    }

    /**
     * 确保post_stats的年度记录存在
     */
    private void ensurePostYearRecord(int type, Long objectId, int year) {
        ContentStatsYearlyDO existing = contentStatsYearlyMapper.getByTypeAndObjectIdAndYear(type, objectId, year);
        if (existing == null) {
            ContentStatsYearlyDO yearRecord = new ContentStatsYearlyDO();
            yearRecord.setObjectType(type);
            yearRecord.setObjectId(objectId);
            yearRecord.setStatYear(year);
            yearRecord.setStats("{}");
            contentStatsYearlyMapper.insert(yearRecord);
            log.debug("创建{}对象{}的{}年度统计记录", type, objectId, year);
        }
    }

    /**
     * 获取当前post指定日期的统计数据
     */
    public Map<String, Integer> getCurrentPostDayStats(byte type, Long objectId, int year, String dayKey) {
        try {
            String dayStatsJson = contentStatsYearlyMapper.getDayStats(type, objectId, year, dayKey);
            if (dayStatsJson != null) {
                return objectMapper.readValue(dayStatsJson, new TypeReference<Map<String, Integer>>() {});
            }
        } catch (Exception e) {
            log.debug("获取post当日统计失败，使用默认值: type={}, objectId={}, dayKey={}", type, objectId, dayKey);
        }

        Map<String, Integer> defaultStats = new HashMap<>();
        defaultStats.put("views", 0);
        defaultStats.put("twice", 0);
        defaultStats.put("like", 0);
        defaultStats.put("comments", 0);
        return defaultStats;
    }

    // ========== 辅助方法 ==========

    /**
     * 生成文章统计Redis键名
     */
    private String generatePostStatsKey(String dateStr) {
        return STATS_KEY_PREFIX + dateStr + CONTENT_STATS_SUFFIX;
    }

    /**
     * 文章单日统计数据结构
     */
    private static class PostDayStats {
        int views = 0;
        int twice = 0;
        int like = 0;
        int comments = 0;
    }
}
