package com.prosper.learn.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Slf4j
@Service
public class RedisStatsService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 记录文章访问
     */
    public void recordArticleView(Long articleId, Integer userId) {
        String today = LocalDate.now().toString();
        
        try {
            // 文章维度统计
            String postKey = "stats:" + today + ":post";
            String postField = articleId + ":view";
            redisTemplate.opsForHash().increment(postKey, postField, 1);
            redisTemplate.expire(postKey, Duration.ofDays(3));
            
            // 用户维度统计
            if (userId != null) {
                String userKey = "stats:" + today + ":user";
                String userField = userId + ":view";
                redisTemplate.opsForHash().increment(userKey, userField, 1);
                redisTemplate.expire(userKey, Duration.ofDays(3));
            }
            
            log.debug("记录文章访问: articleId={}, userId={}", articleId, userId);
        } catch (Exception e) {
            log.error("记录文章访问失败: articleId={}, userId={}", articleId, userId, e);
        }
    }

    /**
     * 记录点赞
     */
    public void recordUpvote(Long articleId, Integer userId, String upvoteType) {
        String today = LocalDate.now().toString();
        
        try {
            // 文章维度统计
            String postKey = "stats:" + today + ":post";
            String postField = articleId + ":" + upvoteType;
            redisTemplate.opsForHash().increment(postKey, postField, 1);
            redisTemplate.expire(postKey, Duration.ofDays(3));
            
            // 用户维度统计
            String userKey = "stats:" + today + ":user";
            String userField = userId + ":" + upvoteType;
            redisTemplate.opsForHash().increment(userKey, userField, 1);
            redisTemplate.expire(userKey, Duration.ofDays(3));
            
            log.debug("记录点赞: articleId={}, userId={}, type={}", articleId, userId, upvoteType);
        } catch (Exception e) {
            log.error("记录点赞失败: articleId={}, userId={}, type={}", articleId, userId, upvoteType, e);
        }
    }

    /**
     * 撤销点赞
     */
    public void removeUpvote(Long articleId, Integer userId, String upvoteType) {
        String today = LocalDate.now().toString();
        
        try {
            // 文章维度统计
            String postKey = "stats:" + today + ":post";
            String postField = articleId + ":" + upvoteType;
            redisTemplate.opsForHash().increment(postKey, postField, -1);
            
            // 用户维度统计
            String userKey = "stats:" + today + ":user";
            String userField = userId + ":" + upvoteType;
            redisTemplate.opsForHash().increment(userKey, userField, -1);
            
            log.debug("撤销点赞: articleId={}, userId={}, type={}", articleId, userId, upvoteType);
        } catch (Exception e) {
            log.error("撤销点赞失败: articleId={}, userId={}, type={}", articleId, userId, upvoteType, e);
        }
    }

    /**
     * 记录评论
     */
    public void recordComment(Long articleId, Integer userId) {
        String today = LocalDate.now().toString();
        
        try {
            // 文章维度统计
            String postKey = "stats:" + today + ":post";
            String postField = articleId + ":comment";
            redisTemplate.opsForHash().increment(postKey, postField, 1);
            redisTemplate.expire(postKey, Duration.ofDays(3));
            
            // 用户维度统计
            String userKey = "stats:" + today + ":user";
            String userField = userId + ":comment";
            redisTemplate.opsForHash().increment(userKey, userField, 1);
            redisTemplate.expire(userKey, Duration.ofDays(3));
            
            log.debug("记录评论: articleId={}, userId={}", articleId, userId);
        } catch (Exception e) {
            log.error("记录评论失败: articleId={}, userId={}", articleId, userId, e);
        }
    }

    /**
     * 删除评论
     */
    public void removeComment(Long articleId, Integer userId) {
        String today = LocalDate.now().toString();
        
        try {
            // 文章维度统计
            String postKey = "stats:" + today + ":post";
            String postField = articleId + ":comment";
            redisTemplate.opsForHash().increment(postKey, postField, -1);
            
            // 用户维度统计
            String userKey = "stats:" + today + ":user";
            String userField = userId + ":comment";
            redisTemplate.opsForHash().increment(userKey, userField, -1);
            
            log.debug("删除评论: articleId={}, userId={}", articleId, userId);
        } catch (Exception e) {
            log.error("删除评论失败: articleId={}, userId={}", articleId, userId, e);
        }
    }
}