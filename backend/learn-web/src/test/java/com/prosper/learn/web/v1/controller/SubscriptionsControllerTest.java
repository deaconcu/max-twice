package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.shared.domain.Enums.*;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 订阅管理接口测试
 * 测试文档: docs/test/subscription.md
 */
@Transactional
public class SubscriptionsControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private UserProfileDataService userProfileDataService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // ==================== 测试辅助方法 ====================

    /**
     * 创建测试用户
     */
    private UserDO createUser(String email) {
        return userDomainService.createUser(email, "password123");
    }

    /**
     * 创建已发布课程
     */
    private CourseDO createPublishedCourse(String name, Long creatorId) {
        CourseDO course = new CourseDO();
        course.setName(name);
        course.setDescription("课程描述");
        course.setCreatorId(creatorId);
        course.setState(ContentState.PUBLISHED.value());
        course.setMainCategory(1);
        course.setSubCategory(1);
        course.setRootNodeId(0L);
        course.setParentCourseId(0L);
        courseDataService.insert(course);

        // 创建根节点
        NodeDO rootNode = NodeDO.createRoot(creatorId, course.getId());
        nodeDataService.insert(rootNode);

        // 更新课程的 rootNodeId
        course.setRootNodeId(rootNode.getId());
        courseDataService.update(course);

        return course;
    }

    /**
     * 获取用户的订阅列表（从数据库）
     */
    private List<Long> getUserSubscriptions(Long userId) {
        UserProfileDO userProfileDO = userProfileDataService.getById(userId);
        String subscriptions = userProfileDO.getSubscription();
        if (subscriptions == null || subscriptions.isEmpty()) {
            return List.of();
        }
        return java.util.Arrays.stream(subscriptions.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();
    }

    // ==================== 接口1: 获取用户订阅列表 ====================

    /**
     * 1.1 成功获取订阅列表 - 有订阅
     */
    @Test
    @DisplayName("成功获取订阅列表 - 有订阅")
    void testGetUserSubscriptions_WithSubscriptions_Success() throws Exception {
        // 准备数据
        UserDO user = createUser("user1@test.com");
        CourseDO course1 = createPublishedCourse("课程1", user.getId());
        CourseDO course2 = createPublishedCourse("课程2", user.getId());
        CourseDO course3 = createPublishedCourse("课程3", user.getId());

        // 用户订阅课程
        StpUtil.login(user.getId());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course1.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course2.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course3.getId())))
                .andExpect(status().isOk());

        // 获取订阅列表（保持登录状态）
        mockMvc.perform(get("/api/v1/users/" + user.getId() + "/subscriptions")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].description").exists())
                .andExpect(jsonPath("$.data[0].mainCategory").exists())
                .andExpect(jsonPath("$.data[0].subCategory").exists())
                .andExpect(jsonPath("$.data[0].learnerCount").exists())
                .andExpect(jsonPath("$.data[0].subscriptionCount").exists())
                .andExpect(jsonPath("$.data[0].subscribed").value(true))
                .andExpect(jsonPath("$.data[0].progress").exists());

        StpUtil.logout();
    }

    /**
     * 1.2 成功获取订阅列表 - 无订阅
     */
    @Test
    @DisplayName("成功获取订阅列表 - 无订阅")
    void testGetUserSubscriptions_NoSubscriptions_Success() throws Exception {
        UserDO user = createUser("user2@test.com");

        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/" + user.getId() + "/subscriptions")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 1.4 字段验证 - userId 无效（0）
     */
    @Test
    @DisplayName("字段验证 - userId 无效为0")
    void testGetUserSubscriptions_UserIdZero_Fail() throws Exception {
        UserDO user = createUser("user-param-1@test.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/0/subscriptions")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 1.5 字段验证 - userId 无效（负数）
     */
    @Test
    @DisplayName("字段验证 - userId 无效为负数")
    void testGetUserSubscriptions_UserIdNegative_Fail() throws Exception {
        UserDO user = createUser("user-param-2@test.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/-1/subscriptions")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 1.6 业务验证 - 用户不存在
     */
    @Test
    @DisplayName("业务验证 - 用户不存在")
    void testGetUserSubscriptions_UserNotFound_Fail() throws Exception {
        UserDO user = createUser("user-param-3@test.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/99999/subscriptions")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_FOUND.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 1.7 权限验证 - 需要登录才能访问
     */
    @Test
    @DisplayName("权限验证 - 需要登录才能访问")
    void testGetUserSubscriptions_NoLoginRequired_Success() throws Exception {
        UserDO user = createUser("user3@test.com");

        StpUtil.login(user.getId());

        try {
            // 需要传token才能获取订阅列表
            mockMvc.perform(get("/api/v1/users/" + user.getId() + "/subscriptions")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 1.9 用户字段填充 - subscribed 始终为 true
     */
    @Test
    @DisplayName("用户字段填充 - subscribed 始终为 true")
    void testGetUserSubscriptions_SubscribedAlwaysTrue_Success() throws Exception {
        UserDO user = createUser("user4@test.com");
        CourseDO course = createPublishedCourse("课程A", user.getId());

        // 订阅课程
        StpUtil.login(user.getId());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course.getId())))
                .andExpect(status().isOk());

        // 获取订阅列表（保持登录状态）
        mockMvc.perform(get("/api/v1/users/" + user.getId() + "/subscriptions")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].subscribed").value(true));

        StpUtil.logout();
    }

    // ==================== 接口2: 订阅课程 ====================

    /**
     * 2.1 成功订阅课程
     */
    @Test
    @DisplayName("成功订阅课程")
    void testSubscribe_Success() throws Exception {
        UserDO user = createUser("user5@test.com");
        CourseDO course = createPublishedCourse("课程B", user.getId());

        StpUtil.login(user.getId());

        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 验证数据库
        List<Long> subscriptions = getUserSubscriptions(user.getId());
        assertThat(subscriptions).contains(course.getId());

        StpUtil.logout();
    }

    /**
     * 2.2 字段验证 - courseId 缺失
     */
    @Test
    @DisplayName("字段验证 - courseId 缺失")
    void testSubscribe_CourseIdMissing_Fail() throws Exception {
        UserDO user = createUser("user6@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 2.3 字段验证 - courseId 无效（0）
     */
    @Test
    @DisplayName("字段验证 - courseId 无效为0")
    void testSubscribe_CourseIdZero_Fail() throws Exception {
        UserDO user = createUser("user7@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content("{\"courseId\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 2.4 字段验证 - courseId 无效（负数）
     */
    @Test
    @DisplayName("字段验证 - courseId 无效为负数")
    void testSubscribe_CourseIdNegative_Fail() throws Exception {
        UserDO user = createUser("user8@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content("{\"courseId\": -1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 2.5 业务验证 - 课程不存在
     */
    @Test
    @DisplayName("业务验证 - 课程不存在")
    void testSubscribe_CourseNotFound_Fail() throws Exception {
        UserDO user = createUser("user9@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content("{\"courseId\": 99999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.COURSE_NOT_FOUND.getCode()));

        StpUtil.logout();
    }

    /**
     * 2.8 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录订阅课程")
    void testSubscribe_NotLoggedIn_Fail() throws Exception {
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\": 123}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 2.9 业务验证 - 订阅数量达到上限
     */
    @Test
    @DisplayName("业务验证 - 订阅数量达到上限")
    void testSubscribe_LimitExceeded_Fail() throws Exception {
        UserDO user = createUser("user22@test.com");

        // 直接在数据库层面添加99个订阅，避免触发限流
        for (int i = 1; i <= 99; i++) {
            CourseDO course = createPublishedCourse("课程" + i, user.getId());
            userDomainService.addSubscription(user.getId(), course.getId());
        }

        StpUtil.login(user.getId());

        // 通过API订阅第100个课程（应该成功）
        CourseDO course100 = createPublishedCourse("课程100", user.getId());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course100.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 尝试订阅第101个课程（应该失败）
        CourseDO course101 = createPublishedCourse("课程101", user.getId());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course101.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_SUBSCRIPTION_LIMIT_EXCEEDED.getCode()));

        // 验证订阅数量仍为100
        List<Long> subscriptions = getUserSubscriptions(user.getId());
        assertThat(subscriptions.size()).isEqualTo(100);
        assertThat(subscriptions).doesNotContain(course101.getId());

        StpUtil.logout();
    }

    /**
     * 2.10 并发订阅 - 多个用户订阅同一课程
     */
    @Test
    @DisplayName("并发订阅 - 多个用户订阅同一课程")
    void testSubscribe_MultipleUsers_Success() throws Exception {
        UserDO user1 = createUser("user10@test.com");
        UserDO user2 = createUser("user11@test.com");
        UserDO user3 = createUser("user12@test.com");
        CourseDO course = createPublishedCourse("热门课程", user1.getId());

        // 用户1订阅
        StpUtil.login(user1.getId());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        StpUtil.logout();

        // 用户2订阅
        StpUtil.login(user2.getId());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        StpUtil.logout();

        // 用户3订阅
        StpUtil.login(user3.getId());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        StpUtil.logout();

        // 验证所有用户都订阅成功
        assertThat(getUserSubscriptions(user1.getId())).contains(course.getId());
        assertThat(getUserSubscriptions(user2.getId())).contains(course.getId());
        assertThat(getUserSubscriptions(user3.getId())).contains(course.getId());
    }

    // ==================== 接口3: 取消订阅 ====================

    /**
     * 3.1 成功取消订阅
     */
    @Test
    @DisplayName("成功取消订阅")
    void testUnsubscribe_Success() throws Exception {
        UserDO user = createUser("user13@test.com");
        CourseDO course = createPublishedCourse("课程C", user.getId());

        // 先订阅
        StpUtil.login(user.getId());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course.getId())))
                .andExpect(status().isOk());

        // 取消订阅
        mockMvc.perform(delete("/api/v1/users/current/subscriptions/" + course.getId())
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());

        // 验证数据库
        List<Long> subscriptions = getUserSubscriptions(user.getId());
        assertThat(subscriptions).doesNotContain(course.getId());

        StpUtil.logout();
    }

    /**
     * 3.2 字段验证 - courseId 无效（0）
     */
    @Test
    @DisplayName("字段验证 - courseId 无效为0")
    void testUnsubscribe_CourseIdZero_Fail() throws Exception {
        UserDO user = createUser("user14@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(delete("/api/v1/users/current/subscriptions/0")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 3.3 字段验证 - courseId 无效（负数）
     */
    @Test
    @DisplayName("字段验证 - courseId 无效为负数")
    void testUnsubscribe_CourseIdNegative_Fail() throws Exception {
        UserDO user = createUser("user15@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(delete("/api/v1/users/current/subscriptions/-1")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        StpUtil.logout();
    }

    /**
     * 3.4 业务验证 - 课程不存在
     */
    @Test
    @DisplayName("业务验证 - 课程不存在")
    void testUnsubscribe_CourseNotFound_Fail() throws Exception {
        UserDO user = createUser("user16@test.com");
        StpUtil.login(user.getId());

        mockMvc.perform(delete("/api/v1/users/current/subscriptions/99999")
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.COURSE_NOT_FOUND.getCode()));

        StpUtil.logout();
    }

    /**
     * 3.5 幂等性验证 - 取消未订阅的课程
     */
    @Test
    @DisplayName("业务验证 - 取消未订阅的课程")
    void testUnsubscribe_NotSubscribed_Fail() throws Exception {
        UserDO user = createUser("user17@test.com");
        CourseDO course = createPublishedCourse("课程D", user.getId());

        StpUtil.login(user.getId());

        // 直接取消订阅（未订阅过）
        mockMvc.perform(delete("/api/v1/users/current/subscriptions/" + course.getId())
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_COURSE_NOT_SUBSCRIBED.getCode()));

        StpUtil.logout();
    }

    /**
     * 3.6 业务验证 - 重复取消订阅
     */
    @Test
    @DisplayName("业务验证 - 重复取消订阅")
    void testUnsubscribe_Twice_Fail() throws Exception {
        UserDO user = createUser("user18@test.com");
        CourseDO course = createPublishedCourse("课程E", user.getId());

        StpUtil.login(user.getId());

        // 订阅
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course.getId())))
                .andExpect(status().isOk());

        // 第一次取消订阅
        mockMvc.perform(delete("/api/v1/users/current/subscriptions/" + course.getId())
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 第二次取消订阅（应该失败）
        mockMvc.perform(delete("/api/v1/users/current/subscriptions/" + course.getId())
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_COURSE_NOT_SUBSCRIBED.getCode()));

        StpUtil.logout();
    }

    /**
     * 3.7 权限验证 - 未登录
     */
    @Test
    @DisplayName("权限验证 - 未登录取消订阅")
    void testUnsubscribe_NotLoggedIn_Fail() throws Exception {
        mockMvc.perform(delete("/api/v1/users/current/subscriptions/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 3.9 订阅列表完整性 - 取消部分订阅
     */
    @Test
    @DisplayName("订阅列表完整性 - 取消部分订阅")
    void testUnsubscribe_PartialUnsubscribe_Success() throws Exception {
        UserDO user = createUser("user19@test.com");
        CourseDO course1 = createPublishedCourse("课程1", user.getId());
        CourseDO course2 = createPublishedCourse("课程2", user.getId());
        CourseDO course3 = createPublishedCourse("课程3", user.getId());

        StpUtil.login(user.getId());

        // 订阅3个课程
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course1.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course2.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course3.getId())))
                .andExpect(status().isOk());

        // 取消订阅课程2
        mockMvc.perform(delete("/api/v1/users/current/subscriptions/" + course2.getId())
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 验证订阅列表
        List<Long> subscriptions = getUserSubscriptions(user.getId());
        assertThat(subscriptions).contains(course1.getId(), course3.getId());
        assertThat(subscriptions).doesNotContain(course2.getId());

        StpUtil.logout();
    }

    // ==================== 业务场景测试 ====================

    /**
     * 6.1 完整订阅-取消流程
     */
    @Test
    @DisplayName("完整订阅-取消流程")
    void testSubscribeUnsubscribeFlow_Success() throws Exception {
        UserDO user = createUser("user20@test.com");
        CourseDO course = createPublishedCourse("流程测试课程", user.getId());

        StpUtil.login(user.getId());

        // 1. 订阅课程
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 2. 获取订阅列表，应包含课程
        List<Long> subscriptions1 = getUserSubscriptions(user.getId());
        assertThat(subscriptions1).contains(course.getId());

        // 3. 取消订阅
        mockMvc.perform(delete("/api/v1/users/current/subscriptions/" + course.getId())
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 4. 获取订阅列表，不应包含课程
        List<Long> subscriptions2 = getUserSubscriptions(user.getId());
        assertThat(subscriptions2).doesNotContain(course.getId());

        StpUtil.logout();
    }

    /**
     * 6.2 多课程订阅管理
     */
    @Test
    @DisplayName("多课程订阅管理")
    void testMultipleCourseSubscription_Success() throws Exception {
        UserDO user = createUser("user21@test.com");
        CourseDO course1 = createPublishedCourse("课程1", user.getId());
        CourseDO course2 = createPublishedCourse("课程2", user.getId());
        CourseDO course3 = createPublishedCourse("课程3", user.getId());
        CourseDO course4 = createPublishedCourse("课程4", user.getId());

        StpUtil.login(user.getId());

        // 1. 订阅课程1, 2, 3
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course1.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course2.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course3.getId())))
                .andExpect(status().isOk());

        // 2. 验证包含课程1, 2, 3
        List<Long> subscriptions1 = getUserSubscriptions(user.getId());
        assertThat(subscriptions1).containsExactly(course1.getId(), course2.getId(), course3.getId());

        // 3. 取消订阅课程2
        mockMvc.perform(delete("/api/v1/users/current/subscriptions/" + course2.getId())
                        .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk());

        // 4. 验证包含课程1, 3
        List<Long> subscriptions2 = getUserSubscriptions(user.getId());
        assertThat(subscriptions2).containsExactly(course1.getId(), course3.getId());

        // 5. 订阅课程4
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course4.getId())))
                .andExpect(status().isOk());

        // 6. 验证包含课程1, 3, 4
        List<Long> subscriptions3 = getUserSubscriptions(user.getId());
        assertThat(subscriptions3).contains(course1.getId(), course3.getId(), course4.getId());

        StpUtil.logout();
    }

    /**
     * 6.3 跨用户隔离
     */
    @Test
    @DisplayName("跨用户隔离")
    void testCrossUserIsolation_Success() throws Exception {
        UserDO userA = createUser("userA@test.com");
        UserDO userB = createUser("userB@test.com");
        CourseDO course1 = createPublishedCourse("课程1", userA.getId());
        CourseDO course2 = createPublishedCourse("课程2", userA.getId());

        // 用户A订阅课程1
        StpUtil.login(userA.getId());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course1.getId())))
                .andExpect(status().isOk());
        StpUtil.logout();

        // 用户B订阅课程2
        StpUtil.login(userB.getId());
        mockMvc.perform(post("/api/v1/users/current/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("token", StpUtil.getTokenValue())
                        .content(String.format("{\"courseId\": %d}", course2.getId())))
                .andExpect(status().isOk());
        StpUtil.logout();

        // 验证用户A只订阅了课程1
        List<Long> subscriptionsA = getUserSubscriptions(userA.getId());
        assertThat(subscriptionsA).containsExactly(course1.getId());

        // 验证用户B只订阅了课程2
        List<Long> subscriptionsB = getUserSubscriptions(userB.getId());
        assertThat(subscriptionsB).containsExactly(course2.getId());
    }
}
