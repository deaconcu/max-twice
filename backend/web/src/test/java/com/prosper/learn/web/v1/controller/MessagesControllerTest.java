package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.interaction.message.MessageDO;
import com.prosper.learn.interaction.message.MessageDataService;
import com.prosper.learn.interaction.message.MessageDomainService;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
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

import java.util.List;

import static com.prosper.learn.shared.domain.Enums.MessageType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 消息管理接口测试
 * 测试文档: docs/api/messages-test.md
 */
@Transactional
public class MessagesControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private MessageDomainService messageDomainService;

    @Autowired
    private MessageDataService messageDataService;

    @Autowired
    private UserDataService userDataService;

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
     * 创建有完整信息的测试用户
     */
    private UserDO createUserWithProfile(String email, String biography, String avatar) {
        UserDO user = userDomainService.createUser(email, "password123");
        user.setBiography(biography);
        user.setAvatar(avatar);
        userDataService.update(user);
        // 重新获取以确保得到更新后的对象
        return userDataService.getById(user.getId());
    }

    /**
     * 创建关注消息
     */
    private void createFollowMessage(long receiverId, long followerId) {
        messageDomainService.createFollowMessage(receiverId, followerId);
    }

    /**
     * 创建系统消息
     */
    private void createSystemMessage(long receiverId, int type, String content) {
        messageDomainService.createSystemMessage(type, receiverId, content);
    }

    // ==================== 1. 按分类获取消息接口测试 ====================

    /**
     * 测试 1.1.1: 获取互动消息列表 - 成功
     */
    @Test
    @DisplayName("按分类获取消息 - 获取互动消息列表成功")
    void testGetMessagesByCategory_InteractionMessages_Success() throws Exception {
        // 准备测试数据
        UserDO user1 = createUser("test-msg-1@test.com");
        UserDO user2 = createUserWithProfile("test-msg-2@test.com", "Java开发", "avatar.jpg");

        // 创建关注消息
        createFollowMessage(user1.getId(), user2.getId());

        StpUtil.login(user1.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").exists())
                    .andExpect(jsonPath("$.data[0].type").value(2))
                    .andExpect(jsonPath("$.data[0].createdAt").exists())
                    .andExpect(jsonPath("$.data[0].follower.id").value(user2.getId()))
                    .andExpect(jsonPath("$.data[0].follower.biography").value("Java开发"));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.1.2: 获取系统消息列表 - 成功
     */
    @Test
    @DisplayName("按分类获取消息 - 获取系统消息列表成功")
    void testGetMessagesByCategory_SystemMessages_Success() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-msg-3@test.com");

        // 创建系统消息
        String content = "{\"courseId\":100,\"courseName\":\"Java编程\",\"reason\":\"内容不符合规范\"}";
        createSystemMessage(user.getId(), MessageType.courseRejected.value(), content);

        StpUtil.login(user.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "2")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").exists())
                    .andExpect(jsonPath("$.data[0].type").value(11))
                    .andExpect(jsonPath("$.data[0].content").exists());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.1.3: 按类型过滤互动消息 - 成功
     */
    @Test
    @DisplayName("按分类获取消息 - 按类型过滤互动消息成功")
    void testGetMessagesByCategory_FilterByType_Success() throws Exception {
        // 准备测试数据
        UserDO user1 = createUser("test-msg-4@test.com");
        UserDO user2 = createUser("test-msg-5@test.com");
        UserDO user3 = createUser("test-msg-6@test.com");

        // 创建不同类型的消息
        createFollowMessage(user1.getId(), user2.getId());
        createFollowMessage(user1.getId(), user3.getId());

        StpUtil.login(user1.getId());

        try {
            // 只查询关注消息
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
                    .param("type", "2")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[*].type").value(everyItem(equalTo(2))));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.2.1: 首次查询不传 lastId - 成功
     */
    @Test
    @DisplayName("按分类获取消息 - 首次查询不传lastId成功")
    void testGetMessagesByCategory_WithoutLastId_Success() throws Exception {
        // 准备测试数据
        UserDO user1 = createUser("test-msg-7@test.com");
        UserDO user2 = createUser("test-msg-8@test.com");

        // 创建多条消息
        for (int i = 0; i < 5; i++) {
            createFollowMessage(user1.getId(), user2.getId());
        }

        StpUtil.login(user1.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(5));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.2.2: lastId 超出范围 - 返回空列表
     */
    @Test
    @DisplayName("按分类获取消息 - lastId超出范围返回空列表")
    void testGetMessagesByCategory_LastIdOutOfRange_ReturnsEmpty() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-msg-9@test.com");

        StpUtil.login(user.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
                    .param("lastId", "999999999")
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
     * 测试 1.2.3: 数据库无消息数据 - 返回空列表
     */
    @Test
    @DisplayName("按分类获取消息 - 无消息数据返回空列表")
    void testGetMessagesByCategory_NoMessages_ReturnsEmpty() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-msg-10@test.com");

        StpUtil.login(user.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
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
     * 测试 1.4.1: 未登录访问 - 失败
     */
    @Test
    @DisplayName("按分类获取消息 - 未登录访问失败")
    void testGetMessagesByCategory_NotLoggedIn_Fails() throws Exception {
        mockMvc.perform(get("/api/v1/messages/category")
                .param("category", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试 1.4.2: token 无效 - 失败
     */
    @Test
    @DisplayName("按分类获取消息 - token无效失败")
    void testGetMessagesByCategory_InvalidToken_Fails() throws Exception {
        mockMvc.perform(get("/api/v1/messages/category")
                .param("category", "1")
                .header("token", "invalid-token-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试 1.4.3: 只能查看自己的消息 - 成功
     */
    @Test
    @DisplayName("按分类获取消息 - 只能查看自己的消息")
    void testGetMessagesByCategory_OnlyOwnMessages_Success() throws Exception {
        // 准备测试数据
        UserDO user1 = createUser("test-msg-11@test.com");
        UserDO user2 = createUser("test-msg-12@test.com");
        UserDO user3 = createUser("test-msg-13@test.com");

        // 给 user1 和 user2 分别创建消息
        createFollowMessage(user1.getId(), user3.getId());
        createFollowMessage(user2.getId(), user3.getId());

        // 使用 user1 登录
        StpUtil.login(user1.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));

            // 验证只返回 user1 的消息
            List<MessageDO> messages = messageDataService.listByCategory(user1.getId(), 1, null, 20);
            assertThat(messages).hasSize(1);
            assertThat(messages.get(0).getReceiverId()).isEqualTo(user1.getId());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.5.1: category 参数缺失 - 失败
     */
    @Test
    @DisplayName("按分类获取消息 - category参数缺失失败")
    void testGetMessagesByCategory_MissingCategory_Fails() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-msg-14@test.com");

        StpUtil.login(user.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.5.2: category 参数小于最小值 - 失败
     */
    @Test
    @DisplayName("按分类获取消息 - category小于最小值失败")
    void testGetMessagesByCategory_CategoryTooSmall_Fails() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-msg-15@test.com");

        StpUtil.login(user.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "0")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.5.3: category 参数大于最大值 - 失败
     */
    @Test
    @DisplayName("按分类获取消息 - category大于最大值失败")
    void testGetMessagesByCategory_CategoryTooLarge_Fails() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-msg-16@test.com");

        StpUtil.login(user.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "4")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.5.4: type 参数无效 - 返回空列表
     */
    @Test
    @DisplayName("按分类获取消息 - type参数无效返回空列表")
    void testGetMessagesByCategory_InvalidType_ReturnsEmpty() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-msg-17@test.com");

        StpUtil.login(user.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
                    .param("type", "999")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        } finally {
            StpUtil.logout();
        }
    }

    // ==================== 2. 邀请用户接口测试 ====================

    /**
     * 测试 2.1.1: 成功邀请用户
     */
    @Test
    @DisplayName("邀请用户 - 成功邀请")
    void testInviteUser_Success() throws Exception {
        // 准备测试数据
        UserDO inviter = createUser("test-invite-1@test.com");
        UserDO invitee = createUser("test-invite-2@test.com");

        StpUtil.login(inviter.getId());

        try {
            // 构造请求体
            String requestBody = String.format("{\"userId\":%d,\"nodeId\":%d}", invitee.getId(), 1L);

            // 发起请求
            mockMvc.perform(post("/api/v1/messages/invite")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证消息已创建
            List<MessageDO> messages = messageDataService.listByCategory(invitee.getId(), 1, null, 20);
            assertThat(messages).isNotEmpty();
            assertThat(messages.get(0).getType()).isEqualTo(MessageType.invite.value());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 2.1.2: 邀请不存在的用户 - 失败
     */
    @Test
    @DisplayName("邀请用户 - 邀请不存在的用户失败")
    void testInviteUser_UserNotExists_Fails() throws Exception {
        // 准备测试数据
        UserDO inviter = createUser("test-invite-3@test.com");

        StpUtil.login(inviter.getId());

        try {
            // 构造请求体
            String requestBody = String.format("{\"userId\":%d,\"nodeId\":%d}", 999999L, 1L);

            // 发起请求
            mockMvc.perform(post("/api/v1/messages/invite")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(StatusCode.OK.getCode())));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 2.2.1: 未登录访问 - 失败
     */
    @Test
    @DisplayName("邀请用户 - 未登录访问失败")
    void testInviteUser_NotLoggedIn_Fails() throws Exception {
        // 构造请求体
        String requestBody = "{\"userId\":1,\"nodeId\":1}";

        mockMvc.perform(post("/api/v1/messages/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试 2.2.2: token 无效 - 失败
     */
    @Test
    @DisplayName("邀请用户 - token无效失败")
    void testInviteUser_InvalidToken_Fails() throws Exception {
        // 构造请求体
        String requestBody = "{\"userId\":1,\"nodeId\":1}";

        mockMvc.perform(post("/api/v1/messages/invite")
                .header("token", "invalid-token-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试 2.3.1: userId 参数缺失 - 失败
     */
    @Test
    @DisplayName("邀请用户 - userId参数缺失失败")
    void testInviteUser_MissingUserId_Fails() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-invite-4@test.com");

        StpUtil.login(user.getId());

        try {
            // 构造请求体 - 不设置 userId
            String requestBody = "{\"nodeId\":1}";

            // 发起请求
            mockMvc.perform(post("/api/v1/messages/invite")
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
     * 测试 2.3.2: nodeId 参数缺失 - 失败
     */
    @Test
    @DisplayName("邀请用户 - nodeId参数缺失失败")
    void testInviteUser_MissingNodeId_Fails() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-invite-5@test.com");

        StpUtil.login(user.getId());

        try {
            // 构造请求体 - 不设置 nodeId
            String requestBody = "{\"userId\":1}";

            // 发起请求
            mockMvc.perform(post("/api/v1/messages/invite")
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
     * 测试 2.3.3: userId 小于等于 0 - 失败
     */
    @Test
    @DisplayName("邀请用户 - userId小于等于0失败")
    void testInviteUser_InvalidUserId_Fails() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-invite-6@test.com");

        StpUtil.login(user.getId());

        try {
            // 构造请求体
            String requestBody = "{\"userId\":0,\"nodeId\":1}";

            // 发起请求
            mockMvc.perform(post("/api/v1/messages/invite")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(StatusCode.OK.getCode())));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 2.3.4: nodeId 小于等于 0 - 失败
     */
    @Test
    @DisplayName("邀请用户 - nodeId小于等于0失败")
    void testInviteUser_InvalidNodeId_Fails() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-invite-7@test.com");

        StpUtil.login(user.getId());

        try {
            // 构造请求体
            String requestBody = "{\"userId\":1,\"nodeId\":0}";

            // 发起请求
            mockMvc.perform(post("/api/v1/messages/invite")
                    .header("token", StpUtil.getTokenValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(StatusCode.OK.getCode())));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.1: 关注消息结构验证
     */
    @Test
    @DisplayName("消息类型 - 关注消息结构正确")
    void testMessageType_FollowMessage_StructureCorrect() throws Exception {
        // 准备测试数据
        UserDO user1 = createUser("test-type-1@test.com");
        UserDO user2 = createUserWithProfile("test-type-2@test.com", "前端工程师", "avatar.jpg");

        // 创建关注消息
        createFollowMessage(user1.getId(), user2.getId());

        StpUtil.login(user1.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
                    .param("type", "2")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data[0].type").value(2))
                    .andExpect(jsonPath("$.data[0].follower").exists())
                    .andExpect(jsonPath("$.data[0].follower.id").value(user2.getId()))
                    .andExpect(jsonPath("$.data[0].follower.biography").value("前端工程师"))
                    .andExpect(jsonPath("$.data[0].follower.avatar").value("avatar.jpg"));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.2: 系统消息结构验证
     */
    @Test
    @DisplayName("消息类型 - 系统消息结构正确")
    void testMessageType_SystemMessage_StructureCorrect() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-type-3@test.com");

        // 创建系统消息
        String content = "{\"courseId\":100,\"courseName\":\"Java编程\",\"linkUrl\":\"/read?courseId=100\"}";
        createSystemMessage(user.getId(), MessageType.courseApproved.value(), content);

        StpUtil.login(user.getId());

        try {
            // 发起请求
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "2")
                    .param("type", "25")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data[0].type").value(25))
                    .andExpect(jsonPath("$.data[0].content").exists())
                    .andExpect(jsonPath("$.data[0].content").value(containsString("courseId")))
                    .andExpect(jsonPath("$.data[0].content").value(containsString("courseName")))
                    .andExpect(jsonPath("$.data[0].content").value(containsString("linkUrl")));
        } finally {
            StpUtil.logout();
        }
    }

    // ==================== 4. MessageListResponse 和 lastViewedMessageId 测试 ====================

    /**
     * 测试 4.1: 首次查询返回 lastViewedMessageId
     */
    @Test
    @DisplayName("消息列表响应 - 首次查询返回lastViewedMessageId")
    void testGetMessagesByCategory_FirstQuery_ReturnsLastViewedMessageId() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-response-1@test.com");
        UserDO follower = createUser("test-response-2@test.com");

        // 设置用户的 lastViewedMessageId 为 5
        user.setLastViewedMessageId(5L);
        userDataService.update(user);

        // 创建消息
        createFollowMessage(user.getId(), follower.getId());

        StpUtil.login(user.getId());

        try {
            // 首次查询（不传 lastId）
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.messages").isArray())
                    .andExpect(jsonPath("$.data.lastViewedMessageId").value(5));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 4.2: 首次查询时更新 lastViewedMessageId
     */
    @Test
    @DisplayName("消息列表响应 - 首次查询更新lastViewedMessageId")
    void testGetMessagesByCategory_FirstQuery_UpdatesLastViewedMessageId() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-response-3@test.com");
        UserDO follower = createUser("test-response-4@test.com");

        // 设置初始 lastViewedMessageId 为 0
        user.setLastViewedMessageId(0L);
        userDataService.update(user);

        // 创建多条消息
        createFollowMessage(user.getId(), follower.getId());
        createFollowMessage(user.getId(), follower.getId());

        StpUtil.login(user.getId());

        try {
            // 首次查询
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.messages").isArray());

            // 验证 lastViewedMessageId 已被更新为第一条消息的 ID
            UserDO updatedUser = userDataService.getById(user.getId());
            List<MessageDO> messages = messageDataService.listByCategory(user.getId(), 1, null, 20);
            assertThat(updatedUser.getLastViewedMessageId()).isEqualTo(messages.get(0).getId());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 4.3: 分页查询不返回 lastViewedMessageId
     */
    @Test
    @DisplayName("消息列表响应 - 分页查询不返回lastViewedMessageId")
    void testGetMessagesByCategory_PaginatedQuery_NoLastViewedMessageId() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-response-5@test.com");
        UserDO follower = createUser("test-response-6@test.com");

        // 创建多条消息
        for (int i = 0; i < 5; i++) {
            createFollowMessage(user.getId(), follower.getId());
        }

        StpUtil.login(user.getId());

        try {
            // 获取第一页消息
            List<MessageDO> messages = messageDataService.listByCategory(user.getId(), 1, null, 20);
            Long firstMessageId = messages.get(0).getId();

            // 分页查询（传入 lastId）
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
                    .param("lastId", String.valueOf(firstMessageId))
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.messages").isArray())
                    .andExpect(jsonPath("$.data.lastViewedMessageId").doesNotExist());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 4.4: 首次查询无消息时不更新 lastViewedMessageId
     */
    @Test
    @DisplayName("消息列表响应 - 无消息时不更新lastViewedMessageId")
    void testGetMessagesByCategory_NoMessages_DoesNotUpdateLastViewedMessageId() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-response-7@test.com");

        // 设置初始 lastViewedMessageId
        Long initialLastViewedMessageId = 10L;
        user.setLastViewedMessageId(initialLastViewedMessageId);
        userDataService.update(user);

        StpUtil.login(user.getId());

        try {
            // 首次查询（无消息）
            mockMvc.perform(get("/api/v1/messages/category")
                    .param("category", "1")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.messages").isEmpty());

            // 验证 lastViewedMessageId 未被更新
            UserDO updatedUser = userDataService.getById(user.getId());
            assertThat(updatedUser.getLastViewedMessageId()).isEqualTo(initialLastViewedMessageId);
        } finally {
            StpUtil.logout();
        }
    }

    // ==================== 5. 未读数量接口测试 ====================

    /**
     * 测试 5.1: 获取未读数量 - 有未读消息
     */
    @Test
    @DisplayName("未读数量 - 获取未读数量成功（有未读）")
    void testGetUnreadCount_HasUnread_Success() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-unread-1@test.com");
        UserDO follower = createUser("test-unread-2@test.com");

        // 设置 lastViewedMessageId
        user.setLastViewedMessageId(0L);
        userDataService.update(user);

        // 创建消息
        createFollowMessage(user.getId(), follower.getId());
        createFollowMessage(user.getId(), follower.getId());
        createFollowMessage(user.getId(), follower.getId());

        StpUtil.login(user.getId());

        try {
            // 查询未读数量
            mockMvc.perform(get("/api/v1/messages/unread-count")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").value(3));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 5.2: 获取未读数量 - 无未读消息
     */
    @Test
    @DisplayName("未读数量 - 获取未读数量成功（无未读）")
    void testGetUnreadCount_NoUnread_Success() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-unread-3@test.com");
        UserDO follower = createUser("test-unread-4@test.com");

        // 创建消息
        createFollowMessage(user.getId(), follower.getId());

        // 获取消息ID并设置为 lastViewedMessageId
        List<MessageDO> messages = messageDataService.listByCategory(user.getId(), 1, null, 20);
        user.setLastViewedMessageId(messages.get(0).getId());
        userDataService.update(user);

        StpUtil.login(user.getId());

        try {
            // 查询未读数量
            mockMvc.perform(get("/api/v1/messages/unread-count")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").value(0));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 5.3: 获取未读数量 - lastViewedMessageId 为 null
     */
    @Test
    @DisplayName("未读数量 - lastViewedMessageId为null时返回全部")
    void testGetUnreadCount_NullLastViewedMessageId_ReturnsAll() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-unread-5@test.com");
        UserDO follower = createUser("test-unread-6@test.com");

        // 确保 lastViewedMessageId 为 null
        user.setLastViewedMessageId(null);
        userDataService.update(user);

        // 创建消息
        createFollowMessage(user.getId(), follower.getId());
        createFollowMessage(user.getId(), follower.getId());

        StpUtil.login(user.getId());

        try {
            // 查询未读数量
            mockMvc.perform(get("/api/v1/messages/unread-count")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").value(2));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 5.4: 获取未读数量 - 未登录失败
     */
    @Test
    @DisplayName("未读数量 - 未登录访问失败")
    void testGetUnreadCount_NotLoggedIn_Fails() throws Exception {
        mockMvc.perform(get("/api/v1/messages/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试 5.5: 获取未读数量 - 包含系统消息和互动消息
     */
    @Test
    @DisplayName("未读数量 - 同时统计系统消息和互动消息")
    void testGetUnreadCount_IncludesBothCategories_Success() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-unread-7@test.com");
        UserDO follower = createUser("test-unread-8@test.com");

        // 设置 lastViewedMessageId
        user.setLastViewedMessageId(0L);
        userDataService.update(user);

        // 创建互动消息
        createFollowMessage(user.getId(), follower.getId());
        createFollowMessage(user.getId(), follower.getId());

        // 创建系统消息
        String content = "{\"courseId\":100,\"courseName\":\"Java编程\"}";
        createSystemMessage(user.getId(), MessageType.courseApproved.value(), content);

        StpUtil.login(user.getId());

        try {
            // 查询未读数量（应该包含互动消息 + 系统消息）
            mockMvc.perform(get("/api/v1/messages/unread-count")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").value(3));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 5.6: 获取未读数量 - 只统计新消息
     */
    @Test
    @DisplayName("未读数量 - 只统计id大于lastViewedMessageId的消息")
    void testGetUnreadCount_OnlyCountNewMessages_Success() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-unread-9@test.com");
        UserDO follower = createUser("test-unread-10@test.com");

        // 创建旧消息
        createFollowMessage(user.getId(), follower.getId());
        createFollowMessage(user.getId(), follower.getId());

        // 获取旧消息的最后ID并设置
        List<MessageDO> oldMessages = messageDataService.listByCategory(user.getId(), 1, null, 20);
        user.setLastViewedMessageId(oldMessages.get(0).getId());
        userDataService.update(user);

        // 创建新消息
        createFollowMessage(user.getId(), follower.getId());
        createFollowMessage(user.getId(), follower.getId());

        StpUtil.login(user.getId());

        try {
            // 查询未读数量（应该只统计新创建的2条消息）
            mockMvc.perform(get("/api/v1/messages/unread-count")
                    .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").value(2));
        } finally {
            StpUtil.logout();
        }
    }
}

