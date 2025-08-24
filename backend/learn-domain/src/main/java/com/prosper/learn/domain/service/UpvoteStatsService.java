package com.prosper.learn.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.persistence.dataobject.PostStatsDO;
import com.prosper.learn.persistence.mapper.PostStatsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UpvoteStatsService {

    @Autowired
    private PostStatsMapper postStatsMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 记录点赞事件
     * @param type 对象类型 (POST, COMMENT等)
     * @param objectId 对象ID
     * @param upvoteType 点赞类型 (once, twice, helpful)
     */
    public void recordUpvote(String type, Long objectId, String upvoteType) {
        try {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            String dayKey = today.getMonthValue() + "-" + today.getDayOfMonth();

            // 使用MySQL JSON操作直接更新
            int updated = postStatsMapper.incrementUpvoteCount(type, objectId, currentYear, dayKey, upvoteType);

            if (updated == 0) {
                // 记录不存在，创建新记录
                Map<String, Map<String, Integer>> yearlyData = new HashMap<>();
                Map<String, Integer> dayData = new HashMap<>();
                dayData.put("once", 0);
                dayData.put("twice", 0);
                dayData.put("helpful", 0);
                dayData.put(upvoteType, 1);
                yearlyData.put(dayKey, dayData);

                PostStatsDO yearStats = new PostStatsDO();
                yearStats.setType(type);
                yearStats.setObjectId(objectId);
                yearStats.setStatYear(currentYear);
                yearStats.setStats(objectMapper.writeValueAsString(yearlyData));

                postStatsMapper.insert(yearStats);
                log.debug("创建新的年度统计记录: type={}, objectId={}, year={}", type, objectId, currentYear);
            } else {
                log.debug("更新年度统计记录: type={}, objectId={}, year={}", type, objectId, currentYear);
            }

        } catch (Exception e) {
            log.error("记录点赞统计失败: type={}, objectId={}, upvoteType={}", type, objectId, upvoteType, e);
        }
    }

    /**
     * 撤销点赞事件
     */
    public void removeUpvote(String type, Long objectId, String upvoteType) {
        try {
            LocalDate today = LocalDate.now();
            int currentYear = today.getYear();
            String dayKey = today.getMonthValue() + "-" + today.getDayOfMonth();

            // 使用MySQL JSON操作直接减少计数
            postStatsMapper.decrementUpvoteCount(type, objectId, currentYear, dayKey, upvoteType);
            log.debug("撤销点赞统计: type={}, objectId={}, upvoteType={}", type, objectId, upvoteType);

        } catch (Exception e) {
            log.error("撤销点赞统计失败: type={}, objectId={}, upvoteType={}", type, objectId, upvoteType, e);
        }
    }

    /**
     * 获取指定日期的点赞统计
     */
    public Map<String, Integer> getDayUpvoteStats(String type, Long objectId, LocalDate date) {
        try {
            int year = date.getYear();
            String dayKey = date.getMonthValue() + "-" + date.getDayOfMonth();

            String dayStatsJson = postStatsMapper.getDayStats(type, objectId, year, dayKey);

            if (dayStatsJson != null) {
                return objectMapper.readValue(dayStatsJson, new TypeReference<Map<String, Integer>>() {});
            }

            return new HashMap<>();
        } catch (Exception e) {
            log.error("获取日统计失败: type={}, objectId={}, date={}", type, objectId, date, e);
            return new HashMap<>();
        }
    }
}
