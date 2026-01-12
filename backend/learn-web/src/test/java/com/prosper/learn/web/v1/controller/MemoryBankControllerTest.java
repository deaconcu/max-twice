package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.content.post.PostDO;
import com.prosper.learn.content.post.PostDataService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.memory.review.UserCardInCourseDataService;
import com.prosper.learn.memory.review.UserCardSrsDO;
import com.prosper.learn.memory.review.UserCourseSrsSettingDO;
import com.prosper.learn.memory.review.UserCourseSrsSettingDataService;
import com.prosper.learn.shared.domain.Enums.ContentState;
import com.prosper.learn.shared.domain.Enums.PostType;
import com.prosper.learn.shared.domain.exception.StatusCode;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import com.prosper.learn.user.profile.UserDomainService;
import org.junit.jupiter.api.AfterEach;
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
 * 记忆库管理接口测试
 * 测试文档: docs/test/memory-bank.md
 */
@Transactional
public class MemoryBankControllerTest extends BaseControllerTest {

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
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private PostDataService postDataService;

    @Autowired
    private MemoryCardDeckDataService deckDataService;

    @Autowired
    private MemoryCardDataService cardDataService;

    @Autowired
    private UserCardInCourseDataService userCardInCourseDataService;

    @Autowired
    private UserCourseSrsSettingDataService srsSettingDataService;

    @Autowired
    private com.prosper.learn.memory.card.MemoryCardDomainService cardDomainService;

    @Autowired
    private com.prosper.learn.memory.review.UserCardSrsDataService userCardSrsDataService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @AfterEach
    void tearDown() {
        StpUtil.logout();
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
     * 创建已发布节点
     */
    private NodeDO createPublishedNode(String name, Long courseId, Long creatorId) {
        NodeDO node = new NodeDO();
        node.setName(name);
        node.setDescription("");
        node.setCourseId(courseId);
        node.setCreatorId(creatorId);
        node.setState(ContentState.PUBLISHED.value());
        nodeDataService.insert(node);
        return node;
    }

    /**
     * 创建帖子
     */
    private PostDO createPost(String content, Long nodeId, Long creatorId) {
        PostDO post = new PostDO();
        post.setContent(content);
        post.setNodeId(nodeId);
        post.setCreatorId(creatorId);
        post.setType(PostType.article.value());
        post.setState(ContentState.PUBLISHED.value());
        postDataService.insert(post);
        return post;
    }

    /**
     * 创建卡片组
     */
    private MemoryCardDeckDO createDeck(String title, Long nodeId, Long creatorId) {
        // 先创建帖子
        PostDO post = createPost("卡片组内容", nodeId, creatorId);

        MemoryCardDeckDO deck = new MemoryCardDeckDO();
        deck.setTitle(title);
        deck.setDescription("卡片组描述");
        deck.setPostId(post.getId());
        deck.setNodeId(nodeId);
        deck.setCreatorId(creatorId);
        deck.setState(ContentState.PUBLISHED.value());
        deck.setCardCount(0);
        deck.setVersion(1); // 设置初始版本号
        deckDataService.insert(deck);
        return deck;
    }

    /**
     * 为卡片组创建卡片
     */
    private void createCardsForDeck(Long deckId, int count) {
        MemoryCardDeckDO deck = deckDataService.getById(deckId);
        Long creatorId = deck.getCreatorId();

        for (int i = 0; i < count; i++) {
            cardDomainService.createCard(creatorId, deckId, "问题 " + (i + 1), "答案 " + (i + 1));
        }

        // 创建卡片后，deck状态会变为SUBMITTED，需要改回PUBLISHED才能添加到记忆库
        deck = deckDataService.getById(deckId);
        deck.setState(ContentState.PUBLISHED.value());
        deckDataService.update(deck);
    }

    /**
     * 验证SRS设置存在
     */
    private boolean srsSettingExists(Long userId, Long courseId) {
        UserCourseSrsSettingDO setting = srsSettingDataService.getByUserAndCourse(userId, courseId);
        return setting != null;
    }

    // ==================== 接口1: 添加卡片组到记忆库 ====================

    /**
     * 测试 1.1.1: 首次添加卡片组成功
     */
    @Test
    @DisplayName("添加卡片组 - 首次添加成功")
    void testAddDeckToMemoryBank_Success() throws Exception {
        // 准备测试数据
        UserDO user1 = createUser("test-memory-1@test.com");
        CourseDO course1 = createPublishedCourse("测试课程1", user1.getId());
        NodeDO node1 = createPublishedNode("测试节点1", course1.getId(), user1.getId());
        MemoryCardDeckDO deck1 = createDeck("测试卡片组1", node1.getId(), user1.getId());
        createCardsForDeck(deck1.getId(), 5);

        StpUtil.login(user1.getId());

        try {
            String requestBody = String.format(
                    "{\"deckId\": %d, \"courseId\": %d}",
                    deck1.getId(), course1.getId()
            );

            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").doesNotExist());

            // 验证数据库
            assertThat(srsSettingExists(user1.getId(), course1.getId())).isTrue();

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.1.2: 重复添加相同卡片组
     */
    @Test
    @DisplayName("添加卡片组 - 重复添加相同卡片组")
    void testAddDeckToMemoryBank_Duplicate() throws Exception {
        UserDO user1 = createUser("test-memory-2@test.com");
        CourseDO course1 = createPublishedCourse("测试课程2", user1.getId());
        NodeDO node1 = createPublishedNode("测试节点2", course1.getId(), user1.getId());
        MemoryCardDeckDO deck1 = createDeck("测试卡片组2", node1.getId(), user1.getId());
        createCardsForDeck(deck1.getId(), 5);

        StpUtil.login(user1.getId());

        try {
            String requestBody = String.format(
                    "{\"deckId\": %d, \"courseId\": %d}",
                    deck1.getId(), course1.getId()
            );

            // 第一次添加
            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 第二次添加（重复）
            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
            // 注意：重复添加使用INSERT IGNORE，操作仍成功，但不会重复插入

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.1.3: 向已有课程添加新卡片组
     */
    @Test
    @DisplayName("添加卡片组 - 向已有课程添加新卡片组")
    void testAddDeckToMemoryBank_AddSecondDeck() throws Exception {
        UserDO user1 = createUser("test-memory-3@test.com");
        CourseDO course1 = createPublishedCourse("测试课程3", user1.getId());
        NodeDO node1 = createPublishedNode("测试节点3", course1.getId(), user1.getId());
        MemoryCardDeckDO deck1 = createDeck("测试卡片组3-1", node1.getId(), user1.getId());
        MemoryCardDeckDO deck2 = createDeck("测试卡片组3-2", node1.getId(), user1.getId());
        createCardsForDeck(deck1.getId(), 5);
        createCardsForDeck(deck2.getId(), 3);

        StpUtil.login(user1.getId());

        try {
            // 添加第一个卡片组
            String requestBody1 = String.format(
                    "{\"deckId\": %d, \"courseId\": %d}",
                    deck1.getId(), course1.getId()
            );
            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 添加第二个卡片组
            String requestBody2 = String.format(
                    "{\"deckId\": %d, \"courseId\": %d}",
                    deck2.getId(), course1.getId()
            );
            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.2.1: deckId 为 null
     */
    @Test
    @DisplayName("添加卡片组 - deckId 为 null")
    void testAddDeckToMemoryBank_DeckIdNull() throws Exception {
        UserDO user1 = createUser("test-memory-4@test.com");
        CourseDO course1 = createPublishedCourse("测试课程4", user1.getId());

        StpUtil.login(user1.getId());

        try {
            String requestBody = String.format("{\"courseId\": %d}", course1.getId());

            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
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
     * 测试 1.2.2: deckId 为 0
     */
    @Test
    @DisplayName("添加卡片组 - deckId 为 0")
    void testAddDeckToMemoryBank_DeckIdZero() throws Exception {
        UserDO user1 = createUser("test-memory-5@test.com");
        CourseDO course1 = createPublishedCourse("测试课程5", user1.getId());

        StpUtil.login(user1.getId());

        try {
            String requestBody = String.format("{\"deckId\": 0, \"courseId\": %d}", course1.getId());

            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
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
     * 测试 1.2.3: deckId 为负数
     */
    @Test
    @DisplayName("添加卡片组 - deckId 为负数")
    void testAddDeckToMemoryBank_DeckIdNegative() throws Exception {
        UserDO user1 = createUser("test-memory-6@test.com");
        CourseDO course1 = createPublishedCourse("测试课程6", user1.getId());

        StpUtil.login(user1.getId());

        try {
            String requestBody = String.format("{\"deckId\": -1, \"courseId\": %d}", course1.getId());

            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
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
     * 测试 1.2.4: courseId 为 null
     */
    @Test
    @DisplayName("添加卡片组 - courseId 为 null")
    void testAddDeckToMemoryBank_CourseIdNull() throws Exception {
        UserDO user1 = createUser("test-memory-7@test.com");

        StpUtil.login(user1.getId());

        try {
            String requestBody = "{\"deckId\": 1}";

            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
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
     * 测试 1.2.5: courseId 为 0
     */
    @Test
    @DisplayName("添加卡片组 - courseId 为 0")
    void testAddDeckToMemoryBank_CourseIdZero() throws Exception {
        UserDO user1 = createUser("test-memory-8@test.com");

        StpUtil.login(user1.getId());

        try {
            String requestBody = "{\"deckId\": 1, \"courseId\": 0}";

            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
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
     * 测试 1.2.6: courseId 为负数
     */
    @Test
    @DisplayName("添加卡片组 - courseId 为负数")
    void testAddDeckToMemoryBank_CourseIdNegative() throws Exception {
        UserDO user1 = createUser("test-memory-9@test.com");

        StpUtil.login(user1.getId());

        try {
            String requestBody = "{\"deckId\": 1, \"courseId\": -1}";

            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
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
     * 测试 1.3.1: 卡片组不存在
     */
    @Test
    @DisplayName("添加卡片组 - 卡片组不存在")
    void testAddDeckToMemoryBank_DeckNotFound() throws Exception {
        UserDO user1 = createUser("test-memory-10@test.com");
        CourseDO course1 = createPublishedCourse("测试课程10", user1.getId());

        StpUtil.login(user1.getId());

        try {
            String requestBody = String.format("{\"deckId\": 99999, \"courseId\": %d}", course1.getId());

            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.MEMORY_CARD_DECK_NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.3.2: 课程不存在
     */
    @Test
    @DisplayName("添加卡片组 - 课程不存在")
    void testAddDeckToMemoryBank_CourseNotFound() throws Exception {
        UserDO user1 = createUser("test-memory-11@test.com");
        CourseDO course1 = createPublishedCourse("测试课程11", user1.getId());
        NodeDO node1 = createPublishedNode("测试节点11", course1.getId(), user1.getId());
        MemoryCardDeckDO deck1 = createDeck("测试卡片组11", node1.getId(), user1.getId());

        StpUtil.login(user1.getId());

        try {
            String requestBody = String.format("{\"deckId\": %d, \"courseId\": 99999}", deck1.getId());

            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.COURSE_NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 1.3.3: 未登录尝试添加
     */
    @Test
    @DisplayName("添加卡片组 - 未登录")
    void testAddDeckToMemoryBank_NotLogin() throws Exception {
        UserDO user1 = createUser("test-memory-12@test.com");
        CourseDO course1 = createPublishedCourse("测试课程12", user1.getId());

        String requestBody = String.format("{\"deckId\": 1, \"courseId\": %d}", course1.getId());

        mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试 1.3.4: 节点下卡片数量超出限制
     */
    @Test
    @DisplayName("添加卡片组 - 节点下卡片数量超出限制")
    void testAddDeckToMemoryBank_NodeCardLimitExceeded() throws Exception {
        UserDO user1 = createUser("test-memory-12-1@test.com");
        CourseDO course1 = createPublishedCourse("测试课程12-1", user1.getId());
        NodeDO node1 = createPublishedNode("测试节点12-1", course1.getId(), user1.getId());

        // 创建第一个卡片组，包含150张卡片
        MemoryCardDeckDO deck1 = createDeck("测试卡片组12-1", node1.getId(), user1.getId());
        createCardsForDeck(deck1.getId(), 150);

        // 验证deck1确实有150张卡片
        List<MemoryCardDO> deck1Cards = cardDataService.getByDeckId(deck1.getId());
        assertThat(deck1Cards).hasSize(150);

        // 创建第二个卡片组，包含60张卡片
        MemoryCardDeckDO deck2 = createDeck("测试卡片组12-2", node1.getId(), user1.getId());
        createCardsForDeck(deck2.getId(), 60);

        // 验证deck2确实有60张卡片
        List<MemoryCardDO> deck2Cards = cardDataService.getByDeckId(deck2.getId());
        assertThat(deck2Cards).hasSize(60);

        StpUtil.login(user1.getId());

        try {
            // 先添加第一个卡片组（150张）
            String requestBody1 = String.format(
                    "{\"deckId\": %d, \"courseId\": %d}",
                    deck1.getId(), course1.getId()
            );
            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证第一次添加后，node1下有150张卡片
            List<UserCardSrsDO> existingCards =
                userCardSrsDataService.getByUserAndNodeId(user1.getId(), node1.getId());
            assertThat(existingCards).hasSize(150);

            // 尝试添加第二个卡片组（60张），总数210张，超过200张限制
            String requestBody2 = String.format(
                    "{\"deckId\": %d, \"courseId\": %d}",
                    deck2.getId(), course1.getId()
            );
            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.NODE_CARD_LIMIT_EXCEEDED.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    // ==================== 接口2: 获取记忆库课程列表 ====================

    /**
     * 测试 2.1.1: 获取空记忆库
     */
    @Test
    @DisplayName("获取记忆库课程 - 空记忆库")
    void testGetMemoryBankCourses_Empty() throws Exception {
        UserDO user1 = createUser("test-memory-13@test.com");

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/memory-bank/courses")
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
     * 测试 2.1.2: 获取有数据的记忆库
     */
    @Test
    @DisplayName("获取记忆库课程 - 有数据")
    void testGetMemoryBankCourses_WithData() throws Exception {
        UserDO user1 = createUser("test-memory-14@test.com");
        CourseDO course1 = createPublishedCourse("测试课程14", user1.getId());
        NodeDO node1 = createPublishedNode("测试节点14", course1.getId(), user1.getId());
        MemoryCardDeckDO deck1 = createDeck("测试卡片组14", node1.getId(), user1.getId());
        createCardsForDeck(deck1.getId(), 10);

        StpUtil.login(user1.getId());

        try {
            // 先添加卡片组到记忆库
            String addRequestBody = String.format(
                    "{\"deckId\": %d, \"courseId\": %d}",
                    deck1.getId(), course1.getId()
            );
            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 获取记忆库课程列表
            mockMvc.perform(get("/api/v1/memory/memory-bank/courses")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].course.id").value(course1.getId()))
                    .andExpect(jsonPath("$.data[0].course.name").value(course1.getName()))
                    .andExpect(jsonPath("$.data[0].setting").exists())
                    .andExpect(jsonPath("$.data[0].cardCount").isNumber());

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 2.2.1: status 为负数
     */
    @Test
    @DisplayName("获取记忆库课程 - status 为负数")
    void testGetMemoryBankCourses_StatusNegative() throws Exception {
        UserDO user1 = createUser("test-memory-15@test.com");

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/memory-bank/courses?status=-1")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 2.3.1: 未登录尝试获取
     */
    @Test
    @DisplayName("获取记忆库课程 - 未登录")
    void testGetMemoryBankCourses_NotLogin() throws Exception {
        mockMvc.perform(get("/api/v1/memory/memory-bank/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 接口3: 更新课程复习策略 ====================

    /**
     * 测试 3.1.1: 更新每日新卡数量
     */
    @Test
    @DisplayName("更新课程设置 - 更新每日新卡数量")
    void testUpdateCourseSetting_UpdateFrequency() throws Exception {
        UserDO user1 = createUser("test-memory-16@test.com");
        CourseDO course1 = createPublishedCourse("测试课程16", user1.getId());
        NodeDO node1 = createPublishedNode("测试节点16", course1.getId(), user1.getId());
        MemoryCardDeckDO deck1 = createDeck("测试卡片组16", node1.getId(), user1.getId());
        createCardsForDeck(deck1.getId(), 10);

        StpUtil.login(user1.getId());

        try {
            // 先添加卡片组到记忆库
            String addRequestBody = String.format(
                    "{\"deckId\": %d, \"courseId\": %d}",
                    deck1.getId(), course1.getId()
            );
            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 更新设置
            String updateRequestBody = String.format(
                    "{\"courseId\": %d, \"frequencySetting\": 30}",
                    course1.getId()
            );
            mockMvc.perform(put("/api/v1/memory/memory-bank/courses/" + course1.getId() + "/settings")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.message").value("操作成功"));

            // 验证数据库
            UserCourseSrsSettingDO setting = srsSettingDataService.getByUserAndCourse(user1.getId(), course1.getId());
            assertThat(setting).isNotNull();
            assertThat(setting.getFrequencySetting()).isEqualTo((byte) 30);

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.1.2: 更新学习状态为暂停
     */
    @Test
    @DisplayName("更新课程设置 - 更新学习状态为暂停")
    void testUpdateCourseSetting_UpdateStatusToPause() throws Exception {
        UserDO user1 = createUser("test-memory-17@test.com");
        CourseDO course1 = createPublishedCourse("测试课程17", user1.getId());
        NodeDO node1 = createPublishedNode("测试节点17", course1.getId(), user1.getId());
        MemoryCardDeckDO deck1 = createDeck("测试卡片组17", node1.getId(), user1.getId());
        createCardsForDeck(deck1.getId(), 10);

        StpUtil.login(user1.getId());

        try {
            // 先添加卡片组到记忆库
            String addRequestBody = String.format(
                    "{\"deckId\": %d, \"courseId\": %d}",
                    deck1.getId(), course1.getId()
            );
            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 更新状态为暂停
            String updateRequestBody = String.format(
                    "{\"courseId\": %d, \"status\": 0}",
                    course1.getId()
            );
            mockMvc.perform(put("/api/v1/memory/memory-bank/courses/" + course1.getId() + "/settings")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证数据库
            UserCourseSrsSettingDO setting = srsSettingDataService.getByUserAndCourse(user1.getId(), course1.getId());
            assertThat(setting).isNotNull();
            assertThat(setting.getState()).isEqualTo((byte) 0);

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.1.3: 同时更新频率和状态
     */
    @Test
    @DisplayName("更新课程设置 - 同时更新频率和状态")
    void testUpdateCourseSetting_UpdateBoth() throws Exception {
        UserDO user1 = createUser("test-memory-18@test.com");
        CourseDO course1 = createPublishedCourse("测试课程18", user1.getId());
        NodeDO node1 = createPublishedNode("测试节点18", course1.getId(), user1.getId());
        MemoryCardDeckDO deck1 = createDeck("测试卡片组18", node1.getId(), user1.getId());
        createCardsForDeck(deck1.getId(), 10);

        StpUtil.login(user1.getId());

        try {
            // 先添加卡片组到记忆库
            String addRequestBody = String.format(
                    "{\"deckId\": %d, \"courseId\": %d}",
                    deck1.getId(), course1.getId()
            );
            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 同时更新两个字段
            String updateRequestBody = String.format(
                    "{\"courseId\": %d, \"frequencySetting\": 25, \"status\": 1}",
                    course1.getId()
            );
            mockMvc.perform(put("/api/v1/memory/memory-bank/courses/" + course1.getId() + "/settings")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 验证数据库
            UserCourseSrsSettingDO setting = srsSettingDataService.getByUserAndCourse(user1.getId(), course1.getId());
            assertThat(setting).isNotNull();
            assertThat(setting.getFrequencySetting()).isEqualTo((byte) 25);
            assertThat(setting.getState()).isEqualTo((byte) 1);

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.2.1: 路径 courseId 为 0
     */
    @Test
    @DisplayName("更新课程设置 - 路径 courseId 为 0")
    void testUpdateCourseSetting_CourseIdZero() throws Exception {
        UserDO user1 = createUser("test-memory-19@test.com");

        StpUtil.login(user1.getId());

        try {
            String updateRequestBody = "{\"courseId\": 0, \"frequencySetting\": 20}";

            mockMvc.perform(put("/api/v1/memory/memory-bank/courses/0/settings")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.2.2: 路径 courseId 为负数
     */
    @Test
    @DisplayName("更新课程设置 - 路径 courseId 为负数")
    void testUpdateCourseSetting_CourseIdNegative() throws Exception {
        UserDO user1 = createUser("test-memory-20@test.com");

        StpUtil.login(user1.getId());

        try {
            String updateRequestBody = "{\"courseId\": -1, \"frequencySetting\": 20}";

            mockMvc.perform(put("/api/v1/memory/memory-bank/courses/-1/settings")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.3.1: 课程不存在
     */
    @Test
    @DisplayName("更新课程设置 - 课程不存在")
    void testUpdateCourseSetting_CourseNotFound() throws Exception {
        UserDO user1 = createUser("test-memory-21@test.com");

        StpUtil.login(user1.getId());

        try {
            String updateRequestBody = "{\"courseId\": 99999, \"frequencySetting\": 20}";

            mockMvc.perform(put("/api/v1/memory/memory-bank/courses/99999/settings")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.COURSE_NOT_FOUND.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 3.3.2: 未登录尝试更新
     */
    @Test
    @DisplayName("更新课程设置 - 未登录")
    void testUpdateCourseSetting_NotLogin() throws Exception {
        String updateRequestBody = "{\"courseId\": 1, \"frequencySetting\": 20}";

        mockMvc.perform(put("/api/v1/memory/memory-bank/courses/1/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 接口4: 移除卡片组 ====================

    /**
     * 测试 4.1.1: 成功移除卡片组
     */
    @Test
    @DisplayName("移除卡片组 - 成功移除")
    void testRemoveDeck_Success() throws Exception {
        UserDO user1 = createUser("test-memory-22@test.com");
        CourseDO course1 = createPublishedCourse("测试课程22", user1.getId());
        NodeDO node1 = createPublishedNode("测试节点22", course1.getId(), user1.getId());
        MemoryCardDeckDO deck1 = createDeck("测试卡片组22", node1.getId(), user1.getId());
        createCardsForDeck(deck1.getId(), 5);

        StpUtil.login(user1.getId());

        try {
            // 先添加卡片组到记忆库
            String addRequestBody = String.format(
                    "{\"deckId\": %d, \"courseId\": %d}",
                    deck1.getId(), course1.getId()
            );
            mockMvc.perform(post("/api/v1/memory/memory-bank/decks")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addRequestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));

            // 移除卡片组
            mockMvc.perform(delete("/api/v1/memory/memory-bank/courses/" + course1.getId() + "/decks/" + deck1.getId())
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.message").value("操作成功"));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 4.2.1: courseId 为 0
     */
    @Test
    @DisplayName("移除卡片组 - courseId 为 0")
    void testRemoveDeck_CourseIdZero() throws Exception {
        UserDO user1 = createUser("test-memory-23@test.com");

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(delete("/api/v1/memory/memory-bank/courses/0/decks/1")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 4.2.2: courseId 为负数
     */
    @Test
    @DisplayName("移除卡片组 - courseId 为负数")
    void testRemoveDeck_CourseIdNegative() throws Exception {
        UserDO user1 = createUser("test-memory-24@test.com");

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(delete("/api/v1/memory/memory-bank/courses/-1/decks/1")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 4.2.3: deckId 为 0
     */
    @Test
    @DisplayName("移除卡片组 - deckId 为 0")
    void testRemoveDeck_DeckIdZero() throws Exception {
        UserDO user1 = createUser("test-memory-25@test.com");

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(delete("/api/v1/memory/memory-bank/courses/1/decks/0")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 4.2.4: deckId 为负数
     */
    @Test
    @DisplayName("移除卡片组 - deckId 为负数")
    void testRemoveDeck_DeckIdNegative() throws Exception {
        UserDO user1 = createUser("test-memory-26@test.com");

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(delete("/api/v1/memory/memory-bank/courses/1/decks/-1")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));

        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试 4.3.1: 未登录尝试移除
     */
    @Test
    @DisplayName("移除卡片组 - 未登录")
    void testRemoveDeck_NotLogin() throws Exception {
        mockMvc.perform(delete("/api/v1/memory/memory-bank/courses/1/decks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 5. 并发和分布式锁测试 ====================

    /**
     * 测试场景 5.1：测试分布式锁防止并发导致的数量限制失效
     *
     * 说明：虽然这是集成测试，无法真正测试并发（单线程），
     * 但可以验证分布式锁的基本功能：同一用户对同一节点的串行操作
     */
    @Test
    @DisplayName("分布式锁验证 - 添加卡片组成功获取和释放锁")
    void testDistributedLock_AddDeck() throws Exception {
        // 准备测试数据
        UserDO user = createUser("test-lock@test.com");
        CourseDO course = createCourse("Test Course", user.getId());
        NodeDO node = createNode("Test Node", course.getId(), user.getId());
        MemoryCardDeckDO deck = createDeck("Test Deck", "Description", course.getId(), node.getId(), user.getId());

        // 创建 10 张卡片
        for (int i = 0; i < 10; i++) {
            createCard("Front " + i, "Back " + i, deck.getId(), user.getId());
        }

        // 登录用户
        StpUtil.login(user.getId());

        try {
            // 第一次添加卡片组（应该成功）
            mockMvc.perform(post("/api/v1/memory/memory-bank/courses/" + course.getId() + "/decks/" + deck.getId())
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // 验证：卡片已添加到记忆库
            int cardCount = userCardSrsDataService.getByUserAndNodeId(user.getId(), node.getId()).size();
            assertThat(cardCount).isEqualTo(10);

            // 再次添加相同的卡片组（因为使用 INSERT IGNORE，应该成功但不增加卡片）
            mockMvc.perform(post("/api/v1/memory/memory-bank/courses/" + course.getId() + "/decks/" + deck.getId())
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // 验证：卡片数量没有增加（幂等性）
            int cardCount2 = userCardSrsDataService.getByUserAndNodeId(user.getId(), node.getId()).size();
            assertThat(cardCount2).isEqualTo(10);

        } finally {
            StpUtil.logout();
        }
    }
}
