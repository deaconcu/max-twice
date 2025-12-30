package com.prosper.learn.web.v1.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prosper.learn.application.dto.request.CreateCardRequest;
import com.prosper.learn.application.dto.request.UpdateCardRequest;
import com.prosper.learn.content.course.CourseDO;
import com.prosper.learn.content.course.CourseDataService;
import com.prosper.learn.content.node.NodeDO;
import com.prosper.learn.content.node.NodeDataService;
import com.prosper.learn.memory.card.MemoryCardDO;
import com.prosper.learn.memory.card.MemoryCardDataService;
import com.prosper.learn.memory.deck.MemoryDeckDO;
import com.prosper.learn.memory.deck.MemoryDeckDataService;
import com.prosper.learn.memory.srs.UserCardSrsDO;
import com.prosper.learn.memory.srs.UserCardSrsDataService;
import com.prosper.learn.shared.domain.Enums.UserRole;
import com.prosper.learn.shared.domain.Enums.UserState;
import com.prosper.learn.user.profile.UserDO;
import com.prosper.learn.user.profile.UserDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class MemoryCardControllerTest extends BaseControllerTest {

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
    private MemoryDeckDataService deckDataService;

    @Autowired
    private MemoryCardDataService cardDataService;

    @Autowired
    private UserCardSrsDataService srsDataService;

    // 测试数据
    private UserDO testUser;
    private UserDO otherUser;
    private CourseDO testCourse;
    private NodeDO testNode;
    private MemoryDeckDO testDeck;
    private MemoryCardDO testCard1;
    private MemoryCardDO testCard2;
    private String testUserToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 创建测试用户
        testUser = createUser("test@example.com");
        otherUser = createUser("other@example.com");

        // 生成 Token
        testUserToken = generateToken(testUser.getId());
        otherUserToken = generateToken(otherUser.getId());

        // 创建测试课程
        testCourse = createCourse("测试课程");

        // 创建测试节点
        testNode = createNode(testCourse.getId(), "测试节点");

        // 创建测试卡片组
        testDeck = createDeck(testUser.getId(), testNode.getId(), testCourse.getId(), "测试卡片组");

        // 创建测试卡片
        testCard1 = createCard(testDeck.getId(), testUser.getId(), "问题1", "答案1");
        testCard2 = createCard(testDeck.getId(), testUser.getId(), "问题2", "答案2");

        // 创建 SRS 状态
        createSrsState(testUser.getId(), testCard1.getId(), 0); // NEW
        createSrsState(testUser.getId(), testCard2.getId(), 2); // REVIEW
    }

    // ========== 辅助方法 ==========

    /**
     * 创建测试用户
     */
    private UserDO createUser(String email) {
        UserDO user = new UserDO();
        user.setEmail(email);
        user.setName(email.split("@")[0]);
        user.setPassword("hashed_password");
        user.setState(UserState.ACTIVE.value());
        user.setEmailValidated(true);
        user.setBiography("");
        user.setRole(UserRole.USER.value());
        user.setMsgReadTime(LocalDateTime.now());
        userDataService.insert(user);
        return user;
    }

    /**
     * 创建测试课程
     */
    private CourseDO createCourse(String name) {
        CourseDO course = new CourseDO();
        course.setName(name);
        course.setDescription("测试课程描述");
        course.setCreatorId(testUser != null ? testUser.getId() : 1L);
        course.setParentCourseId(0L);
        course.setRootNodeId(0L);
        course.setMainCategory(1);
        course.setSubCategory(1);
        course.setState((byte) 2); // 已发布
        courseDataService.insert(course);
        return course;
    }

    /**
     * 创建测试节点
     */
    private NodeDO createNode(Long courseId, String name) {
        NodeDO node = new NodeDO();
        node.setCourseId(courseId);
        node.setName(name);
        node.setParentNodeId(0L);
        node.setNodeOrder(1);
        node.setContent("测试节点内容");
        nodeDataService.insert(node);
        return node;
    }

    /**
     * 创建测试卡片组
     */
    private MemoryDeckDO createDeck(Long creatorId, Long nodeId, Long courseId, String title) {
        MemoryDeckDO deck = new MemoryDeckDO();
        deck.setCreatorId(creatorId);
        deck.setNodeId(nodeId);
        deck.setCourseId(courseId);
        deck.setPostId(0L);
        deck.setTitle(title);
        deck.setDescription("测试卡片组描述");
        deck.setState(1); // 已发布
        deckDataService.insert(deck);
        return deck;
    }

    /**
     * 创建测试卡片
     */
    private MemoryCardDO createCard(Long deckId, Long creatorId, String front, String back) {
        MemoryCardDO card = new MemoryCardDO();
        card.setDeckId(deckId);
        card.setCreatorId(creatorId);
        card.setFront(front);
        card.setBack(back);
        cardDataService.insert(card);
        return card;
    }

    /**
     * 创建 SRS 学习状态
     */
    private UserCardSrsDO createSrsState(Long userId, Long cardId, int type) {
        UserCardSrsDO srs = new UserCardSrsDO();
        srs.setUserId(userId);
        srs.setCardId(cardId);
        srs.setType(type);
        srs.setCurrentStep(0);
        srs.setInterval(type == 2 ? 7 : 0); // REVIEW 状态间隔7天，其他为0
        srs.setReviewDueAt(LocalDateTime.now());
        srs.setLastReviewedAt(type == 0 ? null : LocalDateTime.now().minusDays(1));
        srs.setRepetitions(type == 0 ? 0 : 3);
        srs.setLapseCount(0);
        srs.setEaseFactor(2.5);
        srsDataService.insert(srs);
        return srs;
    }

    /**
     * 生成测试 Token
     */
    private String generateToken(Long userId) {
        StpUtil.login(userId);
        return StpUtil.getTokenValue();
    }

    // ========== 一、创建卡片接口测试 ==========

    @Test
    void testCreateCard_Success() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("什么是闭包？");
        request.setBack("闭包是函数和其周围词法环境的组合");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("创建成功"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testCreateCard_MissingDeckId() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setFront("什么是闭包？");
        request.setBack("闭包是函数和其周围词法环境的组合");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testCreateCard_MissingFront() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setBack("闭包是函数和其周围词法环境的组合");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testCreateCard_MissingBack() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("什么是闭包？");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testCreateCard_FrontTooLong() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("A".repeat(3000)); // 超过最大长度
        request.setBack("答案");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testCreateCard_BackTooLong() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("问题");
        request.setBack("A".repeat(3000)); // 超过最大长度

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testCreateCard_EmptyFront() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("   ");
        request.setBack("答案");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testCreateCard_DeckNotFound() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(999999L);
        request.setFront("什么是闭包？");
        request.setBack("闭包是函数和其周围词法环境的组合");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(2201));
    }

    @Test
    void testCreateCard_NotLogin() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("什么是闭包？");
        request.setBack("闭包是函数和其周围词法环境的组合");

        mockMvc.perform(post("/api/v1/memory/cards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1101));
    }

    // ========== 二、更新卡片接口测试 ==========

    @Test
    void testUpdateCard_Success() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("什么是闭包？（更新后）");
        request.setBack("闭包是函数和其词法环境的组合（更新后）");

        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("更新成功"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testUpdateCard_CardNotFound() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("更新的问题");
        request.setBack("更新的答案");

        mockMvc.perform(put("/api/v1/memory/cards/999999")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(2202));
    }

    @Test
    void testUpdateCard_InvalidCardId() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("更新的问题");
        request.setBack("更新的答案");

        mockMvc.perform(put("/api/v1/memory/cards/0")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testUpdateCard_PermissionDenied() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("更新的问题");
        request.setBack("更新的答案");

        // 使用另一个用户的 token
        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", otherUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1006));
    }

    @Test
    void testUpdateCard_FrontTooLong() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("A".repeat(2100)); // 超过 2000 字符
        request.setBack("答案");

        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testUpdateCard_BackTooLong() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("问题");
        request.setBack("A".repeat(2100)); // 超过 2000 字符

        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testUpdateCard_NotLogin() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("更新的问题");
        request.setBack("更新的答案");

        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1101));
    }

    // ========== 三、获取节点下卡片列表接口测试 ==========

    @Test
    void testGetCardsByNode_Success() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/node/" + testNode.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testGetCardsByNode_EmptyList() throws Exception {
        // 创建一个新节点，没有卡片
        NodeDO emptyNode = createNode(testCourse.getId(), "空节点");

        mockMvc.perform(get("/api/v1/memory/cards/node/" + emptyNode.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testGetCardsByNode_ContainsCompleteData() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/memory/cards/node/" + testNode.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("\"id\""));
        assertTrue(content.contains("\"front\""));
        assertTrue(content.contains("\"back\""));
        assertTrue(content.contains("\"deck\""));
        assertTrue(content.contains("\"creator\""));
        assertTrue(content.contains("\"srsState\""));
    }

    @Test
    void testGetCardsByNode_InvalidNodeId() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/node/0")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testGetCardsByNode_NotLogin() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/node/" + testNode.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1101));
    }

    // ========== 四、获取卡片内容差异接口测试 ==========

    @Test
    void testGetCardDiff_NoDiff() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/" + testCard1.getId() + "/diff")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.hasDiff").value(false));
    }

    @Test
    void testGetCardDiff_CardNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/999999/diff")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(2202));
    }

    @Test
    void testGetCardDiff_PermissionDenied() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/" + testCard1.getId() + "/diff")
                .header("token", otherUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1006));
    }

    @Test
    void testGetCardDiff_InvalidCardId() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/-1/diff")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testGetCardDiff_NotLogin() throws Exception {
        mockMvc.perform(get("/api/v1/memory/cards/" + testCard1.getId() + "/diff"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1101));
    }

    // ========== 五、删除卡片接口测试 ==========

    @Test
    void testDeleteCard_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("删除成功"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testDeleteCard_CardNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/memory/cards/999999")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(2202));
    }

    @Test
    void testDeleteCard_AlreadyDeleted() throws Exception {
        // 先删除一次
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        // 再删除一次，应该返回卡片不存在
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(2202));
    }

    @Test
    void testDeleteCard_PermissionDenied() throws Exception {
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", otherUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1006));
    }

    @Test
    void testDeleteCard_InvalidCardId() throws Exception {
        mockMvc.perform(delete("/api/v1/memory/cards/0")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void testDeleteCard_NotLogin() throws Exception {
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1101));
    }

    // ========== 六、响应格式测试 ==========

    @Test
    void testCreateCard_ResponseFormat() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("问题");
        request.setBack("答案");

        MvcResult result = mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();

        // 验证包含必要字段
        assertTrue(content.contains("\"code\""));
        assertTrue(content.contains("\"message\""));
        assertTrue(content.contains("\"timestamp\""));

        // 验证不包含 data 字段（或 data 为 null 被省略）
        assertFalse(content.contains("\"data\""));
    }

    @Test
    void testUpdateCard_ResponseFormat() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setFront("更新的问题");
        request.setBack("更新的答案");

        MvcResult result = mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();

        assertTrue(content.contains("\"code\""));
        assertTrue(content.contains("\"message\""));
        assertTrue(content.contains("\"timestamp\""));
        assertFalse(content.contains("\"data\""));
    }

    @Test
    void testDeleteCard_ResponseFormat() throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();

        assertTrue(content.contains("\"code\""));
        assertTrue(content.contains("\"message\""));
        assertTrue(content.contains("\"timestamp\""));
        assertFalse(content.contains("\"data\""));
    }

    // ========== 七、边界条件测试 ==========

    @Test
    void testCreateCard_SpecialCharacters() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("问题包含特殊字符：<>\"'&\n\t😀");
        request.setBack("答案包含特殊字符：<>\"'&\n\t😀");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testCreateCard_UnicodeCharacters() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setDeckId(testDeck.getId());
        request.setFront("问题：日本語、中文、한국어");
        request.setBack("答案：🎉🎊🎈");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    // ========== 八、集成测试 ==========

    @Test
    void testCardLifecycle_Complete() throws Exception {
        // 1. 创建卡片
        CreateCardRequest createRequest = new CreateCardRequest();
        createRequest.setDeckId(testDeck.getId());
        createRequest.setFront("生命周期测试问题");
        createRequest.setBack("生命周期测试答案");

        mockMvc.perform(post("/api/v1/memory/cards")
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        // 2. 查询卡片列表（验证创建成功）
        MvcResult listResult = mockMvc.perform(get("/api/v1/memory/cards/node/" + testNode.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn();

        String listContent = listResult.getResponse().getContentAsString();
        assertTrue(listContent.contains("生命周期测试问题"));

        // 3. 更新卡片
        UpdateCardRequest updateRequest = new UpdateCardRequest();
        updateRequest.setFront("生命周期测试问题（已更新）");
        updateRequest.setBack("生命周期测试答案（已更新）");

        mockMvc.perform(put("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        // 4. 删除卡片
        mockMvc.perform(delete("/api/v1/memory/cards/" + testCard1.getId())
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        // 5. 验证删除后不能再次访问
        mockMvc.perform(get("/api/v1/memory/cards/" + testCard1.getId() + "/diff")
                .header("token", testUserToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(2202));
    }
}
