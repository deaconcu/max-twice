package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.interaction.follow.FollowDataService;
import com.prosper.learn.interaction.follow.FollowDO;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 关注管理接口测试
 * 测试文档: docs/test/follow.md
 */
@Transactional
public class FollowsControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private FollowDataService followDataService;

    @Autowired
    private com.prosper.learn.user.profile.UserDataService userDataService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
     * 创建有完整信息的测试用户（包含 avatar 和 biography）
     */
    private UserDO createUserWithProfile(String email, String biography, String avatar) {
        UserDO user = userDomainService.createUser(email, "password123");
        user.setBiography(biography);
        user.setAvatar(avatar);
        userDataService.update(user);
        return user;
    }

    /**
     * 创建关注关系
     */
    private void createFollow(Long followerId, Long followeeId) {
        followDataService.insert(followerId, followeeId);
    }

    /**
     * 验证关注关系是否存在
     */
    private boolean followExists(Long followerId, Long followeeId) {
        FollowDO follow = followDataService.get(followerId, followeeId);
        return follow != null;
    }

    // ==================== 1. 关注用户接口测试 ====================

    /**
     * 测试 1.1.1: 首次关注用户成功
     */
    @Test
    @DisplayName("关注用户 - 首次关注成功")
    void testFollowUser_Success() throws Exception {
        // 准备测试数据
        UserDO user1 = createUser("test-follow-1@test.com");
        UserDO user2 = createUser("test-follow-2@test.com");

        StpUtil.login(user1.getId());

        try {
            String requestBody = String.format("{\"followeeId\": %d}", user2.getId());

            mockMvc.perform(post("/api/v1/follows")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            // 验证数据库
            assertThat(followExists(user1.getId(), user2.getId())).isTrue();

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.1.2: 重复关注（幂等性）
     */
    @Test
    @DisplayName("关注用户 - 重复关注（幂等性）")
    void testFollowUser_Idempotent() throws Exception {
        UserDO user1 = createUser("test-follow-3@test.com");
        UserDO user2 = createUser("test-follow-4@test.com");

        // 先创建关注关系
        createFollow(user1.getId(), user2.getId());

        StpUtil.login(user1.getId());

        try {
            String requestBody = String.format("{\"followeeId\": %d}", user2.getId());

            // 重复关注
            mockMvc.perform(post("/api/v1/follows")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证数据库中仍然只有一条记录
            assertThat(followExists(user1.getId(), user2.getId())).isTrue();

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.1.3: 多个用户关注同一用户
     */
    @Test
    @DisplayName("关注用户 - 多个用户关注同一用户")
    void testFollowUser_MultipleFollowers() throws Exception {
        UserDO user1 = createUser("test-follow-5@test.com");
        UserDO user2 = createUser("test-follow-6@test.com");
        UserDO user3 = createUser("test-follow-7@test.com");

        // user1 关注 user3
        StpUtil.login(user1.getId());
        try {
            String requestBody = String.format("{\"followeeId\": %d}", user3.getId());
            mockMvc.perform(post("/api/v1/follows")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
        } finally {
            StpUtil.logout();
        }

        // user2 关注 user3
        StpUtil.login(user2.getId());
        try {
            String requestBody = String.format("{\"followeeId\": %d}", user3.getId());
            mockMvc.perform(post("/api/v1/follows")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
        } finally {
            StpUtil.logout();
        }

        // 验证两条关注记录都存在
        assertThat(followExists(user1.getId(), user3.getId())).isTrue();
        assertThat(followExists(user2.getId(), user3.getId())).isTrue();
    }

    /**
     * 测试 1.2.1: followeeId 为 null
     */
    @Test
    @DisplayName("参数验证 - followeeId 为 null")
    void testFollowUser_NullFolloweeId() throws Exception {
        UserDO user1 = createUser("test-follow-8@test.com");
        StpUtil.login(user1.getId());

        try {
            String requestBody = "{}";

            mockMvc.perform(post("/api/v1/follows")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.2.2: followeeId 为 0
     */
    @Test
    @DisplayName("参数验证 - followeeId 为 0")
    void testFollowUser_ZeroFolloweeId() throws Exception {
        UserDO user1 = createUser("test-follow-9@test.com");
        StpUtil.login(user1.getId());

        try {
            String requestBody = "{\"followeeId\": 0}";

            mockMvc.perform(post("/api/v1/follows")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.2.3: followeeId 为负数
     */
    @Test
    @DisplayName("参数验证 - followeeId 为负数")
    void testFollowUser_NegativeFolloweeId() throws Exception {
        UserDO user1 = createUser("test-follow-10@test.com");
        StpUtil.login(user1.getId());

        try {
            String requestBody = "{\"followeeId\": -1}";

            mockMvc.perform(post("/api/v1/follows")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.3.1: 被关注用户不存在
     */
    @Test
    @DisplayName("业务错误 - 被关注用户不存在")
    void testFollowUser_UserNotFound() throws Exception {
        UserDO user1 = createUser("test-follow-11@test.com");
        StpUtil.login(user1.getId());

        try {
            String requestBody = "{\"followeeId\": 99999}";

            mockMvc.perform(post("/api/v1/follows")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.3.2: 未登录尝试关注
     */
    @Test
    @DisplayName("业务错误 - 未登录尝试关注")
    void testFollowUser_NotLogin() throws Exception {
        UserDO user2 = createUser("test-follow-12@test.com");

        String requestBody = String.format("{\"followeeId\": %d}", user2.getId());

        mockMvc.perform(post("/api/v1/follows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 2. 取消关注接口测试 ====================

    /**
     * 测试 2.1.1: 成功取消关注
     */
    @Test
    @DisplayName("取消关注 - 成功")
    void testUnfollowUser_Success() throws Exception {
        UserDO user1 = createUser("test-unfollow-1@test.com");
        UserDO user2 = createUser("test-unfollow-2@test.com");

        // 先创建关注关系
        createFollow(user1.getId(), user2.getId());
        assertThat(followExists(user1.getId(), user2.getId())).isTrue();

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(delete("/api/v1/follows/" + user2.getId())
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            // 验证关注记录已删除
            assertThat(followExists(user1.getId(), user2.getId())).isFalse();

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 2.1.2: 重复取消关注（幂等性）
     */
    @Test
    @DisplayName("取消关注 - 重复取消（幂等性）")
    void testUnfollowUser_Idempotent() throws Exception {
        UserDO user1 = createUser("test-unfollow-3@test.com");
        UserDO user2 = createUser("test-unfollow-4@test.com");

        // 不创建关注关系
        assertThat(followExists(user1.getId(), user2.getId())).isFalse();

        StpUtil.login(user1.getId());

        try {
            // 取消不存在的关注
            mockMvc.perform(delete("/api/v1/follows/" + user2.getId())
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证仍然不存在
            assertThat(followExists(user1.getId(), user2.getId())).isFalse();

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 2.2.1: followeeId 为 null 字符串
     */
    @Test
    @DisplayName("参数验证 - followeeId 为 null 字符串")
    void testUnfollowUser_NullFolloweeId() throws Exception {
        UserDO user1 = createUser("test-unfollow-5@test.com");
        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(delete("/api/v1/follows/null")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 2.2.2: followeeId 为 0
     */
    @Test
    @DisplayName("参数验证 - followeeId 为 0")
    void testUnfollowUser_ZeroFolloweeId() throws Exception {
        UserDO user1 = createUser("test-unfollow-6@test.com");
        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(delete("/api/v1/follows/0")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 2.2.3: followeeId 为负数
     */
    @Test
    @DisplayName("参数验证 - followeeId 为负数")
    void testUnfollowUser_NegativeFolloweeId() throws Exception {
        UserDO user1 = createUser("test-unfollow-7@test.com");
        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(delete("/api/v1/follows/-1")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 2.3.1: 被关注用户不存在
     */
    @Test
    @DisplayName("业务错误 - 取消关注不存在的用户")
    void testUnfollowUser_UserNotFound() throws Exception {
        UserDO user1 = createUser("test-unfollow-8@test.com");
        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(delete("/api/v1/follows/99999")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 2.3.2: 未登录尝试取消关注
     */
    @Test
    @DisplayName("业务错误 - 未登录尝试取消关注")
    void testUnfollowUser_NotLogin() throws Exception {
        UserDO user2 = createUser("test-unfollow-9@test.com");

        mockMvc.perform(delete("/api/v1/follows/" + user2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 3. 获取关注列表接口测试 ====================

    /**
     * 测试 3.1.1: 获取第一页（空列表）
     */
    @Test
    @DisplayName("获取关注列表 - 空列表")
    void testGetFollowees_EmptyList() throws Exception {
        UserDO user1 = createUser("test-followees-1@test.com");
        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.1.2: 获取第一页（有数据）
     */
    @Test
    @DisplayName("获取关注列表 - 有数据")
    void testGetFollowees_WithData() throws Exception {
        UserDO user1 = createUser("test-followees-2@test.com");
        UserDO user2 = createUserWithProfile("test-followees-3@test.com", "全栈工程师", "https://example.com/avatar2.jpg");
        UserDO user3 = createUserWithProfile("test-followees-4@test.com", "产品经理", "https://example.com/avatar3.jpg");

        // user1 关注 user2 和 user3
        createFollow(user1.getId(), user2.getId());
        Thread.sleep(10); // 确保时间不同
        createFollow(user1.getId(), user3.getId());

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").exists())
                    .andExpect(jsonPath("$.data[0].name").exists())
                    .andExpect(jsonPath("$.data[0].biography").exists())
                    .andExpect(jsonPath("$.data[0].avatar").exists())
                    .andExpect(jsonPath("$.data[0].createdAt").exists());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.1.3: 获取第二页（分页测试）
     */
    @Test
    @DisplayName("获取关注列表 - 分页测试")
    void testGetFollowees_Pagination() throws Exception {
        UserDO user1 = createUser("test-followees-5@test.com");

        // 创建 12 个被关注用户
        for (int i = 0; i < 12; i++) {
            UserDO followee = createUser("test-followees-followee-" + i + "@test.com");
            createFollow(user1.getId(), followee.getId());
        }

        StpUtil.login(user1.getId());

        try {
            // 获取第一页
            String response = mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.length()").value(10))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // 从响应中提取最后一条记录的ID（用于分页）
            int lastIdIndex = response.lastIndexOf("\"id\":");
            String idSubstring = response.substring(lastIdIndex + 5);
            int commaIndex = idSubstring.indexOf(",");
            Long lastId = Long.parseLong(idSubstring.substring(0, commaIndex).trim());

            // 获取第二页
            mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue())
                            .param("lastId", String.valueOf(lastId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.length()").value(2));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.1.4: 获取已加载完的分页
     */
    @Test
    @DisplayName("获取关注列表 - 已加载完返回空列表")
    void testGetFollowees_NoMoreData() throws Exception {
        UserDO user1 = createUser("test-followees-6@test.com");
        UserDO user2 = createUser("test-followees-7@test.com");

        createFollow(user1.getId(), user2.getId());

        StpUtil.login(user1.getId());

        try {
            // 获取第一页
            String response = mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // 提取记录的ID
            int idIndex = response.indexOf("\"id\":");
            String idSubstring = response.substring(idIndex + 5);
            int commaIndex = idSubstring.indexOf(",");
            Long lastId = Long.parseLong(idSubstring.substring(0, commaIndex).trim());

            // 获取第二页（应该为空）
            mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue())
                            .param("lastId", String.valueOf(lastId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.length()").value(0));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.1.5: 查看其他用户的关注列表（公开接口）
     */
    @Test
    @DisplayName("获取关注列表 - 查看其他用户的关注列表")
    void testGetFollowees_OtherUser() throws Exception {
        UserDO user1 = createUser("test-followees-8@test.com");
        UserDO user2 = createUser("test-followees-9@test.com");
        UserDO visitor = createUser("test-followees-10@test.com");

        createFollow(user1.getId(), user2.getId());

        // visitor 登录后查看 user1 的关注列表
        StpUtil.login(visitor.getId());

        try {
            mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.length()").value(1));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.2.1: userId 为 null 字符串
     */
    @Test
    @DisplayName("参数验证 - userId 为 null 字符串")
    void testGetFollowees_NullUserId() throws Exception {
        UserDO user = createUser("test-param-1@test.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/null/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.2.2: userId 为 0
     */
    @Test
    @DisplayName("参数验证 - userId 为 0")
    void testGetFollowees_ZeroUserId() throws Exception {
        UserDO user = createUser("test-param-2@test.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/0/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.2.3: userId 为负数
     */
    @Test
    @DisplayName("参数验证 - userId 为负数")
    void testGetFollowees_NegativeUserId() throws Exception {
        UserDO user = createUser("test-param-3@test.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/-1/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.2.4: lastId 格式错误
     */
    @Test
    @DisplayName("参数验证 - lastId 格式错误")
    void testGetFollowees_InvalidIdFormat() throws Exception {
        UserDO user1 = createUser("test-followees-11@test.com");

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue())
                            .param("lastId", "invalid-id"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.3.1: 用户不存在
     */
    @Test
    @DisplayName("业务错误 - 用户不存在")
    void testGetFollowees_UserNotFound() throws Exception {
        UserDO user = createUser("test-param-4@test.com");
        StpUtil.login(user.getId());

        try {
            mockMvc.perform(get("/api/v1/users/99999/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_FOUND.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.4.1: 头像字段正确填充
     */
    @Test
    @DisplayName("数据一致性 - 头像字段正确填充")
    void testGetFollowees_AvatarField() throws Exception {
        UserDO user1 = createUser("test-followees-12@test.com");
        UserDO user2 = createUserWithProfile("test-followees-13@test.com", "测试简介", "https://example.com/avatar.jpg");

        createFollow(user1.getId(), user2.getId());

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data[0].avatar").value("https://example.com/avatar.jpg"));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.4.3: 关注时间格式正确
     */
    @Test
    @DisplayName("数据一致性 - 关注时间格式正确")
    void testGetFollowees_CreatedAtFormat() throws Exception {
        UserDO user1 = createUser("test-followees-14@test.com");
        UserDO user2 = createUser("test-followees-15@test.com");

        createFollow(user1.getId(), user2.getId());

        // 从数据库获取实际的关注时间
        FollowDO followDO = followDataService.get(user1.getId(), user2.getId());
        LocalDateTime actualTime = followDO.getCreatedAt();

        StpUtil.login(user1.getId());

        try {
            String response = mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].createdAt").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // 验证时间格式
            int createdAtIndex = response.indexOf("\"createdAt\":\"");
            String createdAt = response.substring(
                    createdAtIndex + 13,
                    response.indexOf("\"", createdAtIndex + 13)
            );

            // 解析返回的时间字符串
            LocalDateTime parsedTime = LocalDateTime.parse(createdAt, TIME_FORMATTER);

            // 验证返回的时间与数据库中的时间一致（允许1秒误差）
            assertThat(parsedTime).isAfterOrEqualTo(actualTime.minusSeconds(1));
            assertThat(parsedTime).isBeforeOrEqualTo(actualTime.plusSeconds(1));
        } finally {
            StpUtil.logout();
        }
    }

    // ==================== 4. 集成测试场景 ====================

    /**
     * 测试 5.1: 完整关注流程
     */
    @Test
    @DisplayName("集成测试 - 完整关注流程")
    void testFollowWorkflow_Complete() throws Exception {
        UserDO user1 = createUser("test-workflow-1@test.com");
        UserDO user2 = createUser("test-workflow-2@test.com");

        StpUtil.login(user1.getId());

        try {
            // 1. 关注 user2
            String requestBody = String.format("{\"followeeId\": %d}", user2.getId());
            mockMvc.perform(post("/api/v1/follows")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 2. 验证关注列表包含 user2
            mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].userId").value(user2.getId()));

            // 3. 取消关注 user2
            mockMvc.perform(delete("/api/v1/follows/" + user2.getId())
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 4. 验证关注列表为空
            mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 5.2: 多用户关注网络
     */
    @Test
    @DisplayName("集成测试 - 多用户关注网络")
    void testFollowWorkflow_Network() throws Exception {
        UserDO user1 = createUser("test-network-1@test.com");
        UserDO user2 = createUser("test-network-2@test.com");
        UserDO user3 = createUser("test-network-3@test.com");
        UserDO user4 = createUser("test-network-4@test.com");

        // 建立关注关系
        createFollow(user1.getId(), user2.getId());
        Thread.sleep(10);
        createFollow(user1.getId(), user3.getId());
        Thread.sleep(10);
        createFollow(user2.getId(), user3.getId());
        Thread.sleep(10);
        createFollow(user2.getId(), user4.getId());
        Thread.sleep(10);
        createFollow(user3.getId(), user1.getId());
        Thread.sleep(10);
        createFollow(user3.getId(), user4.getId());

        // 验证 user1 关注列表
        StpUtil.login(user1.getId());
        try {
            mockMvc.perform(get("/api/v1/users/" + user1.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        } finally {
            StpUtil.logout();
        }

        // 验证 user2 关注列表
        StpUtil.login(user2.getId());
        try {
            mockMvc.perform(get("/api/v1/users/" + user2.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        } finally {
            StpUtil.logout();
        }

        // 验证 user3 关注列表
        StpUtil.login(user3.getId());
        try {
            mockMvc.perform(get("/api/v1/users/" + user3.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        } finally {
            StpUtil.logout();
        }

        // 验证 user4 关注列表（为空）
        StpUtil.login(user4.getId());
        try {
            mockMvc.perform(get("/api/v1/users/" + user4.getId() + "/followees")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        } finally {
            StpUtil.logout();
        }
    }
}
