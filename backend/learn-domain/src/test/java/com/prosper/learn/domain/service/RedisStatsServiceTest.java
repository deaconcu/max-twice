package com.prosper.learn.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisStatsService 单元测试
 * 
 * 测试策略:
 * 1. Mock RedisTemplate 和 HashOperations，隔离Redis依赖
 * 2. 验证Redis操作的调用次数和参数
 * 3. 测试正常场景和异常场景
 * 4. 验证日志输出和异常处理
 */
@ExtendWith(MockitoExtension.class)
class RedisStatsServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    
    @InjectMocks
    private RedisStatsService redisStatsService;
    
    private final Long TEST_ARTICLE_ID = 123L;
    private final Integer TEST_USER_ID = 456;
    private final String TEST_UPVOTE_TYPE = "twice";
    private final String TODAY = LocalDate.now().toString();
    
    @BeforeEach
    void setUp() {
        // Mock RedisTemplate返回HashOperations
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    void testRecordArticleView_WithUserId_ShouldRecordBothDimensions() {
        // Given
        String expectedPostKey = "stats:" + TODAY + ":post";
        String expectedUserKey = "stats:" + TODAY + ":user";
        String expectedPostField = TEST_ARTICLE_ID + ":view";
        String expectedUserField = TEST_USER_ID + ":view";
        
        // When
        redisStatsService.recordArticleView(TEST_ARTICLE_ID, TEST_USER_ID);
        
        // Then
        // 验证文章维度统计
        verify(hashOperations).increment(expectedPostKey, expectedPostField, 1);
        verify(redisTemplate).expire(expectedPostKey, Duration.ofDays(3));
        
        // 验证用户维度统计
        verify(hashOperations).increment(expectedUserKey, expectedUserField, 1);
        verify(redisTemplate).expire(expectedUserKey, Duration.ofDays(3));
        
        // 验证总共的调用次数
        verify(hashOperations, times(2)).increment(anyString(), anyString(), eq(1L));
        verify(redisTemplate, times(2)).expire(anyString(), eq(Duration.ofDays(3)));
    }
    
    @Test
    void testRecordArticleView_WithoutUserId_ShouldOnlyRecordPostDimension() {
        // Given
        String expectedPostKey = "stats:" + TODAY + ":post";
        String expectedPostField = TEST_ARTICLE_ID + ":view";
        
        // When
        redisStatsService.recordArticleView(TEST_ARTICLE_ID, null);
        
        // Then
        // 验证只记录文章维度统计
        verify(hashOperations, times(1)).increment(expectedPostKey, expectedPostField, 1);
        verify(redisTemplate, times(1)).expire(expectedPostKey, Duration.ofDays(3));
        
        // 验证没有用户维度统计
        verify(hashOperations, never()).increment(contains(":user"), anyString(), anyLong());
    }

    @Test
    void testRecordUpvote_ShouldRecordBothDimensions() {
        // Given
        String expectedPostKey = "stats:" + TODAY + ":post";
        String expectedUserKey = "stats:" + TODAY + ":user";
        String expectedPostField = TEST_ARTICLE_ID + ":" + TEST_UPVOTE_TYPE;
        String expectedUserField = TEST_USER_ID + ":" + TEST_UPVOTE_TYPE;
        
        // When
        redisStatsService.recordUpvote(TEST_ARTICLE_ID, TEST_USER_ID, TEST_UPVOTE_TYPE);
        
        // Then
        verify(hashOperations).increment(expectedPostKey, expectedPostField, 1);
        verify(hashOperations).increment(expectedUserKey, expectedUserField, 1);
        verify(redisTemplate, times(2)).expire(anyString(), eq(Duration.ofDays(3)));
    }

    @Test
    void testRemoveUpvote_ShouldDecrementBothDimensions() {
        // Given
        String expectedPostKey = "stats:" + TODAY + ":post";
        String expectedUserKey = "stats:" + TODAY + ":user";
        String expectedPostField = TEST_ARTICLE_ID + ":" + TEST_UPVOTE_TYPE;
        String expectedUserField = TEST_USER_ID + ":" + TEST_UPVOTE_TYPE;
        
        // When
        redisStatsService.removeUpvote(TEST_ARTICLE_ID, TEST_USER_ID, TEST_UPVOTE_TYPE);
        
        // Then
        verify(hashOperations).increment(expectedPostKey, expectedPostField, -1);
        verify(hashOperations).increment(expectedUserKey, expectedUserField, -1);
        
        // 撤销操作不应该设置过期时间
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void testRecordComment_ShouldRecordBothDimensions() {
        // Given
        String expectedPostKey = "stats:" + TODAY + ":post";
        String expectedUserKey = "stats:" + TODAY + ":user";
        String expectedPostField = TEST_ARTICLE_ID + ":comment";
        String expectedUserField = TEST_USER_ID + ":comment";
        
        // When
        redisStatsService.recordComment(TEST_ARTICLE_ID, TEST_USER_ID);
        
        // Then
        verify(hashOperations).increment(expectedPostKey, expectedPostField, 1);
        verify(hashOperations).increment(expectedUserKey, expectedUserField, 1);
        verify(redisTemplate, times(2)).expire(anyString(), eq(Duration.ofDays(3)));
    }

    @Test
    void testRemoveComment_ShouldDecrementBothDimensions() {
        // Given
        String expectedPostKey = "stats:" + TODAY + ":post";
        String expectedUserKey = "stats:" + TODAY + ":user";
        String expectedPostField = TEST_ARTICLE_ID + ":comment";
        String expectedUserField = TEST_USER_ID + ":comment";
        
        // When
        redisStatsService.removeComment(TEST_ARTICLE_ID, TEST_USER_ID);
        
        // Then
        verify(hashOperations).increment(expectedPostKey, expectedPostField, -1);
        verify(hashOperations).increment(expectedUserKey, expectedUserField, -1);
        
        // 删除操作不应该设置过期时间
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void testRecordArticleView_RedisException_ShouldNotThrowException() {
        // Given
        when(hashOperations.increment(anyString(), anyString(), anyLong()))
            .thenThrow(new RuntimeException("Redis connection failed"));
        
        // When & Then - 不应该抛出异常
        redisStatsService.recordArticleView(TEST_ARTICLE_ID, TEST_USER_ID);
        
        // 验证方法被调用了（异常被捕获了）
        verify(hashOperations).increment(anyString(), anyString(), eq(1L));
    }

    @Test
    void testRecordUpvote_RedisException_ShouldNotThrowException() {
        // Given
        when(hashOperations.increment(anyString(), anyString(), anyLong()))
            .thenThrow(new RuntimeException("Redis connection failed"));
        
        // When & Then - 不应该抛出异常
        redisStatsService.recordUpvote(TEST_ARTICLE_ID, TEST_USER_ID, TEST_UPVOTE_TYPE);
        
        // 验证方法被调用了（异常被捕获了）
        verify(hashOperations).increment(anyString(), anyString(), eq(1L));
    }

    @Test
    void testRecordComment_RedisException_ShouldNotThrowException() {
        // Given
        when(hashOperations.increment(anyString(), anyString(), anyLong()))
            .thenThrow(new RuntimeException("Redis connection failed"));
        
        // When & Then - 不应该抛出异常
        redisStatsService.recordComment(TEST_ARTICLE_ID, TEST_USER_ID);
        
        // 验证方法被调用了（异常被捕获了）
        verify(hashOperations).increment(anyString(), anyString(), eq(1L));
    }

    @Test
    void testRedisKeyFormat_ShouldFollowExpectedPattern() {
        // When
        redisStatsService.recordArticleView(TEST_ARTICLE_ID, TEST_USER_ID);
        
        // Then - 验证Key格式正确
        verify(hashOperations).increment(
            eq("stats:" + TODAY + ":post"), 
            eq(TEST_ARTICLE_ID + ":view"), 
            eq(1L)
        );
        verify(hashOperations).increment(
            eq("stats:" + TODAY + ":user"), 
            eq(TEST_USER_ID + ":view"), 
            eq(1L)
        );
    }

    @Test
    void testDifferentUpvoteTypes_ShouldHandleCorrectly() {
        // Test twice type
        redisStatsService.recordUpvote(TEST_ARTICLE_ID, TEST_USER_ID, "twice");
        verify(hashOperations).increment(anyString(), eq(TEST_ARTICLE_ID + ":twice"), eq(1L));
        
        // Test helpful type
        redisStatsService.recordUpvote(TEST_ARTICLE_ID, TEST_USER_ID, "helpful");
        verify(hashOperations).increment(anyString(), eq(TEST_ARTICLE_ID + ":helpful"), eq(1L));
    }

    @Test
    void testExpireTime_ShouldBeThreeDaysForNewRecords() {
        // When
        redisStatsService.recordArticleView(TEST_ARTICLE_ID, TEST_USER_ID);
        redisStatsService.recordUpvote(TEST_ARTICLE_ID, TEST_USER_ID, "twice");
        redisStatsService.recordComment(TEST_ARTICLE_ID, TEST_USER_ID);
        
        // Then - 验证过期时间都是3天
        verify(redisTemplate, times(6)).expire(anyString(), eq(Duration.ofDays(3)));
    }

    @Test
    void testRemoveOperations_ShouldNotSetExpireTime() {
        // When
        redisStatsService.removeUpvote(TEST_ARTICLE_ID, TEST_USER_ID, "twice");
        redisStatsService.removeComment(TEST_ARTICLE_ID, TEST_USER_ID);
        
        // Then - 验证删除操作不设置过期时间
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }
}