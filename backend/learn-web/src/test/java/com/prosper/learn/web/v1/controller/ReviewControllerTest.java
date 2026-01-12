package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.card.MemoryCardDomainService;
import com.prosper.learn.memory.card.MemoryCardVersionDataService;
import com.prosper.learn.memory.card.MemoryCardVersionDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDataService;
import com.prosper.learn.memory.deck.MemoryCardDeckDO;
import com.prosper.learn.memory.deck.MemoryCardDeckDomainService;
import com.prosper.learn.memory.review.UserCardInCourseDataService;
import com.prosper.learn.memory.review.UserCardInCourseDO;
import com.prosper.learn.memory.review.UserCardSrsDataService;
import com.prosper.learn.memory.review.UserCardSrsDO;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.shared.domain.Enums;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 复习功能接口测试
 * 测试文档: docs/test/review.md
 */
@Transactional
public class ReviewControllerTest extends BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private MemoryCardDeckDataService deckDataService;

    @Autowired
    private MemoryCardDataService cardDataService;

    @Autowired
    private MemoryCardVersionDataService cardVersionDataService;

    @Autowired
    private UserCardSrsDataService userCardSrsDataService;

    @Autowired
    private MemoryCardDomainService cardDomainService;

    @Autowired
    private MemoryCardDeckDomainService deckDomainService;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private NodeDataService nodeDataService;

    @Autowired
    private UserCardInCourseDataService userCardInCourseDataService;

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
     * 创建测试课程
     */
    private CourseDO createCourse(String name, Long creatorId) {
        CourseDO course = new CourseDO();
        course.setName(name);
        course.setDescription("");
        course.setCreatorId(creatorId);
        course.setParentCourseId(0L);
        course.setRootNodeId(0L);
        course.setMainCategory(1);
        course.setSubCategory(1);
        course.setState(Enums.ContentState.PUBLISHED.value());
        courseDataService.insert(course);
        return course;
    }

    /**
     * 创建测试节点
     */
    private NodeDO createNode(String name, Long courseId, Long creatorId) {
        NodeDO node = new NodeDO();
        node.setName(name);
        node.setDescription("");
        node.setCourseId(courseId);
        node.setCreatorId(creatorId);
        node.setState(Enums.ContentState.PUBLISHED.value());
        nodeDataService.insert(node);
        return node;
    }

    /**
     * 创建测试卡片组
     */
    private MemoryCardDeckDO createDeck(String title, String description, Long courseId, Long nodeId, Long creatorId) {
        // 使用 DomainService 创建 deck (默认状态是 SUBMITTED)
        MemoryCardDeckDO deck = deckDomainService.createDeck(creatorId, 1L, nodeId, description, 0);

        // 手动更新状态为 PUBLISHED,以便查询时能找到
        deck.setState(Enums.ContentState.PUBLISHED.value());
        deck.setTitle(title);
        deck.setReason(""); // reason 字段不能为 null
        deck.setScore(0.0); // score 字段不能为 null
        deckDataService.update(deck);

        return deckDataService.getById(deck.getId());
    }

    /**
     * 创建测试卡片（使用 DomainService）
     */
    private MemoryCardDO createCard(String front, String back, Long deckId, Long creatorId) {
        MemoryCardDO card = cardDomainService.createCard(creatorId, deckId, front, back);

        // 创建卡片后,DomainService 会把 deck 的 state 改回 SUBMITTED,需要再次更新为 PUBLISHED
        MemoryCardDeckDO deck = deckDataService.getById(deckId);
        deck.setState(Enums.ContentState.PUBLISHED.value());
        deckDataService.update(deck);

        return card;
    }

    /**
     * 创建用户卡片 SRS 状态
     */
    private UserCardSrsDO createSrsState(Long userId, Long cardId, Long deckId, Long nodeId,
                                         byte type, LocalDateTime reviewDueAt) {
        // 获取卡片和deck信息以获取真实的版本ID
        MemoryCardDO card = cardDataService.getById(cardId);
        MemoryCardDeckDO deck = deckDataService.getById(deckId);

        UserCardSrsDO srs = new UserCardSrsDO();
        srs.setUserId(userId);
        srs.setCardId(cardId);
        srs.setDeckId(deckId);
        srs.setNodeId(nodeId);
        srs.setType(type);
        srs.setCurrentStep((byte) 0);
        srs.setInterval(1);
        srs.setDeckVersion(deck.getVersion());  // 使用真实的 deck version
        srs.setCardVersionId(card.getCurrentVersionId());  // 使用真实的 card version ID
        srs.setReviewDueAt(reviewDueAt);
        srs.setLastReviewedAt(LocalDateTime.now().minusDays(1));
        srs.setEaseFactor(BigDecimal.valueOf(2.5));
        srs.setRepetitions(0);
        srs.setLapseCount(0);
        userCardSrsDataService.insert(srs);
        return srs;
    }

    /**
     * 创建用户卡片与课程的关联
     */
    private void createUserCardInCourse(Long userId, Long cardId, Long deckId, Long courseId) {
        UserCardInCourseDO relation = new UserCardInCourseDO();
        relation.setUserId(userId);
        relation.setCardId(cardId);
        relation.setDeckId(deckId);
        relation.setCourseId(courseId);
        userCardInCourseDataService.insert(relation);
    }

    // ==================== 1. 获取复习队列接口测试 ====================

    /**
     * 测试场景 1.1.1：获取到期的卡片（有数据）
     */
    @Test
    @DisplayName("获取复习队列 - 有到期卡片")
    void testGetReviewQueue_WithDueCards() throws Exception {
        // 准备测试数据
        UserDO user1 = createUser("test-queue-1@test.com");
        MemoryCardDeckDO deck1 = createDeck("Java 基础", "Java 核心知识", 100L, 10L, user1.getId());

        MemoryCardDO card1 = createCard("什么是JVM？", "Java虚拟机", deck1.getId(), user1.getId());
        MemoryCardDO card2 = createCard("什么是GC？", "垃圾回收", deck1.getId(), user1.getId());
        MemoryCardDO card3 = createCard("什么是多态？", "面向对象特性", deck1.getId(), user1.getId());

        // card1 和 card2 已到期，card3 未到期
        createSrsState(user1.getId(), card1.getId(), deck1.getId(), deck1.getNodeId(),
            (byte) 2, LocalDateTime.now().minusDays(1));
        createSrsState(user1.getId(), card2.getId(), deck1.getId(), deck1.getNodeId(),
            (byte) 2, LocalDateTime.now().minusHours(1));
        createSrsState(user1.getId(), card3.getId(), deck1.getId(), deck1.getNodeId(),
            (byte) 2, LocalDateTime.now().plusDays(1));

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/queue")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").exists())
                    .andExpect(jsonPath("$.data[0].front").exists())
                    .andExpect(jsonPath("$.data[0].back").exists())
                    .andExpect(jsonPath("$.data[0].deck").exists())
                    .andExpect(jsonPath("$.data[0].creator").exists())
                    .andExpect(jsonPath("$.data[0].srsState").exists())
                    .andExpect(jsonPath("$.data[0].hasDeckUpdate").exists())
                    .andExpect(jsonPath("$.data[0].hasCardUpdate").exists());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 1.1.2：获取空队列（无到期卡片）
     */
    @Test
    @DisplayName("获取复习队列 - 无到期卡片")
    void testGetReviewQueue_EmptyQueue() throws Exception {
        UserDO user1 = createUser("test-queue-2@test.com");
        MemoryCardDeckDO deck1 = createDeck("Python 基础", "Python 核心知识", 100L, 10L, user1.getId());
        MemoryCardDO card1 = createCard("什么是Python？", "编程语言", deck1.getId(), user1.getId());

        // 卡片未到期
        createSrsState(user1.getId(), card1.getId(), deck1.getId(), deck1.getNodeId(),
            (byte) 2, LocalDateTime.now().plusDays(1));

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/queue")
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
     * 测试场景 1.1.3：按课程筛选到期卡片
     */
    @Test
    @DisplayName("获取复习队列 - 按课程筛选")
    void testGetReviewQueue_FilterByCourse() throws Exception {
        UserDO user1 = createUser("test-queue-3@test.com");

        // 创建两个真实的课程和节点
        CourseDO course1 = createCourse("Java课程", user1.getId());
        NodeDO node1 = createNode("Java节点", course1.getId(), user1.getId());

        CourseDO course2 = createCourse("Python课程", user1.getId());
        NodeDO node2 = createNode("Python节点", course2.getId(), user1.getId());

        // 创建两个课程的卡片
        MemoryCardDeckDO deck1 = createDeck("Java 基础", "Java 知识", course1.getId(), node1.getId(), user1.getId());
        MemoryCardDeckDO deck2 = createDeck("Python 基础", "Python 知识", course2.getId(), node2.getId(), user1.getId());

        MemoryCardDO card1 = createCard("Java问题1", "Java答案1", deck1.getId(), user1.getId());
        MemoryCardDO card2 = createCard("Java问题2", "Java答案2", deck1.getId(), user1.getId());
        MemoryCardDO card3 = createCard("Python问题1", "Python答案1", deck2.getId(), user1.getId());

        // 创建 user_card_in_course 关联
        createUserCardInCourse(user1.getId(), card1.getId(), deck1.getId(), course1.getId());
        createUserCardInCourse(user1.getId(), card2.getId(), deck1.getId(), course1.getId());
        createUserCardInCourse(user1.getId(), card3.getId(), deck2.getId(), course2.getId());

        // 都已到期
        createSrsState(user1.getId(), card1.getId(), deck1.getId(), deck1.getNodeId(),
            (byte) 2, LocalDateTime.now().minusDays(1));
        createSrsState(user1.getId(), card2.getId(), deck1.getId(), deck1.getNodeId(),
            (byte) 2, LocalDateTime.now().minusDays(1));
        createSrsState(user1.getId(), card3.getId(), deck2.getId(), deck2.getNodeId(),
            (byte) 2, LocalDateTime.now().minusDays(1));

        StpUtil.login(user1.getId());

        try {
            // 只查询course1的卡片
            mockMvc.perform(get("/api/v1/memory/review/queue")
                            .param("courseId", course1.getId().toString())
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.length()").value(2));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 1.1.4：验证返回数量限制（20张）
     */
    @Test
    @DisplayName("获取复习队列 - 验证数量限制")
    void testGetReviewQueue_LimitTo20() throws Exception {
        UserDO user1 = createUser("test-queue-4@test.com");
        MemoryCardDeckDO deck1 = createDeck("测试卡片组", "大量卡片", 100L, 10L, user1.getId());

        // 创建 25 张到期卡片
        for (int i = 0; i < 25; i++) {
            MemoryCardDO card = createCard("问题" + i, "答案" + i, deck1.getId(), user1.getId());
            createSrsState(user1.getId(), card.getId(), deck1.getId(), deck1.getNodeId(),
                (byte) 2, LocalDateTime.now().minusDays(1));
        }

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/queue")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data.length()").value(20));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 1.2.1：courseId 为 0
     */
    @Test
    @DisplayName("参数验证 - courseId 为 0")
    void testGetReviewQueue_CourseIdZero() throws Exception {
        UserDO user1 = createUser("test-queue-5@test.com");
        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/queue")
                            .param("courseId", "0")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 1.2.2：courseId 为负数
     */
    @Test
    @DisplayName("参数验证 - courseId 为负数")
    void testGetReviewQueue_CourseIdNegative() throws Exception {
        UserDO user1 = createUser("test-queue-6@test.com");
        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/queue")
                            .param("courseId", "-1")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 1.3.1：未登录访问
     */
    @Test
    @DisplayName("权限验证 - 未登录访问复习队列")
    void testGetReviewQueue_NotLogin() throws Exception {
        mockMvc.perform(get("/api/v1/memory/review/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    /**
     * 测试场景 1.4.1：验证 SRS 状态字段完整性
     */
    @Test
    @DisplayName("数据一致性 - SRS 状态字段完整")
    void testGetReviewQueue_SrsStateComplete() throws Exception {
        UserDO user1 = createUser("test-queue-7@test.com");
        MemoryCardDeckDO deck1 = createDeck("测试卡片组", "测试", 100L, 10L, user1.getId());
        MemoryCardDO card1 = createCard("问题", "答案", deck1.getId(), user1.getId());
        createSrsState(user1.getId(), card1.getId(), deck1.getId(), deck1.getNodeId(),
            (byte) 2, LocalDateTime.now().minusDays(1));

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/queue")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].srsState.id").exists())
                    .andExpect(jsonPath("$.data[0].srsState.type").exists())
                    .andExpect(jsonPath("$.data[0].srsState.currentStep").exists())
                    .andExpect(jsonPath("$.data[0].srsState.interval").exists())
                    .andExpect(jsonPath("$.data[0].srsState.reviewDueAt").exists())
                    .andExpect(jsonPath("$.data[0].srsState.lastReviewedAt").exists())
                    .andExpect(jsonPath("$.data[0].srsState.repetitions").exists())
                    .andExpect(jsonPath("$.data[0].srsState.lapseCount").exists());
        } finally {
            StpUtil.logout();
        }
    }

    // ==================== 2. 获取卡片列表接口测试 ====================

    /**
     * 测试场景 2.1.1：获取第一页卡片（默认20张）
     */
    @Test
    @DisplayName("获取卡片列表 - 第一页")
    void testGetCardList_FirstPage() throws Exception {
        UserDO user1 = createUser("test-cards-1@test.com");
        MemoryCardDeckDO deck1 = createDeck("测试卡片组", "测试", 100L, 10L, user1.getId());

        // 创建 15 张卡片（5张到期，10张未到期）
        for (int i = 0; i < 15; i++) {
            MemoryCardDO card = createCard("问题" + i, "答案" + i, deck1.getId(), user1.getId());
            LocalDateTime dueAt = (i < 5)
                ? LocalDateTime.now().minusDays(1)
                : LocalDateTime.now().plusDays(1);
            createSrsState(user1.getId(), card.getId(), deck1.getId(), deck1.getNodeId(),
                (byte) 2, dueAt);
        }

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/cards")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(15));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 2.1.2：分页加载（使用 lastId）
     */
    @Test
    @DisplayName("获取卡片列表 - 分页加载")
    void testGetCardList_Pagination() throws Exception {
        UserDO user1 = createUser("test-cards-2@test.com");
        MemoryCardDeckDO deck1 = createDeck("测试卡片组", "测试", 100L, 10L, user1.getId());

        // 创建 25 张卡片
        for (int i = 0; i < 25; i++) {
            MemoryCardDO card = createCard("问题" + i, "答案" + i, deck1.getId(), user1.getId());
            createSrsState(user1.getId(), card.getId(), deck1.getId(), deck1.getNodeId(),
                (byte) 2, LocalDateTime.now().plusDays(1));
        }

        StpUtil.login(user1.getId());

        try {
            // 获取第一页
            String response = mockMvc.perform(get("/api/v1/memory/review/cards")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(20))
                    .andReturn().getResponse().getContentAsString();

            // 提取最后一张卡片的 ID
            int lastIdIndex = response.lastIndexOf("\"id\":");
            String idSubstring = response.substring(lastIdIndex + 5);
            int commaIndex = idSubstring.indexOf(",");
            Long lastId = Long.parseLong(idSubstring.substring(0, commaIndex).trim());

            // 获取第二页
            mockMvc.perform(get("/api/v1/memory/review/cards")
                            .param("lastId", String.valueOf(lastId))
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(5));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 2.1.4：空列表（用户无卡片）
     */
    @Test
    @DisplayName("获取卡片列表 - 空列表")
    void testGetCardList_EmptyList() throws Exception {
        UserDO user1 = createUser("test-cards-4@test.com");

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/cards")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 2.2.1：courseId 为 0
     */
    @Test
    @DisplayName("参数验证 - courseId 为 0")
    void testGetCardList_CourseIdZero() throws Exception {
        UserDO user1 = createUser("test-cards-5@test.com");
        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/cards")
                            .param("courseId", "0")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 2.2.4：lastId 为负数
     */
    @Test
    @DisplayName("参数验证 - lastId 为负数")
    void testGetCardList_LastIdNegative() throws Exception {
        UserDO user1 = createUser("test-cards-6@test.com");
        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/cards")
                            .param("lastId", "-1")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.INVALID_PARAMETER.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 2.3.1：未登录访问
     */
    @Test
    @DisplayName("权限验证 - 未登录访问卡片列表")
    void testGetCardList_NotLogin() throws Exception {
        mockMvc.perform(get("/api/v1/memory/review/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 3. 提交复习结果接口测试 ====================

    /**
     * 测试场景 3.1.1：提交复习结果（Good - 评分3）
     */
    @Test
    @DisplayName("提交复习结果 - Good 评分")
    void testSubmitReview_Good() throws Exception {
        UserDO user1 = createUser("test-submit-1@test.com");
        MemoryCardDeckDO deck1 = createDeck("测试卡片组", "测试", 100L, 10L, user1.getId());
        MemoryCardDO card1 = createCard("问题", "答案", deck1.getId(), user1.getId());
        UserCardSrsDO srs = createSrsState(user1.getId(), card1.getId(), deck1.getId(), deck1.getNodeId(),
            (byte) 2, LocalDateTime.now().minusDays(1));

        StpUtil.login(user1.getId());

        try {
            String requestBody = String.format(
                "{\"cardId\": %d, \"result\": 3, \"timeSpent\": 15}",
                card1.getId()
            );

            mockMvc.perform(post("/api/v1/memory/review/submit")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.message").value("操作成功"));

            // 验证 SRS 状态已更新
            UserCardSrsDO updated = userCardSrsDataService.getByUserAndCard(user1.getId(), card1.getId());
            assertThat(updated).isNotNull();
            assertThat(updated.getRepetitions()).isGreaterThan(srs.getRepetitions());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 3.1.4：提交复习结果（不传 timeSpent）
     */
    @Test
    @DisplayName("提交复习结果 - 不传 timeSpent")
    void testSubmitReview_WithoutTimeSpent() throws Exception {
        UserDO user1 = createUser("test-submit-2@test.com");
        MemoryCardDeckDO deck1 = createDeck("测试卡片组", "测试", 100L, 10L, user1.getId());
        MemoryCardDO card1 = createCard("问题", "答案", deck1.getId(), user1.getId());
        createSrsState(user1.getId(), card1.getId(), deck1.getId(), deck1.getNodeId(),
            (byte) 2, LocalDateTime.now().minusDays(1));

        StpUtil.login(user1.getId());

        try {
            String requestBody = String.format(
                "{\"cardId\": %d, \"result\": 3}",
                card1.getId()
            );

            mockMvc.perform(post("/api/v1/memory/review/submit")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 3.2.1：cardId 为 null
     */
    @Test
    @DisplayName("参数验证 - cardId 为 null")
    void testSubmitReview_CardIdNull() throws Exception {
        UserDO user1 = createUser("test-submit-3@test.com");
        StpUtil.login(user1.getId());

        try {
            String requestBody = "{\"result\": 3}";

            mockMvc.perform(post("/api/v1/memory/review/submit")
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
     * 测试场景 3.2.2：result 为 null
     */
    @Test
    @DisplayName("参数验证 - result 为 null")
    void testSubmitReview_ResultNull() throws Exception {
        UserDO user1 = createUser("test-submit-4@test.com");
        StpUtil.login(user1.getId());

        try {
            String requestBody = "{\"cardId\": 1}";

            mockMvc.perform(post("/api/v1/memory/review/submit")
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
     * 测试场景 3.2.3：result 为 0（小于1）
     */
    @Test
    @DisplayName("参数验证 - result 为 0")
    void testSubmitReview_ResultZero() throws Exception {
        UserDO user1 = createUser("test-submit-5@test.com");
        StpUtil.login(user1.getId());

        try {
            String requestBody = "{\"cardId\": 1, \"result\": 0}";

            mockMvc.perform(post("/api/v1/memory/review/submit")
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
     * 测试场景 3.2.4：result 为 5（大于4）
     */
    @Test
    @DisplayName("参数验证 - result 为 5")
    void testSubmitReview_ResultGreaterThan4() throws Exception {
        UserDO user1 = createUser("test-submit-6@test.com");
        StpUtil.login(user1.getId());

        try {
            String requestBody = "{\"cardId\": 1, \"result\": 5}";

            mockMvc.perform(post("/api/v1/memory/review/submit")
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
     * 测试场景 3.3.1：卡片不存在
     */
    @Test
    @DisplayName("业务错误 - 卡片不存在")
    void testSubmitReview_CardNotFound() throws Exception {
        UserDO user1 = createUser("test-submit-7@test.com");
        StpUtil.login(user1.getId());

        try {
            String requestBody = "{\"cardId\": 99999, \"result\": 3}";

            mockMvc.perform(post("/api/v1/memory/review/submit")
                            .header("token", StpUtil.getTokenValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.SRS_STATE_NOT_FOUND.getCode()));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 3.4.1：未登录提交
     */
    @Test
    @DisplayName("权限验证 - 未登录提交复习结果")
    void testSubmitReview_NotLogin() throws Exception {
        String requestBody = "{\"cardId\": 1, \"result\": 3}";

        mockMvc.perform(post("/api/v1/memory/review/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 4. 获取复习统计接口测试 ====================

    /**
     * 测试场景 4.1.2：获取本周统计（period=WEEK，默认）
     */
    @Test
    @DisplayName("获取复习统计 - 本周统计")
    void testGetReviewStats_Week() throws Exception {
        UserDO user1 = createUser("test-stats-1@test.com");

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/stats")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.totalReviews").exists())
                    .andExpect(jsonPath("$.data.streakDays").exists())
                    .andExpect(jsonPath("$.data.averageScore").exists())
                    .andExpect(jsonPath("$.data.timeSpent").exists());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 4.1.1：获取今日统计（period=DAY）
     */
    @Test
    @DisplayName("获取复习统计 - 今日统计")
    void testGetReviewStats_Day() throws Exception {
        UserDO user1 = createUser("test-stats-2@test.com");

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/stats")
                            .param("period", "DAY")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(StatusCode.OK.getCode()))
                    .andExpect(jsonPath("$.data").exists());
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 4.1.5：无数据时返回零值
     */
    @Test
    @DisplayName("获取复习统计 - 无数据返回零值")
    void testGetReviewStats_NoData() throws Exception {
        UserDO user1 = createUser("test-stats-3@test.com");

        StpUtil.login(user1.getId());

        try {
            mockMvc.perform(get("/api/v1/memory/review/stats")
                            .param("period", "DAY")
                            .header("token", StpUtil.getTokenValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalReviews").value(0))
                    .andExpect(jsonPath("$.data.streakDays").value(0))
                    .andExpect(jsonPath("$.data.averageScore").value(0.0))
                    .andExpect(jsonPath("$.data.timeSpent").value(0));
        } finally {
            StpUtil.logout();
        }
    }

    /**
     * 测试场景 4.3.1：未登录访问
     */
    @Test
    @DisplayName("权限验证 - 未登录访问统计")
    void testGetReviewStats_NotLogin() throws Exception {
        mockMvc.perform(get("/api/v1/memory/review/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(StatusCode.USER_NOT_LOGIN.getCode()));
    }

    // ==================== 5. 连续复习天数计算测试 ====================

    /**
     * 测试场景 5.1：连续复习多天
     */
    @Test
    @DisplayName("连续复习天数 - 连续3天复习")
    void testCalculateStreakDays_ContinuousReview() {
        // 准备测试数据
        UserDO user = createUser("test-streak-1@test.com");
        CourseDO course = createCourse("Test Course", user.getId());
        NodeDO node = createNode("Test Node", course.getId(), user.getId());
        MemoryCardDeckDO deck = createDeck("Test Deck", "Description", course.getId(), node.getId(), user.getId());
        MemoryCardDO card1 = createCard("Front 1", "Back 1", deck.getId(), user.getId());
        MemoryCardDO card2 = createCard("Front 2", "Back 2", deck.getId(), user.getId());
        MemoryCardDO card3 = createCard("Front 3", "Back 3", deck.getId(), user.getId());

        // 创建 SRS 状态，模拟连续3天复习
        LocalDateTime now = LocalDateTime.now();

        // 今天复习 card1
        UserCardSrsDO srs1 = createSrsState(user.getId(), card1.getId(), deck.getId(),
                                            node.getId(), UserCardSrsDO.TYPE_REVIEW, now.plusDays(1));
        srs1.setLastReviewedAt(now);
        userCardSrsDataService.update(srs1);

        // 昨天复习 card2
        UserCardSrsDO srs2 = createSrsState(user.getId(), card2.getId(), deck.getId(),
                                            node.getId(), UserCardSrsDO.TYPE_REVIEW, now.plusDays(1));
        srs2.setLastReviewedAt(now.minusDays(1));
        userCardSrsDataService.update(srs2);

        // 前天复习 card3
        UserCardSrsDO srs3 = createSrsState(user.getId(), card3.getId(), deck.getId(),
                                            node.getId(), UserCardSrsDO.TYPE_REVIEW, now.plusDays(1));
        srs3.setLastReviewedAt(now.minusDays(2));
        userCardSrsDataService.update(srs3);

        // 执行测试
        int streakDays = userCardSrsDataService.calculateStreakDays(user.getId());

        // 验证结果：连续3天复习
        assertThat(streakDays).isEqualTo(3);
    }

    /**
     * 测试场景 5.2：中断后继续复习
     */
    @Test
    @DisplayName("连续复习天数 - 中断后重新开始")
    void testCalculateStreakDays_AfterBreak() {
        // 准备测试数据
        UserDO user = createUser("test-streak-2@test.com");
        CourseDO course = createCourse("Test Course", user.getId());
        NodeDO node = createNode("Test Node", course.getId(), user.getId());
        MemoryCardDeckDO deck = createDeck("Test Deck", "Description", course.getId(), node.getId(), user.getId());
        MemoryCardDO card1 = createCard("Front 1", "Back 1", deck.getId(), user.getId());
        MemoryCardDO card2 = createCard("Front 2", "Back 2", deck.getId(), user.getId());

        LocalDateTime now = LocalDateTime.now();

        // 今天复习
        UserCardSrsDO srs1 = createSrsState(user.getId(), card1.getId(), deck.getId(),
                                            node.getId(), UserCardSrsDO.TYPE_REVIEW, now.plusDays(1));
        srs1.setLastReviewedAt(now);
        userCardSrsDataService.update(srs1);

        // 3天前复习（中间有中断）
        UserCardSrsDO srs2 = createSrsState(user.getId(), card2.getId(), deck.getId(),
                                            node.getId(), UserCardSrsDO.TYPE_REVIEW, now.plusDays(1));
        srs2.setLastReviewedAt(now.minusDays(3));
        userCardSrsDataService.update(srs2);

        // 执行测试
        int streakDays = userCardSrsDataService.calculateStreakDays(user.getId());

        // 验证结果：只有1天（因为中断了）
        assertThat(streakDays).isEqualTo(1);
    }

    /**
     * 测试场景 5.3：没有复习记录
     */
    @Test
    @DisplayName("连续复习天数 - 无复习记录")
    void testCalculateStreakDays_NoReview() {
        // 准备测试数据
        UserDO user = createUser("test-streak-3@test.com");

        // 执行测试
        int streakDays = userCardSrsDataService.calculateStreakDays(user.getId());

        // 验证结果：0天
        assertThat(streakDays).isEqualTo(0);
    }

    /**
     * 测试场景 5.4：今天没复习，昨天有复习
     */
    @Test
    @DisplayName("连续复习天数 - 今天未复习保持昨天的连续天数")
    void testCalculateStreakDays_TodayNoReview() {
        // 准备测试数据
        UserDO user = createUser("test-streak-4@test.com");
        CourseDO course = createCourse("Test Course", user.getId());
        NodeDO node = createNode("Test Node", course.getId(), user.getId());
        MemoryCardDeckDO deck = createDeck("Test Deck", "Description", course.getId(), node.getId(), user.getId());
        MemoryCardDO card1 = createCard("Front 1", "Back 1", deck.getId(), user.getId());
        MemoryCardDO card2 = createCard("Front 2", "Back 2", deck.getId(), user.getId());

        LocalDateTime now = LocalDateTime.now();

        // 昨天复习
        UserCardSrsDO srs1 = createSrsState(user.getId(), card1.getId(), deck.getId(),
                                            node.getId(), UserCardSrsDO.TYPE_REVIEW, now.plusDays(1));
        srs1.setLastReviewedAt(now.minusDays(1));
        userCardSrsDataService.update(srs1);

        // 前天复习
        UserCardSrsDO srs2 = createSrsState(user.getId(), card2.getId(), deck.getId(),
                                            node.getId(), UserCardSrsDO.TYPE_REVIEW, now.plusDays(1));
        srs2.setLastReviewedAt(now.minusDays(2));
        userCardSrsDataService.update(srs2);

        // 执行测试
        int streakDays = userCardSrsDataService.calculateStreakDays(user.getId());

        // 验证结果：连续2天（昨天和前天，今天未复习不影响）
        assertThat(streakDays).isEqualTo(2);
    }
}
