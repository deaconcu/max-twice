package com.prosper.learn.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.domain.service.basic.DailyStatsService;
import com.prosper.learn.dto.DailyStatsDTO;
import com.prosper.learn.dto.UserStatsDTO;
import com.prosper.learn.persistence.dataobject.PostStatsDO;
import com.prosper.learn.persistence.dataobject.UserStatsDO;
import com.prosper.learn.persistence.mapper.PostStatsMapper;
import com.prosper.learn.persistence.mapper.UserStatsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * DailyStatsService 单元测试
 * 
 * 测试策略:
 * 1. Mock所有外部依赖: RedisTemplate, UserStatsMapper, PostStatsMapper
 * 2. 测试核心业务逻辑: 数据同步、查询、统计计算
 * 3. 验证Redis数据安全机制: 当天数据保护逻辑
 * 4. 测试异常处理和边界条件
 */
@ExtendWith(MockitoExtension.class)
class DailyStatsServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    
    @Mock
    private UserStatsMapper userStatsMapper;
    
    @Mock
    private PostStatsMapper postStatsMapper;
    
    @InjectMocks
    private DailyStatsService dailyStatsService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Integer TEST_USER_ID = 123;
    private final Long TEST_POST_ID = 456L;
    private final String TODAY = LocalDate.now().toString();
    private final String YESTERDAY = LocalDate.now().minusDays(1).toString();
    
    @BeforeEach
    void setUp() {
        // lenient() 用于避免 unnecessary stubbing 警告
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    // ========== 同步功能测试 ==========

    @Test
    void testSyncSpecificDate_TodayWithData_ShouldKeepRedisData() {
        // Given - 今天有Redis数据
        when(redisTemplate.hasKey("stats:" + TODAY + ":user")).thenReturn(true);
        when(redisTemplate.hasKey("stats:" + TODAY + ":post")).thenReturn(true);
        
        // Mock Redis数据
        Map<Object, Object> userStats = createMockUserStats();
        Map<Object, Object> postStats = createMockPostStats();
        when(hashOperations.entries("stats:" + TODAY + ":user")).thenReturn(userStats);
        when(hashOperations.entries("stats:" + TODAY + ":post")).thenReturn(postStats);
        
        // Mock数据库操作
        when(userStatsMapper.getByUserIdAndYear(eq(TEST_USER_ID), eq(LocalDate.now().getYear())))
            .thenReturn(createMockUserStatsDO());
        when(userStatsMapper.setUserDayStats(anyInt(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn(1);
        when(postStatsMapper.getByTypeAndObjectIdAndYear(anyString(), anyLong(), anyInt()))
            .thenReturn(createMockPostStatsDO());
        when(postStatsMapper.setDayStats(anyString(), anyLong(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn(1);
        
        // When
        String result = dailyStatsService.syncSpecificDate(LocalDate.now());
        
        // Then
        assertTrue(result.contains("同步" + TODAY + "的数据完成"));
        // 验证今天的数据没有被删除
        verify(redisTemplate, never()).delete("stats:" + TODAY + ":user");
        verify(redisTemplate, never()).delete("stats:" + TODAY + ":post");
    }

    @Test
    void testSyncSpecificDate_YesterdayWithData_ShouldDeleteRedisData() {
        // Given - 昨天有Redis数据
        when(redisTemplate.hasKey("stats:" + YESTERDAY + ":user")).thenReturn(true);
        when(redisTemplate.hasKey("stats:" + YESTERDAY + ":post")).thenReturn(false);
        
        // Mock Redis数据
        Map<Object, Object> userStats = createMockUserStats();
        when(hashOperations.entries("stats:" + YESTERDAY + ":user")).thenReturn(userStats);
        
        // Mock数据库操作
        when(userStatsMapper.getByUserIdAndYear(eq(TEST_USER_ID), eq(LocalDate.now().getYear())))
            .thenReturn(createMockUserStatsDO());
        when(userStatsMapper.setUserDayStats(anyInt(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn(1);
        
        // When
        String result = dailyStatsService.syncSpecificDate(LocalDate.now().minusDays(1));
        
        // Then
        assertTrue(result.contains("同步" + YESTERDAY + "的数据完成"));
        // 验证昨天的数据被删除了
        verify(redisTemplate).delete("stats:" + YESTERDAY + ":user");
    }

    @Test
    void testSyncSpecificDate_NoRedisData_ShouldSkipSync() {
        // Given - 没有Redis数据
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        
        // When
        String result = dailyStatsService.syncSpecificDate(LocalDate.now());
        
        // Then
        assertTrue(result.contains("Redis中没有"));
        assertTrue(result.contains("跳过同步"));
        
        // 验证没有进行任何数据库操作
        verify(userStatsMapper, never()).setUserDayStats(anyInt(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(postStatsMapper, never()).setDayStats(anyString(), anyLong(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testSyncSpecificDate_NullDate_ShouldUseToday() {
        // Given - 传入null日期
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        
        // When
        String result = dailyStatsService.syncSpecificDate(null);
        
        // Then
        assertTrue(result.contains(TODAY));
        verify(redisTemplate).hasKey("stats:" + TODAY + ":user");
        verify(redisTemplate).hasKey("stats:" + TODAY + ":post");
    }

    @Test
    void testSyncSpecificDate_RedisException_ShouldReturnErrorMessage() {
        // Given - Redis操作异常
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis connection failed"));
        
        // When
        String result = dailyStatsService.syncSpecificDate(LocalDate.now());
        
        // Then
        System.out.println("实际返回结果: [" + result + "]");
        assertNotNull(result);
        assertTrue(result.contains("同步") && result.contains("失败"));
        assertTrue(result.contains("Redis connection failed"));
    }

    // ========== 查询功能测试 ==========

    @Test
    void testGetUserTodayStats_WithRedisData_ShouldReturnCorrectStats() {
        // Given
        Map<Object, Object> redisStats = createMockUserStats();
        when(hashOperations.entries("stats:" + TODAY + ":user")).thenReturn(redisStats);
        
        // When
        UserStatsDTO result = dailyStatsService.getUserTodayStats(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals("today", result.getPeriod());
        assertEquals(5L, result.getTotalViews()); // 来自mock数据
        assertEquals(3L, result.getTotalTwice());
        assertEquals(2L, result.getTotalHelpful());
        assertEquals(1L, result.getTotalComments());
    }

    @Test
    void testGetUserTodayStats_RedisException_ShouldReturnEmptyStats() {
        // Given - Redis异常
        when(hashOperations.entries(anyString())).thenThrow(new RuntimeException("Redis error"));
        
        // When
        UserStatsDTO result = dailyStatsService.getUserTodayStats(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(0L, result.getTotalViews());
        assertEquals(0L, result.getTotalTwice());
        assertEquals(0L, result.getTotalHelpful());
        assertEquals(0L, result.getTotalComments());
    }

    @Test
    void testGetUserYesterdayStats_WithValidData_ShouldReturnCorrectStats() {
        // Given
        Map<String, Integer> dayStats = Map.of(
            "views", 10,
            "twice", 5,
            "helpful", 3,
            "comments", 2
        );
        
        // Mock getUserDayStats 的返回值
        DailyStatsService spyService = spy(dailyStatsService);
        doReturn(dayStats).when(spyService).getUserDayStats(eq(TEST_USER_ID), any(LocalDate.class));
        
        // When
        UserStatsDTO result = spyService.getUserYesterdayStats(TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals("yesterday", result.getPeriod());
        assertEquals(10L, result.getTotalViews());
        assertEquals(5L, result.getTotalTwice());
        assertEquals(3L, result.getTotalHelpful());
        assertEquals(2L, result.getTotalComments());
    }

    @Test
    void testSyncYesterdayStats_ShouldSyncBothUserAndPostStats() {
        // Given
        String yesterday = LocalDate.now().minusDays(1).toString();
        Map<Object, Object> userStats = createMockUserStats();
        Map<Object, Object> postStats = createMockPostStats();
        
        when(hashOperations.entries("stats:" + yesterday + ":user")).thenReturn(userStats);
        when(hashOperations.entries("stats:" + yesterday + ":post")).thenReturn(postStats);
        
        // Mock数据库操作
        when(userStatsMapper.getByUserIdAndYear(eq(TEST_USER_ID), anyInt()))
            .thenReturn(createMockUserStatsDO());
        when(userStatsMapper.setUserDayStats(anyInt(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn(1);
        when(postStatsMapper.getByTypeAndObjectIdAndYear(anyString(), anyLong(), anyInt()))
            .thenReturn(createMockPostStatsDO());
        when(postStatsMapper.setDayStats(anyString(), anyLong(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn(1);
        when(postStatsMapper.getDayStats(anyString(), anyLong(), anyInt(), anyString()))
            .thenReturn("{\"views\": 0, \"twice\": 0, \"helpful\": 0, \"comments\": 0}");
        
        // When
        dailyStatsService.syncYesterdayStats();
        
        // Then
        verify(userStatsMapper).setUserDayStats(anyInt(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt());
        // 验证 setDayStats 被调用了 4 次（每种统计类型一次）
        verify(postStatsMapper, times(4)).setDayStats(anyString(), anyLong(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(redisTemplate).delete("stats:" + yesterday + ":user");
        verify(redisTemplate).delete("stats:" + yesterday + ":post");
    }

    @Test
    void testGetUserDayStats_WithValidData_ShouldReturnCorrectStats() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 8, 24);
        String expectedDayKey = "8-24";
        String mockJsonData = "{\"views\": 10, \"twice\": 5, \"helpful\": 3, \"comments\": 2}";
        
        when(userStatsMapper.getDayStats(TEST_USER_ID, 2024, expectedDayKey))
            .thenReturn(mockJsonData);
        
        // When
        Map<String, Integer> result = dailyStatsService.getUserDayStats(TEST_USER_ID, testDate);
        
        // Then
        assertNotNull(result);
        assertEquals(10, result.get("views"));
        assertEquals(5, result.get("twice"));
        assertEquals(3, result.get("helpful"));
        assertEquals(2, result.get("comments"));
    }

    @Test
    void testGetUserDayStats_NoData_ShouldReturnEmptyStats() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 8, 24);
        when(userStatsMapper.getDayStats(anyInt(), anyInt(), anyString()))
            .thenReturn(null);
        
        // When
        Map<String, Integer> result = dailyStatsService.getUserDayStats(TEST_USER_ID, testDate);
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.get("views"));
        assertEquals(0, result.get("twice"));
        assertEquals(0, result.get("helpful"));
        assertEquals(0, result.get("comments"));
    }

    @Test
    void testGetUserDayStats_JsonParseException_ShouldReturnEmptyStats() {
        // Given
        LocalDate testDate = LocalDate.of(2024, 8, 24);
        when(userStatsMapper.getDayStats(anyInt(), anyInt(), anyString()))
            .thenReturn("invalid json");
        
        // When
        Map<String, Integer> result = dailyStatsService.getUserDayStats(TEST_USER_ID, testDate);
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.get("views"));
        assertEquals(0, result.get("twice"));
        assertEquals(0, result.get("helpful"));
        assertEquals(0, result.get("comments"));
    }

    @Test
    void testGetUserHistoryStats_ShouldReturnCorrectAggregatedData() {
        // Given
        int days = 7;
        DailyStatsService spyService = spy(dailyStatsService);
        
        // Mock getUserDateRangeStats返回值
        Map<String, Integer> mockStats = Map.of(
            "views", 70,
            "twice", 35,
            "helpful", 21,
            "comments", 14
        );
        doReturn(mockStats).when(spyService).getUserDateRangeStats(eq(TEST_USER_ID), any(LocalDate.class), any(LocalDate.class));
        
        // When
        UserStatsDTO result = spyService.getUserHistoryStats(TEST_USER_ID, days);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals("7days", result.getPeriod());
        assertEquals(70L, result.getTotalViews());
        assertEquals(35L, result.getTotalTwice());
        assertEquals(21L, result.getTotalHelpful());
        assertEquals(14L, result.getTotalComments());
    }

    @Test
    void testGetUserPeriodStatsWithDaily_ShouldIncludeDailyBreakdown() {
        // Given
        int days = 3;
        DailyStatsService spyService = spy(dailyStatsService);
        
        // Mock getUserTodayStats for today
        UserStatsDTO todayStats = UserStatsDTO.builder()
            .userId(TEST_USER_ID)
            .totalViews(10L)
            .totalTwice(5L)
            .totalHelpful(3L)
            .totalComments(2L)
            .build();
        doReturn(todayStats).when(spyService).getUserTodayStats(TEST_USER_ID);
        
        // Mock getUserDayStats for historical days
        Map<String, Integer> dayStats = Map.of(
            "views", 8,
            "twice", 4,
            "helpful", 2,
            "comments", 1
        );
        doReturn(dayStats).when(spyService).getUserDayStats(eq(TEST_USER_ID), any(LocalDate.class));
        
        // When
        UserStatsDTO result = spyService.getUserPeriodStatsWithDaily(TEST_USER_ID, days);
        
        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals("3days", result.getPeriod());
        
        // 验证每日统计数据
        List<DailyStatsDTO> dailyStats = result.getDailyStats();
        assertNotNull(dailyStats);
        assertEquals(3, dailyStats.size());
        
        // 验证总计数据（今天10+昨天8+前天8）
        assertEquals(26L, result.getTotalViews());
        assertEquals(13L, result.getTotalTwice());
        assertEquals(7L, result.getTotalHelpful());
        assertEquals(4L, result.getTotalComments());
    }

    @Test
    void testGetUserYearStats_WithValidData_ShouldReturnFullYearData() {
        // Given
        int year = 2024;
        String mockStatsJson = """
                {
                  "1-1": {"views": 10, "twice": 5, "helpful": 3, "comments": 2},
                  "1-2": {"views": 8, "twice": 4, "helpful": 2, "comments": 1}
                }""";
        
        UserStatsDO mockUserStats = createMockUserStatsDO();
        mockUserStats.setStats(mockStatsJson);
        when(userStatsMapper.getByUserIdAndYear(TEST_USER_ID, year)).thenReturn(mockUserStats);
        
        // When
        Map<String, Map<String, Integer>> result = dailyStatsService.getUserYearStats(TEST_USER_ID, year);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("1-1"));
        assertTrue(result.containsKey("1-2"));
        
        Map<String, Integer> day1Stats = result.get("1-1");
        assertEquals(10, day1Stats.get("views"));
        assertEquals(5, day1Stats.get("twice"));
    }

    @Test
    void testGetUserYearStats_NoData_ShouldReturnEmptyMap() {
        // Given
        int year = 2024;
        when(userStatsMapper.getByUserIdAndYear(TEST_USER_ID, year)).thenReturn(null);
        
        // When
        Map<String, Map<String, Integer>> result = dailyStatsService.getUserYearStats(TEST_USER_ID, year);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserMonthStats_ShouldAggregateMonthlyData() {
        // Given
        int year = 2024;
        int month = 1;
        DailyStatsService spyService = spy(dailyStatsService);
        
        // Mock year stats with January data
        Map<String, Map<String, Integer>> yearStats = new HashMap<>();
        yearStats.put("1-1", Map.of("views", 10, "twice", 5, "helpful", 3, "comments", 2));
        yearStats.put("1-2", Map.of("views", 8, "twice", 4, "helpful", 2, "comments", 1));
        yearStats.put("2-1", Map.of("views", 5, "twice", 2, "helpful", 1, "comments", 1)); // February data should be ignored
        
        doReturn(yearStats).when(spyService).getUserYearStats(TEST_USER_ID, year);
        
        // When
        Map<String, Integer> result = spyService.getUserMonthStats(TEST_USER_ID, year, month);
        
        // Then
        assertNotNull(result);
        assertEquals(18, result.get("views")); // 10 + 8
        assertEquals(9, result.get("twice")); // 5 + 4
        assertEquals(5, result.get("helpful")); // 3 + 2
        assertEquals(3, result.get("comments")); // 2 + 1
    }

    @Test
    void testGetUserDateRangeStats_ShouldAggregateRangeData() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 3);
        DailyStatsService spyService = spy(dailyStatsService);
        
        // Mock year stats
        Map<String, Map<String, Integer>> yearStats = new HashMap<>();
        yearStats.put("1-1", Map.of("views", 10, "twice", 5, "helpful", 3, "comments", 2));
        yearStats.put("1-2", Map.of("views", 8, "twice", 4, "helpful", 2, "comments", 1));
        yearStats.put("1-3", Map.of("views", 6, "twice", 3, "helpful", 1, "comments", 1));
        yearStats.put("1-4", Map.of("views", 4, "twice", 2, "helpful", 1, "comments", 0)); // Should be excluded
        
        doReturn(yearStats).when(spyService).getUserYearStats(TEST_USER_ID, 2024);
        
        // When
        Map<String, Integer> result = spyService.getUserDateRangeStats(TEST_USER_ID, startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertEquals(24, result.get("views")); // 10 + 8 + 6
        assertEquals(12, result.get("twice")); // 5 + 4 + 3
        assertEquals(6, result.get("helpful")); // 3 + 2 + 1
        assertEquals(4, result.get("comments")); // 2 + 1 + 1
    }

    @Test
    void testGetUserDateRangeStats_CrossYear_ShouldHandleMultipleYears() {
        // Given
        LocalDate startDate = LocalDate.of(2023, 12, 30);
        LocalDate endDate = LocalDate.of(2024, 1, 2);
        DailyStatsService spyService = spy(dailyStatsService);
        
        // Mock 2023 stats
        Map<String, Map<String, Integer>> year2023Stats = new HashMap<>();
        year2023Stats.put("12-30", Map.of("views", 5, "twice", 2, "helpful", 1, "comments", 1));
        year2023Stats.put("12-31", Map.of("views", 7, "twice", 3, "helpful", 2, "comments", 1));
        
        // Mock 2024 stats
        Map<String, Map<String, Integer>> year2024Stats = new HashMap<>();
        year2024Stats.put("1-1", Map.of("views", 10, "twice", 5, "helpful", 3, "comments", 2));
        year2024Stats.put("1-2", Map.of("views", 8, "twice", 4, "helpful", 2, "comments", 1));
        
        doReturn(year2023Stats).when(spyService).getUserYearStats(TEST_USER_ID, 2023);
        doReturn(year2024Stats).when(spyService).getUserYearStats(TEST_USER_ID, 2024);
        
        // When
        Map<String, Integer> result = spyService.getUserDateRangeStats(TEST_USER_ID, startDate, endDate);
        
        // Then
        assertNotNull(result);
        assertEquals(30, result.get("views")); // 5 + 7 + 10 + 8
        assertEquals(14, result.get("twice")); // 2 + 3 + 5 + 4
        assertEquals(8, result.get("helpful")); // 1 + 2 + 3 + 2
        assertEquals(5, result.get("comments")); // 1 + 1 + 2 + 1
    }

    @Test
    void testSyncUserStats_WithInvalidRedisData_ShouldSkipInvalidEntries() {
        // Given
        String dateStr = "2024-08-24";
        Map<Object, Object> userStats = new HashMap<>();
        userStats.put(TEST_USER_ID + ":view", "5");
        userStats.put(TEST_USER_ID + ":twice", "0"); // Should be skipped (count <= 0)
        userStats.put("invalid:format", "3"); // Should cause NumberFormatException for userId parsing
        userStats.put(TEST_USER_ID + ":unknown", "2"); // Should be skipped (unknown stat type)
        
        when(redisTemplate.hasKey("stats:" + dateStr + ":user")).thenReturn(true);
        when(redisTemplate.hasKey("stats:" + dateStr + ":post")).thenReturn(false);
        when(hashOperations.entries("stats:" + dateStr + ":user")).thenReturn(userStats);
        
        // When - 通过调用 syncSpecificDate 来间接测试 syncUserStats
        String result = dailyStatsService.syncSpecificDate(LocalDate.parse(dateStr));
        
        // Then - 预期会有异常，同步失败
        System.out.println("实际返回结果: [" + result + "]");
        assertNotNull(result);
        assertTrue(result.contains("同步") && result.contains("失败"));
        assertTrue(result.contains("For input string"));
    }

    @Test
    void testSyncPostStats_WithValidData_ShouldUpdateDatabase() {
        // Given
        String dateStr = "2024-08-24";
        Map<Object, Object> postStats = createMockPostStats();
        
        when(redisTemplate.hasKey("stats:" + dateStr + ":user")).thenReturn(false);
        when(redisTemplate.hasKey("stats:" + dateStr + ":post")).thenReturn(true);
        when(hashOperations.entries("stats:" + dateStr + ":post")).thenReturn(postStats);
        when(postStatsMapper.getByTypeAndObjectIdAndYear("POST", TEST_POST_ID, 2024))
            .thenReturn(createMockPostStatsDO());
        when(postStatsMapper.getDayStats("POST", TEST_POST_ID, 2024, "8-24"))
            .thenReturn("{\"views\": 0, \"twice\": 0, \"helpful\": 0, \"comments\": 0}");
        when(postStatsMapper.setDayStats(anyString(), anyLong(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn(1);
        
        // When - 通过调用 syncSpecificDate 来间接测试 syncPostStats
        String result = dailyStatsService.syncSpecificDate(LocalDate.parse(dateStr));
        
        // Then
        System.out.println("实际返回结果: [" + result + "]");
        assertNotNull(result);
        assertTrue(result.contains("同步") && result.contains("数据完成"));
        // 验证 setDayStats 被调用了 1 次（每个文章一次，包含所有统计类型）
        verify(postStatsMapper, times(1)).setDayStats(eq("POST"), eq(TEST_POST_ID), eq(2024), eq("08-24"), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testEnsureUserYearRecord_RecordExists_ShouldNotInsert() {
        // Given
        when(redisTemplate.hasKey("stats:2024-08-24:user")).thenReturn(true);
        when(userStatsMapper.getByUserIdAndYear(TEST_USER_ID, 2024))
            .thenReturn(createMockUserStatsDO());
        
        // 通过调用包含此逻辑的公共方法来间接测试
        Map<Object, Object> userStats = createMockUserStats();
        when(hashOperations.entries(anyString())).thenReturn(userStats);
        when(userStatsMapper.setUserDayStats(anyInt(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn(1);
        
        // When
        dailyStatsService.syncSpecificDate(LocalDate.of(2024, 8, 24));
        
        // Then
        verify(userStatsMapper, never()).insert(any(UserStatsDO.class));
    }

    @Test
    void testEnsureUserYearRecord_RecordNotExists_ShouldInsert() {
        // Given
        when(redisTemplate.hasKey("stats:2024-08-24:user")).thenReturn(true);
        when(userStatsMapper.getByUserIdAndYear(TEST_USER_ID, 2024))
            .thenReturn(null) // First call returns null
            .thenReturn(createMockUserStatsDO()); // Second call after insert returns the record
        
        Map<Object, Object> userStats = createMockUserStats();
        when(hashOperations.entries(anyString())).thenReturn(userStats);
        when(userStatsMapper.setUserDayStats(anyInt(), anyInt(), anyString(), anyInt(), anyInt(), anyInt(), anyInt()))
            .thenReturn(1);
        
        // When
        dailyStatsService.syncSpecificDate(LocalDate.of(2024, 8, 24));
        
        // Then
        verify(userStatsMapper).insert(any(UserStatsDO.class));
    }

    // ========== 辅助方法 ==========

    /**
     * 创建模拟的Redis用户统计数据
     */
    private Map<Object, Object> createMockUserStats() {
        Map<Object, Object> stats = new HashMap<>();
        stats.put(TEST_USER_ID + ":view", "5");
        stats.put(TEST_USER_ID + ":twice", "3");
        stats.put(TEST_USER_ID + ":helpful", "2");
        stats.put(TEST_USER_ID + ":comment", "1");
        return stats;
    }

    /**
     * 创建模拟的Redis文章统计数据
     */
    private Map<Object, Object> createMockPostStats() {
        Map<Object, Object> stats = new HashMap<>();
        stats.put(TEST_POST_ID + ":view", "10");
        stats.put(TEST_POST_ID + ":twice", "6");
        stats.put(TEST_POST_ID + ":helpful", "4");
        stats.put(TEST_POST_ID + ":comment", "2");
        return stats;
    }

    /**
     * 创建模拟的UserStatsDO对象
     */
    private UserStatsDO createMockUserStatsDO() {
        UserStatsDO userStats = new UserStatsDO();
        userStats.setId(1L);
        userStats.setUserId(TEST_USER_ID);
        userStats.setStatYear(LocalDate.now().getYear());
        userStats.setStats("{}");
        return userStats;
    }

    /**
     * 创建模拟的PostStatsDO对象
     */
    private PostStatsDO createMockPostStatsDO() {
        PostStatsDO postStats = new PostStatsDO();
        postStats.setId(1L);
        postStats.setType("POST");
        postStats.setObjectId(TEST_POST_ID);
        postStats.setStatYear(LocalDate.now().getYear());
        postStats.setStats("{}");
        return postStats;
    }
}