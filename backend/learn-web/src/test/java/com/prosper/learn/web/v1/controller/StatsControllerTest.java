package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyDO;
import com.prosper.learn.analytics.stats.mapper.UserStatsYearlyMapper;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.profession.ProfessionDataService;
import com.prosper.learn.content.roadmap.RoadmapDataService;
import com.prosper.learn.shared.domain.Enums.UserRole;
import com.prosper.learn.shared.domain.Enums.UserState;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class StatsControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private ProfessionDataService professionDataService;

    @Autowired
    private RoadmapDataService roadmapDataService;

    @Autowired
    private UserStatsYearlyMapper userStatsYearlyMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 清理 Redis 统计数据
        Set<String> keys = redisTemplate.keys("stats:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建测试用户
     */
    private UserDO createUser(String email, UserRole role) {
        UserDO user = new UserDO();
        user.setEmail(email);
        user.setName(email.split("@")[0]);
        user.setPassword("hashed_password");
        user.setState(UserState.ACTIVE.value());
        user.setEmailValidated(true);
        user.setBiography("");
        user.setRole(role.value());
        user.setMsgReadTime(java.time.LocalDateTime.now());
        userDataService.insert(user);
        return user;
    }

    private UserDO createUser(String email) {
        return createUser(email, UserRole.USER);
    }

    /**
     * 创建测试课程
     */
    private CourseDO createCourse(String name) {
        CourseDO course = new CourseDO();
        course.setName(name);
        course.setDescription("测试课程");
        course.setCreatorId(1L);
        course.setParentCourseId(0L);
        course.setRootNodeId(0L);
        course.setMainCategory(1);
        course.setSubCategory(1);
        course.setState((byte) 2);
        courseDataService.insert(course);
        return course;
    }

    /**
     * 生成测试 Token
     */
    private String generateToken(Long userId) {
        StpUtil.login(userId);
        return StpUtil.getTokenValue();
    }

    /**
     * 在 Redis 中插入用户今日统计数据
     */
    private void insertUserTodayStats(Long userId, int views, int twices, int likes, int comments) {
        String today = LocalDate.now().toString();
        String userKey = "stats:" + today + ":user";

        if (views > 0) {
            redisTemplate.opsForHash().put(userKey, userId + ":view", String.valueOf(views));
        }
        if (twices > 0) {
            redisTemplate.opsForHash().put(userKey, userId + ":twice", String.valueOf(twices));
        }
        if (likes > 0) {
            redisTemplate.opsForHash().put(userKey, userId + ":like", String.valueOf(likes));
        }
        if (comments > 0) {
            redisTemplate.opsForHash().put(userKey, userId + ":comment", String.valueOf(comments));
        }
    }

    /**
     * 在数据库中插入用户历史统计数据
     */
    private void insertUserHistoryStats(Long userId, LocalDate date, int views, int twices, int likes, int comments) {
        int year = date.getYear();
        String dayKey = date.getMonthValue() + "-" + date.getDayOfMonth();

        // 查询是否已有该年的记录
        UserStatsYearlyDO yearlyDO = userStatsYearlyMapper.getByUserIdAndYear(userId, year);

        if (yearlyDO == null) {
            // 创建新记录
            UserStatsYearlyDO newRecord = new UserStatsYearlyDO();
            newRecord.setUserId(userId);
            newRecord.setStatYear(year);
            newRecord.setStats("{}");
            userStatsYearlyMapper.insert(newRecord);
        }

        // 使用 updateYearlyStatsArray 更新指定日期的统计（现在 SQL 已修复，支持带引号的 key）
        userStatsYearlyMapper.updateYearlyStatsArray(userId, year, dayKey, views, twices, likes, comments);
    }

    // ========== 记录访问接口测试 ==========

    @Test
    void testRecordView_Success() throws Exception {
        CourseDO course = createCourse("Java基础");
        UserDO user = createUser("user1@test.com");

        Map<String, Object> requestBody = Map.of(
            "articleId", course.getId(),
            "userId", user.getId()
        );

        mockMvc.perform(post("/api/v1/stats/views")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        // 验证 Redis 中写入了数据
        String today = LocalDate.now().toString();
        String contentKey = "stats:" + today + ":content";
        assertTrue(redisTemplate.hasKey(contentKey));
    }

    @Test
    void testRecordView_MissingArticleId() throws Exception {
        UserDO user = createUser("user2@test.com");

        Map<String, Object> requestBody = Map.of("userId", user.getId());

        mockMvc.perform(post("/api/v1/stats/views")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testRecordView_InvalidArticleId() throws Exception {
        UserDO user = createUser("user3@test.com");

        Map<String, Object> requestBody = Map.of(
            "articleId", -1,
            "userId", user.getId()
        );

        mockMvc.perform(post("/api/v1/stats/views")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testRecordView_WithIpAddress() throws Exception {
        CourseDO course = createCourse("Python入门");
        UserDO user = createUser("user4@test.com");

        Map<String, Object> requestBody = Map.of(
            "articleId", course.getId(),
            "userId", user.getId(),
            "ipAddress", "192.168.1.100"
        );

        mockMvc.perform(post("/api/v1/stats/views")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    // ========== 用户今日统计接口测试 ==========

    @Test
    void testGetUserTodayStats_Success() throws Exception {
        UserDO user = createUser("user5@test.com");
        insertUserTodayStats(user.getId(), 50, 10, 5, 3);

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/stats/users/{userId}/today", user.getId())
                    .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.views").value(50))
                .andExpect(jsonPath("$.data.twices").value(10))
                .andExpect(jsonPath("$.data.likes").value(5))
                .andExpect(jsonPath("$.data.comments").value(3));
        } finally {
            StpUtil.logout();
        }
    }

    @Test
    void testGetUserTodayStats_NoData() throws Exception {
        UserDO user = createUser("user6@test.com");

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/stats/users/{userId}/today", user.getId())
                    .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.views").value(0))
                .andExpect(jsonPath("$.data.twices").value(0))
                .andExpect(jsonPath("$.data.likes").value(0))
                .andExpect(jsonPath("$.data.comments").value(0));
        } finally {
            StpUtil.logout();
        }
    }

    @Test
    void testGetUserTodayStats_InvalidUserId() throws Exception {
        UserDO user = createUser("user-param@test.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/stats/users/{userId}/today", -1)
                    .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

            mockMvc.perform(get("/api/v1/stats/users/{userId}/today", 0)
                    .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    @Test
    void testGetUserTodayStats_Aggregation() throws Exception {
        UserDO user = createUser("user7@test.com");

        // 插入多条统计数据
        String today = LocalDate.now().toString();
        String userKey = "stats:" + today + ":user";
        redisTemplate.opsForHash().put(userKey, user.getId() + ":view", "10");
        redisTemplate.opsForHash().put(userKey, user.getId() + ":view", "20"); // 覆盖
        redisTemplate.opsForHash().put(userKey, user.getId() + ":twice", "5");
        redisTemplate.opsForHash().put(userKey, user.getId() + ":like", "3");
        redisTemplate.opsForHash().put(userKey, user.getId() + ":comment", "2");

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/stats/users/{userId}/today", user.getId())
                    .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.views").value(20))
                .andExpect(jsonPath("$.data.twices").value(5))
                .andExpect(jsonPath("$.data.likes").value(3))
                .andExpect(jsonPath("$.data.comments").value(2));
        } finally {
            StpUtil.logout();
        }
    }

    // ========== 用户历史统计接口测试 ==========

    @Test
    void testGetUserHistoryStats_Default7Days() throws Exception {
        UserDO user = createUser("user8@test.com");

        // 准备过去7天的数据
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 1; i--) {
            LocalDate date = today.minusDays(i);
            insertUserHistoryStats(user.getId(), date, 10 * i, 2 * i, 1 * i, 1);
        }

        // 准备今日数据（Redis）
        insertUserTodayStats(user.getId(), 70, 14, 7, 1);

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/stats/users/{userId}/history", user.getId())
                    .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.views").exists())
                .andExpect(jsonPath("$.data.twices").exists())
                .andExpect(jsonPath("$.data.likes").exists())
                .andExpect(jsonPath("$.data.comments").exists())
                .andExpect(jsonPath("$.data.dailyStats").isArray())
                .andExpect(jsonPath("$.data.dailyStats.length()").value(7));
        } finally {
            StpUtil.logout();
        }
    }

    @Test
    void testGetUserHistoryStats_CustomDays() throws Exception {
        UserDO user = createUser("user9@test.com");

        // 准备过去30天的数据
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 1; i--) {
            LocalDate date = today.minusDays(i);
            insertUserHistoryStats(user.getId(), date, 10, 2, 1, 1);
        }

        // 准备今日数据
        insertUserTodayStats(user.getId(), 10, 2, 1, 1);

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/stats/users/{userId}/history", user.getId())
                    .param("days", "30")
                    .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dailyStats").isArray())
                .andExpect(jsonPath("$.data.dailyStats.length()").value(30))
                .andExpect(jsonPath("$.data.views").value(300))
                .andExpect(jsonPath("$.data.twices").value(60))
                .andExpect(jsonPath("$.data.likes").value(30))
                .andExpect(jsonPath("$.data.comments").value(30));
        } finally {
            StpUtil.logout();
        }
    }

    @Test
    void testGetUserHistoryStats_OnlyTodayData() throws Exception {
        UserDO user = createUser("user12@test.com");

        // 只有今日数据
        insertUserTodayStats(user.getId(), 50, 10, 5, 3);

        mockMvc.perform(get("/api/v1/stats/users/{userId}/history", user.getId())
                .param("days", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.views").value(50));
    }

    @Test
    void testGetUserHistoryStats_NoData() throws Exception {
        UserDO user = createUser("user13@test.com");

        mockMvc.perform(get("/api/v1/stats/users/{userId}/history", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.views").value(0))
            .andExpect(jsonPath("$.data.twices").value(0))
            .andExpect(jsonPath("$.data.likes").value(0))
            .andExpect(jsonPath("$.data.comments").value(0))
            .andExpect(jsonPath("$.data.dailyStats").isArray())
            .andExpect(jsonPath("$.data.dailyStats.length()").value(7))
            .andExpect(jsonPath("$.data.dailyStats[0].views").value(0))
            .andExpect(jsonPath("$.data.dailyStats[0].twice").value(0));
    }

    @Test
    void testGetUserHistoryStats_InvalidDays() throws Exception {
        UserDO user = createUser("user14@test.com");

        mockMvc.perform(get("/api/v1/stats/users/{userId}/history", user.getId())
                .param("days", "-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        mockMvc.perform(get("/api/v1/stats/users/{userId}/history", user.getId())
                .param("days", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    // ========== 用户全部时间统计接口测试 ==========

    @Test
    void testGetUserAllTimeStats_Success() throws Exception {
        UserDO user = createUser("user15@test.com");

        // 准备多年历史数据
        insertUserHistoryStats(user.getId(), LocalDate.of(2023, 1, 15), 100, 20, 10, 5);
        insertUserHistoryStats(user.getId(), LocalDate.of(2024, 6, 20), 200, 40, 20, 10);

        // 准备今日数据
        insertUserTodayStats(user.getId(), 50, 10, 5, 3);

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/stats/users/{userId}/all-time", user.getId())
                    .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.views").value(350))
                .andExpect(jsonPath("$.data.twices").value(70))
                .andExpect(jsonPath("$.data.likes").value(35))
                .andExpect(jsonPath("$.data.comments").value(18));
        } finally {
            StpUtil.logout();
        }
    }

    @Test
    void testGetUserAllTimeStats_OnlyHistorical() throws Exception {
        UserDO user = createUser("user16@test.com");

        // 只有历史数据
        insertUserHistoryStats(user.getId(), LocalDate.now().minusDays(10), 100, 20, 10, 5);

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/stats/users/{userId}/all-time", user.getId())
                    .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.views").value(100));
        } finally {
            StpUtil.logout();
        }
    }

    @Test
    void testGetUserAllTimeStats_OnlyToday() throws Exception {
        UserDO user = createUser("user17@test.com");

        // 只有今日数据
        insertUserTodayStats(user.getId(), 50, 10, 5, 3);

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/stats/users/{userId}/all-time", user.getId())
                    .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.views").value(50));
        } finally {
            StpUtil.logout();
        }
    }

    @Test
    void testGetUserAllTimeStats_NoData() throws Exception {
        UserDO user = createUser("user18@test.com");

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/stats/users/{userId}/all-time", user.getId())
                    .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.views").value(0))
                .andExpect(jsonPath("$.data.twices").value(0))
                .andExpect(jsonPath("$.data.likes").value(0))
                .andExpect(jsonPath("$.data.comments").value(0));
        } finally {
            StpUtil.logout();
        }
    }

    // ========== 平台统计接口测试 ==========

    @Test
    void testGetPlatformStats_Success() throws Exception {
        // 创建平台数据
        createCourse("课程1");
        createCourse("课程2");

        mockMvc.perform(get("/api/v1/stats/platform"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.courseCount").exists())
            .andExpect(jsonPath("$.data.careerPathCount").exists())
            .andExpect(jsonPath("$.data.roadmapCount").exists())
            .andExpect(jsonPath("$.data.knowledgeNodeCount").exists())
            .andExpect(jsonPath("$.data.articleCount").exists())
            .andExpect(jsonPath("$.data.lastUpdated").exists());
    }

    @Test
    void testGetPlatformStats_NoAuth() throws Exception {
        // 不需要认证
        mockMvc.perform(get("/api/v1/stats/platform"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    // ========== 手动同步统计接口测试 ==========

    @Test
    void testManualSync_Success() throws Exception {
        UserDO admin = createUser("admin@test.com", UserRole.ADMIN);
        String token = generateToken(admin.getId());

        // 准备昨日 Redis 数据
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String userKey = "stats:" + yesterday + ":user";
        redisTemplate.opsForHash().put(userKey, "1:view", "100");
        redisTemplate.opsForHash().put(userKey, "1:twice", "20");

        mockMvc.perform(post("/api/v1/stats/sync/manual")
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").value("同步成功"));
    }

    @Test
    void testManualSync_NoYesterdayData() throws Exception {
        UserDO admin = createUser("admin2@test.com", UserRole.ADMIN);
        String token = generateToken(admin.getId());

        // Redis 中无昨日数据
        mockMvc.perform(post("/api/v1/stats/sync/manual")
                .header("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testManualSync_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/stats/sync/manual"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ========== 同步指定日期接口测试 ==========

    @Test
    void testSyncByDate_Success() throws Exception {
        UserDO admin = createUser("admin3@test.com", UserRole.ADMIN);
        String token = generateToken(admin.getId());

        // 准备指定日期的 Redis 数据
        String targetDate = "2024-01-15";
        String userKey = "stats:" + targetDate + ":user";
        redisTemplate.opsForHash().put(userKey, "1:view", "50");

        Map<String, Object> requestBody = Map.of("date", targetDate);

        mockMvc.perform(post("/api/v1/stats/sync/date")
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").value("指定日期统计数据同步成功"));
    }

    @Test
    void testSyncByDate_NoDataForDate() throws Exception {
        UserDO admin = createUser("admin4@test.com", UserRole.ADMIN);
        String token = generateToken(admin.getId());

        Map<String, Object> requestBody = Map.of("date", "2024-01-15");

        mockMvc.perform(post("/api/v1/stats/sync/date")
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testSyncByDate_InvalidDateFormat() throws Exception {
        UserDO admin = createUser("admin5@test.com", UserRole.ADMIN);
        String token = generateToken(admin.getId());

        Map<String, Object> requestBody = Map.of("date", "01-15-2024");

        mockMvc.perform(post("/api/v1/stats/sync/date")
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()))
            .andExpect(jsonPath("$.message").value("无效的日期格式，请使用 yyyy-MM-dd"));
    }

    @Test
    void testSyncByDate_EmptyDate() throws Exception {
        UserDO admin = createUser("admin6@test.com", UserRole.ADMIN);
        String token = generateToken(admin.getId());

        Map<String, Object> requestBody = Map.of("date", "");

        mockMvc.perform(post("/api/v1/stats/sync/date")
                .header("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testSyncByDate_Unauthorized() throws Exception {
        Map<String, Object> requestBody = Map.of("date", "2024-01-15");

        mockMvc.perform(post("/api/v1/stats/sync/date")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ========== 边界条件测试 ==========

    @Test
    void testGetUserHistoryStats_OneDayOnly() throws Exception {
        UserDO user = createUser("user19@test.com");
        insertUserTodayStats(user.getId(), 50, 10, 5, 3);

        mockMvc.perform(get("/api/v1/stats/users/{userId}/history", user.getId())
                .param("days", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.dailyStats.length()").value(1));
    }

    @Test
    void testGetUserHistoryStats_LargeDays() throws Exception {
        UserDO user = createUser("user20@test.com");
        insertUserTodayStats(user.getId(), 10, 2, 1, 1);

        mockMvc.perform(get("/api/v1/stats/users/{userId}/history", user.getId())
                .param("days", "365"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testRecordView_LargeValues() throws Exception {
        CourseDO course = createCourse("测试课程");
        UserDO user = createUser("user21@test.com");

        Map<String, Object> requestBody = Map.of(
            "articleId", course.getId(),
            "userId", user.getId()
        );

        // 多次记录访问
        for (int i = 0; i < 1000; i++) {
            mockMvc.perform(post("/api/v1/stats/views")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
        }

        // 验证统计正确
        mockMvc.perform(get("/api/v1/stats/users/{userId}/today", user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.views").exists());
    }

    // ========== 数据一致性测试 ==========

    @Test
    void testDataConsistency_RedisToDatabaseSync() throws Exception {
        UserDO user = createUser("user22@test.com");
        UserDO admin = createUser("admin7@test.com", UserRole.ADMIN);
        String token = generateToken(admin.getId());

        // 在 Redis 中插入昨日数据
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String userKey = "stats:" + yesterday + ":user";
        redisTemplate.opsForHash().put(userKey, user.getId() + ":view", "100");
        redisTemplate.opsForHash().put(userKey, user.getId() + ":twice", "20");

        // 执行同步
        mockMvc.perform(post("/api/v1/stats/sync/manual")
                .header("token", token))
            .andExpect(status().isOk());

        // 查询历史统计验证数据已同步
        mockMvc.perform(get("/api/v1/stats/users/{userId}/history", user.getId())
                .param("days", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.views").value(100))
            .andExpect(jsonPath("$.data.twices").value(20));
    }

    // ========== 参数验证测试 ==========

    @Test
    void testParameterValidation_PathVariable() throws Exception {
        // userId 为负数
        mockMvc.perform(get("/api/v1/stats/users/{userId}/today", -1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        // userId 为 0
        mockMvc.perform(get("/api/v1/stats/users/{userId}/all-time", 0))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testParameterValidation_RequestParam() throws Exception {
        UserDO user = createUser("user23@test.com");

        // days 为负数
        mockMvc.perform(get("/api/v1/stats/users/{userId}/history", user.getId())
                .param("days", "-10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        // days 为 0
        mockMvc.perform(get("/api/v1/stats/users/{userId}/history", user.getId())
                .param("days", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }

    @Test
    void testParameterValidation_RequestBody() throws Exception {
        // articleId 为 null
        Map<String, Object> requestBody1 = Map.of("userId", 1L);
        mockMvc.perform(post("/api/v1/stats/views")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody1)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        // userId 为 null
        Map<String, Object> requestBody2 = Map.of("articleId", 1L);
        mockMvc.perform(post("/api/v1/stats/views")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
    }
}
