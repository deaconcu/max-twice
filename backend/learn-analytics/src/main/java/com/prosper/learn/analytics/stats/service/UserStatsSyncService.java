package com.prosper.learn.analytics.stats.service;

import com.prosper.learn.analytics.stats.dataservice.UserStatsDataService;
import com.prosper.learn.analytics.stats.mapper.UserStatsDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyMapper;
import com.prosper.learn.infrastructure.redis.RedisKeyPrefix;
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

/**
 * 用户统计同步服务
 *
 * 负责将Redis中的用户统计数据同步到数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatsSyncService {

    private final RedisTemplate<String, String> redisTemplate;
    private final UserStatsYearlyMapper userStatsYearlyMapper;
    private final UserStatsDataService userStatsDataService;

    /** 每批处理的Redis记录数 */
    private static final int SCAN_BATCH_SIZE = 1000;
    /** 每批保存到数据库的用户数（避免内存占用过大） */
    private static final int DB_SAVE_BATCH_SIZE = 5000;

    /**
     * 从Redis同步用户每日统计数据到数据库
     *
     * 同步逻辑，直接覆盖当天的完整统计数据:
     * 1. 使用HSCAN从Redis分批获取指定日期的用户统计数据
     * 2. 解析并聚合每个用户当天的完整数据
     * 3. 分批写入数据库，避免内存溢出
     * 4. 删除Redis中的原始数据
     *
     * @param dateStr 日期字符串，格式: YYYY-MM-DD
     * @return 成功同步的记录数
     */
    public int syncUserStats(String dateStr) {
        String userKey = generateUserStatsKey(dateStr);

        // 使用 HSCAN 游标分批获取数据，避免一次性加载所有数据导致内存溢出
        ScanOptions scanOptions = ScanOptions.scanOptions()
            .count(SCAN_BATCH_SIZE)  // 每批获取1000条
            .build();

        Cursor<Map.Entry<Object, Object>> cursor = null;
        try {
            cursor = redisTemplate.opsForHash().scan(userKey, scanOptions);

            if (!cursor.hasNext()) {
                log.info("{}没有用户统计数据", dateStr);
                return 0;
            }

            // 解析日期信息
            LocalDate date = LocalDate.parse(dateStr);
            int year = date.getYear();
            String dayKey = generateDayKey(date);

            // 按用户ID分组并聚合当天的完整统计数据
            Map<Long, UserDayStats> userDayStatsMap = new HashMap<>();
            int totalEntries = 0;
            int totalSaved = 0;

            // 分批处理 Redis 数据
            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();
                String field = (String) entry.getKey();
                Integer count = Integer.parseInt((String) entry.getValue());

                if (count <= 0) continue; // 跳过无效数据

                String[] parts = field.split(":");
                if (parts.length != 2) continue;

                Long userId = Long.parseLong(parts[0]);
                String statType = parts[1];

                UserDayStats dayStats = userDayStatsMap.computeIfAbsent(userId, k -> new UserDayStats());

                // 聚合当天的完整数据
                switch (statType) {
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
                        log.debug("忽略未知的统计类型: {}", statType);
                }

                totalEntries++;

                // 每处理 DB_SAVE_BATCH_SIZE 个用户，批量写入数据库一次（避免内存占用过大）
                if (userDayStatsMap.size() >= DB_SAVE_BATCH_SIZE) {
                    int batchSaved = saveBatchUserStats(userDayStatsMap, year, dayKey, dateStr);
                    totalSaved += batchSaved;
                    log.info("已处理 {} 条Redis记录，批量保存 {} 个用户的统计", totalEntries, batchSaved);
                    userDayStatsMap.clear(); // 清空已处理的数据，释放内存
                }
            }

            // 处理剩余的数据
            if (!userDayStatsMap.isEmpty()) {
                int finalSaved = saveBatchUserStats(userDayStatsMap, year, dayKey, dateStr);
                totalSaved += finalSaved;
                log.info("最后批次保存 {} 个用户的统计", finalSaved);
            }

            log.info("同步{}的用户数据完成: 总计处理 {} 条Redis记录，保存 {} 个用户",
                dateStr, totalEntries, totalSaved);

            // 删除Redis数据，释放内存空间（只有非当天的数据才删除）
            LocalDate today = TimeZoneUtil.now();
            LocalDate syncDate = LocalDate.parse(dateStr);
            if (!syncDate.equals(today)) {
                redisTemplate.delete(userKey);
                log.info("删除Redis中{}的用户统计数据", dateStr);
            } else {
                log.info("当天数据同步完成，保留Redis中{}的用户统计数据以继续收集", dateStr);
            }

            return totalSaved;

        } catch (Exception e) {
            log.error("同步{}的用户统计数据失败", dateStr, e);
            throw new RuntimeException("同步用户统计数据失败", e);
        } finally {
            // 必须关闭游标，避免资源泄漏
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 批量保存用户统计数据到数据库
     *
     * @param userDayStatsMap 用户统计数据Map
     * @param year 年份
     * @param dayKey 日期键
     * @param dateStr 日期字符串
     * @return 成功保存的用户数
     */
    private int saveBatchUserStats(Map<Long, UserDayStats> userDayStatsMap,
                                    int year, String dayKey, String dateStr) {
        int updateCount = 0;
        for (Map.Entry<Long, UserDayStats> entry : userDayStatsMap.entrySet()) {
            Long userId = entry.getKey();
            UserDayStats dayStats = entry.getValue();

            try {
                // 检查是否已经同步过该日期，防止重复累加
                UserStatsDO existingStats = userStatsDataService.getByUserId(userId);
                if (existingStats != null && dateStr.equals(existingStats.getLastSyncDate())) {
                    log.debug("用户{}在{}的统计数据已同步过，跳过", userId, dateStr);
                    continue;
                }

                // 确保用户的年度记录存在
                ensureUserYearRecord(userId, year);

                // 直接设置当天的完整数据（覆盖而非增量）
                int updated = userStatsYearlyMapper.updateYearlyStatsArray(
                    userId, year, dayKey,
                    dayStats.views, dayStats.twice, dayStats.like,
                    dayStats.comments);

                if (updated > 0) {
                    log.debug("覆盖用户{}在{}的统计数据: views={}, twice={}, like={}, comments={}",
                        userId, dateStr, dayStats.views, dayStats.twice, dayStats.like, dayStats.comments);

                    // 同步更新用户总计表，并更新 lastSyncDate
                    userStatsDataService.increase(userId, dayStats.views, dayStats.twice, dayStats.like, dayStats.comments, dateStr);

                    updateCount++;
                } else {
                    log.warn("用户{}的{}年度记录不存在，无法更新统计", userId, year);
                }

            } catch (Exception e) {
                log.error("同步用户{}在{}的统计数据失败", userId, dateStr, e);
            }
        }
        return updateCount;
    }

    /**
     * 确保用户的年度统计记录存在
     */
    private void ensureUserYearRecord(long userId, int year) {
        UserStatsYearlyDO existing = userStatsYearlyMapper.getByUserIdAndYear(userId, year);
        if (existing == null) {
            UserStatsYearlyDO yearRecord = new UserStatsYearlyDO();
            yearRecord.setUserId(userId);
            yearRecord.setStatYear(year);
            yearRecord.setStats("{}"); // 初始化为空JSON对象
            userStatsYearlyMapper.insert(yearRecord);
            log.debug("创建用户{}的{}年度统计记录", userId, year);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 生成用户统计Redis键名（带语言前缀）
     */
    private String generateUserStatsKey(String dateStr) {
        return RedisKeyPrefix.prefix(STATS_KEY_PREFIX + dateStr + USER_STATS_SUFFIX);
    }

    /**
     * 生成日期键（月-日格式）
     */
    private String generateDayKey(LocalDate date) {
        return date.getMonthValue() + "-" + date.getDayOfMonth();
    }

    /**
     * 用户单日统计数据结构
     */
    private static class UserDayStats {
        int views = 0;
        int twice = 0;
        int like = 0;
        int comments = 0;
    }
}
