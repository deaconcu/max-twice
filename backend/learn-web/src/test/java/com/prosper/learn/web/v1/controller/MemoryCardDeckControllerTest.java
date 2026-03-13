package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.application.dto.request.CreateDeckRequest;
import com.prosper.learn.application.dto.request.UpdateDeckRequest;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDomainService;
import com.prosper.learn.shared.domain.Enums.*;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 记忆卡片组管理接口测试
 * 测试文档: docs/api/memory-decks-test.md
 *
 * 测试范围:
 * - 获取帖子公共卡片组接口
 * - 获取帖子创建者卡片组接口
 * - 获取用户卡片组接口
 * - 创建卡片组接口
 * - 更新卡片组接口
 * - 删除卡片组接口
 * - 卡片组详情接口
 * - 节点卡片组接口
 */
@Transactional
public class MemoryCardDeckControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private PostDataService postDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private MemoryCardDeckDataService deckDataService;

    @Autowired
    private MemoryCardDeckDomainService deckDomainService;

    @Autowired
    private CourseDataService courseDataService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // ==================== 测试辅助方法 ====================

    /**
     * 创建测试用户
     */
    private UserDO createTestUser(String email) {
        UserDO user = userDomainService.createUser(email, "password123");
        user.setEmailValidated(true);
        userDataService.update(user);
        return user;
    }

    /**
     * 创建测试课程
     */
    private CourseDO createTestCourse(String title, Long creatorId) {
        CourseDO course = new CourseDO();
        course.setName(title);
        course.setDescription("");
        course.setCreatorId(creatorId);
        course.setParentCourseId(0L); // 顶级课程
        course.setRootNodeId(0L); // 暂时设为 0
        course.setMainCategory(1); // 主分类设为 1
        course.setSubCategory(1); // 子分类设为 1
        course.setState(ContentState.PUBLISHED.value());
        courseDataService.insert(course);
        return course;
    }

    /**
     * 创建测试节点
     */
    private NodeDO createTestNode(String name, Long courseId, Long creatorId) {
        NodeDO node = new NodeDO();
        node.setName(name);
        node.setDescription(""); // description 不能为 null
        node.setCourseId(courseId);
        node.setCreatorId(creatorId);
        node.setState(ContentState.PUBLISHED.value());
        nodeDataService.insert(node);
        return node;
    }

    /**
     * 创建测试帖子
     */
    private PostDO createTestPost(Long userId, Long nodeId) {
        PostDO post = new PostDO();
        post.setCreatorId(userId);
        post.setNodeId(nodeId);
        post.setContent("Test content");
        post.setType(PostType.article.value());
        post.setState(ContentState.PUBLISHED.value());
        post.setScore(0.0);
        postDataService.insert(post);
        return post;
    }

    /**
     * 创建测试卡片组
     */
    private MemoryCardDeckDO createTestDeck(Long userId, Long postId, Long nodeId, ContentState state) {
        MemoryCardDeckDO deck = new MemoryCardDeckDO();
        deck.setCreatorId(userId);
        deck.setPostId(postId);
        deck.setNodeId(nodeId);
        deck.setTitle("Test Deck");
        deck.setDescription("Test Description");
        deck.setVersion(1);
        deck.setState(state.value());
        deck.setCardCount(5);
        deckDataService.insert(deck);
        return deck;
    }

    /**
     * 登录用户
     */
    private void loginUser(Long userId) {
        StpUtil.login(userId);
    }

    /**
     * 登出
     */
    private void logout() {
        StpUtil.logout();
    }

    // ==================== 1. 获取帖子公共卡片组接口测试 ====================

    @Test
    @DisplayName("1.1.1 按分数排序获取卡片组列表")
    void testGetPostPublicDecks_SortByScore() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("test1@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        // 创建多个已发布的卡片组
        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        loginUser(user.getId());

        // 执行测试
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.hasMore").exists());

        logout();
    }

    @Test
    @DisplayName("1.1.2 按时间排序获取卡片组列表")
    void testGetPostPublicDecks_SortByCreatedAt() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("test2@example.com");
        NodeDO node = createTestNode("Test Node 2", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        loginUser(user.getId());

        // 执行测试
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isArray());

        logout();
    }

    @Test
    @DisplayName("1.1.3 只返回正常状态的卡片组")
    void testGetPostPublicDecks_OnlyPublishedState() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("test3@example.com");
        NodeDO node = createTestNode("Test Node 3", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        // 创建不同状态的卡片组
        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.SUBMITTED);
        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.REJECTED);

        loginUser(user.getId());

        // 执行测试 - 应该只返回已发布状态的
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items[?(@.state != 2)]").doesNotExist());

        logout();
    }

    @Test
    @DisplayName("1.1.4 未登录用户访问")
    void testGetPostPublicDecks_Unauthorized() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("test4@example.com");
        NodeDO node = createTestNode("Test Node 4", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        // 不登录直接访问 - 现在应该返回未登录错误
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .param("sortBy", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("1.1.5 已登录用户查看点赞状态")
    void testGetPostPublicDecks_WithUpvoteStatus() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("test5@example.com");
        NodeDO node = createTestNode("Test Node 5", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        MemoryCardDeckDO deck = createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        loginUser(user.getId());

        // 现在应该能返回点赞状态了
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items[0].hasUpvoted").exists());

        logout();
    }

    // ==================== 1.2 分页测试 ====================

    @Test
    @DisplayName("1.2.1 按分数分页 - 首次查询")
    void testGetPostPublicDecks_ScorePaginationFirst() throws Exception {
        // 准备测试数据 - 创建超过20个卡片组
        UserDO user = createTestUser("test_page1@example.com");
        NodeDO node = createTestNode("Test Node Page", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        for (int i = 0; i < 25; i++) {
            createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        }

        loginUser(user.getId());

        // 首次查询，不传游标
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(20))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor.lastScore").exists())
                .andExpect(jsonPath("$.data.nextCursor.lastId").exists());

        logout();
    }

    @Test
    @DisplayName("1.2.2 按分数分页 - 使用游标")
    void testGetPostPublicDecks_ScorePaginationWithCursor() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("test_page2@example.com");
        NodeDO node = createTestNode("Test Node Page 2", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        for (int i = 0; i < 25; i++) {
            createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        }

        loginUser(user.getId());

        // 第一次查询
        String response1 = mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // TODO: 解析 lastScore 和 lastId，然后进行第二次查询验证数据不重复

        logout();
    }

    @Test
    @DisplayName("1.2.3 按时间分页 - 首次查询")
    void testGetPostPublicDecks_TimePaginationFirst() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("test_page3@example.com");
        NodeDO node = createTestNode("Test Node Page 3", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        for (int i = 0; i < 25; i++) {
            createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        }

        loginUser(user.getId());

        // 首次查询，不传游标
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items.length()").value(20))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor.lastId").exists());

        logout();
    }

    @Test
    @DisplayName("1.2.4 按时间分页 - 使用游标")
    void testGetPostPublicDecks_TimePaginationWithCursor() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("test_page4@example.com");
        NodeDO node = createTestNode("Test Node Page 4", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        for (int i = 0; i < 25; i++) {
            createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        }

        loginUser(user.getId());

        // TODO: 实现游标分页测试

        logout();
    }

    @Test
    @DisplayName("1.2.5 最后一页数据")
    void testGetPostPublicDecks_LastPage() throws Exception {
        // 准备测试数据 - 恰好25个
        UserDO user = createTestUser("test_page5@example.com");
        NodeDO node = createTestNode("Test Node Page 5", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        for (int i = 0; i < 25; i++) {
            createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        }

        loginUser(user.getId());

        // TODO: 实现最后一页测试

        logout();
    }

    @Test
    @DisplayName("1.2.6 无数据情况")
    void testGetPostPublicDecks_NoData() throws Exception {
        // 准备测试数据 - 帖子存在但没有卡片组
        UserDO user = createTestUser("test_page6@example.com");
        NodeDO node = createTestNode("Test Node Page 6", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.hasMore").value(false));

        logout();
    }

    // ==================== 1.3 DTO 结构测试 ====================

    @Test
    @DisplayName("1.3.1 验证 DeckFullDTO 完整结构")
    void testGetPostPublicDecks_DTOStructure() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("test_dto@example.com");
        CourseDO course = createTestCourse("Test Course", user.getId());
        NodeDO node = createTestNode("Test Node DTO", course.getId(), user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items[0].id").exists())
                .andExpect(jsonPath("$.data.items[0].postId").exists())
                .andExpect(jsonPath("$.data.items[0].nodeId").exists())
                .andExpect(jsonPath("$.data.items[0].courseId").exists())
                .andExpect(jsonPath("$.data.items[0].description").exists())
                .andExpect(jsonPath("$.data.items[0].state").exists())
                .andExpect(jsonPath("$.data.items[0].cardCount").exists())
                .andExpect(jsonPath("$.data.items[0].creator").exists())
                .andExpect(jsonPath("$.data.items[0].creator.id").exists())
                .andExpect(jsonPath("$.data.items[0].course").exists())
                .andExpect(jsonPath("$.data.items[0].node").exists())
                .andExpect(jsonPath("$.data.items[0].hasUpvoted").exists());

        logout();
    }

    // ==================== 1.4 参数校验测试 ====================

    @Test
    @DisplayName("1.4.1 postId 参数缺失")
    void testGetPostPublicDecks_MissingPostId() throws Exception {
        mockMvc.perform(get("/api/v1/memory/posts//decks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("1.4.2 postId 为 0 或负数")
    void testGetPostPublicDecks_InvalidPostId() throws Exception {
        UserDO user = createTestUser("test_invalid_post@example.com");
        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", 0)
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        logout();
    }

    @Test
    @DisplayName("1.4.3 sortBy 参数无效值")
    void testGetPostPublicDecks_InvalidSortBy() throws Exception {
        UserDO user = createTestUser("test_sort@example.com");
        NodeDO node = createTestNode("Test Node Sort", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        loginUser(user.getId());

        // 使用无效的 sortBy 参数，应该使用默认值
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isArray());

        logout();
    }

    @Test
    @DisplayName("1.4.4 lastId 为负数")
    void testGetPostPublicDecks_NegativeLastId() throws Exception {
        UserDO user = createTestUser("test_lastid@example.com");
        NodeDO node = createTestNode("Test Node LastId", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score")
                .param("lastId", "-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        logout();
    }

    @Test
    @DisplayName("1.4.5 lastScore 为负数")
    void testGetPostPublicDecks_NegativeLastScore() throws Exception {
        UserDO user = createTestUser("test_lastscore@example.com");
        NodeDO node = createTestNode("Test Node LastScore", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        loginUser(user.getId());

        // 负分数应该返回空数组
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score")
                .param("lastScore", "-10.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isEmpty());

        logout();
    }

    @Test
    @DisplayName("1.4.6 帖子不存在")
    void testGetPostPublicDecks_PostNotFound() throws Exception {
        UserDO user = createTestUser("test_notfound@example.com");
        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/decks", 999999999L)
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isEmpty());

        logout();
    }

    // ==================== 2. 获取帖子创建者卡片组接口测试 ====================

    @Test
    @DisplayName("2.1.1 非帖子作者查看作者卡片组")
    void testGetPostCreatorDeck_NonAuthorView() throws Exception {
        // 准备测试数据
        UserDO author = createTestUser("author@example.com");
        UserDO viewer = createTestUser("viewer@example.com");
        NodeDO node = createTestNode("Test Node", 1L, author.getId());
        PostDO post = createTestPost(author.getId(), node.getId());

        // 作者创建不同状态的卡片组
        createTestDeck(author.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        createTestDeck(author.getId(), post.getId(), node.getId(), ContentState.SUBMITTED);

        // 登录非作者用户
        loginUser(viewer.getId());

        // 执行测试 - 应该只看到已发布的
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/creator-deck", post.getId())
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items[?(@.state != 2)]").doesNotExist());

        logout();
    }

    @Test
    @DisplayName("2.1.2 帖子作者查看自己的卡片组")
    void testGetPostCreatorDeck_AuthorView() throws Exception {
        // 准备测试数据
        UserDO author = createTestUser("author2@example.com");
        NodeDO node = createTestNode("Test Node 2", 1L, author.getId());
        PostDO post = createTestPost(author.getId(), node.getId());

        // 创建不同状态的卡片组
        createTestDeck(author.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        createTestDeck(author.getId(), post.getId(), node.getId(), ContentState.SUBMITTED);
        createTestDeck(author.getId(), post.getId(), node.getId(), ContentState.REJECTED);

        // 登录作者
        loginUser(author.getId());

        // 执行测试 - 应该看到所有状态
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/creator-deck", post.getId())
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isArray());

        logout();
    }

    @Test
    @DisplayName("2.1.3 帖子作者没有卡片组")
    void testGetPostCreatorDeck_NoDecks() throws Exception {
        // 准备测试数据
        UserDO author = createTestUser("author3@example.com");
        NodeDO node = createTestNode("Test Node 3", 1L, author.getId());
        PostDO post = createTestPost(author.getId(), node.getId());

        loginUser(author.getId());

        // 执行测试 - 作者没有创建卡片组
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/creator-deck", post.getId())
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.hasMore").value(false));

        logout();
    }

    // ==================== 2.2 分页测试 ====================

    @Test
    @DisplayName("2.2.1 固定ID降序分页")
    void testGetPostCreatorDeck_IdPagination() throws Exception {
        // 准备测试数据 - 创建超过20个卡片组
        UserDO author = createTestUser("author_page@example.com");
        NodeDO node = createTestNode("Test Node Page", 1L, author.getId());
        PostDO post = createTestPost(author.getId(), node.getId());

        for (int i = 0; i < 25; i++) {
            createTestDeck(author.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        }

        loginUser(author.getId());

        // 第一次查询
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/creator-deck", post.getId())
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items.length()").value(20))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor.lastId").exists());

        logout();
    }

    @Test
    @DisplayName("2.2.2 最后一页判断")
    void testGetPostCreatorDeck_LastPage() throws Exception {
        // 准备测试数据 - 恰好25个
        UserDO author = createTestUser("author_last@example.com");
        NodeDO node = createTestNode("Test Node Last", 1L, author.getId());
        PostDO post = createTestPost(author.getId(), node.getId());

        for (int i = 0; i < 25; i++) {
            createTestDeck(author.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        }

        loginUser(author.getId());

        // TODO: 实现最后一页测试

        logout();
    }

    // ==================== 2.3 权限测试 ====================

    @Test
    @DisplayName("2.3.1 未登录访问")
    void testGetPostCreatorDeck_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/creator-deck", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("2.3.2 token 过期")
    void testGetPostCreatorDeck_TokenExpired() throws Exception {
        // TODO: 需要生成过期的 token 进行测试
        // 暂时跳过此测试
    }

    // ==================== 2.4 参数校验测试 ====================

    @Test
    @DisplayName("2.4.1 postId 为 0 或负数")
    void testGetPostCreatorDeck_InvalidPostId() throws Exception {
        UserDO user = createTestUser("test_invalid@example.com");
        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/creator-deck", 0)
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        logout();
    }

    @Test
    @DisplayName("2.4.2 lastId 为 0 或负数")
    void testGetPostCreatorDeck_InvalidLastId() throws Exception {
        UserDO user = createTestUser("test_lastid2@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/creator-deck", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("lastId", "-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        logout();
    }

    // ==================== 3. 获取用户卡片组接口测试 ====================

    @Test
    @DisplayName("3.1.1 获取当前用户所有卡片组")
    void testGetCurrentUserDecks_Success() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("user1@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        // 创建不同状态的卡片组
        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.SUBMITTED);

        // 登录用户
        loginUser(user.getId());

        // 执行测试 - 应该看到所有状态
        mockMvc.perform(get("/api/v1/memory/users/me/memory-decks")
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.hasMore").exists());

        logout();
    }

    @Test
    @DisplayName("3.1.2 获取其他用户的卡片组")
    void testGetUserDecks_OtherUser() throws Exception {
        // 准备测试数据
        UserDO owner = createTestUser("owner@example.com");
        UserDO viewer = createTestUser("viewer2@example.com");
        NodeDO node = createTestNode("Test Node", 1L, owner.getId());
        PostDO post = createTestPost(owner.getId(), node.getId());

        // owner创建不同状态的卡片组
        createTestDeck(owner.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        createTestDeck(owner.getId(), post.getId(), node.getId(), ContentState.SUBMITTED);

        // 登录viewer
        loginUser(viewer.getId());

        // 执行测试 - 应该只看到已发布的
        mockMvc.perform(get("/api/v1/memory/users/{userId}/memory-decks", owner.getId())
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        logout();
    }

    @Test
    @DisplayName("3.1.3 用户没有卡片组")
    void testGetUserDecks_NoDecks() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("user_nodecks@example.com");

        loginUser(user.getId());

        // 执行测试
        mockMvc.perform(get("/api/v1/memory/users/me/memory-decks")
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.hasMore").value(false));

        logout();
    }

    // ==================== 3.2 分页测试 ====================

    @Test
    @DisplayName("3.2.1 ID降序分页")
    void testGetUserDecks_IdPagination() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("user_page@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        for (int i = 0; i < 25; i++) {
            createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        }

        loginUser(user.getId());

        // 执行测试
        mockMvc.perform(get("/api/v1/memory/users/me/memory-decks")
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items.length()").value(20))
                .andExpect(jsonPath("$.data.hasMore").value(true));

        logout();
    }

    @Test
    @DisplayName("3.2.2 分页连续性验证")
    void testGetUserDecks_PaginationContinuity() throws Exception {
        // TODO: 实现分页连续性测试
    }

    // ==================== 3.3 权限测试 ====================

    @Test
    @DisplayName("3.3.1 未登录访问")
    void testGetUserDecks_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/memory/users/me/memory-decks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("3.3.2 查看不存在的用户")
    void testGetUserDecks_UserNotFound() throws Exception {
        UserDO user = createTestUser("user_find@example.com");
        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/users/{userId}/memory-decks", 999999999L)
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        logout();
    }

    // ==================== 3.4 参数校验测试 ====================

    @Test
    @DisplayName("3.4.1 userId 为 0 或负数")
    void testGetUserDecks_InvalidUserId() throws Exception {
        UserDO user = createTestUser("user_invalid@example.com");
        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/users/{userId}/memory-decks", 0)
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        logout();
    }

    @Test
    @DisplayName("3.4.2 lastId 为负数")
    void testGetUserDecks_NegativeLastId() throws Exception {
        UserDO user = createTestUser("user_lastid@example.com");
        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/users/me/memory-decks")
                .header("token", StpUtil.getTokenValue())
                .param("lastId", "-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        logout();
    }

    // ==================== 4. 获取我的帖子卡片组接口测试 ====================

    @Test
    @DisplayName("4.1.1 获取自己在指定帖子下的卡片组")
    void testGetMyPostDeck_Success() throws Exception {
        UserDO user = createTestUser("mypost@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.SUBMITTED);

        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/my-deck", post.getId())
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isArray());

        logout();
    }

    @Test
    @DisplayName("4.1.2 按分数排序")
    void testGetMyPostDeck_SortByScore() throws Exception {
        UserDO user = createTestUser("mypost_score@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/my-deck", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isArray());

        logout();
    }

    @Test
    @DisplayName("4.1.3 按时间排序")
    void testGetMyPostDeck_SortByCreatedAt() throws Exception {
        UserDO user = createTestUser("mypost_time@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/my-deck", post.getId())
                .header("token", StpUtil.getTokenValue())
                .param("sortBy", "createdAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isArray());

        logout();
    }

    @Test
    @DisplayName("4.1.4 用户在该帖子下没有卡片组")
    void testGetMyPostDeck_NoDecks() throws Exception {
        UserDO user = createTestUser("mypost_nodecks@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/my-deck", post.getId())
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.hasMore").value(false));

        logout();
    }

    // ==================== 4.2 分页测试 ====================

    @Test
    @DisplayName("4.2.1 动态排序分页")
    void testGetMyPostDeck_DynamicSortPagination() throws Exception {
        // TODO: 实现动态排序分页测试
    }

    // ==================== 4.3 权限测试 ====================

    @Test
    @DisplayName("4.3.1 未登录访问")
    void testGetMyPostDeck_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/memory/posts/{postId}/my-deck", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 4.4 参数校验测试 ====================

    @Test
    @DisplayName("4.4.1 postId 无效")
    void testGetMyPostDeck_InvalidPostId() throws Exception {
        UserDO user = createTestUser("mypost_invalid@example.com");
        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/posts/{postId}/my-deck", 0)
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        logout();
    }

    // ==================== 5. 创建卡片组接口测试 ====================

    @Test
    @DisplayName("5.1.1 成功创建卡片组")
    void testCreateDeck_Success() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("creator@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        loginUser(user.getId());

        // 准备请求体
        CreateDeckRequest.CardInfo card1 = new CreateDeckRequest.CardInfo();
        card1.setFront("What is Java?");
        card1.setBack("A programming language");

        CreateDeckRequest.CardInfo card2 = new CreateDeckRequest.CardInfo();
        card2.setFront("What is Spring?");
        card2.setBack("A framework");

        List<CreateDeckRequest.CardInfo> cards = new ArrayList<>();
        cards.add(card1);
        cards.add(card2);

        CreateDeckRequest request = new CreateDeckRequest();
        request.setSourcePostId(post.getId());
        request.setDescription("Test deck description");
        request.setCards(cards);

        String requestBody = objectMapper.writeValueAsString(request);

        // 执行测试
        mockMvc.perform(post("/api/v1/memory/decks")
                .header("token", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        logout();
    }

    @Test
    @DisplayName("5.2.1 未登录访问")
    void testCreateDeck_Unauthorized() throws Exception {
        String requestBody = """
            {
                "sourcePostId": 1,
                "description": "Test",
                "cards": [
                    {"front": "Q1", "back": "A1"}
                ]
            }
            """;

        mockMvc.perform(post("/api/v1/memory/decks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("5.3.1 sourcePostId 缺失")
    void testCreateDeck_MissingSourcePostId() throws Exception {
        UserDO user = createTestUser("user@example.com");
        loginUser(user.getId());

        String requestBody = """
            {
                "description": "Test",
                "cards": [
                    {"front": "Q1", "back": "A1"}
                ]
            }
            """;

        mockMvc.perform(post("/api/v1/memory/decks")
                .header("token", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        logout();
    }

    // ==================== 6. 更新卡片组接口测试 ====================

    @Test
    @DisplayName("6.1.1 成功更新描述")
    void testUpdateDeck_Success() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("updater@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());
        MemoryCardDeckDO deck = createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        loginUser(user.getId());

        UpdateDeckRequest request = new UpdateDeckRequest();
        request.setDescription("Updated description");

        String requestBody = objectMapper.writeValueAsString(request);

        // 执行测试
        mockMvc.perform(put("/api/v1/memory/decks/{deckId}", deck.getId())
                .header("token", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        logout();
    }

    @Test
    @DisplayName("6.2.2 更新他人的卡片组")
    void testUpdateDeck_Forbidden() throws Exception {
        // 准备测试数据
        UserDO owner = createTestUser("owner2@example.com");
        UserDO other = createTestUser("other@example.com");
        NodeDO node = createTestNode("Test Node", 1L, owner.getId());
        PostDO post = createTestPost(owner.getId(), node.getId());
        MemoryCardDeckDO deck = createTestDeck(owner.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        // 登录其他用户
        loginUser(other.getId());

        String requestBody = """
            {
                "description": "Hacked description"
            }
            """;

        // 执行测试 - 应该返回权限不足
        mockMvc.perform(put("/api/v1/memory/decks/{deckId}", deck.getId())
                .header("token", StpUtil.getTokenValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.PERMISSION_DENIED.getCode()));

        logout();
    }

    // ==================== 7. 删除卡片组接口测试 ====================

    @Test
    @DisplayName("7.1.1 成功删除卡片组")
    void testDeleteDeck_Success() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("deleter@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());
        MemoryCardDeckDO deck = createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        loginUser(user.getId());

        // 执行测试
        mockMvc.perform(delete("/api/v1/memory/decks/{id}", deck.getId())
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        // 验证已删除
        MemoryCardDeckDO deleted = deckDataService.getById(deck.getId());
        assertThat(deleted).isNull();

        logout();
    }

    @Test
    @DisplayName("7.2.2 删除他人的卡片组")
    void testDeleteDeck_Forbidden() throws Exception {
        // 准备测试数据
        UserDO owner = createTestUser("owner3@example.com");
        UserDO other = createTestUser("other2@example.com");
        NodeDO node = createTestNode("Test Node", 1L, owner.getId());
        PostDO post = createTestPost(owner.getId(), node.getId());
        MemoryCardDeckDO deck = createTestDeck(owner.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        // 登录其他用户
        loginUser(other.getId());

        // 执行测试 - 应该返回权限不足
        mockMvc.perform(delete("/api/v1/memory/decks/{id}", deck.getId())
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.PERMISSION_DENIED.getCode()));

        logout();
    }

    // ==================== 8. 获取卡片组详情接口测试 ====================

    @Test
    @DisplayName("8.1.1 成功获取卡片组详情")
    void testGetDeckDetail_Success() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("detail@example.com");
        NodeDO node = createTestNode("Test Node", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());
        MemoryCardDeckDO deck = createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        loginUser(user.getId());

        // 执行测试
        mockMvc.perform(get("/api/v1/memory/decks/{deckId}", deck.getId())
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.id").value(deck.getId()))
                .andExpect(jsonPath("$.data.creator").exists())
                .andExpect(jsonPath("$.data.cards").isArray());

        logout();
    }

    @Test
    @DisplayName("8.2.1 未登录访问")
    void testGetDeckDetail_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/memory/decks/{deckId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    @Test
    @DisplayName("8.3.2 deckId 不存在")
    void testGetDeckDetail_NotFound() throws Exception {
        UserDO user = createTestUser("user3@example.com");
        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/decks/{deckId}", 999999999L)
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.MEMORY_CARD_DECK_NOT_FOUND.getCode()));

        logout();
    }

    // ==================== 9. 获取节点下的卡片组接口测试 ====================

    @Test
    @DisplayName("9.1.1 按分数排序获取节点卡片组")
    void testGetDecksByNode_Success() throws Exception {
        // 准备测试数据
        UserDO user = createTestUser("node@example.com");
        NodeDO node = createTestNode("Test Node for Decks", 1L, user.getId());
        PostDO post = createTestPost(user.getId(), node.getId());

        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);
        createTestDeck(user.getId(), post.getId(), node.getId(), ContentState.PUBLISHED);

        loginUser(user.getId());

        // 执行测试
        mockMvc.perform(get("/api/v1/memory/decks/node/{nodeId}", node.getId())
                .header("token", StpUtil.getTokenValue())
                .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.hasMore").exists());

        logout();
    }

    @Test
    @DisplayName("9.1.3 limit 超过最大值")
    void testGetDecksByNode_LimitExceedsMax() throws Exception {
        UserDO user = createTestUser("node2@example.com");
        NodeDO node = createTestNode("Test Node 2", 1L, user.getId());

        loginUser(user.getId());

        // 执行测试 - limit=100 应该被限制为 50
        mockMvc.perform(get("/api/v1/memory/decks/node/{nodeId}", node.getId())
                .header("token", StpUtil.getTokenValue())
                .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        logout();
    }

    @Test
    @DisplayName("9.4.1 nodeId 无效")
    void testGetDecksByNode_InvalidNodeId() throws Exception {
        UserDO user = createTestUser("node3@example.com");
        loginUser(user.getId());

        mockMvc.perform(get("/api/v1/memory/decks/node/{nodeId}", 0)
                .header("token", StpUtil.getTokenValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        logout();
    }
}
